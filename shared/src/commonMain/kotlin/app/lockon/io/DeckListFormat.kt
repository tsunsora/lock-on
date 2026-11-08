package app.lockon.io

import app.lockon.model.Card
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.Printing
import app.lockon.model.SearchText

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

    /** A card number or printing ID: `GD01-001`, `GD01-001_p1`, or whitebase.dev's `GD01-001-p1`. */
    private const val CARD_ID = """[A-Za-z]{1,6}\d*-\d+[A-Za-z0-9_]*(?:-[pP]\d+)?"""
    private val cardId = Regex(CARD_ID)
    private val lineWithCount = Regex("""^(\d+)\s*[xX×]?\s+(.+)$""")
    private val startsWithId = Regex("""^($CARD_ID)\b.*$""")
    /** Simulator lists write one card number per copy, on lines of their own or all on one. */
    private val idsOnly = Regex("""^$CARD_ID(?:[\s,;]+$CARD_ID)*$""")
    /** DeckPlanet's `Guntank [GD01-008]`, ExBurst's `Guntank (GD01-008)`, Limitless's `Guntank GD01-008`. */
    private val idAfterName = Regex("""[\[(]\s*($CARD_ID)\s*[\])]|\s($CARD_ID)$""")
    /** Section lines written without `//` or a colon, such as `Main Deck (50)` or `=== Side Deck ===`. */
    private val plainHeader = Regex("""^[=\-\s]*(main|main ?deck|deck|resources?|resource ?deck|side|side ?deck|sideboard|maybe|considering)\s*(\(\d+\))?[=\-\s]*$""", RegexOption.IGNORE_CASE)

    /** Whether [text] is written like a card number or printing ID. */
    fun isCardId(text: String) = cardId.matches(text)

    /** `GD01-001-p1` as the catalog writes it, `GD01-001_P1`. */
    private fun normalizeId(id: String) = id.uppercase().replace(Regex("""-P(\d+)$"""), "_P$1")

    /** More copies than any list needs; a bigger number is a typo. */
    private const val MAX_QUANTITY = 99

    /**
     * Lists from elsewhere write "Zaku II" and "Uso Ewin" for the catalog's Zaku Ⅱ and Üso Ewin.
     * Punctuation stays, as GN Armor (Type-E) and GN Armor Type-E are different cards.
     */
    private fun nameKey(name: String) = SearchText.fold(name.trim())

    /**
     * Reads a deck list. Lines may name cards by number, by printing ID (`GD01-001_p1`, which keeps
     * that art) or by name, as long as the name belongs to only one card. That covers the lists deck
     * sites copy out: ExBurst (`4 x GD01-001`), Egman (`4 GD01-001 Gundam | RX-78-2`), Limitless
     * (`4 Gundam GD01-001`), DeckPlanet (`4 Gundam [GD01-001]`) and simulator lists with one card
     * number per copy. Deck links and JSON that [DeckCodes] knows are read too.
     */
    fun parse(
        text: String,
        cards: Map<String, Card>,
        defaultName: String = "Imported deck",
        printings: Map<String, List<Printing>> = emptyMap(),
    ): ParseResult {
        val problems = mutableListOf<String>()
        // Tokens share names with real cards (Zaku Ⅱ), and a list naming one means the card.
        val byName = cards.values.filter { !it.type.isToken }.groupBy { nameKey(it.name) }
        val byPrinting = printings.values.flatten().associateBy { it.printingId.uppercase() }
        var name = defaultName
        var section = DeckSection.MAIN
        val totals = linkedMapOf<Pair<String, DeckSection>, Int>()
        val art = mutableMapOf<Pair<String, DeckSection>, String>()

        /** The card [raw] counts as, and the printing whose art it keeps. */
        fun resolveId(raw: String): Pair<String, Printing?> {
            val id = normalizeId(raw)
            val printing = byPrinting[id]?.takeIf { id !in cards }
            // An unknown printing still counts toward its card's copy limit.
            val unprinted = id.substringBefore('_').takeIf { id !in cards && it in cards }
            return (printing?.cardNumber ?: unprinted ?: id) to printing
        }

        fun add(index: Int, count: String, card: Pair<String, Printing?>) {
            val (cardNumber, printing) = card
            val quantity = count.toIntOrNull()?.takeIf { it <= MAX_QUANTITY }
            if (quantity == null) {
                problems += "Line ${index + 1}: $count copies is more than a deck can hold."
                return
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

        val list = DeckCodes.toDeckList(text) ?: text
        for ((index, raw) in list.removePrefix("﻿").lines().withIndex()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#")) {
                line.removePrefix("#").trim().takeIf { it.isNotEmpty() }?.let { name = it }
                continue
            }
            if (line.startsWith("//") || line.endsWith(":") || plainHeader.matches(line)) {
                val header = line.removePrefix("//").removeSuffix(":").trim('=', '-', ' ').lowercase()
                when {
                    header.startsWith("resource") -> section = DeckSection.RESOURCE
                    header.startsWith("side") || header.startsWith("consider") || header.startsWith("maybe") ->
                        section = DeckSection.SIDE
                    header.startsWith("main") || header.startsWith("deck") -> section = DeckSection.MAIN
                }
                continue
            }
            if (idsOnly.matches(line)) {
                for (id in cardId.findAll(line)) add(index, "1", resolveId(id.value))
                continue
            }
            val counted = lineWithCount.matchEntire(line)
            if (counted == null) {
                problems += "Line ${index + 1}: could not read \"$line\"."
                continue
            }
            val count = counted.groupValues[1]
            val rest = counted.groupValues[2].trim()
            startsWithId.matchEntire(rest)?.let {
                add(index, count, resolveId(it.groupValues[1]))
                continue
            }
            // A number beside the name settles which card it is, even when the name is shared.
            val ids = idAfterName.findAll(rest).map { m -> m.groupValues[1].ifEmpty { m.groupValues[2] } }.map(::resolveId).toList()
            ids.firstOrNull { it.first in cards }?.let {
                add(index, count, it)
                continue
            }
            val matches = byName[nameKey(rest)].orEmpty()
            when {
                matches.size == 1 -> add(index, count, matches.single().cardNumber to null)
                ids.isNotEmpty() -> add(index, count, ids.first())
                matches.size > 1 ->
                    problems += "Line ${index + 1}: \"${matches.first().name}\" could be ${matches.joinToString(", ") { it.cardNumber }}. Use the card number."
                else -> problems += "Line ${index + 1}: could not read \"$line\"."
            }
        }
        val entries = totals.map { (key, quantity) -> DeckEntry(key.first, quantity, key.second, art[key]) }
        return ParseResult(DeckSnapshot(name = name, entries = entries), problems)
    }
}
