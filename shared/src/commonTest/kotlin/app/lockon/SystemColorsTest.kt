package app.lockon

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import app.lockon.ui.theme.Oklch
import app.lockon.ui.theme.contrast
import app.lockon.ui.theme.systemColorScheme
import app.lockon.ui.theme.towardSeed
import app.lockon.ui.theme.usableSeed
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Material You colours keep Sky and Twilight's contrast and quiet whatever the accent (UX rules V3, V3a, V6). */
class SystemColorsTest {
    private val seeds = (0 until 24).flatMap { step ->
        val h = (step * 15) * PI.toFloat() / 180f
        listOf(Oklch(0.6f, 0.15f, h).toColor(), Oklch(0.8f, 0.17f, h).toColor(), Oklch(0.4f, 0.08f, h).toColor())
    } + listOf(Color(0xFF0078D4), Color(0xFFFFC600), Color(0xFF62BA46), Color(0xFFF74F9E))

    @Test
    fun textAndActionsStayReadable() {
        for (seed in seeds) for (dark in listOf(false, true)) {
            val (c, t) = systemColorScheme(seed, dark, if (dark) darkColorScheme() else lightColorScheme())
            val where = "seed=${seed.value.toString(16)} dark=$dark"
            assertTrue(contrast(c.primary, c.onPrimary) >= 4.5f, "$where: action label ${contrast(c.primary, c.onPrimary)}")
            assertTrue(contrast(c.onSurface, c.background) >= 4.5f, "$where: text on the ground")
            assertTrue(contrast(c.onSurfaceVariant, c.surface) >= 4.5f, "$where: secondary text on a panel")
            assertTrue(contrast(t.mutedInk, t.panelStrong) >= 4.5f, "$where: quiet text on the raised step ${contrast(t.mutedInk, t.panelStrong)}")
            assertTrue(contrast(c.primary, t.panelFill.compositeOver(c.background)) >= 3f, "$where: teal-role control on a panel")
            assertTrue(contrast(c.onPrimaryContainer, c.primaryContainer) >= 4.5f, "$where: container text")
        }
    }

    @Test
    fun theGroundStaysQuiet() {
        for (seed in seeds) for (dark in listOf(false, true)) {
            val (c, t) = systemColorScheme(seed, dark, if (dark) darkColorScheme() else lightColorScheme())
            val cap = if (dark) 0.03f else 0.015f
            for (tone in t.sky + listOf(c.background, c.surface)) {
                assertTrue(Oklch.of(tone).c <= cap, "seed=${seed.value.toString(16)} dark=$dark: ${Oklch.of(tone).c} over $cap")
            }
        }
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

    /** The launch screen leans toward the accent at its own lightness, and without one is exactly as it was. */
    @Test
    fun launchScreenKeepsItsLightness() {
        for (twilight in listOf(false, true)) {
            val own = app.lockon.ui.components.skyStops(twilight, boot = true)
            assertTrue(app.lockon.ui.components.bootSkyStops(twilight, null) == own)
            for (seed in seeds) for (stop in own) {
                val turned = towardSeed(stop, seed)
                assertTrue(kotlin.math.abs(Oklch.of(turned).l - Oklch.of(stop).l) < 0.01f, "$stop toward $seed")
                assertTrue(Oklch.of(turned).c <= Oklch.of(stop).c + 0.005f, "$stop toward $seed")
            }
        }
    }
}
