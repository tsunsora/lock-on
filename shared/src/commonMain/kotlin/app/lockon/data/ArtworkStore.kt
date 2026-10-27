package app.lockon.data

import app.lockon.model.Printing
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import okio.ByteString.Companion.encodeUtf8

/** How many of the catalog's card images are saved on this device. */
data class ArtworkStatus(val saved: Int = 0, val total: Int = 0, val downloading: Boolean = false) {
    val complete: Boolean get() = total > 0 && saved >= total
}

/**
 * Card images kept on this device for good, so cards show their artwork offline. Each image is
 * downloaded once, either when a card is first shown or by [downloadAll], and is never fetched
 * again while the catalog still links to it. Images never sync.
 */
class ArtworkStore(directory: String, client: () -> HttpClient) {
    private val http by lazy(client)
    private val dir = Path(directory)
    private val lock = Mutex()
    private val inFlight = mutableMapOf<String, CompletableDeferred<Path>>()
    private val _status = MutableStateFlow(ArtworkStatus())
    val status: StateFlow<ArtworkStatus> = _status.asStateFlow()

    /** The saved file for [url]: named by a hash of the link, so a changed link is a new image. */
    fun fileFor(url: String): Path = Path(dir, url.encodeUtf8().sha256().hex())

    fun has(url: String): Boolean = SystemFileSystem.exists(fileFor(url))

    /** The saved image for [url], downloading it first if needed. Two callers never download it twice. */
    suspend fun fetch(url: String): Path {
        val file = fileFor(url)
        if (SystemFileSystem.exists(file)) return file
        val (waiter, mine) = lock.withLock {
            inFlight[url]?.let { it to false } ?: CompletableDeferred<Path>().also { inFlight[url] = it }.let { it to true }
        }
        if (!mine) return waiter.await()
        try {
            val saved = download(url, file)
            waiter.complete(saved)
            return saved
        } catch (e: Throwable) {
            waiter.completeExceptionally(e)
            throw e
        } finally {
            lock.withLock { inFlight.remove(url) }
        }
    }

    private suspend fun download(url: String, file: Path): Path = withContext(Dispatchers.IO) {
        val response = http.get(url)
        check(response.status.isSuccess()) { "Image download failed: HTTP ${response.status.value}" }
        val bytes = response.readRawBytes()
        SystemFileSystem.createDirectories(dir)
        // Written aside and moved into place, so a download cut short never looks like a saved image.
        val partial = Path(dir, "${file.name}.part")
        SystemFileSystem.sink(partial).buffered().use { it.write(bytes) }
        SystemFileSystem.atomicMove(partial, file)
        file
    }

    /**
     * Downloads every image [printings] link to that isn't saved yet, standard printings first, a
     * few at a time so cards on screen still load quickly. Failures are skipped and retried the
     * next time this runs. Once everything is saved, images the catalog no longer links to go.
     */
    suspend fun downloadAll(printings: Collection<Printing>) {
        val urls = printings.sortedBy { it.isAlternateArt }.mapNotNull { it.artworkUrl?.takeIf(String::isNotBlank) }.distinct()
        val missing = withContext(Dispatchers.IO) { urls.filterNot(::has) }
        _status.value = ArtworkStatus(urls.size - missing.size, urls.size, downloading = missing.isNotEmpty())
        if (missing.isNotEmpty()) {
            val permits = Semaphore(BULK_DOWNLOADS)
            coroutineScope {
                missing.forEach { url ->
                    launch {
                        permits.withPermit {
                            if (runCatching { fetch(url) }.isSuccess) _status.update { it.copy(saved = it.saved + 1) }
                        }
                    }
                }
            }
        }
        _status.update { it.copy(downloading = false) }
        if (urls.isNotEmpty() && _status.value.complete) withContext(Dispatchers.IO) { removeAllExcept(urls) }
    }

    private fun removeAllExcept(urls: List<String>) {
        runCatching {
            if (!SystemFileSystem.exists(dir)) return
            val keep = urls.mapTo(HashSet()) { fileFor(it).name }
            SystemFileSystem.list(dir).filter { it.name !in keep }.forEach { SystemFileSystem.delete(it, mustExist = false) }
        }
    }

    private companion object {
        /** Background downloads at once, leaving room in the client for the cards on screen. */
        const val BULK_DOWNLOADS = 4
    }
}
