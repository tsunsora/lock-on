package app.lockon.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.awt.ComposeWindow
import app.lockon.AppContainer
import app.lockon.ui.AppState
import app.lockon.ui.Screen
import app.lockon.ui.Tab
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * Times opening decks from the library in the real window, where the first few opens are slower
 * than later ones. Run with `-Plockon.deckOpenProbe=<report file>` (best with `-Plockon.dataDir`
 * pointing at a copy of real data): it opens decks one after another, going back between them,
 * writes each open's frame times and slow-frame profiles (the window's thread for all opens and for
 * the first, and every thread for the first), and closes the app. `-Plockon.deckOpenProbe.wait=<ms>`
 * sets how long the library shows first.
 */
@Composable
fun DeckOpenProbe(window: ComposeWindow, container: AppContainer, state: AppState, report: File, exit: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3_000)
        state.select(Tab.DECKS)
        val decks = container.decks.liveDecks.first { it.isNotEmpty() }.filterNot { it.snapshot.deleted }
        // Past the editor's warmup (EditorWarmup.kt), as when someone looks over the library first.
        delay(System.getProperty("lockon.deckOpenProbe.wait")?.toLong() ?: 10_000)
        val ids = decks.map { it.deckId }
        // The same deck a few times, then others, so first-open costs and per-deck costs both show.
        val order = listOf(0, 0, 0, 1, 2, 1, 0, 3).map { ids[it % ids.size] }
        val sampler = StackSampler(Thread.currentThread()).apply { start() }
        val slow = mutableListOf<LongRange>()
        var firstSlow = emptyList<LongRange>()
        val lines = mutableListOf<String>()
        // Every thread during the first open, since the window also draws on a render thread.
        val everything = java.util.concurrent.ConcurrentLinkedQueue<Pair<String, Array<StackTraceElement>>>()
        for ((n, id) in order.withIndex()) {
            val frames = mutableListOf<Long>()
            if (n == 0) kotlin.concurrent.thread(isDaemon = true) {
                val until = System.nanoTime() + 600_000_000L
                while (System.nanoTime() < until) {
                    Thread.getAllStackTraces().forEach { (t, st) -> if (t.state == Thread.State.RUNNABLE && st.isNotEmpty()) everything += t.name to st }
                    Thread.sleep(2)
                }
            }
            coroutineScope {
                val recording = launch { while (true) withFrameNanos { frames += System.nanoTime() } }
                val start = System.nanoTime()
                state.open(Screen.DeckEditor(id))
                delay(1_500)
                recording.cancel()
                val gaps = (listOf(start) + frames).zipWithNext { a, b -> (b - a) / 1_000_000.0 }
                slow += (listOf(start) + frames).zipWithNext { a, b -> a..b }.filter { it.last - it.first > 16_700_000L }
                if (n == 0) firstSlow = slow.toList()
                lines += "open %d (%s): %d frames, worst %.1f ms, %d over 16.7 ms, %.0f ms in those; slow frames (#: ms) %s".format(
                    n + 1, id.take(8), frames.size, gaps.maxOrNull() ?: 0.0, gaps.count { it > 16.7 }, gaps.filter { it > 16.7 }.sum(),
                    gaps.withIndex().filter { it.value > 16.7 }.joinToString { "%d: %.0f".format(it.index, it.value) },
                )
            }
            state.goBack()
            delay(1_500)
        }
        sampler.finish()
        File(report.path + ".threads.txt").writeText(
            everything.groupingBy { (name, st) -> name.replace(Regex("[0-9]+"), "#") + " :: " + st.take(4).joinToString(" < ") { "${it.className.substringAfterLast('.')}.${it.methodName}" } }
                .eachCount().entries.sortedByDescending { it.value }.take(60).joinToString("\n") { "%5d  %s".format(it.value, it.key) },
        )
        File(report.path + ".samples.txt").writeText(sampler.report(slow))
        // The first open on its own, since it is the one that costs the most.
        File(report.path + ".first.samples.txt").writeText(sampler.report(firstSlow))
        report.appendText("${window.renderApi}:\n" + lines.joinToString("\n") + "\n\n")
        exit()
    }
}
