package app.lockon

import app.lockon.model.Card
import app.lockon.model.CardType
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.rules.DeckAnalysis
import app.lockon.rules.SampleHand
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SampleHandTest {
    private val deck = DeckSnapshot(
        name = "Test",
        entries = (1..12).map { DeckEntry("C-%02d".format(it), 4) } + DeckEntry("C-13", 2) + DeckEntry("R", 10, DeckSection.RESOURCE),
    )

    @Test fun dealsFiveFromTheMainDeckOnly() {
        val game = SampleHand.deal(deck, Random(1))
        assertEquals(5, game.hand.size)
        assertEquals(45, game.library.size)
        assertTrue("R" !in game.hand + game.library)
    }

    @Test fun redrawSendsTheHandToTheBottomOnce() {
        val first = SampleHand.deal(deck, Random(2))
        val redrawn = first.redraw()
        assertEquals(first.library.take(5), redrawn.hand)
        assertEquals(first.hand, redrawn.library.takeLast(5))
        assertFalse(redrawn.canRedraw)
        assertEquals(redrawn, redrawn.redraw().let { if (it === redrawn) redrawn else it })
    }

    @Test fun keepingSetsSixShieldsAndDrawsComeAfterThem() {
        val game = SampleHand.deal(deck, Random(3))
        val drawn = game.draw()
        assertEquals(6, drawn.shields.size)
        assertEquals(game.library.drop(6).first(), drawn.hand.last())
        assertEquals(1, drawn.drawsTaken)
        assertFalse(drawn.canRedraw)
        assertEquals(50, drawn.hand.size + drawn.shields.size + drawn.library.size)
    }
}

class LinkPairTest {
    private fun card(n: String, type: CardType, name: String = n, link: String? = null, traits: List<String> = emptyList()) =
        Card(n, name, type, link = link, traits = traits, setCode = "T")

    @Test fun matchesPilotsByNameOrTrait() {
        val byName = card("U1", CardType.UNIT, link = "Ace Rook")
        val byTrait = card("U2", CardType.UNIT, link = "Pilot with trait (Orbital Guard)")
        val noPartner = card("U3", CardType.UNIT, link = "Somebody Else")
        val rook = card("P1", CardType.PILOT, name = "Ace Rook")
        val guard = card("P2", CardType.PILOT, name = "Mira", traits = listOf("Orbital Guard"))
        val pairs = DeckAnalysis.linkPairs(listOf(byName, byTrait, noPartner, rook, guard)).associate { it.first.cardNumber to it.second.map { p -> p.cardNumber } }
        assertEquals(mapOf("U1" to listOf("P1"), "U2" to listOf("P2")), pairs)
    }
}
