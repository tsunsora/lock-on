package app.lockon.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import androidx.room.Room
import app.lockon.AppContainer
import app.lockon.data.db.LockOnDatabase
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.platform.FileBridge
import app.lockon.sync.HostInfo
import app.lockon.sync.HostSyncController
import app.lockon.sync.PairedDevice
import app.lockon.sync.PairingPrompt
import app.lockon.sync.SyncStatus
import app.lockon.ui.AppState
import app.lockon.ui.LockOnApp
import app.lockon.ui.Screen
import app.lockon.ui.Tab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Renders key screens to build/screenshots so layouts can be reviewed without a device. */
class ScreenshotTest {
    private val out = File("build/screenshots").apply { mkdirs() }

    private val container = runBlocking {
        AppContainer.create(LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>()), "Test Desktop")
    }
    private val deckId = runBlocking {
        container.decks.create(
            DeckSnapshot(
                name = "Blue Red Aggro", tags = listOf("tournament"),
                entries = (1..12).map { DeckEntry("SMP-%03d".format(it), 4) } + DeckEntry("SMP-013", 2) +
                    DeckEntry("SMP-019", 6, DeckSection.RESOURCE) + DeckEntry("SMP-020", 4, DeckSection.RESOURCE) +
                    DeckEntry("SMP-014", 2, DeckSection.CONSIDERING) + DeckEntry("SMP-016", 1, DeckSection.CONSIDERING),
            ),
        ).also { container.decks.create(DeckSnapshot(name = "Green test", entries = listOf(DeckEntry("SMP-015", 3)))) }
    }

    private val sync = object : HostSyncController {
        override val pairedDevices: Flow<List<PairedDevice>> = container.devices.pairedDevices
        override val status = MutableStateFlow(SyncStatus(message = "Ready for phones on port 53340."))
        override val hostInfo = MutableStateFlow<HostInfo?>(
            HostInfo(true, listOf("192.168.1.20"), 53340, "K7Q2MX", System.currentTimeMillis() + 540_000, "ab".repeat(32),
                encodeQr("lockon://pair?host=192.168.1.20&port=53340&code=K7Q2MX&fp=" + "ab".repeat(32))),
        )
        override val pairingPrompt = MutableStateFlow<PairingPrompt?>(null)
        override fun answerPairing(requestId: String, accept: Boolean) = Unit
        override fun newPairingCode() = Unit
        override fun selectAddress(address: String) = Unit
        override fun forget(deviceId: String) = Unit
    }

    private val files = object : FileBridge {
        override suspend fun saveText(suggestedName: String, mimeType: String, content: String) = false
        override suspend fun openText(mimeTypes: List<String>): String? = null
    }

    private fun shoot(name: String, width: Int, height: Int, dark: Boolean = true, setup: AppState.() -> Unit) {
        val state = AppState(CoroutineScope(Dispatchers.Default)).apply(setup)
        ImageComposeScene(width, height, Density(1.5f)) { LockOnApp(container, state, sync, files, dark = dark) }.use { scene ->
            var image = scene.render()
            // Let Room flows and effects settle between frames.
            repeat(15) { Thread.sleep(60); image = scene.render() }
            File(out, "$name.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    @Test fun renderScreens() {
        shoot("desktop-library", 1980, 1290) { select(Tab.DECKS) }
        shoot("desktop-editor", 1980, 1290) { open(Screen.DeckEditor(deckId)) }
        shoot("desktop-sync", 1980, 1290) { select(Tab.SYNC) }
        shoot("phone-library", 600, 1260) { select(Tab.DECKS) }
        shoot("phone-editor", 600, 1260) { open(Screen.DeckEditor(deckId)) }
        shoot("phone-cards", 600, 1260) { select(Tab.CARDS) }
        shoot("phone-add-cards", 600, 1260) { open(Screen.DeckEditor(deckId, startTab = 1)) }
        shoot("phone-considering", 600, 3200) { open(Screen.DeckEditor(deckId)) }
        shoot("phone-test-hand", 600, 1260) { open(Screen.DeckEditor(deckId, startTab = 3)) }
        shoot("light-desktop-editor", 1980, 1290, dark = false) { open(Screen.DeckEditor(deckId)) }
        shoot("light-phone-library", 600, 1260, dark = false) { select(Tab.DECKS) }
        sync.status.value = SyncStatus(message = "Could not reach the desktop. Check that Lock On is open on it.", isError = true)
        shoot("phone-sync-error", 600, 1260) { select(Tab.SYNC) }
        sync.status.value = SyncStatus(message = "Ready for phones on port 53340.")
        assertTrue(File(out, "phone-cards.png").length() > 0)
    }
}
