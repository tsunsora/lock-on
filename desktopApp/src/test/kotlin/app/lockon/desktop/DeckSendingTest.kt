package app.lockon.desktop

import androidx.room.Room
import app.lockon.data.DeckRepository
import app.lockon.data.db.LockOnDatabase
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSnapshot
import app.lockon.sync.DeviceRepository
import app.lockon.sync.DeviceRole
import app.lockon.sync.LocalIdentity
import app.lockon.sync.IncomingShare
import app.lockon.sync.OutgoingShare
import app.lockon.sync.ShareAnswer
import app.lockon.sync.ShareInbox
import app.lockon.sync.ShareOffer
import app.lockon.sync.ShareResponse
import app.lockon.sync.SyncException
import app.lockon.sync.SyncHost
import app.lockon.sync.SyncProtocol
import app.lockon.sync.answerShare
import app.lockon.sync.awaitShare
import app.lockon.sync.lockOnDefaults
import app.lockon.sync.sendDecks
import app.lockon.sync.sha256Hex
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** LocalSend-style sending: decks go to any nearby device, which saves copies only if its owner accepts. */
class DeckSendingTest {
    private val db = LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>())
    private val devices = DeviceRepository(db)
    private val decks = DeckRepository(db, "desk")
    private val identity = LocalIdentity("desk", "Desk")
    private var now = 1_000_000L
    private val inbox = ShareInbox(decks, now = { now }, declineCooldownMs = 30_000)
    private val outbox = ShareOutbox(identity, now = { now }, lifetimeMs = 60_000)
    private val pairing = PairingManager(devices, identity, "cd".repeat(32), now = { now })

    private val aggro = DeckSnapshot(name = "Aggro Red", notes = "Go fast", folder = "Tournament", entries = listOf(DeckEntry("SMP-003", 4)))
    private fun offer(vararg sent: DeckSnapshot, format: Int = SyncProtocol.DATA_FORMAT_VERSION) =
        ShareOffer("share-${sent.size}-$format", "phone", "Pixel", format, sent.toList())

    private suspend fun answerNext(accept: Boolean) = inbox.prompt.filterNotNull().first().also { inbox.answer(it.shareId, accept) }

    @Test fun aPhoneSendsDecksAndTheDesktopSavesCopiesOnlyOnceAccepted() = testApplication {
        application { lockOnSync(SyncHost(decks, devices), devices, pairing, identity, inbox = inbox, outbox = outbox) }
        val http = createClient { lockOnDefaults() }

        var sent = 0f
        val (prompt, response) = coroutineScope {
            val result = async { sendDecks(http, "", offer(aggro, DeckSnapshot(name = "Wing Control"))) { sent = it } }
            val shown = answerNext(accept = true)
            shown to result.await()
        }
        assertEquals(1f, sent, "the whole offer went before the answer came back")
        assertEquals("Pixel", prompt.senderName)
        assertEquals(listOf("Aggro Red", "Wing Control"), prompt.decks.map { it.name })
        assertTrue(response.accepted)
        assertEquals(2, response.saved)

        // The prompt stays to say the decks are saved, until it's closed.
        val done = assertNotNull(inbox.prompt.value)
        assertEquals(IncomingShare.State.SAVED, done.state)
        assertEquals(2, done.saved)
        inbox.dismiss(done.shareId)
        assertNull(inbox.prompt.value)

        val saved = decks.decks.first().map { it.snapshot }.sortedBy { it.name }
        assertEquals(listOf("Aggro Red", "Wing Control"), saved.map { it.name })
        assertEquals(aggro.entries, saved[0].entries)
        assertEquals("Go fast", saved[0].notes)
        assertNull(saved[0].folder, "folders belong to the sender")
    }

    @Test fun aDeclineSavesNothingAndCoolsDownThatSender() = runBlocking<Unit> {
        val declined = coroutineScope {
            val result = async { inbox.receive(offer(aggro)) }
            answerNext(accept = false)
            result.await()
        }
        assertTrue(declined.declined)
        assertTrue(decks.decks.first().isEmpty())

        val again = inbox.receive(offer(aggro))
        assertFalse(again.accepted)
        assertFalse(again.declined, "refused without asking anyone")
        assertNull(inbox.prompt.value)

        now += 31_000
        coroutineScope {
            val later = async { inbox.receive(offer(aggro)) }
            answerNext(accept = true)
            assertTrue(later.await().accepted)
        }
    }

    @Test fun decksInANewerFormatAreRefusedWithoutAsking() = runBlocking<Unit> {
        val response = inbox.receive(offer(aggro, format = SyncProtocol.DATA_FORMAT_VERSION + 1))
        assertFalse(response.accepted)
        assertTrue("newer format" in response.message)
        assertNull(inbox.prompt.value)
    }

    @Test fun theDesktopSendsToANearbyPhoneThatHoldsARequestOpen() = testApplication {
        application { lockOnSync(SyncHost(decks, devices), devices, pairing, identity, maxWaitMs = 1_500, inbox = inbox, outbox = outbox) }
        val http = createClient { lockOnDefaults() }

        assertNull(awaitShare(http, "", "phone", authToken = null), "nothing to send yet")
        outbox.send("phone", "Pixel", listOf(aggro))
        assertEquals(OutgoingShare.State.SENDING, outbox.sends.value["phone"]?.state)
        assertNull(awaitShare(http, "", "someone-else", authToken = null), "decks go only to the phone they were sent to")

        val offer = assertNotNull(awaitShare(http, "", "phone", authToken = null))
        assertEquals(OutgoingShare.State.WAITING, outbox.sends.value["phone"]?.state, "handed over, waiting for an answer")
        assertEquals("Desk", offer.senderName)
        assertEquals(listOf("Aggro Red"), offer.decks.map { it.name })

        answerShare(http, "", ShareAnswer(offer.shareId, "phone", ShareResponse(accepted = true, saved = 1)), authToken = null)
        val done = assertNotNull(outbox.sends.value["phone"])
        assertEquals(OutgoingShare.State.SAVED, done.state)
        assertEquals("Pixel saved 1 deck.", done.message)
        assertNull(awaitShare(http, "", "phone", authToken = null), "an answered send isn't offered again")
    }

    @Test fun decksForAPairedPhoneNeedItsToken() = testApplication {
        devices.savePairing(deviceId = "phone", name = "Pixel", role = DeviceRole.CLIENT, tokenHash = sha256Hex("secret".encodeToByteArray()))
        application { lockOnSync(SyncHost(decks, devices), devices, pairing, identity, maxWaitMs = 1_500, inbox = inbox, outbox = outbox) }
        val http = createClient { lockOnDefaults() }
        outbox.send("phone", "Pixel", listOf(aggro))

        assertFailsWith<SyncException> { awaitShare(http, "", "phone", authToken = null) }
        assertNotNull(awaitShare(http, "", "phone", authToken = "secret"))
    }

    @Test fun unansweredSendsExpire() {
        outbox.send("phone", "Pixel", listOf(aggro))
        now += 61_000
        outbox.expire()
        val send = assertNotNull(outbox.sends.value["phone"])
        assertEquals(OutgoingShare.State.FAILED, send.state)
        assertEquals("Pixel didn't answer.", send.message)
    }
}
