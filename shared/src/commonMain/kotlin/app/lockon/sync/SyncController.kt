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
    val pairingPrompt: StateFlow<PairingPrompt?>
    fun answerPairing(requestId: String, accept: Boolean)
    fun newPairingCode()
    fun selectAddress(address: String)
}
