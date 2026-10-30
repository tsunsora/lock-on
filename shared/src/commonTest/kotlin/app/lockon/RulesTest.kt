package app.lockon

import app.lockon.io.DeckListFormat
import app.lockon.model.BannedCombination
import app.lockon.model.Card
import app.lockon.model.CardColor
import app.lockon.model.CardType
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.Ruleset
import app.lockon.rules.DeckValidator
import app.lockon.sync.PairingInvite
import app.lockon.sync.RevisionGraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private fun unit(n: Int, color: CardColor) =
    Card("T-%03d".format(n), "Unit $n", CardType.UNIT, listOf(color), level = n % 6 + 1, cost = n % 4 + 1, setCode = "T")

private val resource = Card("T-900", "Resource", CardType.RESOURCE, setCode = "T")
private val cards = ((1..13).map { unit(it, if (it <= 7) CardColor.BLUE else CardColor.RED) } +
    unit(20, CardColor.GREEN) + resource).associateBy { it.cardNumber }
private val rules = Ruleset("std", "Standard", "EN", "2026-01-01")

/** 12 cards × 4 + 1 × 2 = 50 main, plus 10 resources, in two colors. */
private fun legalDeck() = DeckSnapshot(
    name = "Legal",
    entries = (1..12).map { DeckEntry("T-%03d".format(it), 4) } + DeckEntry("T-013", 2) +
        DeckEntry("T-900", 10, DeckSection.RESOURCE),
)

class DeckValidatorTest {
    @Test fun legalDeckHasNoErrors() {
        val result = DeckValidator.validate(legalDeck(), cards, rules)
        assertTrue(result.isLegal, result.messages.joinToString())
    }

    @Test fun incompleteDeckIsADraftWithCountErrors() {
        val result = DeckValidator.validate(DeckSnapshot("Draft", entries = listOf(DeckEntry("T-001", 4))), cards, rules)
        assertTrue(result.errors.any { "Main deck has 4 of 50" in it.text })
        assertTrue(result.errors.none { "Resource deck" in it.text }, "the resource deck is not tracked")
    }

    @Test fun copiesCountByCardNumberAcrossPrintings() {
        val deck = legalDeck().let { d ->
            d.copy(entries = d.entries + DeckEntry("T-001", 1, printingId = "T-001-ALT"))
        }
        val result = DeckValidator.validate(deck, cards, rules)
        assertTrue(result.errors.any { "5 copies" in it.text && it.cardNumber == "T-001" })
    }

    @Test fun tokensCantBeInADeck() {
        val token = Card("T-011", "Fatum-00", CardType.UNIT_TOKEN, setCode = "GD01")
        val deck = legalDeck().let { it.copy(entries = it.entries + DeckEntry("T-011", 1)) }
        val result = DeckValidator.validate(deck, cards + (token.cardNumber to token), rules)
        assertTrue(result.errors.any { "token" in it.text && it.cardNumber == "T-011" }, result.messages.joinToString())
        assertEquals(CardType.UNIT_TOKEN, CardType.parse("UNIT TOKEN"))
        assertEquals(CardType.EX_RESOURCE, CardType.parse("EX_RESOURCE"))
    }

    @Test fun resourcesHaveNoCopyLimitByDefault() {
        assertTrue(DeckValidator.validate(legalDeck(), cards, rules).errors.none { it.cardNumber == "T-900" })
        val limited = rules.copy(resourceMaxCopies = 4)
        assertTrue(DeckValidator.validate(legalDeck(), cards, limited).errors.any { it.cardNumber == "T-900" })
    }

    @Test fun thirdColorIsReported() {
        val deck = legalDeck().withQuantity("T-020", DeckSection.MAIN, 1)
        assertTrue(DeckValidator.validate(deck, cards, rules).errors.any { "3 colors" in it.text })
    }

    @Test fun sectionsMustHoldTheRightCardTypes() {
        val deck = legalDeck().withQuantity("T-900", DeckSection.MAIN, 1)
        assertTrue(DeckValidator.validate(deck, cards, rules).errors.any { "belongs in the resource deck" in it.text })
    }

    @Test fun restrictionsAndBannedCombinations() {
        val strict = rules.copy(
            banned = listOf("T-002"),
            restricted = mapOf("T-003" to 1),
            bannedCombinations = listOf(BannedCombination(listOf("T-004", "T-005"))),
        )
        val errors = DeckValidator.validate(legalDeck(), cards, strict).errors.map { it.text }
        assertTrue(errors.any { "banned" in it && "T-002" in it })
        assertTrue(errors.any { "T-003" in it && "limit is 1" in it })
        assertTrue(errors.any { "cannot be in the same deck" in it })
    }
}

class DeckListFormatTest {
    @Test fun exportThenParseRoundTrips() {
        val deck = legalDeck()
        val parsed = DeckListFormat.parse(DeckListFormat.export(deck, cards), cards)
        assertEquals(deck.name, parsed.snapshot.name)
        assertEquals(deck.entries.toSet(), parsed.snapshot.entries.toSet())
        assertTrue(parsed.problems.isEmpty())
    }

    @Test fun parsesNamesQuantitiesAndPutsResourcesInTheirSection() {
        val parsed = DeckListFormat.parse("4x T-001\n2 Unit 8\n10 Resource\nnonsense line", cards)
        assertEquals(4, parsed.snapshot.quantityOf("T-001", DeckSection.MAIN))
        assertEquals(2, parsed.snapshot.quantityOf("T-008", DeckSection.MAIN))
        assertEquals(10, parsed.snapshot.quantityOf("T-900", DeckSection.RESOURCE))
        assertEquals(1, parsed.problems.size)
    }
}

class RevisionGraphTest {
    private fun node(id: String, vararg parents: String, deleted: Boolean = false, at: Long = 0) =
        RevisionGraph.Node(id, parents.toList(), at, deleted)

    @Test fun linearHistoryResolvesToLatestChild() {
        val state = RevisionGraph.state(listOf(node("a"), node("b", "a"), node("c", "b")))
        assertEquals("c", assertIs<RevisionGraph.DeckState.Resolved>(state).head.revisionId)
    }

    @Test fun independentEditsConflictUntilMerged() {
        val split = listOf(node("a"), node("b", "a", at = 2), node("c", "a", at = 1))
        assertEquals(setOf("b", "c"), assertIs<RevisionGraph.DeckState.Conflicted>(RevisionGraph.state(split)).heads.map { it.revisionId }.toSet())
        val merged = split + node("m", "b", "c")
        assertEquals("m", assertIs<RevisionGraph.DeckState.Resolved>(RevisionGraph.state(merged)).head.revisionId)
    }

    @Test fun deleteAgainstEditIsAConflictButTwoDeletesAgree() {
        assertIs<RevisionGraph.DeckState.Conflicted>(RevisionGraph.state(listOf(node("a"), node("d", "a", deleted = true), node("e", "a"))))
        assertIs<RevisionGraph.DeckState.Resolved>(
            RevisionGraph.state(listOf(node("a"), node("d1", "a", deleted = true), node("d2", "a", deleted = true))),
        )
    }
}

class PairingInviteTest {
    @Test fun uriRoundTrips() {
        val invite = PairingInvite("192.168.1.20", 53340, "ABC234", "ab".repeat(32), "Tsun's PC")
        assertEquals(invite, PairingInvite.parse(invite.toUri()))
    }
}
