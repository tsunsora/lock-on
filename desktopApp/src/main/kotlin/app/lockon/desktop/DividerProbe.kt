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
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.io.File
import kotlin.math.sin

/**
 * Times dragging the deck editor's divider in the real window, where both areas follow the pointer.
 * Run with `-Plockon.dividerProbe=<report file>` (best with `-Plockon.dataDir` pointing at a copy of
 * real data): it opens a deck, swings the divider back and forth for five seconds with mouse events
 * sent straight to the window, so the real mouse never moves, writes the report and closes the app.
 */
@Composable
fun DividerProbe(window: ComposeWindow, container: AppContainer, state: AppState, report: File, exit: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3_000)
        state.select(Tab.DECKS)
        val deck = container.decks.liveDecks.first { it.isNotEmpty() }.first { !it.snapshot.deleted }
        state.open(Screen.DeckEditor(deck.deckId))
        // Let the card list fill in and its first screen of artwork arrive.
        delay(5_000)
        val split = container.database.metaDao().get("ui.deckEditor.split")?.toFloatOrNull() ?: 0.615f
        val pane = window.contentPane
        // The editor's areas sit 10 dp in from each side with a 10 dp divider between them; window
        // units are dp here.
        val usable = pane.width - 30
        val startX = 10 + (usable * split).toInt() + 5
        val y = pane.height / 2
        val hz = window.graphicsConfiguration.device.displayMode.refreshRate.takeIf { it > 0 } ?: 60
        val frames = mutableListOf<Long>()
        val sampler = StackSampler(Thread.currentThread()).apply { start() }
        coroutineScope {
            val recording = launch { while (true) withFrameNanos { frames += System.nanoTime() } }
            withContext(Dispatchers.IO) {
                val queue = Toolkit.getDefaultToolkit().systemEventQueue
                fun post(id: Int, x: Int, mods: Int) =
                    queue.postEvent(MouseEvent(pane, id, System.currentTimeMillis(), mods, x, y, 1, false, MouseEvent.BUTTON1))
                post(MouseEvent.MOUSE_PRESSED, startX, InputEvent.BUTTON1_DOWN_MASK)
                // A pointer event every 4 ms, swinging 300 dp each way, faster than a hand drags.
                repeat(1_250) { i ->
                    post(MouseEvent.MOUSE_DRAGGED, startX + (300 * sin(i / 60.0)).toInt(), InputEvent.BUTTON1_DOWN_MASK)
                    Thread.sleep(4)
                }
                post(MouseEvent.MOUSE_RELEASED, startX, 0)
            }
            recording.cancel()
        }
        val slow = frames.zipWithNext { a, b -> a..b }.filter { it.last - it.first > 16_700_000L }
        File(report.path + ".samples.txt").writeText(sampler.finish().report(slow))
        val gaps = frames.zipWithNext { a, b -> (b - a) / 1_000_000.0 }.sorted()
        val budget = 1_000.0 / hz
        report.appendText(
            "${window.renderApi}, $hz Hz, ${window.width}×${window.height}, divider drag: %d frames in %.1f s, mean %.1f ms, median %.1f ms, p90 %.1f ms, p99 %.1f ms, worst %.1f ms, %d over 1.5× budget (%.1f ms), %d over 16.7 ms\n".format(
                frames.size, (frames.last() - frames.first()) / 1e9, gaps.average(), gaps[gaps.size / 2], gaps[gaps.size * 9 / 10],
                gaps[gaps.size * 99 / 100], gaps.last(), gaps.count { it > budget * 1.5 }, budget, gaps.count { it > 1_000.0 / 60 },
            ),
        )
        exit()
    }
}
