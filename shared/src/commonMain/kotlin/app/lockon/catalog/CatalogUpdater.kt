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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
        val converted = GcgApi.toCatalog(manifest, entries)
        val checked = catalog.validate(converted.catalog)
        val note = "${converted.skippedTokens} token cards (EX Base, EX Resource, Unit tokens) are left out because they can't be put in a deck."
        preview(checked.copy(warnings = checked.warnings + note), CatalogOrigin.GCG_API, GcgApi.MANIFEST_URL)
    }

    sealed interface UpdateCheck {
        data class UpToDate(val version: Int) : UpdateCheck
        data class Available(val version: Int, val cardCount: Int, val builtAt: String) : UpdateCheck
    }

    /** Asks gcg-api for its latest version without downloading the cards (a small file). */
    suspend fun checkCardDatabase(): UpdateCheck {
        val manifest = officialManifest()
        catalog.markUpdateChecked()
        val installed = catalog.info.value
        val newer = installed?.catalogId != GcgApi.CATALOG_ID || manifest.run > installed.version
        return if (newer) UpdateCheck.Available(manifest.run, manifest.cardCount, manifest.builtAt.take(10))
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
            isNewer = installed?.catalogId != checked.file.catalogId || checked.file.version > installed.version,
            missingFromDecks = used - numbers,
        )
    }

    private companion object {
        const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

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
