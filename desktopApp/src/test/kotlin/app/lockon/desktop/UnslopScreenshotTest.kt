package app.lockon.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import androidx.room.Room
import app.lockon.AppContainer
import app.lockon.data.CatalogFile
import app.lockon.data.CatalogOrigin
import app.lockon.data.db.LockOnDatabase
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.MatchRecord
import app.lockon.model.MatchResult
import app.lockon.platform.FileBridge
import app.lockon.sync.AutoSyncState
import app.lockon.sync.ClientSyncController
import app.lockon.sync.DeckSharing
import app.lockon.sync.DeviceRole
import app.lockon.sync.HostInfo
import app.lockon.sync.HostSyncController
import app.lockon.sync.IncomingShare
import app.lockon.sync.NearbyDevice
import app.lockon.sync.OutgoingShare
import app.lockon.sync.PairedDevice
import app.lockon.sync.PairingInvite
import app.lockon.sync.PairingPrompt
import app.lockon.sync.SyncController
import app.lockon.sync.SyncStatus
import app.lockon.ui.AppState
import app.lockon.ui.LockOnApp
import app.lockon.ui.Screen
import app.lockon.ui.Tab
import app.lockon.ui.components.LocalReduceMotion
import app.lockon.ui.components.SkyBackground
import app.lockon.ui.theme.LockOnTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * One fixed set of screens for comparing two versions of the interface side by side: desktop and
 * phone, both themes, drawn with motion reduced so nothing is caught mid-animation and two runs over
 * the same sources give the same pixels. Writes to build/unslop-shots.
 */
class UnslopScreenshotTest {
    private val out = File("build/unslop-shots").apply { deleteRecursively(); mkdirs() }

    private val container = runBlocking {
        AppContainer.create(LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>()), "Test Desktop").apply { settings.setWelcomed() }
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
                DeckEntry("SMP-014", 2, DeckSection.SIDE) + DeckEntry("SMP-016", 1, DeckSection.SIDE),
        )
        container.decks.create(first).also { id ->
            container.decks.keepVersion(id, first)
            container.decks.keepVersion(id, first.withQuantity("SMP-012", DeckSection.MAIN, 4).copy(matches = matches.take(2)))
            container.decks.save(id, container.decks.currentSnapshot(id)!!.copy(matches = matches))
            container.decks.create(DeckSnapshot(name = "Green test", folder = "Testing", entries = listOf(DeckEntry("SMP-015", 3))))
            container.decks.create(DeckSnapshot(name = "Wing Control", entries = listOf(DeckEntry("SMP-003", 4))))
        }
    }

    private class FakeSharing(targets: List<NearbyDevice>, outgoing: Map<String, OutgoingShare>) : DeckSharing {
        override val targets = MutableStateFlow(targets)
        override val outgoing = MutableStateFlow(outgoing)
        override val incoming = MutableStateFlow<IncomingShare?>(null)
        override fun send(device: NearbyDevice, deckIds: List<String>) = Unit
        override fun cancelSend(deviceId: String) = Unit
        override fun answer(shareId: String, accept: Boolean) = Unit
        override fun dismiss(shareId: String) = Unit
    }

    /** A computer's view: one phone paired and just matched, another nearby and unpaired. */
    private val host = object : HostSyncController {
        override val pairedDevices: Flow<List<PairedDevice>> = flowOf(listOf(
            PairedDevice("phone-1", "Pixel 8", DeviceRole.CLIENT, "192.168.1.51", 53340, "cd".repeat(32),
                1_762_000_000_000, 1_762_700_000_000, null, 0, 0),
        ))
        override val status = MutableStateFlow(SyncStatus(message = "Ready for phones on port 53340."))
        override val hostInfo = MutableStateFlow<HostInfo?>(
            HostInfo(true, listOf("192.168.1.20"), 53340, "K7Q2MX", 4_102_444_800_000, "ab".repeat(32),
                encodeQr("lockon://pair?host=192.168.1.20&port=53340&code=K7Q2MX&fp=" + "ab".repeat(32))),
        )
        override val pairingPrompt = MutableStateFlow<PairingPrompt?>(null)
        override val liveClients = MutableStateFlow(setOf("phone-1"))
        override fun answerPairing(requestId: String, accept: Boolean) = Unit
        override fun newPairingCode() = Unit
        override fun selectAddress(address: String) = Unit
        override val nearby = MutableStateFlow(listOf(NearbyDevice("phone-2", "Pixel 9", "192.168.1.52", 53340, null, true, app.lockon.sync.SyncProtocol.ROLE_PHONE)))
        override val invited = MutableStateFlow(emptySet<String>())
        override fun invite(device: NearbyDevice) = Unit
        override fun cancelInvite(deviceId: String) = Unit
        override fun forget(deviceId: String) = Unit
        override val transfers = MutableStateFlow(mapOf("phone-1" to app.lockon.sync.SyncTransfer("Pixel 8", app.lockon.sync.SyncMode.TAKE,
            app.lockon.sync.SyncTransfer.State.DONE,
            app.lockon.sync.matchedMessage("This computer", "Pixel 8", app.lockon.sync.MirrorSummary(updated = 3, added = 2, removed = 1, settings = true)))))
        override fun sync(deviceId: String, mode: app.lockon.sync.SyncMode) = Unit
        override fun clearTransfer(deviceId: String) = Unit
        override val sharing = FakeSharing(nearby.value, emptyMap())
    }

    /** A phone's view: one paired desktop live, another desktop nearby and unpaired. */
    private val client = object : ClientSyncController {
        override val pairedDevices: Flow<List<PairedDevice>> = flowOf(listOf(
            PairedDevice("desk-1", "DESKTOP-VPP8OD2", DeviceRole.HOST, "192.168.1.20", 53340, "ab".repeat(32),
                1_762_000_000_000, 1_762_700_000_000, null, 0, 0),
        ))
        override val status = MutableStateFlow(SyncStatus(message = app.lockon.sync.syncedMessage("DESKTOP-VPP8OD2", 1, 0)))
        override val canScanQr = true
        override val merging = MutableStateFlow(setOf("desk-1"))
        override val autoStates = MutableStateFlow(mapOf("desk-1" to AutoSyncState.LIVE))
        override val nearby = MutableStateFlow(listOf(NearbyDevice("desk-2", "Studio iMac", "192.168.1.41", 53340, null, true)))
        override val pendingVerification = MutableStateFlow<String?>(null)
        override fun setMerging(deviceId: String, on: Boolean) = Unit
        override val incomingRequest = MutableStateFlow<app.lockon.sync.SyncRequest?>(null)
        override fun answerRequest(accept: Boolean) = Unit
        override val incomingInvite = MutableStateFlow<app.lockon.sync.IncomingInvite?>(null)
        override fun answerInvite(accept: Boolean) = Unit
        override fun scanQr() = Unit
        override fun pair(invite: PairingInvite) = Unit
        override fun forget(deviceId: String) = Unit
        override val transfers = MutableStateFlow(emptyMap<String, app.lockon.sync.SyncTransfer>())
        override fun sync(deviceId: String, mode: app.lockon.sync.SyncMode) = Unit
        override fun clearTransfer(deviceId: String) = Unit
        override val sharing = FakeSharing(listOf(NearbyDevice("desk-1", "DESKTOP-VPP8OD2", "192.168.1.20", 53340, null, true)) + nearby.value, emptyMap())
    }

    private val files = object : FileBridge {
        override suspend fun saveText(suggestedName: String, mimeType: String, content: String) = false
        override suspend fun saveBytes(suggestedName: String, mimeType: String, content: ByteArray) = false
        override suspend fun openText(mimeTypes: List<String>): String? = null
    }

    private fun save(name: String, image: org.jetbrains.skia.Image) =
        File(out, "$name.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)

    /** The whole app on one screen: 1980×1290 for a computer window, 600×1260 for a phone, both at 1.5 px per dp. */
    private fun app(name: String, phone: Boolean, dark: Boolean, controller: SyncController = if (phone) client else host, setup: AppState.() -> Unit) {
        val state = AppState(CoroutineScope(Dispatchers.Default)).apply(setup).apply { bootClock.skipToLock() }
        app.lockon.ui.components.previewPhoneControls = phone
        try {
            ImageComposeScene(if (phone) 600 else 1980, if (phone) 1260 else 1290, Density(1.5f)) {
                // A computer window shows the title bar above the app, as Windows does.
                androidx.compose.foundation.layout.Column {
                    if (!phone) app.lockon.ui.theme.LockOnTheme(dark) {
                        TitleBar(maximized = false, background = androidx.compose.material3.MaterialTheme.colorScheme.background, onMinimize = {}, onToggleMaximize = {}, onClose = {})
                    }
                    LockOnApp(container, state, controller, files, artworkDir = File("build/artwork-cache").absolutePath, dark = dark, reduceMotion = true)
                }
            }.use { scene ->
                var time = 0L
                var image = scene.render(time)
                fun frame() { Thread.sleep(150); time += 100_000_000L; image = scene.render(time) }
                // Until the boot screen has gone (it waits on the screens underneath), then long enough
                // for every list to have its data and its last frame.
                var waited = 0
                while ((!state.booted || waited < 20) && waited < 200) { frame(); waited++ }
                check(state.booted) { "$name: the boot screen never finished" }
                repeat(15) { frame() }
                save(name, image)
            }
        } finally {
            app.lockon.ui.components.previewPhoneControls = false
        }
    }

    /** One composable over the sky, for dialogs and the launch screen, which open from a button or before the app. */
    private fun alone(name: String, phone: Boolean, dark: Boolean, content: @Composable () -> Unit) {
        app.lockon.ui.components.previewPhoneControls = phone
        try {
            ImageComposeScene(if (phone) 600 else 1980, if (phone) 1260 else 1290, Density(1.5f)) {
                LockOnTheme(dark = dark) {
                    CompositionLocalProvider(LocalReduceMotion provides true) {
                        SkyBackground(Modifier.fillMaxSize()) { content() }
                    }
                }
            }.use { scene ->
                var time = 0L
                var image = scene.render(time)
                repeat(25) { Thread.sleep(120); time += 100_000_000L; image = scene.render(time) }
                save(name, image)
            }
        } finally {
            app.lockon.ui.components.previewPhoneControls = false
        }
    }

    @Test fun renderUnslopSet() {
        // The first shot in a run starts slowly and can catch the boot screen, so one is thrown away.
        app("warm-up", phone = false, dark = true) { select(Tab.DECKS) }
        File(out, "warm-up.png").delete()

        app("desktop-decks-dark", phone = false, dark = true) { select(Tab.DECKS) }
        app("desktop-editor-light", phone = false, dark = false) { open(Screen.DeckEditor(deckId)) }
        app("desktop-analysis-dark", phone = false, dark = true) { open(Screen.DeckEditor(deckId, startTab = 2)) }
        app("desktop-matches-light", phone = false, dark = false) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        app("desktop-cards-dark", phone = false, dark = true) { select(Tab.CARDS) }
        app("desktop-sync-dark", phone = false, dark = true) { select(Tab.SYNC) }
        app("desktop-settings-light", phone = false, dark = false) { select(Tab.SETTINGS) }
        app("desktop-tour-light", phone = false, dark = false) { touring = true; tourStop = 0 }

        app("phone-decks-light", phone = true, dark = false) { select(Tab.DECKS) }
        app("phone-editor-dark", phone = true, dark = true) { open(Screen.DeckEditor(deckId)) }
        app("phone-add-cards-light", phone = true, dark = false) { open(Screen.DeckEditor(deckId, startTab = 1)) }
        app("phone-matches-dark", phone = true, dark = true) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        app("phone-sync-light", phone = true, dark = false) { select(Tab.SYNC) }
        app("phone-settings-dark", phone = true, dark = true) { select(Tab.SETTINGS) }

        val deck = runBlocking { container.decks.currentSnapshot(deckId)!! }
        alone("desktop-export-dark", phone = false, dark = true) {
            app.lockon.ui.decks.DeckExportDialog(deck, container.catalog.cards.value, container.catalog.ruleset.value, files,
                AppState(CoroutineScope(Dispatchers.Default)), onDismiss = {}, startTab = 1, qrTab = false)
        }
        val card = container.catalog.cards.value.getValue("SMP-001")
        alone("phone-card-details-dark", phone = true, dark = true) {
            app.lockon.ui.components.CardDetailDialog(card, emptyList(), onDismiss = {}) {
                Column {
                    app.lockon.ui.components.PlateStepper(4, "copy", onMinus = {}, onPlus = {}, limit = 4)
                    app.lockon.ui.components.PlateStepper(0, "side deck copy", onMinus = {}, onPlus = {})
                }
            }
        }
        alone("phone-launch-light", phone = true, dark = false) {
            app.lockon.ui.BootScreen(app.lockon.ui.BootStep.CARDS, app.lockon.ui.BootClock())
        }
        alone("desktop-launch-dark", phone = false, dark = true) {
            androidx.compose.foundation.layout.Column {
                TitleBar(maximized = false, background = androidx.compose.material3.MaterialTheme.colorScheme.background, onMinimize = {}, onToggleMaximize = {}, onClose = {})
                app.lockon.ui.BootScreen(app.lockon.ui.BootStep.CARDS, app.lockon.ui.BootClock())
            }
        }

        // Last, since it swaps the catalog: the card database dialog with the official list installed.
        val cards = container.catalog.cards.value.values.toList()
        runBlocking {
            container.catalog.install(
                CatalogFile(catalogId = "gcg-api", version = 31, name = "Official card list", source = app.lockon.catalog.GcgApi.ATTRIBUTION + " Dataset built 2026-11-02.", cards = cards, printings = emptyList()),
                CatalogOrigin.GCG_API, null,
            )
        }
        alone("desktop-card-database-light", phone = false, dark = false) {
            app.lockon.ui.cards.CatalogDialog(container, AppState(CoroutineScope(Dispatchers.Default)), files, onDismiss = {})
        }
    }
}
