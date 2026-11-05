package app.lockon.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.lockon.ui.components.CopyPips
import app.lockon.ui.components.DialogStage
import app.lockon.ui.components.DialogStageHost
import app.lockon.ui.components.FadeSwap
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.GameDialog
import app.lockon.ui.components.LocalDialogStage
import app.lockon.ui.components.Reveal
import app.lockon.ui.components.selectionEdge
import app.lockon.ui.components.takeThemeFrame
import app.lockon.ui.components.takesThemeFrame
import app.lockon.ui.theme.LockOnTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Records the motion added after the council's audit into build/animation-frames/audit-<name>/, and
 * checks that a closed dialog really does leave the stage: the picture it leaves behind must go away.
 */
@OptIn(ExperimentalComposeUiApi::class)
class MotionAuditPreviewTest {
    private val frameNanos = 16_666_667L

    private fun record(name: String, frames: Int, step: (Int) -> Unit, content: @Composable () -> Unit) {
        val dir = File("build/animation-frames/audit-$name").apply { deleteRecursively(); mkdirs() }
        ImageComposeScene(700, 500, Density(2f)) {
            LockOnTheme(dark = false) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
            }
        }.use { scene ->
            for (i in 0 until frames) {
                step(i)
                val image = scene.render(i * frameNanos)
                Thread.sleep(16)
                File(dir, "frame-%03d.png".format(i)).writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
    }

    @Test
    fun dialogFadesWithItsDim() {
        val open = mutableStateOf(false)
        val stage = DialogStage()
        record("dialog", 60, { i -> if (i == 2) open.value = true; if (i == 30) open.value = false }) {
            CompositionLocalProvider(LocalDialogStage provides stage) {
                Box(Modifier.fillMaxSize()) {
                    Text("The screen under the dialog", Modifier.align(Alignment.Center))
                    if (open.value) GameDialog(
                        onDismissRequest = {}, title = "Delete deck?",
                        confirmButton = { GameButton("Delete", onClick = {}) },
                        dismissButton = { GameButton("Cancel", onClick = {}, style = GameButtonStyle.SECONDARY) },
                        text = { Text("This removes the deck from every device.") },
                    )
                    DialogStageHost(stage)
                }
            }
        }
        assertEquals(0, stageOpen(stage), "the dim should have let go of the closed dialog")
        assertTrue(stageGhosts(stage) == 0, "the closed dialog's picture should be gone")
    }

    @Test
    fun tileStates() {
        var count by mutableIntStateOf(0)
        val selected = mutableStateOf(false)
        record("tile", 70, { i ->
            if (i == 4) { count = 1; selected.value = true }
            if (i == 24) count = 3
            if (i == 44) { count = 0; selected.value = false }
        }) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                val shape = RoundedCornerShape(12.dp)
                Box(Modifier.size(120.dp, 160.dp).selectionEdge(shape, MaterialTheme.colorScheme.primary, selected.value).background(MaterialTheme.colorScheme.surfaceVariant, shape))
                CopyPips(count, 3, pip = 10.dp)
            }
        }
    }

    @Test
    fun buttonWakesUp() {
        val enabled = mutableStateOf(false)
        record("button", 40, { i -> if (i == 4) enabled.value = true; if (i == 24) enabled.value = false }) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { GameButton("Apply", onClick = {}, enabled = enabled.value) }
        }
    }

    @Test
    fun revealAndSwap() {
        val shown = mutableStateOf(false)
        val state = mutableStateOf("Waiting")
        record("reveal", 50, { i ->
            if (i == 4) { shown.value = true; state.value = "Sending" }
            if (i == 28) { shown.value = false; state.value = "Done" }
        }) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Above")
                Reveal(shown.value) { Text("A problem line that opens and closes") }
                FadeSwap(state.value) { Text(it) }
                Text("Below")
            }
        }
    }

    /** The same steps as the app's root: the frame is taken in the old theme, then the theme changes under it. */
    @Test
    fun themeFades() {
        val stage = DialogStage()
        val wanted = mutableStateOf(false)
        val shown = mutableStateOf(false)
        val dir = File("build/animation-frames/audit-theme").apply { deleteRecursively(); mkdirs() }
        ImageComposeScene(700, 500, Density(2f)) {
            CompositionLocalProvider(LocalDialogStage provides stage) {
                androidx.compose.runtime.LaunchedEffect(wanted.value) {
                    if (wanted.value == shown.value) return@LaunchedEffect
                    stage.takeThemeFrame()
                    shown.value = wanted.value
                }
                LockOnTheme(dark = shown.value) {
                    Box(Modifier.fillMaxSize()) {
                        Surface(Modifier.fillMaxSize().takesThemeFrame(stage), color = MaterialTheme.colorScheme.background) {
                            Box(Modifier.fillMaxSize()) {
                                Box(Modifier.align(Alignment.Center)) { GameButton("Apply", onClick = {}) }
                            }
                        }
                        DialogStageHost(stage)
                    }
                }
            }
        }.use { scene ->
            for (i in 0 until 50) {
                if (i == 4) wanted.value = true
                val image = scene.render(i * frameNanos)
                Thread.sleep(16)
                File(dir, "frame-%03d.png".format(i)).writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
        assertEquals(true, shown.value, "the theme should have changed")
        assertEquals(0, stageGhosts(stage), "the old theme's frame should be gone")
    }

    private fun stageOpen(stage: DialogStage): Int = stage.javaClass.methods.first { it.name.startsWith("getOpen") }.invoke(stage) as Int

    private fun stageGhosts(stage: DialogStage): Int =
        (stage.javaClass.methods.first { it.name.startsWith("getGhosts") }.invoke(stage) as List<*>).size
}
