package app.lockon.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.lockon.AppContainer
import app.lockon.ui.AppState
import app.lockon.ui.Screen
import app.lockon.ui.Tab
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * Times the app's own animations in the real window, each over just the stretch it plays: tabs fading
 * through each other, a deck sliding in and out, a deck's pages, a menu, a dialog and the dim behind it,
 * the filter drawer, card details, buttons lighting under the pointer, and a change of theme. Run with
 * `-Plockon.motionProbe=<report file>` (best with `-Plockon.dataDir` pointing at a copy of real data);
 * `-Plockon.motionProbe.only=tabs,deck,pages,menu,dialog,hover,filters,details,theme` picks some.
 * Each is done once unmeasured first, so what is timed is the animation and not a first use. Clicks and
 * pointer moves are mouse events sent straight to the window, so the real mouse never moves. For each
 * animation it appends the gaps between frames while it played, what the window's thread spent on each
 * of those frames, and the slowest frame after each time it was started; then it closes the app.
 */
@Composable
fun MotionProbe(window: ComposeWindow, container: AppContainer, state: AppState, report: File, exit: () -> Unit) {
    LaunchedEffect(Unit) {
        try {
        snapshotFlow { state.booted }.first { it }
        delay(3_000)
        val only = System.getProperty("lockon.motionProbe.only")?.split(',')?.map { it.trim() }
        fun wanted(step: String) = only == null || step in only
        val pointer = ProbePointer(window)
        val drawn = DrawnFrames(window)
        val sampler = StackSampler(Thread.currentThread()).apply { start() }
        val render = renderThread()?.let { StackSampler(it).apply { start() } }
        val frames = mutableListOf<Long>()
        val recording = launch { while (true) withFrameNanos { frames += System.nanoTime() } }
        // The stretches each animation played over, by name, in the order first timed.
        val played = LinkedHashMap<String, MutableList<LongRange>>()

        /** Starts an animation with [act] and counts the next [length] ms as its own. */
        suspend fun timed(name: String, length: Long = 400, act: () -> Unit) {
            val from = System.nanoTime()
            act()
            delay(length)
            played.getOrPut(name) { mutableListOf() } += from..System.nanoTime()
            delay(300)
        }

        /** Reading the screen's accessibility tree takes this thread tens of milliseconds, so it happens between animations. */
        suspend fun find(match: (SemanticsNode) -> Boolean): List<SemanticsNode> {
            val found = nodes(window).filter { match(it) && it.layoutInfo.isPlaced && it.boundsInWindow.width > 1f && it.boundsInWindow.height > 1f }
            delay(200)
            return found
        }
        fun SemanticsNode.clicks() = config.getOrNull(SemanticsActions.OnClick) != null
        // A button's label can sit several layers inside it.
        fun SemanticsNode.says(label: String): Boolean = text() == label || description() == label || children.any { it.says(label) }
        suspend fun button(label: String) = find { it.clicks() && it.says(label) }.minByOrNull { it.boundsInWindow.width * it.boundsInWindow.height }
        // The window's own close button is also called Close; a dialog's is a word.
        suspend fun closeButton() = find { it.clicks() && it.says("Close") && it.description() != "Close" }.minByOrNull { it.boundsInWindow.width * it.boundsInWindow.height }

        if (wanted("tabs")) {
            for (tab in Tab.entries) { state.select(tab); delay(700) }
            for (tab in listOf(Tab.CARDS, Tab.DECKS, Tab.CARDS, Tab.SYNC, Tab.SETTINGS, Tab.DECKS, Tab.CARDS, Tab.DECKS)) timed("tab switch") { state.select(tab) }
        }

        state.select(Tab.DECKS)
        delay(700)
        val deck = container.decks.liveDecks.first().filterNot { it.snapshot.deleted }
            .sortedByDescending { it.updatedAt }.maxByOrNull { it.snapshot.entries.sumOf { e -> e.quantity } }
        if (deck != null) {
            state.open(Screen.DeckEditor(deck.deckId)); delay(2_000)
            state.goBack(); delay(1_000)
            if (wanted("deck")) repeat(5) {
                timed("deck open") { state.open(Screen.DeckEditor(deck.deckId)) }
                timed("deck back") { state.goBack() }
            }
            state.open(Screen.DeckEditor(deck.deckId)); delay(1_500)
            if (wanted("pages")) {
                val pages = listOf("Analysis", "Test", "Matches", "Versions", "Deck")
                val tabs = pages.mapNotNull { label -> find { it.config.getOrNull(SemanticsProperties.Role) == Role.Tab && (it.text() == label || it.description() == label) }.firstOrNull() }
                // Once round unmeasured, so each page has been built before.
                for (tab in tabs) { pointer.click(tab); delay(600) }
                repeat(2) { for (tab in tabs) timed("deck page") { pointer.click(tab) } }
            }
            if (wanted("menu")) button("More")?.let { more ->
                repeat(4) { n ->
                    val name = if (n == 0) null else "menu"
                    if (name == null) { pointer.click(more); delay(600); pointer.clickCorner(); delay(600) }
                    else { timed("menu open") { pointer.click(more) }; timed("menu close") { pointer.clickCorner() } }
                }
            }
            if (wanted("dialog")) button("Export")?.let { export ->
                repeat(4) { n ->
                    if (n == 0) pointer.click(export).also { delay(900) } else timed("dialog open") { pointer.click(export) }
                    val close = closeButton() ?: return@repeat
                    if (n == 0) pointer.click(close).also { delay(700) } else timed("dialog close") { pointer.click(close) }
                }
            }
            state.goBack(); delay(1_000)
        }

        // The pointer crossing the buttons on show, a new one every 70 ms, so several are easing at once.
        if (wanted("hover")) {
            val buttons = find { it.clicks() && it.config.getOrNull(SemanticsProperties.Role) == Role.Button }
                .sortedWith(compareBy({ it.boundsInWindow.top.toInt() / 24 }, { it.boundsInWindow.left })).take(24)
            if (buttons.size >= 2) repeat(3) { n ->
                val from = System.nanoTime()
                for (b in buttons) { pointer.move(b.boundsInWindow.center.x, b.boundsInWindow.center.y); delay(70) }
                pointer.leave()
                delay(200)
                if (n > 0) played.getOrPut("hover buttons") { mutableListOf() } += from..System.nanoTime()
                delay(200)
            }
        }

        state.select(Tab.CARDS)
        delay(1_500)
        if (wanted("filters")) find { it.clicks() && it.text() == "Filters" }.firstOrNull()?.let { filters ->
            pointer.click(filters); delay(700); pointer.click(filters); delay(700)
            repeat(3) {
                timed("filters open") { pointer.click(filters) }
                timed("filters close") { pointer.click(filters) }
            }
        }
        if (wanted("details")) {
            val tiles = find { it.config.getOrNull(SemanticsActions.OnClick)?.label == "Card details" }.take(6)
            for ((n, tile) in tiles.withIndex()) {
                if (n == 0) pointer.click(tile).also { delay(800) } else timed("card details") { pointer.click(tile) }
                // A narrow window shows details in a dialog; a wide one in its side panel, which stays.
                val close = closeButton() ?: continue
                if (n == 0) pointer.click(close).also { delay(700) } else timed("card details close") { pointer.click(close) }
            }
        }

        if (wanted("theme")) {
            state.select(Tab.SETTINGS)
            delay(1_500)
            val dark = button("Twilight"); val light = button("Sky")
            if (dark != null && light != null) {
                pointer.click(dark); delay(900); pointer.click(light); delay(900)
                repeat(3) {
                    timed("theme change", 700) { pointer.click(dark) }
                    timed("theme change", 700) { pointer.click(light) }
                }
            }
        }

        recording.cancel()
        sampler.finish()
        render?.finish()
        val gaps = frames.zipWithNext { a, b -> a..b }
        // What the window's thread and the render thread were doing in each animation's late frames.
        if (!PROBE_QUIET) for ((name, spans) in played) {
            val slow = gaps.filter { gap -> gap.last - gap.first > LATE_FRAME_NS && spans.any { it.first < gap.last && it.last > gap.first } }
            val file = name.replace(' ', '-')
            File("${report.path}.$file.samples.txt").writeText(sampler.report(slow))
            render?.let { File("${report.path}.$file.render.samples.txt").writeText(it.report(slow)) }
            // Frame by frame: when each began after the animation started and how long the window's thread spent on it.
            File("${report.path}.$file.timeline.txt").writeText(spans.joinToString("|") { drawn.timeline(it.first, it.last) })
        }
        val lines = played.map { (name, spans) ->
            val during = gaps.filter { gap -> spans.any { it.first < gap.last && it.last > gap.first } }
            val worst = spans.map { span -> gaps.filter { span.first < it.last && span.last > it.first }.maxOfOrNull { (it.last - it.first) / 1e6 } ?: 0.0 }
            "%-18s %d times: %s; %s; slowest frame each time: median %.1f, worst %.1f ms (%s)".format(
                name, spans.size, gapSummary(during), drawn.workSummary(spans), worst.sorted()[worst.size / 2], worst.max(),
                worst.joinToString(" ") { "%.0f".format(it) },
            )
        }
        report.appendText("${System.getProperty("lockon.motionProbe.label") ?: "run"}: ${window.renderApi}, ${window.refreshHz()} Hz, ${window.width}×${window.height}:\n" + lines.joinToString("\n") { "  $it" } + "\n\n")
        } catch (e: Throwable) { report.appendText("failed: " + e.stackTraceToString()); throw e }
        exit()
    }
}
