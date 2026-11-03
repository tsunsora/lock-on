package app.lockon.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.awt.ComposeWindow
import app.lockon.ui.AppState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * Times the boot screen's frames in the real window, from its first frame until it has faded away.
 * Run with `-Plockon.bootProbe=<report file>` (best with `-Plockon.dataDir` pointing at a copy of real
 * data): it writes each frame's gap, a summary and `.samples.txt` for the slow frames, then closes the app.
 * With a boot cover up, the cover's own frames are what you see, so they are timed too.
 */
@Composable
fun BootProbe(window: ComposeWindow, state: AppState, cover: DesktopBootCover?, report: File, exit: () -> Unit) {
    LaunchedEffect(Unit) {
        val frames = mutableListOf<Long>()
        var bootedAt = 0L
        val sampler = StackSampler(Thread.currentThread()).apply { start() }
        // Pictures of the screen at a few moments, taken off the app's thread so they show what you'd see.
        val shots = System.getProperty("lockon.bootProbe.shots") == "true"
        val began = System.nanoTime()
        fun shoot(name: String) {
            val bounds = runCatching { window.contentPane.let { java.awt.Rectangle(it.locationOnScreen, it.size) } }.getOrNull() ?: return
            javax.imageio.ImageIO.write(java.awt.Robot().createScreenCapture(bounds), "png", File("${report.path}.$name.png"))
        }
        // A window opened from a terminal often opens behind others; the pictures need it in front.
        if (shots) window.isAlwaysOnTop = true
        if (shots) kotlin.concurrent.thread(isDaemon = true) {
            for (at in listOf(250L, 500L, 750L, 1_000L, 1_500L)) {
                Thread.sleep((at - (System.nanoTime() - began) / 1_000_000).coerceAtLeast(0))
                shoot("at-$at")
            }
        }
        // `-Plockon.bootProbe.resize=<width>x<height>` (or `max`) resizes the window 900 ms in, while the
        // boot screen plays, with pictures just after and once the cover has caught up.
        System.getProperty("lockon.bootProbe.resize")?.let { size ->
            kotlin.concurrent.thread(isDaemon = true) {
                Thread.sleep((900 - (System.nanoTime() - began) / 1_000_000).coerceAtLeast(0))
                java.awt.EventQueue.invokeLater {
                    if (size == "max") window.extendedState = java.awt.Frame.MAXIMIZED_BOTH
                    else size.split("x").map { it.trim().toInt() }.let { (w, h) -> window.setSize(w, h) }
                }
                val resizedAt = System.nanoTime()
                for (after in listOf(40L, 150L, 250L, 350L, 450L, 600L, 900L)) {
                    Thread.sleep((after - (System.nanoTime() - resizedAt) / 1_000_000).coerceAtLeast(0))
                    shoot("resized-$after")
                }
            }
        }
        coroutineScope {
            val recording = launch { while (true) withFrameNanos { frames += System.nanoTime() } }
            snapshotFlow { state.booted }.first { it }
            bootedAt = System.nanoTime()
            if (shots) kotlin.concurrent.thread(isDaemon = true) {
                for (after in listOf(0L, 150L, 300L, 700L)) {
                    Thread.sleep((after - (System.nanoTime() - bootedAt) / 1_000_000).coerceAtLeast(0))
                    shoot("booted-$after")
                }
            }
            // The boot screen's fade out, and a little after.
            delay(900)
            recording.cancel()
        }
        val slow = frames.zipWithNext { a, b -> a..b }.filter { it.last - it.first > 16_700_000L }
        File(report.path + ".samples.txt").writeText(sampler.finish().report(slow))
        val hz = window.graphicsConfiguration.device.displayMode.refreshRate.takeIf { it > 0 } ?: 60
        fun summary(label: String, times: List<Long>): String {
            val gaps = times.zipWithNext { a, b -> (b - a) / 1_000_000.0 }.sorted()
            if (gaps.isEmpty()) return "$label: no frames\n"
            return "$label: %d frames in %.2f s, median %.1f ms, p90 %.1f ms, worst %.1f ms, %d over 16.7 ms, %d over 50 ms, %.0f ms in frames over 16.7 ms\n".format(
                gaps.size, (times.last() - times.first()) / 1e9, gaps[gaps.size / 2], gaps[gaps.size * 9 / 10], gaps.last(),
                gaps.count { it > 16.7 }, gaps.count { it > 50 }, gaps.filter { it > 16.7 }.sum(),
            )
        }
        val start = frames.first()
        val covered = cover?.frames?.toList().orEmpty()
        // Motion against time: how far the animation moved each cover frame for each ms that passed. An
        // even 1.0 is smooth; well above it is a burst, well below it a stall.
        val moved = cover?.playedAt?.toList().orEmpty()
        val pace = covered.indices.drop(1).map { i -> (moved[i] - moved[i - 1]) / ((covered[i] - covered[i - 1]) / 1e6f) }
        val work = cover?.work?.toList().orEmpty()
        fun median(i: Int) = work.map { it[i] }.sorted().let { if (it.isEmpty()) 0f else it[it.size / 2] }
        val workLine = if (work.isEmpty()) "" else "  cover work per frame, median ms: Skia %.1f, Java2D %.1f, presenting %.1f\n".format(median(0), median(1), median(2))
        val paceLine = workLine + if (pace.isEmpty()) "" else "  cover pace (animation ms per real ms): min %.2f, max %.2f, %d frames outside 0.8-1.25\n".format(
            pace.min(), pace.max(), pace.count { it < 0.8f || it > 1.25f })
        report.appendText(
            "${window.renderApi}, $hz Hz, ${window.width}×${window.height}\n" +
                summary("  boot", frames.filter { it <= bootedAt }) +
                summary("  fade", frames.filter { it >= bootedAt }) +
                (if (covered.isEmpty()) "  cover: never showed\n" else
                    "  cover up %.0f ms after the first frame\n".format((covered.first() - start) / 1e6) +
                        summary("  cover", covered) + paceLine +
                        "  cover gaps over 12 ms: " + covered.zipWithNext { a, b -> "%.0f:%.0f".format((a - start) / 1e6, (b - a) / 1e6) }
                            .filterIndexed { i, _ -> (covered[i + 1] - covered[i]) > 12_000_000L }.joinToString(", ") + "\n") +
                "  gaps (ms since first frame: gap): " + frames.zipWithNext { a, b ->
                    "%.0f:%.0f".format((a - start) / 1e6, (b - a) / 1e6) + if (a < bootedAt && b >= bootedAt) " [booted]" else ""
                }.joinToString(", ") + "\n",
        )
        exit()
    }
}
