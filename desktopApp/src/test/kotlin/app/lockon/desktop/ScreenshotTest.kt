package app.lockon.desktop
import androidx.compose.runtime.Composable
import app.lockon.sync.RevisionDto
import androidx.compose.foundation.background
import app.lockon.ui.components.shineSweep

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp

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
import app.lockon.sync.DeckSharing
import app.lockon.sync.IncomingShare
import app.lockon.sync.OutgoingShare
import app.lockon.ui.AppState
import app.lockon.ui.LockOnApp
import app.lockon.ui.Screen
import app.lockon.ui.Tab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Renders key screens to build/screenshots so layouts can be reviewed without a device. */
class ScreenshotTest {
    private val out = File("build/screenshots").apply { mkdirs() }

    private val container = runBlocking {
        // A fresh database would open on the welcome tour; these shots want the screens themselves.
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
        override val transfers = MutableStateFlow(emptyMap<String, app.lockon.sync.SyncTransfer>())
        override fun sync(deviceId: String, mode: app.lockon.sync.SyncMode) = Unit
        override fun clearTransfer(deviceId: String) = Unit
        override val sharing = FakeSharing(nearby.value, mapOf("phone-2" to OutgoingShare("Pixel 9", 2, OutgoingShare.State.WAITING)))
    }

    /** A phone's view: one paired desktop live, another desktop nearby and unpaired. */
    private val client = object : ClientSyncController {
        override val pairedDevices: Flow<List<PairedDevice>> = flowOf(listOf(
            PairedDevice("desk-1", "DESKTOP-VPP8OD2", DeviceRole.HOST, "192.168.1.20", 53340, "ab".repeat(32),
                System.currentTimeMillis() - 86_400_000, System.currentTimeMillis() - 60_000, null, 0, 0),
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
        override val sharing = FakeSharing(
            listOf(NearbyDevice("desk-1", "DESKTOP-VPP8OD2", "192.168.1.20", 53340, null, true)) + nearby.value,
            mapOf("desk-1" to OutgoingShare("DESKTOP-VPP8OD2", 2, OutgoingShare.State.SAVED, "DESKTOP-VPP8OD2 saved 2 decks.")),
        )
    }

    class FakeSharing(targets: List<NearbyDevice>, outgoing: Map<String, OutgoingShare>) : DeckSharing {
        override val targets = MutableStateFlow(targets)
        override val outgoing = MutableStateFlow(outgoing)
        override val incoming = MutableStateFlow<IncomingShare?>(null)
        override fun send(device: NearbyDevice, deckIds: List<String>) = Unit
        override fun cancelSend(deviceId: String) = Unit
        override fun answer(shareId: String, accept: Boolean) = Unit
        override fun dismiss(shareId: String) = Unit
    }

    private val files = object : FileBridge {
        override suspend fun saveText(suggestedName: String, mimeType: String, content: String) = false
        override suspend fun saveBytes(suggestedName: String, mimeType: String, content: ByteArray) = false
        override suspend fun openText(mimeTypes: List<String>): String? = null
    }

    /** The desktop title bar as the app draws it, with no window to move. */
    private val chrome = app.lockon.ui.WindowChrome(
        dragArea = { this },
        controls = { WindowControls(maximized = false, onMinimize = {}, onToggleMaximize = {}, onClose = {}) },
    )

    /** Desktop-sized shots (phones are 600 px wide) include the title bar, as the Windows app shows it. */
    private fun shoot(
        name: String, width: Int, height: Int, dark: Boolean = true, controller: SyncController = sync,
        taps: List<androidx.compose.ui.geometry.Offset> = emptyList(), hover: androidx.compose.ui.geometry.Offset? = null,
        scrollAt: androidx.compose.ui.geometry.Offset? = null, setup: AppState.() -> Unit,
    ) {
        // Frames here step 50-100 ms, faster than the boot clock follows, so it starts past its first lock.
        val state = AppState(CoroutineScope(Dispatchers.Default)).apply(setup).apply { bootClock.skipToLock() }
        val titleBar = if (width > 600) chrome else null
        ImageComposeScene(width, height, Density(1.5f)) { LockOnApp(container, state, controller, files, artworkDir = File("build/artwork-cache").absolutePath, dark = dark, chrome = titleBar) }.use { scene ->
            var image = scene.render()
            // Let Room flows settle and the boot screen's 2.5 s minimum pass, and advance the frame clock
            // so animations reach their end state.
            repeat(15) { frame -> Thread.sleep(200); image = scene.render((frame + 1) * 100_000_000L) }
            // Taps are in pixels, each followed by time for what it opens to finish animating.
            var time = 15 * 100_000_000L
            for (tap in taps) {
                scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Move, tap, type = androidx.compose.ui.input.pointer.PointerType.Mouse)
                scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press, tap, type = androidx.compose.ui.input.pointer.PointerType.Mouse,
                    buttons = androidx.compose.ui.input.pointer.PointerButtons(isPrimaryPressed = true), button = androidx.compose.ui.input.pointer.PointerButton.Primary)
                scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release, tap, type = androidx.compose.ui.input.pointer.PointerType.Mouse,
                    button = androidx.compose.ui.input.pointer.PointerButton.Primary)
                repeat(8) { Thread.sleep(150); time += 100_000_000L; image = scene.render(time) }
            }
            // Wheel turns at [scrollAt], far enough to reach the end of a short list.
            if (scrollAt != null) {
                repeat(10) {
                    scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Scroll, scrollAt, type = androidx.compose.ui.input.pointer.PointerType.Mouse,
                        scrollDelta = androidx.compose.ui.geometry.Offset(0f, 3f))
                    Thread.sleep(60); time += 50_000_000L; image = scene.render(time)
                }
                repeat(8) { Thread.sleep(150); time += 100_000_000L; image = scene.render(time) }
            }
            // A mouse resting at [hover], for hover effects.
            if (hover != null) {
                scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Move, hover, type = androidx.compose.ui.input.pointer.PointerType.Mouse)
                repeat(8) { Thread.sleep(150); time += 100_000_000L; image = scene.render(time) }
            }
            File(out, "$name.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    /**
     * The Test tab's panel on its own over the sky, at the size it gets on a phone (with the nav pill's
     * clearance) or in the desktop editor's deck panel. [taps] are in pixels.
     */
    private fun shootHandPanel(name: String, width: Int, height: Int, dark: Boolean, phone: Boolean, taps: List<androidx.compose.ui.geometry.Offset> = emptyList()) {
        val deck = runBlocking { container.decks.currentSnapshot(deckId)!! }
        ImageComposeScene(width, height, Density(1.5f)) {
            app.lockon.ui.theme.LockOnTheme(dark = dark) {
                app.lockon.ui.components.SkyBackground(androidx.compose.ui.Modifier.fillMaxSize()) {
                    androidx.compose.runtime.CompositionLocalProvider(app.lockon.ui.components.LocalNavClearance provides if (phone) 84.dp else 0.dp) {
                        app.lockon.ui.decks.SampleHandPanel(deck, container.catalog.cards.value, androidx.compose.ui.Modifier.fillMaxSize())
                    }
                }
            }
        }.use { scene ->
            var time = 0L
            fun settle(frames: Int) = repeat(frames) { Thread.sleep(120); time += 100_000_000L; scene.render(time) }
            settle(10)
            for (tap in taps) {
                scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Move, tap, type = androidx.compose.ui.input.pointer.PointerType.Mouse)
                scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press, tap, type = androidx.compose.ui.input.pointer.PointerType.Mouse,
                    buttons = androidx.compose.ui.input.pointer.PointerButtons(isPrimaryPressed = true), button = androidx.compose.ui.input.pointer.PointerButton.Primary)
                scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release, tap, type = androidx.compose.ui.input.pointer.PointerType.Mouse,
                    button = androidx.compose.ui.input.pointer.PointerButton.Primary)
                settle(10)
            }
            File(out, "$name.png").writeBytes(scene.render(time).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    @Test fun renderTestHandPanel() {
        val keep = androidx.compose.ui.geometry.Offset(60f, 927f)
        val keepDesktop = androidx.compose.ui.geometry.Offset(60f, 992f)
        for (dark in listOf(true, false)) {
            val theme = if (dark) "dark" else "light"
            shootHandPanel("panel-phone-open-$theme", 600, 1095, dark, phone = true)
            shootHandPanel("panel-phone-kept-$theme", 600, 1095, dark, phone = true, taps = listOf(keep, keep))
            shootHandPanel("panel-desktop-open-$theme", 740, 1040, dark, phone = false)
            shootHandPanel("panel-desktop-kept-$theme", 740, 1040, dark, phone = false, taps = listOf(keepDesktop, keepDesktop))
            shootHandPanel("panel-phone-drawn-$theme", 600, 1095, dark, phone = true, taps = listOf(keep, keep, keep, keep))
            // Reveal sits at the end of the Shields header.
            shootHandPanel("panel-phone-revealed-$theme", 600, 1095, dark, phone = true, taps = listOf(keep, keep, androidx.compose.ui.geometry.Offset(518f, 672f)))
            shootHandPanel("panel-desktop-revealed-$theme", 740, 1040, dark, phone = false, taps = listOf(keepDesktop, keepDesktop, androidx.compose.ui.geometry.Offset(658f, 728f)))
        }
    }

    /** Sends part way, waiting and done, and a receive saving then saved. */
    @Test fun renderSendAndReceive() {
        val decks = listOf(
            DeckSnapshot(name = "Wing Control", entries = listOf(DeckEntry("SMP-003", 4))),
            DeckSnapshot(name = "Green test", entries = listOf(DeckEntry("SMP-015", 3))),
            DeckSnapshot(name = "Blue Red Aggro", entries = listOf(DeckEntry("SMP-001", 4))),
        )
        val sending = client.sharing.outgoing.value
        client.sharing.outgoing.value = sending + ("desk-2" to OutgoingShare("Studio iMac", 3, OutgoingShare.State.SENDING, progress = 0.6f))
        shoot("phone-sync-sending", 600, 1260, controller = client) { select(Tab.SYNC); syncSending = true }
        shoot("light-phone-sync-sending", 600, 1260, dark = false, controller = client) { select(Tab.SYNC); syncSending = true }
        shoot("desktop-sync-sending", 1980, 1290, controller = client) { select(Tab.SYNC); syncSending = true }
        client.sharing.outgoing.value = sending
        client.sharing.incoming.value = IncomingShare("s-2", "DESKTOP-VPP8OD2", decks, IncomingShare.State.SAVING, saved = 1)
        shoot("phone-receive-saving", 600, 1260, controller = client) { select(Tab.SYNC) }
        client.sharing.incoming.value = IncomingShare("s-2", "DESKTOP-VPP8OD2", decks, IncomingShare.State.SAVED, saved = 3)
        shoot("phone-receive-saved", 600, 1260, controller = client) { select(Tab.SYNC) }
        shoot("light-phone-receive-saved", 600, 1260, dark = false, controller = client) { select(Tab.SYNC) }
        client.sharing.incoming.value = null
    }

    /** The desktop waiting on a phone it invited, spinner beside Cancel. */
    @Test fun renderInviting() {
        sync.invited.value = setOf("phone-2")
        shoot("desktop-sync-inviting", 1980, 1290) { select(Tab.SYNC) }
        shoot("light-desktop-sync-inviting", 1980, 1290, dark = false) { select(Tab.SYNC) }
        shoot("phone-sync-inviting", 600, 1260) { select(Tab.SYNC) }
        sync.invited.value = emptySet()
    }

    /** Sync devices: a paired desktop and one nearby to pair, then the paired one just matched. */
    @Test fun renderDeviceSync() {
        shoot("phone-sync-devices", 600, 1260, controller = client) { select(Tab.SYNC) }
        shoot("light-phone-sync-devices", 600, 1260, dark = false, controller = client) { select(Tab.SYNC) }
        shoot("desktop-sync-devices-idle", 1980, 1290, controller = client) { select(Tab.SYNC) }
        shoot("desktop-sync-hover", 1980, 1290, controller = client, hover = androidx.compose.ui.geometry.Offset(1180f, 600f)) { select(Tab.SYNC) }
        shoot("light-desktop-sync-hover", 1980, 1290, dark = false, controller = client, hover = androidx.compose.ui.geometry.Offset(1180f, 600f)) { select(Tab.SYNC) }
        client.transfers.value = mapOf("desk-1" to app.lockon.sync.SyncTransfer("DESKTOP-VPP8OD2", app.lockon.sync.SyncMode.MERGE,
            app.lockon.sync.SyncTransfer.State.WORKING, null))
        shoot("phone-sync-working", 600, 1260, controller = client) { select(Tab.SYNC) }
        shoot("phone-sync-small", 540, 960, controller = client) { select(Tab.SYNC) }
        val summary = app.lockon.sync.MirrorSummary(updated = 3, added = 2, removed = 1, settings = true)
        client.transfers.value = mapOf("desk-1" to app.lockon.sync.SyncTransfer("DESKTOP-VPP8OD2", app.lockon.sync.SyncMode.TAKE,
            app.lockon.sync.SyncTransfer.State.DONE, app.lockon.sync.matchedMessage("This phone", "DESKTOP-VPP8OD2", summary)))
        shoot("phone-sync-matched", 600, 1260, controller = client, hover = androidx.compose.ui.geometry.Offset(5f, 5f)) { select(Tab.SYNC) }
        shoot("desktop-sync-devices", 1980, 1290, controller = client) { select(Tab.SYNC) }
        client.transfers.value = emptyMap()
    }

    @Test fun renderWelcomeTour() {
        repeat(6) { stop -> shoot("desktop-tour-$stop", 1980, 1290, dark = stop % 2 == 1) { touring = true; tourStop = stop } }
        shoot("phone-tour-1", 600, 1260, dark = false) { touring = true; tourStop = 1 }
        shoot("phone-tour-2", 600, 1260) { touring = true; tourStop = 2 }
    }

    /** Settings in both layouts and themes, and the phone editor's More menu open over the Deck page. */
    @Test fun renderPhoneDeckViews() {
        // A small phone, where the header's problem line has the least room, with a legal deck and one card short.
        // Hovering off to the side only gives the boot screen time to finish.
        val settled = androidx.compose.ui.geometry.Offset(1f, 1f)
        val short = runBlocking {
            val full = container.decks.liveDeck(deckId).filterNotNull().first().snapshot
            container.decks.create(full.withQuantity("SMP-001", DeckSection.MAIN, full.quantityOf("SMP-001", DeckSection.MAIN) - 1))
        }
        shoot("phone-editor-small", 540, 1200, hover = settled) { open(Screen.DeckEditor(deckId)) }
        shoot("phone-editor-small-short", 540, 1200, hover = settled) { open(Screen.DeckEditor(short)) }
        runBlocking { container.settings.setDeckAsCards(true) }
        shoot("phone-editor-card-images", 600, 1260, hover = settled) { open(Screen.DeckEditor(deckId)) }
        runBlocking { container.settings.setDeckAsCards(false) }
    }

    @Test fun renderSettingsAndMoreMenu() {
        shoot("desktop-settings", 1980, 1290) { select(Tab.SETTINGS) }
        shoot("light-desktop-settings", 1980, 1290, dark = false) { select(Tab.SETTINGS) }
        shoot("phone-settings", 600, 1260) { select(Tab.SETTINGS) }
        shoot("light-phone-settings", 600, 1260, dark = false) { select(Tab.SETTINGS) }
        shoot("phone-settings-full", 600, 3000) { select(Tab.SETTINGS) }
        shoot("light-phone-settings-full", 600, 3000, dark = false) { select(Tab.SETTINGS) }
        // More is the bar's last button: the pill spans most of the 400 dp phone, 42 dp above the bottom.
        val more = androidx.compose.ui.geometry.Offset(476f, 1260f - 1.5f * 42f)
        shoot("phone-more-menu", 600, 1260, taps = listOf(more)) { open(Screen.DeckEditor(deckId)) }
        shoot("light-phone-more-menu", 600, 1260, dark = false, taps = listOf(more)) { open(Screen.DeckEditor(deckId)) }
    }

    /** The phone deck editor, with a throwaway first shot since the first can catch the boot cover. */
    @Test fun renderPhoneEditor() {
        shoot("tmp-phone-first", 600, 1260) { open(Screen.DeckEditor(deckId)) }
        shoot("phone-editor", 600, 1260) { open(Screen.DeckEditor(deckId)) }
        shoot("light-phone-editor", 600, 1260, dark = false) { open(Screen.DeckEditor(deckId)) }
        shoot("phone-add-cards", 600, 1260) { open(Screen.DeckEditor(deckId, startTab = 1)) }
        shoot("light-phone-add-cards", 600, 1260, dark = false) { open(Screen.DeckEditor(deckId, startTab = 1)) }
        val filters = androidx.compose.ui.geometry.Offset(545f, 181f)
        // The way a person gets there: open the deck, tap Add in the bar, then Filters.
        val add = androidx.compose.ui.geometry.Offset(300f, 1198f)
        // Opened from the Decks list after the app has started, not restored while it boots.
        val row = androidx.compose.ui.geometry.Offset(350f, 640f)
        shoot("light-phone-add-filters-path", 600, 1260, dark = false, taps = listOf(row, add, filters)) { select(Tab.DECKS) }
        shoot("phone-add-filters-path", 600, 1260, taps = listOf(row, add, filters)) { select(Tab.DECKS) }
        shoot("phone-add-filters-picked", 600, 1260, taps = listOf(row, add, filters, androidx.compose.ui.geometry.Offset(105f, 642f), androidx.compose.ui.geometry.Offset(270f, 798f), androidx.compose.ui.geometry.Offset(335f, 798f))) { select(Tab.DECKS) }
        shoot("light-phone-add-filters-picked", 600, 1260, dark = false, taps = listOf(row, add, filters, androidx.compose.ui.geometry.Offset(105f, 642f), androidx.compose.ui.geometry.Offset(270f, 798f), androidx.compose.ui.geometry.Offset(335f, 798f))) { select(Tab.DECKS) }
        shoot("phone-small-filters", 540, 960, taps = listOf(androidx.compose.ui.geometry.Offset(485f, 181f))) { open(Screen.DeckEditor(deckId, startTab = 1)) }
        shoot("phone-card-details", 600, 1260, taps = listOf(androidx.compose.ui.geometry.Offset(110f, 330f))) { select(Tab.CARDS) }
        shoot("light-phone-card-details", 600, 1260, dark = false, taps = listOf(androidx.compose.ui.geometry.Offset(110f, 330f))) { select(Tab.CARDS) }
        shoot("phone-cards-closed", 600, 1260) { select(Tab.CARDS) }
        shoot("light-phone-cards-closed", 600, 1260, dark = false) { select(Tab.CARDS) }
        shoot("phone-cards-filters", 600, 1260, taps = listOf(androidx.compose.ui.geometry.Offset(545f, 114f))) { select(Tab.CARDS) }
        shoot("light-phone-cards-filters", 600, 1260, dark = false, taps = listOf(androidx.compose.ui.geometry.Offset(545f, 114f))) { select(Tab.CARDS) }
        shoot("phone-add-filters", 600, 1260, taps = listOf(filters), hover = androidx.compose.ui.geometry.Offset(5f, 5f)) { open(Screen.DeckEditor(deckId, startTab = 1)) }
        shoot("light-phone-add-filters", 600, 1260, dark = false, taps = listOf(filters), hover = androidx.compose.ui.geometry.Offset(5f, 5f)) { open(Screen.DeckEditor(deckId, startTab = 1)) }
    }

    /** One deck section at a time, for going through them: both themes, and a tall shot of the whole page. */
    @Test fun renderPhoneSections() {
        shoot("tmp-phone-first", 600, 1260) { open(Screen.DeckEditor(deckId)) }
        val rest = androidx.compose.ui.geometry.Offset(5f, 5f)
        val draw = androidx.compose.ui.geometry.Offset(300f, 1115f)
        shoot("phone-test-settled", 600, 1260, hover = rest) { open(Screen.DeckEditor(deckId, startTab = 3)) }
        shoot("light-phone-test-settled", 600, 1260, dark = false, hover = rest) { open(Screen.DeckEditor(deckId, startTab = 3)) }
        shoot("phone-test-long", 600, 1260, taps = listOf(draw, draw, draw)) { open(Screen.DeckEditor(deckId, startTab = 3)) }
        shoot("phone-test-small", 540, 960, hover = rest) { open(Screen.DeckEditor(deckId, startTab = 3)) }
        shoot("phone-matches-picked", 600, 1260, taps = listOf(androidx.compose.ui.geometry.Offset(380f, 985f)), hover = rest) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        shoot("light-phone-matches-picked", 600, 1260, dark = false, taps = listOf(androidx.compose.ui.geometry.Offset(380f, 985f)), hover = rest) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        shoot("phone-matches-small", 540, 960, hover = rest) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        listOf(2 to "analysis", 3 to "test", 4 to "matches", 5 to "versions").forEach { (tab, name) ->
            shoot("s-phone-$name", 600, 1260) { open(Screen.DeckEditor(deckId, startTab = tab)) }
            shoot("s-light-phone-$name", 600, 1260, dark = false) { open(Screen.DeckEditor(deckId, startTab = tab)) }
            shoot("s-light-phone-$name-full", 600, 2800, dark = false) { open(Screen.DeckEditor(deckId, startTab = tab)) }
            // Cards taken out, so the list shows a loss as well as gains.
        runBlocking {
            val now = container.decks.currentSnapshot(deckId)!!
            val fewer = now.withQuantity("SMP-001", DeckSection.MAIN, 2)
            container.decks.keepVersion(deckId, fewer)
            container.decks.keepVersion(deckId, fewer.withQuantity("SMP-002", DeckSection.MAIN, 3))
        }
        shoot("phone-versions-losses", 600, 1260, hover = rest) { open(Screen.DeckEditor(deckId, startTab = 5)) }
        shoot("light-phone-versions-losses", 600, 1260, dark = false, hover = rest) { open(Screen.DeckEditor(deckId, startTab = 5)) }
        shoot("light-phone-versions-open", 600, 1260, dark = false, taps = listOf(androidx.compose.ui.geometry.Offset(300f, 245f)), hover = rest) { open(Screen.DeckEditor(deckId, startTab = 5)) }
        shoot("phone-versions-open", 600, 1260, taps = listOf(androidx.compose.ui.geometry.Offset(300f, 245f)), hover = rest) { open(Screen.DeckEditor(deckId, startTab = 5)) }
    }
    }

    /**
     * The filter sheet with the ranges the real card database has (level and cost to 10, AP and HP 0 to 7):
     * the bundled sample stops short of them, and the strips list what the cards have.
     */
    @Test fun renderPhoneFilterRanges() {
        val cards = container.catalog.cards.value.values.toList()
        val top = cards.first { it.cost != null }.copy(cardNumber = "RNG-001", level = 10, cost = 10, ap = 7, hp = 7)
        val low = top.copy(cardNumber = "RNG-002", level = 1, cost = 1, ap = 0, hp = 0)
        runBlocking { container.catalog.install(CatalogFile(catalogId = "range-test", version = 1, name = "Range test", cards = cards + top + low, printings = emptyList()), CatalogOrigin.FILE, null) }
        shoot("tmp-phone-first", 600, 1260) { select(Tab.DECKS) }
        val row = androidx.compose.ui.geometry.Offset(350f, 640f)
        val add = androidx.compose.ui.geometry.Offset(300f, 1198f)
        val filters = androidx.compose.ui.geometry.Offset(545f, 181f)
        shoot("phone-filters-real", 600, 1260, taps = listOf(row, add, filters)) { select(Tab.DECKS) }
        shoot("light-phone-filters-real", 600, 1260, dark = false, taps = listOf(row, add, filters)) { select(Tab.DECKS) }
        shoot("phone-small-filters-real", 540, 960, taps = listOf(androidx.compose.ui.geometry.Offset(485f, 181f))) { open(Screen.DeckEditor(deckId, startTab = 1)) }
        shoot("light-phone-cards-filters-real", 600, 1260, dark = false, taps = listOf(androidx.compose.ui.geometry.Offset(545f, 114f))) { select(Tab.CARDS) }
        runBlocking { container.catalog.restoreBundled(File("../shared/src/commonMain/composeResources/files/catalog/sample-catalog.json").readText()) }
    }

    /** The phone Decks screen, and its + opened. */
    /** The header on every page of a deck on a phone. */
    @Test fun renderPhoneDeckHeaders() {
        shoot("tmp-phone-first", 600, 1260) { select(Tab.DECKS) }
        (0..7).forEach { tab ->
            shoot("head-phone-$tab", 600, 1260, hover = androidx.compose.ui.geometry.Offset(5f, 5f)) { open(Screen.DeckEditor(deckId, startTab = tab)) }
        }
        shoot("head-light-phone-0", 600, 1260, dark = false, hover = androidx.compose.ui.geometry.Offset(5f, 5f)) { open(Screen.DeckEditor(deckId, startTab = 0)) }
    }

    /** What desktop took from the phone review. */
    /** The desktop's controls side by side: the editor's two panels, the card browser and the deck list, in both themes. */
    @Test fun renderDesktopButtons() {
        val rest = androidx.compose.ui.geometry.Offset(5f, 700f)
        for (dark in listOf(true, false)) {
            val t = if (dark) "dark" else "light"
            shoot("btn-editor-$t", 1980, 1290, dark = dark, hover = rest) { open(Screen.DeckEditor(deckId)) }
            shoot("btn-cards-$t", 1980, 1290, dark = dark, hover = rest) { select(Tab.CARDS) }
            shoot("btn-decks-$t", 1980, 1290, dark = dark, hover = rest) { select(Tab.DECKS) }
            // Deck colors off and scrolled: cards with room for more copies, so + is teal, and − disabled at zero.
            shoot("btn-steppers-$t", 1980, 1290, dark = dark, taps = listOf(androidx.compose.ui.geometry.Offset(1015f, 266f)),
                scrollAt = androidx.compose.ui.geometry.Offset(600f, 800f), hover = rest) { open(Screen.DeckEditor(deckId)) }
            shoot("btn-filters-$t", 1980, 1290, dark = dark, taps = listOf(androidx.compose.ui.geometry.Offset(795f, 192f)), hover = rest) { open(Screen.DeckEditor(deckId)) }
            // A + under the pointer, to see the hover step.
            shoot("btn-hover-$t", 1980, 1290, dark = dark, hover = androidx.compose.ui.geometry.Offset(216f, 649f)) { open(Screen.DeckEditor(deckId)) }
        }
    }

    /** The desktop selection outline: a hovered deck tile, card and deck row, and a selected card, in both themes. */
    @Test fun renderSelectionEdges() {
        for (dark in listOf(true, false)) {
            val t = if (dark) "dark" else "light"
            shoot("edge-deck-$t", 1980, 1290, dark = dark, hover = androidx.compose.ui.geometry.Offset(580f, 500f)) { select(Tab.DECKS) }
            shoot("edge-card-$t", 1980, 1290, dark = dark, taps = listOf(androidx.compose.ui.geometry.Offset(300f, 460f))) { select(Tab.CARDS) }
            shoot("edge-editor-$t", 1980, 1290, dark = dark, hover = androidx.compose.ui.geometry.Offset(140f, 460f)) { open(Screen.DeckEditor(deckId)) }
        }
    }

    @Test fun renderDesktopParity() {
        shoot("tmp-desktop-first", 1980, 1290) { select(Tab.DECKS) }
        val rest = androidx.compose.ui.geometry.Offset(5f, 700f)
        for (dark in listOf(true, false)) {
            val t = if (dark) "dark" else "light"
            shoot("par-settings-$t", 1980, 1290, dark = dark, hover = rest) { select(Tab.SETTINGS) }
            shoot("par-sync-$t", 1980, 1290, dark = dark, controller = client, hover = rest) { select(Tab.SYNC) }
            shoot("par-filters-$t", 1980, 1290, dark = dark, taps = listOf(androidx.compose.ui.geometry.Offset(930f, 195f)), hover = rest) { open(Screen.DeckEditor(deckId)) }
            shoot("par-details-$t", 1980, 1290, dark = dark, taps = listOf(androidx.compose.ui.geometry.Offset(140f, 460f)), hover = rest) { open(Screen.DeckEditor(deckId)) }
            shoot("par-versions-$t", 1980, 1290, dark = dark, taps = listOf(androidx.compose.ui.geometry.Offset(1880f, 243f)), hover = rest) { open(Screen.DeckEditor(deckId)) }
        }
    }

    @Test fun renderPhoneDecks() {
        val plus = androidx.compose.ui.geometry.Offset(531f, 1068f)
        shoot("phone-library", 600, 1260) { select(Tab.DECKS) }
        shoot("light-phone-library", 600, 1260, dark = false) { select(Tab.DECKS) }
        shoot("phone-decks-actions", 600, 1260, taps = listOf(plus)) { select(Tab.DECKS) }
        shoot("light-phone-decks-actions", 600, 1260, dark = false, taps = listOf(plus)) { select(Tab.DECKS) }
    }

    /** New deck, drawn on its own because it opens from a button. */
    @Test fun renderNewDeck() {
        for (dark in listOf(false, true)) {
            ImageComposeScene(1440, 900, Density(1f)) {
                app.lockon.ui.theme.LockOnTheme(dark = dark) {
                    androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                        app.lockon.ui.io.NewDeckDialog(container, files, AppState(CoroutineScope(Dispatchers.Default)), onDismiss = {}, folders = listOf("Store championship", "Testing"), folder = "Testing")
                    }
                }
            }.use { scene ->
                var image = scene.render()
                repeat(10) { frame -> Thread.sleep(60); image = scene.render((frame + 1) * 100_000_000L) }
                File(out, "new-deck-${if (dark) "dark" else "light"}.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
    }

    /** Screens that need a state the app rarely reaches, each drawn on its own. */
    @Test fun renderRareScreens() {
        fun draw(name: String, width: Int, height: Int, content: @Composable () -> Unit) {
            ImageComposeScene(width, height, Density(1f)) {
                app.lockon.ui.theme.LockOnTheme(dark = false) {
                    androidx.compose.material3.Surface(color = androidx.compose.ui.graphics.Color(0xFFD6ECFA)) { content() }
                }
            }.use { scene ->
                var image = scene.render()
                repeat(10) { frame -> Thread.sleep(60); image = scene.render((frame + 1) * 100_000_000L) }
                File(out, "$name.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
        draw("phone-no-decks", 390, 844) { app.lockon.ui.decks.NoDecks({}, {}, {}) }
        val deck = runBlocking { container.decks.currentSnapshot(deckId)!! }
        val changed = deck.withQuantity("SMP-012", DeckSection.MAIN, 2)
        val cards = container.catalog.cards.value
        val mine = app.lockon.model.DeckVersion("a", "here", 1_761_400_000_000, deck)
        val theirs = app.lockon.model.DeckVersion("b", "there", 1_761_300_000_000, changed)
        draw("desktop-compare-card", 620, 460) {
            app.lockon.ui.decks.VersionCard(mine, listOf(theirs), cards, "This device", fromComputer = true, androidx.compose.ui.Modifier.padding(16.dp), onKeep = {})
        }
        sync.pairingPrompt.value = app.lockon.sync.PairingPrompt("r1", "Pixel 9", "482913", false)
        draw("desktop-pairing", 1440, 900) { app.lockon.ui.sync.SyncPrompts(sync) }
        sync.pairingPrompt.value = null
    }

    /**
     * Deck tiles in every state, with the widest last line: two colours, a double-digit record and
     * "Yesterday". Uses downloaded artwork when this machine has it, since the crop is what's judged.
     */
    @Test fun renderDeckTiles() = runBlocking {
        val cards = container.catalog.cards.value.values.toList()
        val cached = File("build/artwork-cache").listFiles().orEmpty().filter { it.isFile }.sorted()
        // A blue and a red card get no art, so a deck of them shows the tile without any.
        val noArtBlue = cards.last { app.lockon.model.CardColor.BLUE in it.colors }.cardNumber
        val noArtRed = cards.last { app.lockon.model.CardColor.RED in it.colors }.cardNumber
        val printings = cards.filter { it.cardNumber != noArtBlue && it.cardNumber != noArtRed }.mapIndexed { i, card ->
            val url = cached.getOrNull(i * 37)?.let { source ->
                val copy = File("build/test-art/tile-$i.webp").apply { parentFile.mkdirs() }
                "file:///" + source.copyTo(copy, overwrite = true).absolutePath.replace('\\', '/')
            } ?: fakeArt("tile-$i", java.awt.Color(0x2A5BB8))
            Printing(card.cardNumber, card.cardNumber, "SMP", "R", artworkUrl = url)
        }
        container.catalog.install(CatalogFile(catalogId = "tile-test", version = 1, name = "Tile test", cards = cards, printings = printings), CatalogOrigin.FILE, null)

        val blue = cards.first { app.lockon.model.CardColor.BLUE in it.colors }.cardNumber
        val red = cards.first { app.lockon.model.CardColor.RED in it.colors }.cardNumber
        
        val full = container.decks.currentSnapshot(deckId)!!.entries
        val record = List(11) { MatchRecord("w$it", 1_761_400_000_000, MatchResult.WIN) } + List(4) { MatchRecord("l$it", 1_761_400_000_000, MatchResult.LOSS) }
        val day = 86_400_000L
        var serial = 0
        suspend fun seed(id: String, name: String, entries: List<DeckEntry>, matches: List<MatchRecord> = emptyList(), ago: Long = 0L) {
            val created = System.currentTimeMillis() - ago
            container.decks.applyIncoming(listOf(RevisionDto("tile-${serial++}", id, emptyList(), "other-device", created,
                snapshot = DeckSnapshot(name = name, entries = entries, matches = matches))), "other-device")
        }
        seed("full", "friends with the monster", full, ago = 120_000)
        seed("draft", "Wing Control", listOf(DeckEntry(blue, 4), DeckEntry(red, 4)) + List(9) { DeckEntry(cards[it + 2].cardNumber, 3) }.distinctBy { it.cardNumber }, record, day)
        seed("empty", "New deck", emptyList())
        seed("one", "One card so far", listOf(DeckEntry(blue, 1)))
        seed("two", "Two cards so far", listOf(DeckEntry(blue, 2), DeckEntry(red, 2)))
        seed("review", "Store championship list, second revision", full, record, day)
        seed("review", "Store championship list, second revision", full.drop(1), record, day)
        seed("noart", "No art downloaded", listOf(DeckEntry(noArtBlue, 4), DeckEntry(noArtRed, 4)), ago = 5 * day)
        seed("over", "Over the limit", full + DeckEntry("SMP-012", 3), record, day)
        seed("legal", "Newtype Control", full, record, day)

        shoot("desktop-tiles", 1980, 1290, hover = androidx.compose.ui.geometry.Offset(1380f, 300f)) { select(Tab.DECKS) }
        shoot("light-desktop-tiles", 1980, 1290, dark = false, hover = androidx.compose.ui.geometry.Offset(1380f, 300f)) { select(Tab.DECKS) }
        container.catalog.restoreBundled(File("../shared/src/commonMain/composeResources/files/catalog/sample-catalog.json").readText())
    }

    /**
     * The Matches page for a design council: desktop and phone, both themes, plus a small phone and a
     * tall phone page. Designers may add shots of their own states (editing a game) after these.
     */
    @Test fun renderMatchesCouncil() {
        val rest = androidx.compose.ui.geometry.Offset(5f, 5f)
        shoot("council-desktop-matches", 1980, 1290) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        shoot("council-light-desktop-matches", 1980, 1290, dark = false) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        shoot("council-phone-matches", 600, 1260, hover = rest) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        shoot("council-light-phone-matches", 600, 1260, dark = false, hover = rest) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        shoot("council-phone-matches-small", 540, 960, hover = rest) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        shoot("council-light-phone-matches-full", 600, 2800, dark = false, hover = rest) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        // Tapping a game opens it for editing.
        shoot("council-phone-matches-edit", 600, 1260, taps = listOf(androidx.compose.ui.geometry.Offset(250f, 705f)), hover = rest) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        shoot("council-light-desktop-matches-edit", 1980, 1290, dark = false, taps = listOf(androidx.compose.ui.geometry.Offset(1500f, 987f))) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        // Last, since these log a game.
        // Just after logging a win: the new game is lit at the top of the list.
        shoot("council-phone-matches-logged", 600, 1260, taps = listOf(androidx.compose.ui.geometry.Offset(123f, 1080f))) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        shoot("council-desktop-matches-logged", 1980, 1290, taps = listOf(androidx.compose.ui.geometry.Offset(1372f, 593f))) { open(Screen.DeckEditor(deckId, startTab = 4)) }
    }

    /**
     * The quantity controls for a design council: the deck view and Add cards, on a phone (drawn with
     * phone controls) in both themes, with the deck as strips and as card images, and on desktop.
     */
    @Test fun renderQuantityCouncil() {
        val rest = androidx.compose.ui.geometry.Offset(5f, 5f)
        shoot("tmp-qty-first", 600, 1260, hover = rest) { open(Screen.DeckEditor(deckId)) }
        app.lockon.ui.components.previewPhoneControls = true
        try {
            for (dark in listOf(true, false)) {
                val theme = if (dark) "dark" else "light"
                shoot("qty-phone-deck-$theme", 600, 1260, dark = dark, hover = rest) { open(Screen.DeckEditor(deckId)) }
                shoot("qty-phone-add-$theme", 600, 1260, dark = dark, hover = rest) { open(Screen.DeckEditor(deckId, startTab = 1)) }
                runBlocking { container.settings.setDeckAsCards(true) }
                shoot("qty-phone-deck-cards-$theme", 600, 1260, dark = dark, hover = rest) { open(Screen.DeckEditor(deckId)) }
                runBlocking { container.settings.setDeckAsCards(false) }
                // Add cards as a list: Filters, the list toggle, then Show.
                val asList = listOf(androidx.compose.ui.geometry.Offset(545f, 181f), androidx.compose.ui.geometry.Offset(530f, 720f), androidx.compose.ui.geometry.Offset(362f, 1188f))
                shoot("qty-phone-add-list-$theme", 600, 1260, dark = dark, taps = asList, hover = rest) { open(Screen.DeckEditor(deckId, startTab = 1)) }
            }
        } finally {
            app.lockon.ui.components.previewPhoneControls = false
        }
        shoot("qty-desktop-dark", 1980, 1290, hover = rest) { open(Screen.DeckEditor(deckId)) }
        shoot("qty-desktop-light", 1980, 1290, dark = false, hover = rest) { open(Screen.DeckEditor(deckId)) }
        // The card list as a list, and the deck panel as card images.
        val listToggle = androidx.compose.ui.geometry.Offset(1153f, 183f)
        val deckCards = androidx.compose.ui.geometry.Offset(1907f, 321f)
        shoot("qty-desktop-lists-dark", 1980, 1290, taps = listOf(listToggle, deckCards), hover = rest) { open(Screen.DeckEditor(deckId)) }
        shoot("qty-desktop-lists-light", 1980, 1290, dark = false, taps = listOf(listToggle, deckCards), hover = rest) { open(Screen.DeckEditor(deckId)) }
    }

    /** The Card database dialog with the official list installed, as on a computer and a phone, in both themes. */
    @Test fun renderCatalogDialog() {
        val cards = container.catalog.cards.value.values.toList()
        runBlocking {
            container.catalog.install(
                CatalogFile(catalogId = "gcg-api", version = 31, name = "Official card list", source = app.lockon.catalog.GcgApi.ATTRIBUTION + " Dataset built 2026-11-02.", cards = cards, printings = emptyList()),
                CatalogOrigin.GCG_API, null,
            )
        }
        fun render(file: String, size: Pair<Int, Int>, dark: Boolean, sources: Boolean = false) =
            ImageComposeScene(size.first, size.second, Density(1.5f)) {
                app.lockon.ui.theme.LockOnTheme(dark = dark) {
                    androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                        app.lockon.ui.cards.CatalogDialog(container, AppState(CoroutineScope(Dispatchers.Default)), files, onDismiss = {}, showOtherSources = sources)
                    }
                }
            }.use { scene ->
                var image = scene.render()
                repeat(10) { frame -> Thread.sleep(60); image = scene.render((frame + 1) * 100_000_000L) }
                File(out, "$file.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        for ((name, size) in listOf("desktop" to (1980 to 1290), "phone" to (600 to 1260))) for (dark in listOf(true, false)) {
            ImageComposeScene(size.first, size.second, Density(1.5f)) {
                app.lockon.ui.theme.LockOnTheme(dark = dark) {
                    androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                        app.lockon.ui.cards.CatalogDialog(container, AppState(CoroutineScope(Dispatchers.Default)), files, onDismiss = {})
                    }
                }
            }.use { scene ->
                var image = scene.render()
                repeat(10) { frame -> Thread.sleep(60); image = scene.render((frame + 1) * 100_000_000L) }
                File(out, "catalog-${if (dark) "" else "light-"}$name.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
        // Other sources opened.
        render("catalog-sources-desktop", 1980 to 1290, true, sources = true)
        render("catalog-sources-phone", 600 to 1260, true, sources = true)
        runBlocking { container.catalog.restoreBundled(File("../shared/src/commonMain/composeResources/files/catalog/sample-catalog.json").readText()) }
        // On the built-in sample, where getting the official list is the thing to do.
        render("catalog-sample-desktop", 1980 to 1290, true)
        render("catalog-sample-phone", 600 to 1260, true)
    }

    /** The deck panel's other tabs on a computer, in both themes. */
    @Test fun renderDeckPanelTabs() {
        for ((name, tab) in listOf("analysis" to 2, "matches" to 4, "versions" to 5)) {
            shoot("desktop-$name", 1980, 1290) { open(Screen.DeckEditor(deckId, startTab = tab)) }
            shoot("light-desktop-$name", 1980, 1290, dark = false) { open(Screen.DeckEditor(deckId, startTab = tab)) }
        }
    }

    @Test fun renderScreens() {
        shoot("desktop-library", 1980, 1290) { select(Tab.DECKS) }
        shoot("desktop-editor", 1980, 1290) { open(Screen.DeckEditor(deckId)) }
        shoot("desktop-sync", 1980, 1290) { select(Tab.SYNC) }
        shoot("desktop-settings", 1980, 1290) { select(Tab.SETTINGS) }
        shoot("desktop-cards", 1980, 1290) { select(Tab.CARDS) }
        shoot("light-phone-settings", 600, 1260, dark = false) { select(Tab.SETTINGS) }
        shoot("phone-library", 600, 1260) { select(Tab.DECKS) }
        shoot("phone-editor", 600, 1260) { open(Screen.DeckEditor(deckId)) }
        shoot("phone-cards", 600, 1260) { select(Tab.CARDS) }
        shoot("phone-add-cards", 600, 1260) { open(Screen.DeckEditor(deckId, startTab = 1)) }
        shoot("phone-side-deck", 600, 3200) { open(Screen.DeckEditor(deckId)) }
        shoot("phone-test-hand", 600, 1260) { open(Screen.DeckEditor(deckId, startTab = 3)) }
        shoot("light-phone-test-hand", 600, 1260, dark = false) { open(Screen.DeckEditor(deckId, startTab = 3)) }
        shoot("desktop-test-hand", 1980, 1290) { open(Screen.DeckEditor(deckId, startTab = 3)) }
        shoot("light-desktop-test-hand", 1980, 1290, dark = false) { open(Screen.DeckEditor(deckId, startTab = 3)) }
        shoot("phone-matches", 600, 1260) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        shoot("phone-versions", 600, 1260) { open(Screen.DeckEditor(deckId, startTab = 5)) }
        shoot("light-desktop-editor", 1980, 1290, dark = false) { open(Screen.DeckEditor(deckId)) }
        shoot("light-desktop-library", 1980, 1290, dark = false) { select(Tab.DECKS) }
        shoot("desktop-narrow-library", 1200, 1290) { select(Tab.DECKS) }
        shoot("light-phone-library", 600, 1260, dark = false) { select(Tab.DECKS) }
        shoot("light-phone-sync", 600, 1260, dark = false, controller = client) { select(Tab.SYNC) }
        shoot("light-phone-matches", 600, 1260, dark = false) { open(Screen.DeckEditor(deckId, startTab = 4)) }
        shoot("phone-analysis", 600, 1260) { open(Screen.DeckEditor(deckId, startTab = 2)) }
        shoot("light-phone-editor", 600, 1260, dark = false) { open(Screen.DeckEditor(deckId)) }
        shoot("light-phone-cards", 600, 1260, dark = false) { select(Tab.CARDS) }
        shoot("light-phone-add-cards", 600, 1260, dark = false) { open(Screen.DeckEditor(deckId, startTab = 1)) }
        sync.status.value = SyncStatus(message = "Could not reach the desktop. Check that Lock On is open on it.", isError = true)
        shoot("phone-sync-error", 600, 1260) { select(Tab.SYNC) }
        shoot("phone-sync-client", 600, 1260, controller = client) { select(Tab.SYNC) }
        shoot("phone-sync-receive", 600, 1260, controller = client) { select(Tab.SYNC); syncSending = false }
        shoot("desktop-sync-receive", 1980, 1290) { select(Tab.SYNC); syncSending = false }
        client.sharing.incoming.value = IncomingShare("s-1", "DESKTOP-VPP8OD2", listOf(
            DeckSnapshot(name = "Wing Control", entries = listOf(DeckEntry("SMP-003", 4))),
            DeckSnapshot(name = "Green test", entries = listOf(DeckEntry("SMP-015", 3))),
        ))
        shoot("phone-receive-decks", 600, 1260, controller = client) { select(Tab.SYNC) }
        client.sharing.incoming.value = null
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
        // The export dialog at phone width, on its List tab.
        run {
            val d = runBlocking { container.decks.currentSnapshot(deckId)!! }
            ImageComposeScene(600, 1260, Density(1.5f)) {
                app.lockon.ui.theme.LockOnTheme(dark = true) {
                    androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                        app.lockon.ui.decks.DeckExportDialog(d, container.catalog.cards.value, container.catalog.ruleset.value, files,
                            AppState(CoroutineScope(Dispatchers.Default)), onDismiss = {}, startTab = 0)
                    }
                }
            }.use { scene ->
                var image = scene.render()
                repeat(10) { frame -> Thread.sleep(60); image = scene.render((frame + 1) * 100_000_000L) }
                File(out, "phone-export.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
        // Card details at phone width, with deck controls, so the condensed layout can be checked.
        run {
            val card = container.catalog.cards.value.getValue("SMP-001")
            ImageComposeScene(600, 1260, Density(1.5f)) {
                app.lockon.ui.theme.LockOnTheme(dark = true) {
                    androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                        app.lockon.ui.components.CardDetailDialog(card, emptyList(), onDismiss = {}) {
                            androidx.compose.foundation.layout.Column {
                                app.lockon.ui.components.PlateStepper(4, "copy", onMinus = {}, onPlus = {}, limit = 4)
                                app.lockon.ui.components.PlateStepper(0, "side deck copy", onMinus = {}, onPlus = {})
                            }
                        }
                    }
                }
            }.use { scene ->
                var image = scene.render()
                repeat(10) { frame -> Thread.sleep(60); image = scene.render((frame + 1) * 100_000_000L) }
                File(out, "phone-card-detail.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
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
        // The export dialog at a full desktop window, where the whole image fits, and the image on a phone.
        exportShot("desktop-export-image-window", 1980, 1290, deck, tab = 1)
        exportShot("desktop-export-list-window", 1980, 1290, deck, tab = 0)
        exportShot("phone-export-image", 600, 1260, deck, tab = 1)
        assertTrue(File(out, "phone-cards.png").length() > 0)
    }

    /** The phone export in both themes: the editor's Export page and the dialog the library opens. */
    @Test fun renderCardFilters() {
        val at = { x: Int, y: Int -> androidx.compose.ui.geometry.Offset(x.toFloat(), y.toFloat()) }
        shoot("desktop-card-filters", 1980, 1290, taps = listOf(at(910, 195))) { select(Tab.CARDS) }
        shoot("phone-card-filters", 600, 1260, taps = listOf(at(528, 126))) { select(Tab.CARDS) }
        shoot("light-phone-card-filters", 600, 1260, dark = false, taps = listOf(at(528, 126), at(328, 463), at(399, 463), at(258, 340))) { select(Tab.CARDS) }
        shoot("desktop-card-filters-picked", 1980, 1290, taps = listOf(at(910, 195), at(407, 398), at(495, 398), at(285, 341))) { select(Tab.CARDS) }
        shoot("phone-cards-scrolled", 600, 900, scrollAt = at(300, 500)) { select(Tab.CARDS) }
    }

    @Test fun renderPhoneExport() {
        shoot("phone-export-page", 600, 1260) { open(Screen.DeckEditor(deckId, startTab = 6)) }
        shoot("light-phone-export-page", 600, 1260, dark = false) { open(Screen.DeckEditor(deckId, startTab = 6)) }
        val deck = runBlocking { container.decks.currentSnapshot(deckId)!! }
        exportShot("phone-export-list", 600, 1260, deck, tab = 0)
        exportShot("light-phone-export-list", 600, 1260, deck, tab = 0, dark = false)
        exportShot("light-phone-export-image", 600, 1260, deck, tab = 1, dark = false)
        exportShot("phone-export-qr", 600, 1260, deck, tab = 2)
        exportShot("light-phone-export-qr", 600, 1260, deck, tab = 2, dark = false)
    }

    private fun exportShot(name: String, width: Int, height: Int, deck: DeckSnapshot, tab: Int, dark: Boolean = true) {
        ImageComposeScene(width, height, Density(1.5f)) {
            app.lockon.ui.theme.LockOnTheme(dark = dark) {
                androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    app.lockon.ui.decks.DeckExportDialog(
                        deck, container.catalog.cards.value, container.catalog.ruleset.value, files,
                        AppState(CoroutineScope(Dispatchers.Default)), onDismiss = {}, startTab = tab, qrTab = tab == 2,
                    )
                }
            }
        }.use { scene ->
            var image = scene.render()
            repeat(10) { frame -> Thread.sleep(60); image = scene.render((frame + 1) * 100_000_000L) }
            File(out, "$name.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    /**
     * Holds on the export image to open it large, then puts it away, writing frames of both moves into
     * build/animation-frames/<name>: the desktop dialog, and the phone editor's Export page.
     */
    @Test fun zoomViewFrames() {
        val deck = runBlocking { container.decks.currentSnapshot(deckId)!! }
        val cards = container.catalog.cards.value
        val ruleset = container.catalog.ruleset.value
        zoomFrames("zoom-desktop", 1980, 1290, at = androidx.compose.ui.geometry.Offset(750f, 700f), closeAt = androidx.compose.ui.geometry.Offset(60f, 640f)) {
            app.lockon.ui.decks.DeckExportDialog(deck, cards, ruleset, files, AppState(CoroutineScope(Dispatchers.Default)), onDismiss = {}, startTab = 1)
        }
        zoomFrames("zoom-phone", 600, 1260, at = androidx.compose.ui.geometry.Offset(300f, 700f), closeAt = androidx.compose.ui.geometry.Offset(549f, 45f),
            first = androidx.compose.ui.geometry.Offset(450f, 232f)) {
            app.lockon.ui.components.SkyBackground(androidx.compose.ui.Modifier.fillMaxSize()) {
                androidx.compose.runtime.CompositionLocalProvider(app.lockon.ui.components.LocalNavClearance provides 84.dp) {
                    app.lockon.ui.decks.DeckExportPanel(deck, cards, ruleset, files, AppState(CoroutineScope(Dispatchers.Default)),
                        androidx.compose.ui.Modifier.fillMaxSize().padding(top = 120.dp))
                }
            }
        }
    }

    /** Holds at [at] past the hold time, records the opening, then clicks [closeAt] and records the closing. */
    private fun zoomFrames(name: String, width: Int, height: Int, at: androidx.compose.ui.geometry.Offset, closeAt: androidx.compose.ui.geometry.Offset, first: androidx.compose.ui.geometry.Offset? = null, content: @androidx.compose.runtime.Composable () -> Unit) {
        val dir = File("build/animation-frames/$name").apply { deleteRecursively(); mkdirs() }
        ImageComposeScene(width, height, Density(1.5f)) {
            app.lockon.ui.theme.LockOnTheme(dark = true) {
                androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) { content() }
            }
        }.use { scene ->
            var time = 0L
            var n = 0
            fun frame(save: Boolean) {
                Thread.sleep(20); time += 16_666_667L
                val image = scene.render(time)
                if (save) File(dir, "frame-%03d.png".format(n++)).writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
            repeat(40) { Thread.sleep(40); time += 100_000_000L; scene.render(time) }
            val mouse = androidx.compose.ui.input.pointer.PointerType.Mouse
            val primary = androidx.compose.ui.input.pointer.PointerButton.Primary
            // A click to set the page up first, such as switching it to the image.
            if (first != null) {
                scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press, first, type = mouse,
                    buttons = androidx.compose.ui.input.pointer.PointerButtons(isPrimaryPressed = true), button = primary)
                scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release, first, type = mouse, button = primary)
                repeat(10) { Thread.sleep(40); time += 100_000_000L; scene.render(time) }
            }
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Move, at, type = mouse)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press, at, type = mouse,
                buttons = androidx.compose.ui.input.pointer.PointerButtons(isPrimaryPressed = true), button = primary)
            // The hold, then the opening.
            repeat(40) { frame(it % 2 == 0) }
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release, at, type = mouse, button = primary)
            repeat(10) { frame(it == 9) }
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Move, closeAt, type = mouse)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press, closeAt, type = mouse,
                buttons = androidx.compose.ui.input.pointer.PointerButtons(isPrimaryPressed = true), button = primary)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release, closeAt, type = mouse, button = primary)
            repeat(24) { frame(it % 2 == 0) }
        }
        assertTrue(File(dir, "frame-000.png").exists())
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
        // The app's scope runs on the thread that draws the window. Here that is this thread, between
        // frames: capturing the image from another thread while a frame draws crashes or hangs.
        val uiThread = FrameThreadDispatcher()
        ImageComposeScene(1200, 1400, Density(1.5f)) {
            app.lockon.ui.theme.LockOnTheme(dark = true) {
                app.lockon.ui.decks.DeckExportDialog(
                    deck, container.catalog.cards.value, container.catalog.ruleset.value, capturing,
                    AppState(CoroutineScope(uiThread)), onDismiss = {}, startTab = 1,
                )
            }
        }.use { scene ->
            repeat(5) { frame -> Thread.sleep(60); scene.render((frame + 1) * 100_000_000L) }
            File(out, "desktop-export-image-before-save.png").writeBytes(scene.render(600_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            val button = androidx.compose.ui.geometry.Offset(905f, 550f)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press, button)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release, button)
            // Frames keep coming until the image arrives, as they would in a window.
            var frame = 6L
            val deadline = System.nanoTime() + 60_000_000_000L
            while (!saved.isDone && System.nanoTime() < deadline) {
                uiThread.runPending()
                Thread.sleep(16)
                scene.render(frame++ * 100_000_000L)
            }
            val png = saved.get(1, java.util.concurrent.TimeUnit.SECONDS)
            // Kept beside the screenshots, so the saved picture itself can be looked at.
            File(out, "saved-deck-image.png").writeBytes(png)
            val image = org.jetbrains.skia.Image.makeFromEncoded(png)
            kotlin.test.assertEquals(2160, image.width)
            assertTrue(image.height > 2160, "the whole deck is in the image, not just what the dialog shows")
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

    /**
     * Game feedback on one card tile, laid out on its own so the positions below follow from the
     * tile's size and not from the app around it: the pointer rests on the card so it lifts and
     * tilts, then + adds a copy and the card flashes. Frames are caught mid-animation.
     */
    @Test fun renderFeedback() {
        val card = container.catalog.cards.value.getValue("SMP-001")
        val copies = androidx.compose.runtime.mutableStateOf(0)
        // 240 dp wide at 1.5×: the card is 240 × 88/63 dp tall, then a 6 dp gap and the 48 dp control plate.
        val width = 240f * 1.5f
        val cardHeight = 240f * 88f / 63f * 1.5f
        val margin = 40f * 1.5f
        ImageComposeScene((width + margin * 2).toInt(), (cardHeight + 54f * 1.5f + margin * 2).toInt(), Density(1.5f)) {
            app.lockon.ui.theme.LockOnTheme(dark = true) {
                app.lockon.ui.components.SkyBackground(androidx.compose.ui.Modifier.fillMaxSize()) {
                    app.lockon.ui.components.CardTile(
                        card, onClick = {}, modifier = androidx.compose.ui.Modifier.padding(40.dp).width(240.dp),
                        quantity = copies.value, onMinus = { copies.value-- }, onPlus = { copies.value++ }, limit = 4,
                    )
                }
            }
        }.use { scene ->
            var time = 0L
            fun frames(n: Int) = repeat(n) { Thread.sleep(40); time += 16_000_000L; scene.render(time) }
            frames(10)
            // The card's upper-left quarter, so the tilt leans that way.
            val aim = androidx.compose.ui.geometry.Offset(margin + width * 0.25f, margin + cardHeight * 0.25f)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Enter, aim)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Move, aim + androidx.compose.ui.geometry.Offset(4f, 4f))
            frames(20)
            File(out, "desktop-hover.png").writeBytes(scene.render(time).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            // + is the 48 dp target at the right end of the control plate.
            val plus = androidx.compose.ui.geometry.Offset(margin + width - 24f * 1.5f, margin + cardHeight + (6f + 24f) * 1.5f)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Move, plus)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press, plus)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release, plus)
            frames(4)
            File(out, "desktop-add-flash.png").writeBytes(scene.render(time).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        kotlin.test.assertEquals(1, copies.value, "+ under the card adds one copy")
    }

    /**
     * The card preview on hover in the editor: resting on a deck card opens the whole card to its
     * left, since the deck panel sits at the window's right edge, and resting on a card in the card
     * list opens it to the right.
     */
    @Test fun renderHoverPreview() {
        val state = AppState(CoroutineScope(Dispatchers.Default)).apply { open(Screen.DeckEditor(deckId)); bootClock.skipToLock() }
        ImageComposeScene(1980, 1290, Density(1.5f)) { LockOnApp(container, state, sync, files, artworkDir = File("build/artwork-cache").absolutePath, dark = true) }.use { scene ->
            var time = 0L
            fun frames(n: Int) = repeat(n) { Thread.sleep(40); time += 16_000_000L; scene.render(time) }
            fun restOn(name: String, x: Float, y: Float) {
                val at = androidx.compose.ui.geometry.Offset(x, y)
                scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Move, at)
                scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Move, at + androidx.compose.ui.geometry.Offset(2f, 2f))
                frames(20)
                File(out, "$name.png").writeBytes(scene.render(time).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
            frames(15)
            // The first card in the deck panel's grid, then a card in the middle of the card list.
            restOn("desktop-hover-preview-deck", 1318f, 770f)
            restOn("desktop-hover-preview-list", 757f, 600f)
        }
    }

    /**
     * The Lock On moment on a banner-sized plate: the deck turns legal, and a frame is caught while
     * the light band crosses the plate and the brackets close on the reticle.
     */
    @Test fun renderLockSweep() {
        val locked = androidx.compose.runtime.mutableStateOf(false)
        ImageComposeScene(1200, 240, Density(1.5f)) {
            app.lockon.ui.theme.LockOnTheme(dark = true) {
                app.lockon.ui.components.SkyBackground(androidx.compose.ui.Modifier.fillMaxSize()) {
                    androidx.compose.foundation.layout.Row(
                        androidx.compose.ui.Modifier.fillMaxSize()
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow)
                            .then(androidx.compose.ui.Modifier.shineSweep(locked.value))
                            .padding(24.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        app.lockon.ui.components.LockIndicator(locked.value, size = 80.dp)
                        androidx.compose.material3.Text(if (locked.value) "LOCKED ON" else "NOT READY",
                            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            modifier = androidx.compose.ui.Modifier.padding(start = 16.dp))
                    }
                }
            }
        }.use { scene ->
            var time = 0L
            fun frames(n: Int) = repeat(n) { Thread.sleep(30); time += 16_000_000L; scene.render(time) }
            frames(10)
            locked.value = true
            frames(18)
            File(out, "desktop-lock-sweep.png").writeBytes(scene.render(time).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            frames(60)
            File(out, "desktop-lock-sweep-end.png").writeBytes(scene.render(time).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
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

    /** The window controls on their own, at rest and with the pointer resting on Close, in both themes. */
    @Test fun renderWindowControls() {
        for (dark in listOf(false, true)) {
            ImageComposeScene(600, 200, Density(1.5f)) {
                app.lockon.ui.theme.LockOnTheme(dark = dark) {
                    app.lockon.ui.components.SkyBackground(androidx.compose.ui.Modifier.fillMaxSize()) {
                        WindowControls(maximized = dark, onMinimize = {}, onToggleMaximize = {}, onClose = {},
                            modifier = androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(18.dp))
                    }
                }
            }.use { scene ->
                var time = 0L
                fun frames(n: Int) = repeat(n) { Thread.sleep(40); time += 50_000_000L; scene.render(time) }
                frames(10)
                // Close is the last 40 dp button, 4 dp inside the capsule, 18 dp from the corner: 1.5 px per dp.
                val close = androidx.compose.ui.geometry.Offset(600f - (18 + 4 + 20) * 1.5f, (18 + 4 + 16) * 1.5f)
                scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Move, close)
                scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Move, close + androidx.compose.ui.geometry.Offset(1f, 1f))
                frames(20)
                File(out, "${if (dark) "" else "light-"}desktop-window-controls.png").writeBytes(scene.render(time).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
    }
}

/** Stands in for a window's UI thread: queued work runs on the test's thread when it calls [runPending]. */
private class FrameThreadDispatcher : kotlinx.coroutines.CoroutineDispatcher() {
    private val queue = java.util.concurrent.ConcurrentLinkedQueue<Runnable>()

    override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) { queue += block }

    fun runPending() {
        while (true) (queue.poll() ?: return).run()
    }
}

