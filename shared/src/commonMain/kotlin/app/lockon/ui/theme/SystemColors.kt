package app.lockon.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import app.lockon.data.ColorStyle
import app.lockon.data.SettingsRepository
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeNeutral
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The system's colours, for Material You colours (UX rule V3a): its accent (the wallpaper's on Android 12
 * and later, the accent colour on Windows, macOS and GNOME) and, where the system builds them itself,
 * its own Material You schemes. Each platform sets them at launch and when they may have changed.
 */
object SystemAccent {
    /** The accent, or null where the system has none and Lock On's own Sky and Twilight show. */
    val color = MutableStateFlow<Color?>(null)
    /** The system's own light and dark Material You schemes (Android 12 and later), or null to build them from [color]. */
    val schemes = MutableStateFlow<SystemSchemes?>(null)
    /** The system's contrast setting, from -1 to 1 (Android 14 and later); 0 elsewhere. */
    val contrast = MutableStateFlow(0f)
}

/** The system's own Material You schemes, as the rest of the system shows them. */
@Immutable
class SystemSchemes(val light: ColorScheme, val dark: ColorScheme)

/**
 * What the app is coloured from: the accent to build Material colours from (null for Sky and
 * Twilight's own), how to build them, and whether the dark theme's ground is pure black. A [custom]
 * seed is the theme colour picked in Settings, not the system's accent.
 */
@Immutable
data class ThemeLook(
    val seed: Color? = null,
    val style: ColorStyle = ColorStyle.TONAL_SPOT,
    val pureBlack: Boolean = false,
    val custom: Boolean = false,
)

/**
 * The [ThemeLook] Settings ask for: the system's accent when Material You colours are on (they are
 * unless turned off) and the system has a colourful one, otherwise the theme colour picked in Settings,
 * if any; then the colour style, and pure black.
 */
@Composable
fun themeLook(settings: SettingsRepository?): ThemeLook {
    val accent by SystemAccent.color.collectAsState()
    val on = settings?.systemColors?.collectAsState()?.value ?: true
    val custom = settings?.themeColor?.collectAsState()?.value
    val style = settings?.colorStyle?.collectAsState()?.value ?: ColorStyle.TONAL_SPOT
    val black = settings?.pureBlack?.collectAsState()?.value ?: false
    val system = if (on) accent?.takeIf { usableSeed(it) } else null
    return ThemeLook(system ?: custom?.let(::themeColor), style, black, custom = system == null && custom != null)
}

/** An opaque colour from 0xRRGGBB. */
fun themeColor(rgb: Int): Color = Color(0xFF000000.toInt() or rgb)

/**
 * Ready-made theme colours for Settings › Appearance › Theme color, beside Lock On's own: Material's
 * baseline purple and a few of its sample seeds, each far enough apart in hue to look different.
 */
val ThemeColorPresets: List<Pair<String, Int>> = listOf(
    "Purple" to 0x6750A4,
    "Blue" to 0x0061A4,
    "Green" to 0x386A20,
    "Rose" to 0x8C4A60,
    "Red" to 0x9C4146,
    "Gold" to 0x765A00,
)

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
    /** The chosen option's text on [softThumb]; null for the action colour. */
    val softThumbInk: Color? = null,
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
 * Material You colours (UX rule V3a), built as Material 3 builds them: tonal palettes from the [seed]
 * in Material's 2025 colour spec, giving every role, from primary, secondary and their containers to the
 * whole ladder of surface containers. With [ColorStyle.TONAL_SPOT] and the [system]'s own scheme at
 * hand, that one is used, so Lock On matches the rest of the system; it falls back to the built one if
 * the system's fails a contrast check. Celebration gold (tertiary) keeps its meaning and stays put.
 */
fun materialYouScheme(
    seed: Color,
    dark: Boolean,
    style: ColorStyle = ColorStyle.TONAL_SPOT,
    contrast: Float = 0f,
    system: ColorScheme? = null,
): ColorScheme {
    val scheme = system?.takeIf { style == ColorStyle.TONAL_SPOT && legible(it, dark) } ?: builtScheme(seed, dark, style, contrast)
    val own = if (dark) DarkColors else LightColors
    return scheme.copy(
        tertiary = own.tertiary, onTertiary = own.onTertiary,
        tertiaryContainer = own.tertiaryContainer, onTertiaryContainer = own.onTertiaryContainer,
    )
}

/** Material's dynamic scheme for [seed] in [style], as a Compose [ColorScheme]. */
internal fun builtScheme(seed: Color, dark: Boolean, style: ColorStyle, contrast: Float): ColorScheme {
    val source = Hct.fromInt(seed.toArgb())
    val level = contrast.toDouble().coerceIn(-1.0, 1.0)
    val spec = ColorSpec.SpecVersion.SPEC_2025
    val platform = DynamicScheme.Platform.PHONE
    val d = when (style) {
        ColorStyle.TONAL_SPOT -> SchemeTonalSpot(source, dark, level, spec, platform)
        ColorStyle.NEUTRAL -> SchemeNeutral(source, dark, level, spec, platform)
        ColorStyle.VIBRANT -> SchemeVibrant(source, dark, level, spec, platform)
        ColorStyle.EXPRESSIVE -> SchemeExpressive(source, dark, level, spec, platform)
    }
    fun c(argb: Int) = Color(argb)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c(d.primary), onPrimary = c(d.onPrimary),
        primaryContainer = c(d.primaryContainer), onPrimaryContainer = c(d.onPrimaryContainer),
        inversePrimary = c(d.inversePrimary),
        secondary = c(d.secondary), onSecondary = c(d.onSecondary),
        secondaryContainer = c(d.secondaryContainer), onSecondaryContainer = c(d.onSecondaryContainer),
        tertiary = c(d.tertiary), onTertiary = c(d.onTertiary),
        tertiaryContainer = c(d.tertiaryContainer), onTertiaryContainer = c(d.onTertiaryContainer),
        background = c(d.background), onBackground = c(d.onBackground),
        surface = c(d.surface), onSurface = c(d.onSurface),
        surfaceVariant = c(d.surfaceVariant), onSurfaceVariant = c(d.onSurfaceVariant),
        surfaceTint = c(d.surfaceTint),
        inverseSurface = c(d.inverseSurface), inverseOnSurface = c(d.inverseOnSurface),
        error = c(d.error), onError = c(d.onError),
        errorContainer = c(d.errorContainer), onErrorContainer = c(d.onErrorContainer),
        outline = c(d.outline), outlineVariant = c(d.outlineVariant),
        scrim = c(d.scrim),
        surfaceBright = c(d.surfaceBright), surfaceDim = c(d.surfaceDim),
        surfaceContainer = c(d.surfaceContainer), surfaceContainerHigh = c(d.surfaceContainerHigh),
        surfaceContainerHighest = c(d.surfaceContainerHighest), surfaceContainerLow = c(d.surfaceContainerLow),
        surfaceContainerLowest = c(d.surfaceContainerLowest),
    )
}

/**
 * Whether a system's scheme is what it says: dark where asked, with text and action labels at 4.5:1.
 * Some makers' system roles are stale or half filled in.
 */
internal fun legible(c: ColorScheme, dark: Boolean): Boolean =
    (c.surface.luminance() < 0.5f) == dark && c.surface.alpha == 1f && c.primary.alpha == 1f &&
        contrast(c.onSurface, c.surface) >= 4.5f && contrast(c.onBackground, c.background) >= 4.5f &&
        contrast(c.onPrimary, c.primary) >= 4.5f && contrast(c.onSurfaceVariant, c.surfaceContainerHighest) >= 4.5f &&
        contrast(c.onSecondaryContainer, c.secondaryContainer) >= 4.5f

/**
 * Lock On's own tones from a Material scheme (UX rule V3a), so the sky, panels and controls sit on
 * Material's surface ladder: at twilight the sky runs through the lowest steps and panels take the high
 * one; by day the sky takes the high steps under white panels. A chosen option's thumb is the
 * secondary container with its own text colour, as in Material's segmented buttons.
 */
fun materialTones(c: ColorScheme, dark: Boolean): LockOnTones {
    val strong = if (dark) c.surfaceContainerHighest else c.surfaceContainerLowest
    return LockOnTones(
        sky = if (dark) listOf(c.surfaceContainerLow, c.surface, c.surfaceContainerLowest)
        else listOf(c.surfaceContainerHighest, c.surfaceContainerHigh, c.surfaceContainer),
        panelFill = (if (dark) c.surfaceContainerHigh else c.surfaceContainerLowest).copy(alpha = 0.96f),
        panelStrong = strong,
        line = c.onSurface.copy(alpha = 0.1f),
        shade = if (dark) Color.Black else c.onSurface.copy(alpha = 0.5f),
        mutedInk = quietInk(c, strong),
        softThumb = c.secondaryContainer,
        track = if (dark) c.onSurface.copy(alpha = 0.12f) else c.surfaceContainerHighest,
        softThumbInk = c.onSecondaryContainer,
    )
}

/** Quiet text: from the outline toward [ColorScheme.onSurfaceVariant] until it reads at 4.5:1 on [on]. */
private fun quietInk(c: ColorScheme, on: Color): Color {
    for (step in 0..10) {
        val ink = lerp(c.outline, c.onSurfaceVariant, step / 10f)
        if (contrast(ink, on) >= 4.5f) return ink
    }
    return c.onSurfaceVariant
}

/**
 * The dark theme's ground in pure black (UX rule V3a), for OLED screens: the background, surface and
 * lower surface steps go black, while panels keep their raised steps so they still stand off the ground.
 */
fun ColorScheme.pureBlack(): ColorScheme = copy(
    background = Color.Black, surface = Color.Black, surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color.Black,
)

/** The seed [LockOnTheme] made the app's colours from, or null when Sky and Twilight's own colours show. */
val LocalThemeSeed = staticCompositionLocalOf<Color?> { null }

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
