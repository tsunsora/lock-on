package app.lockon.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import app.lockon.model.Printing
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.AsyncImage
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import okio.Path.Companion.toPath

/**
 * Finds the artwork for a card: the chosen printing's image, else the standard printing's, else any.
 * Artwork is display-only and never syncs; with it turned off, every lookup returns null.
 */
class ArtworkSource(val enabled: Boolean, private val printings: Map<String, List<Printing>>) {
    fun urlFor(cardNumber: String, printingId: String? = null): String? {
        if (!enabled) return null
        val options = printings[cardNumber]?.filter { !it.artworkUrl.isNullOrBlank() } ?: return null
        val chosen = printingId?.let { id -> options.firstOrNull { it.printingId == id } }
        return (chosen ?: options.firstOrNull { !it.isAlternateArt } ?: options.firstOrNull())?.artworkUrl
    }
}

val LocalArtwork = staticCompositionLocalOf { ArtworkSource(enabled = false, printings = emptyMap()) }

/**
 * Draws [fallback] (the coloured placeholder face) and lays the artwork over it once it loads, so
 * nothing jumps while images arrive and a broken link simply leaves the placeholder.
 */
@Composable
fun CardArtwork(url: String?, contentDescription: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop, fallback: @Composable () -> Unit) {
    var failed by remember(url) { mutableStateOf(false) }
    Box(modifier) {
        fallback()
        if (url != null && !failed) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = contentScale,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Image loader with a disk cache, so each image downloads once per device. */
fun artworkImageLoader(context: PlatformContext, cacheDir: String): ImageLoader =
    ImageLoader.Builder(context)
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.15).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.toPath()).maxSizeBytes(300L * 1024 * 1024).build() }
        .crossfade(true)
        .build()
