package app.lockon.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.remember
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.lockon.ui.components.SkyBackground
import app.lockon.ui.components.SpotlightGrid
import app.lockon.ui.components.glass
import app.lockon.ui.components.hoverLift
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.theme.LockOnTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Renders a [SpotlightGrid] of glass tiles with the pointer resting between them, in both themes,
 * into build/animation-frames/spotlight-<theme>.png, and prints how long a pointer-move frame takes.
 */
class SpotlightPreviewTest {
    private val tints = listOf(Color(0xFFE5484D), Color(0xFF3E8BFF), Color(0xFF2FB36B), Color(0xFFF2B53A), Color(0xFF9A6BFF), Color(0xFF7A8699))

    private fun render(dark: Boolean, spotlight: Boolean = true) {
        val dir = File("build/animation-frames").apply { mkdirs() }
        ImageComposeScene(900, 600, Density(1.5f)) {
            LockOnTheme(dark = dark) {
                SkyBackground(Modifier.fillMaxSize()) {
                    val grid: @androidx.compose.runtime.Composable (@androidx.compose.runtime.Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) -> Unit =
                        if (spotlight) { c -> SpotlightGrid(Modifier.fillMaxSize(), c) } else { c -> Box(Modifier.fillMaxSize(), content = c) }
                    grid {
                        LazyVerticalGrid(
                            GridCells.Fixed(5), contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(15) { i ->
                                val press = remember { MutableInteractionSource() }
                                Box(
                                    Modifier.aspectRatio(0.72f).hoverLift(press, tints[i % tints.size], LockOnShapes.tile)
                                        .glass(LockOnShapes.tile).clickable(press, null) {},
                                )
                            }
                        }
                    }
                }
            }
        }.use { scene ->
            var t = 0L
            scene.render(t)
            // Enter on the second tile, then drift right toward the gap between tiles two and three.
            val steps = 60
            var worst = 0L
            var total = 0L
            for (i in 0..steps) {
                scene.sendPointerEvent(PointerEventType.Move, Offset(280f + i * 1.2f, 200f))
                t += 16_666_667L
                val start = System.nanoTime()
                scene.render(t)
                val took = System.nanoTime() - start
                if (i > 5) { total += took; worst = maxOf(worst, took) }
                Thread.sleep(16)
            }
            println("spotlight=$spotlight ${if (dark) "twilight" else "day"}: mean ${total / (steps - 5) / 1000} µs, worst ${worst / 1000} µs per pointer-move frame")
            repeat(40) { t += 16_666_667L; scene.render(t); Thread.sleep(16) }
            val image = scene.render(t + 16_666_667L)
            if (spotlight) File(dir, "spotlight-${if (dark) "twilight" else "day"}.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    @Test fun day() = render(dark = false)

    @Test fun twilight() = render(dark = true)

    @Test fun withoutSpotlight() { render(dark = false, spotlight = false); render(dark = false); render(dark = false, spotlight = false); render(dark = false) }
}
