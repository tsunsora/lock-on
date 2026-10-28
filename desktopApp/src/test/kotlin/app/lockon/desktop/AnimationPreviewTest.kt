package app.lockon.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import androidx.room.Room
import app.lockon.AppContainer
import app.lockon.data.db.LockOnDatabase
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.ui.components.DeckGauge
import app.lockon.ui.components.LockIndicator
import app.lockon.ui.decks.SampleHandPanel
import app.lockon.ui.theme.LockOnTheme
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Renders animations frame by frame into build/animation-frames/<name>/ so motion can be reviewed
 * without a device. scripts/frames_to_gif.py turns each folder into a GIF.
 */
class AnimationPreviewTest {
    private val frameNanos = 33_333_333L

    private val container = runBlocking {
        AppContainer.create(LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>()), "Preview")
    }

    private fun record(name: String, width: Int, height: Int, frames: Int, onFrame: (Int) -> Unit = {}, content: @Composable () -> Unit) {
        val dir = File("build/animation-frames/$name").apply { deleteRecursively(); mkdirs() }
        ImageComposeScene(width, height, Density(2f)) {
            LockOnTheme(dark = true) { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() } }
        }.use { scene ->
            for (i in 0 until frames) {
                onFrame(i)
                // Real sleeps let coroutine delays (staggers) advance alongside the frame clock.
                val image = scene.render(i * frameNanos)
                Thread.sleep(33)
                File(dir, "frame-%03d.png".format(i)).writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
        assertTrue(File(dir, "frame-000.png").exists())
    }

    @Test fun lockOnMoment() {
        val main = mutableStateOf(48)
        record("lock-on", 640, 220, frames = 60, onFrame = { i -> main.value = if (i < 12) 48 else if (i < 24) 49 else 50 }) {
            LockOnDemo(main)
        }
    }

    @Test fun segmentSlide() {
        val choice = mutableStateOf(0)
        record("segment-slide", 520, 140, frames = 14, onFrame = { i -> if (i == 2) choice.value = 2 }) {
            app.lockon.ui.components.PillSegmented(listOf("Not noted", "Went first", "Went second"), choice.value, { choice.value = it },
                Modifier.padding(12.dp))
        }
    }

    @Test fun dealAndFlip() {
        val deck = DeckSnapshot(
            "Preview",
            entries = (1..12).map { DeckEntry("SMP-%03d".format(it), 4) } + DeckEntry("SMP-013", 2) + DeckEntry("SMP-019", 10, DeckSection.RESOURCE),
        )
        record("deal-hand", 620, 900, frames = 45) {
            SampleHandPanel(deck, container.catalog.cards.value, Modifier.fillMaxSize())
        }
    }

    @Composable
    private fun LockOnDemo(main: MutableState<Int>) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Blue Red Aggro", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    DeckGauge("Main", main.value, 50)
                    DeckGauge("Resource", 10, 10)
                }
            }
            LockIndicator(locked = main.value == 50, size = 64.dp)
        }
    }
}
