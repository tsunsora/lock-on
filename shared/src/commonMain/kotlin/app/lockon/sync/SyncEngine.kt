package app.lockon.sync

import app.lockon.data.DeckRepository
import app.lockon.model.DeckSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.uuid.Uuid

/** How the client reaches a host. HTTP in the apps; a direct call in tests. */
interface SyncTransport {
    suspend fun hello(): HelloResponse
    suspend fun push(request: PushRequest): PushResponse
    suspend fun pull(after: Long, limit: Int): PullResponse
    /** Waits up to [SyncProtocol.MAX_WAIT_SECONDS] for the host to have revisions after [after]. */
    suspend fun waitForChanges(after: Long): WaitResponse = WaitResponse(changed = false)
    /** The host's [DeviceState], for this device to take on. */
    suspend fun state(): DeviceState = throw SyncException("This desktop can't be matched. Update Lock On on it.")
    suspend fun catalog(): CatalogTransfer = throw SyncException("This desktop can't send its card list. Update Lock On on it.")
    /** Makes the host take on this device's state. */
    suspend fun mirror(push: MirrorPush): MirrorSummary = throw SyncException("This desktop can't be matched. Update Lock On on it.")
}

class SyncException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class SyncResult(val sent: Int, val received: Int)

/**
 * Client side of a sync. Merging pushes local revisions the host has not seen, then pulls the
 * host's. Each batch commits with its cursor, so an interrupted sync resumes where it stopped and a
 * repeated batch is ignored by revision ID. Matching moves revisions one way only, then the device
 * being matched takes on the other's [DeviceState].
 */
class SyncClient(
    private val decks: DeckRepository,
    private val devices: DeviceRepository,
    private val mirror: DeviceMirror? = null,
) {
    /**
     * @param recordErrors false for background auto-sync, whose failures (desktop closed, other
     *   network) are expected and shown as "waiting" rather than as an error (UX rule C1).
     */
    suspend fun sync(hostId: String, transport: SyncTransport, batchSize: Int = 200, recordErrors: Boolean = true): SyncResult =
        reporting(hostId, recordErrors) {
            checkHost(hostId, transport)
            val sent = pushAll(hostId, transport, batchSize)
            val received = pullAll(hostId, transport, batchSize)
            devices.markSynced(hostId)
            SyncResult(sent, received)
        }

    /** Makes this device match the host: its revisions come here, then its decks, folders, settings and card list replace these. */
    suspend fun take(hostId: String, transport: SyncTransport, batchSize: Int = 200): MirrorSummary = reporting(hostId, true) {
        val mirror = mirror ?: throw SyncException("Matching isn't available on this device.")
        val hello = checkHost(hostId, transport)
        if (!hello.mirrors) throw SyncException("Update Lock On on ${hello.deviceName} to make devices match.")
        val before = mirror.shown()
        pullAll(hostId, transport, batchSize)
        val state = transport.state()
        val summary = mirror.apply(state, if (mirror.needsCatalog(state)) transport.catalog() else null, before)
        devices.markSynced(hostId)
        summary
    }

    /** Makes the host match this device: these revisions go there, then it takes on this device's state. */
    suspend fun give(hostId: String, transport: SyncTransport, requestId: String? = null, batchSize: Int = 200): MirrorSummary =
        reporting(hostId, true) {
            val mirror = mirror ?: throw SyncException("Matching isn't available on this device.")
            val hello = checkHost(hostId, transport)
            if (!hello.mirrors) throw SyncException("Update Lock On on ${hello.deviceName} to make devices match.")
            // Reading the host's state first lets it count what the match changes there.
            transport.state()
            pushAll(hostId, transport, batchSize)
            val state = mirror.state()
            val catalog = if (state.catalog != null && state.catalog != hello.catalog) mirror.exportCatalog() else null
            val summary = transport.mirror(MirrorPush(state, catalog, requestId))
            devices.markSynced(hostId)
            summary
        }

    private suspend fun checkHost(hostId: String, transport: SyncTransport): HelloResponse {
        val hello = transport.hello()
        SyncProtocol.incompatibility(hello)?.let { throw SyncException(it) }
        if (hello.deviceId != hostId) {
            throw SyncException("A different desktop answered at this address. Pair again to sync with it.")
        }
        return hello
    }

    private suspend fun pushAll(hostId: String, transport: SyncTransport, batchSize: Int): Int {
        var sent = 0
        while (true) {
            val device = devices.device(hostId) ?: throw SyncException("This desktop is no longer paired.")
            val out = decks.outgoing(device.pushCursor, batchSize, peer = hostId)
            if (out.revisions.isNotEmpty()) {
                transport.push(PushRequest(batchId = Uuid.random().toString(), revisions = out.revisions))
                sent += out.revisions.size
            }
            devices.advancePush(hostId, out.cursor)
            if (!out.hasMore) return sent
        }
    }

    private suspend fun pullAll(hostId: String, transport: SyncTransport, batchSize: Int): Int {
        var received = 0
        while (true) {
            val device = devices.device(hostId) ?: throw SyncException("This desktop is no longer paired.")
            val page = transport.pull(device.pullCursor, batchSize)
            val applied = decks.applyIncoming(page.revisions, peer = hostId) {
                devices.advancePull(hostId, page.nextCursor)
            }
            received += applied.received
            if (!page.hasMore) return received
        }
    }

    private suspend fun <T> reporting(hostId: String, recordErrors: Boolean, block: suspend () -> T): T {
        try {
            return block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = (e as? SyncException)?.message ?: e.message ?: "Sync failed."
            if (recordErrors) devices.markError(hostId, message)
            throw e as? SyncException ?: SyncException(message, e)
        }
    }
}

/** Host side of a sync. The transport layer authenticates the client before calling in. */
class SyncHost(
    private val decks: DeckRepository,
    private val devices: DeviceRepository,
    private val mirror: DeviceMirror? = null,
) {
    /** Whether this host can be matched and match others. */
    val mirrors: Boolean get() = mirror != null

    suspend fun push(clientId: String, request: PushRequest): PushResponse {
        val applied = decks.applyIncoming(request.revisions, peer = clientId)
        devices.markSynced(clientId)
        return PushResponse(request.batchId, applied.received, applied.alreadyKnown)
    }

    /** Holds until a revision past [after] exists or [timeoutMs] passes. */
    suspend fun waitForChanges(after: Long, timeoutMs: Long): WaitResponse {
        val changed = withTimeoutOrNull(timeoutMs) { decks.latestSeq.first { it > after } } != null
        return WaitResponse(changed)
    }

    suspend fun pull(clientId: String, after: Long, limit: Int): PullResponse {
        val out = decks.outgoing(after, limit.coerceIn(1, 500), peer = clientId)
        devices.markSynced(clientId)
        return PullResponse(out.revisions, out.cursor, out.hasMore)
    }

    suspend fun catalogKey(): String? = mirror?.catalogKey()

    /** What each phone last read here, so a match it then asks for counts against it. */
    private val readBy = mutableMapOf<String, Map<String, DeckSnapshot>>()

    suspend fun state(clientId: String): DeviceState {
        val state = requireMirror().state()
        synchronized(readBy) { readBy[clientId] = state.decks.associate { it.deckId to it.snapshot } }
        return state
    }

    suspend fun catalog(): CatalogTransfer = requireMirror().exportCatalog()

    /** Takes on a phone's state; its revisions were pushed first. */
    suspend fun mirror(clientId: String, push: MirrorPush): MirrorSummary {
        val before = synchronized(readBy) { readBy.remove(clientId) }
        val summary = requireMirror().apply(push.state, push.catalog, before)
        devices.markSynced(clientId)
        return summary
    }

    private fun requireMirror() = mirror ?: throw IllegalArgumentException("This desktop can't be matched.")
}
