package app.lockon.desktop

import androidx.room.Room
import app.lockon.data.DeckRepository
import app.lockon.data.db.LockOnDatabase
import app.lockon.model.DeckSnapshot
import app.lockon.sync.DeviceRepository
import app.lockon.sync.DeviceRole
import app.lockon.sync.HttpSyncTransport
import app.lockon.sync.LocalIdentity
import app.lockon.sync.PairRequest
import app.lockon.sync.SyncClient
import app.lockon.sync.SyncException
import app.lockon.sync.SyncHost
import app.lockon.sync.lockOnDefaults
import app.lockon.sync.requestPairing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Pairing and sync through the real HTTP routes, with the shared client on the other end. */
class SyncRoutesTest {
    private val desktopDb = LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>())
    private val phoneDb = LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>())
    private val identity = LocalIdentity("desktop-id", "Test Desktop")
    private val desktopDevices = DeviceRepository(desktopDb)
    private val desktopDecks = DeckRepository(desktopDb, identity.deviceId)
    private val phoneDevices = DeviceRepository(phoneDb)
    private val phoneDecks = DeckRepository(phoneDb, "phone-id")
    private val pairing = PairingManager(desktopDevices, identity, fingerprint = "ab".repeat(32))

    @Test fun pairThenSyncBothWays() = testApplication {
        application { lockOnSync(SyncHost(desktopDecks, desktopDevices), desktopDevices, pairing, identity) }
        val http = createClient { lockOnDefaults() }

        // A wrong code is refused without bothering the person at the desktop.
        assertFailsWith<SyncException> { requestPairing(http, "", PairRequest("WRONG1", "phone-id", "Phone")) }

        val response = coroutineScope {
            val request = async { requestPairing(http, "", PairRequest(pairing.code.value.value, "phone-id", "Phone")) }
            val prompt = pairing.prompt.filterNotNull().first()
            assertEquals("Phone", prompt.deviceName)
            pairing.answer(prompt.requestId, accept = true)
            request.await()
        }
        assertTrue(response.accepted)
        val token = response.authToken!!
        phoneDevices.savePairing("desktop-id", "Test Desktop", DeviceRole.HOST, "localhost", 0, "fp")

        val id = phoneDecks.create(DeckSnapshot(name = "From phone"))
        desktopDecks.create(DeckSnapshot(name = "From desktop"))
        val result = SyncClient(phoneDecks, phoneDevices).sync("desktop-id", HttpSyncTransport(http, "", token))
        assertEquals(1, result.sent)
        assertEquals(1, result.received)
        assertEquals("From phone", desktopDecks.currentSnapshot(id)?.name)
        assertEquals(setOf("From phone", "From desktop"), phoneDecks.decks.first().map { it.snapshot.name }.toSet())

        // A stolen or stale token gets nothing.
        val stranger = SyncClient(phoneDecks, phoneDevices)
        val error = assertFailsWith<SyncException> { stranger.sync("desktop-id", HttpSyncTransport(http, "", "not-a-token")) }
        assertTrue("Pair again" in error.message.orEmpty())
    }

    @Test fun declinedPairingStoresNothing() = testApplication {
        application { lockOnSync(SyncHost(desktopDecks, desktopDevices), desktopDevices, pairing, identity) }
        val http = createClient { lockOnDefaults() }
        val response = coroutineScope {
            val request = async { requestPairing(http, "", PairRequest(pairing.code.value.value, "phone-id", "Phone")) }
            pairing.answer(pairing.prompt.filterNotNull().first().requestId, accept = false)
            request.await()
        }
        assertFalse(response.accepted)
        assertTrue(desktopDevices.pairedDevices.first().isEmpty())
    }
}
