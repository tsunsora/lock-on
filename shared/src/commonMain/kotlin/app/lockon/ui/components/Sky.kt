package app.lockon.ui.components

import androidx.compose.ui.graphics.Color
import app.lockon.ui.theme.SuitColors
import kotlin.math.abs
import kotlin.math.max

// The smooth parts of the sky, painted pixel by pixel instead of with gradient brushes. A gentle
// gradient across a whole window changes by only a few dozen 8-bit steps, so brushes show it as
// visible bands, and a glow that fades linearly ends in a bright rim. Here every falloff eases to
// zero at both ends, and each pixel gets a touch of triangular noise before it is rounded, which
// dissolves the steps. The noise is a hash of the pixel's place, so a size always paints the same.

/** A soft round glow: centre as a fraction of the width and height, radius of the longer side. */
internal class SkyGlow(val x: Float, val y: Float, val radius: Float, val color: Color, val alpha: Float)

/** Where the bokeh float, shared by the painted twilight glows and the drawn day discs. */
internal val SkyBokeh = listOf(
    SkyGlow(0.08f, 0.18f, 0.05f, Color.White, 0f), SkyGlow(0.22f, 0.62f, 0.03f, Color.White, 0f),
    SkyGlow(0.47f, 0.3f, 0.025f, Color.White, 0f), SkyGlow(0.71f, 0.72f, 0.045f, Color.White, 0f),
    SkyGlow(0.9f, 0.4f, 0.03f, Color.White, 0f), SkyGlow(0.35f, 0.9f, 0.04f, Color.White, 0f),
    SkyGlow(0.6f, 0.12f, 0.02f, Color.White, 0f), SkyGlow(0.95f, 0.88f, 0.035f, Color.White, 0f),
)

internal val DaySky = listOf(Color(0xFFB7DEF5), Color(0xFFDDEFFA), Color(0xFFF5FAFE))
internal val TwilightSky = listOf(Color(0xFF26346A), Color(0xFF19213F), Color(0xFF11172E))

private val DayGlows = listOf(
    SkyGlow(0.86f, 0.02f, 0.5f, Color(0xFF7FE3E8), 0.45f),
    SkyGlow(0.04f, 0.96f, 0.45f, SuitColors.blush, 0.35f),
    SkyGlow(0.62f, 1.05f, 0.4f, Gold, 0.18f),
)

// At twilight the bokeh are faint glows; rings would read as holes in the night sky.
private val TwilightGlows = listOf(
    SkyGlow(0.86f, 0.02f, 0.5f, Color(0xFF3FC6D4), 0.22f),
    SkyGlow(0.06f, 0.95f, 0.5f, Color(0xFF8E6BE8), 0.2f),
) + SkyBokeh.map { SkyGlow(it.x, it.y, it.radius, Color.White, 0.05f) }

/**
 * The sky's gradient, glows and diagonal beam of light as opaque 0xAARRGGBB pixels, row by row.
 * The day's bokeh discs are crisp shapes and stay with the drawing code.
 */
internal fun paintSky(width: Int, height: Int, twilight: Boolean): IntArray {
    val w = width.toFloat()
    val h = height.toFloat()
    val m = max(w, h)
    val stops = if (twilight) TwilightSky else DaySky
    val glows = if (twilight) TwilightGlows else DayGlows
    val n = glows.size
    val gx = FloatArray(n) { glows[it].x * w }
    val gy = FloatArray(n) { glows[it].y * h }
    val gr = FloatArray(n) { glows[it].radius * m }
    val inv = FloatArray(n) { 1f / (gr[it] * gr[it]) }
    val dy2 = FloatArray(n)
    val beamPeak = if (twilight) 0.05f else 0.3f
    val out = IntArray(width * height)
    for (y in 0 until height) {
        val py = y + 0.5f
        val t = py / h
        // A curve through the three stops rather than two straight segments, so there's no crease.
        val baseR = through(stops[0].red, stops[1].red, stops[2].red, t)
        val baseG = through(stops[0].green, stops[1].green, stops[2].green, t)
        val baseB = through(stops[0].blue, stops[1].blue, stops[2].blue, t)
        for (i in 0 until n) { val d = py - gy[i]; dy2[i] = if (abs(d) < gr[i]) d * d else -1f }
        // The beam slants from the top right towards the bottom left, widening as it falls, and is
        // brightest halfway down.
        val beamX = w * (0.58f - 0.32f * t)
        val beamHalf = w * (0.08f + 0.06f * t)
        val beamAlpha = beamPeak * (0.35f + 2.6f * t * (1f - t))
        val row = y * width
        for (x in 0 until width) {
            val px = x + 0.5f
            var r = baseR; var g = baseG; var b = baseB
            for (i in 0 until n) {
                if (dy2[i] < 0f) continue
                val dx = px - gx[i]
                val d2 = (dx * dx + dy2[i]) * inv[i]
                if (d2 < 1f) {
                    val f = 1f - d2
                    val a = glows[i].alpha * f * f
                    val c = glows[i].color
                    r += (c.red - r) * a; g += (c.green - g) * a; b += (c.blue - b) * a
                }
            }
            val bt = (px - beamX) / beamHalf
            if (bt > -1f && bt < 1f) {
                val f = 1f - bt * bt
                val a = beamAlpha * f * f
                r += (1f - r) * a; g += (1f - g) * a; b += (1f - b) * a
            }
            val noise = dither(x, y)
            out[row + x] = (0xFF shl 24) or (quantize(r, noise) shl 16) or (quantize(g, noise) shl 8) or quantize(b, noise)
        }
    }
    return out
}

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
