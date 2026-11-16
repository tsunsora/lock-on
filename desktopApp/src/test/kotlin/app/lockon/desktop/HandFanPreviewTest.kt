package app.lockon.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import androidx.room.Room
import app.lockon.AppContainer
import app.lockon.data.db.LockOnDatabase
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.ui.components.LocalNavClearance
import app.lockon.ui.components.SkyBackground
import app.lockon.ui.decks.SampleHandPanel
import app.lockon.ui.theme.LockOnTheme
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Records a mouse sweeping across the Test tab's fan and a finger sliding along it, frame by frame,
 * into build/animation-frames/<name>/ (scripts/frames_to_gif.py makes GIFs of them), so picking a
 * card from the fan can be reviewed without a device.
 */
class HandFanPreviewTest {
    private val frameNanos = 33_333_333L
    private val container = runBlocking {
        AppContainer.create(LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>()), "Preview")
    }
    private val deck = DeckSnapshot(
        "Preview",
        entries = (1..12).map { DeckEntry("SMP-%03d".format(it), 4) } + DeckEntry("SMP-013", 2) + DeckEntry("SMP-019", 10, DeckSection.RESOURCE),
    )

    /** One step of a recording: what the pointer does before the frame is drawn. */
    private class Step(val type: PointerEventType?, val at: Offset, val pointer: PointerType)

    private fun record(name: String, width: Int, height: Int, phone: Boolean, before: List<Offset>, steps: List<Step>) {
        val dir = File("build/animation-frames/$name").apply { deleteRecursively(); mkdirs() }
        ImageComposeScene(width, height, Density(1.5f)) {
            LockOnTheme(dark = true) {
                SkyBackground(Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalNavClearance provides if (phone) 84.dp else 0.dp) {
                        SampleHandPanel(deck, container.catalog.cards.value, Modifier.fillMaxSize())
                    }
                }
            }
        }.use { scene ->
            var time = 0L
            fun frame(): org.jetbrains.skia.Image { Thread.sleep(33); time += frameNanos; return scene.render(time) }
            // Let the deal finish, then press any buttons (such as Keep, then Draw) to set up the hand.
            repeat(30) { frame() }
            for (tap in before) {
                scene.sendPointerEvent(PointerEventType.Press, tap, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release, tap, type = PointerType.Mouse, button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Exit, Offset(-1f, -1f), type = PointerType.Mouse)
                repeat(20) { frame() }
            }
            steps.forEachIndexed { i, step ->
                if (step.type != null) {
                    // A finger only moves while it is down; the mouse moves with no button held.
                    val pressed = step.type == PointerEventType.Press || (step.pointer == PointerType.Touch && step.type == PointerEventType.Move)
                    scene.sendPointerEvent(
                        step.type, step.at, type = step.pointer,
                        buttons = if (pressed) PointerButtons(isPrimaryPressed = true) else PointerButtons(),
                        button = if (step.type == PointerEventType.Press || step.type == PointerEventType.Release) PointerButton.Primary else null,
                    )
                }
                File(dir, "frame-%03d.png".format(i)).writeBytes(frame().encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
        assertTrue(File(dir, "frame-000.png").exists())
    }

    /** A sweep from [from] to [to] over [frames] frames. */
    private fun sweep(from: Offset, to: Offset, frames: Int, type: PointerEventType, pointer: PointerType) =
        (0 until frames).map { Step(type, from + (to - from) * (it / (frames - 1f)), pointer) }

    private fun hold(at: Offset, frames: Int, pointer: PointerType) = List(frames) { Step(null, at, pointer) }

    private fun mouseSweep(y: Float, left: Float, right: Float): List<Step> {
        val a = Offset(left, y)
        val b = Offset(right, y)
        return hold(a, 4, PointerType.Mouse) +
            sweep(a, b, 50, PointerEventType.Move, PointerType.Mouse) +
            sweep(b, a, 50, PointerEventType.Move, PointerType.Mouse) +
            listOf(Step(PointerEventType.Exit, Offset(-1f, -1f), PointerType.Mouse)) + hold(a, 10, PointerType.Mouse)
    }

    /** A finger lands on the left of the fan, slides most of the way along it and lets go there. */
    private fun fingerSlide(y: Float, left: Float, right: Float): List<Step> {
        val a = Offset(left, y)
        val b = Offset(right, y)
        return hold(a, 4, PointerType.Touch) +
            listOf(Step(PointerEventType.Press, a, PointerType.Touch)) + hold(a, 10, PointerType.Touch) +
            sweep(a, b, 45, PointerEventType.Move, PointerType.Touch) + hold(b, 10, PointerType.Touch) +
            listOf(Step(PointerEventType.Release, b, PointerType.Touch)) + hold(b, 25, PointerType.Touch)
    }

    // Phone: 400 × 730 dp. Desktop deck panel: 493 × 693 dp. Taps and sweeps are in pixels at 1.5×.
    private val phoneKeep = Offset(300f, 927f)
    private val desktopKeep = Offset(370f, 992f)

    @Test fun phoneFanFingerSlide() = record("fan-phone-slide", 600, 1095, phone = true, before = emptyList(), steps = fingerSlide(300f, 70f, 470f))

    @Test fun phoneFanFingerSlideFullHand() =
        record("fan-phone-slide-ten", 600, 1095, phone = true, before = List(6) { phoneKeep }, steps = fingerSlide(300f, 60f, 520f))

    @Test fun desktopFanMouseSweep() = record("fan-desktop-hover", 740, 1040, phone = false, before = emptyList(), steps = mouseSweep(330f, 90f, 650f))

    @Test fun desktopFanMouseSweepFullHand() =
        record("fan-desktop-hover-ten", 740, 1040, phone = false, before = List(6) { desktopKeep }, steps = mouseSweep(330f, 60f, 680f))
}
