package app.lockon.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.awt.ComposeWindow
import app.lockon.ui.AppState
import app.lockon.ui.Tab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.event.MouseWheelEvent
import java.io.File
import javax.swing.SwingUtilities

/**
 * Times scrolling in the real window, where the GPU draws and the grid can prefetch between
 * frames, which the off-screen benchmark can't show. Run with `-Plockon.scrollProbe=<report file>`
 * (best with `-Plockon.dataDir` pointing at a copy of real data): it opens Cards, scrolls the grid
 * with wheel events sent straight to the window, so the real mouse never moves, writes the report
 * and closes the app.
 */
@Composable
fun ScrollProbe(window: ComposeWindow, state: AppState, report: File, exit: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3_000)
        state.select(Tab.CARDS)
        // Let the first screen of artwork arrive and the tab's animations finish.
        delay(4_000)
        val target = SwingUtilities.getDeepestComponentAt(window.contentPane, window.contentPane.width / 3, window.contentPane.height * 2 / 3)
            ?: window.contentPane
        val x = target.width / 2
        val y = target.height / 2
        val hz = window.graphicsConfiguration.device.displayMode.refreshRate.takeIf { it > 0 } ?: 60
        val frames = mutableListOf<Long>()
        // The window draws on this thread, so its stack during slow frames says what they spend time on.
        val sampler = StackSampler(Thread.currentThread()).apply { start() }
        coroutineScope {
            val recording = launch { while (true) withFrameNanos { frames += System.nanoTime() } }
            // Two wheel notches a refresh at 60 Hz for ten seconds, a long steady scroll through the grid.
            // They go through the event queue from their own thread, the way real wheel input arrives.
            withContext(Dispatchers.IO) {
                val queue = Toolkit.getDefaultToolkit().systemEventQueue
                repeat(300) {
                    queue.postEvent(MouseWheelEvent(target, MouseWheelEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0,
                        x, y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, 2))
                    Thread.sleep(33)
                }
            }
            recording.cancel()
        }
        val slow = frames.zipWithNext { a, b -> a..b }.filter { it.last - it.first > 16_700_000L }
        File(report.path + ".samples.txt").writeText(sampler.finish().report(slow))
        val gaps = frames.zipWithNext { a, b -> (b - a) / 1_000_000.0 }.sorted()
        val budget = 1_000.0 / hz
        report.appendText(
            "${window.renderApi}, ${hz} Hz, ${window.width}×${window.height}: %d frames in %.1f s, mean %.1f ms, median %.1f ms, p90 %.1f ms, p99 %.1f ms, worst %.1f ms, %d over 1.5× budget (%.1f ms), %d over 16.7 ms\n".format(
                frames.size, (frames.last() - frames.first()) / 1e9, gaps.average(), gaps[gaps.size / 2], gaps[gaps.size * 9 / 10],
                gaps[gaps.size * 99 / 100], gaps.last(), gaps.count { it > budget * 1.5 }, budget, gaps.count { it > 1_000.0 / 60 },
            ),
        )
        exit()
    }
}

/** A plain sampling profiler: it sees native calls too, which Flight Recorder leaves out. */
internal class StackSampler(private val target: Thread) : Thread("stack-sampler") {
    private val samples = java.util.concurrent.ConcurrentLinkedQueue<Pair<Long, Array<StackTraceElement>>>()
    @Volatile private var running = true

    init { isDaemon = true }

    override fun run() {
        while (running) {
            samples += System.nanoTime() to target.stackTrace
            sleep(1)
        }
    }

    fun finish() = apply { running = false; join() }

    /** Methods by the share of slow-frame samples they appear in, and the innermost app frames. */
    fun report(slow: List<LongRange>): String {
        val kept = samples.filter { (at, _) -> slow.any { at in it } }.map { it.second }
        fun name(e: StackTraceElement) = "${e.className.substringAfterLast('.')}.${e.methodName}"
        val inclusive = kept.flatMap { stack -> stack.map(::name).distinct() }.groupingBy { it }.eachCount()
        val top = kept.groupingBy { stack -> stack.take(3).joinToString(" < ", transform = ::name) }.eachCount()
        // Who starts coroutines and posts window events, the costs that scale with tiles on screen.
        val callers = kept.mapNotNull { stack ->
            val at = stack.indexOfFirst { name(it) == "BuildersKt.launch" || name(it) == "EventQueue.postEvent" || name(it) == "SwingDispatcher.dispatch" }
            if (at < 0) null else stack.drop(at).filter { it.className.startsWith("app.lockon") || it.className.startsWith("androidx") || it.className.startsWith("coil") }
                .take(6).joinToString(" < ", transform = ::name)
        }.groupingBy { it }.eachCount()
        val leaf = kept.groupingBy { stack -> stack.firstOrNull { it.className.startsWith("app.lockon") }?.let { "${name(it)}:${it.lineNumber}" } ?: "(outside the app)" }.eachCount()
        fun table(counts: Map<String, Int>, n: Int) = counts.entries.sortedByDescending { it.value }.take(n)
            .joinToString("\n") { "%5.1f%%  %s".format(100.0 * it.value / kept.size.coerceAtLeast(1), it.key) }
        return "${slow.size} slow frames, ${kept.size} samples\n\nInclusive:\n${table(inclusive, 160)}\n\nInnermost app frame:\n${table(leaf, 40)}\n\nStarting coroutines and posting events:\n${table(callers, 30)}\n\nTop of stack:\n${table(top, 60)}\n"
    }
}
