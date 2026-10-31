package app.lockon.desktop

import app.lockon.sync.LocalIdentity
import app.lockon.sync.SyncMode
import app.lockon.sync.SyncRequest
import app.lockon.sync.SyncRequestAnswer
import app.lockon.sync.SyncTransfer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.uuid.Uuid

/**
 * Syncs this desktop asks paired phones for. Phones only make requests, so each paired phone with
 * Lock On open holds `GET /v1/mirror/requests` open, picks up a request meant for it, does the work
 * and answers. A phone that would have its decks replaced asks its owner first.
 */
class SyncRequests(
    private val identity: LocalIdentity,
    private val now: () -> Long = System::currentTimeMillis,
    private val lifetimeMs: Long = 3 * 60_000,
) {
    private data class Pending(val request: SyncRequest, val deviceName: String, val expiresAt: Long)

    /** Requests not yet picked up, and ones a phone is working on, by phone device ID. */
    private val waiting = MutableStateFlow<Map<String, Pending>>(emptyMap())
    private val picked = MutableStateFlow<Map<String, Pending>>(emptyMap())
    private val _transfers = MutableStateFlow<Map<String, SyncTransfer>>(emptyMap())
    val transfers: StateFlow<Map<String, SyncTransfer>> = _transfers.asStateFlow()

    fun request(deviceId: String, deviceName: String, mode: SyncMode) {
        val request = SyncRequest(Uuid.random().toString(), identity.deviceId, identity.deviceName, mode)
        waiting.update { it + (deviceId to Pending(request, deviceName, now() + lifetimeMs)) }
        picked.update { it - deviceId }
        _transfers.update {
            it + (deviceId to SyncTransfer(deviceName, mode, SyncTransfer.State.WAITING, "Waiting for $deviceName… Open Lock On on it."))
        }
    }

    fun clear(deviceId: String) {
        waiting.update { it - deviceId }
        picked.update { it - deviceId }
        _transfers.update { it - deviceId }
    }

    /** Ends requests no phone picked up, or answered, in time. */
    fun expire() {
        val expired = (waiting.value + picked.value).filterValues { it.expiresAt <= now() }
        if (expired.isEmpty()) return
        waiting.update { it - expired.keys }
        picked.update { it - expired.keys }
        _transfers.update { transfers ->
            transfers + expired.map { (id, p) ->
                id to SyncTransfer(p.deviceName, p.request.mode, SyncTransfer.State.FAILED, "${p.deviceName} didn't answer. Open Lock On on it and try again.")
            }
        }
    }

    /** Held open by a paired phone; answers as soon as there's a request for it, or null after [timeoutMs]. */
    suspend fun await(deviceId: String, timeoutMs: Long): SyncRequest? {
        val pending = withTimeoutOrNull(timeoutMs) {
            waiting.first { map -> map[deviceId]?.let { it.expiresAt > now() } == true }[deviceId]
        } ?: return null
        waiting.update { if (it[deviceId] == pending) it - deviceId else it }
        picked.update { it + (deviceId to pending.copy(expiresAt = now() + lifetimeMs)) }
        val state = if (pending.request.mode == SyncMode.GIVE) SyncTransfer.State.WAITING else SyncTransfer.State.WORKING
        val message = if (state == SyncTransfer.State.WAITING) "Waiting for ${pending.deviceName} to accept…" else "Syncing with ${pending.deviceName}…"
        _transfers.update { it + (deviceId to SyncTransfer(pending.deviceName, pending.request.mode, state, message)) }
        return pending.request
    }

    /** This desktop took on a phone's state for [requestId]; says so before the phone answers. */
    fun matched(deviceId: String, requestId: String, message: String) {
        val pending = picked.value[deviceId]?.takeIf { it.request.requestId == requestId } ?: return
        _transfers.update { it + (deviceId to SyncTransfer(pending.deviceName, pending.request.mode, SyncTransfer.State.DONE, message)) }
    }

    /** Records how the phone says a request ended. False when no such request is out for that phone. */
    fun answer(answer: SyncRequestAnswer): Boolean {
        val pending = picked.value[answer.deviceId]?.takeIf { it.request.requestId == answer.requestId } ?: return false
        picked.update { it - answer.deviceId }
        val name = pending.deviceName
        val mode = pending.request.mode
        _transfers.update { transfers ->
            val current = transfers[answer.deviceId]
            val ended = when {
                answer.done && current?.state == SyncTransfer.State.DONE -> current
                answer.done -> SyncTransfer(name, mode, SyncTransfer.State.DONE, answer.message.ifBlank { "Synced with $name." })
                answer.declined -> SyncTransfer(name, mode, SyncTransfer.State.DECLINED, "$name declined.")
                else -> SyncTransfer(name, mode, SyncTransfer.State.FAILED, answer.message.ifBlank { "$name couldn't sync." })
            }
            transfers + (answer.deviceId to ended)
        }
        return true
    }
}
