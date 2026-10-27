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
    fun txt(identity: LocalIdentity, fingerprint: String?, role: String = SyncProtocol.ROLE_DESKTOP): Map<String, String> = buildMap {
        put(SyncProtocol.TXT_ID, identity.deviceId)
        put(SyncProtocol.TXT_NAME, identity.deviceName.take(60))
        fingerprint?.let { put(SyncProtocol.TXT_FINGERPRINT, it) }
        put(SyncProtocol.TXT_PROTOCOL, SyncProtocol.PROTOCOL_VERSION.toString())
        put(SyncProtocol.TXT_DATA_FORMAT, SyncProtocol.DATA_FORMAT_VERSION.toString())
        put(SyncProtocol.TXT_ROLE, role)
    }

    fun parse(txt: Map<String, String>, host: String, port: Int, serviceName: String): NearbyDevice? {
        val id = txt[SyncProtocol.TXT_ID]?.takeIf { it.isNotBlank() } ?: return null
        val protocol = txt[SyncProtocol.TXT_PROTOCOL]?.toIntOrNull() ?: return null
        val format = txt[SyncProtocol.TXT_DATA_FORMAT]?.toIntOrNull() ?: return null
        return NearbyDevice(
            deviceId = id,
            name = txt[SyncProtocol.TXT_NAME]?.takeIf { it.isNotBlank() } ?: serviceName,
            host = host,
            port = port,
            fingerprint = txt[SyncProtocol.TXT_FINGERPRINT]?.takeIf { it.length == 64 },
            compatible = protocol >= SyncProtocol.MIN_PROTOCOL_VERSION && format == SyncProtocol.DATA_FORMAT_VERSION,
            // Announcements from before roles existed came only from desktops.
            role = txt[SyncProtocol.TXT_ROLE]?.takeIf { it == SyncProtocol.ROLE_PHONE } ?: SyncProtocol.ROLE_DESKTOP,
        )
    }
}
