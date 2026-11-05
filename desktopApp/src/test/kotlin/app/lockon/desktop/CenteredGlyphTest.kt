package app.lockon.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.use
import app.lockon.ui.components.CenteredGlyph
import app.lockon.ui.theme.LockOnTheme
import org.jetbrains.skia.Bitmap
import kotlin.test.Test
import kotlin.test.assertTrue

class CenteredGlyphTest {
    private fun ink(letter: String, sizeDp: Int, ratio: Float, density: Float): List<Int> {
        val px = kotlin.math.round(sizeDp * density).toInt()
        val image = ImageComposeScene(px, px, Density(density)) {
            LockOnTheme(dark = true) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    Box(Modifier.size(sizeDp.dp)) { CenteredGlyph(letter, Color.White, (sizeDp * ratio).sp) }
                }
            }
        }.use { it.render() }
        val bitmap = Bitmap.makeFromImage(image)
        var minX = px; var maxX = -1; var minY = px; var maxY = -1
        for (y in 0 until px) for (x in 0 until px) {
            val v = (bitmap.getColor(x, y) shr 8) and 0xFF
            if (v >= 128) { if (x < minX) minX = x; if (x > maxX) maxX = x; if (y < minY) minY = y; if (y > maxY) maxY = y }
        }
        return listOf(minX, px - 1 - maxX, minY, px - 1 - maxY)
    }

    /** At most one pixel's difference between opposite sides: half a pixel off, the best whole-pixel text can do. */
    @Test fun lettersAndDigitsSitInTheMiddleOfTheirDots() {
        val off = mutableListOf<String>()
        fun check(glyphs: String, sizes: List<Int>, ratio: Float) {
            for (density in listOf(1f, 1.25f, 1.5f, 2f)) for (sizeDp in sizes) for (c in glyphs) {
                val (l, r, t, b) = ink(c.toString(), sizeDp, ratio, density)
                if (kotlin.math.abs(l - r) > 1 || kotlin.math.abs(t - b) > 1) off += "$c at ${sizeDp}dp x$density: left $l right $r top $t bottom $b"
            }
        }
        check("BGRWP", listOf(16, 18, 24, 28), 0.56f)
        check("0123456789", listOf(24, 28, 32), 0.46f)
        assertTrue(off.isEmpty(), off.joinToString(separator = "; "))
    }
}
