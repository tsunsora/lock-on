package app.lockon.io

import app.lockon.model.Card
import app.lockon.model.CardType
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.Ruleset

/**
 * A printable deck registration sheet as one self-contained web page: blanks for the player and
 * event, then every card with its number and count. Any browser can print it to paper or PDF, so
 * no platform needs its own PDF code.
 */
object DeckSheetFormat {
    fun html(deck: DeckSnapshot, cards: Map<String, Card>, ruleset: Ruleset, date: String): String {
        val colors = deck.entries.filter { it.section == DeckSection.MAIN }
            .flatMap { cards[it.cardNumber]?.colors.orEmpty() }.distinct().sortedBy { it.ordinal }.joinToString(", ") { it.label }
        return buildString {
            append("<!doctype html>\n<html lang=\"en\"><head><meta charset=\"utf-8\">")
            append("<title>${escape(deck.name)} · deck sheet</title>\n<style>$STYLE</style></head><body>\n")
            append("<h1>Deck registration sheet</h1>\n<div class=\"fields\">")
            listOf("Player name" to "", "Player ID" to "", "Event" to "", "Date" to "", "Deck name" to deck.name, "Colors" to colors)
                .forEach { (label, value) -> append("<div class=\"field\"><span>$label</span>${escape(value)}</div>") }
            append("</div>\n")
            section("Main deck", deck.entries.filter { it.section == DeckSection.MAIN }, cards, ruleset.mainDeckSize)
            section("Resource deck", deck.entries.filter { it.section == DeckSection.RESOURCE }, cards, ruleset.resourceDeckSize)
            append("<p class=\"foot\">Made with Lock On on ${escape(date)}. Check which sheet your event asks for.</p>\n</body></html>\n")
        }
    }

    private fun StringBuilder.section(title: String, entries: List<DeckEntry>, cards: Map<String, Card>, size: Int) {
        val typeOrder = CardType.entries
        val sorted = entries.sortedWith(
            compareBy<DeckEntry>({ cards[it.cardNumber]?.type?.let(typeOrder::indexOf) ?: typeOrder.size },
                { cards[it.cardNumber]?.level ?: 99 }, { cards[it.cardNumber]?.cost ?: 99 }, { it.cardNumber }),
        )
        append("<table><caption>$title</caption>")
        append("<thead><tr><th class=\"n\">Qty</th><th class=\"no\">Card no.</th><th>Name</th><th>Type</th><th class=\"n\">Lv</th></tr></thead><tbody>")
        for (entry in sorted) {
            val card = cards[entry.cardNumber]
            append("<tr><td class=\"n\">${entry.quantity}</td><td class=\"no\">${escape(entry.cardNumber)}</td>")
            append("<td>${escape(card?.name ?: "Unknown card")}</td><td>${card?.type?.label.orEmpty()}</td><td class=\"n\">${card?.level ?: ""}</td></tr>")
        }
        append("</tbody><tfoot><tr><td class=\"n\">${entries.sumOf { it.quantity }}</td><td colspan=\"4\">Total (a deck has $size)</td></tr></tfoot></table>\n")
    }

    private fun escape(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private const val STYLE = """
@page { size: A4; margin: 14mm; }
body { font: 10.5pt/1.35 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; color: #000; background: #fff; max-width: 190mm; margin: 0 auto; }
h1 { font-size: 16pt; margin: 0 0 4mm; }
.fields { display: grid; grid-template-columns: 1fr 1fr; gap: 2mm 8mm; margin-bottom: 6mm; }
.field { border-bottom: 1px solid #000; min-height: 9mm; display: flex; align-items: flex-end; gap: 3mm; }
.field span { font-size: 8pt; color: #444; text-transform: uppercase; letter-spacing: .04em; }
table { width: 100%; border-collapse: collapse; margin-bottom: 6mm; break-inside: auto; }
caption { text-align: left; font-weight: 700; font-size: 11pt; padding: 1mm 0 2mm; }
th, td { border: 1px solid #555; padding: 1mm 2mm; text-align: left; }
th { background: #eee; font-size: 8.5pt; }
tr { break-inside: avoid; }
.n { text-align: right; width: 11mm; }
.no { width: 26mm; font-family: ui-monospace, Consolas, monospace; }
tfoot td { font-weight: 700; }
.foot { font-size: 8pt; color: #555; }
"""
}
