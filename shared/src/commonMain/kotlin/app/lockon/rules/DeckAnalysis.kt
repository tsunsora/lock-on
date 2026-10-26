package app.lockon.rules

import app.lockon.model.Card
import app.lockon.model.CardColor
import app.lockon.model.CardType
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot

data class DeckStats(
    /** Main-deck copies by level, including empty levels between the lowest and highest. */
    val levelCurve: List<Pair<Int, Int>>,
    val costCurve: List<Pair<Int, Int>>,
    val typeCounts: Map<CardType, Int>,
    val colorCounts: Map<CardColor, Int>,
    val mainCount: Int,
    val resourceCount: Int,
)

object DeckAnalysis {
    fun stats(deck: DeckSnapshot, cards: Map<String, Card>): DeckStats {
        val main = deck.entries.filter { it.section == DeckSection.MAIN }
        fun curve(selector: (Card) -> Int?): List<Pair<Int, Int>> {
            val counts = mutableMapOf<Int, Int>()
            for (entry in main) {
                val value = cards[entry.cardNumber]?.let(selector) ?: continue
                counts[value] = (counts[value] ?: 0) + entry.quantity
            }
            if (counts.isEmpty()) return emptyList()
            val range = counts.keys.min()..counts.keys.max()
            return range.map { it to (counts[it] ?: 0) }
        }
        val types = mutableMapOf<CardType, Int>()
        val colors = mutableMapOf<CardColor, Int>()
        for (entry in deck.entries) {
            val card = cards[entry.cardNumber] ?: continue
            types[card.type] = (types[card.type] ?: 0) + entry.quantity
            if (entry.section == DeckSection.MAIN) {
                card.colors.forEach { colors[it] = (colors[it] ?: 0) + entry.quantity }
            }
        }
        return DeckStats(
            levelCurve = curve { it.level },
            costCurve = curve { it.cost },
            typeCounts = types,
            colorCounts = colors,
            mainCount = deck.count(DeckSection.MAIN),
            resourceCount = deck.count(DeckSection.RESOURCE),
        )
    }
}
