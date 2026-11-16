package app.lockon

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import app.lockon.ui.theme.Oklch
import app.lockon.ui.theme.contrast
import app.lockon.data.ColorStyle
import app.lockon.ui.theme.DarkColors
import app.lockon.ui.theme.LightColors
import app.lockon.ui.theme.materialTones
import app.lockon.ui.theme.materialYouScheme
import app.lockon.ui.theme.pureBlack
import app.lockon.ui.theme.usableSeed
import app.lockon.ui.theme.ThemeColorPresets
import app.lockon.ui.theme.themeColor
import app.lockon.data.hexRgb
import app.lockon.data.parseHexRgb
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Material You colours stay readable whatever the accent and style, and keep their fixed meanings (UX rules V3, V3a, V6). */
class SystemColorsTest {
    private val darkScheme = DarkColors
    private val lightScheme = LightColors
    private val seeds = (0 until 24).flatMap { step ->
        val h = (step * 15) * PI.toFloat() / 180f
        listOf(Oklch(0.6f, 0.15f, h).toColor(), Oklch(0.8f, 0.17f, h).toColor(), Oklch(0.4f, 0.08f, h).toColor())
    } + listOf(Color(0xFF0078D4), Color(0xFFFFC600), Color(0xFF62BA46), Color(0xFFF74F9E)) +
        // The ready-made theme colours, and a grey one typed in, which Settings takes though an accent that grey is passed over.
        ThemeColorPresets.map { themeColor(it.second) } + listOf(Color(0xFF808080), Color.Black, Color.White)

    /** A theme colour is kept as six hex digits, typed with or without a #. */
    @Test
    fun themeColorsReadAsHex() {
        assertEquals(0x6750A4, parseHexRgb("#6750a4"))
        assertEquals(0x6750A4, parseHexRgb("6750A4"))
        assertEquals("0061A4", hexRgb(0x0061A4))
        for (bad in listOf(null, "", "#6750A", "6750A4F", "#GG50A4")) assertEquals(null, parseHexRgb(bad), "$bad")
    }

    @Test
    fun textAndActionsStayReadable() {
        for (style in ColorStyle.entries) for (seed in seeds) for (dark in listOf(false, true)) for (black in listOf(false, true)) {
            if (black && !dark) continue
            val c = materialYouScheme(seed, dark, style).let { if (black) it.pureBlack() else it }
            val t = materialTones(c, dark)
            val where = "style=$style seed=${seed.value.toString(16)} dark=$dark black=$black"
            assertTrue(contrast(c.primary, c.onPrimary) >= 4.5f, "$where: action label ${contrast(c.primary, c.onPrimary)}")
            assertTrue(contrast(c.onSurface, c.background) >= 4.5f, "$where: text on the ground")
            assertTrue(contrast(c.onSurfaceVariant, c.surface) >= 4.5f, "$where: secondary text on the ground")
            assertTrue(contrast(c.onSurfaceVariant, t.panelFill.compositeOver(c.background)) >= 4.5f, "$where: secondary text on a panel")
            assertTrue(contrast(t.mutedInk, t.panelStrong) >= 4.5f, "$where: quiet text on the raised step ${contrast(t.mutedInk, t.panelStrong)}")
            assertTrue(contrast(c.primary, t.panelFill.compositeOver(c.background)) >= 3f, "$where: action colour on a panel")
            val ink = t.softThumbInk ?: c.primary
            assertTrue(contrast(ink, t.softThumb) >= 4.5f, "$where: chosen option on its thumb ${contrast(ink, t.softThumb)}")
            assertTrue(contrast(c.onPrimaryContainer, c.primaryContainer) >= 4.5f, "$where: container text")
            assertTrue(contrast(c.onSecondaryContainer, c.secondaryContainer) >= 4.5f, "$where: chip text")
        }
    }

    /** Tonal spot, the default, keeps the sky quiet under the cards (UX rules V3, V3a). */
    @Test
    fun theDefaultGroundStaysQuiet() {
        for (style in listOf(ColorStyle.TONAL_SPOT, ColorStyle.NEUTRAL)) for (seed in seeds) for (dark in listOf(false, true)) {
            val c = materialYouScheme(seed, dark, style)
            val t = materialTones(c, dark)
            for (tone in t.sky + listOf(c.background, c.surface)) {
                assertTrue(Oklch.of(tone).c <= 0.035f, "style=$style seed=${seed.value.toString(16)} dark=$dark: ${Oklch.of(tone).c}")
            }
        }
    }

    /** Celebration gold means the same with any accent. */
    @Test
    fun goldStaysGold() {
        for (seed in seeds) for (dark in listOf(false, true)) {
            val own = if (dark) darkScheme else lightScheme
            assertEquals(own.tertiary, materialYouScheme(seed, dark).tertiary)
        }
    }

    /** The system's own scheme is used as it is, unless it is unreadable or another style is chosen. */
    @Test
    fun systemSchemeWinsWhenReadable() {
        val seed = Color(0xFF0078D4)
        val system = materialYouScheme(Color(0xFF62BA46), dark = true)
        assertEquals(system.primary, materialYouScheme(seed, true, system = system).primary)
        assertNotEquals(system.primary, materialYouScheme(seed, true, ColorStyle.VIBRANT, system = system).primary)
        val stale = system.copy(onSurface = system.surface)
        assertEquals(materialYouScheme(seed, true).primary, materialYouScheme(seed, true, system = stale).primary)
        assertEquals(materialYouScheme(seed, true).primary, materialYouScheme(seed, true, system = lightColorScheme()).primary)
    }

    @Test
    fun greyAccentsArePassedOver() {
        assertFalse(usableSeed(Color(0xFF8C8C8C)))
        assertFalse(usableSeed(Color(0xFF6F7377)))
        assertTrue(usableSeed(Color(0xFF0078D4)))
    }

    @Test
    fun oklchRoundTrips() {
        for (seed in seeds) {
            val back = Oklch.of(seed).toColor()
            assertTrue(kotlin.math.abs(back.red - seed.red) < 0.01f && kotlin.math.abs(back.green - seed.green) < 0.01f && kotlin.math.abs(back.blue - seed.blue) < 0.01f)
        }
    }
}
