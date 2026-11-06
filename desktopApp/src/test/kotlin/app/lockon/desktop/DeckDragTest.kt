package app.lockon.desktop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import androidx.room.Room
import app.lockon.AppContainer
import app.lockon.data.db.LockOnDatabase
import app.lockon.model.DeckSnapshot
import app.lockon.platform.FileBridge
import app.lockon.sync.ClientSyncController
import app.lockon.ui.AppState
import app.lockon.ui.LockOnApp
import app.lockon.ui.Tab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** Drags a deck box onto a folder in the desktop rail with the mouse, and checks the deck moved. */
@OptIn(ExperimentalComposeUiApi::class)
class DeckDragTest {
    private val out = File("build/screenshots").apply { mkdirs() }
    private val container = runBlocking {
        AppContainer.create(LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>()), "Test Desktop").apply { settings.setWelcomed() }
    }
    private val beta = runBlocking {
        container.decks.create(DeckSnapshot(name = "Alpha", folder = "Testing"))
        container.decks.create(DeckSnapshot(name = "Beta"))
    }
    private val noSync = object : ClientSyncController {
        override val pairedDevices = flowOf(emptyList<app.lockon.sync.PairedDevice>())
        override val status = MutableStateFlow(app.lockon.sync.SyncStatus(message = ""))
        override val canScanQr = false
        override val merging = MutableStateFlow(emptySet<String>())
        override val autoStates = MutableStateFlow(emptyMap<String, app.lockon.sync.AutoSyncState>())
        override val nearby = MutableStateFlow(emptyList<app.lockon.sync.NearbyDevice>())
        override val pendingVerification = MutableStateFlow<String?>(null)
        override fun setMerging(deviceId: String, on: Boolean) = Unit
        override val incomingRequest = MutableStateFlow<app.lockon.sync.SyncRequest?>(null)
        override fun answerRequest(accept: Boolean) = Unit
        override val incomingInvite = MutableStateFlow<app.lockon.sync.IncomingInvite?>(null)
        override fun answerInvite(accept: Boolean) = Unit
        override fun scanQr() = Unit
        override fun pair(invite: app.lockon.sync.PairingInvite) = Unit
        override fun forget(deviceId: String) = Unit
        override val transfers = MutableStateFlow(emptyMap<String, app.lockon.sync.SyncTransfer>())
        override fun sync(deviceId: String, mode: app.lockon.sync.SyncMode) = Unit
        override fun clearTransfer(deviceId: String) = Unit
        override val sharing = ScreenshotTest.FakeSharing(emptyList(), emptyMap())
    }
    private val files = object : FileBridge {
        override suspend fun saveText(suggestedName: String, mimeType: String, content: String) = false
        override suspend fun saveBytes(suggestedName: String, mimeType: String, content: ByteArray) = false
        override suspend fun openText(mimeTypes: List<String>): String? = null
    }

    /** A theme change takes a picture of the real app's layers. Drawn from inside them, that picture would never finish, and the crash is native. */
    @Test fun themeChangeFadesAcrossTheWholeApp() {
        val state = AppState(CoroutineScope(Dispatchers.Default)).apply { select(Tab.DECKS); bootClock.skipToLock() }
        val dark = androidx.compose.runtime.mutableStateOf(false)
        ImageComposeScene(1980, 1290, Density(1.5f)) {
            LockOnApp(container, state, noSync, files, artworkDir = File("build/artwork-cache").absolutePath, dark = dark.value)
        }.use { scene ->
            var time = 0L
            fun settle(frames: Int) = repeat(frames) { Thread.sleep(16); time += 16_666_667L; scene.render(time) }
            settle(200)
            File(out, "theme-before.png").writeBytes(scene.render(time).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            dark.value = true
            settle(6)
            File(out, "theme-mid.png").writeBytes(scene.render(time).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            settle(40)
            File(out, "theme-after.png").writeBytes(scene.render(time).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    @Test fun dragDeckOntoFolder() {
        // Frames here step 50-100 ms, faster than the boot clock follows, so it starts past its first lock.
        val state = AppState(CoroutineScope(Dispatchers.Default)).apply { select(Tab.DECKS); bootClock.skipToLock() }
        ImageComposeScene(1980, 1290, Density(1.5f)) {
            LockOnApp(container, state, noSync, files, artworkDir = File("build/artwork-cache").absolutePath, dark = false)
        }.use { scene ->
            var time = 0L
            fun settle(frames: Int = 10) = repeat(frames) { Thread.sleep(40); time += 50_000_000L; scene.render(time) }
            fun shot(name: String) = File(out, "$name.png").writeBytes(scene.render(time).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            // Past the boot screen, which shows for at least 2.5 s of real time.
            settle(70)
            shot("deck-drag-before")
            // From the Beta box to the Testing folder in the rail, at this size and density.
            val from = Offset(500f, 290f)
            val to = Offset(150f, 243f)
            val held = PointerButtons(isPrimaryPressed = true)
            scene.sendPointerEvent(PointerEventType.Move, from, type = PointerType.Mouse)
            scene.sendPointerEvent(PointerEventType.Press, from, type = PointerType.Mouse, buttons = held, button = PointerButton.Primary)
            for (i in 1..12) {
                scene.sendPointerEvent(PointerEventType.Move, from + (to - from) * (i / 12f), type = PointerType.Mouse, buttons = held)
                settle(1)
            }
            settle(5)
            shot("deck-drag-held")
            scene.sendPointerEvent(PointerEventType.Release, to, type = PointerType.Mouse, button = PointerButton.Primary)
            settle(15)
            shot("deck-drag-after")
            assertEquals("Testing", runBlocking { container.decks.currentSnapshot(beta)!!.folder })
        }
    }
}
