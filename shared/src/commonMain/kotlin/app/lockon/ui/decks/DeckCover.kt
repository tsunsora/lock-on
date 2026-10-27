package app.lockon.ui.decks

import app.lockon.model.Card
import app.lockon.model.CardType
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot

/** A deck's cover: a card plus the printing to show it in. */
data class DeckCover(val card: Card, val printingId: String?)

/**
 * The card a deck is recognised by: the cover someone picked, or else its highest-level Unit.
 * A picked cover that has left the catalog falls back the same way.
 */
fun deckCover(deck: DeckSnapshot, cards: Map<String, Card>): DeckCover? {
    deck.coverCard?.let { number ->
        val card = cards[number] ?: return@let
        return DeckCover(card, deck.entries.firstOrNull { it.cardNumber == number && it.printingId != null }?.printingId)
    }
    val entry = deck.entries.filter { it.section == DeckSection.MAIN && it.cardNumber in cards }
        .sortedWith(
            compareByDescending<DeckEntry> { cards.getValue(it.cardNumber).type == CardType.UNIT }
                .thenByDescending { cards.getValue(it.cardNumber).level ?: 0 }
                .thenBy { it.cardNumber },
        )
        .firstOrNull() ?: return null
    return DeckCover(cards.getValue(entry.cardNumber), entry.printingId)
}
