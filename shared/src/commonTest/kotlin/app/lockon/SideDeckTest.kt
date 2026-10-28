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
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SideDeckTest {
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
            // Over the copy limit, a third color, a banned card and a resource: all ignored in the side deck.
            DeckEntry("B-01", 3, DeckSection.SIDE) + DeckEntry("G-01", 4, DeckSection.SIDE) +
            DeckEntry("R-01", 1, DeckSection.SIDE) + DeckEntry("RS-01", 2, DeckSection.SIDE),
    )

    @Test fun sideDeckCardsNeverAffectLegality() {
        val result = DeckValidator.validate(deck, cards, rules)
        assertTrue(result.isLegal, result.messages.joinToString { it.text })
    }

    @Test fun sideDeckCardsStayOutOfStatsAndTestHands() {
        val stats = DeckAnalysis.stats(deck, cards)
        assertEquals(setOf(CardColor.BLUE), stats.colorCounts.keys)
        assertEquals(0, stats.burstCount)
        assertEquals(50, stats.typeCounts[CardType.UNIT], "side deck units aren't counted")
        assertEquals(10, stats.typeCounts[CardType.RESOURCE])
        val game = SampleHand.deal(deck)
        assertEquals(50, game.hand.size + game.library.size)
        assertTrue((game.hand + game.library).none { it == "G-01" || it == "R-01" })
    }

    @Test fun moveShiftsCopiesAndKeepsThePrinting() {
        val start = DeckSnapshot("D", entries = listOf(DeckEntry("B-01", 4, printingId = "B-01-ALT")))
        val parked = start.move("B-01", DeckSection.MAIN, DeckSection.SIDE)
        assertEquals(3, parked.quantityOf("B-01", DeckSection.MAIN))
        assertEquals(1, parked.quantityOf("B-01", DeckSection.SIDE))
        assertEquals("B-01-ALT", parked.entries.first { it.section == DeckSection.SIDE }.printingId)
        // Moving more than exists moves only what is there.
        val back = parked.move("B-01", DeckSection.SIDE, DeckSection.MAIN, copies = 5)
        assertEquals(4, back.quantityOf("B-01", DeckSection.MAIN))
        assertEquals(0, back.quantityOf("B-01", DeckSection.SIDE))
    }

    @Test fun deckListsRoundTripTheSideDeck() {
        val text = DeckListFormat.export(deck, cards)
        assertTrue("// Side deck (10)" in text)
        val parsed = DeckListFormat.parse(text, cards).snapshot
        assertEquals(deck.entries.toSet(), parsed.entries.toSet())
    }

    @Test fun considerMaybeAndSideboardHeadersMeanTheSideDeck() {
        val parsed = DeckListFormat.parse("4 B-01\nMaybe:\n2 RS-01\n// Sideboard\n1 G-01\n// Considering (1)\n1 B-02", cards).snapshot
        assertEquals(2, parsed.quantityOf("RS-01", DeckSection.SIDE), "a side deck resource stays in the side deck")
        assertEquals(1, parsed.quantityOf("G-01", DeckSection.SIDE))
        assertEquals(1, parsed.quantityOf("B-02", DeckSection.SIDE), "lists exported before the side deck still read back")
        assertEquals(4, parsed.quantityOf("B-01", DeckSection.MAIN))
    }

    @Test fun theSideDeckKeepsItsOldStoredAndSyncedName() {
        assertEquals("CONSIDERING", DeckSection.SIDE.storedName)
        assertEquals(DeckSection.SIDE, DeckSection.fromStored("CONSIDERING"))
        assertEquals(DeckSection.MAIN, DeckSection.fromStored("MAIN"))
        val json = Json.encodeToString(DeckEntry.serializer(), DeckEntry("B-01", 1, DeckSection.SIDE))
        assertTrue("\"CONSIDERING\"" in json, json)
        assertEquals(DeckSection.SIDE, Json.decodeFromString(DeckEntry.serializer(), json).section)
    }

    @Test fun olderDataFormatOnEitherSideStopsSync() {
        fun hello(format: Int) = HelloResponse(deviceId = "d", deviceName = "D", dataFormatVersion = format)
        assertNotNull(SyncProtocol.incompatibility(hello(1)), "an older desktop would drop side deck cards")
        assertNotNull(SyncProtocol.incompatibility(hello(SyncProtocol.DATA_FORMAT_VERSION + 1)))
        assertEquals(null, SyncProtocol.incompatibility(hello(SyncProtocol.DATA_FORMAT_VERSION)))
    }
}
