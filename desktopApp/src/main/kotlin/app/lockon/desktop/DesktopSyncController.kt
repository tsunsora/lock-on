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
import io.ktor.server.engine.EmbeddedServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** The desktop hosts sync: it runs the HTTPS server and shows the pairing QR code. */
class DesktopSyncController(private val container: AppContainer, private val scope: CoroutineScope) : HostSyncController {
    private val tls = TlsIdentity.loadOrCreate(AppDirs.data)
    private val pairing = PairingManager(container.devices, container.identity, tls.fingerprint)
    private val _hostInfo = MutableStateFlow<HostInfo?>(null)
    private val _status = MutableStateFlow(SyncStatus())
    private var addresses = lanAddresses()
    private var port = SyncProtocol.DEFAULT_PORT
    private var server: EmbeddedServer<*, *>? = null

    override val pairedDevices: Flow<List<PairedDevice>> = container.devices.pairedDevices
    override val status: StateFlow<SyncStatus> = _status.asStateFlow()
    override val hostInfo: StateFlow<HostInfo?> = _hostInfo.asStateFlow()
    override val pairingPrompt: StateFlow<PairingPrompt?> = pairing.prompt

    fun start() {
        scope.launch(Dispatchers.IO) {
            try {
                val (started, chosenPort) = startSyncServer(tls, SyncProtocol.DEFAULT_PORT) {
                    lockOnSync(container.syncHost, container.devices, pairing, container.identity)
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
                val fresh = lanAddresses()
                if (fresh.toSet() != addresses.toSet()) { addresses = fresh; publish() }
            }
        }
    }

    fun stop() { server?.stop(500, 1500) }

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
