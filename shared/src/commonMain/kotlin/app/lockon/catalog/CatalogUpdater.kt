package app.lockon.catalog

import app.lockon.data.CatalogException
import app.lockon.data.CatalogOrigin
import app.lockon.data.CatalogRepository
import app.lockon.data.CheckedCatalog
import app.lockon.data.DeckRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** Platform HTTP client for public downloads, trusting the normal certificate authorities. */
expect fun downloadClient(): HttpClient

/** A checked catalog waiting for the person to confirm, with what installing it would mean. */
data class CatalogPreview(
    val checked: CheckedCatalog,
    val origin: CatalogOrigin,
    val url: String?,
    /** Same catalog ID as the installed one, so this is an update rather than a replacement. */
    val isUpdate: Boolean,
    val isNewer: Boolean,
    /** Card numbers used in decks that this catalog doesn't have. */
    val missingFromDecks: Set<String>,
)

/**
 * Brings in a catalog from a file or a link. Nothing is installed until [install] is called with
 * a preview the person has seen (UX rule C2: replacing a catalog can't be undone).
 */
class CatalogUpdater(
    private val catalog: CatalogRepository,
    private val decks: DeckRepository,
    private val fetch: suspend (url: String) -> String = ::httpGet,
) {
    // A full card list is megabytes of JSON, so parsing and checking it stays off the main thread.

    suspend fun fromText(text: String): CatalogPreview =
        withContext(Dispatchers.Default) { preview(catalog.check(text), CatalogOrigin.FILE, null) }

    suspend fun fromUrl(url: String): CatalogPreview = withContext(Dispatchers.Default) {
        val clean = url.trim()
        if (!clean.startsWith("https://")) throw CatalogException("Use an https:// link.")
        val text = try {
            fetch(clean)
        } catch (e: CatalogException) {
            throw e
        } catch (e: Exception) {
            throw CatalogException("Couldn't download the catalog. Check the link and your connection.")
        }
        preview(catalog.check(text), CatalogOrigin.URL, clean)
    }

    suspend fun install(preview: CatalogPreview) = catalog.install(preview.checked.file, preview.origin, preview.url)

    /** Downloads the latest official card list from gcg-api and converts it on this device. */
    suspend fun latestCardDatabase(): CatalogPreview = withContext(Dispatchers.Default) {
        val manifest = officialManifest()
        val entries = try {
            GcgApi.parseCards(fetchOrExplain(GcgApi.CARDS_URL))
        } catch (e: CatalogException) {
            throw e
        } catch (e: Exception) {
            throw CatalogException("The card database didn't download in a readable form. Try again later.")
        }
        val extras = bandaiAltArts(entries)
        val converted = GcgApi.toCatalog(manifest, entries + extras)
        val file = converted.catalog.let { if (extras.isEmpty()) it else it.copy(source = it.source + " " + BANDAI_NOTE) }
        val checked = catalog.validate(file)
        val notes = listOfNotNull(
            "${converted.skippedUnknown} cards of a type Lock On doesn't know yet are left out.".takeIf { converted.skippedUnknown > 0 },
            "${extras.size} alt arts come from Bandai's card list, ahead of gcg-api.".takeIf { extras.isNotEmpty() },
        )
        preview(checked.copy(warnings = checked.warnings + notes), CatalogOrigin.GCG_API, GcgApi.MANIFEST_URL)
    }

    /**
     * Alt arts on Bandai's card list that gcg-api doesn't have yet, as gcg-api entries copied from the
     * standard printing. Only sets with no alt arts in gcg-api are looked up, and any failure just means
     * none are added.
     */
    private suspend fun bandaiAltArts(entries: List<GcgApi.Entry>): List<GcgApi.Entry> {
        val standards = entries.filter { it.productId == it.cardNumber }.associateBy { it.cardNumber }
        val missing = missingBandaiPrintings(
            entries.map { it.setCode to (it.productId != it.cardNumber) },
            known = entries.map { it.productId }.toSet(),
            cardNumbers = standards.keys,
        )
        if (missing.isEmpty()) return emptyList()
        val gate = Semaphore(4)
        return coroutineScope {
            missing.map { listed ->
                async {
                    val detail = gate.withPermit {
                        try {
                            BandaiCardList.parseDetail(fetch(BandaiCardList.detailUrl(listed.printingId)))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            null
                        }
                    } ?: return@async null
                    standards.getValue(listed.cardNumber).copy(
                        productId = listed.printingId,
                        rarity = detail.rarity,
                        whereToGet = detail.whereToGet,
                        imageUrl = listed.artworkUrl,
                    )
                }
            }.awaitAll().filterNotNull()
        }
    }

    /** Printings of known cards that Bandai lists in sets without alt arts and [known] lacks. Empty when Bandai can't be read. */
    private suspend fun missingBandaiPrintings(
        printings: List<Pair<String, Boolean>>,
        known: Set<String>,
        cardNumbers: Set<String>,
    ): List<BandaiCardList.Listed> = try {
        val sets = BandaiCardList.setsWithoutAltArts(printings)
        if (sets.isEmpty()) return emptyList()
        val packages = BandaiCardList.parsePackages(fetch(BandaiCardList.SEARCH_URL)).filter { it.code in sets }
        packages.flatMap { pkg ->
            BandaiCardList.parseListing(fetch(BandaiCardList.packageUrl(pkg.id)))
                .filter { !it.isStandard && it.printingId !in known && it.cardNumber in cardNumbers }
        }.distinctBy { it.printingId }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyList()
    }

    sealed interface UpdateCheck {
        data class UpToDate(val version: Int) : UpdateCheck
        /** [newAltArts] counts alt arts on Bandai's card list that the installed list lacks. */
        data class Available(val version: Int, val cardCount: Int, val builtAt: String, val newAltArts: Int = 0) : UpdateCheck
    }

    /**
     * Asks gcg-api for its latest version without downloading the cards (a small file). When that
     * isn't newer, Bandai's card list is checked for alt arts the installed list lacks.
     */
    suspend fun checkCardDatabase(): UpdateCheck {
        val manifest = officialManifest()
        catalog.markUpdateChecked()
        val installed = catalog.info.value
        val newer = installed?.catalogId != GcgApi.CATALOG_ID || manifest.run > installed.version || convertedWithoutTokens()
        if (newer) return UpdateCheck.Available(manifest.run, manifest.cardCount, manifest.builtAt.take(10))
        val printings = catalog.printings.value.values.flatten()
        val altArts = withContext(Dispatchers.Default) {
            missingBandaiPrintings(
                printings.map { it.setCode to it.isAlternateArt },
                known = printings.map { it.printingId }.toSet(),
                cardNumbers = catalog.cards.value.keys,
            ).size
        }
        return if (altArts > 0) UpdateCheck.Available(manifest.run, manifest.cardCount, manifest.builtAt.take(10), altArts)
        else UpdateCheck.UpToDate(manifest.run)
    }

    /**
     * Background check, at most weekly, only for people already using the official list. A newer
     * list shows as a quiet note in the Cards tab; nothing pops up and nothing installs itself (UX
     * rules C1, C2).
     */
    suspend fun quietWeeklyCheck(now: Long) {
        if (catalog.info.value?.origin != CatalogOrigin.GCG_API) return
        if (now - catalog.lastUpdateCheck() < WEEK_MS) return
        val result = runCatching { checkCardDatabase() }.getOrNull()
        if (result is UpdateCheck.Available) catalog.setUpdateAvailable("Card database update available")
    }

    /**
     * Official lists converted before tokens were kept have none, and ones converted before base tokens
     * had their own type list them as bases, so the same version is worth downloading again.
     */
    private fun convertedWithoutTokens(): Boolean {
        if (catalog.info.value?.catalogId != GcgApi.CATALOG_ID) return false
        val cards = catalog.cards.value.values
        return cards.none { it.type.isToken } || cards.any { GcgApi.isTokenNumber(it.cardNumber) && !it.type.isToken }
    }

    /** Same gcg-api version, but with alt arts from Bandai's card list that the installed one lacks. */
    private fun addsPrintings(checked: CheckedCatalog): Boolean {
        val installed = catalog.printings.value.values.flatten().map { it.printingId }.toSet()
        return checked.file.printings.any { it.printingId !in installed }
    }

    private suspend fun officialManifest(): GcgApi.Manifest = try {
        GcgApi.parseManifest(fetchOrExplain(GcgApi.MANIFEST_URL))
    } catch (e: CatalogException) {
        throw e
    } catch (e: Exception) {
        throw CatalogException("The card database's version file wasn't readable. Try again later.")
    }

    private suspend fun fetchOrExplain(url: String): String = try {
        fetch(url)
    } catch (e: CatalogException) {
        throw e
    } catch (e: Exception) {
        throw CatalogException("Couldn't reach the card database. Check your internet connection.")
    }

    private suspend fun preview(checked: CheckedCatalog, origin: CatalogOrigin, url: String?): CatalogPreview {
        val installed = catalog.info.value
        val numbers = checked.file.cards.map { it.cardNumber }.toSet()
        val used = decks.decks.first().flatMap { deck -> deck.snapshot.entries.map { it.cardNumber } }.toSet()
        return CatalogPreview(
            checked = checked,
            origin = origin,
            url = url,
            isUpdate = installed?.catalogId == checked.file.catalogId,
            isNewer = installed?.catalogId != checked.file.catalogId || checked.file.version > installed.version ||
                (origin == CatalogOrigin.GCG_API && (convertedWithoutTokens() || addsPrintings(checked))),
            missingFromDecks = used - numbers,
        )
    }

    private companion object {
        const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
        const val BANDAI_NOTE = "Alt arts not yet in gcg-api are read from Bandai's official card list."

        suspend fun httpGet(url: String): String {
            val client = downloadClient()
            try {
                val response = client.get(url)
                if (!response.status.isSuccess()) throw CatalogException("The link answered with an error (${response.status.value}).")
                return response.bodyAsText()
            } finally {
                client.close()
            }
        }
    }
}
