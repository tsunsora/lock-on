package app.lockon.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.awt.ComposeWindow
import app.lockon.AppContainer
import app.lockon.ui.AppState
import app.lockon.ui.Screen
import app.lockon.ui.Tab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.event.MouseWheelEvent
import java.io.File
import javax.swing.SwingUtilities

/**
 * Times scrolling in the real window, where the GPU draws and the window's thread is free between
 * frames while it does, which the off-screen benchmark can't show. Run with
 * `-Plockon.scrollProbe=<report file>` (best with `-Plockon.dataDir` pointing at a copy of real
 * data): it opens Cards, scrolls the grid with wheel events sent straight to the window, so the real
 * mouse never moves, writes the report and closes the app.
 */
@Composable
fun ScrollProbe(window: ComposeWindow, container: AppContainer, state: AppState, report: File, exit: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3_000)
        // `-Plockon.scrollProbe.in=deck` scrolls the card list of the fullest deck's editor instead of Cards.
        val deck = if (System.getProperty("lockon.scrollProbe.in") != "deck") null
            else container.decks.liveDecks.first().filterNot { it.snapshot.deleted }.maxByOrNull { it.snapshot.entries.sumOf { e -> e.quantity } }
        if (deck == null) state.select(Tab.CARDS) else { state.select(Tab.DECKS); state.open(Screen.DeckEditor(deck.deckId)) }
        // Let the first screen of artwork arrive and the tab's animations finish.
        delay(4_000)
        val target = SwingUtilities.getDeepestComponentAt(window.contentPane, window.contentPane.width / 3, window.contentPane.height * 2 / 3)
            ?: window.contentPane
        val x = target.width / 2
        val y = target.height / 2
        val frames = mutableListOf<Long>()
        val back = mutableListOf<Long>()
        val gc = gcTotals()
        val pauses = GcPauses()
        val drawn = DrawnFrames(window)
        // The window draws on this thread, so its stack during slow frames says what they spend time on.
        val sampler = StackSampler(Thread.currentThread()).apply { start() }
        val render = renderThread()?.let { StackSampler(it).apply { start() } }
        // What the window's thread does between frames too, and when not timing, who posts tasks to its
        // queue: taking each poster's stack slows the run.
        val queue = WindowQueue(stacks = !PROBE_QUIET).start()
        // Wheel notches go through the event queue from their own thread, the way real wheel input arrives.
        suspend fun scroll(into: MutableList<Long>, times: Int, notches: Int) = coroutineScope {
            val recording = launch { while (true) withFrameNanos { into += System.nanoTime() } }
            withContext(Dispatchers.IO) {
                val queue = Toolkit.getDefaultToolkit().systemEventQueue
                repeat(times) {
                    queue.postEvent(MouseWheelEvent(target, MouseWheelEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0,
                        x, y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, notches))
                    Thread.sleep(33)
                }
            }
            recording.cancel()
        }
        // Two wheel notches a refresh at 60 Hz for ten seconds, a long steady scroll through the grid,
        // then half as long back up, past cards that were on screen a moment ago.
        scroll(frames, 300, 2)
        delay(500)
        scroll(back, 150, -2)
        // The first scroll after launch runs code for the first time, which loads and compiles as it goes,
        // so its first moments are slow for their own reasons and are reported and profiled apart.
        val settled = frames.first() + 1_500_000_000L
        val (start, steady) = frames.partition { it < settled }
        fun slow(times: List<Long>) = times.zipWithNext { a, b -> a..b }.filter { it.last - it.first > LATE_FRAME_NS }
        sampler.finish()
        render?.finish()
        File(report.path + ".samples.txt").writeText(sampler.report(slow(steady) + slow(back)))
        File(report.path + ".start.samples.txt").writeText(sampler.report(slow(start)))
        render?.let {
            File(report.path + ".render.samples.txt").writeText(it.report(slow(steady) + slow(back)))
            File(report.path + ".start.render.samples.txt").writeText(it.report(slow(start)))
        }
        if (!PROBE_QUIET) File(report.path + ".posted.txt").writeText(queue.report(frames.size + back.size))
        File(report.path + ".longest.txt").writeText(
            "The first 1.5 s:\n" + queue.longest(start.zipWithNext { a, b -> a..b }, drawn) +
                "\n\nAfter that:\n" + queue.longest(steady.zipWithNext { a, b -> a..b }, drawn) + "\n")
        // Every frame's time, to line up with other records of the same run, such as a collector's log.
        // Those give times since the JVM started, so the first line says what that was on this clock.
        val jvmStart = System.nanoTime() - java.lang.management.ManagementFactory.getRuntimeMXBean().uptime * 1_000_000L
        File(report.path + ".frames.txt").writeText("$jvmStart\n" + (frames + back).joinToString("\n"))
        report.appendText(
            "${window.renderApi}, ${window.refreshHz()} Hz, ${window.width}×${window.height}: ${frameSummary(frames)}; collections: ${gcSince(gc)}; ${memorySummary()}\n" +
                "  first 1.5 s: ${frameSummary(start)}; ${drawn.workSummary(start.first(), start.last())}\n" +
                "  after that: ${frameSummary(steady)}; ${drawn.workSummary(steady.first(), steady.last())}\n" +
                "  back up: ${frameSummary(back)}; ${drawn.workSummary(back.first(), back.last())}\n" +
                "  collector, after the first 1.5 s and back up: ${pauses.summary(steady, back)}\n",
        )
        exit()
    }
}

/** A plain sampling profiler: it sees native calls too, which Flight Recorder leaves out. */
/** The thread Skia draws and presents the window on; slow frames with an idle UI thread are waiting on it. */
internal fun renderThread(): Thread? = Thread.getAllStackTraces().keys.firstOrNull { it.name.startsWith("skiko-dispatcher-to-block-on") }

/** Over this a frame is late at 180 Hz (1.5 refreshes); probes profile frames slower than this. */
internal const val LATE_FRAME_NS = 8_300_000L

internal val PROBE_QUIET = System.getProperty("lockon.probeQuiet") == "true"

internal class StackSampler(private val target: Thread) : Thread("stack-sampler") {
    private val samples = java.util.concurrent.ConcurrentLinkedQueue<Pair<Long, Array<StackTraceElement>>>()
    @Volatile private var running = true

    init { isDaemon = true }

    override fun run() {
        // `-Plockon.probeQuiet=true` times without sampling, since sampling stops threads and slows the frames it watches.
        if (PROBE_QUIET) return
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
        val leaf = kept.groupingBy { stack -> stack.firstOrNull { it.className.startsWith("app.lockon") && !it.className.startsWith("app.lockon.desktop.DrawnFrames") }?.let { "${name(it)}:${it.lineNumber}" } ?: "(outside the app)" }.eachCount()
        // Every app function on the stack, so a frame spent in Compose's own code still says whose screen it was building.
        val screens = kept.flatMap { stack -> stack.filter { it.className.startsWith("app.lockon.ui") }.map { "${it.className.substringAfterLast('.')}.${it.methodName.substringBefore('$').substringBefore('-')}" }.distinct() }
            .groupingBy { it }.eachCount()
        fun table(counts: Map<String, Int>, n: Int) = counts.entries.sortedByDescending { it.value }.take(n)
            .joinToString("\n") { "%5.1f%%  %s".format(100.0 * it.value / kept.size.coerceAtLeast(1), it.key) }
        return "${slow.size} slow frames, ${kept.size} samples\n\nInclusive:\n${table(inclusive, 160)}\n\nInnermost app frame:\n${table(leaf, 40)}\n\nApp functions on the stack:\n${table(screens, 50)}\n\nStarting coroutines and posting events:\n${table(callers, 30)}\n\nTop of stack:\n${table(top, 60)}\n"
    }
}
