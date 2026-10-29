package app.lockon.io

import app.lockon.model.Card
import app.lockon.model.CardType
import app.lockon.model.DeckEntry
import app.lockon.model.ReleaseOrder

/**
 * How an exported deck orders its cards. The list and image both use the one chosen in
 * the Export dialog. Every order keeps the types apart: all the Units first, then Pilots, Commands
 * and Bases, each sorted by the chosen key within its group.
 */
enum class ExportOrder(val label: String) {
    LEVEL("Level"),
    COST("Cost"),
    NUMBER("Card number"),
    NAME("Name"),
    COUNT("Copies");

    /** Sorts within a type group. */
    private fun within(cards: Map<String, Card>): Comparator<DeckEntry> {
        fun card(entry: DeckEntry) = cards[entry.cardNumber]
        val byLevel = compareBy<DeckEntry>({ card(it)?.level ?: 99 }, { card(it)?.cost ?: 99 }, { it.cardNumber })
        return when (this) {
            LEVEL -> byLevel
            COST -> compareBy<DeckEntry>({ card(it)?.cost ?: 99 }, { card(it)?.level ?: 99 }, { it.cardNumber })
            NUMBER -> compareBy(ReleaseOrder.comparator) { it.cardNumber }
            NAME -> compareBy<DeckEntry>({ card(it)?.name?.lowercase() ?: "￿" }, { it.cardNumber })
            COUNT -> compareByDescending<DeckEntry> { it.quantity }.then(byLevel)
        }
    }

    /** Type first (Units, Pilots, Commands, Bases, Resources, then unknown cards), then this order. */
    fun comparator(cards: Map<String, Card>): Comparator<DeckEntry> =
        compareBy<DeckEntry> { cards[it.cardNumber]?.type?.let(CardType.entries::indexOf) ?: CardType.entries.size }.then(within(cards))

    /** The entries in this order, grouped under their type's plural name, such as "Units". */
    fun groups(entries: List<DeckEntry>, cards: Map<String, Card>): List<Pair<String, List<DeckEntry>>> =
        entries.sortedWith(comparator(cards)).groupBy { cards[it.cardNumber]?.type }
            .map { (type, group) -> (type?.let { "${it.label}s" } ?: "Unknown cards") to group }
}
