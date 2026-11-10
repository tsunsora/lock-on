package app.lockon.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.foundation.text.TextContextMenuArea
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.rememberPopupPositionProviderAtPosition
import app.lockon.ui.components.GameMenuDivider
import app.lockon.ui.components.Motion
import app.lockon.ui.components.PillShape
import app.lockon.ui.components.arrive
import app.lockon.ui.components.collectIsPressedHeld
import app.lockon.ui.components.glassColors
import app.lockon.ui.components.motion
import app.lockon.ui.components.rememberArrival
import app.lockon.ui.theme.LockOnShapes
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.Window
import java.awt.datatransfer.DataFlavor
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

/** A menu row with its [shortcut] shown at the right. Greyed out when it has no [action]. */
internal class MenuAction(label: String, val shortcut: String?, val action: (() -> Unit)?) : ContextMenuItem(label, action ?: {})

/** A hairline between groups of rows. */
internal object MenuDivider : ContextMenuItem("", {})

/** Draws right-click menus on the app's menu panel. */
internal object AppContextMenu : ContextMenuRepresentation {
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        val status = state.status
        if (status !is ContextMenuState.Status.Open) return
        val close = { state.status = ContextMenuState.Status.Closed }
        Popup(
            popupPositionProvider = rememberPopupPositionProviderAtPosition(status.rect.center),
            onDismissRequest = close,
            properties = PopupProperties(focusable = true),
            onKeyEvent = { event ->
                (event.type == KeyEventType.KeyDown && event.key == Key.Escape).also { if (it) close() }
            },
        ) {
            MenuPanel(remember(status) { items() }, close)
        }
    }
}

/** The rows of a text field's or selectable text's right-click menu. */
@OptIn(ExperimentalFoundationApi::class)
internal class AppTextContextMenu(private val window: Window) : TextContextMenu {
    @Composable
    override fun Area(textManager: TextContextMenu.TextManager, state: ContextMenuState, content: @Composable () -> Unit) {
        val scope = rememberCoroutineScope()
        // Compose has no call for undo, so the row presses the field's own shortcut. The menu holds the
        // focus until it has gone from the screen, which takes a frame or two after it closes.
        fun press(keyCode: Int, extraKeys: Int = 0): () -> Unit = {
            scope.launch {
                repeat(2) { withFrameNanos { } }
                pressShortcut(window, keyCode, extraKeys)
            }
        }
        val undo = press(KeyEvent.VK_Z)
        val redo = if (isMac) press(KeyEvent.VK_Z, InputEvent.SHIFT_DOWN_MASK) else press(KeyEvent.VK_Y)
        TextContextMenuArea(textManager, items = { textMenu(textManager, undo, redo) }, state, content)
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun textMenu(text: TextContextMenu.TextManager, undo: () -> Unit, redo: () -> Unit): List<ContextMenuItem> {
    val copy = MenuAction("Copy", shortcut("C"), text.copy)
    val selectAll = MenuAction("Select all", shortcut("A"), text.selectAll)
    // Text that can't be edited offers nothing to cut or paste, even with something on the clipboard.
    val readOnly = text.cut == null && (text.copy != null || (text.paste == null && clipboardHasText()))
    if (readOnly) return listOf(copy, MenuDivider, selectAll)
    return listOf(
        MenuAction("Undo", shortcut("Z"), undo),
        MenuAction("Redo", if (isMac) "⇧⌘Z" else shortcut("Y"), redo),
        MenuDivider,
        MenuAction("Cut", shortcut("X"), text.cut),
        copy,
        MenuAction("Paste", shortcut("V"), text.paste),
        MenuDivider,
        selectAll,
    )
}

private val isMac = System.getProperty("os.name").orEmpty().startsWith("Mac")

private fun shortcut(letter: String) = if (isMac) "⌘$letter" else "Ctrl+$letter"

private fun clipboardHasText(): Boolean =
    runCatching { Toolkit.getDefaultToolkit().systemClipboard.isDataFlavorAvailable(DataFlavor.stringFlavor) }.getOrDefault(false)

private fun pressShortcut(window: Window, keyCode: Int, extraKeys: Int) {
    val target = window.focusOwner ?: return
    val keys = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx or extraKeys
    listOf(KeyEvent.KEY_PRESSED, KeyEvent.KEY_RELEASED).forEach { id ->
        target.dispatchEvent(KeyEvent(target, id, System.currentTimeMillis(), keys, keyCode, KeyEvent.CHAR_UNDEFINED))
    }
}

/** The panel [GameMenu][app.lockon.ui.components.GameMenu] draws, with closely set rows for the mouse. */
@Composable
internal fun MenuPanel(items: List<ContextMenuItem>, onClose: () -> Unit) {
    val g = glassColors()
    val shape = LockOnShapes.panel
    Column(
        Modifier
            .arrive(rememberArrival(Motion.SHORT), from = 0.97f)
            .padding(12.dp) // Room for the shadow inside the popup.
            .shadow(10.dp, shape, ambientColor = g.shade, spotColor = g.shade)
            .clip(shape)
            .background(g.strong.compositeOver(MaterialTheme.colorScheme.background))
            .border(BorderStroke(app.lockon.ui.components.HairlineWidth, g.line), shape)
            .width(IntrinsicSize.Max)
            .widthIn(min = 200.dp)
            .padding(6.dp),
    ) {
        items.forEach { item ->
            when (item) {
                MenuDivider -> GameMenuDivider()
                is MenuAction -> MenuRow(item.label, item.shortcut, item.action?.let { action -> { onClose(); action() } })
                else -> MenuRow(item.label, null) { onClose(); item.onClick() }
            }
        }
    }
}

@Composable
private fun MenuRow(label: String, shortcut: String?, onClick: (() -> Unit)?) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedHeld()
    val enabled = onClick != null
    val fill by animateColorAsState(
        when {
            enabled && pressed -> scheme.onSurface.copy(alpha = 0.20f)
            enabled && hovered -> scheme.onSurface.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        motion(Motion.SHORT), label = "context-menu-row",
    )
    val ink = if (enabled) scheme.onSurface else scheme.onSurface.copy(alpha = 0.38f)
    Row(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(PillShape)
            .background(fill)
            .hoverable(interaction, enabled)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button) { onClick?.invoke() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(label, color = ink, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (shortcut != null) {
            Text(shortcut, color = if (enabled) scheme.onSurface.copy(alpha = 0.62f) else ink, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
