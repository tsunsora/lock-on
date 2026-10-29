package app.lockon.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
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
 * Times what someone does first once the app is up: switching tabs and opening decks, in the real
 * window. Run with `-Plockon.tabProbe=<report file>` (best with `-Plockon.dataDir` pointing at a
 * copy of real data). It waits for the boot screen to go, runs the steps, appends each one's frame
 * times and a slow-frame profile to the report, and closes the app.
 */
@Composable
fun TabSwitchProbe(window: ComposeWindow, container: AppContainer, state: AppState, report: File, exit: () -> Unit) {
    LaunchedEffect(Unit) {
        snapshotFlow { state.booted }.first { it }
        System.getProperty("lockon.tabProbe.settle")?.toLong()?.let { delay(it) }
        delay(700)
        val deck = container.decks.liveDecks.first().filterNot { it.snapshot.deleted }.maxByOrNull { it.snapshot.entries.size }
        val steps = buildList<Pair<String, () -> Unit>> {
            add("cards 1" to { state.select(Tab.CARDS) })
            add("decks 1" to { state.select(Tab.DECKS) })
            add("cards 2" to { state.select(Tab.CARDS) })
            add("decks 2" to { state.select(Tab.DECKS) })
            if (deck != null) {
                add("open 1" to { state.open(Screen.DeckEditor(deck.deckId)) })
                add("back 1" to { state.goBack() })
                add("open 2" to { state.open(Screen.DeckEditor(deck.deckId)) })
                add("back 2" to { state.goBack() })
            }
        }
        val sampler = StackSampler(Thread.currentThread()).apply { start() }
        val slow = mutableListOf<LongRange>()
        val slowByStep = mutableMapOf<String, List<LongRange>>()
        val lines = mutableListOf<String>()
        // Every thread during the first switch, since the window also draws on a render thread.
        val everything = java.util.concurrent.ConcurrentLinkedQueue<Pair<String, Array<StackTraceElement>>>()
        for ((name, act) in steps) {
            val frames = mutableListOf<Long>()
            if (name == "cards 1") kotlin.concurrent.thread(isDaemon = true) {
                val until = System.nanoTime() + 700_000_000L
                while (System.nanoTime() < until) {
                    Thread.getAllStackTraces().forEach { (t, st) -> if (t.state == Thread.State.RUNNABLE && st.isNotEmpty()) everything += t.name to st }
                    Thread.sleep(2)
                }
            }
            coroutineScope {
                val recording = launch { while (true) withFrameNanos { frames += System.nanoTime() } }
                val start = System.nanoTime()
                act()
                delay(1_200)
                recording.cancel()
                val times = listOf(start) + frames
                val gaps = times.zipWithNext { a, b -> (b - a) / 1_000_000.0 }
                val stepSlow = times.zipWithNext { a, b -> a..b }.filter { it.last - it.first > 16_700_000L }
                slow += stepSlow
                slowByStep[name] = stepSlow
                lines += "%-8s %3d frames, worst %5.1f ms, %2d over 16.7 ms, %4.0f ms in those".format(
                    name, frames.size, gaps.maxOrNull() ?: 0.0, gaps.count { it > 16.7 }, gaps.filter { it > 16.7 }.sum(),
                )
            }
            delay(600)
        }
        sampler.finish()
        File(report.path + ".samples.txt").writeText(sampler.report(slow))
        File(report.path + ".cards1.threads.txt").writeText(
            everything.groupingBy { (thread, st) -> thread.replace(Regex("[0-9]+"), "#") + " :: " + st.take(6).joinToString(" < ") { "${it.className.substringAfterLast('.')}.${it.methodName}" } }
                .eachCount().entries.sortedByDescending { it.value }.take(50).joinToString("\n") { "%5d  %s".format(it.value, it.key) },
        )
        File(report.path + ".cards1.samples.txt").writeText(sampler.report(slowByStep["cards 1"].orEmpty()))
        report.appendText("${System.getProperty("lockon.tabProbe.label") ?: "run"} (${window.renderApi}):\n" + lines.joinToString("\n") + "\n\n")
        exit()
    }
}
