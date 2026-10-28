package app.lockon

import androidx.room.Room
import app.lockon.data.DeckRepository
import app.lockon.data.db.LockOnDatabase
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.MatchRecord
import app.lockon.model.MatchResult
import app.lockon.sync.DeviceRepository
import app.lockon.sync.DeviceRole
import app.lockon.sync.HelloResponse
import app.lockon.sync.PullResponse
import app.lockon.sync.PushRequest
import app.lockon.sync.PushResponse
import app.lockon.sync.SyncClient
import app.lockon.sync.SyncHost
import app.lockon.sync.SyncTransport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A phone and a desktop, each with its own database, syncing through a direct transport. */
class SyncEngineTest {
    private var clock = 1_000L
    private val tick: () -> Long = { clock++ }

    private inner class Device(val id: String) {
        val db = LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>())
        val decks = DeckRepository(db, id, tick)
        val devices = DeviceRepository(db)
    }

    private val phone = Device("phone")
    private val desktop = Device("desktop")
    private val host = SyncHost(desktop.decks, desktop.devices)
    private val client = SyncClient(phone.decks, phone.devices)

    /** Calls the host directly, optionally failing to simulate a dropped connection. */
    private inner class Loopback(var failPullAfterApply: Boolean = false) : SyncTransport {
        val pushed = mutableListOf<PushRequest>()
        override suspend fun hello() = HelloResponse(deviceId = "desktop", deviceName = "Desktop")
        override suspend fun push(request: PushRequest): PushResponse { pushed += request; return host.push("phone", request) }
        override suspend fun pull(after: Long, limit: Int): PullResponse {
            val response = host.pull("phone", after, limit)
            if (failPullAfterApply) { failPullAfterApply = false; throw java.io.IOException("connection dropped") }
            return response
        }
    }

    private val transport = Loopback()

    private suspend fun pair() {
        phone.devices.savePairing("desktop", "Desktop", DeviceRole.HOST, "127.0.0.1", 53340, "fp")
        desktop.devices.savePairing("phone", "Phone", DeviceRole.CLIENT, tokenHash = "hash")
    }

    private suspend fun sync() = client.sync("desktop", transport)

    @AfterTest fun close() { phone.db.close(); desktop.db.close() }

    @Test fun deckMakesTheRoundTrip() = runTest {
        pair()
        val id = phone.decks.create(DeckSnapshot(name = "Blue Red"))
        phone.decks.save(id, phone.decks.currentSnapshot(id)!!.withQuantity("SMP-001", DeckSection.MAIN, 4))
        assertEquals(1, sync().sent)

        val onDesktop = desktop.decks.currentSnapshot(id)!!
        assertEquals(4, onDesktop.quantityOf("SMP-001", DeckSection.MAIN))
        desktop.decks.save(id, onDesktop.withQuantity("SMP-002", DeckSection.MAIN, 2))

        val back = sync()
        assertEquals(0, back.sent, "the phone must not echo the desktop's revision back")
        assertEquals(1, back.received)
        assertEquals(2, phone.decks.currentSnapshot(id)!!.quantityOf("SMP-002", DeckSection.MAIN))
        assertFalse(phone.decks.decks.first().single().hasConflict)
    }

    @Test fun sideDeckCardsSyncWithTheDeck() = runTest {
        pair()
        val id = phone.decks.create(DeckSnapshot(name = "Maybe list"))
        phone.decks.save(id, phone.decks.currentSnapshot(id)!!.withQuantity("SMP-013", DeckSection.SIDE, 2))
        sync()
        assertEquals(2, desktop.decks.currentSnapshot(id)!!.quantityOf("SMP-013", DeckSection.SIDE))
        assertEquals(2, desktop.decks.decks.first().single().snapshot.quantityOf("SMP-013", DeckSection.SIDE))
    }

    @Test fun phonesReachEachOtherThroughTheDesktop() = runTest {
        pair()
        // A second phone paired with the same desktop.
        val phone2 = Device("phone2")
        phone2.devices.savePairing("desktop", "Desktop", DeviceRole.HOST, "127.0.0.1", 53340, "fp")
        desktop.devices.savePairing("phone2", "Phone 2", DeviceRole.CLIENT, tokenHash = "hash2")
        val client2 = SyncClient(phone2.decks, phone2.devices)
        val transport2 = object : SyncTransport {
            override suspend fun hello() = HelloResponse(deviceId = "desktop", deviceName = "Desktop")
            override suspend fun push(request: PushRequest) = host.push("phone2", request)
            override suspend fun pull(after: Long, limit: Int) = host.pull("phone2", after, limit)
        }

        val id = phone.decks.create(DeckSnapshot(name = "Built on phone 1"))
        sync()
        client2.sync("desktop", transport2)
        assertEquals("Built on phone 1", phone2.decks.currentSnapshot(id)?.name, "the desktop relays phone 1's deck")

        phone2.decks.save(id, phone2.decks.currentSnapshot(id)!!.copy(name = "Edited on phone 2"))
        client2.sync("desktop", transport2)
        sync()
        assertEquals("Edited on phone 2", phone.decks.currentSnapshot(id)?.name, "and phone 2's edit comes back")
        phone2.db.close()
    }

    @Test fun autosavesFoldUntilShared() = runTest {
        pair()
        val id = phone.decks.create(DeckSnapshot(name = "Draft"))
        repeat(5) { n -> phone.decks.save(id, phone.decks.currentSnapshot(id)!!.withQuantity("SMP-001", DeckSection.MAIN, n + 1)) }
        assertEquals(1, phone.db.revisionDao().forDeck(id).size)
        sync()
        phone.decks.save(id, phone.decks.currentSnapshot(id)!!.copy(name = "Renamed"))
        assertEquals(2, phone.db.revisionDao().forDeck(id).size, "a shared revision is never edited in place")
    }

    @Test fun concurrentEditsKeepBothVersionsUntilResolved() = runTest {
        pair()
        val id = phone.decks.create(DeckSnapshot(name = "Shared"))
        sync()
        phone.decks.save(id, phone.decks.currentSnapshot(id)!!.copy(name = "Phone edit"))
        desktop.decks.save(id, desktop.decks.currentSnapshot(id)!!.copy(name = "Desktop edit"))
        sync()

        for (side in listOf(phone, desktop)) {
            assertTrue(side.decks.decks.first().single().hasConflict)
            assertEquals(setOf("Phone edit", "Desktop edit"), side.decks.versions(id).map { it.snapshot.name }.toSet())
        }

        val keep = phone.decks.versions(id).first { it.snapshot.name == "Desktop edit" }
        phone.decks.resolve(id, keep.snapshot)
        sync()
        for (side in listOf(phone, desktop)) {
            val deck = side.decks.decks.first().single()
            assertFalse(deck.hasConflict)
            assertEquals("Desktop edit", deck.snapshot.name)
        }
    }

    @Test fun deletionPropagatesAndAnUnchangedOldCopyCannotRestoreIt() = runTest {
        pair()
        val id = phone.decks.create(DeckSnapshot(name = "Doomed"))
        sync()
        desktop.decks.delete(id)
        sync()
        assertTrue(phone.decks.decks.first().isEmpty())
        // Syncing again, with the phone's old revision still on disk, must not bring it back.
        sync()
        assertTrue(phone.decks.decks.first().isEmpty())
        assertTrue(desktop.decks.decks.first().isEmpty())
    }

    @Test fun editAgainstDeletionIsSurfacedNotLost() = runTest {
        pair()
        val id = phone.decks.create(DeckSnapshot(name = "Contested"))
        sync()
        desktop.decks.delete(id)
        phone.decks.save(id, phone.decks.currentSnapshot(id)!!.copy(notes = "still using this"))
        sync()
        val deck = phone.decks.decks.first().single()
        assertTrue(deck.hasConflict)
        assertEquals("still using this", deck.snapshot.notes)
    }

    @Test fun deletingAConflictedDeckDeletesEveryVersion() = runTest {
        pair()
        val id = phone.decks.create(DeckSnapshot(name = "Shared"))
        sync()
        phone.decks.save(id, phone.decks.currentSnapshot(id)!!.copy(name = "Phone edit"))
        desktop.decks.save(id, desktop.decks.currentSnapshot(id)!!.copy(name = "Desktop edit"))
        sync()
        assertTrue(phone.decks.decks.first().single().hasConflict)

        phone.decks.delete(id)
        assertTrue(phone.decks.decks.first().isEmpty(), "the other version must not take the deleted one's place")
        sync()
        assertTrue(desktop.decks.decks.first().isEmpty())
    }

    @Test fun anEditBasedOnAnOlderVersionConflictsInsteadOfOverwriting() = runTest {
        pair()
        val id = desktop.decks.create(DeckSnapshot(name = "Open on desktop"))
        sync()
        // The desktop editor loads the deck, then the phone's edit syncs in before the autosave.
        val base = desktop.decks.decks.first().single().headRevisionId
        val draft = desktop.decks.currentSnapshot(id)!!.copy(notes = "desktop draft")
        phone.decks.save(id, phone.decks.currentSnapshot(id)!!.withQuantity("SMP-001", DeckSection.MAIN, 2))
        sync()
        desktop.decks.save(id, draft, base)

        assertTrue(desktop.decks.decks.first().single().hasConflict)
        val versions = desktop.decks.versions(id).map { it.snapshot }
        assertTrue(versions.any { it.quantityOf("SMP-001", DeckSection.MAIN) == 2 }, "the phone's edit survives")
        assertTrue(versions.any { it.notes == "desktop draft" })
    }

    @Test fun anEditorsOwnEarlierSavesAreNotAConflict() = runTest {
        pair()
        val id = phone.decks.create(DeckSnapshot(name = "Mine"))
        sync()
        val base = phone.decks.decks.first().single().headRevisionId
        // The first save starts a new version because the old one was shared; the second builds on it.
        phone.decks.save(id, phone.decks.currentSnapshot(id)!!.copy(notes = "one"), base)
        phone.decks.save(id, phone.decks.currentSnapshot(id)!!.copy(notes = "two"), base)
        val deck = phone.decks.decks.first().single()
        assertFalse(deck.hasConflict)
        assertEquals("two", deck.snapshot.notes)
    }

    @Test fun undoingADeleteBringsBackEveryVersionOfAConflictedDeck() = runTest {
        pair()
        val id = phone.decks.create(DeckSnapshot(name = "Shared"))
        sync()
        phone.decks.save(id, phone.decks.currentSnapshot(id)!!.copy(name = "Phone edit"))
        desktop.decks.save(id, desktop.decks.currentSnapshot(id)!!.copy(name = "Desktop edit"))
        sync()
        val before = phone.decks.decks.first().single()

        phone.decks.delete(id)
        phone.decks.restore(id, before.snapshot)
        val after = phone.decks.decks.first().single()
        assertTrue(after.hasConflict)
        assertEquals(setOf("Phone edit", "Desktop edit"), phone.decks.versions(id).map { it.snapshot.name }.toSet())
    }

    @Test fun undoingAnOrdinaryDeleteKeepsTheLatestEdit() = runTest {
        val id = phone.decks.create(DeckSnapshot(name = "Mine"))
        phone.decks.save(id, phone.decks.currentSnapshot(id)!!.copy(notes = "fresh edit"))
        val before = phone.decks.currentSnapshot(id)!!
        phone.decks.delete(id)
        phone.decks.restore(id, before)
        assertEquals("fresh edit", phone.decks.decks.first().single().snapshot.notes)
    }

    @Test fun resolvingKeepsGamesRemovedOnEitherDeviceRemoved() = runTest {
        pair()
        val mistake = MatchRecord("m1", playedAt = 1, result = MatchResult.WIN)
        val id = phone.decks.create(DeckSnapshot(name = "Logged", matches = listOf(mistake)))
        sync()
        // The phone removes a mistaken game while the desktop logs a new one and renames the deck.
        phone.decks.save(id, phone.decks.currentSnapshot(id)!!.copy(matches = emptyList()))
        val fresh = MatchRecord("m2", playedAt = 2, result = MatchResult.LOSS)
        desktop.decks.save(id, desktop.decks.currentSnapshot(id)!!.let { it.copy(name = "Renamed", matches = it.matches + fresh) })
        sync()

        val desktopVersion = phone.decks.versions(id).first { it.snapshot.name == "Renamed" }
        phone.decks.resolve(id, desktopVersion.snapshot)
        assertEquals(listOf("m2"), phone.decks.currentSnapshot(id)!!.matches.map { it.id })
    }

    @Test fun aBackupRestoredOnAFreshPhoneIsNotAConflict() = runTest {
        val id = desktop.decks.create(DeckSnapshot(name = "Backed up", notes = "same everywhere"))
        // The phone was reinstalled and restored from a backup made before it was paired.
        phone.decks.importBackup(desktop.decks.exportBackup())
        pair()
        sync()
        sync()
        for (side in listOf(phone, desktop)) {
            val deck = side.decks.decks.first().single()
            assertFalse(deck.hasConflict, "identical copies join on their own")
            assertEquals(id, deck.deckId)
        }
    }

    @Test fun interruptedPullAndRetriedPushAreHarmless() = runTest {
        pair()
        val id = desktop.decks.create(DeckSnapshot(name = "From desktop"))
        transport.failPullAfterApply = true
        runCatching { sync() }
        assertNull(phone.decks.currentSnapshot(id), "nothing was applied because the response never arrived")
        assertEquals("Connection dropped", phone.devices.device("desktop")!!.lastError?.replaceFirstChar { it.uppercase() })

        sync()
        assertEquals("From desktop", phone.decks.currentSnapshot(id)!!.name)

        // Replaying a batch the host already applied changes nothing.
        phone.decks.create(DeckSnapshot(name = "From phone"))
        sync()
        val replay = host.push("phone", transport.pushed.last())
        assertEquals(0, replay.received)
        assertEquals(2, desktop.decks.decks.first().size)
    }

    @Test fun incompatibleProtocolStopsBeforeSendingAnything() = runTest {
        pair()
        phone.decks.create(DeckSnapshot(name = "Pending"))
        val future = object : SyncTransport by transport {
            override suspend fun hello() = HelloResponse(deviceId = "desktop", deviceName = "Desktop", minProtocolVersion = 99, protocolVersion = 99)
        }
        val error = runCatching { client.sync("desktop", future) }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("Update Lock On"))
        assertTrue(desktop.decks.decks.first().isEmpty())
    }
}
