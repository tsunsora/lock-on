package app.lockon.sync

import app.lockon.data.DeckRepository
import app.lockon.model.DeckSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock

/**
 * Sending decks, LocalSend-style: pick decks, tap a nearby device, and its owner accepts or
 * declines. Accepted decks arrive as new copies, so this works with any nearby device, paired or not.
 */
interface DeckSharing {
    /** Nearby devices that decks can be sent to, paired or not. */
    val targets: StateFlow<List<NearbyDevice>>
    /** Sends waiting for an answer or just finished, by receiving device ID. */
    val outgoing: StateFlow<Map<String, OutgoingShare>>
    fun send(device: NearbyDevice, deckIds: List<String>)
    /** Stops waiting for an answer, or clears a finished send from the list. */
    fun cancelSend(deviceId: String)
    /** Decks another device wants to send here, waiting for [answer]. */
    val incoming: StateFlow<IncomingShare?>
    fun answer(shareId: String, accept: Boolean)
}

data class OutgoingShare(val deviceName: String, val deckCount: Int, val state: State, val message: String? = null) {
    enum class State { WAITING, SAVED, DECLINED, FAILED }
}

data class IncomingShare(val shareId: String, val senderName: String, val decks: List<DeckSnapshot>)

/**
 * The receiving side of [DeckSharing]: shows one offer at a time and saves the decks only once
 * someone accepts. A sender that was just declined can't ask again straight away.
 */
class ShareInbox(
    private val decks: DeckRepository,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val confirmTimeoutMs: Long = 120_000,
    private val declineCooldownMs: Long = 30_000,
) {
    private val _prompt = MutableStateFlow<IncomingShare?>(null)
    val prompt: StateFlow<IncomingShare?> = _prompt.asStateFlow()

    private val lock = Mutex()
    private var pending: CompletableDeferred<Boolean>? = null
    private val declinedUntil = mutableMapOf<String, Long>()

    fun answer(shareId: String, accept: Boolean) {
        if (_prompt.value?.shareId == shareId) pending?.complete(accept)
    }

    /** Asks this device's owner about [offer] and waits for the answer, which goes back to the sender. */
    suspend fun receive(offer: ShareOffer): ShareResponse {
        val incoming = offer.decks.filterNot { it.deleted }
        val deferred = lock.withLock {
            when {
                offer.dataFormatVersion > SyncProtocol.DATA_FORMAT_VERSION ->
                    return refused("These decks use a newer format. Update Lock On on the receiving device.")
                incoming.isEmpty() -> return refused("No decks to send.")
                incoming.size > MAX_DECKS -> return refused("Send at most $MAX_DECKS decks at a time.")
                incoming.any { it.entries.size > MAX_ENTRIES } -> return refused("One of those decks is too large to send.")
                pending != null -> return refused("The other device is answering another send. Try again in a moment.")
                (declinedUntil[offer.senderId] ?: 0) > now() -> return refused("Declined a moment ago. Try again in a minute.")
            }
            CompletableDeferred<Boolean>().also {
                pending = it
                _prompt.value = IncomingShare(offer.shareId, offer.senderName.take(60), incoming)
            }
        }
        val accepted = try {
            withTimeoutOrNull(confirmTimeoutMs) { deferred.await() } ?: false
        } finally {
            lock.withLock { pending = null; _prompt.value = null }
        }
        if (!accepted) {
            declinedUntil[offer.senderId] = now() + declineCooldownMs
            return ShareResponse(accepted = false, declined = true)
        }
        incoming.forEach { decks.create(asCopy(it)) }
        return ShareResponse(accepted = true, saved = incoming.size)
    }

    private fun refused(message: String) = ShareResponse(accepted = false, message = message)

    companion object {
        const val MAX_DECKS = 50
        const val MAX_ENTRIES = 500

        /** A received deck keeps its cards, notes, tags and cover; folders and games belong to the sender. */
        fun asCopy(deck: DeckSnapshot) = deck.copy(name = deck.name.take(80), folder = null, matches = emptyList(), deleted = false)
    }
}

/** What a send's answer says to the person who sent it. */
fun ShareResponse.toOutgoing(deviceName: String, deckCount: Int) = when {
    accepted -> OutgoingShare(deviceName, deckCount, OutgoingShare.State.SAVED,
        "$deviceName saved ${if (saved == 1) "1 deck" else "$saved decks"}.")
    declined -> OutgoingShare(deviceName, deckCount, OutgoingShare.State.DECLINED, "$deviceName declined.")
    else -> OutgoingShare(deviceName, deckCount, OutgoingShare.State.FAILED, message.ifBlank { "$deviceName didn't take the decks." })
}
