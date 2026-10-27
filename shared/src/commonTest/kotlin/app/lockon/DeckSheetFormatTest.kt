package app.lockon

import app.lockon.io.DeckSheetFormat
import app.lockon.model.Card
import app.lockon.model.CardColor
import app.lockon.model.CardType
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.Ruleset
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeckSheetFormatTest {
    private val cards = listOf(
        Card("T-001", "Strike <Custom>", CardType.UNIT, listOf(CardColor.BLUE), level = 3, setCode = "T"),
        Card("T-002", "Pilot", CardType.PILOT, listOf(CardColor.RED), level = 1, setCode = "T"),
        Card("T-010", "Resource", CardType.RESOURCE, setCode = "T"),
    ).associateBy { it.cardNumber }

    private val html = DeckSheetFormat.html(
        DeckSnapshot(
            name = "Mine & Yours",
            entries = listOf(
                DeckEntry("T-002", 2), DeckEntry("T-001", 4), DeckEntry("T-010", 10, DeckSection.RESOURCE),
                DeckEntry("T-001", 1, DeckSection.CONSIDERING),
            ),
        ),
        cards, Ruleset("std", "Standard", "EN", ""), date = "26 Oct 2026",
    )

    @Test fun listsPlayedCardsWithTotals() {
        assertTrue("<td class=\"n\">6</td><td colspan=\"4\">Total (a deck has 50)" in html)
        assertTrue("<td class=\"n\">10</td><td colspan=\"4\">Total (a deck has 10)" in html)
        assertTrue(html.indexOf("T-001") < html.indexOf("T-002"), "Units come before Pilots")
        assertTrue("Blue, Red" in html)
    }

    @Test fun escapesNamesAndLeavesOutConsidering() {
        assertTrue("Strike &lt;Custom&gt;" in html)
        assertTrue("Mine &amp; Yours" in html)
        assertFalse("<td class=\"n\">1</td><td class=\"no\">T-001" in html)
    }
}
