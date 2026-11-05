package app.lockon.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.lockon.ui.theme.LockOnShapes

/** Names used around the app for the outline icons. */
object Glyphs {
    val Folder get() = LineIcons.Folder
    val Paste get() = LineIcons.Paste
    val Shield get() = LineIcons.Unit
    val Link get() = LineIcons.Link
    val Bolt get() = LineIcons.Command
    val Monitor get() = LineIcons.Desktop
    val Phone get() = LineIcons.Phone
    val Filters get() = LineIcons.Filter
    val Sort get() = LineIcons.Sort
    val Grid get() = LineIcons.Grid
    val Rows get() = LineIcons.List
    val SideCard get() = LineIcons.Side
    val Undo get() = LineIcons.Undo
}

/** A deck's state as an outlined tag: Legal with a check, Needs review in red with a warning, Draft plain. */
@Composable
fun StateTag(legal: Boolean, review: Boolean = false, modifier: Modifier = Modifier, label: String? = null, icon: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    val ink = if (review) scheme.error else scheme.onSurface
    val shape = RoundedCornerShape(9.dp)
    Row(
        modifier.clip(shape).background(if (review) scheme.error.copy(alpha = 0.1f) else scheme.onSurface.copy(alpha = 0.06f)).border(1.dp, if (review) scheme.error else scheme.onSurface.copy(alpha = 0.16f), shape).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon && review) Icon(LineIcons.Info, contentDescription = null, Modifier.size(14.dp), tint = ink)
        else if (icon && legal && !review) Icon(LineIcons.Check, contentDescription = null, Modifier.size(14.dp), tint = ink)
        Text(label ?: if (review) "Needs review" else if (legal) "Legal" else "Draft", style = MaterialTheme.typography.labelMedium, color = ink, maxLines = 1, softWrap = false)
    }
}

/** Whole cards or a list, as two small icons on a soft track. On desktop, [small] takes the height of a chip or small icon button. */
@Composable
fun ViewToggle(asCards: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, cardsFirst: Boolean = false, small: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val height = if (!desktopControls) 36.dp else if (small) SmallControlSize else ControlSize
    val inset = if (desktopControls && !small) 4.dp else 3.dp
    val trackShape = if (desktopControls && small) SmallControlShape else LockOnShapes.button
    val thumbShape = if (!desktopControls) RoundedCornerShape(10.dp) else if (small) RoundedCornerShape(7.dp) else InsetControlShape
    Row(modifier.height(height).clip(trackShape).background(if (desktopControls) neutralWash() else scheme.onSurface.copy(alpha = 0.09f)).padding(inset), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(LineIcons.List to false, LineIcons.Grid to true).let { if (cardsFirst) it.reversed() else it }.forEach { (icon, cards) ->
            val on = asCards == cards
            Box(
                Modifier.fillMaxHeight().width(if (desktopControls && small) 30.dp else 36.dp).clip(thumbShape).background(softThumbFill(on))
                    .selectable(on, role = Role.RadioButton) { onChange(cards) }.semantics { contentDescription = if (cards) "Show as cards" else "Show as list" },
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, Modifier.size(18.dp), tint = softThumbTone(on)) }
        }
    }
}

/** A hand-laid thumb's fill, which eases as the choice moves on to or off it. */
@Composable
fun softThumbFill(on: Boolean): Color {
    val thumb = softThumb()
    return animateColorAsState(if (on) thumb else thumb.copy(alpha = 0f), motion(Motion.SHORT), label = "thumb").value
}

/** A hand-laid thumb's label or icon, easing with [softThumbFill]. */
@Composable
fun softThumbTone(on: Boolean): Color =
    animateColorAsState(if (on) softThumbInk() else MaterialTheme.colorScheme.onSurfaceVariant, motion(Motion.SHORT), label = "thumb-ink").value

/** The fill of a solid panel on the sky, the same as the phone's sheets. */
@Composable
fun panelFill(): Color = if (isTwilight()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest

/**
 * A quiet tile on a panel: a soft fill rather than another card with an edge. [solid] is for a tile on
 * the sky, with no panel under it, and takes the colour of the phone's sheets.
 */
@Composable
fun SoftTile(modifier: Modifier = Modifier, solid: Boolean = false, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val fill = if (solid) panelFill() else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    Column(
        modifier.clip(LockOnShapes.tile).background(fill).padding(padding),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content,
    )
}
