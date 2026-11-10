// Compose marks choosing a PrefetchScheduler as no longer supported, since each platform is meant to
// bring its own. Desktop's is empty (CMP-1265), and the strategy still accepts one.
@file:Suppress("DEPRECATION")
@file:OptIn(ExperimentalFoundationApi::class)

package app.lockon.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridPrefetchStrategy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.layout.PrefetchRequest
import androidx.compose.foundation.lazy.layout.PrefetchRequestScope
import androidx.compose.foundation.lazy.layout.PrefetchScheduler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.awt.KeyboardFocusManager
import java.awt.Window

@Composable
actual fun rememberPrefetchingGridState(): LazyGridState {
    if (!PrefetchOn) return rememberLazyGridState()
    val scope = rememberCoroutineScope()
    return rememberLazyGridState(prefetchStrategy = remember { GridPrefetch(BetweenFramesScheduler(scope)) })
}

@Composable
actual fun rememberPrefetchingListState(): LazyListState {
    if (!PrefetchOn) return rememberLazyListState()
    val scope = rememberCoroutineScope()
    return rememberLazyListState(prefetchStrategy = remember { ListPrefetch(BetweenFramesScheduler(scope)) })
}

/** `-Dlockon.prefetch=off` goes back to building rows as they scroll in, to compare. */
private val PrefetchOn = System.getProperty("lockon.prefetch") != "off"

/** Compose's own choice of which row to build next, run by [scheduler]. */
private class GridPrefetch(private val scheduler: PrefetchScheduler, choice: LazyGridPrefetchStrategy = LazyGridPrefetchStrategy()) :
    LazyGridPrefetchStrategy by choice {
    override val prefetchScheduler: PrefetchScheduler get() = scheduler
}

private class ListPrefetch(private val scheduler: PrefetchScheduler, choice: LazyListPrefetchStrategy = LazyListPrefetchStrategy()) :
    LazyListPrefetchStrategy by choice {
    override val prefetchScheduler: PrefetchScheduler get() = scheduler
}

/**
 * Builds rows in the time the window's thread has spare in each frame. A desktop frame composes and
 * records its drawing on that thread, then hands the drawing to the render thread and waits for the
 * display's next refresh; the window's thread is idle from there until the next frame starts, one
 * refresh after this one did. So a slice of work is posted behind the frame's own and runs until
 * [Margin] before that, and what is left waits for the frame after.
 *
 * Asking for the frame's start time asks for a frame, so the window keeps drawing while work is
 * waiting, which is only while or just after something scrolled. A piece of work turns time down
 * when it doesn't expect to fit. One that turns down [StallLimit] fair slices in a row is too big for
 * any slice, and runs whatever the time, so it can't wait, and keep the window drawing, forever.
 */
private class BetweenFramesScheduler(private val scope: CoroutineScope) : PrefetchScheduler, PrefetchRequestScope {
    private val waiting = ArrayDeque<PrefetchRequest>()
    /** A slice is on its way: waiting for the next frame to start, or posted behind it. */
    private var scheduled = false
    private var sliceEnd = 0L
    private var stalled = 0
    /** What [availableTimeNanos] answers while a piece of work runs: null for the time left in the slice. */
    private var offer: Long? = null

    override fun schedulePrefetch(prefetchRequest: PrefetchRequest) {
        // Scenes drawn off screen, as tests and exported pictures are, have no window thread and no
        // display to keep up with; they go without, as before.
        if (!EventQueue.isDispatchThread()) return
        if (!scope.isActive) return waiting.clear()
        waiting.addLast(prefetchRequest)
        awaitFrame()
    }

    private fun awaitFrame() {
        if (scheduled) return
        scheduled = true
        // Undispatched, so the frame is asked for now rather than after the one being built.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            withFrameNanos { frame ->
                val now = System.nanoTime()
                // The frame's time is on the same clock here; if a platform's ever isn't, now is close.
                val start = if (now - frame in 0..FramePeriod.nanos()) frame else now
                sliceEnd = start + FramePeriod.nanos() - Margin
                EventQueue.invokeLater(::runSlice)
            }
        }
    }

    private fun runSlice() {
        scheduled = false
        if (!scope.isActive) return waiting.clear()
        // Once a piece of work stops for lack of time the slice is spent. The rest are still asked, with
        // no time on offer, since ones that were cancelled or are no longer needed say so and leave;
        // a fast scroll cancels rows faster than they are built, and they would pile up here.
        var spent = false
        val each = waiting.iterator()
        while (each.hasNext()) {
            val request = each.next()
            val began = System.nanoTime()
            val force = !spent && stalled >= StallLimit
            offer = if (force) Long.MAX_VALUE else if (spent) 0L else null
            val more = with(request) { execute() }
            offer = null
            if (!more) {
                each.remove()
                if (!spent) stalled = 0
            } else if (!spent) {
                spent = true
                // Turned down a fair slice without starting; a short or late slice says nothing about it.
                if (force || System.nanoTime() - began >= Progress) stalled = 0 else if (sliceEnd - began >= FairSlice) stalled++
            }
        }
        if (waiting.isNotEmpty()) awaitFrame()
    }

    override fun availableTimeNanos(): Long = offer ?: (sliceEnd - System.nanoTime()).coerceAtLeast(0)

    private companion object {
        /** Stop this long before the next frame is due, in nanoseconds. */
        const val Margin = 1_000_000L
        const val StallLimit = 2
        /** A run shorter than this did no work. */
        const val Progress = 100_000L
        /** Turning down a slice this long means the work is too big for one, not that the frame ran long. */
        const val FairSlice = 2_000_000L
    }
}

/** The time between refreshes of the display the window is on, looked up at most once a second. */
private object FramePeriod {
    private var nanos = 1_000_000_000L / 60
    private var checked = 0L

    fun nanos(): Long {
        val now = System.nanoTime()
        if (checked == 0L || now - checked > 1_000_000_000L) {
            checked = now
            val window = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow ?: Window.getWindows().firstOrNull { it.isShowing }
            val device = window?.graphicsConfiguration?.device
                ?: runCatching { GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice }.getOrNull()
            val hz = runCatching { device?.displayMode?.refreshRate }.getOrNull()?.takeIf { it > 0 } ?: 60
            nanos = 1_000_000_000L / hz
        }
        return nanos
    }
}
