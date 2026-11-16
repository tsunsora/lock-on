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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
fun StateTag(legal: Boolean, review: Boolean = false, modifier: Modifier = Modifier, label: String? = null, icon: Boolean = true, checkShown: () -> Float = { 1f }) {
    val scheme = MaterialTheme.colorScheme
    val ink = if (review) scheme.error else scheme.onSurface
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier.clip(shape).background(if (review) scheme.error.copy(alpha = 0.1f) else scheme.onSurface.copy(alpha = 0.06f)).border(1.dp, if (review) scheme.error else scheme.onSurface.copy(alpha = 0.16f), shape).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon && review) Icon(LineIcons.Info, contentDescription = null, Modifier.size(14.dp), tint = ink)
        else if (icon && legal && !review) Icon(LineIcons.Check, contentDescription = null, tint = ink,
            // The check can draw itself in from the left, read while drawing so it costs no recomposition.
            modifier = Modifier.size(14.dp).drawWithContent { clipRect(right = size.width * checkShown()) { this@drawWithContent.drawContent() } })
        Text(label ?: if (review) "Needs review" else if (legal) "Legal" else "Draft", style = MaterialTheme.typography.labelMedium, color = ink, maxLines = 1, softWrap = false)
    }
}

/**
 * The deck editor's state tag, and the Lock On moment (UX rule V5). When [legal] turns true the tag
 * turns over to Legal, its check draws in, and its edge and fill warm to gold for a moment, then
 * settle back to the quiet tag. Only this small tag lights, beside the gauge that has just turned
 * gold, so the change is clear without anything sweeping across the screen. It plays only on the
 * change, never when a legal deck opens; with reduced motion the tag simply changes.
 */
@Composable
fun LegalStamp(legal: Boolean, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val warmth = remember { Animatable(0f) }
    val check = remember { Animatable(1f) }
    var last by remember { mutableStateOf(legal) }
    LaunchedEffect(legal) {
        val became = legal && !last
        last = legal
        if (!became || reduce) { warmth.snapTo(0f); check.snapTo(1f); return@LaunchedEffect }
        check.snapTo(0f)
        launch { delay(Motion.SHORT.toLong()); check.animateTo(1f, tween(Motion.LONG, easing = Motion.emphasizedDecelerate)) }
        warmth.animateTo(1f, tween(Motion.MEDIUM, easing = Motion.emphasizedDecelerate))
        delay(Motion.SIGNATURE.toLong())
        warmth.animateTo(0f, tween(Motion.SIGNATURE, easing = Motion.emphasized))
    }
    // The gold that carries text on each theme, the same as the full gauge's number.
    val gold = if (isTwilight()) Gold else MaterialTheme.colorScheme.tertiary
    val shape = RoundedCornerShape(8.dp)
    // Fades through rather than across, so Draft and Legal never show on top of each other, while
    // the tag's shape grows to fit the new word.
    AnimatedContent(legal, modifier.drawWithContent {
        val w = warmth.value
        if (w > 0f) {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(outline, gold.copy(alpha = 0.16f * w))
            drawContent()
            drawOutline(outline, gold.copy(alpha = w), style = Stroke(1.5.dp.toPx()))
        } else drawContent()
    }, transitionSpec = {
        if (reduce) fadeIn(snap()) togetherWith fadeOut(snap()) using SizeTransform(clip = false) { _, _ -> snap() }
        else fadeIn(tween(Motion.MEDIUM, Motion.SHORT)) togetherWith fadeOut(tween(Motion.SHORT)) using
            SizeTransform(clip = false) { _, _ -> tween(Motion.LONG, easing = Motion.emphasized) }
    }, label = "legality") { now ->
        StateTag(now, checkShown = { check.value })
    }
}

/**
 * A deck's state as one quiet line of text, no capsule (UX rules I2, I4): "✓ Legal · 3–1 · 75%" or
 * "Draft". A deck that needs review or is over the limit says so in the error colour, in words. The record
 * and other facts follow in [extras], split by muted dots.
 */
@Composable
fun DeckStatusLine(legal: Boolean, modifier: Modifier = Modifier, review: Boolean = false, over: Int = 0, extras: List<String> = emptyList()) {
    val scheme = MaterialTheme.colorScheme
    val problem = review || over > 0
    val ink = when { problem -> scheme.error; legal -> scheme.onSurface; else -> scheme.onSurfaceVariant }
    val style = MaterialTheme.typography.labelMedium
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (legal && !problem) Icon(LineIcons.Check, contentDescription = null, Modifier.size(14.dp), tint = ink)
        Text(when { review -> "Needs review"; over > 0 -> "$over over"; legal -> "Legal"; else -> "Draft" }, style = style, color = ink, maxLines = 1, softWrap = false)
        extras.forEach {
            Text("·", style = style, color = mutedInk(), maxLines = 1)
            Text(it, style = style, color = scheme.onSurfaceVariant, maxLines = 1, softWrap = false)
        }
    }
}

/**
 * Whole cards or a list, as two small icons on a soft track: cards first and the list second, in every
 * list that has the choice, since cards are the default view. On desktop, [small] takes the height of a
 * chip or small icon button.
 */
@Composable
fun ViewToggle(asCards: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, small: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val height = if (!desktopControls) 36.dp else if (small) SmallControlSize else ControlSize
    val inset = if (desktopControls && !small) 4.dp else 3.dp
    val trackShape = if (desktopControls && small) SmallControlShape else LockOnShapes.button
    val thumbShape = if (!desktopControls) RoundedCornerShape(10.dp) else if (small) RoundedCornerShape(7.dp) else InsetControlShape
    Row(modifier.height(height).clip(trackShape).background(if (desktopControls) neutralWash() else scheme.onSurface.copy(alpha = 0.09f)).padding(inset), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(LineIcons.Grid to true, LineIcons.List to false).forEach { (icon, cards) ->
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
