package app.lockon.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import app.lockon.model.Printing
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.AsyncImage
import coil3.memory.MemoryCache
import app.lockon.data.ArtworkStore
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.request.crossfade
import io.ktor.client.HttpClient
import coil3.EventListener
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okio.Path.Companion.toPath
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Finds the artwork for a card: the chosen printing's image, else the standard printing's, else any.
 * Artwork is display-only and never syncs; with it turned off, every lookup returns null.
 */
class ArtworkSource(val enabled: Boolean, printings: Map<String, List<Printing>>) {
    // Worked out once: every card view on screen asks on each recomposition.
    private val options = if (enabled) printings.mapValues { (_, list) -> list.filter { !it.artworkUrl.isNullOrBlank() } } else emptyMap()
    private val standard = options.mapValues { (_, list) -> (list.firstOrNull { !it.isAlternateArt } ?: list.firstOrNull())?.artworkUrl }

    fun urlFor(cardNumber: String, printingId: String? = null): String? {
        if (!enabled) return null
        val chosen = printingId?.let { id -> options[cardNumber]?.firstOrNull { it.printingId == id } }
        return chosen?.artworkUrl ?: standard[cardNumber]
    }
}

val LocalArtwork = staticCompositionLocalOf { ArtworkSource(enabled = false, printings = emptyMap()) }

/** Where card images are saved on this device; null outside the app, as in previews. */
val LocalArtworkStore = staticCompositionLocalOf<ArtworkStore?> { null }

/** How long a loaded image takes to fade in over its placeholder. */
private const val CROSSFADE_MS = 200

/** How long an image may take before its placeholder face shows. Saved images arrive well within it. */
private const val PLACEHOLDER_DELAY_MS = 150L

/**
 * Draws the artwork, with [fallback] (the coloured placeholder face) under it when there is no
 * image, the image fails, or it takes longer than [PLACEHOLDER_DELAY_MS]. Building the placeholder
 * is the costliest part of a card coming into view, so cards scrolled in from saved images skip it.
 * Once an image has faded in over a placeholder, the placeholder goes.
 */
@Composable
fun CardArtwork(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    fallback: @Composable () -> Unit,
) {
    var failed by remember(url) { mutableStateOf(false) }
    var loaded by remember(url) { mutableStateOf(false) }
    var slow by remember(url) { mutableStateOf(false) }
    var covered by remember(url) { mutableStateOf(false) }
    // Until the image loads, a timer waits to show the placeholder. Only a placeholder that showed
    // needs a second timer to drop it after the crossfade; most images arrive first and need neither.
    val timers = LocalArtworkTimers.current
    if (!loaded) After(timers, url, PLACEHOLDER_DELAY_MS) { slow = true }
    else if (slow && !covered) After(timers, url, CROSSFADE_MS + 50L) { covered = true }
    Box(modifier) {
        if (url == null || failed || (slow && !covered)) fallback()
        if (url != null && !failed) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = contentScale,
                alignment = alignment,
                onSuccess = { loaded = true },
                onError = { failed = true },
                // Images are decoded at the size they're shown, so smooth filtering with mipmaps is
                // enough; High (bicubic) costs more on every frame and looks the same here.
                filterQuality = FilterQuality.Medium,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Runs [then] in [millis], unless this leaves the screen or [key] changes first. */
@Composable
private fun After(timers: ArtworkTimers?, key: Any?, millis: Long, then: () -> Unit) {
    if (timers == null) LaunchedEffect(key) { delay(millis); then() }
    else DisposableEffect(key, timers) {
        val wait = timers.after(millis, then)
        onDispose { wait.cancel() }
    }
}

/**
 * One timer for every card waiting on its image. Each card used to start a coroutine and a timer of
 * its own and cancel them when its image arrived, a moment later. On desktop each of those is a task
 * posted to the window's event queue, three a card, in the frame a row of cards scrolls in. Here a
 * card only adds itself to a list; one coroutine sleeps until the earliest is due. Used from the UI
 * thread only.
 */
class ArtworkTimers(private val scope: CoroutineScope) {
    class Wait internal constructor(internal val due: ComparableTimeMark, internal val then: () -> Unit) {
        internal var cancelled = false
        fun cancel() { cancelled = true }
    }

    // Soonest first. Cancelled ones stay until their turn comes, which costs nothing.
    private val waits = ArrayDeque<Wait>()
    private var ticking: Job? = null

    fun after(millis: Long, then: () -> Unit): Wait {
        val wait = Wait(TimeSource.Monotonic.markNow() + millis.milliseconds, then)
        // Nearly always the latest, so this is nearly always an append.
        var at = waits.size
        while (at > 0 && waits[at - 1].due > wait.due) at--
        waits.add(at, wait)
        // The sleeper is waiting for what was the soonest; a new soonest starts it again.
        if (at == 0) {
            ticking?.cancel()
            ticking = scope.launch { tick() }
        }
        return wait
    }

    private suspend fun tick() {
        while (true) {
            val next = waits.firstOrNull() ?: break
            if (next.cancelled) { waits.removeFirst(); continue }
            val left = -next.due.elapsedNow()
            if (left.isPositive()) { delay(left); continue }
            waits.removeFirst()
            next.then()
        }
        ticking = null
    }
}

/** The app's [ArtworkTimers]; null outside the app, as in previews, where each card keeps a timer of its own. */
val LocalArtworkTimers = staticCompositionLocalOf<ArtworkTimers?> { null }

/** Counts artwork images loading right now, so the boot screen can stay up until the screens under it have theirs. */
object ArtworkLoads : EventListener() {
    private val count = MutableStateFlow(0)
    val inFlight: StateFlow<Int> = count.asStateFlow()

    override fun onStart(request: ImageRequest) = count.update { it + 1 }
    override fun onSuccess(request: ImageRequest, result: SuccessResult) = count.update { it - 1 }
    override fun onError(request: ImageRequest, result: ErrorResult) = count.update { it - 1 }
    override fun onCancel(request: ImageRequest) = count.update { it - 1 }
}

/** Image loader for card artwork. Images come from [store], so each downloads once and then works offline. */
fun artworkImageLoader(context: PlatformContext, store: ArtworkStore): ImageLoader =
    ImageLoader.Builder(context)
        .components {
            add(SizedArtworkKeyer)
            add(StoredArtworkFetcher.Factory(store))
            artworkDecoder()?.let { add(it) }
        }
        .eventListener(ArtworkLoads)
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.15).build() }
        .crossfade(CROSSFADE_MS)
        .build()

/**
 * Keys each image in the memory cache by its link and the size it's shown at. Coil keys by link
 * alone, so a card decoded large for its hover preview would be reused by every small copy of it
 * and shrunk while drawing, which leaves text and edges jagged; each size gets its own smooth decode.
 */
object SizedArtworkKeyer : Keyer<Uri> {
    override fun key(data: Uri, options: Options): String = "$data#${options.size}"
}

/** Loads an artwork link from the [ArtworkStore], downloading it into the store the first time. */
private class StoredArtworkFetcher(private val url: String, private val store: ArtworkStore, private val options: Options) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val wasSaved = store.has(url)
        val file = store.fetch(url)
        return SourceFetchResult(
            ImageSource(file.toString().toPath(), options.fileSystem),
            mimeType = null,
            dataSource = if (wasSaved) DataSource.DISK else DataSource.NETWORK,
        )
    }

    class Factory(private val store: ArtworkStore) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? =
            if (data.scheme == "https") StoredArtworkFetcher(data.toString(), store, options) else null
    }
}

/** A decoder that resizes artwork more smoothly than Coil's default on this platform, if it needs one. */
internal expect fun artworkDecoder(): Decoder.Factory?

/** How many artwork downloads run at once from one server. */
internal const val ARTWORK_DOWNLOADS = 12

/** The HTTP client for artwork, allowing [ARTWORK_DOWNLOADS] requests per server. */
expect fun artworkHttpClient(): HttpClient
