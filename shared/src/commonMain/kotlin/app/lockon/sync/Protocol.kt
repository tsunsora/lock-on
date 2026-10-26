package app.lockon.sync

import app.lockon.BuildInfo
import app.lockon.model.DeckSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Wire format for device-to-device sync. Bump [PROTOCOL_VERSION] for breaking changes and keep
 * [MIN_PROTOCOL_VERSION] at the oldest version this build can still talk to.
 */
object SyncProtocol {
    const val PROTOCOL_VERSION = 1
    const val MIN_PROTOCOL_VERSION = 1
    /**
     * Version of [DeckSnapshot] inside revisions. A device refuses revisions newer than it understands.
     * 2 added the Considering section; a version-1 app would drop those cards on its next save.
     */
    const val DATA_FORMAT_VERSION = 2
    const val DEFAULT_PORT = 53340
    const val PAIRING_SCHEME = "lockon"

    /** DNS-SD service the desktop advertises on the local network. */
    const val SERVICE_TYPE = "_lockon._tcp"
    /** TXT record keys: device ID, display name, certificate fingerprint, protocol and data versions. */
    const val TXT_ID = "id"
    const val TXT_NAME = "name"
    const val TXT_FINGERPRINT = "fp"
    const val TXT_PROTOCOL = "pv"
    const val TXT_DATA_FORMAT = "df"
    /** Longest a phone's change-wait request is held open before the desktop answers "nothing new". */
    const val MAX_WAIT_SECONDS = 25

    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Returns a reason the two sides cannot sync, or null when they are compatible. */
    fun incompatibility(peer: HelloResponse): String? = when {
        peer.app != APP_ID -> "That address is not a Lock On desktop."
        peer.minProtocolVersion > PROTOCOL_VERSION -> "The desktop app is newer than this app. Update Lock On on this device."
        peer.protocolVersion < MIN_PROTOCOL_VERSION -> "The desktop app is too old for this device. Update Lock On on the desktop."
        peer.dataFormatVersion > DATA_FORMAT_VERSION -> "The desktop stores decks in a newer format. Update Lock On on this device."
        // An older desktop would lose parts of decks it cannot read, so both sides must match.
        peer.dataFormatVersion < DATA_FORMAT_VERSION -> "The desktop app is too old for this device. Update Lock On on the desktop."
        else -> null
    }

    const val APP_ID = "lock-on"
}

@Serializable
data class HelloResponse(
    val app: String = SyncProtocol.APP_ID,
    val protocolVersion: Int = SyncProtocol.PROTOCOL_VERSION,
    val minProtocolVersion: Int = SyncProtocol.MIN_PROTOCOL_VERSION,
    val dataFormatVersion: Int = SyncProtocol.DATA_FORMAT_VERSION,
    val deviceId: String,
    val deviceName: String,
    /** Informational only; compatibility is decided by the version fields above. */
    val appVersion: String = BuildInfo.VERSION,
)

@Serializable
data class PairRequest(
    val pairingCode: String,
    val deviceId: String,
    val deviceName: String,
    val protocolVersion: Int = SyncProtocol.PROTOCOL_VERSION,
)

@Serializable
data class PairResponse(
    val accepted: Boolean,
    val message: String = "",
    val authToken: String? = null,
    val hostDeviceId: String? = null,
    val hostDeviceName: String? = null,
)

/** One immutable deck revision. [parents] is empty for a new deck and has two or more for a merge. */
@Serializable
data class RevisionDto(
    val revisionId: String,
    val deckId: String,
    val parents: List<String>,
    val originDeviceId: String,
    val createdAt: Long,
    val dataFormatVersion: Int = SyncProtocol.DATA_FORMAT_VERSION,
    val snapshot: DeckSnapshot,
)

@Serializable
data class PushRequest(val batchId: String, val revisions: List<RevisionDto>)

@Serializable
data class PushResponse(val batchId: String, val received: Int, val alreadyKnown: Int)

@Serializable
data class PullResponse(val revisions: List<RevisionDto>, val nextCursor: Long, val hasMore: Boolean)

@Serializable
data class ErrorResponse(val message: String)

/** Answer to a change-wait: true when the desktop has revisions after the phone's cursor. */
@Serializable
data class WaitResponse(val changed: Boolean)

/** A Lock On desktop seen on the local network. Trust still comes from pairing, never from this. */
data class NearbyDesktop(
    val deviceId: String,
    val name: String,
    val host: String,
    val port: Int,
    val fingerprint: String?,
    val compatible: Boolean,
)

/** Contents of the desktop's pairing QR code. */
data class PairingInvite(
    val host: String,
    val port: Int,
    val pairingCode: String,
    /** SHA-256 of the desktop's TLS certificate, hex. Null when typed in by hand. */
    val fingerprint: String?,
    val name: String = "",
) {
    fun toUri(): String = buildString {
        append("${SyncProtocol.PAIRING_SCHEME}://pair?host=$host&port=$port&code=$pairingCode")
        fingerprint?.let { append("&fp=$it") }
        if (name.isNotBlank()) append("&name=${encode(name)}")
    }

    companion object {
        fun parse(uri: String): PairingInvite? {
            val prefix = "${SyncProtocol.PAIRING_SCHEME}://pair?"
            if (!uri.startsWith(prefix)) return null
            val params = uri.removePrefix(prefix).split('&').mapNotNull {
                val i = it.indexOf('=')
                if (i <= 0) null else it.substring(0, i) to decode(it.substring(i + 1))
            }.toMap()
            return PairingInvite(
                host = params["host"] ?: return null,
                port = params["port"]?.toIntOrNull() ?: return null,
                pairingCode = params["code"] ?: return null,
                fingerprint = params["fp"],
                name = params["name"].orEmpty(),
            )
        }

        private fun encode(value: String) = buildString {
            for (ch in value) if (ch.isLetterOrDigit() || ch in "-_.") append(ch) else {
                ch.toString().encodeToByteArray().forEach { append('%'); append(((it.toInt() and 0xff) + 0x100).toString(16).substring(1)) }
            }
        }

        private fun decode(value: String): String {
            val bytes = mutableListOf<Byte>()
            var i = 0
            while (i < value.length) {
                if (value[i] == '%' && i + 2 < value.length) {
                    bytes += value.substring(i + 1, i + 3).toInt(16).toByte(); i += 3
                } else {
                    value[i].toString().encodeToByteArray().forEach { bytes += it }; i++
                }
            }
            return bytes.toByteArray().decodeToString()
        }
    }
}

/** Short code both screens show during pairing so people can confirm they reached the same desktop. */
fun verificationCode(fingerprint: String): String =
    fingerprint.uppercase().filter { it.isLetterOrDigit() }.take(8).chunked(4).joinToString("-")
