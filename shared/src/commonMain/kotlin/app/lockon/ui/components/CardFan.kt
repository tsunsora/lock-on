package app.lockon.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lockon.model.Card

/**
 * Up to three cards held in a fan: the first in the middle, the others tilted out to either side.
 * A slot with no card holds a card back, so an empty deck still reads as a deck.
 *
 * @param spread how far each side card sits from the middle, as a share of a card's width.
 * @param sideDrop how far the side cards sit below the middle one; negative raises them.
 * @param middleOnTop draws the middle card over both others, rather than dealing left to right.
 * @param elevation the shadow under each card with a face, to lift it off a busy background.
 */
@Composable
fun CardFan(
    cards: List<Pair<Card, String?>>,
    cardWidth: Dp,
    modifier: Modifier = Modifier,
    spread: Float = 0.75f,
    tilt: Float = 14f,
    sideDrop: Dp = 16.dp,
    middleOnTop: Boolean = true,
    elevation: Dp = 3.dp,
) {
    val rise = if (sideDrop < 0.dp) -sideDrop else 0.dp
    val drop = if (sideDrop > 0.dp) sideDrop else 0.dp
    Box(modifier.size(cardWidth * (1f + 2f * spread), cardWidth / CARD_ASPECT + rise + drop)) {
        // Slot -1 is left, 0 the middle and 1 right; the list's first card takes the middle.
        (if (middleOnTop) listOf(-1, 1, 0) else listOf(-1, 0, 1)).forEach { slot ->
            val entry = cards.getOrNull(if (slot == 0) 0 else if (slot < 0) 1 else 2)
            val place = Modifier.width(cardWidth)
                .offset(cardWidth * spread * (1f + slot), if (slot == 0) rise else rise + sideDrop)
                .graphicsLayer { rotationZ = tilt * slot }
            if (entry != null) CardImage(entry.first, place.shadow(elevation, CardShape), entry.second, shadow = false) else CardBack(place)
        }
    }
}
