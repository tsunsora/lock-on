package app.lockon.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import app.lockon.AppContainer
import app.lockon.platform.FileBridge
import app.lockon.ui.AppState
import app.lockon.ui.decks.DeckEditorScreen
import app.lockon.ui.theme.LockOnTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory

/**
 * Draws the deck editor once off-screen, on a background thread, soon after launch. The first time
 * the editor opened, its classes loaded and its code ran unoptimized, holding the window for most
 * of a second; the JVM shares that work across threads, so doing it here makes the first real
 * open as quick as later ones. Card images stay off, so nothing is read or downloaded for it, and
 * nothing is edited, so nothing is saved.
 */
object EditorWarmup {
    private const val WARMUPS = 2

    fun start(container: AppContainer) {
        kotlin.concurrent.thread(name = "editor-warmup", isDaemon = true, priority = Thread.MIN_PRIORITY) {
            val log = LoggerFactory.getLogger("LockOn")
            runCatching {
                Thread.sleep(1_500)
                // The biggest deck, so the deck list draws every kind of row.
                val deck = runBlocking { withTimeoutOrNull(10_000) { container.decks.liveDecks.first { it.isNotEmpty() } } }
                    ?.filterNot { it.snapshot.deleted }?.maxByOrNull { it.snapshot.entries.size } ?: return@thread
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                val started = System.nanoTime()
                // A few times over, since the JIT optimizes code only once it has run often.
                repeat(WARMUPS) {
                    val scene = ImageComposeScene(1600, 1000, Density(1.25f)) {
                        LockOnTheme { DeckEditorScreen(container, AppState(scope), NoFiles, deck.deckId, wide = true) }
                    }
                    // Enough frames for the editor and its card list to fill in.
                    try { repeat(40) { frame -> scene.render(frame * 16_000_000L) } } finally { scene.close() }
                }
                scope.cancel()
                log.info("Warmed up the deck editor in ${(System.nanoTime() - started) / 1_000_000} ms")
            }.onFailure { log.warn("Couldn't warm up the deck editor", it) }
        }
    }

    private object NoFiles : FileBridge {
        override suspend fun saveText(suggestedName: String, mimeType: String, content: String) = false
        override suspend fun saveBytes(suggestedName: String, mimeType: String, content: ByteArray) = false
        override suspend fun openText(mimeTypes: List<String>): String? = null
    }
}
