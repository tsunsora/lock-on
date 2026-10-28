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
    /** Copies with a 【Burst】 effect, which can trigger when they are taken from shields. */
    val burstCount: Int,
    val blockerCount: Int,
    /** Units whose link condition a pilot in this deck satisfies, with those pilots. */
    val linkPairs: List<Pair<Card, List<Card>>>,
)

object DeckAnalysis {
    private val traitLink = Regex("""trait\s*\(([^)]+)\)""", RegexOption.IGNORE_CASE)

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
            // The side deck isn't played, so it stays out of the deck's make-up.
            if (entry.section == DeckSection.SIDE) continue
            val card = cards[entry.cardNumber] ?: continue
            types[card.type] = (types[card.type] ?: 0) + entry.quantity
            if (entry.section == DeckSection.MAIN) {
                card.colors.forEach { colors[it] = (colors[it] ?: 0) + entry.quantity }
            }
        }
        fun copiesWith(keyword: String) = main.sumOf { entry ->
            if (cards[entry.cardNumber]?.displayText?.contains(keyword, ignoreCase = true) == true) entry.quantity else 0
        }
        val mainCards = main.mapNotNull { cards[it.cardNumber] }.distinctBy { it.cardNumber }
        return DeckStats(
            levelCurve = curve { it.level },
            costCurve = curve { it.cost },
            typeCounts = types,
            colorCounts = colors,
            mainCount = deck.count(DeckSection.MAIN),
            resourceCount = deck.count(DeckSection.RESOURCE),
            burstCount = copiesWith("Burst】"),
            blockerCount = copiesWith("<Blocker>"),
            linkPairs = linkPairs(mainCards),
        )
    }

    /** Matches a unit's link text against pilots by name, or by trait for "Pilot with trait (X)". */
    fun linkPairs(deckCards: List<Card>): List<Pair<Card, List<Card>>> {
        val pilots = deckCards.filter { it.type == CardType.PILOT }
        return deckCards.filter { it.type == CardType.UNIT && !it.link.isNullOrBlank() }.mapNotNull { unit ->
            val link = unit.link.orEmpty()
            val trait = traitLink.find(link)?.groupValues?.get(1)?.trim()
            val partners = pilots.filter { pilot ->
                if (trait != null) pilot.traits.any { it.equals(trait, ignoreCase = true) }
                else link.contains(pilot.name, ignoreCase = true)
            }
            if (partners.isEmpty()) null else unit to partners
        }
    }
}
