package app.lockon.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.lockon.model.CardColor
import app.lockon.ui.components.ColorPips
import app.lockon.ui.components.LegalStamp
import app.lockon.ui.components.LocalReduceMotion
import app.lockon.ui.components.ProgressGauge
import app.lockon.ui.theme.LockOnTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The Lock On moment (UX rule V5) frame by frame, in build/animation-frames/legal-moment-<look>/:
 * an editor header whose main deck goes 48, 49, 50 of 50. Twilight and Day, Material You from a pink
 * wallpaper, and with reduced motion. scripts/frames_to_gif.py turns each folder into a GIF.
 */
class LegalMomentPreviewTest {
    private val frameNanos = 33_333_333L

    private fun record(name: String, dark: Boolean, seed: Color? = null, reduce: Boolean = false) {
        val main = mutableStateOf(48)
        val dir = File("build/animation-frames/legal-moment-$name").apply { deleteRecursively(); mkdirs() }
        ImageComposeScene(960, 210, Density(2f)) {
            LockOnTheme(dark = dark, seed = seed) {
                CompositionLocalProvider(LocalReduceMotion provides reduce) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Header(main) }
                }
            }
        }.use { scene ->
            for (i in 0 until 54) {
                main.value = when { i < 6 -> 48; i < 12 -> 49; else -> 50 }
                val image = scene.render(i * frameNanos)
                Thread.sleep(33)
                File(dir, "frame-%03d.png".format(i)).writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
        assertTrue(File(dir, "frame-000.png").exists())
    }

    @Composable
    private fun Header(main: MutableState<Int>) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Blue Red Aggro", style = MaterialTheme.typography.titleLarge)
                ColorPips(listOf(CardColor.BLUE, CardColor.RED), size = 22.dp)
                LegalStamp(main.value == 50)
            }
            ProgressGauge("Main deck", main.value, 50, Modifier.fillMaxWidth(), compact = true, inline = true, numberLast = true)
        }
    }

    @Test fun twilight() = record("twilight", dark = true)
    @Test fun day() = record("day", dark = false)
    @Test fun materialYouDay() = record("material-you-day", dark = false, seed = Color(0xFFD0507A))
    @Test fun materialYouTwilight() = record("material-you-twilight", dark = true, seed = Color(0xFFD0507A))
    @Test fun reducedMotion() = record("reduced-motion", dark = true, reduce = true)
}
