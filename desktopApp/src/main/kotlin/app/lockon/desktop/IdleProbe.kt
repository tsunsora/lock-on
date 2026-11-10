package app.lockon.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.awt.ComposeWindow
import app.lockon.ui.AppState
import app.lockon.ui.Tab
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Counts what the window does while nobody touches it: an app at rest should draw nothing and collect
 * no garbage. Run with `-Plockon.idleProbe=<report file>` (best with `-Plockon.dataDir` pointing at a
 * copy of real data). It sits on each tab in turn, Cards for `-Plockon.idleProbe.seconds` (40) and the
 * others for a quarter of that, counts the frames drawn and the collections run in that time, and
 * closes the app. Without `-Plockon.probeQuiet=true` it also writes what the window's thread was
 * doing while it sat on Cards, to `.samples.txt`.
 */
@Composable
fun IdleProbe(window: ComposeWindow, state: AppState, report: File, exit: () -> Unit) {
    LaunchedEffect(Unit) {
        snapshotFlow { state.booted }.first { it }
        val seconds = System.getProperty("lockon.idleProbe.seconds")?.toIntOrNull() ?: 40
        val drawn = DrawnFrames(window)
        val lines = mutableListOf<String>()
        // Cards last and longest, after every other tab has been looked at once, as in a long session.
        for (tab in listOf(Tab.DECKS, Tab.SYNC, Tab.SETTINGS, Tab.CARDS)) {
            state.select(tab)
            // The tab's fade, its artwork and anything it started on showing.
            delay(4_000)
            val length = if (tab == Tab.CARDS) seconds else (seconds / 4).coerceAtLeast(5)
            val sampler = if (tab == Tab.CARDS) StackSampler(Thread.currentThread()).apply { start() } else null
            val gc = gcTotals()
            val from = drawn.count
            val began = System.nanoTime()
            delay(length * 1_000L)
            val frames = drawn.count - from
            val ended = System.nanoTime()
            sampler?.finish()?.let { if (!PROBE_QUIET) File(report.path + ".samples.txt").writeText(it.report(listOf(began..ended))) }
            lines += "%-8s %5d frames drawn in %d s (%.1f a second); collections: %s".format(tab.name, frames, length, frames / length.toDouble(), gcSince(gc))
        }
        report.appendText("${window.renderApi}, ${window.refreshHz()} Hz, ${window.width}×${window.height}, at rest:\n" + lines.joinToString("\n") + "\n\n")
        exit()
    }
}
