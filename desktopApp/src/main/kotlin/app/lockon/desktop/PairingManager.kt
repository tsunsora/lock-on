package app.lockon.desktop

import app.lockon.sync.DeviceRepository
import app.lockon.sync.DeviceRole
import app.lockon.sync.LocalIdentity
import app.lockon.sync.PairRequest
import app.lockon.sync.PairResponse
import app.lockon.sync.PairingPrompt
import app.lockon.sync.verificationCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.uuid.Uuid

/**
 * Short-lived pairing codes and the desktop-side confirmation. A phone needs the current code
 * and a person clicking Pair on the desktop; only then does it get a long-lived token.
 */
class PairingManager(
    private val devices: DeviceRepository,
    private val identity: LocalIdentity,
    private val fingerprint: String,
    private val now: () -> Long = System::currentTimeMillis,
    private val codeLifetimeMs: Long = 10 * 60_000,
    private val confirmTimeoutMs: Long = 120_000,
) {
    data class Code(val value: String, val expiresAt: Long)

    sealed interface Outcome {
        data class Accepted(val response: PairResponse) : Outcome
        data class Rejected(val response: PairResponse) : Outcome
        data class Invalid(val message: String) : Outcome
        data class Busy(val message: String) : Outcome
    }

    private val _code = MutableStateFlow(newCode())
    private val _prompt = MutableStateFlow<PairingPrompt?>(null)
    val code: StateFlow<Code> = _code.asStateFlow()
    val prompt: StateFlow<PairingPrompt?> = _prompt.asStateFlow()

    private val lock = Mutex()
    private var pending: CompletableDeferred<Boolean>? = null
    private var failures = 0

    fun rotate() { _code.value = newCode(); failures = 0 }

    fun rotateIfExpired() { if (now() >= _code.value.expiresAt) rotate() }

    fun answer(requestId: String, accept: Boolean) {
        if (_prompt.value?.requestId == requestId) pending?.complete(accept)
    }

    suspend fun handle(request: PairRequest): Outcome {
        val deferred = lock.withLock {
            rotateIfExpired()
            if (pending != null) return Outcome.Busy("The desktop is already confirming another device. Try again in a moment.")
            if (!constantTimeEquals(request.pairingCode.uppercase(), _code.value.value)) {
                // Rotating after repeated misses stops anyone guessing codes on the network.
                if (++failures >= MAX_FAILURES) rotate()
                return Outcome.Invalid("That pairing code is wrong or has expired. Check the code on the desktop.")
            }
            CompletableDeferred<Boolean>().also {
                pending = it
                _prompt.value = PairingPrompt(Uuid.random().toString(), request.deviceName.take(60), verificationCode(fingerprint))
            }
        }
        val accepted = try {
            withTimeoutOrNull(confirmTimeoutMs) { deferred.await() } ?: false
        } finally {
            lock.withLock { pending = null; _prompt.value = null }
        }
        if (!accepted) return Outcome.Rejected(PairResponse(accepted = false, message = "Pairing was declined on the desktop."))

        val token = randomToken()
        devices.savePairing(
            deviceId = request.deviceId, name = request.deviceName.take(60), role = DeviceRole.CLIENT,
            tokenHash = sha256Hex(token.toByteArray()),
        )
        rotate() // Each code pairs one device.
        return Outcome.Accepted(
            PairResponse(accepted = true, authToken = token, hostDeviceId = identity.deviceId, hostDeviceName = identity.deviceName),
        )
    }

    private fun newCode() = Code(randomPairingCode(), now() + codeLifetimeMs)

    private companion object {
        const val MAX_FAILURES = 5
    }
}
