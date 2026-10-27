package app.lockon.io

import app.lockon.model.Card
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot

/**
 * Plain-text deck lists people can paste into chat or forums:
 *
 * ```
 * # My Deck
 * // Main deck (50)
 * 4 SMP-001 Vanguard Frame
 * // Resource deck (10)
 * 10 SMP-020 Standard Resource
 * // Side deck (2)
 * 2 SMP-013 Scorched Earth
 * ```
 */
object DeckListFormat {
    data class ParseResult(val snapshot: DeckSnapshot, val problems: List<String>)

    /**
     * The deck as text. With an [order] the cards are sorted and the main deck gets a `// Units (32)`
     * line before each type; [parse] ignores those lines, so the list still reads back. [compact]
     * leaves out names, tags, type lines and the side deck, for a QR code.
     */
    fun export(deck: DeckSnapshot, cards: Map<String, Card>, order: ExportOrder? = null, compact: Boolean = false): String = buildString {
        appendLine("# ${deck.name}")
        if (deck.tags.isNotEmpty() && !compact) appendLine("// Tags: ${deck.tags.joinToString(", ")}")
        for (section in DeckSection.entries) {
            if (compact && section == DeckSection.SIDE) continue
            val entries = deck.entries.filter { it.section == section }
            if (entries.isEmpty()) continue
            if (!compact) appendLine()
            appendLine("// ${section.label} (${entries.sumOf { it.quantity }})")
            for ((label, group) in order?.groups(entries, cards) ?: listOf(null to entries)) {
                if (label != null && !compact && section == DeckSection.MAIN) appendLine("// $label (${group.sumOf { it.quantity }})")
                for (entry in group) {
                    val name = cards[entry.cardNumber]?.name?.takeUnless { compact }
                    appendLine(listOfNotNull("${entry.quantity}", entry.cardNumber, name).joinToString(" "))
                }
            }
        }
    }.trimEnd() + "\n"

    private val lineWithNumber = Regex("""^(\d+)\s*[xX]?\s+([A-Za-z]{1,6}\d*-\d+[A-Za-z0-9_]*)\b.*$""")
    private val lineWithName = Regex("""^(\d+)\s*[xX]?\s+(.+)$""")

    fun parse(text: String, cards: Map<String, Card>, defaultName: String = "Imported deck"): ParseResult {
        val problems = mutableListOf<String>()
        val byName = cards.values.associateBy { it.name.lowercase() }
        var name = defaultName
        var section = DeckSection.MAIN
        val totals = linkedMapOf<Pair<String, DeckSection>, Int>()

        for ((index, raw) in text.lines().withIndex()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#")) {
                line.removePrefix("#").trim().takeIf { it.isNotEmpty() }?.let { name = it }
                continue
            }
            if (line.startsWith("//") || line.endsWith(":")) {
                val header = line.removePrefix("//").removeSuffix(":").trim().lowercase()
                when {
                    header.startsWith("resource") -> section = DeckSection.RESOURCE
                    header.startsWith("side") || header.startsWith("consider") || header.startsWith("maybe") ->
                        section = DeckSection.SIDE
                    header.startsWith("main") || header.startsWith("deck") -> section = DeckSection.MAIN
                }
                continue
            }
            val numbered = lineWithNumber.matchEntire(line)
            val (quantity, cardNumber) = when {
                numbered != null -> numbered.groupValues[1].toInt() to numbered.groupValues[2].uppercase()
                else -> {
                    val named = lineWithName.matchEntire(line)
                    val card = named?.let { byName[it.groupValues[2].trim().lowercase()] }
                    if (named == null || card == null) {
                        problems += "Line ${index + 1}: could not read \"$line\"."
                        continue
                    }
                    named.groupValues[1].toInt() to card.cardNumber
                }
            }
            if (cardNumber !in cards) problems += "Line ${index + 1}: $cardNumber is not in the card catalog."
            // A resource card listed under the main deck header still belongs in the resource deck.
            val target = when {
                section == DeckSection.SIDE -> section
                cards[cardNumber]?.type?.isResource == true -> DeckSection.RESOURCE
                else -> section
            }
            val key = cardNumber to target
            totals[key] = (totals[key] ?: 0) + quantity
        }
        val entries = totals.map { (key, quantity) -> DeckEntry(key.first, quantity, key.second) }
        return ParseResult(DeckSnapshot(name = name, entries = entries), problems)
    }
}
