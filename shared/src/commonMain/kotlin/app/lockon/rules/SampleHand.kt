package app.lockon.rules

import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import kotlin.random.Random

/**
 * Opening-hand practice that follows the official setup: draw 5, optionally redraw once (the hand
 * goes to the bottom of the deck and 5 new cards are drawn), then the top 6 cards become shields.
 * Later draws come from what is left.
 */
class SampleHand private constructor(
    val hand: List<String>,
    val shields: List<String>,
    /** Remaining main deck, top first. */
    val library: List<String>,
    val redrawUsed: Boolean,
    val shieldsPlaced: Boolean,
    val drawsTaken: Int,
    private val random: Random,
) {
    val canRedraw: Boolean get() = !redrawUsed && !shieldsPlaced
    val canDraw: Boolean get() = library.isNotEmpty()

    /** Sends the hand to the bottom and draws 5 new cards. Allowed once, before shields are set. */
    fun redraw(): SampleHand {
        if (!canRedraw) return this
        val reordered = library + hand
        return SampleHand(reordered.take(HAND_SIZE), emptyList(), reordered.drop(HAND_SIZE), true, false, 0, random)
    }

    /** Keeps the hand: sets 6 shields from the top. Drawing does this automatically. */
    fun keep(): SampleHand {
        if (shieldsPlaced) return this
        return SampleHand(hand, library.take(SHIELDS), library.drop(SHIELDS), redrawUsed, true, drawsTaken, random)
    }

    fun draw(): SampleHand {
        val kept = keep()
        if (!kept.canDraw) return kept
        return SampleHand(kept.hand + kept.library.first(), kept.shields, kept.library.drop(1), kept.redrawUsed, true, kept.drawsTaken + 1, random)
    }

    companion object {
        const val HAND_SIZE = 5
        const val SHIELDS = 6

        fun deal(deck: DeckSnapshot, random: Random = Random.Default): SampleHand {
            val cards = deck.entries.filter { it.section == DeckSection.MAIN }
                .flatMap { entry -> List(entry.quantity) { entry.cardNumber } }
                .shuffled(random)
            return SampleHand(cards.take(HAND_SIZE), emptyList(), cards.drop(HAND_SIZE), false, false, 0, random)
        }
    }
}
