package app.lockon.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select

enum class AutoSyncState(val label: String) {
    SYNCING("Syncing…"),
    LIVE("Live: changes sync automatically"),
    WAITING("Waiting for the desktop"),
}

/**
 * Keeps paired desktops in sync while the app is open. For each host it syncs once, then waits for
 * whichever comes first: a local edit (after a short quiet period) or the desktop reporting new
 * revisions through a held-open wait request. Failures back off quietly and are reported as
 * [AutoSyncState.WAITING], never as errors (UX rule C1). Cancel the [run] coroutine to stop.
 */
@OptIn(FlowPreview::class)
class AutoSyncLoop(
    private val hostIds: Flow<List<String>>,
    private val localChanges: Flow<Long>,
    private val syncOnce: suspend (hostId: String) -> Unit,
    private val waitForRemote: suspend (hostId: String) -> Boolean,
    private val onState: (hostId: String, state: AutoSyncState?) -> Unit,
    private val debounceMs: Long = 2_000,
    private val minBackoffMs: Long = 5_000,
    private val maxBackoffMs: Long = 60_000,
) {
    private val nudges = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** Retry a host now, for example when discovery sees it come back. */
    fun nudge(hostId: String) { nudges.tryEmit(hostId) }

    suspend fun run() {
        hostIds.distinctUntilChanged().collectLatest { ids ->
            try {
                coroutineScope { ids.forEach { id -> launch { hostLoop(id) } } }
            } finally {
                ids.forEach { onState(it, null) }
            }
        }
    }

    private suspend fun hostLoop(id: String) = coroutineScope {
        val trigger = Channel<Unit>(Channel.CONFLATED)
        // Edits arrive as a burst of autosaves; one sync after they settle is enough.
        launch { localChanges.drop(1).debounce(debounceMs).collect { trigger.trySend(Unit) } }
        launch { nudges.filter { it == id }.collect { trigger.trySend(Unit) } }

        var backoff = minBackoffMs
        var needSync = true
        while (true) {
            try {
                if (needSync) {
                    onState(id, AutoSyncState.SYNCING)
                    syncOnce(id)
                }
                onState(id, AutoSyncState.LIVE)
                backoff = minBackoffMs
                // Failures are carried as a Result so a dropped wait doesn't cancel this host's loop.
                val remote = async { runCatching { waitForRemote(id) } }
                needSync = select {
                    trigger.onReceive { true }
                    remote.onAwait { it.getOrThrow() }
                }
                remote.cancel()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onState(id, AutoSyncState.WAITING)
                // A local edit or a nudge cuts the wait short; otherwise retry after the backoff.
                select {
                    trigger.onReceive { }
                    onTimeout(backoff) { }
                }
                backoff = (backoff * 2).coerceAtMost(maxBackoffMs)
                needSync = true
            }
        }
    }
}

/** Builds and reads the TXT record a desktop advertises on the LAN. */
object Discovery {
    fun txt(identity: LocalIdentity, fingerprint: String): Map<String, String> = mapOf(
        SyncProtocol.TXT_ID to identity.deviceId,
        SyncProtocol.TXT_NAME to identity.deviceName.take(60),
        SyncProtocol.TXT_FINGERPRINT to fingerprint,
        SyncProtocol.TXT_PROTOCOL to SyncProtocol.PROTOCOL_VERSION.toString(),
        SyncProtocol.TXT_DATA_FORMAT to SyncProtocol.DATA_FORMAT_VERSION.toString(),
    )

    fun parse(txt: Map<String, String>, host: String, port: Int, serviceName: String): NearbyDesktop? {
        val id = txt[SyncProtocol.TXT_ID]?.takeIf { it.isNotBlank() } ?: return null
        val protocol = txt[SyncProtocol.TXT_PROTOCOL]?.toIntOrNull() ?: return null
        val format = txt[SyncProtocol.TXT_DATA_FORMAT]?.toIntOrNull() ?: return null
        return NearbyDesktop(
            deviceId = id,
            name = txt[SyncProtocol.TXT_NAME]?.takeIf { it.isNotBlank() } ?: serviceName,
            host = host,
            port = port,
            fingerprint = txt[SyncProtocol.TXT_FINGERPRINT]?.takeIf { it.length == 64 },
            compatible = protocol >= SyncProtocol.MIN_PROTOCOL_VERSION && format == SyncProtocol.DATA_FORMAT_VERSION,
        )
    }
}
