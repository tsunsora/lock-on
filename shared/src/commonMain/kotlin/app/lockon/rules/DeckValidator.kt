package app.lockon.rules

import app.lockon.model.Card
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.Ruleset

enum class Severity { ERROR, WARNING }

data class LegalityMessage(val severity: Severity, val text: String, val cardNumber: String? = null)

data class DeckLegality(val messages: List<LegalityMessage>) {
    val errors: List<LegalityMessage> get() = messages.filter { it.severity == Severity.ERROR }
    val isLegal: Boolean get() = errors.isEmpty()
}

/**
 * Checks a deck against a ruleset. Problems are reported, never enforced, so an incomplete deck
 * still saves as a draft.
 */
object DeckValidator {
    fun validate(deck: DeckSnapshot, cards: Map<String, Card>, ruleset: Ruleset): DeckLegality {
        val messages = mutableListOf<LegalityMessage>()
        fun error(text: String, card: String? = null) = messages.add(LegalityMessage(Severity.ERROR, text, card))
        fun warn(text: String, card: String? = null) = messages.add(LegalityMessage(Severity.WARNING, text, card))
        fun nameOf(number: String) = cards[number]?.let { "${it.name} ($number)" } ?: number

        val main = deck.count(DeckSection.MAIN)
        val resource = deck.count(DeckSection.RESOURCE)
        if (main != ruleset.mainDeckSize) error("Main deck has $main of ${ruleset.mainDeckSize} cards.")
        if (resource != ruleset.resourceDeckSize) error("Resource deck has $resource of ${ruleset.resourceDeckSize} cards.")

        // Copies are counted by card number, so alternate art never adds extra copies.
        val totals = deck.entries.groupBy { it.cardNumber to it.section }.mapValues { (_, e) -> e.sumOf { it.quantity } }
        for ((key, quantity) in totals) {
            val (number, section) = key
            val card = cards[number]
            if (card == null) {
                warn("${nameOf(number)} is not in the card catalog, so it cannot be fully checked.", number)
                continue
            }
            if (section == DeckSection.MAIN && card.type.isResource) {
                error("${nameOf(number)} is a resource card and belongs in the resource deck.", number)
            }
            if (section == DeckSection.RESOURCE && !card.type.isResource) {
                error("${nameOf(number)} is not a resource card and cannot be in the resource deck.", number)
            }
            val limit = when {
                number in ruleset.restricted -> ruleset.restricted.getValue(number)
                card.type.isResource -> ruleset.resourceMaxCopies
                else -> ruleset.maxCopies
            }
            if (limit != null && quantity > limit) error("${nameOf(number)} has $quantity copies; the limit is $limit.", number)
        }

        val present = deck.entries.filter { it.quantity > 0 }.map { it.cardNumber }.toSet()
        for (number in present.intersect(ruleset.banned.toSet())) error("${nameOf(number)} is banned.", number)
        for (combo in ruleset.bannedCombinations) {
            if (combo.cardNumbers.isNotEmpty() && combo.cardNumbers.all { it in present }) {
                val names = combo.cardNumbers.joinToString(" and ") { nameOf(it) }
                error("$names cannot be in the same deck." + if (combo.reason.isNotBlank()) " ${combo.reason}" else "")
            }
        }

        val colors = deck.entries.filter { it.section == DeckSection.MAIN }
            .flatMap { cards[it.cardNumber]?.colors.orEmpty() }.toSet()
        if (colors.size > ruleset.maxColors) {
            error("Deck uses ${colors.size} colors (${colors.joinToString { it.label }}); the limit is ${ruleset.maxColors}.")
        }
        return DeckLegality(messages)
    }
}
