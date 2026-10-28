package app.lockon.io

import app.lockon.model.Card
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.Printing

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

    /** More copies than any list needs; a bigger number is a typo. */
    private const val MAX_QUANTITY = 99

    /**
     * Reads a deck list. Lines may name cards by number, by printing ID (`GD01-001_p1`, which keeps
     * that art) or by name, as long as the name belongs to only one card.
     */
    fun parse(
        text: String,
        cards: Map<String, Card>,
        defaultName: String = "Imported deck",
        printings: Map<String, List<Printing>> = emptyMap(),
    ): ParseResult {
        val problems = mutableListOf<String>()
        val byName = cards.values.groupBy { it.name.lowercase() }
        val byPrinting = printings.values.flatten().associateBy { it.printingId.uppercase() }
        var name = defaultName
        var section = DeckSection.MAIN
        val totals = linkedMapOf<Pair<String, DeckSection>, Int>()
        val art = mutableMapOf<Pair<String, DeckSection>, String>()

        for ((index, raw) in text.removePrefix("﻿").lines().withIndex()) {
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
            var printing: Printing? = null
            val (count, cardNumber) = when {
                numbered != null -> {
                    val id = numbered.groupValues[2].uppercase()
                    printing = byPrinting[id]?.takeIf { id !in cards }
                    // An unknown printing still counts toward its card's copy limit.
                    val unprinted = id.substringBefore('_').takeIf { id !in cards && it in cards }
                    numbered.groupValues[1] to (printing?.cardNumber ?: unprinted ?: id)
                }
                else -> {
                    val named = lineWithName.matchEntire(line)
                    val matches = named?.let { byName[it.groupValues[2].trim().lowercase()] }.orEmpty()
                    if (named == null || matches.isEmpty()) {
                        problems += "Line ${index + 1}: could not read \"$line\"."
                        continue
                    }
                    if (matches.size > 1) {
                        problems += "Line ${index + 1}: \"${matches.first().name}\" could be ${matches.joinToString(", ") { it.cardNumber }}. Use the card number."
                        continue
                    }
                    named.groupValues[1] to matches.single().cardNumber
                }
            }
            val quantity = count.toIntOrNull()?.takeIf { it <= MAX_QUANTITY }
            if (quantity == null) {
                problems += "Line ${index + 1}: $count copies is more than a deck can hold."
                continue
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
            if (printing != null) art.getOrPut(key) { printing.printingId }
        }
        val entries = totals.map { (key, quantity) -> DeckEntry(key.first, quantity, key.second, art[key]) }
        return ParseResult(DeckSnapshot(name = name, entries = entries), problems)
    }
}
