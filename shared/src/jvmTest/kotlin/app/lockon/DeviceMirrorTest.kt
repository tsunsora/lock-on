package app.lockon

import androidx.room.Room
import app.lockon.data.CatalogRepository
import app.lockon.data.DeckRepository
import app.lockon.data.InterfaceSize
import app.lockon.data.SettingsRepository
import app.lockon.data.ThemeChoice
import app.lockon.data.db.LockOnDatabase
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.MatchRecord
import app.lockon.model.MatchResult
import app.lockon.sync.DeviceMirror
import app.lockon.sync.DeviceRepository
import app.lockon.sync.DeviceRole
import app.lockon.sync.HelloResponse
import app.lockon.sync.LocalIdentity
import app.lockon.sync.MirrorPush
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
import kotlin.test.assertTrue

/** Making a phone match a desktop and the other way round, through a direct transport. */
class DeviceMirrorTest {
    private var clock = 1_000L
    private val tick: () -> Long = { clock++ }

    private inner class Device(val id: String) {
        val db = LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>())
        val decks = DeckRepository(db, id, tick)
        val devices = DeviceRepository(db)
        val settings = SettingsRepository(db.metaDao())
        val catalog = CatalogRepository(db)
        val mirror = DeviceMirror(LocalIdentity(id, id), decks, settings, catalog)

        suspend fun names() = decks.decks.first().map { it.snapshot.name }.toSet()
    }

    private val phone = Device("phone")
    private val desktop = Device("desktop")
    private val host = SyncHost(desktop.decks, desktop.devices, desktop.mirror)
    private val client = SyncClient(phone.decks, phone.devices, phone.mirror)

    private val transport = object : SyncTransport {
        override suspend fun hello() = HelloResponse(deviceId = "desktop", deviceName = "Desktop", mirrors = true, catalog = host.catalogKey())
        override suspend fun push(request: PushRequest): PushResponse = host.push("phone", request)
        override suspend fun pull(after: Long, limit: Int): PullResponse = host.pull("phone", after, limit)
        override suspend fun state() = host.state("phone")
        override suspend fun catalog() = host.catalog()
        override suspend fun mirror(push: MirrorPush) = host.mirror("phone", push)
    }

    @AfterTest fun close() { phone.db.close(); desktop.db.close() }

    private suspend fun setUp(): Triple<String, String, String> {
        phone.settings.load(); desktop.settings.load()
        phone.devices.savePairing("desktop", "Desktop", DeviceRole.HOST, "127.0.0.1", 53340, "fp")
        desktop.devices.savePairing("phone", "Phone", DeviceRole.CLIENT, tokenHash = "hash")
        // A deck both have, then edited apart; one only on each side.
        val shared = desktop.decks.create(DeckSnapshot(name = "Shared"))
        client.sync("desktop", transport)
        phone.decks.save(shared, phone.decks.currentSnapshot(shared)!!.withQuantity("SMP-001", DeckSection.MAIN, 4))
        desktop.decks.save(shared, desktop.decks.currentSnapshot(shared)!!.withQuantity("SMP-002", DeckSection.MAIN, 2))
        val phoneOnly = phone.decks.create(DeckSnapshot(name = "Phone only"))
        val desktopOnly = desktop.decks.create(DeckSnapshot(name = "Desktop only", folder = "Tournament"))
        desktop.decks.moveToFolder(emptyList(), "Empty for now")
        desktop.settings.setTheme(ThemeChoice.DARK)
        desktop.settings.setSize(InterfaceSize.LARGE)
        return Triple(shared, phoneOnly, desktopOnly)
    }

    @Test fun phoneTakesOnTheDesktop() = runTest {
        val (shared, phoneOnly, desktopOnly) = setUp()
        val summary = client.take("desktop", transport)

        assertEquals(setOf("Shared", "Desktop only"), phone.names())
        val onPhone = phone.decks.currentSnapshot(shared)!!
        assertEquals(2, onPhone.quantityOf("SMP-002", DeckSection.MAIN))
        assertEquals(0, onPhone.quantityOf("SMP-001", DeckSection.MAIN), "the phone's own edit is replaced")
        assertFalse(phone.decks.decks.first().any { it.hasConflict })
        assertEquals("Tournament", phone.decks.currentSnapshot(desktopOnly)?.folder)
        assertTrue("Empty for now" in phone.decks.keptFolders.first())
        assertEquals(ThemeChoice.DARK, phone.settings.theme.value)
        assertEquals(InterfaceSize.LARGE, phone.settings.size.value)
        assertEquals(1, summary.updated)
        assertEquals(1, summary.added)
        assertEquals(1, summary.removed)
        assertTrue(summary.settings)
        // What was replaced stays in the phone's history.
        assertTrue(phone.decks.history(shared).any { it.snapshot.quantityOf("SMP-001", DeckSection.MAIN) == 4 })

        // The desktop is untouched, and merging afterwards keeps the two the same.
        assertEquals(setOf("Shared", "Desktop only"), desktop.names())
        client.sync("desktop", transport)
        assertEquals(setOf("Shared", "Desktop only"), desktop.names())
        assertEquals(setOf("Shared", "Desktop only"), phone.names())
        assertFalse(desktop.decks.decks.first().any { it.hasConflict })
        assertEquals(desktop.decks.currentSnapshot(shared), phone.decks.currentSnapshot(shared))
        assertTrue(phone.decks.currentSnapshot(phoneOnly)?.deleted != false)
    }

    @Test fun desktopTakesOnThePhone() = runTest {
        val (shared, _, _) = setUp()
        phone.settings.setTheme(ThemeChoice.LIGHT)
        val summary = client.give("desktop", transport)

        assertEquals(setOf("Shared", "Phone only"), desktop.names())
        assertEquals(4, desktop.decks.currentSnapshot(shared)!!.quantityOf("SMP-001", DeckSection.MAIN))
        assertEquals(0, desktop.decks.currentSnapshot(shared)!!.quantityOf("SMP-002", DeckSection.MAIN))
        assertFalse("Empty for now" in desktop.decks.keptFolders.first())
        assertEquals(ThemeChoice.LIGHT, desktop.settings.theme.value)
        assertEquals(InterfaceSize.DEFAULT, desktop.settings.size.value)
        assertEquals(1, summary.removed)
        assertEquals(setOf("Shared", "Phone only"), phone.names(), "the phone is untouched")
    }

    @Test fun matchingAgainChangesNothing() = runTest {
        setUp()
        client.take("desktop", transport)
        val again = client.take("desktop", transport)
        assertEquals(0, again.updated + again.added + again.removed)
        assertFalse(again.settings)
    }

    @Test fun matchLogsAreCopiedExactly() = runTest {
        val (shared, _, _) = setUp()
        val game = MatchRecord(id = "g1", playedAt = 5, result = MatchResult.WIN)
        phone.decks.save(shared, phone.decks.currentSnapshot(shared)!!.copy(matches = listOf(game)))
        client.take("desktop", transport)
        assertEquals(emptyList(), phone.decks.currentSnapshot(shared)!!.matches, "a game logged only on the phone goes when it matches the desktop")
    }
}
