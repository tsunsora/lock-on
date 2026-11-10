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
 * end and the worst, how many missed one refresh at 180 Hz (5.6 ms), a refresh and a half (8.3 ms)
 * and a 60 Hz frame (16.7 ms), and the time lost in gaps over 5.6 ms beyond that budget.
 */
internal fun frameSummary(times: List<Long>): String {
    val gaps = times.zipWithNext { a, b -> (b - a) / 1_000_000.0 }.sorted()
    if (gaps.isEmpty()) return "no frames"
    fun at(share: Double) = gaps[((gaps.size - 1) * share).toInt()]
    return "%d frames in %.1f s, mean %.2f, p50 %.1f, p95 %.1f, p99 %.1f, max %.1f ms, %d over 5.6, %d over 8.3, %d over 16.7, %.0f ms late".format(
        times.size, (times.last() - times.first()) / 1e9, gaps.average(), at(0.5), at(0.95), at(0.99), gaps.last(),
        gaps.count { it > 5.6 }, gaps.count { it > 8.3 }, gaps.count { it > 16.7 }, gaps.filter { it > 5.6 }.sumOf { it - 5.6 },
    )
}

/**
 * Counts the frames the window really draws, without asking for any: waiting on the frame clock, the
 * way the other probes time frames, makes the window draw every refresh, which hides whether it would
 * have drawn on its own.
 */
internal class DrawnFrames(window: ComposeWindow) {
    @Volatile var count = 0L
        private set

    init {
        val layer = components(window).filterIsInstance<SkiaLayer>().first()
        val drawing = layer.renderDelegate
        layer.renderDelegate = object : SkikoRenderDelegate {
            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                count++
                drawing?.onRender(canvas, width, height, nanoTime)
            }
        }
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
