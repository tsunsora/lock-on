package app.lockon.ui.components

import androidx.compose.ui.graphics.Color

// The sky is one quiet gradient, painted pixel by pixel instead of with a gradient brush. A gentle
// gradient across a whole window changes by only a few dozen 8-bit steps, so a brush shows it as
// visible bands. Here each pixel gets a touch of triangular noise before it is rounded, which
// dissolves the steps. The noise is a hash of the pixel's place, so a size always paints the same.

// Cards lie directly on the sky, so it stays quiet: little colour, and a brightness well away from the
// card faces', lighter by day and darker at twilight, so the cards are the most colourful thing on screen.
internal val DaySky = listOf(Color(0xFFCFE2F0), Color(0xFFDCE8F2), Color(0xFFE6EDF4))
internal val TwilightSky = listOf(Color(0xFF141A3C), Color(0xFF181E44), Color(0xFF1E214A))

// The launch screen's sky: the app's own, leaning violet, so it fades into the app as a slight turn of hue.
internal val DayBootSky = listOf(Color(0xFFC9B9F7), Color(0xFFD5C9F9), Color(0xFFE1D9FB))
internal val TwilightBootSky = listOf(Color(0xFF181546), Color(0xFF1C1850), Color(0xFF221D5A))

/**
 * The sky's gradient as opaque 0xAARRGGBB pixels, row by row: the theme's three tones from top to
 * bottom and nothing else. No glow, beam or shape sits in it (UX rule V7). With [boot] it paints the
 * launch screen's sky.
 */
internal fun paintSky(width: Int, height: Int, twilight: Boolean, boot: Boolean = false): IntArray {
    val h = height.toFloat()
    val stops = skyStops(twilight, boot)
    val out = IntArray(width * height)
    for (y in 0 until height) {
        val t = (y + 0.5f) / h
        // A curve through the three stops rather than two straight segments, so there's no crease.
        val r = through(stops[0].red, stops[1].red, stops[2].red, t)
        val g = through(stops[0].green, stops[1].green, stops[2].green, t)
        val b = through(stops[0].blue, stops[1].blue, stops[2].blue, t)
        val row = y * width
        for (x in 0 until width) {
            val noise = dither(x, y)
            out[row + x] = (0xFF shl 24) or (quantize(r, noise) shl 16) or (quantize(g, noise) shl 8) or quantize(b, noise)
        }
    }
    return out
}

/** The top, middle and bottom of a sky. */
internal fun skyStops(twilight: Boolean, boot: Boolean): List<Color> =
    if (boot) (if (twilight) TwilightBootSky else DayBootSky) else if (twilight) TwilightSky else DaySky

/** The quadratic through [a] at 0, [b] at ½ and [c] at 1. */
private fun through(a: Float, b: Float, c: Float, t: Float): Float =
    a + (4 * b - 3 * a - c) * t + (2 * a + 2 * c - 4 * b) * t * t

/** Triangular noise in (-1, 1), from a hash of the pixel's place. */
private fun dither(x: Int, y: Int): Float {
    var hash = x * 374761393 + y * 668265263
    hash = (hash xor (hash ushr 13)) * 1274126177
    hash = hash xor (hash ushr 16)
    return ((hash and 0xFFFF) - ((hash ushr 16) and 0xFFFF)) / 65536f
}

private fun quantize(channel: Float, noise: Float): Int = (channel * 255f + 0.5f + noise).toInt().coerceIn(0, 255)
