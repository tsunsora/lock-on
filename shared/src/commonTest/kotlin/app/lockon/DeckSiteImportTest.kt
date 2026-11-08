package app.lockon

import app.lockon.io.DeckCodes
import app.lockon.io.DeckListFormat
import app.lockon.model.Card
import app.lockon.model.CardColor
import app.lockon.model.CardType
import app.lockon.model.DeckSection
import app.lockon.model.Printing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Lists and codes as the Gundam Card Game deck sites export them. */
class DeckSiteImportTest {
    private val gundam = Card("GD01-001", "Gundam", CardType.UNIT, listOf(CardColor.BLUE), setCode = "GD01")
    private val zakuA = Card("GD01-010", "Zaku II", CardType.UNIT, listOf(CardColor.GREEN), setCode = "GD01")
    private val zakuB = Card("ST03-005", "Zaku II", CardType.UNIT, listOf(CardColor.GREEN), setCode = "ST03")
    private val maForm = Card("ST01-002", "Gundam (MA Form)", CardType.UNIT, listOf(CardColor.BLUE), setCode = "ST01")
    private val cards = listOf(gundam, zakuA, zakuB, maForm).associateBy { it.cardNumber }
    private val printings = mapOf(
        "GD01-001" to listOf(
            Printing("GD01-001", "GD01-001", "GD01", "LR"),
            Printing("GD01-001_p1", "GD01-001", "GD01", "LR", isAlternateArt = true),
        ),
    )

    private fun read(text: String) = DeckListFormat.parse(text, cards, printings = printings)

    private fun assertClean(result: DeckListFormat.ParseResult) = assertTrue(result.problems.isEmpty(), result.problems.toString())

    @Test fun deckPlanetPutsTheNumberInBrackets() {
        val result = read("4 Gundam (MA Form) [ST01-002]\n3 Zaku II [ST03-005]")
        assertClean(result)
        assertEquals(4, result.snapshot.quantityOf("ST01-002", DeckSection.MAIN))
        assertEquals(3, result.snapshot.quantityOf("ST03-005", DeckSection.MAIN))
    }

    @Test fun limitlessPutsTheNumberAfterTheName() {
        val result = read("// Main Deck\n4 Zaku Ⅱ GD01-010\n2 Zaku II ST03-005\n\n// Side Deck\n1 Gundam GD01-001")
        assertClean(result)
        assertEquals(4, result.snapshot.quantityOf("GD01-010", DeckSection.MAIN))
        assertEquals(2, result.snapshot.quantityOf("ST03-005", DeckSection.MAIN))
        assertEquals(1, result.snapshot.quantityOf("GD01-001", DeckSection.SIDE))
    }

    @Test fun exBurstWritesCountTimesNumberAndSometimesTheNumberInParentheses() {
        val result = read("4 x GD01-001\n4x GD01-010\n2 Zaku II (ST03-005)\n4 x ST01-002 Gundam (MA Form)")
        assertClean(result)
        assertEquals(4, result.snapshot.quantityOf("GD01-001", DeckSection.MAIN))
        assertEquals(4, result.snapshot.quantityOf("GD01-010", DeckSection.MAIN))
        assertEquals(2, result.snapshot.quantityOf("ST03-005", DeckSection.MAIN))
        assertEquals(4, result.snapshot.quantityOf("ST01-002", DeckSection.MAIN))
    }

    @Test fun egmanPutsTheModelNumberAfterABar() {
        val result = read("4 GD01-010 Zaku II | MS-06\n3 GD01-001 Gundam | RX-78-2")
        assertClean(result)
        assertEquals(4, result.snapshot.quantityOf("GD01-010", DeckSection.MAIN))
        assertEquals(3, result.snapshot.quantityOf("GD01-001", DeckSection.MAIN))
    }

    @Test fun simulatorListsWriteOneNumberPerCopy() {
        val lines = read("1 GD01-001\n1 GD01-010\n1 GD01-001")
        assertClean(lines)
        assertEquals(2, lines.snapshot.quantityOf("GD01-001", DeckSection.MAIN))
        val bare = read("GD01-001 GD01-001 GD01-010\nGD01-001")
        assertClean(bare)
        assertEquals(3, bare.snapshot.quantityOf("GD01-001", DeckSection.MAIN))
        assertEquals(1, bare.snapshot.quantityOf("GD01-010", DeckSection.MAIN))
        val array = read("""["GD01-001","GD01-001","ST03-005"]""")
        assertClean(array)
        assertEquals(2, array.snapshot.quantityOf("GD01-001", DeckSection.MAIN))
    }

    @Test fun headersWithoutSlashesOrColonsStillSwitchSections() {
        val result = read("Main Deck (4)\n4 GD01-001\n=== Side Deck ===\n2 GD01-010")
        assertClean(result)
        assertEquals(4, result.snapshot.quantityOf("GD01-001", DeckSection.MAIN))
        assertEquals(2, result.snapshot.quantityOf("GD01-010", DeckSection.SIDE))
    }

    @Test fun exBurstJsonKeepsTheSideDeck() {
        val result = read("""{"slots": [["GD01-001", {"count": 4}], ["GD01-010", {"count": 3}]], "sideboardSlots": [["ST03-005", {"count": 2}]]}""")
        assertClean(result)
        assertEquals(4, result.snapshot.quantityOf("GD01-001", DeckSection.MAIN))
        assertEquals(3, result.snapshot.quantityOf("GD01-010", DeckSection.MAIN))
        assertEquals(2, result.snapshot.quantityOf("ST03-005", DeckSection.SIDE))
    }

    @Test fun simulatorJsonCanBeBase64() {
        val code = "eyJkZWNrTmFtZSI6IldpbmcgLSBDcmVhdGVkIG9uIEV4QnVyc3QuREVWIiwiZGVjayI6W3siaWQiOiJHRDAxLTAwMSIsInF0eSI6NH0seyJpZCI6IkdEMDEtMDEwIiwicXR5IjoyfV19"
        val result = read(code)
        assertClean(result)
        assertEquals("Wing - Created on ExBurst.DEV", result.snapshot.name)
        assertEquals(4, result.snapshot.quantityOf("GD01-001", DeckSection.MAIN))
        assertEquals(2, result.snapshot.quantityOf("GD01-010", DeckSection.MAIN))
    }

    @Test fun whitebaseLinksKeepTheNameAndTheParallelArt() {
        // {"g":"GCG","v":2,"n":"Test","d":{"deck":{"GD01-001":4,"GD01-001-p1":2,"ST03-005":3}}}
        val code = "N4Ig5iBcIOIMIxAGhANygJhQOyiAKgKYDOALsiACZSiWEDGA1jbACIAMAjALTtdQAWFDA48+PAA6dMKAMr52AZl7sArFEUBfbUA"
        for (pasted in listOf("https://www.whitebase.dev/db/deckview?$code", code)) {
            val result = read(pasted)
            assertClean(result)
            assertEquals("Test", result.snapshot.name)
            assertEquals(6, result.snapshot.quantityOf("GD01-001", DeckSection.MAIN))
            assertEquals("GD01-001_p1", result.snapshot.entries.first { it.printingId != null }.printingId)
            assertEquals(3, result.snapshot.quantityOf("ST03-005", DeckSection.MAIN))
        }
    }

    @Test fun aRealWhitebaseCodeDecodes() {
        val code = "N4Ig5iBcIOIMIxAGhANygJhQOyiA0gO4CG22AngOQDOABABLEBeArgNbHUAWAlj7QHUe2MLRgtsAE2IBbWvgD2LAMa82LZCElRQkgKbK2O2ABEADAEYAtBbMB2KBZQBlACpmMVs5asAHC1AALC7unpZmQSE+3gCskabRFgDM8W6JEZDBIGnW3gGQSVG5yfEw5sVxmVFhJZBY2aFeHqnuFaXlTQAcUIUNZkleFgBsmChlPrYZSQC+s0A"
        val list = DeckCodes.toDeckList(code)!!
        assertTrue(list.startsWith("# Kwanny's Hazukashii Wing Gundam Kouchiku"), list)
        assertTrue("4 ST02-001-p1" in list, list)
        assertEquals(50, list.lines().mapNotNull { it.substringBefore(' ').toIntOrNull() }.sum())
    }

    @Test fun plainListsAndBackupsAreNotDeckCodes() {
        assertNull(DeckCodes.toDeckList("4 GD01-001"))
        assertNull(DeckCodes.toDeckList("Gundam"))
        assertNull(DeckCodes.toDeckList("""{"format": "lock-on-backup", "formatVersion": 1, "exportedAt": 0, "decks": []}"""))
        assertNull(DeckCodes.toDeckList("not a code at all, just words"))
    }
}
