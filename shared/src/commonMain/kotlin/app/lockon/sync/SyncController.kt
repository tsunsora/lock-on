package app.lockon.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** What the sync screen shows and can do. The desktop hosts; phones connect to it. */
sealed interface SyncController {
    val pairedDevices: Flow<List<PairedDevice>>
    val status: StateFlow<SyncStatus>
    fun forget(deviceId: String)
}

data class SyncStatus(val working: Boolean = false, val message: String? = null, val isError: Boolean = false)

interface ClientSyncController : SyncController {
    val canScanQr: Boolean
    val autoSync: StateFlow<Boolean>
    /** Auto-sync state per paired desktop; absent while auto-sync is off or the app is in the background. */
    val autoStates: StateFlow<Map<String, AutoSyncState>>
    /** Desktops found on the local network that aren't paired yet. */
    val nearby: StateFlow<List<NearbyDesktop>>
    fun setAutoSync(enabled: Boolean)
    /** Set while a pairing waits for the desktop. People compare this code with the desktop's. */
    val pendingVerification: StateFlow<String?>
    fun scanQr()
    fun pair(invite: PairingInvite)
    fun syncNow(deviceId: String)
}

data class QrMatrix(val size: Int, val bits: BooleanArray) {
    operator fun get(x: Int, y: Int) = bits[y * size + x]
}

data class HostInfo(
    val running: Boolean,
    val addresses: List<String>,
    val port: Int,
    val pairingCode: String,
    val codeExpiresAt: Long,
    val fingerprint: String,
    val qr: QrMatrix?,
    val error: String? = null,
)

data class PairingPrompt(val requestId: String, val deviceName: String, val verificationCode: String)

interface HostSyncController : SyncController {
    val hostInfo: StateFlow<HostInfo?>
    /** Phones with auto-sync connected right now. */
    val liveClients: StateFlow<Set<String>>
    val pairingPrompt: StateFlow<PairingPrompt?>
    fun answerPairing(requestId: String, accept: Boolean)
    fun newPairingCode()
    fun selectAddress(address: String)
}
