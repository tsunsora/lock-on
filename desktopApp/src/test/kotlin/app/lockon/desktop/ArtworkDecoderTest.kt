package app.lockon.desktop

import app.lockon.ui.components.SizedArtworkKeyer
import app.lockon.ui.components.SmoothArtworkDecoder
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.decode.SkiaImageDecoder
import coil3.fetch.SourceFetchResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArtworkDecoderTest {
    private val context = PlatformContext.INSTANCE
    private val loader = ImageLoader(context)

    /** A 600Ã—838 card-shaped image with fine one-pixel stripes, like small card text. */
    private fun cardFile(): File {
        val bitmap = Bitmap().apply { allocN32Pixels(600, 838) }
        Canvas(bitmap).apply {
            clear(0xFFFFFFFF.toInt())
            val ink = Paint().apply { color = 0xFF000000.toInt() }
            for (x in 0 until 600 step 3) drawRect(org.jetbrains.skia.Rect.makeXYWH(x.toFloat(), 0f, 1f, 838f), ink)
        }
        val png = Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)!!.bytes
        return File.createTempFile("card", ".png").apply { writeBytes(png); deleteOnExit() }
    }

    private fun decode(file: File, width: Int, height: Int, smooth: Boolean): Bitmap = runBlocking {
        val options = Options(context, size = Size(width, height), scale = Scale.FIT)
        val result = SourceFetchResult(ImageSource(file.toOkioPath(), FileSystem.SYSTEM), null, DataSource.DISK)
        val decoder = if (smooth) SmoothArtworkDecoder.Factory().create(result, options, loader)
        else SkiaImageDecoder.Factory().create(result, options, loader)!!
        (decoder.decode()!!.image as BitmapImage).bitmap
    }

    /** The share of pixels left pure black or pure white, i.e. not blended with their neighbours. */
    private fun unblended(bitmap: Bitmap): Double {
        var pure = 0
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            val grey = bitmap.getColor(x, y) and 0xFF
            if (grey < 8 || grey > 247) pure++
        }
        return pure.toDouble() / (bitmap.width * bitmap.height)
    }

    @Test
    fun decodesAtTheShownSizeKeepingTheCardShape() {
        val bitmap = decode(cardFile(), 200, 400, smooth = true)
        assertEquals(200, bitmap.width)
        assertEquals(279, bitmap.height)
    }

    @Test
    fun neverEnlargesPastTheSource() {
        val bitmap = decode(cardFile(), 1200, 1676, smooth = true)
        assertEquals(600, bitmap.width)
        assertEquals(838, bitmap.height)
    }

    @Test
    fun shrinkingAveragesFineDetailInsteadOfDroppingPixels() {
        // At a third of the size each pixel should blend black and white stripes into grey;
        // nearest-neighbour copies single source pixels, so they stay pure black or white.
        val file = cardFile()
        val smooth = unblended(decode(file, 200, 279, smooth = true))
        assertTrue(smooth < 0.05, "smooth decoder left $smooth unblended")
        val coil = unblended(decode(file, 200, 279, smooth = false))
        assertTrue(coil > 0.5, "Coil's decoder now blends ($coil unblended); the smooth decoder may no longer be needed")
    }

    @Test
    fun aSmallCardGetsItsOwnDecodeAfterALargeOne() = runBlocking {
        // The hover preview decodes a card large; grid copies of it must not reuse that bitmap
        // and shrink it while drawing, which is what made small cards look jagged. AsyncImage asks
        // for inexact sizes, which is what lets Coil reuse a larger bitmap.
        val sized = ImageLoader.Builder(context).components { add(SizedArtworkKeyer); add(SmoothArtworkDecoder.Factory()) }.build()
        val uri = cardFile().toPath().toUri().toString()
        suspend fun width(w: Int, h: Int): Int {
            val result = sized.execute(ImageRequest.Builder(context).data(uri).size(w, h).scale(Scale.FIT).precision(Precision.INEXACT).build())
            return ((result as SuccessResult).image as BitmapImage).bitmap.width
        }
        assertEquals(600, width(600, 838))
        assertEquals(200, width(200, 279))
        assertEquals(600, width(600, 838))
    }

    /** Writes build/artwork-decode/{coil,smooth}-N.png from real saved artwork, when there is some. */
    @Test
    fun compareRealArtwork() {
        val dir = File(System.getenv("APPDATA") ?: return, "LockOn/artwork")
        val files = dir.listFiles { f -> !f.name.endsWith(".part") }?.take(3) ?: return
        val out = File("build/artwork-decode").apply { mkdirs() }
        files.forEachIndexed { i, f ->
            for (smooth in listOf(false, true)) {
                val bitmap = decode(f, 240, 335, smooth)
                File(out, "${if (smooth) "smooth" else "coil"}-$i.png").writeBytes(Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
    }
}
