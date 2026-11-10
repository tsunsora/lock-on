package app.lockon.desktop

import androidx.compose.ui.awt.ComposeWindow
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import java.awt.Component
import java.awt.Container
import java.lang.management.ManagementFactory

/** The display's refresh rate, or 60 when the system doesn't say. */
internal fun ComposeWindow.refreshHz(): Int = graphicsConfiguration.device.displayMode.refreshRate.takeIf { it > 0 } ?: 60

/**
 * The gaps between frame times as one line every probe reports the same way: the middle, the slow
 * end and the worst, how many were over one refresh at 180 Hz (5.6 ms, which on-time frames straddle,
 * so about half of them count), clearly over it (7 ms), over a refresh and a half (8.3 ms) and over a
 * 60 Hz frame (16.7 ms), and the time lost in gaps over 5.6 ms beyond that budget.
 */
internal fun frameSummary(times: List<Long>): String = gapSummary(times.zipWithNext { a, b -> a..b })

/** [frameSummary] for gaps picked out of a run, such as those that don't overlap the probe's own work. */
internal fun gapSummary(between: List<LongRange>): String {
    val gaps = between.map { (it.last - it.first) / 1_000_000.0 }.sorted()
    if (gaps.isEmpty()) return "no frames"
    fun at(share: Double) = gaps[((gaps.size - 1) * share).toInt()]
    return "%d frames in %.1f s, mean %.2f, p50 %.1f, p95 %.1f, p99 %.1f, max %.1f ms, %d over 5.6, %d over 7.0, %d over 8.3, %d over 16.7, %.0f ms late".format(
        gaps.size + 1, gaps.sum() / 1e3, gaps.average(), at(0.5), at(0.95), at(0.99), gaps.last(),
        gaps.count { it > 5.6 }, gaps.count { it > 7.0 }, gaps.count { it > 8.3 }, gaps.count { it > 16.7 }, gaps.filter { it > 5.6 }.sumOf { it - 5.6 },
    )
}

/**
 * Watches the frames the window really draws without asking for any: waiting on the frame clock, the
 * way the probes time frames, makes the window draw every refresh, which hides whether it would
 * have drawn on its own. It counts them, and keeps how long each took the window's thread to compose,
 * lay out and record, which is the part of a frame that has to fit in a refresh along with the GPU's.
 */
internal class DrawnFrames(window: ComposeWindow) {
    @Volatile var count = 0L
        private set
    private val work = java.util.concurrent.ConcurrentLinkedQueue<LongArray>()

    init {
        val layer = components(window).filterIsInstance<SkiaLayer>().first()
        val drawing = layer.renderDelegate
        layer.renderDelegate = object : SkikoRenderDelegate {
            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                count++
                val began = System.nanoTime()
                drawing?.onRender(canvas, width, height, nanoTime)
                work += longArrayOf(began, System.nanoTime())
            }
        }
    }

    /** The window thread's work in the frames drawn from [from] to [to] (nanoTime): the middle, the slow end and the worst. */
    fun workSummary(from: Long, to: Long): String {
        val took = work.filter { it[0] in from..to }.map { (it[1] - it[0]) / 1_000_000.0 }.sorted()
        if (took.isEmpty()) return "no frames"
        fun at(share: Double) = took[((took.size - 1) * share).toInt()]
        return "window thread per frame: p50 %.1f, p95 %.1f, p99 %.1f, max %.1f ms".format(at(0.5), at(0.95), at(0.99), took.last())
    }
}

/** Garbage collections so far and the time they took, by collector, to compare before and after a stretch. */
internal fun gcTotals(): Map<String, Pair<Long, Long>> =
    ManagementFactory.getGarbageCollectorMXBeans().associate { it.name to (it.collectionCount to it.collectionTime) }

internal fun gcSince(before: Map<String, Pair<Long, Long>>): String =
    gcTotals().entries.joinToString(", ") { (name, now) ->
        val then = before[name] ?: (0L to 0L)
        "$name ${now.first - then.first} (${now.second - then.second} ms)"
    }

/** [root] and everything inside it, parents before children. */
internal fun components(root: Component): Sequence<Component> = sequence {
    yield(root)
    if (root is Container) root.components.forEach { yieldAll(components(it)) }
}

/**
 * Counts the tasks posted to the window's event queue and who posted them. Every coroutine Compose
 * starts, resumes or cancels on desktop is one, and on Java 21 each costs a walk of the whole call
 * stack (about 13 µs from inside a composition), so a screen that posts hundreds while it builds
 * pays for them in frames. Started with [start], it stays in place for the rest of the run.
 */
internal class PostedTasks : java.awt.EventQueue() {
    private val posters = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger>()
    private val count = java.util.concurrent.atomic.AtomicInteger()
    @Volatile private var counting = false

    fun start() = apply { java.awt.Toolkit.getDefaultToolkit().systemEventQueue.push(this); counting = true }

    override fun postEvent(event: java.awt.AWTEvent) {
        if (counting && event is java.awt.event.InvocationEvent && java.awt.EventQueue.isDispatchThread()) {
            count.incrementAndGet()
            // The nearest callers that say whose work it is, skipping the coroutine machinery between.
            val who = Thread.currentThread().stackTrace.asSequence()
                .map { "${it.className.substringAfterLast('.')}.${it.methodName}" }
                .filterNot { name -> Machinery.any { name.startsWith(it) } }
                .take(4).joinToString(" < ")
            posters.computeIfAbsent(who) { java.util.concurrent.atomic.AtomicInteger() }.incrementAndGet()
        }
        super.postEvent(event)
    }

    fun report(frames: Int): String {
        counting = false
        val total = count.get()
        return "$total tasks posted from the window's thread, %.1f a frame over $frames frames\n".format(total / frames.coerceAtLeast(1).toDouble()) +
            posters.entries.sortedByDescending { it.value.get() }.take(40).joinToString("\n") { "%6d  %s".format(it.value.get(), it.key) } + "\n"
    }

    private companion object {
        val Machinery = listOf(
            "Thread.", "PostedTasks.", "EventQueue.", "SwingUtilities.", "SwingDispatcher.", "Dispatched", "CancellableContinuation", "Cancellable",
            "BuildersKt", "CoroutineStart.", "AbstractCoroutine.", "BaseContinuationImpl.", "FlushCoroutineDispatcher", "JobSupport", "ChildContinuation.",
            "StandaloneCoroutine.", "JobKt", "CoroutineScopeKt.", "ContinuationImpl.", "SafeContinuation.", "Timer.", "AWTEvent.", "InvocationEvent.",
            "Actuals_skikoKt.", "DelayKt.", "EventLoop", "Undispatched", "ScopeCoroutine.", "ContinuationKt.", "IntrinsicsKt",
        )
    }
}
