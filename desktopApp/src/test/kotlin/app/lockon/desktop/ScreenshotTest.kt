package app.lockon.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import androidx.room.Room
import app.lockon.AppContainer
import app.lockon.data.CatalogFile
import app.lockon.data.CatalogOrigin
import app.lockon.data.db.LockOnDatabase
import app.lockon.model.Printing
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.MatchRecord
import app.lockon.model.MatchResult
import app.lockon.platform.FileBridge
import app.lockon.sync.AutoSyncState
import app.lockon.sync.ClientSyncController
import app.lockon.sync.DeviceRole
import app.lockon.sync.HostInfo
import app.lockon.sync.NearbyDevice
import app.lockon.sync.PairingInvite
import app.lockon.sync.SyncController
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
import kotlinx.coroutines.flow.flowOf
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
        val matches = listOf(
            MatchRecord("m1", 1_761_400_000_000, MatchResult.WIN, "Zeon Rush", wentFirst = true),
            MatchRecord("m2", 1_761_403_000_000, MatchResult.LOSS, "Wing Control", wentFirst = false, notes = "Bricked on resources"),
            MatchRecord("m3", 1_761_406_000_000, MatchResult.WIN, "Zeon Rush", wentFirst = false),
            MatchRecord("m4", 1_761_409_000_000, MatchResult.WIN),
        )
        val first = DeckSnapshot(
            name = "Blue Red Aggro", tags = listOf("tournament"), folder = "Store championship",
            entries = (1..11).map { DeckEntry("SMP-%03d".format(it), 4) } + DeckEntry("SMP-013", 2) +
                DeckEntry("SMP-019", 6, DeckSection.RESOURCE) + DeckEntry("SMP-020", 4, DeckSection.RESOURCE) +
                DeckEntry("SMP-014", 2, DeckSection.CONSIDERING) + DeckEntry("SMP-016", 1, DeckSection.CONSIDERING),
        )
        container.decks.create(first).also { id ->
            // Two kept versions, so the history has something to compare.
            container.decks.keepVersion(id, first)
            container.decks.keepVersion(id, first.withQuantity("SMP-012", DeckSection.MAIN, 4).copy(matches = matches.take(2)))
            container.decks.save(id, container.decks.currentSnapshot(id)!!.copy(matches = matches))
            container.decks.create(DeckSnapshot(name = "Green test", folder = "Testing", entries = listOf(DeckEntry("SMP-015", 3))))
            container.decks.create(DeckSnapshot(name = "Wing Control", entries = listOf(DeckEntry("SMP-003", 4))))
        }
    }

    private val sync = object : HostSyncController {
        override val pairedDevices: Flow<List<PairedDevice>> = container.devices.pairedDevices
        override val status = MutableStateFlow(SyncStatus(message = "Ready for phones on port 53340."))
        override val hostInfo = MutableStateFlow<HostInfo?>(
            HostInfo(true, listOf("192.168.1.20"), 53340, "K7Q2MX", System.currentTimeMillis() + 540_000, "ab".repeat(32),
                encodeQr("lockon://pair?host=192.168.1.20&port=53340&code=K7Q2MX&fp=" + "ab".repeat(32))),
        )
        override val pairingPrompt = MutableStateFlow<PairingPrompt?>(null)
        override val liveClients = MutableStateFlow(setOf("phone-1"))
        override fun answerPairing(requestId: String, accept: Boolean) = Unit
        override fun newPairingCode() = Unit
        override fun selectAddress(address: String) = Unit
        override val nearby = MutableStateFlow(listOf(app.lockon.sync.NearbyDevice("phone-2", "Pixel 9", "192.168.1.52", 53340, null, true, app.lockon.sync.SyncProtocol.ROLE_PHONE)))
        override val invited = MutableStateFlow(emptySet<String>())
        override fun invite(device: app.lockon.sync.NearbyDevice) = Unit
        override fun cancelInvite(deviceId: String) = Unit
        override fun forget(deviceId: String) = Unit
    }

    /** A phone's view: one paired desktop live, another desktop nearby and unpaired. */
    private val client = object : ClientSyncController {
        override val pairedDevices: Flow<List<PairedDevice>> = flowOf(listOf(
            PairedDevice("desk-1", "DESKTOP-VPP8OD2", DeviceRole.HOST, "192.168.1.20", 53340, "ab".repeat(32),
                System.currentTimeMillis() - 86_400_000, System.currentTimeMillis() - 60_000, null, 0, 0),
        ))
        override val status = MutableStateFlow(SyncStatus(message = "Auto-synced with DESKTOP-VPP8OD2: sent 1, received 0 changes."))
        override val canScanQr = true
        override val autoSync = MutableStateFlow(true)
        override val autoStates = MutableStateFlow(mapOf("desk-1" to AutoSyncState.LIVE))
        override val nearby = MutableStateFlow(listOf(NearbyDevice("desk-2", "Studio iMac", "192.168.1.41", 53340, null, true)))
        override val pendingVerification = MutableStateFlow<String?>(null)
        override fun setAutoSync(enabled: Boolean) = Unit
        override val incomingInvite = MutableStateFlow<app.lockon.sync.IncomingInvite?>(null)
        override fun answerInvite(accept: Boolean) = Unit
        override fun scanQr() = Unit
        override fun pair(invite: PairingInvite) = Unit
        override fun syncNow(deviceId: String) = Unit
        override fun forget(deviceId: String) = Unit
    }

    private val files = object : FileBridge {
        override suspend fun saveText(suggestedName: String, mimeType: String, content: String) = false
        override suspend fun saveBytes(suggestedName: String, mimeType: String, content: ByteArray) = false
        override suspend fun openText(mimeTypes: List<String>): String? = null
    }

    private fun shoot(name: String, width: Int, height: Int, dark: Boolean = true, controller: SyncController = sync, setup: AppState.() -> Unit) {
        val state = AppState(CoroutineScope(Dispatchers.Default)).apply(setup)
        ImageComposeScene(width, height, Density(1.5f)) { LockOnApp(container, state, controller, files, imageCacheDir = File("build/artwork-cache").absolutePath, dark = dark) }.use { scene ->
            var image = scene.render()
            // Let Room flows settle, and advance the frame clock so animations reach their end state.
            repeat(15) { frame -> Thread.sleep(60); image = scene.render((frame + 1) * 100_000_000L) }
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
        shoot("phone-matches", 600, 1260) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        shoot("phone-versions", 600, 1260) { open(Screen.DeckEditor(deckId, startTab = 5)) }
        shoot("light-desktop-editor", 1980, 1290, dark = false) { open(Screen.DeckEditor(deckId)) }
        shoot("light-phone-library", 600, 1260, dark = false) { select(Tab.DECKS) }
        sync.status.value = SyncStatus(message = "Could not reach the desktop. Check that Lock On is open on it.", isError = true)
        shoot("phone-sync-error", 600, 1260) { select(Tab.SYNC) }
        shoot("phone-sync-client", 600, 1260, controller = client) { select(Tab.SYNC) }
        sync.status.value = SyncStatus(message = "Ready for phones on port 53340.")
        // The Catalog dialog, drawn on its own because it opens from a button.
        ImageComposeScene(600, 1260, Density(1.5f)) {
            app.lockon.ui.theme.LockOnTheme(dark = true) {
                androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    app.lockon.ui.cards.CatalogDialog(container, AppState(CoroutineScope(Dispatchers.Default)), files, onDismiss = {})
                }
            }
        }.use { scene ->
            var image = scene.render()
            repeat(10) { frame -> Thread.sleep(60); image = scene.render((frame + 1) * 100_000_000L) }
            File(out, "phone-catalog.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        // The export dialog's Image tab, drawn on its own for the same reason.
        val deck = runBlocking { container.decks.currentSnapshot(deckId)!! }
        ImageComposeScene(1200, 1400, Density(1.5f)) {
            app.lockon.ui.theme.LockOnTheme(dark = true) {
                androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    app.lockon.ui.decks.DeckExportDialog(
                        deck, container.catalog.cards.value, container.catalog.ruleset.value, files,
                        AppState(CoroutineScope(Dispatchers.Default)), onDismiss = {}, startTab = 1,
                    )
                }
            }
        }.use { scene ->
            var image = scene.render()
            repeat(10) { frame -> Thread.sleep(60); image = scene.render((frame + 1) * 100_000_000L) }
            File(out, "desktop-export-image.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        assertTrue(File(out, "phone-cards.png").length() > 0)
    }

    /** Clicks Save image in the export dialog and checks the file is a 1080-pixel-wide PNG. */
    @Test fun savesTheDeckImageAsPng() {
        val saved = java.util.concurrent.CompletableFuture<ByteArray>()
        val capturing = object : FileBridge {
            override suspend fun saveText(suggestedName: String, mimeType: String, content: String) = false
            override suspend fun saveBytes(suggestedName: String, mimeType: String, content: ByteArray): Boolean {
                saved.complete(content); return true
            }
            override suspend fun openText(mimeTypes: List<String>): String? = null
        }
        val deck = runBlocking { container.decks.currentSnapshot(deckId)!! }
        ImageComposeScene(1200, 1400, Density(1.5f)) {
            app.lockon.ui.theme.LockOnTheme(dark = true) {
                app.lockon.ui.decks.DeckExportDialog(
                    deck, container.catalog.cards.value, container.catalog.ruleset.value, capturing,
                    AppState(CoroutineScope(Dispatchers.Default)), onDismiss = {}, startTab = 1,
                )
            }
        }.use { scene ->
            repeat(5) { frame -> Thread.sleep(60); scene.render((frame + 1) * 100_000_000L) }
            val button = androidx.compose.ui.geometry.Offset(905f, 1113f)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press, button)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release, button)
            repeat(5) { frame -> Thread.sleep(60); scene.render((frame + 6) * 100_000_000L) }
            val image = org.jetbrains.skia.Image.makeFromEncoded(saved.get(10, java.util.concurrent.TimeUnit.SECONDS))
            kotlin.test.assertEquals(1080, image.width)
            assertTrue(image.height > 1080, "the whole deck is in the image, not just what the dialog shows")
        }
    }

    /** Placeholder artwork drawn locally, linked from a catalog the same way real artwork would be. */
    private fun fakeArt(name: String, color: java.awt.Color): String {
        val file = File("build/test-art/$name.png").apply { parentFile.mkdirs() }
        val image = java.awt.image.BufferedImage(630, 880, java.awt.image.BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            paint = java.awt.GradientPaint(0f, 0f, color, 630f, 880f, color.darker().darker())
            fillRect(0, 0, 630, 880)
            this.color = java.awt.Color(255, 255, 255, 90)
            stroke = java.awt.BasicStroke(18f)
            drawOval(165, 290, 300, 300)
            drawLine(315, 200, 315, 680); drawLine(75, 440, 555, 440)
            this.color = java.awt.Color.WHITE
            font = java.awt.Font("SansSerif", java.awt.Font.BOLD, 54)
            drawString(name, 40, 820)
            dispose()
        }
        javax.imageio.ImageIO.write(image, "png", file)
        return "file:///" + file.absolutePath.replace('\\', '/')
    }

    @Test fun renderArtwork() = runBlocking {
        val cards = container.catalog.cards.value.values.toList()
        val art = mapOf(
            "SMP-001" to fakeArt("SMP-001", java.awt.Color(0x2A5BB8)),
            "SMP-002" to fakeArt("SMP-002", java.awt.Color(0x3B7BE8)),
            "SMP-008" to fakeArt("SMP-008", java.awt.Color(0xD3352B)),
            "SMP-010" to fakeArt("SMP-010", java.awt.Color(0xB3261E)),
        )
        val printings = art.map { (number, url) -> Printing(number, number, "SMP", "R", artworkUrl = url) } +
            Printing("SMP-001-P1", "SMP-001", "SMP", "R+", label = "Alternate art", isAlternateArt = true,
                artworkUrl = fakeArt("SMP-001 alt", java.awt.Color(0x745B00)))
        container.catalog.install(
            CatalogFile(catalogId = "art-test", version = 1, name = "Artwork test", cards = cards, printings = printings),
            CatalogOrigin.FILE, null,
        )
        shoot("phone-cards-artwork", 600, 1260) { select(Tab.CARDS) }
        shoot("desktop-editor-artwork", 1980, 1290) { open(Screen.DeckEditor(deckId)) }
        ImageComposeScene(600, 1260, Density(1.5f)) {
            app.lockon.ui.theme.LockOnTheme(dark = true) {
                app.lockon.ui.ArtworkProvider(container, File("build/artwork-cache").absolutePath) {
                    androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                        app.lockon.ui.components.CardDetailDialog(
                            container.catalog.cards.value.getValue("SMP-001"),
                            container.catalog.printings.value["SMP-001"].orEmpty(), onDismiss = {},
                        )
                    }
                }
            }
        }.use { scene ->
            var image = scene.render()
            repeat(20) { frame -> Thread.sleep(80); image = scene.render((frame + 1) * 100_000_000L) }
            File(out, "phone-card-detail-artwork.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        container.catalog.restoreBundled(File("../shared/src/commonMain/composeResources/files/catalog/sample-catalog.json").readText())
    }

    /** Real card data and Bandai artwork; needs the network, so it only runs with LOCKON_LIVE=1. */
    @Test fun renderLiveCardDatabase() = runBlocking {
        if (System.getenv("LOCKON_LIVE") != "1") return@runBlocking
        container.catalogUpdater.install(container.catalogUpdater.latestCardDatabase())
        shoot("live-phone-cards", 600, 1260) { select(Tab.CARDS) }
        ImageComposeScene(600, 1260, Density(1.5f)) {
            app.lockon.ui.theme.LockOnTheme(dark = true) {
                app.lockon.ui.ArtworkProvider(container, File("build/artwork-cache").absolutePath) {
                    androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                        app.lockon.ui.components.CardDetailDialog(
                            container.catalog.cards.value.getValue("GD01-001"),
                            container.catalog.printings.value["GD01-001"].orEmpty(), onDismiss = {},
                        )
                    }
                }
            }
        }.use { scene ->
            var image = scene.render()
            repeat(40) { frame -> Thread.sleep(150); image = scene.render((frame + 1) * 100_000_000L) }
            File(out, "live-card-detail.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        container.catalog.restoreBundled(File("../shared/src/commonMain/composeResources/files/catalog/sample-catalog.json").readText())
    }
}
