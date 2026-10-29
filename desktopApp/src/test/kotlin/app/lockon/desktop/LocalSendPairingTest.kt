package app.lockon.desktop

import androidx.room.Room
import app.lockon.data.DeckRepository
import app.lockon.data.db.LockOnDatabase
import app.lockon.sync.DeviceRepository
import app.lockon.sync.LocalIdentity
import app.lockon.sync.PairRequest
import app.lockon.sync.SyncHost
import app.lockon.sync.awaitInvite
import app.lockon.sync.lockOnDefaults
import app.lockon.sync.pairingCheckNumber
import app.lockon.sync.requestPairing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** LocalSend-style pairing: tap a nearby desktop, or be invited by it, and confirm a matching number. */
class LocalSendPairingTest {
    private val db = LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>())
    private val devices = DeviceRepository(db)
    private val identity = LocalIdentity("desk", "Desk")
    private val fingerprint = "cd".repeat(32)
    private var now = 1_000_000L
    private val pairing = PairingManager(devices, identity, fingerprint, now = { now }, inviteLifetimeMs = 60_000, declineCooldownMs = 30_000)

    private suspend fun answerNext(accept: Boolean) = pairing.prompt.filterNotNull().first().also { pairing.answer(it.requestId, accept) }

    @Test fun tapToPairNeedsNoCodeButShowsTheMatchingNumber() = runBlocking<Unit> {
        val (prompt, outcome) = coroutineScope {
            val result = async { pairing.handle(PairRequest(deviceId = "phone", deviceName = "Pixel", nonce = "n-1")) }
            answerNext(accept = true) to result.await()
        }
        assertEquals(pairingCheckNumber(fingerprint, "n-1"), prompt.verificationCode, "the phone computes the same number")
        assertFalse(prompt.invited)
        assertIs<PairingManager.Outcome.Accepted>(outcome)
        assertNotNull(devices.device("phone"))
    }

    @Test fun anImpostorsRequestShowsADifferentNumber() {
        // The real phone displays the number for its own nonce; an impostor can't make the desktop show it.
        assertTrue(pairingCheckNumber(fingerprint, "real-phone-nonce") != pairingCheckNumber(fingerprint, "impostor-nonce"))
        // An interceptor with its own certificate changes the phone's number too.
        assertTrue(pairingCheckNumber(fingerprint, "n") != pairingCheckNumber("ef".repeat(32), "n"))
        assertEquals(7, pairingCheckNumber(fingerprint, "n").length, "six digits shown as two groups")
    }

    @Test fun aDeclineCoolsDownThatDevice() = runBlocking<Unit> {
        coroutineScope {
            val first = async { pairing.handle(PairRequest(deviceId = "phone", deviceName = "Pixel", nonce = "a")) }
            answerNext(accept = false)
            assertIs<PairingManager.Outcome.Rejected>(first.await())
        }
        assertIs<PairingManager.Outcome.Busy>(pairing.handle(PairRequest(deviceId = "phone", deviceName = "Pixel", nonce = "b")))
        now += 31_000
        coroutineScope {
            val later = async { pairing.handle(PairRequest(deviceId = "phone", deviceName = "Pixel", nonce = "c")) }
            answerNext(accept = true)
            assertIs<PairingManager.Outcome.Accepted>(later.await())
        }
    }

    @Test fun codesFromTheQrStillWorkAndWrongOnesAreRefused() = runBlocking<Unit> {
        assertIs<PairingManager.Outcome.Invalid>(pairing.handle(PairRequest(pairingCode = "WRONG1", deviceId = "p", deviceName = "P")))
        coroutineScope {
            val ok = async { pairing.handle(PairRequest(pairingCode = pairing.code.value.value, deviceId = "p", deviceName = "P")) }
            answerNext(accept = true)
            assertIs<PairingManager.Outcome.Accepted>(ok.await())
        }
    }

    @Test fun invitationsReachThePhoneOverHttpAndStillNeedConfirmation() = testApplication {
        application {
            lockOnSync(SyncHost(DeckRepository(db, "desk"), devices), devices, pairing, identity, maxWaitMs = 1_500)
        }
        val http = createClient { lockOnDefaults() }

        assertNull(awaitInvite(http, "", "phone"), "no invitation yet: the held request times out empty")

        val invite = pairing.invite("phone", "Pixel")
        val offer = assertNotNull(awaitInvite(http, "", "phone"))
        assertEquals(invite.inviteId, offer.inviteId)
        assertEquals("Desk", offer.hostName)
        assertNull(awaitInvite(http, "", "someone-else"), "invitations are only offered to the device they name")
        assertNull(awaitInvite(http, "", "phone", answered = invite.inviteId), "an answered invitation isn't offered again")
        val again = pairing.invite("phone", "Pixel")
        assertEquals(again.inviteId, awaitInvite(http, "", "phone", answered = invite.inviteId)?.inviteId, "a new invitation still gets through")

        val (prompt, response) = coroutineScope {
            val result = async { requestPairing(http, "", PairRequest(deviceId = "phone", deviceName = "Pixel", nonce = "z", inviteId = again.inviteId)) }
            answerNext(accept = true) to result.await()
        }
        assertTrue(prompt.invited, "the desktop says the phone accepted its invitation")
        assertTrue(response.accepted)
        assertTrue(pairing.invites.value.isEmpty(), "the invitation is used up")
    }

    @Test fun expiredOrForeignInvitationsAreRefused() = runBlocking<Unit> {
        val invite = pairing.invite("phone", "Pixel")
        assertIs<PairingManager.Outcome.Invalid>(
            pairing.handle(PairRequest(deviceId = "other-phone", deviceName = "X", inviteId = invite.inviteId)),
        )
        now += 61_000
        assertIs<PairingManager.Outcome.Invalid>(pairing.handle(PairRequest(deviceId = "phone", deviceName = "Pixel", inviteId = invite.inviteId)))
    }
}
