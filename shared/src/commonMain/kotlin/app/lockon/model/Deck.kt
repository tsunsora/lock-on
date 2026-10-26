package app.lockon.model

import kotlinx.serialization.Serializable

@Serializable
enum class DeckSection(val label: String) { MAIN("Main deck"), RESOURCE("Resource deck") }

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
