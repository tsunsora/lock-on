package app.lockon.desktop

import app.lockon.AppContainer
import app.lockon.platform.formatTimestamp
import app.lockon.sync.HostInfo
import app.lockon.sync.HostSyncController
import app.lockon.sync.PairedDevice
import app.lockon.sync.PairingInvite
import app.lockon.sync.PairingPrompt
import app.lockon.sync.SyncProtocol
import app.lockon.sync.SyncStatus
import app.lockon.sync.syncedMessage
import io.ktor.server.engine.EmbeddedServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import app.lockon.sync.NearbyDevice
import app.lockon.sync.DeckSharing
import app.lockon.sync.IncomingShare
import app.lockon.sync.OutgoingShare
import app.lockon.sync.ShareInbox
import kotlinx.coroutines.launch

/** The desktop hosts sync: it runs the HTTPS server and shows the pairing QR code. */
class DesktopSyncController(private val container: AppContainer, private val scope: CoroutineScope) : HostSyncController {
    private val tls = TlsIdentity.loadOrCreate(AppDirs.data)
    private val pairing = PairingManager(container.devices, container.identity, tls.fingerprint)
    private val live = LiveClients()
    private val inbox = ShareInbox(container.decks)
    private val outbox = ShareOutbox(container.identity)
    private val advertiser = LanAdvertiser(container.identity, tls.fingerprint)
    private val _hostInfo = MutableStateFlow<HostInfo?>(null)
    private val _status = MutableStateFlow(SyncStatus())
    private val _notices = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** Changes moved per phone since its last notice, with the job that posts it once the sync goes quiet. */
    private val pendingChanges = mutableMapOf<String, Pair<Int, Int>>()
    private val noticeJobs = mutableMapOf<String, Job>()
    private var addresses = lanAddresses()
    private var port = SyncProtocol.DEFAULT_PORT
    private var server: EmbeddedServer<*, *>? = null

    override val pairedDevices: Flow<List<PairedDevice>> = container.devices.pairedDevices
    override val status: StateFlow<SyncStatus> = _status.asStateFlow()
    override val notices: Flow<String> = _notices.asSharedFlow()
    override val hostInfo: StateFlow<HostInfo?> = _hostInfo.asStateFlow()
    override val pairingPrompt: StateFlow<PairingPrompt?> = pairing.prompt
    override val liveClients: StateFlow<Set<String>> = live.ids
    // Phones only: other desktops can't pair with this one yet.
    override val nearby: StateFlow<List<NearbyDevice>> = advertiser.nearby
        .map { found -> found.values.filter { !it.isDesktop }.distinctBy { it.deviceId }.sortedBy { it.name } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    override val invited: StateFlow<Set<String>> = pairing.invites
        .map { invites -> invites.values.map { it.deviceId }.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    override fun invite(device: NearbyDevice) {
        pairing.invite(device.deviceId, device.name)
        _status.value = SyncStatus(message = "Invited ${device.name}. Accept on the phone, then confirm here.")
    }

    override fun cancelInvite(deviceId: String) = pairing.cancelInvite(deviceId)

    // Phones pick up decks sent to them with a held-open request, so any nearby phone can receive.
    override val sharing: DeckSharing = object : DeckSharing {
        override val targets: StateFlow<List<NearbyDevice>> = nearby
        override val outgoing: StateFlow<Map<String, OutgoingShare>> = outbox.sends
        override val incoming: StateFlow<IncomingShare?> = inbox.prompt

        override fun send(device: NearbyDevice, deckIds: List<String>) {
            scope.launch {
                val decks = deckIds.mapNotNull { container.decks.currentSnapshot(it) }.filterNot { it.deleted }.map(ShareInbox::asCopy)
                if (decks.isNotEmpty()) outbox.send(device.deviceId, device.name, decks)
            }
        }

        override fun cancelSend(deviceId: String) = outbox.cancel(deviceId)
        override fun answer(shareId: String, accept: Boolean) = inbox.answer(shareId, accept)
        override fun dismiss(shareId: String) = inbox.dismiss(shareId)
    }

    fun start() {
        scope.launch(Dispatchers.IO) {
            try {
                val (started, chosenPort) = startSyncServer(tls, SyncProtocol.DEFAULT_PORT) {
                    lockOnSync(container.syncHost, container.devices, pairing, container.identity, live, inbox = inbox, outbox = outbox, onChanges = ::onChanges)
                }
                server = started
                port = chosenPort
                publish()
            } catch (e: Exception) {
                _hostInfo.value = HostInfo(false, emptyList(), 0, "", 0, tls.fingerprint, null,
                    error = "Sync could not start: ${e.message}. Another copy of Lock On may already be running.")
            }
        }
        // Keep the shown code fresh and pick up network changes, such as joining new Wi-Fi.
        scope.launch {
            combine(pairing.code, container.devices.pairedDevices) { _, devices -> devices }.collect { devices ->
                if (server != null) publish()
                devices.maxByOrNull { it.lastSyncAt ?: 0 }?.lastSyncAt?.let {
                    _status.value = SyncStatus(message = "Ready for phones. Last sync ${formatTimestamp(it)}.")
                }
            }
        }
        scope.launch {
            while (true) {
                delay(15_000)
                pairing.rotateIfExpired()
                outbox.expire()
                val fresh = lanAddresses()
                if (fresh.toSet() != addresses.toSet()) { addresses = fresh; publish() }
            }
        }
    }

    /** A sync arrives as several push and pull batches; one notice covers them all. */
    private fun onChanges(deviceId: String, sent: Int, received: Int) = synchronized(pendingChanges) {
        val (s, r) = pendingChanges[deviceId] ?: (0 to 0)
        pendingChanges[deviceId] = (s + sent) to (r + received)
        noticeJobs[deviceId]?.cancel()
        noticeJobs[deviceId] = scope.launch {
            delay(NOTICE_QUIET_MS)
            val (totalSent, totalReceived) = synchronized(pendingChanges) { noticeJobs.remove(deviceId); pendingChanges.remove(deviceId) } ?: return@launch
            val name = container.devices.device(deviceId)?.name ?: "your phone"
            _notices.tryEmit(syncedMessage(name, totalSent, totalReceived))
        }
    }

    fun stop() {
        advertiser.stop()
        server?.stop(500, 1500)
    }

    private fun publish() {
        val code = pairing.code.value
        val primary = addresses.firstOrNull()
        val invite = primary?.let { PairingInvite(it, port, code.value, tls.fingerprint, container.identity.deviceName) }
        _hostInfo.value = HostInfo(
            running = true, addresses = addresses, port = port, pairingCode = code.value, codeExpiresAt = code.expiresAt,
            fingerprint = tls.fingerprint, qr = invite?.let { encodeQr(it.toUri()) },
            error = if (primary == null) "No network connection found. Connect this computer to the same Wi-Fi as your phone." else null,
        )
        if (_status.value.message == null) _status.value = SyncStatus(message = "Ready for phones on port $port.")
        // Let phones on the same network find this desktop without typing the address.
        primary?.let { address -> scope.launch(Dispatchers.IO) { advertiser.advertise(address, port) } }
    }

    override fun answerPairing(requestId: String, accept: Boolean) = pairing.answer(requestId, accept)

    override fun newPairingCode() = pairing.rotate()

    override fun selectAddress(address: String) {
        addresses = listOf(address) + (addresses - address)
        publish()
    }

    override fun forget(deviceId: String) {
        scope.launch { container.devices.forget(deviceId) }
    }
}

/** How long a phone's sync must go quiet before the desktop says what it moved. */
private const val NOTICE_QUIET_MS = 1_500L
