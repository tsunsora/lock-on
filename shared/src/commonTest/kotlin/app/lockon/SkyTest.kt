package app.lockon

import app.lockon.ui.components.paintSky
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class SkyTest {
    @Test
    fun neighbouringPixelsNeverJump() {
        // Dither moves a channel by at most one step either way; anything past a few steps is an edge.
        for (twilight in listOf(false, true)) {
            val w = 400
            val h = 300
            val pixels = paintSky(w, h, twilight)
            var worst = 0
            for (y in 0 until h) for (x in 0 until w) {
                val p = pixels[y * w + x]
                if (x + 1 < w) worst = maxOf(worst, step(p, pixels[y * w + x + 1]))
                if (y + 1 < h) worst = maxOf(worst, step(p, pixels[(y + 1) * w + x]))
            }
            assertTrue(worst <= 4, "twilight=$twilight: neighbouring pixels differ by $worst steps")
        }
    }

    @Test
    fun theSameSizePaintsTheSameSky() {
        assertContentEquals(paintSky(120, 80, twilight = false), paintSky(120, 80, twilight = false))
    }

    private fun step(a: Int, b: Int): Int = (0..16 step 8).maxOf { abs((a shr it and 0xFF) - (b shr it and 0xFF)) }
}
