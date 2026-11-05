package app.lockon.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import kotlin.test.Test
import kotlin.test.assertTrue

class GameButtonPressTest {
    /**
     * A click whose button then loses the mouse straight away, as when it opens a menu over itself.
     * The button has to come back up: the hover ending must not cancel the wait that releases it.
     */
    @Test fun buttonComesBackUpWhenHoverEndsRightAfterAClick() {
        ImageComposeScene(320, 120, Density(1f)) {
            MaterialTheme {
                Box(Modifier.fillMaxSize().background(Color(0xFF15193D)), contentAlignment = Alignment.Center) {
                    GameButton("Last edited", onClick = {}, style = GameButtonStyle.SECONDARY, compact = true)
                }
            }
        }.use { scene ->
            var time = 0L
            var image: Image = scene.render()
            fun frames(n: Int) = repeat(n) { Thread.sleep(30); time += 30_000_000L; image = scene.render(time) }
            val centre = Offset(160f, 60f)
            val away = Offset(5f, 5f)
            frames(5)
            val resting = pixels(image)

            scene.sendPointerEvent(PointerEventType.Move, centre, type = PointerType.Mouse)
            frames(10)
            scene.sendPointerEvent(PointerEventType.Press, centre, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            frames(1)
            scene.sendPointerEvent(PointerEventType.Release, centre, type = PointerType.Mouse, button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Move, away, type = PointerType.Mouse)
            frames(40)

            val settled = pixels(image)
            val changed = resting.indices.count { resting[it] != settled[it] }
            assertTrue(changed == 0, "The button should look as it did before the click, but $changed pixels differ")
        }
    }

    private fun pixels(image: Image): IntArray {
        val bitmap = Bitmap.makeFromImage(image)
        return IntArray(image.width * image.height) { bitmap.getColor(it % image.width, it / image.width) }
    }
}
