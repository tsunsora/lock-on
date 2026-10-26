package app.lockon

import app.lockon.io.DeckListFormat
import app.lockon.model.Card
import app.lockon.model.CardColor
import app.lockon.model.CardType
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.Ruleset
import app.lockon.rules.DeckAnalysis
import app.lockon.rules.DeckValidator
import app.lockon.rules.SampleHand
import app.lockon.sync.HelloResponse
import app.lockon.sync.SyncProtocol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConsideringTest {
    private val blue = (1..13).map { Card("B-%02d".format(it), "Blue $it", CardType.UNIT, listOf(CardColor.BLUE), level = 2, setCode = "T") }
    private val green = Card("G-01", "Green", CardType.UNIT, listOf(CardColor.GREEN), level = 3, text = "【Burst】 Draw.", setCode = "T")
    private val red = Card("R-01", "Red", CardType.UNIT, listOf(CardColor.RED), setCode = "T")
    private val resource = Card("RS-01", "Resource", CardType.RESOURCE, setCode = "T")
    private val cards = (blue + green + red + resource).associateBy { it.cardNumber }
    private val rules = Ruleset("std", "Standard", "EN", "", banned = listOf("R-01"))

    private val deck = DeckSnapshot(
        name = "Mono blue",
        entries = blue.take(12).map { DeckEntry(it.cardNumber, 4) } + DeckEntry("B-13", 2) +
            DeckEntry("RS-01", 10, DeckSection.RESOURCE) +
            // Over the copy limit, a third color, a banned card and a resource: all ignored while considered.
            DeckEntry("B-01", 3, DeckSection.CONSIDERING) + DeckEntry("G-01", 4, DeckSection.CONSIDERING) +
            DeckEntry("R-01", 1, DeckSection.CONSIDERING) + DeckEntry("RS-01", 2, DeckSection.CONSIDERING),
    )

    @Test fun consideredCardsNeverAffectLegality() {
        val result = DeckValidator.validate(deck, cards, rules)
        assertTrue(result.isLegal, result.messages.joinToString { it.text })
    }

    @Test fun consideredCardsStayOutOfStatsAndTestHands() {
        val stats = DeckAnalysis.stats(deck, cards)
        assertEquals(setOf(CardColor.BLUE), stats.colorCounts.keys)
        assertEquals(0, stats.burstCount)
        val game = SampleHand.deal(deck)
        assertEquals(50, game.hand.size + game.library.size)
        assertTrue((game.hand + game.library).none { it == "G-01" || it == "R-01" })
    }

    @Test fun moveShiftsCopiesAndKeepsThePrinting() {
        val start = DeckSnapshot("D", entries = listOf(DeckEntry("B-01", 4, printingId = "B-01-ALT")))
        val parked = start.move("B-01", DeckSection.MAIN, DeckSection.CONSIDERING)
        assertEquals(3, parked.quantityOf("B-01", DeckSection.MAIN))
        assertEquals(1, parked.quantityOf("B-01", DeckSection.CONSIDERING))
        assertEquals("B-01-ALT", parked.entries.first { it.section == DeckSection.CONSIDERING }.printingId)
        // Moving more than exists moves only what is there.
        val back = parked.move("B-01", DeckSection.CONSIDERING, DeckSection.MAIN, copies = 5)
        assertEquals(4, back.quantityOf("B-01", DeckSection.MAIN))
        assertEquals(0, back.quantityOf("B-01", DeckSection.CONSIDERING))
    }

    @Test fun deckListsRoundTripTheConsideringSection() {
        val text = DeckListFormat.export(deck, cards)
        assertTrue("// Considering (10)" in text)
        val parsed = DeckListFormat.parse(text, cards).snapshot
        assertEquals(deck.entries.toSet(), parsed.entries.toSet())
    }

    @Test fun maybeAndSideboardHeadersMeanConsidering() {
        val parsed = DeckListFormat.parse("4 B-01\nMaybe:\n2 RS-01\n// Sideboard\n1 G-01", cards).snapshot
        assertEquals(2, parsed.quantityOf("RS-01", DeckSection.CONSIDERING), "a considered resource stays considered")
        assertEquals(1, parsed.quantityOf("G-01", DeckSection.CONSIDERING))
        assertEquals(4, parsed.quantityOf("B-01", DeckSection.MAIN))
    }

    @Test fun olderDataFormatOnEitherSideStopsSync() {
        fun hello(format: Int) = HelloResponse(deviceId = "d", deviceName = "D", dataFormatVersion = format)
        assertNotNull(SyncProtocol.incompatibility(hello(1)), "an older desktop would drop considered cards")
        assertNotNull(SyncProtocol.incompatibility(hello(SyncProtocol.DATA_FORMAT_VERSION + 1)))
        assertEquals(null, SyncProtocol.incompatibility(hello(SyncProtocol.DATA_FORMAT_VERSION)))
    }
}
