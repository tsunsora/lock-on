package app.lockon.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.lockon.ui.components.DialogStage
import app.lockon.ui.components.DialogStageHost
import app.lockon.ui.components.LocalDialogStage
import app.lockon.ui.components.takeThemeFrame
import app.lockon.ui.components.takesThemeFrame
import app.lockon.ui.components.turnTheme
import app.lockon.ui.theme.LockOnTheme
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A change of theme fades the window's old picture away over the new one. Nothing may move while it
 * does: the picture has to lie exactly over the app it was taken of, wherever the app sits in the window.
 * On Windows the app sits under Lock On's own title bar, and the picture was once drawn that much too high.
 */
@OptIn(ExperimentalComposeUiApi::class)
class ThemeFadeTest {
    private val frameNanos = 16_666_667L
    private val marker = Color(0xFFFF0000)

    /** The rows and columns of [image] that the red marker covers: its top, bottom, left and right, or null if it shows nowhere. */
    private fun markerBounds(image: Image): List<Int>? {
        val bitmap = Bitmap.makeFromImage(image)
        var top = Int.MAX_VALUE; var bottom = -1; var left = Int.MAX_VALUE; var right = -1
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            val c = bitmap.getColor(x, y)
            val r = c shr 16 and 0xFF; val g = c shr 8 and 0xFF; val b = c and 0xFF
            // Red through and through: a marker drawn out of place blends with what is under it and shows as a paler or darker red.
            if (r > 150 && g < 110 && b < 110 && r - maxOf(g, b) > 60) {
                top = minOf(top, y); bottom = maxOf(bottom, y); left = minOf(left, x); right = maxOf(right, x)
            }
        }
        return if (bottom < 0) null else listOf(top, bottom, left, right)
    }

    @Test
    fun nothingMovesWhileTheThemeChanges() {
        for (toDark in listOf(true, false)) {
            val stage = DialogStage()
            val wanted = mutableStateOf(!toDark)
            val shown = mutableStateOf(!toDark)
            val seen = mutableListOf<List<Int>?>()
            ImageComposeScene(600, 400, Density(2f)) {
                CompositionLocalProvider(LocalDialogStage provides stage) {
                    LaunchedEffect(wanted.value) {
                        if (wanted.value == shown.value) return@LaunchedEffect
                        stage.takeThemeFrame()
                        stage.turnTheme { shown.value = wanted.value }
                    }
                    // As in the desktop window: a title bar's strip, and the app beneath it.
                    Column(Modifier.fillMaxSize()) {
                        Spacer(Modifier.fillMaxWidth().height(32.dp).background(Color.Gray))
                        Box(Modifier.fillMaxWidth().weight(1f)) {
                            LockOnTheme(dark = shown.value) {
                                Box(Modifier.fillMaxSize()) {
                                    Surface(Modifier.fillMaxSize().takesThemeFrame(stage), color = MaterialTheme.colorScheme.background) {
                                        Box(Modifier.fillMaxSize()) { Box(Modifier.offset(40.dp, 50.dp).size(60.dp, 30.dp).background(marker)) }
                                    }
                                    DialogStageHost(stage)
                                }
                            }
                        }
                    }
                }
            }.use { scene ->
                for (i in 0 until 60) {
                    if (i == 4) wanted.value = toDark
                    seen += markerBounds(scene.render(i * frameNanos))
                    Thread.sleep(16)
                }
            }
            assertEquals(toDark, shown.value, "the theme should have changed")
            // 2 px to the dp: the marker is 82 dp down (32 for the strip and 50 more) and 40 in.
            val resting = listOf(164, 223, 80, 199)
            assertEquals(resting, seen.first(), "the marker before the change")
            val moved = seen.withIndex().filter { it.value != resting }
            assertTrue(moved.isEmpty(), "to ${if (toDark) "dark" else "light"}: the marker left its place in frames ${moved.map { "${it.index}: ${it.value}" }}")
        }
    }
}
