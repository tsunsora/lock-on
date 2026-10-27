package app.lockon

import app.lockon.sync.AutoSyncLoop
import app.lockon.sync.AutoSyncState
import app.lockon.sync.Discovery
import app.lockon.sync.LocalIdentity
import app.lockon.sync.SyncProtocol
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AutoSyncTest {
    private val hosts = MutableStateFlow(listOf("desk"))
    private val local = MutableStateFlow(0L)
    private val remote = Channel<Boolean>()
    private var syncs = 0
    private var failing = false
    private val states = mutableMapOf<String, AutoSyncState?>()

    private val loop = AutoSyncLoop(
        hostIds = hosts,
        localChanges = local,
        syncOnce = { syncs++; if (failing) error("desktop closed") },
        waitForRemote = { remote.receive() },
        onState = { id, state -> states[id] = state },
        debounceMs = 2_000,
        minBackoffMs = 5_000,
        maxBackoffMs = 20_000,
    )

    @Test fun syncsOnStartThenOncePerBurstOfEdits() = runTest {
        val job = launch { loop.run() }
        runCurrent()
        assertEquals(1, syncs)
        assertEquals(AutoSyncState.LIVE, states["desk"])

        // Five quick autosaves become one sync after they settle.
        repeat(5) { local.value = it + 1L; advanceTimeBy(300) }
        assertEquals(1, syncs)
        advanceTimeBy(2_100)
        assertEquals(2, syncs)
        job.cancel()
    }

    @Test fun desktopChangesTriggerASyncAndQuietWaitsDoNot() = runTest {
        val job = launch { loop.run() }
        runCurrent()
        remote.send(true)
        runCurrent()
        assertEquals(2, syncs)
        remote.send(false)
        runCurrent()
        assertEquals(2, syncs, "a wait that timed out with nothing new needs no sync")
        job.cancel()
    }

    @Test fun failuresBackOffQuietlyAndANudgeRetriesAtOnce() = runTest {
        failing = true
        val job = launch { loop.run() }
        runCurrent()
        assertEquals(1, syncs)
        assertEquals(AutoSyncState.WAITING, states["desk"])
        advanceTimeBy(4_900)
        assertEquals(1, syncs)
        advanceTimeBy(200)
        assertEquals(2, syncs, "first retry after the 5 s backoff")
        advanceTimeBy(9_000)
        assertEquals(2, syncs, "second retry waits twice as long")

        failing = false
        loop.nudge("desk")
        runCurrent()
        assertEquals(3, syncs)
        assertEquals(AutoSyncState.LIVE, states["desk"])
        job.cancel()
    }

    @Test fun turningAutoSyncOffClearsTheState() = runTest {
        val job = launch { loop.run() }
        runCurrent()
        hosts.value = emptyList()
        runCurrent()
        assertNull(states["desk"])
        job.cancel()
    }
}

class DiscoveryTest {
    private val identity = LocalIdentity("desk-id", "Tsun's PC")
    private val fingerprint = "ab".repeat(32)

    @Test fun txtRecordRoundTrips() {
        val found = Discovery.parse(Discovery.txt(identity, fingerprint), "192.168.1.20", 53340, "Tsun's PC")
        assertNotNull(found)
        assertEquals("desk-id", found.deviceId)
        assertEquals(fingerprint, found.fingerprint)
        assertEquals(true, found.compatible)
    }

    @Test fun phonesAnnounceTheirRoleAndOldDesktopsDefaultToDesktop() {
        val phone = Discovery.parse(Discovery.txt(identity, null, SyncProtocol.ROLE_PHONE), "192.168.1.52", 53340, "Pixel")!!
        assertFalse(phone.isDesktop)
        assertNull(phone.fingerprint)
        val legacy = Discovery.txt(identity, fingerprint) - SyncProtocol.TXT_ROLE
        assertEquals(true, Discovery.parse(legacy, "192.168.1.20", 53340, "x")!!.isDesktop)
    }

    @Test fun otherServicesAndOtherDataFormatsAreFlagged() {
        assertNull(Discovery.parse(mapOf("name" to "Printer"), "192.168.1.30", 631, "Printer"))
        val older = Discovery.txt(identity, fingerprint) + (SyncProtocol.TXT_DATA_FORMAT to "1")
        assertFalse(Discovery.parse(older, "192.168.1.20", 53340, "x")!!.compatible)
        val badFp = Discovery.txt(identity, "short")
        assertNull(Discovery.parse(badFp, "192.168.1.20", 53340, "x")!!.fingerprint)
    }
}
