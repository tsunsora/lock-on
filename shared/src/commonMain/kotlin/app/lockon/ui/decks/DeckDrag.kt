package app.lockon.ui.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag as followDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.lockon.model.DeckSummary
import app.lockon.ui.components.CardBack
import app.lockon.ui.components.CardImage
import app.lockon.ui.components.glassFrame
import app.lockon.ui.theme.LockOnShapes
import kotlin.math.roundToInt

/** Where a dragged deck can land: a folder (null is No folder), or New folder. */
sealed interface DeckDropSpot {
    data class Folder(val name: String?) : DeckDropSpot
    data object NewFolder : DeckDropSpot
}

/**
 * Dragging a deck box onto a folder in the rail or the chip row. Positions are in root
 * coordinates, like [app.lockon.ui.cards.CardDrag], since the decks and the folders sit apart.
 */
@Stable
class DeckDrag(private val onDrop: (DeckSummary, DeckDropSpot) -> Unit) {
    var deck by mutableStateOf<DeckSummary?>(null)
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set
    internal val spots = mutableStateMapOf<DeckDropSpot, Rect>()

    /** The spot under a held deck. Derived, so folders recompose only when it changes, not per pointer move. */
    val over: DeckDropSpot? by derivedStateOf {
        if (deck == null) null else spots.entries.firstOrNull { it.value.contains(pointer) }?.key
    }

    internal fun start(deck: DeckSummary, at: Offset) { this.deck = deck; pointer = at }
    internal fun moveTo(at: Offset) { pointer = at }
    internal fun end(drop: Boolean) {
        val held = deck
        val spot = over
        deck = null
        if (drop && held != null && spot != null) onDrop(held, spot)
    }
}

/** Marks a folder a dragged deck can be dropped on. */
fun Modifier.deckDropSpot(drag: DeckDrag, spot: DeckDropSpot): Modifier = composed {
    DisposableEffect(drag, spot) { onDispose { drag.spots.remove(spot) } }
    // Bounds in root are clipped, so a chip scrolled out of its row can't catch a drop.
    onGloballyPositioned { drag.spots[spot] = it.boundsInRoot() }
}

/** Lets a deck box be picked up: a mouse drags straight away, a finger after a long press so the list still scrolls. */
fun Modifier.deckDragSource(deck: DeckSummary, drag: DeckDrag): Modifier = composed {
    val haptics = LocalHapticFeedback.current
    // Kept once rather than written to state on every scrolled frame.
    val placed = remember { arrayOfNulls<LayoutCoordinates>(1) }
    onPlaced { placed[0] = it }.pointerInput(deck, drag) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val held = if (down.type == PointerType.Mouse) awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
            else awaitLongPressOrCancellation(down.id)
            val at = placed[0] ?: return@awaitEachGesture
            if (held == null) return@awaitEachGesture
            if (down.type != PointerType.Mouse) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            drag.start(deck, at.localToRoot(held.position))
            val finished = followDrag(held.id) { change ->
                change.consume()
                placed[0]?.let { drag.moveTo(it.localToRoot(change.position)) }
            }
            drag.end(drop = finished)
        }
    }
}

/** The held deck under the pointer: its cover and name on a small glass plate. Place it over the whole screen. */
@Composable
fun DeckDragGhost(drag: DeckDrag, cover: (DeckSummary) -> DeckCover?, modifier: Modifier = Modifier) {
    val deck = drag.deck ?: return
    val scheme = MaterialTheme.colorScheme
    var origin by remember { mutableStateOf(Offset.Zero) }
    val art = remember(deck) { cover(deck) }
    Box(modifier.onGloballyPositioned { origin = it.positionInRoot() }) {
        Row(
            // The pointer is read during layout, so following it moves the plate without recomposing.
            Modifier.offset { (drag.pointer - origin + Offset(12f, 12f)).let { IntOffset(it.x.roundToInt(), it.y.roundToInt()) } }
                .graphicsLayer { alpha = if (drag.over != null) 0.97f else 0.85f; rotationZ = -2f }
                .widthIn(max = 260.dp)
                .clip(LockOnShapes.button).background(scheme.surfaceContainerHigh).glassFrame(LockOnShapes.button)
                .padding(start = 8.dp, top = 6.dp, bottom = 6.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (art != null) CardImage(art.card, Modifier.width(30.dp), art.printingId) else CardBack(Modifier.width(30.dp))
            Text(deck.snapshot.name, style = MaterialTheme.typography.labelLarge, color = scheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
