package app.lockon.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import androidx.room.Room
import app.lockon.AppContainer
import app.lockon.data.db.LockOnDatabase
import app.lockon.model.CardColor
import app.lockon.model.CardType
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.platform.FileBridge
import app.lockon.sync.HostInfo
import app.lockon.sync.HostSyncController
import app.lockon.sync.PairingPrompt
import app.lockon.sync.SyncStatus
import app.lockon.ui.AppState
import app.lockon.ui.ArtworkProvider
import app.lockon.ui.LockOnApp
import app.lockon.ui.Screen
import app.lockon.ui.Tab
import app.lockon.ui.components.CardDetailContent
import app.lockon.ui.theme.LockOnTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * The real card list and official images at real device sizes: the user's 2576×1456 window at 125%
 * scaling, and a 1080×2400 phone at 2.75×. Needs the network, so it only runs with LOCKON_LIVE=1.
 */
class RealDataScreenshotTest {
    private val out = File("build/screenshots/real").apply { mkdirs() }
    private val cache = File("build/artwork-cache").absolutePath

    @Test fun renderRealScreens() = runBlocking {
        if (System.getenv("LOCKON_LIVE") != "1") return@runBlocking
        val container = AppContainer.create(LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>()), "Desktop")
        container.catalogUpdater.install(container.catalogUpdater.latestCardDatabase())
        val cards = container.catalog.cards.value.values

        // A plausible blue/white deck from GD01, plus an empty one.
        fun pick(color: CardColor, n: Int) = cards.filter { it.setCode == "GD01" && color in it.colors && it.type != CardType.RESOURCE }
            .sortedBy { it.cardNumber }.take(n)
        val main = pick(CardColor.BLUE, 7) + pick(CardColor.WHITE, 6)
        val resource = cards.first { it.type == CardType.RESOURCE }
        val deckId = container.decks.create(
            DeckSnapshot(
                "Federation Vanguard", tags = listOf("tournament"),
                entries = main.mapIndexed { i, c -> DeckEntry(c.cardNumber, if (i == 12) 2 else 4) } + DeckEntry(resource.cardNumber, 10, DeckSection.RESOURCE),
            ),
        )
        val emptyId = container.decks.create(DeckSnapshot("New deck"))

        val sync = object : HostSyncController {
            override val pairedDevices = container.devices.pairedDevices
            override val status = MutableStateFlow(SyncStatus(message = "Ready for phones on port 53340."))
            override val hostInfo = MutableStateFlow<HostInfo?>(null)
            override val pairingPrompt = MutableStateFlow<PairingPrompt?>(null)
            override val liveClients = MutableStateFlow(emptySet<String>())
            override fun answerPairing(requestId: String, accept: Boolean) = Unit
            override fun newPairingCode() = Unit
            override fun selectAddress(address: String) = Unit
        override val nearby = MutableStateFlow(listOf(app.lockon.sync.NearbyDevice("phone-2", "Pixel 9", "192.168.1.52", 53340, null, true, app.lockon.sync.SyncProtocol.ROLE_PHONE)))
        override val invited = MutableStateFlow(emptySet<String>())
        override fun invite(device: app.lockon.sync.NearbyDevice) = Unit
        override fun cancelInvite(deviceId: String) = Unit
            override fun forget(deviceId: String) = Unit
        }
        val files = object : FileBridge {
            override suspend fun saveText(suggestedName: String, mimeType: String, content: String) = false
            override suspend fun saveBytes(suggestedName: String, mimeType: String, content: ByteArray) = false
            override suspend fun openText(mimeTypes: List<String>): String? = null
        }

        fun shoot(name: String, w: Int, h: Int, density: Float, dark: Boolean = true, setup: AppState.() -> Unit) {
            val state = AppState(CoroutineScope(Dispatchers.Default)).apply(setup)
            ImageComposeScene(w, h, Density(density)) { LockOnApp(container, state, sync, files, imageCacheDir = cache, dark = dark) }.use { scene ->
                var image = scene.render()
                // Images arrive from the network on first run; give them time, advancing the frame clock.
                repeat(60) { frame -> Thread.sleep(150); image = scene.render((frame + 1) * 100_000_000L) }
                File(out, "$name.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }

        shoot("desktop-editor", 2576, 1456, 1.25f) { open(Screen.DeckEditor(deckId)) }
        shoot("light-desktop-editor", 2576, 1456, 1.25f, dark = false) { open(Screen.DeckEditor(deckId)) }
        shoot("light-desktop-library", 2576, 1456, 1.25f, dark = false) { select(Tab.DECKS) }
        shoot("light-phone-deck", 1080, 2400, 2.75f, dark = false) { open(Screen.DeckEditor(deckId)) }
        shoot("desktop-editor-empty", 2576, 1456, 1.25f) { open(Screen.DeckEditor(emptyId)) }
        shoot("desktop-library", 2576, 1456, 1.25f) { select(Tab.DECKS) }
        shoot("desktop-cards", 2576, 1456, 1.25f) { select(Tab.CARDS) }
        shoot("phone-add-cards", 1080, 2400, 2.75f) { open(Screen.DeckEditor(deckId, startTab = 1)) }
        shoot("phone-deck", 1080, 2400, 2.75f) { open(Screen.DeckEditor(deckId)) }
        shoot("phone-library", 1080, 2400, 2.75f) { select(Tab.DECKS) }

        // The PNG that Save image writes from the Export dialog, with the real card images.
        // The image follows the app's theme, so export it in both.
        for (dark in listOf(true, false)) {
        val saved = java.util.concurrent.CompletableFuture<ByteArray>()
        val capturing = object : FileBridge {
            override suspend fun saveText(suggestedName: String, mimeType: String, content: String) = false
            override suspend fun saveBytes(suggestedName: String, mimeType: String, content: ByteArray): Boolean {
                saved.complete(content); return true
            }
            override suspend fun openText(mimeTypes: List<String>): String? = null
        }
        val deck = container.decks.currentSnapshot(deckId)!!
        ImageComposeScene(1200, 1400, Density(1.5f)) {
            LockOnTheme(dark = dark) {
                ArtworkProvider(container, cache) {
                    app.lockon.ui.decks.DeckExportDialog(
                        deck, container.catalog.cards.value, container.catalog.ruleset.value, capturing,
                        AppState(CoroutineScope(Dispatchers.Default)), onDismiss = {}, startTab = 1,
                    )
                }
            }
        }.use { scene ->
            // Give the card images time to load before saving.
            repeat(40) { frame -> Thread.sleep(150); scene.render((frame + 1) * 100_000_000L) }
            val button = androidx.compose.ui.geometry.Offset(900f, 1196f)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press, button)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release, button)
            repeat(5) { frame -> Thread.sleep(60); scene.render((frame + 41) * 100_000_000L) }
            File(out, if (dark) "exported-deck-image.png" else "exported-deck-image-light.png").writeBytes(saved.get(10, java.util.concurrent.TimeUnit.SECONDS))
        }
        }

        // Details at side-panel width and at dialog width.
        val gundam = container.catalog.cards.value.getValue("GD01-001")
        for ((name, w) in listOf("detail-panel" to 575, "detail-wide" to 1200)) {
            ImageComposeScene((w * 1.25f).toInt(), (1100 * 1.25f).toInt(), Density(1.25f)) {
                LockOnTheme(dark = true) {
                    ArtworkProvider(container, cache) {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
                            CardDetailContent(gundam, container.catalog.printings.value["GD01-001"].orEmpty(), Modifier.padding(20.dp))
                        }
                    }
                }
            }.use { scene ->
                var image = scene.render()
                repeat(40) { frame -> Thread.sleep(120); image = scene.render((frame + 1) * 100_000_000L) }
                File(out, "$name.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
    }
}
