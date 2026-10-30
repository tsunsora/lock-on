package app.lockon.desktop

import app.lockon.model.DeckSnapshot
import app.lockon.sync.LocalIdentity
import app.lockon.sync.OutgoingShare
import app.lockon.sync.ShareAnswer
import app.lockon.sync.ShareOffer
import app.lockon.sync.toOutgoing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.uuid.Uuid

/**
 * Decks this desktop is sending to phones. Phones only make requests, so each nearby phone holds
 * `GET /v1/shares` open and picks up an offer meant for it, then answers once its owner has decided.
 */
class ShareOutbox(
    private val identity: LocalIdentity,
    private val now: () -> Long = System::currentTimeMillis,
    private val lifetimeMs: Long = 3 * 60_000,
) {
    private data class Pending(val offer: ShareOffer, val deviceId: String, val deviceName: String, val expiresAt: Long)

    private val pending = MutableStateFlow<Map<String, Pending>>(emptyMap())
    private val _sends = MutableStateFlow<Map<String, OutgoingShare>>(emptyMap())
    /** Every send by phone device ID, waiting or finished. */
    val sends: StateFlow<Map<String, OutgoingShare>> = _sends.asStateFlow()

    fun send(deviceId: String, deviceName: String, decks: List<DeckSnapshot>) {
        val offer = ShareOffer(Uuid.random().toString(), identity.deviceId, identity.deviceName, decks = decks)
        pending.update { it + (deviceId to Pending(offer, deviceId, deviceName, now() + lifetimeMs)) }
        _sends.update { it + (deviceId to OutgoingShare(deviceName, decks.size, OutgoingShare.State.SENDING)) }
    }

    fun cancel(deviceId: String) {
        pending.update { it - deviceId }
        _sends.update { it - deviceId }
    }

    /** Ends sends nobody answered in time. */
    fun expire() {
        val expired = pending.value.values.filter { it.expiresAt <= now() }
        if (expired.isEmpty()) return
        pending.update { map -> map - expired.map { it.deviceId }.toSet() }
        _sends.update { sends ->
            sends + expired.map { it.deviceId to OutgoingShare(it.deviceName, it.offer.decks.size, OutgoingShare.State.FAILED, "${it.deviceName} didn't answer.") }
        }
    }

    /**
     * Held open by a phone; answers as soon as there are decks for it, or null after [timeoutMs].
     * Handing them over moves the send on to waiting for the phone's owner.
     */
    suspend fun await(deviceId: String, timeoutMs: Long): ShareOffer? = withTimeoutOrNull(timeoutMs) {
        pending.first { map -> map[deviceId]?.let { it.expiresAt > now() } == true }[deviceId]?.offer
    }?.also { offer ->
        _sends.update { sends ->
            val send = sends[deviceId]?.takeIf { it.state == OutgoingShare.State.SENDING && pending.value[deviceId]?.offer?.shareId == offer.shareId }
            if (send == null) sends else sends + (deviceId to send.copy(state = OutgoingShare.State.WAITING))
        }
    }

    /** Records the phone's answer. False when no such send is waiting for that phone. */
    fun answer(answer: ShareAnswer): Boolean {
        val waiting = pending.value[answer.deviceId]?.takeIf { it.offer.shareId == answer.shareId } ?: return false
        pending.update { it - answer.deviceId }
        _sends.update { it + (answer.deviceId to answer.response.toOutgoing(waiting.deviceName, waiting.offer.decks.size)) }
        return true
    }
}
