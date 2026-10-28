package app.lockon.android

import app.lockon.AppContainer
import app.lockon.sync.ClientSyncController
import app.lockon.sync.DeviceRole
import app.lockon.sync.HttpSyncTransport
import app.lockon.sync.PairRequest
import app.lockon.sync.PairedDevice
import app.lockon.sync.PairingInvite
import app.lockon.sync.SyncException
import app.lockon.sync.SyncStatus
import app.lockon.sync.baseUrl
import app.lockon.sync.requestPairing
import app.lockon.sync.verificationCode
import android.content.Context
import app.lockon.sync.AutoSyncLoop
import app.lockon.sync.AutoSyncState
import app.lockon.sync.NearbyDevice
import io.ktor.client.HttpClient
import app.lockon.sync.IncomingInvite
import app.lockon.sync.awaitInvite
import app.lockon.sync.pairingCheckNumber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.map
import app.lockon.sync.DeckSharing
import app.lockon.sync.IncomingShare
import app.lockon.sync.OutgoingShare
import app.lockon.sync.ShareAnswer
import app.lockon.sync.ShareInbox
import app.lockon.sync.ShareOffer
import app.lockon.sync.answerShare
import app.lockon.sync.awaitShare
import app.lockon.sync.sendDecks
import app.lockon.sync.sendsDecks
import app.lockon.sync.toOutgoing

/** The phone connects to a paired desktop. It starts every pairing and sync; the desktop only answers. */
class AndroidSyncController(
    context: Context,
    private val container: AppContainer,
    private val secrets: SecretStore,
    private val scope: CoroutineScope,
) : ClientSyncController {
    private val _status = MutableStateFlow(SyncStatus())
    private val _verification = MutableStateFlow<String?>(null)
    private val _autoSync = MutableStateFlow(true)
    private val _autoStates = MutableStateFlow<Map<String, AutoSyncState>>(emptyMap())
    private val _nearby = MutableStateFlow<Map<String, NearbyDevice>>(emptyMap())
    private val _incomingInvite = MutableStateFlow<IncomingInvite?>(null)
    private val inviteWatchers = mutableMapOf<String, Job>()
    /** Every desktop seen on the network, paired or not, by service name. Decks can go to any of them. */
    private val _desktops = MutableStateFlow<Map<String, NearbyDevice>>(emptyMap())
    private val shareWatchers = mutableMapOf<String, Job>()
    private val inbox = ShareInbox(container.decks)
    private val _outgoing = MutableStateFlow<Map<String, OutgoingShare>>(emptyMap())
    private val sendJobs = mutableMapOf<String, Job>()
    private val busy = Mutex()
    private var foregroundJob: Job? = null
    /** One pinned client per desktop for held-open wait requests, closed when the app leaves the foreground. */
    private val waitClients = mutableMapOf<String, HttpClient>()

    private val autoLoop = AutoSyncLoop(
        hostIds = combine(container.devices.pairedDevices, _autoSync) { devices, on ->
            if (on) devices.filter { it.role == DeviceRole.HOST }.map { it.deviceId } else emptyList()
        },
        localChanges = container.decks.latestSeq,
        syncOnce = { hostId -> busy.withLock { autoSyncOnce(hostId) } },
        waitForRemote = ::waitForRemote,
        onState = { id, state -> _autoStates.update { if (state == null) it - id else it + (id to state) } },
    )

    private val discovery = NsdDiscovery(
        context,
        onFound = { service, device -> if (device.isDesktop) scope.launch { onDesktopFound(service, device) } },
        onLost = { service ->
            _nearby.update { it - service }
            _desktops.value[service]?.let { gone -> synchronized(shareWatchers) { shareWatchers.remove(gone.deviceId)?.cancel() } }
            _desktops.update { it - service }
        },
    )

    init {
        scope.launch { _autoSync.value = container.devices.autoSyncEnabled() }
    }

    /** Set by the activity, which owns the camera scanner. */
    var scanner: (() -> Unit)? = null

    override val pairedDevices: Flow<List<PairedDevice>> = container.devices.pairedDevices
    override val status: StateFlow<SyncStatus> = _status.asStateFlow()
    override val pendingVerification: StateFlow<String?> = _verification.asStateFlow()
    override val canScanQr: Boolean get() = scanner != null
    override val autoSync: StateFlow<Boolean> = _autoSync.asStateFlow()
    override val autoStates: StateFlow<Map<String, AutoSyncState>> = _autoStates.asStateFlow()
    override val nearby: StateFlow<List<NearbyDevice>> =
        combine(_nearby, container.devices.pairedDevices) { found, paired ->
            val pairedIds = paired.map { it.deviceId }.toSet()
            found.values.filter { it.deviceId !in pairedIds }.distinctBy { it.deviceId }.sortedBy { it.name }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val incomingInvite: StateFlow<IncomingInvite?> = _incomingInvite.asStateFlow()

    override fun answerInvite(accept: Boolean) {
        val invite = _incomingInvite.value ?: return
        _incomingInvite.value = null
        if (accept) {
            val d = invite.desktop
            pair(PairingInvite(d.host, d.port, pairingCode = "", fingerprint = d.fingerprint, name = d.name, inviteId = invite.offer.inviteId))
        }
        watchInvites(invite.desktop)
    }

    /** Keeps one held-open invite request per nearby, unpaired desktop while the app is visible. */
    private fun watchInvites(desktop: NearbyDevice) {
        if (!desktop.isDesktop || !desktop.compatible) return
        synchronized(inviteWatchers) {
            if (inviteWatchers[desktop.deviceId]?.isActive == true) return
            inviteWatchers[desktop.deviceId] = scope.launch {
                var backoff = 2_000L
                while (true) {
                    if (container.devices.device(desktop.deviceId) != null) return@launch
                    try {
                        val offer = pinnedHttpClient(desktop.fingerprint).use {
                            awaitInvite(it, baseUrl(desktop.host, desktop.port), container.identity.deviceId)
                        }
                        backoff = 2_000L
                        if (offer != null && offer.hostDeviceId == desktop.deviceId) {
                            _incomingInvite.value = IncomingInvite(offer, desktop)
                            return@launch
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        delay(backoff)
                        backoff = (backoff * 2).coerceAtMost(60_000L)
                    }
                }
            }
        }
    }

    private fun stopWatchingInvites() = synchronized(inviteWatchers) {
        inviteWatchers.values.forEach { it.cancel() }
        inviteWatchers.clear()
    }

    override val sharing: DeckSharing = object : DeckSharing {
        override val targets: StateFlow<List<NearbyDevice>> = _desktops
            .map { found -> found.values.distinctBy { it.deviceId }.sortedBy { it.name } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
        override val outgoing: StateFlow<Map<String, OutgoingShare>> = _outgoing.asStateFlow()
        override val incoming: StateFlow<IncomingShare?> = inbox.prompt

        override fun send(device: NearbyDevice, deckIds: List<String>) {
            _outgoing.update { it + (device.deviceId to OutgoingShare(device.name, deckIds.size, OutgoingShare.State.WAITING)) }
            val job = scope.launch {
                val result = try {
                    val decks = deckIds.mapNotNull { container.decks.currentSnapshot(it) }.filterNot { it.deleted }.map(ShareInbox::asCopy)
                    val offer = ShareOffer(Uuid.random().toString(), container.identity.deviceId, container.identity.deviceName, decks = decks)
                    clientFor(device).use { sendDecks(it, baseUrl(device.host, device.port), offer) }.toOutgoing(device.name, decks.size)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    OutgoingShare(device.name, deckIds.size, OutgoingShare.State.FAILED, friendly(e))
                }
                _outgoing.update { it + (device.deviceId to result) }
            }
            synchronized(sendJobs) { sendJobs.put(device.deviceId, job)?.cancel() }
        }

        override fun cancelSend(deviceId: String) {
            synchronized(sendJobs) { sendJobs.remove(deviceId)?.cancel() }
            _outgoing.update { it - deviceId }
        }

        override fun answer(shareId: String, accept: Boolean) = inbox.answer(shareId, accept)
    }

    /** A paired desktop is reached with the certificate pinned at pairing; others with the one they announce. */
    private suspend fun clientFor(desktop: NearbyDevice): HttpClient =
        pinnedHttpClient(container.devices.device(desktop.deviceId)?.certFingerprint ?: desktop.fingerprint)

    /**
     * Keeps one held-open request per nearby desktop so it can send this phone decks. Asks first,
     * then tells the desktop what happened. Stops for desktops too old to send decks.
     */
    private fun watchShares(desktop: NearbyDevice) {
        synchronized(shareWatchers) {
            if (shareWatchers[desktop.deviceId]?.isActive == true) return
            shareWatchers[desktop.deviceId] = scope.launch {
                val url = baseUrl(desktop.host, desktop.port)
                var checked = false
                var backoff = 2_000L
                while (true) {
                    try {
                        val token = secrets.get(tokenKey(desktop.deviceId))?.takeIf { container.devices.device(desktop.deviceId) != null }
                        val offer = clientFor(desktop).use { client ->
                            if (!checked) {
                                if (!sendsDecks(client, url)) return@launch
                                checked = true
                            }
                            awaitShare(client, url, container.identity.deviceId, token)
                        }
                        backoff = 2_000L
                        if (offer == null || offer.senderId != desktop.deviceId) continue
                        val response = inbox.receive(offer)
                        clientFor(desktop).use { answerShare(it, url, ShareAnswer(offer.shareId, container.identity.deviceId, response), token) }
                        if (response.accepted) {
                            _status.value = SyncStatus(message = "Saved ${if (response.saved == 1) "1 deck" else "${response.saved} decks"} from ${desktop.name}.")
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        delay(backoff)
                        backoff = (backoff * 2).coerceAtMost(60_000L)
                    }
                }
            }
        }
    }

    private fun stopWatchingShares() = synchronized(shareWatchers) {
        shareWatchers.values.forEach { it.cancel() }
        shareWatchers.clear()
    }

    override fun setAutoSync(enabled: Boolean) {
        _autoSync.value = enabled
        scope.launch { container.devices.setAutoSync(enabled) }
    }

    /**
     * Discovery and auto-sync run only while the app is visible (foreground-only, no background
     * service). The activity calls this from onStart and onStop.
     */
    fun setForeground(visible: Boolean) {
        if (visible) {
            if (foregroundJob?.isActive == true) return
            discovery.start(container.identity)
            foregroundJob = scope.launch { autoLoop.run() }
        } else {
            discovery.stop()
            stopWatchingInvites()
            stopWatchingShares()
            _desktops.value = emptyMap()
            _incomingInvite.value = null
            foregroundJob?.cancel()
            foregroundJob = null
            synchronized(waitClients) { waitClients.values.forEach { it.close() }; waitClients.clear() }
            _nearby.value = emptyMap()
        }
    }

    private suspend fun onDesktopFound(service: String, desktop: NearbyDevice) {
        _desktops.update { it + (service to desktop) }
        watchShares(desktop)
        val paired = container.devices.device(desktop.deviceId)
        if (paired == null) {
            _nearby.update { it + (service to desktop) }
            watchInvites(desktop)
            return
        }
        // A paired desktop that moved (new Wi-Fi, new DHCP lease) is followed automatically.
        if (paired.host != desktop.host || paired.port != desktop.port) {
            container.devices.updateAddress(desktop.deviceId, desktop.host, desktop.port)
        }
        autoLoop.nudge(desktop.deviceId)
    }

    private suspend fun autoSyncOnce(hostId: String) {
        val (device, transport) = transportFor(hostId) ?: throw SyncException("Not paired.")
        val result = container.syncClient.sync(hostId, transport, recordErrors = false)
        if (result.sent > 0 || result.received > 0) {
            _status.value = SyncStatus(message = "Auto-synced with ${device.name}: sent ${result.sent}, received ${result.received} changes.")
        }
    }

    private suspend fun waitForRemote(hostId: String): Boolean {
        val (_, transport) = transportFor(hostId) ?: throw SyncException("Not paired.")
        val device = container.devices.device(hostId) ?: throw SyncException("Not paired.")
        return transport.waitForChanges(device.pullCursor).changed
    }

    private suspend fun transportFor(hostId: String): Pair<PairedDevice, HttpSyncTransport>? {
        val device = container.devices.device(hostId) ?: return null
        val token = secrets.get(tokenKey(hostId)) ?: return null
        val host = device.host ?: return null
        val port = device.port ?: return null
        val fingerprint = device.certFingerprint ?: return null
        val client = synchronized(waitClients) { waitClients.getOrPut(fingerprint) { pinnedHttpClient(fingerprint) } }
        return device to HttpSyncTransport(client, baseUrl(host, port), token)
    }

    override fun scanQr() { scanner?.invoke() }

    fun onScanned(contents: String) {
        val invite = PairingInvite.parse(contents)
        if (invite == null) _status.value = SyncStatus(message = "That QR code is not a Lock On pairing code.", isError = true)
        else pair(invite)
    }

    override fun pair(invite: PairingInvite) {
        scope.launch {
            if (!busy.tryLock()) return@launch
            try {
                _status.value = SyncStatus(working = true, message = "Connecting to ${invite.name.ifBlank { invite.host }}…")
                var seen: String? = null
                // Both screens show the same number, made from the certificate this phone sees and a fresh nonce.
                val nonce = Uuid.random().toString()
                val client = pinnedHttpClient(invite.fingerprint) { fp ->
                    seen = fp
                    _verification.value = pairingCheckNumber(fp, nonce)
                }
                val response = client.use {
                    requestPairing(
                        it, baseUrl(invite.host, invite.port),
                        PairRequest(
                            pairingCode = invite.pairingCode.ifBlank { null },
                            deviceId = container.identity.deviceId,
                            deviceName = container.identity.deviceName,
                            nonce = nonce,
                            inviteId = invite.inviteId,
                        ),
                    )
                }
                val token = response.authToken
                val hostId = response.hostDeviceId
                val fingerprint = seen
                if (!response.accepted || token == null || hostId == null || fingerprint == null) {
                    _status.value = SyncStatus(message = response.message.ifBlank { "The desktop did not accept the pairing." }, isError = true)
                    return@launch
                }
                secrets.put(tokenKey(hostId), token)
                container.devices.savePairing(
                    deviceId = hostId, name = response.hostDeviceName ?: "Desktop", role = DeviceRole.HOST,
                    host = invite.host, port = invite.port, certFingerprint = fingerprint,
                )
                _verification.value = null
                runSync(hostId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _status.value = SyncStatus(message = friendly(e), isError = true)
            } finally {
                _verification.value = null
                busy.unlock()
            }
        }
    }

    override fun syncNow(deviceId: String) {
        scope.launch {
            if (!busy.tryLock()) return@launch
            try { runSync(deviceId) } finally { busy.unlock() }
        }
    }

    private suspend fun runSync(hostId: String) {
        val device = container.devices.device(hostId) ?: return
        val token = secrets.get(tokenKey(hostId))
        val host = device.host
        val port = device.port
        val fingerprint = device.certFingerprint
        if (token == null || host == null || port == null || fingerprint == null) {
            _status.value = SyncStatus(message = "Pair with ${device.name} again to sync.", isError = true)
            return
        }
        _status.value = SyncStatus(working = true, message = "Syncing with ${device.name}…")
        try {
            val result = pinnedHttpClient(fingerprint).use { client ->
                container.syncClient.sync(hostId, HttpSyncTransport(client, baseUrl(host, port), token))
            }
            _status.value = SyncStatus(
                message = if (result.sent == 0 && result.received == 0) "Up to date with ${device.name}."
                else "Synced with ${device.name}: sent ${result.sent}, received ${result.received} changes.",
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _status.value = SyncStatus(message = friendly(e), isError = true)
        }
    }

    override fun forget(deviceId: String) {
        scope.launch {
            secrets.remove(tokenKey(deviceId))
            container.devices.forget(deviceId)
        }
    }

    private fun tokenKey(hostId: String) = "token:$hostId"

    private fun friendly(e: Throwable): String {
        if (e is SyncException && e.cause == null) return e.message ?: "Sync failed."
        val chain = generateSequence(e) { it.cause }.toList()
        return when {
            chain.any { it is java.security.cert.CertificateException } ->
                "The desktop's identity changed. If you reinstalled Lock On on the desktop, forget it here and pair again."
            chain.any { it is java.net.ConnectException || it is java.net.NoRouteToHostException } ->
                "Could not reach the desktop. Check that Lock On is open on it, both devices are on the same Wi-Fi, and the desktop firewall allows Lock On."
            chain.any { it is java.net.SocketTimeoutException } -> "The desktop took too long to answer."
            chain.any { it is java.net.UnknownHostException } -> "That desktop address could not be found."
            else -> e.message ?: "Sync failed."
        }
    }
}
