package app.lockon.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.AnnotatedString
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
import java.awt.EventQueue
import java.awt.Toolkit
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.SwingUtilities

/**
 * Times the things people do besides scrolling, in the real window: typing a search, adding and
 * removing a card in a deck (and the save that follows 400 ms later), looking at card details,
 * sweeping the pointer across the grid, and resizing the window. Run with
 * `-Plockon.actProbe=<report file>` (best with `-Plockon.dataDir` pointing at a copy of real data);
 * `-Plockon.actProbe.only=type,edit,details,hover,resize` picks some. Clicks and pointer moves are mouse
 * events sent straight to the window, at places found through the screen's accessibility tree, so the
 * real mouse never moves. Typing sets the search field's text through the same tree, since key events
 * only reach a window that has the keyboard, and a probe must not take it: that times filtering and
 * drawing the results, not the text field's own handling of a key. It appends each step's frame gaps,
 * writes slow-frame profiles per step (`.<step>.samples.txt`), and closes the app.
 */
@Composable
fun InteractionProbe(window: ComposeWindow, container: AppContainer, state: AppState, report: File, exit: () -> Unit) {
    LaunchedEffect(Unit) {
        snapshotFlow { state.booted }.first { it }
        delay(3_000)
        val only = System.getProperty("lockon.actProbe.only")?.split(',')?.map { it.trim() }
        val pointer = ProbePointer(window)
        val drawn = DrawnFrames(window)
        val sampler = StackSampler(Thread.currentThread()).apply { start() }
        val render = renderThread()?.let { StackSampler(it).apply { start() } }
        val lines = mutableListOf<String>()
        val profiles = mutableListOf<Pair<String, List<LongRange>>>()
        // When the probe was reading the screen's accessibility tree, which takes this thread tens of
        // milliseconds: frames held up by that are the probe's doing and are left out.
        val asides = mutableListOf<LongRange>()
        suspend fun lookUp(match: (SemanticsNode) -> Boolean): List<SemanticsNode> {
            val began = System.nanoTime()
            val found = nodes(window).filter { match(it) && it.layoutInfo.isPlaced && it.boundsInWindow.width > 1f && it.boundsInWindow.height > 1f }
            delay(150)
            asides += began..System.nanoTime()
            return found
        }

        /** Runs [act] while recording frames; [marks] are the moments something was done, to split a step into parts. */
        suspend fun step(name: String, act: suspend (mark: () -> Unit) -> Unit) {
            if (only != null && name !in only) return
            val frames = mutableListOf<Long>()
            val marks = mutableListOf<Long>()
            coroutineScope {
                val recording = launch { while (true) withFrameNanos { frames += System.nanoTime() } }
                delay(100)
                act { marks += System.nanoTime() }
                recording.cancel()
            }
            if (frames.size < 2) { lines += "$name: no frames"; return }
            val gaps = frames.zipWithNext { a, b -> a..b }.filter { gap -> asides.none { it.first < gap.last && it.last > gap.first } }
            profiles += name to gaps.filter { it.last - it.first > LATE_FRAME_NS }
            // An edit's slow frames apart from its save's, which do different work.
            if (name == "edit") {
                fun around(from: Long, to: Long) = gaps.filter { gap -> gap.last - gap.first > LATE_FRAME_NS && marks.any { gap.last > it + from && gap.first < it + to } }
                profiles += "edit.click" to around(0, 300_000_000L)
                profiles += "edit.save" to around(350_000_000L, 900_000_000L)
            }
            // The longest frame gap in the 300 ms after each thing done: what the hand feels as its answer.
            val answers = marks.map { at -> gaps.filter { it.last > at && it.first < at + 300_000_000L }.maxOfOrNull { (it.last - it.first) / 1e6 } ?: 0.0 }.sorted()
            val answered = if (answers.isEmpty()) "" else "; slowest frame after each of %d actions: median %.1f, worst %.1f ms".format(answers.size, answers[answers.size / 2], answers.last())
            // A deck saves itself 400 ms after a change, and every screen that lists decks hears of it.
            val saves = marks.map { at -> gaps.filter { it.last > at + 350_000_000L && it.first < at + 900_000_000L }.maxOfOrNull { (it.last - it.first) / 1e6 } ?: 0.0 }.sorted()
            val saved = if (name != "edit" || saves.isEmpty()) "" else ", and as each saved: median %.1f, worst %.1f ms".format(saves[saves.size / 2], saves.last())
            lines += "$name: ${gapSummary(gaps)}; ${drawn.workSummary(frames.first(), frames.last())}$answered$saved"
        }

        state.select(Tab.CARDS)
        delay(3_000)

        // A word typed and deleted, a little faster than most people type, twice: the second time through code that has run once.
        step("type") { mark ->
            val field = lookUp { it.config.getOrNull(SemanticsActions.SetText) != null }.firstOrNull()
                ?: return@step run { lines += "type: found no search field" }
            val typed = listOf("g", "gu", "gun", "gund", "gunda", "gundam", "gunda", "gund", "gun", "gu", "g", "", "z", "za", "zak", "zaku", "zaku ", "zaku i", "zaku ii", "zaku i", "zaku", "za", "")
            repeat(2) {
                for (text in typed) {
                    mark()
                    field.config.getOrNull(SemanticsActions.SetText)?.action?.invoke(AnnotatedString(text))
                    delay(120)
                }
                delay(400)
            }
        }

        // Six cards looked at one after another. A wide window shows them in its side panel; a narrow one opens a dialog, closed again each time.
        step("details") { mark ->
            val tiles = lookUp { it.config.getOrNull(SemanticsActions.OnClick)?.label == "Card details" }.take(7).drop(1)
            if (tiles.isEmpty()) return@step run { lines += "details: found no cards" }
            var dialog = true
            for (tile in tiles) {
                    mark()
                pointer.click(tile)
                delay(700)
                if (!dialog) continue
                val close = lookUp { it.config.getOrNull(SemanticsActions.OnClick) != null && it.text() == "Close" }.firstOrNull()
                // No dialog means a wide window, which shows details in its side panel; no need to look again.
                if (close == null) { dialog = false; continue }
                // A dialog: closing it is its own action.
                    mark()
                pointer.click(close)
                delay(500)
            }
        }

        // The pointer swept back and forth across the grid's rows for four seconds, an event every 8 ms as a mouse sends them.
        step("hover") {
            val tiles = lookUp { it.config.getOrNull(SemanticsActions.OnClick)?.label == "Card details" }
            if (tiles.isEmpty()) return@step run { lines += "hover: found no cards" }
            val left = tiles.minOf { it.boundsInWindow.left }; val right = tiles.maxOf { it.boundsInWindow.right }
            val rows = tiles.map { it.boundsInWindow.center.y }.distinct().sorted().take(3)
            withContext(Dispatchers.IO) {
                repeat(500) { i ->
                    val row = rows[(i / 125) % rows.size]
                    val along = (i % 125) / 124f
                    val x = if ((i / 125) % 2 == 0) left + (right - left) * along else right - (right - left) * along
                    pointer.move(x, row)
                    Thread.sleep(8)
                }
            }
            pointer.leave()
            delay(300)
        }

        // The window narrowed by 240 and widened again twice over three seconds, a step every 16 ms, as when its edge is dragged.
        step("resize") {
            val size = window.size
            withContext(Dispatchers.IO) {
                repeat(180) { i ->
                    val swing = (1 - kotlin.math.cos(i / 180.0 * 4 * Math.PI)) / 2
                    EventQueue.invokeLater { window.setSize(size.width - (240 * swing).toInt(), size.height) }
                    Thread.sleep(16)
                }
            }
            EventQueue.invokeLater { window.size = size }
            delay(400)
        }

        // A copy of a card added to a deck and taken out again, four cards over; each waits out the save that follows 400 ms on.
        val deck = container.decks.liveDecks.first().filterNot { it.snapshot.deleted }.maxByOrNull { it.snapshot.entries.sumOf { e -> e.quantity } }
        if (deck != null && (only == null || "edit" in only)) {
            state.select(Tab.DECKS)
            delay(1_000)
            state.open(Screen.DeckEditor(deck.deckId))
            delay(4_000)
            step("edit") { mark ->
                // In the card list, which a wide editor has on the left; the deck's own cards have the same buttons.
                val adds = lookUp { it.description()?.startsWith("Add one ") == true }.sortedBy { it.boundsInWindow.left }.take(4)
                if (adds.isEmpty()) return@step run { lines += "edit: found no + buttons" }
                for (add in adds) {
                    val name = add.description()!!.removePrefix("Add one ")
                    val remove = lookUp { it.description() == "Remove one $name" }.firstOrNull { it.boundsInWindow.overlaps(add.boundsInWindow.inflate(200f)) } ?: continue
                            mark()
                    pointer.click(add)
                    delay(1_200)
                    mark()
                    pointer.click(remove)
                    delay(1_200)
                }
            }
            state.goBack()
            delay(500)
        }

        sampler.finish()
        render?.finish()
        if (!PROBE_QUIET) for ((name, slow) in profiles) {
            File("${report.path}.$name.samples.txt").writeText(sampler.report(slow))
            render?.let { File("${report.path}.$name.render.samples.txt").writeText(it.report(slow)) }
        }
        report.appendText("${window.renderApi}, ${window.refreshHz()} Hz, ${window.width}×${window.height}:\n" + lines.joinToString("\n") { "  $it" } + "\n\n")
        exit()
    }
}

/**
 * Every node of the window's accessibility tree, unmerged, so a button and its label are separate nodes.
 * Kept screens that are out of sight stay in the tree, unplaced.
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun nodes(window: ComposeWindow): List<SemanticsNode> = window.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }

private fun SemanticsNode.description(): String? = config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()

private fun SemanticsNode.text(): String? = config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
    ?: children.firstNotNullOfOrNull { it.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text }

/** Mouse events sent straight to the window, at places given in the window's pixels as the accessibility tree has them. */
internal class ProbePointer(private val window: ComposeWindow) {
    private val pane = window.contentPane
    private val queue = Toolkit.getDefaultToolkit().systemEventQueue
    // The window's own units are pixels divided by the display's scale.
    private val scale = window.graphicsConfiguration.defaultTransform.scaleX.toFloat()
    private var lastX = 0
    private var lastY = 0

    private fun post(id: Int, x: Int, y: Int, mods: Int = 0, button: Int = MouseEvent.NOBUTTON, clicks: Int = 0) {
        // To the component Compose listens on, as real input goes, not to the content pane.
        val target = SwingUtilities.getDeepestComponentAt(pane, x, y) ?: pane
        val at = SwingUtilities.convertPoint(pane, x, y, target)
        queue.postEvent(MouseEvent(target, id, System.currentTimeMillis(), mods, at.x, at.y, clicks, false, button))
        lastX = x; lastY = y
    }

    fun move(x: Float, y: Float) = post(MouseEvent.MOUSE_MOVED, (x / scale).toInt(), (y / scale).toInt())

    fun click(node: SemanticsNode) {
        val x = (node.boundsInWindow.center.x / scale).toInt()
        val y = (node.boundsInWindow.center.y / scale).toInt()
        post(MouseEvent.MOUSE_MOVED, x, y)
        post(MouseEvent.MOUSE_PRESSED, x, y, InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1, 1)
        post(MouseEvent.MOUSE_RELEASED, x, y, 0, MouseEvent.BUTTON1, 1)
    }

    /** The pointer leaves the window, so nothing stays hovered. */
    fun leave() = post(MouseEvent.MOUSE_EXITED, lastX, lastY)
}
