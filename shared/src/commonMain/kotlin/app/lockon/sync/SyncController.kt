package app.lockon.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/** What the sync screen shows and can do. The desktop hosts; phones connect to it. */
sealed interface SyncController {
    val pairedDevices: Flow<List<PairedDevice>>
    val status: StateFlow<SyncStatus>
    fun forget(deviceId: String)
    /** Sending copies of decks to nearby devices; null where this device can't. */
    val sharing: DeckSharing? get() = null
    /** One line per sync that moved changes, auto-sync included, shown as a snackbar anywhere in the app. */
    val notices: Flow<String> get() = emptyFlow()
}

data class SyncStatus(val working: Boolean = false, val message: String? = null, val isError: Boolean = false)

/** How a finished sync reads on the Sync screen: in decks and words, not counters. */
fun syncedMessage(deviceName: String, sent: Int, received: Int): String {
    fun changes(n: Int) = if (n == 1) "1 change" else "$n changes"
    val moved = listOfNotNull(
        changes(sent).takeIf { sent > 0 }?.let { "sent $it" },
        changes(received).takeIf { received > 0 }?.let { "got $it" },
    )
    return "Up to date with $deviceName" + (if (moved.isEmpty()) "." else ": ${moved.joinToString(", ")}.")
}

interface ClientSyncController : SyncController {
    val canScanQr: Boolean
    val autoSync: StateFlow<Boolean>
    /** Auto-sync state per paired desktop; absent while auto-sync is off or the app is in the background. */
    val autoStates: StateFlow<Map<String, AutoSyncState>>
    /** Desktops found on the local network that aren't paired yet. */
    val nearby: StateFlow<List<NearbyDevice>>
    fun setAutoSync(enabled: Boolean)
    /** A desktop asking this phone to connect; answered with [answerInvite]. */
    val incomingInvite: StateFlow<IncomingInvite?>
    fun answerInvite(accept: Boolean)
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

/** A pairing request waiting on the desktop. [invited] when it answers an invitation from this desktop. */
data class PairingPrompt(val requestId: String, val deviceName: String, val verificationCode: String, val invited: Boolean = false)

interface HostSyncController : SyncController {
    val hostInfo: StateFlow<HostInfo?>
    /** Phones with auto-sync connected right now. */
    val liveClients: StateFlow<Set<String>>
    val pairingPrompt: StateFlow<PairingPrompt?>
    fun answerPairing(requestId: String, accept: Boolean)
    fun newPairingCode()
    fun selectAddress(address: String)
    /** Phones with Lock On open on the same network, paired or not. */
    val nearby: StateFlow<List<NearbyDevice>>
    /** Phones this desktop has invited and is waiting on, by device ID. */
    val invited: StateFlow<Set<String>>
    fun invite(device: NearbyDevice)
    fun cancelInvite(deviceId: String)
}

/** An invitation shown on a phone, with where the desktop is. */
data class IncomingInvite(val offer: InviteOffer, val desktop: NearbyDevice)
