package app.lockon.rules

import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot

/** One card whose count differs between two versions of a deck. */
data class CardChange(val cardNumber: String, val section: DeckSection, val before: Int, val after: Int)

/** What changed between two versions of a deck, for version history and comparisons. */
object DeckDiff {
    fun cards(before: DeckSnapshot, after: DeckSnapshot): List<CardChange> =
        (before.entries + after.entries).map { it.cardNumber to it.section }.distinct()
            .map { (number, section) -> CardChange(number, section, before.quantityOf(number, section), after.quantityOf(number, section)) }
            .filter { it.before != it.after }
            .sortedWith(compareBy({ it.section }, { it.cardNumber }))

    /** A short line such as "+3 −1 cards · renamed · 2 matches logged". */
    fun summary(before: DeckSnapshot, after: DeckSnapshot): String {
        val played = cards(before, after).filter { it.section != DeckSection.CONSIDERING }
        val added = played.sumOf { (it.after - it.before).coerceAtLeast(0) }
        val removed = played.sumOf { (it.before - it.after).coerceAtLeast(0) }
        val logged = after.matches.size - before.matches.size
        val parts = listOfNotNull(
            when {
                added > 0 && removed > 0 -> "+$added −$removed cards"
                added > 0 -> "+$added ${if (added == 1) "card" else "cards"}"
                removed > 0 -> "−$removed ${if (removed == 1) "card" else "cards"}"
                else -> null
            },
            "considering changed".takeIf { cards(before, after).any { it.section == DeckSection.CONSIDERING } },
            "renamed".takeIf { before.name != after.name },
            "notes edited".takeIf { before.notes != after.notes },
            "tags changed".takeIf { before.tags != after.tags },
            when {
                before.folder == after.folder -> null
                after.folder == null -> "taken out of its folder"
                else -> "moved to ${after.folder}"
            },
            "new cover".takeIf { before.coverCard != after.coverCard },
            when {
                logged > 0 -> "$logged ${if (logged == 1) "match" else "matches"} logged"
                logged < 0 -> "${-logged} ${if (logged == -1) "match" else "matches"} removed"
                else -> null
            },
        )
        return parts.joinToString(" · ").ifEmpty { "No changes" }
    }
}
