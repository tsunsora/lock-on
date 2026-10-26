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
import kotlinx.coroutines.flow.first

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
    suspend fun fromText(text: String): CatalogPreview = preview(catalog.check(text), CatalogOrigin.FILE, null)

    suspend fun fromUrl(url: String): CatalogPreview {
        val clean = url.trim()
        if (!clean.startsWith("https://")) throw CatalogException("Use an https:// link.")
        val text = try {
            fetch(clean)
        } catch (e: CatalogException) {
            throw e
        } catch (e: Exception) {
            throw CatalogException("Couldn't download the catalog. Check the link and your connection.")
        }
        return preview(catalog.check(text), CatalogOrigin.URL, clean)
    }

    suspend fun install(preview: CatalogPreview) = catalog.install(preview.checked.file, preview.origin, preview.url)

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
