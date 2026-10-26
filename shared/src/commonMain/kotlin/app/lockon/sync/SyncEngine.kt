package app.lockon.sync

import app.lockon.data.DeckRepository
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
}

class SyncException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class SyncResult(val sent: Int, val received: Int)

/**
 * Client side of a sync: push local revisions the host has not seen, then pull the host's. Each
 * batch commits with its cursor, so an interrupted sync resumes where it stopped and a repeated
 * batch is ignored by revision ID.
 */
class SyncClient(private val decks: DeckRepository, private val devices: DeviceRepository) {
    /**
     * @param recordErrors false for background auto-sync, whose failures (desktop closed, other
     *   network) are expected and shown as "waiting" rather than as an error (UX rule C1).
     */
    suspend fun sync(hostId: String, transport: SyncTransport, batchSize: Int = 200, recordErrors: Boolean = true): SyncResult {
        try {
            val hello = transport.hello()
            SyncProtocol.incompatibility(hello)?.let { throw SyncException(it) }
            if (hello.deviceId != hostId) {
                throw SyncException("A different desktop answered at this address. Pair again to sync with it.")
            }

            var sent = 0
            while (true) {
                val device = devices.device(hostId) ?: throw SyncException("This desktop is no longer paired.")
                val out = decks.outgoing(device.pushCursor, batchSize, peer = hostId)
                if (out.revisions.isNotEmpty()) {
                    transport.push(PushRequest(batchId = Uuid.random().toString(), revisions = out.revisions))
                    sent += out.revisions.size
                }
                devices.advancePush(hostId, out.cursor)
                if (!out.hasMore) break
            }

            var received = 0
            while (true) {
                val device = devices.device(hostId) ?: throw SyncException("This desktop is no longer paired.")
                val page = transport.pull(device.pullCursor, batchSize)
                val applied = decks.applyIncoming(page.revisions, peer = hostId) {
                    devices.advancePull(hostId, page.nextCursor)
                }
                received += applied.received
                if (!page.hasMore) break
            }
            devices.markSynced(hostId)
            return SyncResult(sent, received)
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
class SyncHost(private val decks: DeckRepository, private val devices: DeviceRepository) {
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
}
