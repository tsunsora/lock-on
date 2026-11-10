package app.lockon.ui.cards

import androidx.compose.animation.core.animateFloatAsState
import app.lockon.ui.components.Motion
import app.lockon.ui.components.motion
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag as followDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.lockon.model.Card
import app.lockon.ui.components.CardImage
import kotlin.math.roundToInt

/**
 * Dragging a card from the card list onto a drop area, such as the deck panel. Positions are in
 * root coordinates so the source and the drop area can sit in different panels.
 */
@Stable
class CardDrag(private val onDrop: (Card) -> Unit) {
    var card by mutableStateOf<Card?>(null)
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set
    internal var target by mutableStateOf(Rect.Zero)

    /** True while a card is held over the drop area. Derived, so readers update only when it flips, not per pointer move. */
    val overTarget: Boolean by derivedStateOf { card != null && target.contains(pointer) }

    internal fun start(card: Card, at: Offset) { this.card = card; pointer = at }
    internal fun moveTo(at: Offset) { pointer = at }
    internal fun end(drop: Boolean) {
        val held = card
        val over = overTarget
        card = null
        if (drop && over && held != null) onDrop(held)
    }
}

/** Marks where a dragged card can be dropped. */
fun Modifier.cardDropTarget(drag: CardDrag): Modifier = onGloballyPositioned { drag.target = it.boundsInRoot() }

/** Lets this card be picked up: a mouse drags straight away, a finger after a long press so lists still scroll. */
fun Modifier.cardDragSource(card: Card, drag: CardDrag?): Modifier = if (drag == null) this else composed {
    val haptics = LocalHapticFeedback.current
    // Layout coordinates stay live, so they are kept once rather than written to state on every
    // scrolled frame, which a grid full of cards would otherwise do.
    val placed = remember { arrayOfNulls<LayoutCoordinates>(1) }
    onPlaced { placed[0] = it }.pointerInput(card, drag) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val held = if (down.type == PointerType.Mouse) awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
            else awaitLongPressOrCancellation(down.id)
            val at = placed[0] ?: return@awaitEachGesture
            if (held == null) return@awaitEachGesture
            if (down.type != PointerType.Mouse) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            drag.start(card, at.localToRoot(held.position))
            val finished = followDrag(held.id) { change ->
                change.consume()
                placed[0]?.let { drag.moveTo(it.localToRoot(change.position)) }
            }
            drag.end(drop = finished)
        }
    }
}

/** The card under the pointer while it is dragged. Place it over the whole screen. */
@Composable
fun CardDragGhost(drag: CardDrag, modifier: Modifier = Modifier) {
    // Kept while it fades, so a drop lets the card go instead of removing it.
    var last by remember { mutableStateOf(drag.card) }
    drag.card?.let { last = it }
    val appear by animateFloatAsState(if (drag.card != null) 1f else 0f, motion(Motion.SHORT), label = "card-ghost")
    val aimed by animateFloatAsState(if (drag.overTarget) 1f else 0f, motion(Motion.SHORT), label = "card-ghost-aim")
    val card = last
    if (card == null || (drag.card == null && appear == 0f)) return
    var origin by remember { mutableStateOf(Offset.Zero) }
    val width = 112.dp
    val half = with(LocalDensity.current) { Offset(width.toPx() / 2, width.toPx() * 0.7f) }
    Box(modifier.onGloballyPositioned { origin = it.positionInRoot() }) {
        CardImage(
            card,
            // The pointer is read during layout, so following it moves the card without recomposing.
            Modifier.offset { (drag.pointer - origin - half).let { IntOffset(it.x.roundToInt(), it.y.roundToInt()) } }.width(width)
                .graphicsLayer {
                    alpha = appear * (0.8f + 0.15f * aimed); rotationZ = -3f
                    val s = 0.92f + 0.08f * appear
                    scaleX = s; scaleY = s
                },
        )
    }
}
