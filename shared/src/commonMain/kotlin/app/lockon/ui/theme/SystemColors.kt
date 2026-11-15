package app.lockon.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.lockon.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The system's accent colour, for Material You colours (UX rule V3a): the wallpaper's on Android 12 and
 * later, the accent colour on Windows, macOS and GNOME. Each platform sets it at launch and when it may
 * have changed; null where the system has none, and Lock On's own Sky and Twilight show instead.
 */
object SystemAccent {
    val color = MutableStateFlow<Color?>(null)
}

/**
 * The seed to colour the app from: the system's accent when Settings' Material You colours are on (they
 * are unless turned off) and the system has a colourful one, otherwise null for Lock On's own palette.
 */
@Composable
fun themeSeed(settings: SettingsRepository?): Color? {
    val accent by SystemAccent.color.collectAsState()
    val on = settings?.systemColors?.collectAsState()?.value ?: true
    return if (on) accent?.takeIf { usableSeed(it) } else null
}

/** An accent too grey to make a teal-like action colour from (macOS's graphite, a grey Windows accent) is passed over. */
fun usableSeed(seed: Color): Boolean = Oklch.of(seed).c >= MinSeedChroma

private const val MinSeedChroma = 0.035f

/**
 * The tones Lock On draws itself, outside Material's colour roles: the sky's three stops, the panels,
 * quiet text, the soft thumb and the gauge's track. Built in for Sky and Twilight, made from the seed for
 * Material You colours, so every surface follows the same palette.
 */
@Immutable
data class LockOnTones(
    val sky: List<Color>,
    val panelFill: Color,
    val panelStrong: Color,
    val line: Color,
    val shade: Color,
    val mutedInk: Color,
    val softThumb: Color,
    val track: Color,
)

internal val DayTones = LockOnTones(
    sky = listOf(Color(0xFFD8E0E7), Color(0xFFDFE7EE), Color(0xFFE6EDF4)),
    panelFill = Color.White.copy(alpha = 0.96f),
    panelStrong = Color.White,
    line = Color(0xFF1C2742).copy(alpha = 0.1f),
    shade = Color(0xFF1C2742).copy(alpha = 0.5f),
    mutedInk = MutedInkDay,
    softThumb = Color.White,
    track = Color(0xFFD3DEEA),
)

internal val TwilightTones = LockOnTones(
    sky = listOf(Color(0xFF1E2331), Color(0xFF1A1F2D), Color(0xFF171B28)),
    panelFill = Color(0xFF232836).copy(alpha = 0.96f),
    panelStrong = Color(0xFF2D3241),
    line = Color(0xFFDCE6FF).copy(alpha = 0.1f),
    shade = Color(0xFF070A16),
    mutedInk = MutedInkTwilight,
    softThumb = Color(0xFF3E4868),
    track = Color(0xFFEEF2FB).copy(alpha = 0.12f),
)

/** The current theme's [LockOnTones], provided by [LockOnTheme]. */
val LocalTones = staticCompositionLocalOf { DayTones }

/**
 * Material You colours from a system accent (UX rule V3a). The seed gives the hue of the whole palette
 * and the strength of the action colour; everything else keeps Sky and Twilight's recipe, so contrast
 * and quiet hold whatever the wallpaper:
 * - the neutrals sit at Sky and Twilight's own lightness steps, tinted toward the seed's hue within V3's
 *   chroma caps (0.03 at twilight, 0.015 by day), so the sky stays quiet under the cards;
 * - primary, the action and selection colour, takes the seed's hue at a lightness that keeps its label
 *   at 4.5:1 or more;
 * - information blue, celebration gold, the error red and the card colours keep their meanings and stay
 *   as they are.
 */
fun systemColorScheme(seed: Color, dark: Boolean, base: ColorScheme): Pair<ColorScheme, LockOnTones> {
    val s = Oklch.of(seed)
    val h = s.h
    // Strong enough to read as an action colour, never louder than a card face.
    val pc = s.c.coerceIn(0.08f, 0.14f)
    fun tone(l: Float, c: Float) = Oklch(l, c, h).toColor()
    val warmth = warmth(h)
    fun neutral(l: Float, c: Float) = tone(l, c * warmth)
    return if (dark) {
        val onPrimary = tone(0.27f, pc * 0.5f)
        val primary = readable(h, pc, from = 0.82f, step = 0.01f, against = onPrimary)
        val background = neutral(0.235f, 0.024f)
        val surface = neutral(0.27f, 0.024f)
        val highest = neutral(0.315f, 0.024f)
        val onSurface = neutral(0.96f, 0.008f)
        val outline = tone(0.74f, 0.03f)
        base.copy(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = tone(0.38f, pc * 0.6f), onPrimaryContainer = tone(0.93f, 0.045f),
            background = background, onBackground = onSurface,
            surface = surface, onSurface = onSurface,
            surfaceVariant = neutral(0.355f, 0.024f), onSurfaceVariant = tone(0.80f, 0.028f),
            surfaceContainerLowest = neutral(0.215f, 0.022f), surfaceContainerLow = neutral(0.25f, 0.024f),
            surfaceContainer = surface, surfaceContainerHigh = surface, surfaceContainerHighest = highest,
            outline = outline, outlineVariant = neutral(0.355f, 0.024f),
            inversePrimary = tone(0.5f, pc), surfaceTint = primary,
        ) to LockOnTones(
            sky = listOf(neutral(0.25f, 0.024f), background, neutral(0.215f, 0.022f)),
            panelFill = surface.copy(alpha = 0.96f),
            panelStrong = highest,
            line = neutral(0.93f, 0.02f).copy(alpha = 0.1f),
            shade = neutral(0.12f, 0.02f),
            mutedInk = outline,
            softThumb = tone(0.40f, 0.045f),
            track = onSurface.copy(alpha = 0.12f),
        )
    } else {
        val primary = readable(h, pc, from = 0.52f, step = -0.01f, against = Color.White)
        val ink = tone(0.27f, 0.04f)
        val surface = neutral(0.985f, 0.005f)
        base.copy(
            primary = primary, onPrimary = Color.White,
            primaryContainer = tone(0.93f, 0.045f), onPrimaryContainer = tone(0.30f, pc * 0.6f),
            background = neutral(0.953f, 0.012f), onBackground = ink,
            surface = surface, onSurface = ink,
            surfaceVariant = neutral(0.935f, 0.01f), onSurfaceVariant = tone(0.46f, 0.04f),
            surfaceContainerLowest = Color.White, surfaceContainerLow = neutral(0.978f, 0.007f),
            surfaceContainer = neutral(0.962f, 0.009f), surfaceContainerHigh = neutral(0.945f, 0.01f),
            surfaceContainerHighest = neutral(0.92f, 0.012f),
            outline = tone(0.63f, 0.04f), outlineVariant = neutral(0.87f, 0.012f),
            inversePrimary = tone(0.82f, pc), surfaceTint = primary,
        ) to LockOnTones(
            sky = listOf(neutral(0.905f, 0.012f), neutral(0.927f, 0.012f), neutral(0.948f, 0.011f)),
            panelFill = Color.White.copy(alpha = 0.96f),
            panelStrong = Color.White,
            line = ink.copy(alpha = 0.1f),
            shade = ink.copy(alpha = 0.5f),
            mutedInk = tone(0.52f, 0.04f),
            softThumb = Color.White,
            track = neutral(0.895f, 0.02f),
        )
    }
}

/**
 * Reds, pinks, oranges and yellows read as far more colourful than blues at the same chroma, and a maroon
 * or olive sky stops being quiet, so warm hues [h] (OKLCH radians) tint at a little over half as much.
 */
private fun warmth(h: Float): Float {
    val degrees = ((h * 180f / PI.toFloat()) + 360f) % 360f
    return if (degrees < 125f || degrees > 330f) 0.55f else 1f
}

/** The seed [LockOnTheme] made the app's colours from, or null when Sky and Twilight's own colours show. */
val LocalThemeSeed = staticCompositionLocalOf<Color?> { null }

/**
 * One of the launch screen's own colours with Material You colours (UX rule V3a): [color] keeps its
 * lightness and its chroma (less for a warm seed, as the neutrals do) and takes the [seed]'s hue, so the
 * launch screen leans toward the accent as much as Sky and Twilight's leans toward violet.
 */
fun towardSeed(color: Color, seed: Color): Color {
    val h = Oklch.of(seed).h
    val c = Oklch.of(color)
    return Oklch(c.l, c.c * warmth(h), h).toColor().copy(alpha = color.alpha)
}

/**
 * The seed's hue at lightness [from], stepped by [step] until text in [against] on it clears 4.5:1, so
 * a pale yellow or a deep blue accent gives a button whose label still reads.
 */
private fun readable(h: Float, c: Float, from: Float, step: Float, against: Color): Color {
    var l = from
    var color = Oklch(l, c, h).toColor()
    while (contrast(color, against) < 4.5f && l in 0.2f..0.98f) {
        l += step
        color = Oklch(l, c, h).toColor()
    }
    return color
}

/** WCAG contrast between two opaque colours. */
fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

/** A colour in OKLCH. [toColor] lowers the chroma until it fits in sRGB, keeping lightness and hue. */
data class Oklch(val l: Float, val c: Float, val h: Float) {
    fun toColor(): Color {
        if (inGamut(l, c, h)) return rgb(l, c, h)
        var lo = 0f
        var hi = c
        repeat(20) {
            val mid = (lo + hi) / 2
            if (inGamut(l, mid, h)) lo = mid else hi = mid
        }
        return rgb(l, lo, h)
    }

    companion object {
        fun of(color: Color): Oklch {
            val r = toLinear(color.red)
            val g = toLinear(color.green)
            val b = toLinear(color.blue)
            val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
            val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
            val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
            val ll = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
            val aa = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
            val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
            return Oklch(ll.toFloat(), sqrt(aa * aa + bb * bb).toFloat(), atan2(bb, aa).toFloat())
        }

        private fun linear(l: Float, c: Float, h: Float): DoubleArray {
            val a = c * cos(h.toDouble())
            val b = c * sin(h.toDouble())
            val l3 = (l + 0.3963377774 * a + 0.2158037573 * b).pow(3)
            val m3 = (l - 0.1055613458 * a - 0.0638541728 * b).pow(3)
            val s3 = (l - 0.0894841775 * a - 1.2914855480 * b).pow(3)
            return doubleArrayOf(
                4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3,
                -1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3,
                -0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3,
            )
        }

        private fun inGamut(l: Float, c: Float, h: Float) = linear(l, c, h).all { it in -0.0001..1.0001 }

        private fun rgb(l: Float, c: Float, h: Float): Color {
            val (r, g, b) = linear(l, c, h).map { fromLinear(it.coerceIn(0.0, 1.0)) }
            return Color(r, g, b)
        }

        private fun toLinear(v: Float): Double = if (v <= 0.04045f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        private fun fromLinear(v: Double): Float = (if (v <= 0.0031308) v * 12.92 else 1.055 * v.pow(1 / 2.4) - 0.055).toFloat()
    }
}
