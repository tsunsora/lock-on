package app.lockon.ui.components

import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.Scale
import coil3.size.pxOrElse
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal actual fun artworkDecoder(): Decoder.Factory? = SmoothArtworkDecoder.Factory()

/**
 * Decodes artwork at the size it's shown, resampled smoothly. Coil's own desktop decoder shrinks
 * images with nearest-neighbour sampling, which drops most source pixels and leaves card text and
 * outlines jagged at grid size, however the result is drawn afterwards.
 */
class SmoothArtworkDecoder(private val source: ImageSource, private val options: Options) : Decoder {
    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
        val image = Image.makeFromEncoded(bytes)
        try {
            val wanted = sizeFor(image.width, image.height)
            val sampled = wanted.first < image.width || wanted.second < image.height
            val bitmap = if (sampled) shrink(image, wanted.first, wanted.second) else redraw(image, image.width, image.height, SamplingMode.DEFAULT)
            return DecodeResult(bitmap.setImmutable().asImage(), isSampled = sampled)
        } finally {
            image.close()
        }
    }

    /** The requested size at the source's shape, never larger than the source. */
    private fun sizeFor(width: Int, height: Int): Pair<Int, Int> {
        val scaleX = options.size.width.pxOrElse { width }.toDouble() / width
        val scaleY = options.size.height.pxOrElse { height }.toDouble() / height
        val scale = (if (options.scale == Scale.FIT) min(scaleX, scaleY) else max(scaleX, scaleY)).coerceAtMost(1.0)
        return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
    }

    /** Halves with box filtering while that stays above the target, then finishes with a Mitchell cubic. */
    private fun shrink(image: Image, width: Int, height: Int): Bitmap {
        var step = image
        while (step.width >= width * 2 && step.height >= height * 2) {
            val half = Image.makeFromBitmap(redraw(step, step.width / 2, step.height / 2, SamplingMode.LINEAR).setImmutable())
            if (step !== image) step.close()
            step = half
        }
        return redraw(step, width, height, SamplingMode.MITCHELL).also { if (step !== image) step.close() }
    }

    private fun redraw(image: Image, width: Int, height: Int, sampling: SamplingMode): Bitmap {
        val bitmap = Bitmap().apply { allocN32Pixels(width, height) }
        val canvas = Canvas(bitmap)
        try {
            val src = Rect.makeWH(image.width.toFloat(), image.height.toFloat())
            canvas.drawImageRect(image, src, Rect.makeWH(width.toFloat(), height.toFloat()), sampling, null, true)
        } finally {
            canvas.close()
        }
        return bitmap
    }

    class Factory : Decoder.Factory {
        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder =
            SmoothArtworkDecoder(result.source, options)
    }
}
