package app.lockon.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import app.lockon.ui.WindowChrome
import app.lockon.ui.components.Motion
import app.lockon.ui.components.PillShape
import app.lockon.ui.components.glassColors
import app.lockon.ui.components.isTwilight
import app.lockon.ui.components.motion
import com.sun.jna.CallbackReference
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.LRESULT
import com.sun.jna.platform.win32.WinDef.POINT
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import java.awt.Cursor
import java.awt.Window

/**
 * Lets Lock On draw its own title bar on Windows while keeping the native frame (UX rule N5). Only
 * the caption strip goes: the window procedure answers WM_NCCALCSIZE with the client area reaching
 * the window's top, so the sky runs to the edge. The window keeps its Windows 11 rounded corners and
 * shadow, its side and bottom resize edges, Snap, Win+arrow keys and Alt+Space. Moving and top-edge
 * resizing are handed back to Windows' own loops, so Snap works when dragging too.
 */
internal object WindowsFrame {
    private val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows")

    // Held here so the garbage collector never frees a window procedure Windows still calls.
    private var procedure: WinUser.WindowProc? = null

    private const val WM_NCCALCSIZE = 0x0083
    private const val WM_NCLBUTTONDOWN = 0x00A1
    /** Ours: "start a native move or resize", posted so it runs on the thread that owns the window. */
    private const val WM_BEGIN_FRAME_DRAG = 0x8000 + 0x4C
    private const val WS_MAXIMIZE = 0x01000000L
    private const val SWP_FRAME_CHANGED = 0x0020 or 0x0001 or 0x0002 or 0x0004 or 0x0010

    const val HT_CAPTION = 2
    const val HT_TOP = 12
    const val HT_TOP_LEFT = 13
    const val HT_TOP_RIGHT = 14

    /** The calls jna-platform's User32 lacks. */
    private interface Capture : StdCallLibrary {
        fun ReleaseCapture(): Boolean

        companion object {
            val INSTANCE: Capture by lazy { Native.load("user32", Capture::class.java, W32APIOptions.DEFAULT_OPTIONS) }
        }
    }

    /** Removes the caption from [window]. False off Windows, or if Windows refuses: the system's title bar then stays. */
    fun install(window: Window): Boolean {
        if (procedure != null) return true
        if (!isWindows) return false
        return runCatching {
            val user32 = User32.INSTANCE
            val hwnd = HWND(Native.getWindowPointer(window))
            // Read first, so a message arriving mid-swap still has somewhere to go.
            val previous = user32.GetWindowLongPtr(hwnd, WinUser.GWL_WNDPROC).toPointer()
            val proc = object : WinUser.WindowProc {
                override fun callback(hWnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT = when {
                    uMsg == WM_NCCALCSIZE && wParam.toLong() != 0L -> {
                        // Let Windows size the frame, then give the caption's height back to the client.
                        val rect = Pointer(lParam.toLong())
                        val left = rect.getInt(0)
                        val top = rect.getInt(4)
                        user32.CallWindowProc(previous, hWnd, uMsg, wParam, lParam)
                        // A maximised window hangs its frame past the screen's edges; keep the top on screen by as much.
                        val maximized = user32.GetWindowLongPtr(hWnd, WinUser.GWL_STYLE).toLong() and WS_MAXIMIZE != 0L
                        rect.setInt(4, top + if (maximized) rect.getInt(0) - left else 0)
                        LRESULT(0)
                    }
                    uMsg == WM_BEGIN_FRAME_DRAG -> {
                        // The press went to the app's canvas, which holds the mouse; Windows' loop needs it.
                        Capture.INSTANCE.ReleaseCapture()
                        user32.DefWindowProc(hWnd, WM_NCLBUTTONDOWN, wParam, lParam)
                    }
                    else -> user32.CallWindowProc(previous, hWnd, uMsg, wParam, lParam)
                }
            }
            user32.SetWindowLongPtr(hwnd, WinUser.GWL_WNDPROC, CallbackReference.getFunctionPointer(proc))
            procedure = proc
            user32.SetWindowPos(hwnd, null, 0, 0, 0, 0, SWP_FRAME_CHANGED)
            true
        }.onFailure {
            org.slf4j.LoggerFactory.getLogger("LockOn").warn("Keeping the system title bar", it)
        }.getOrDefault(false)
    }

    /** Hands the held mouse button to Windows: [hit] [HT_CAPTION] moves the window, the others resize it. */
    fun beginDrag(window: Window, hit: Int) {
        if (procedure == null) return
        runCatching {
            val cursor = POINT().also { User32.INSTANCE.GetCursorPos(it) }
            val at = (cursor.x and 0xFFFF) or ((cursor.y and 0xFFFF) shl 16)
            User32.INSTANCE.PostMessage(HWND(Native.getWindowPointer(window)), WM_BEGIN_FRAME_DRAG, WPARAM(hit.toLong()), LPARAM(at.toLong()))
        }
    }
}

/**
 * The title bar for the shared UI: the top band moves the window and a double-click maximises it,
 * and the controls capsule sits beside the navigation pill.
 */
@Composable
internal fun rememberWindowChrome(window: Window, state: WindowState, onClose: () -> Unit): WindowChrome {
    val close by rememberUpdatedState(onClose)
    return remember(window, state) {
        val toggle = { state.placement = if (state.placement == WindowPlacement.Maximized) WindowPlacement.Floating else WindowPlacement.Maximized }
        WindowChrome(
            dragArea = { height -> windowDrag(window, height, toggle) },
            controls = { WindowControls(state.placement == WindowPlacement.Maximized, { state.isMinimized = true }, toggle, { close() }) },
        )
    }
}

/**
 * Moves the window when the top [height] of this element is dragged, and toggles maximise on a
 * double-click there. It sits on a parent, so it hears presses after the children and skips the ones
 * a button or field consumed. The drag starts past the touch slop, so a click never moves the window.
 */
private fun Modifier.windowDrag(window: Window, height: Dp, onToggleMaximize: () -> Unit): Modifier = pointerInput(window, height) {
    var lastPress = 0L
    var lastAt = Offset.Zero
    awaitPointerEventScope {
        while (true) {
            val press = awaitPointerEvent()
            if (press.type != PointerEventType.Press || !press.buttons.isPrimaryPressed) continue
            val change = press.changes.first()
            val at = change.position
            if (change.isConsumed || at.y > height.toPx()) continue
            if (change.uptimeMillis - lastPress < viewConfiguration.doubleTapTimeoutMillis && (at - lastAt).getDistance() < viewConfiguration.touchSlop) {
                lastPress = 0L
                onToggleMaximize()
                continue
            }
            lastPress = change.uptimeMillis
            lastAt = at
            // Windows' own move loop eats the release, so this waits only until the drag is handed over.
            while (true) {
                val event = awaitPointerEvent()
                if (!event.buttons.isPrimaryPressed || event.changes.first().isConsumed) break
                if ((event.changes.first().position - at).getDistance() > viewConfiguration.touchSlop) {
                    lastPress = 0L
                    WindowsFrame.beginDrag(window, WindowsFrame.HT_CAPTION)
                    break
                }
            }
        }
    }
}

/**
 * The window's top resize edge. Without a caption Windows has no frame above the client area, so a
 * thin strip over the sky takes its place, with its corners resizing diagonally. Hidden when maximised.
 */
@Composable
internal fun BoxScope.TopResizeEdge(window: Window) {
    @Composable
    fun Edge(hit: Int, cursor: Int, modifier: Modifier) = Box(
        modifier.fillMaxHeight()
            .pointerHoverIcon(PointerIcon(Cursor(cursor)))
            .pointerInput(hit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.buttons.isPrimaryPressed) WindowsFrame.beginDrag(window, hit)
                    }
                }
            },
    )
    Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(4.dp)) {
        Edge(WindowsFrame.HT_TOP_LEFT, Cursor.NW_RESIZE_CURSOR, Modifier.width(14.dp))
        Edge(WindowsFrame.HT_TOP, Cursor.N_RESIZE_CURSOR, Modifier.weight(1f))
        Edge(WindowsFrame.HT_TOP_RIGHT, Cursor.NE_RESIZE_CURSOR, Modifier.width(14.dp))
    }
}

/**
 * Minimise, maximise or restore, and close, in a small glass capsule matching the navigation pill
 * (UX rules N5, V9). Each is labelled for screen readers and has a tooltip (I5). Close turns red on
 * hover, as Windows' does, so it is never pressed by mistake for its neighbour.
 */
@Composable
internal fun WindowControls(
    maximized: Boolean,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shade = glassColors().shade
    Row(
        modifier
            .shadow(10.dp, PillShape, ambientColor = shade, spotColor = shade)
            .clip(PillShape)
            // The navigation pill's own fill and hairline, so the two read as a pair.
            .background(if (isTwilight()) scheme.surfaceContainerHigh.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.9f))
            .border(BorderStroke(0.8.dp, scheme.onSurface.copy(alpha = 0.12f)), PillShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        ControlButton("Minimize", onMinimize) { ink, unit ->
            drawLine(ink, Offset(center.x - 5 * unit, center.y), Offset(center.x + 5 * unit, center.y), 1.4f * unit, StrokeCap.Round)
        }
        ControlButton(if (maximized) "Restore" else "Maximize", onToggleMaximize) { ink, unit ->
            val stroke = Stroke(1.4f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
            if (maximized) {
                // Two overlapping windows: the front one whole, the back one peeking out above and right.
                drawRoundRect(ink, Offset(center.x - 4.5f * unit, center.y - 2.5f * unit), Size(7 * unit, 7 * unit), CornerRadius(1.5f * unit), stroke)
                drawPath(Path().apply {
                    moveTo(center.x - 2 * unit, center.y - 2.5f * unit)
                    lineTo(center.x - 2 * unit, center.y - 5 * unit)
                    lineTo(center.x + 5 * unit, center.y - 5 * unit)
                    lineTo(center.x + 5 * unit, center.y + 2 * unit)
                    lineTo(center.x + 2.5f * unit, center.y + 2 * unit)
                }, ink, style = stroke)
            } else {
                drawRoundRect(ink, Offset(center.x - 4.5f * unit, center.y - 4.5f * unit), Size(9 * unit, 9 * unit), CornerRadius(2 * unit), stroke)
            }
        }
        ControlButton("Close", onClose, danger = true) { ink, unit ->
            val r = 4.5f * unit
            drawLine(ink, Offset(center.x - r, center.y - r), Offset(center.x + r, center.y + r), 1.4f * unit, StrokeCap.Round)
            drawLine(ink, Offset(center.x - r, center.y + r), Offset(center.x + r, center.y - r), 1.4f * unit, StrokeCap.Round)
        }
    }
}

/** One window control: a pill that tints on hover and sinks a hair when pressed, drawing [glyph] in 1 dp [unit]s. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ControlButton(label: String, onClick: () -> Unit, danger: Boolean = false, glyph: DrawScope.(ink: Color, unit: Float) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val lit = hovered || pressed
    val fill by animateColorAsState(
        when {
            danger && lit -> scheme.error.copy(alpha = if (pressed) 0.82f else 1f)
            pressed -> scheme.onSurface.copy(alpha = 0.16f)
            hovered -> scheme.onSurface.copy(alpha = 0.08f)
            else -> Color.Transparent
        },
        motion(Motion.SHORT), label = "control-fill",
    )
    val ink by animateColorAsState(if (danger && lit) scheme.onError else scheme.onSurface.copy(alpha = 0.78f), motion(Motion.SHORT), label = "control-ink")
    val squish by animateFloatAsState(if (pressed) 0.94f else 1f, motion(Motion.SHORT), label = "control-press")
    TooltipArea(
        tooltip = { ControlTooltip(label) },
        delayMillis = 500,
        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 18.dp)),
    ) {
        Canvas(
            Modifier.size(40.dp, 32.dp)
                .graphicsLayer { scaleX = squish; scaleY = squish }
                .clip(PillShape)
                .background(fill)
                .hoverable(interaction)
                .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
        ) { glyph(ink, density) }
    }
}

/** A tooltip on a small glass pill. */
@Composable
private fun ControlTooltip(text: String) {
    val g = glassColors()
    Text(
        text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.shadow(6.dp, PillShape, ambientColor = g.shade, spotColor = g.shade).clip(PillShape)
            .background(if (isTwilight()) MaterialTheme.colorScheme.surfaceContainerHighest else Color.White)
            .border(BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)), PillShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
