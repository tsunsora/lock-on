package app.lockon.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.GameMenuItem
import app.lockon.ui.components.GlassIconButton
import app.lockon.ui.components.PillChip
import app.lockon.ui.components.PillSwitch
import app.lockon.ui.theme.LockOnTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Records each kind of button as the pointer hovers it, presses, releases and leaves, into
 * build/animation-frames/button-<name>/, so press and hover feedback can be checked frame by frame.
 */
@OptIn(ExperimentalComposeUiApi::class)
class ButtonMotionPreviewTest {
    private val frameNanos = 16_666_667L

    private fun record(name: String, dark: Boolean, content: @Composable () -> Unit) {
        val dir = File("build/animation-frames/button-$name-${if (dark) "dark" else "light"}").apply { deleteRecursively(); mkdirs() }
        val w = 400; val h = 140
        val center = Offset(w / 2f, h / 2f)
        ImageComposeScene(w, h, Density(2f)) {
            LockOnTheme(dark = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
                }
            }
        }.use { scene ->
            for (i in 0 until 48) {
                when (i) {
                    2 -> scene.sendPointerEvent(PointerEventType.Enter, center)
                    3 -> scene.sendPointerEvent(PointerEventType.Move, center)
                    16 -> scene.sendPointerEvent(PointerEventType.Press, center, button = PointerButton.Primary)
                    // A quick click: the button is down for about 80 ms.
                    21 -> scene.sendPointerEvent(PointerEventType.Release, center, button = PointerButton.Primary)
                    34 -> scene.sendPointerEvent(PointerEventType.Exit, Offset(-10f, -10f))
                }
                val image = scene.render(i * frameNanos)
                Thread.sleep(16)
                File(dir, "frame-%03d.png".format(i)).writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
    }

    private fun both(name: String, content: @Composable () -> Unit) {
        record(name, dark = false, content); record(name, dark = true, content)
    }

    @Test fun primary() = both("primary") { GameButton("New deck", onClick = {}, icon = app.lockon.ui.components.GameIcons.Deck) }
    @Test fun primaryCompact() = both("primary-compact") { GameButton("New deck", onClick = {}, icon = app.lockon.ui.components.GameIcons.Deck, compact = true) }
    @Test fun secondary() = both("secondary") { GameButton("Import", onClick = {}, style = GameButtonStyle.SECONDARY) }
    @Test fun ghost() = both("ghost") { GameButton("Cancel", onClick = {}, style = GameButtonStyle.GHOST) }
    @Test fun glassIcon() = both("glass-icon") { GlassIconButton(app.lockon.ui.components.GameIcons.Deck, "More", onClick = {}) }
    @Test fun materialIcon() = both("material-icon") { IconButton(onClick = {}) { Icon(app.lockon.ui.components.GameIcons.Deck, "Close") } }
    @Test fun chip() {
        val on = mutableStateOf(false)
        both("chip") { PillChip(on.value, onClick = { on.value = !on.value }, label = { Text("Unit") }) }
    }
    @Test fun switch() {
        val on = mutableStateOf(false)
        both("switch") { PillSwitch(on.value, { on.value = it }) }
    }
    @Test fun menuItem() = both("menu-item") { Box(Modifier.fillMaxSize(0.8f)) { GameMenuItem("Duplicate", onClick = {}) } }

    @Test fun dialog() {
        val open = mutableStateOf(false)
        val dir = File("build/animation-frames/dialog-open").apply { deleteRecursively(); mkdirs() }
        ImageComposeScene(700, 500, Density(2f)) {
            LockOnTheme(dark = false) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (open.value) app.lockon.ui.components.GameDialog(
                        onDismissRequest = {}, title = "New deck",
                        confirmButton = { GameButton("Create", onClick = {}) },
                        dismissButton = { GameButton("Cancel", onClick = {}, style = GameButtonStyle.GHOST) },
                        text = { Text("Name it, or import a deck list.") },
                    )
                }
            }
        }.use { scene ->
            for (i in 0 until 20) {
                if (i == 2) open.value = true
                val image = scene.render(i * frameNanos)
                Thread.sleep(16)
                File(dir, "frame-%03d.png".format(i)).writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
    }
}
