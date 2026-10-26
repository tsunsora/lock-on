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
import app.lockon.sync.NearbyDesktop
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
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
    private val _nearby = MutableStateFlow<Map<String, NearbyDesktop>>(emptyMap())
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
        onFound = { service, desktop -> scope.launch { onDesktopFound(service, desktop) } },
        onLost = { service -> _nearby.update { it - service } },
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
    override val nearby: StateFlow<List<NearbyDesktop>> =
        combine(_nearby, container.devices.pairedDevices) { found, paired ->
            val pairedIds = paired.map { it.deviceId }.toSet()
            found.values.filter { it.deviceId !in pairedIds }.distinctBy { it.deviceId }.sortedBy { it.name }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

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
            discovery.start()
            foregroundJob = scope.launch { autoLoop.run() }
        } else {
            discovery.stop()
            foregroundJob?.cancel()
            foregroundJob = null
            synchronized(waitClients) { waitClients.values.forEach { it.close() }; waitClients.clear() }
            _nearby.value = emptyMap()
        }
    }

    private suspend fun onDesktopFound(service: String, desktop: NearbyDesktop) {
        val paired = container.devices.device(desktop.deviceId)
        if (paired == null) {
            _nearby.update { it + (service to desktop) }
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
                _status.value = SyncStatus(working = true, message = "Connecting to ${invite.host}…")
                var seen: String? = null
                val client = pinnedHttpClient(invite.fingerprint) { fp ->
                    seen = fp
                    _verification.value = verificationCode(fp)
                }
                val response = client.use {
                    requestPairing(
                        it, baseUrl(invite.host, invite.port),
                        PairRequest(invite.pairingCode, container.identity.deviceId, container.identity.deviceName),
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
