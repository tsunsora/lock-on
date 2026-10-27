package app.lockon.desktop

import app.lockon.sync.DeviceRepository
import app.lockon.sync.DeviceRole
import app.lockon.sync.InviteOffer
import app.lockon.sync.LocalIdentity
import app.lockon.sync.PairRequest
import app.lockon.sync.PairResponse
import app.lockon.sync.PairingPrompt
import app.lockon.sync.pairingCheckNumber
import app.lockon.sync.verificationCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.uuid.Uuid

/**
 * Pairing, LocalSend-style: a phone can tap this desktop (no code needed) or this desktop can invite
 * a phone, and either way a person confirms on the desktop after checking that both screens show
 * the same 6-digit number. Codes from the QR or typed by hand still work. Only a confirmed pairing
 * hands out a long-lived token.
 */
class PairingManager(
    private val devices: DeviceRepository,
    private val identity: LocalIdentity,
    private val fingerprint: String,
    private val now: () -> Long = System::currentTimeMillis,
    private val codeLifetimeMs: Long = 10 * 60_000,
    private val confirmTimeoutMs: Long = 120_000,
    private val inviteLifetimeMs: Long = 2 * 60_000,
    private val declineCooldownMs: Long = 60_000,
) {
    data class Code(val value: String, val expiresAt: Long)

    /** An invitation this desktop sent, waiting for the phone to answer. */
    data class Invite(val inviteId: String, val deviceId: String, val deviceName: String, val expiresAt: Long)

    sealed interface Outcome {
        data class Accepted(val response: PairResponse) : Outcome
        data class Rejected(val response: PairResponse) : Outcome
        data class Invalid(val message: String) : Outcome
        data class Busy(val message: String) : Outcome
    }

    private val _code = MutableStateFlow(newCode())
    private val _prompt = MutableStateFlow<PairingPrompt?>(null)
    private val _invites = MutableStateFlow<Map<String, Invite>>(emptyMap())
    val code: StateFlow<Code> = _code.asStateFlow()
    val prompt: StateFlow<PairingPrompt?> = _prompt.asStateFlow()
    val invites: StateFlow<Map<String, Invite>> = _invites.asStateFlow()

    private val lock = Mutex()
    private var pending: CompletableDeferred<Boolean>? = null
    private var failures = 0
    private val declinedUntil = mutableMapOf<String, Long>()

    fun rotate() { _code.value = newCode(); failures = 0 }

    fun rotateIfExpired() {
        if (now() >= _code.value.expiresAt) rotate()
        _invites.value = _invites.value.filterValues { it.expiresAt > now() }
    }

    fun answer(requestId: String, accept: Boolean) {
        if (_prompt.value?.requestId == requestId) pending?.complete(accept)
    }

    /** Invites a nearby phone; it shows "Connect?" and, if accepted, pairs as usual. */
    fun invite(deviceId: String, deviceName: String): Invite {
        val invite = Invite(Uuid.random().toString(), deviceId, deviceName.take(60), now() + inviteLifetimeMs)
        _invites.value = _invites.value.filterValues { it.deviceId != deviceId } + (invite.inviteId to invite)
        return invite
    }

    fun cancelInvite(deviceId: String) {
        _invites.value = _invites.value.filterValues { it.deviceId != deviceId }
    }

    /** Held open by a phone; answers as soon as this desktop invites it, or null after [timeoutMs]. */
    suspend fun awaitInvite(deviceId: String, timeoutMs: Long): InviteOffer? {
        val invite = withTimeoutOrNull(timeoutMs) {
            _invites.first { map -> map.values.any { it.deviceId == deviceId && it.expiresAt > now() } }
                .values.first { it.deviceId == deviceId }
        } ?: return null
        return InviteOffer(invite.inviteId, identity.deviceId, identity.deviceName)
    }

    suspend fun handle(request: PairRequest): Outcome {
        val deferred = lock.withLock {
            rotateIfExpired()
            if (pending != null) return Outcome.Busy("The desktop is already confirming another device. Try again in a moment.")
            val invited = request.inviteId?.let { id -> _invites.value[id]?.takeIf { it.deviceId == request.deviceId } }
            val code = request.pairingCode
            when {
                request.inviteId != null && invited == null ->
                    return Outcome.Invalid("That invitation has expired. Invite the phone again from the desktop.")
                code != null && !constantTimeEquals(code.uppercase(), _code.value.value) -> {
                    // Rotating after repeated misses stops anyone guessing codes on the network.
                    if (++failures >= MAX_FAILURES) rotate()
                    return Outcome.Invalid("That pairing code is wrong or has expired. Check the code on the desktop.")
                }
                // Someone who just said no shouldn't be asked again straight away.
                (declinedUntil[request.deviceId] ?: 0) > now() ->
                    return Outcome.Busy("The desktop declined a moment ago. Try again in a minute.")
            }
            val check = request.nonce?.let { pairingCheckNumber(fingerprint, it) } ?: verificationCode(fingerprint)
            CompletableDeferred<Boolean>().also {
                pending = it
                _prompt.value = PairingPrompt(Uuid.random().toString(), request.deviceName.take(60), check, invited = invited != null)
            }
        }
        val accepted = try {
            withTimeoutOrNull(confirmTimeoutMs) { deferred.await() } ?: false
        } finally {
            lock.withLock { pending = null; _prompt.value = null }
        }
        if (!accepted) {
            declinedUntil[request.deviceId] = now() + declineCooldownMs
            return Outcome.Rejected(PairResponse(accepted = false, message = "Pairing was declined on the desktop."))
        }

        val token = randomToken()
        devices.savePairing(
            deviceId = request.deviceId, name = request.deviceName.take(60), role = DeviceRole.CLIENT,
            tokenHash = sha256Hex(token.toByteArray()),
        )
        if (request.pairingCode != null) rotate() // Each code pairs one device.
        cancelInvite(request.deviceId)
        return Outcome.Accepted(
            PairResponse(accepted = true, authToken = token, hostDeviceId = identity.deviceId, hostDeviceName = identity.deviceName),
        )
    }

    private fun newCode() = Code(randomPairingCode(), now() + codeLifetimeMs)

    private companion object {
        const val MAX_FAILURES = 5
    }
}
