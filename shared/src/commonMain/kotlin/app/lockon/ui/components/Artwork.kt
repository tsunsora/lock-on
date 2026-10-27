package app.lockon.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.crossfade
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import okio.Path.Companion.toPath

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
    // One timer per card: until the image loads it waits to show the placeholder, then to drop it.
    LaunchedEffect(url, loaded) {
        if (!loaded) { delay(PLACEHOLDER_DELAY_MS); slow = true } else { delay(CROSSFADE_MS + 50L); covered = true }
    }
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

/** Image loader for card artwork. Images come from [store], so each downloads once and then works offline. */
fun artworkImageLoader(context: PlatformContext, store: ArtworkStore): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(StoredArtworkFetcher.Factory(store)) }
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.15).build() }
        .crossfade(CROSSFADE_MS)
        .build()

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

/** How many artwork downloads run at once from one server. */
internal const val ARTWORK_DOWNLOADS = 12

/** The HTTP client for artwork, allowing [ARTWORK_DOWNLOADS] requests per server. */
expect fun artworkHttpClient(): HttpClient
