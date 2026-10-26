package app.lockon.model

import kotlinx.serialization.Serializable

@Serializable
enum class DeckSection(val label: String) {
    MAIN("Main deck"),
    RESOURCE("Resource deck"),
    /** Cards someone is weighing up. Never counted toward deck size, colors or legality. */
    CONSIDERING("Considering");

    companion object {
        /** Where a card is played: resources in the resource deck, everything else in the main deck. */
        fun playedIn(card: Card) = if (card.type.isResource) RESOURCE else MAIN
    }
}

@Serializable
data class DeckEntry(
    val cardNumber: String,
    val quantity: Int,
    val section: DeckSection = DeckSection.MAIN,
    /** Preferred artwork. Copy limits ignore this and count by [cardNumber]. */
    val printingId: String? = null,
)

/**
 * The full content of one deck at one revision. Revisions store snapshots rather than diffs so a
 * device can always rebuild a deck from a single record, whatever it missed in between.
 */
@Serializable
data class DeckSnapshot(
    val name: String,
    val notes: String = "",
    val format: String = DEFAULT_FORMAT,
    val tags: List<String> = emptyList(),
    val entries: List<DeckEntry> = emptyList(),
    /** True for the synced deletion record that keeps an older device from restoring the deck. */
    val deleted: Boolean = false,
) {
    fun count(section: DeckSection): Int = entries.filter { it.section == section }.sumOf { it.quantity }

    fun quantityOf(cardNumber: String, section: DeckSection): Int =
        entries.filter { it.cardNumber == cardNumber && it.section == section }.sumOf { it.quantity }

    /** Sets the total for a card in a section, keeping its preferred printing. Zero removes it. */
    fun withQuantity(cardNumber: String, section: DeckSection, quantity: Int, printingId: String? = null): DeckSnapshot {
        val existing = entries.firstOrNull { it.cardNumber == cardNumber && it.section == section }
        val others = entries.filterNot { it.cardNumber == cardNumber && it.section == section }
        val updated = if (quantity <= 0) others else {
            others + DeckEntry(cardNumber, quantity, section, printingId ?: existing?.printingId)
        }
        return copy(entries = updated.sortedWith(compareBy({ it.section }, { it.cardNumber })))
    }

    /** Moves up to [copies] of a card between sections, keeping its preferred printing. */
    fun move(cardNumber: String, from: DeckSection, to: DeckSection, copies: Int = 1): DeckSnapshot {
        val available = quantityOf(cardNumber, from)
        val moving = copies.coerceAtMost(available)
        if (moving <= 0 || from == to) return this
        val printing = entries.firstOrNull { it.cardNumber == cardNumber && it.section == from }?.printingId
        return withQuantity(cardNumber, from, available - moving)
            .withQuantity(cardNumber, to, quantityOf(cardNumber, to) + moving, printing)
    }

    companion object {
        const val DEFAULT_FORMAT = "standard"
    }
}

/** A deck as the library shows it: the current snapshot plus its sync state. */
data class DeckSummary(
    val deckId: String,
    val snapshot: DeckSnapshot,
    val headRevisionId: String,
    val hasConflict: Boolean,
    val updatedAt: Long,
)

/** One side of a conflict: a head revision that no other revision has superseded. */
data class DeckVersion(
    val revisionId: String,
    val originDeviceId: String,
    val createdAt: Long,
    val snapshot: DeckSnapshot,
)
