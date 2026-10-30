package app.lockon

import app.lockon.io.DeckListFormat
import app.lockon.model.Card
import app.lockon.model.CardColor
import app.lockon.model.CardType
import app.lockon.model.DeckSection
import app.lockon.model.Printing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeckListImportTest {
    private val zakuA = Card("GD01-010", "Zaku II", CardType.UNIT, listOf(CardColor.GREEN), setCode = "GD01")
    private val zakuB = Card("ST03-005", "Zaku II", CardType.UNIT, listOf(CardColor.GREEN), setCode = "ST03")
    private val gundam = Card("GD01-001", "Gundam", CardType.UNIT, listOf(CardColor.BLUE), setCode = "GD01")
    private val cards = listOf(zakuA, zakuB, gundam).associateBy { it.cardNumber }
    private val printings = mapOf(
        "GD01-001" to listOf(
            Printing("GD01-001", "GD01-001", "GD01", "LR"),
            Printing("GD01-001_p1", "GD01-001", "GD01", "LR", isAlternateArt = true),
        ),
    )

    @Test fun aHugeQuantityIsAProblemNotACrash() {
        val result = DeckListFormat.parse("99999999999 GD01-001\n2 GD01-010", cards)
        assertEquals(0, result.snapshot.quantityOf("GD01-001", DeckSection.MAIN))
        assertEquals(2, result.snapshot.quantityOf("GD01-010", DeckSection.MAIN))
        assertEquals(1, result.problems.size)
    }

    @Test fun aByteOrderMarkIsIgnored() {
        val result = DeckListFormat.parse("﻿# From Notepad\r\n4 GD01-001\r\n", cards)
        assertEquals("From Notepad", result.snapshot.name)
        assertEquals(4, result.snapshot.quantityOf("GD01-001", DeckSection.MAIN))
        assertTrue(result.problems.isEmpty(), result.problems.toString())
    }

    @Test fun aNameMeansTheCardNotTheTokenOfTheSameName() {
        val token = Card("T-001", "Gundam", CardType.UNIT_TOKEN, setCode = "GD01")
        val result = DeckListFormat.parse("4 Gundam", cards + (token.cardNumber to token))
        assertEquals(4, result.snapshot.quantityOf("GD01-001", DeckSection.MAIN))
        assertTrue(result.problems.isEmpty(), result.problems.toString())
    }

    @Test fun aNameSharedByTwoCardsIsReportedNotGuessed() {
        val result = DeckListFormat.parse("4 Zaku II\n4 Gundam", cards)
        assertEquals(listOf("GD01-001"), result.snapshot.entries.map { it.cardNumber })
        assertTrue("GD01-010" in result.problems.single() && "ST03-005" in result.problems.single())
    }

    @Test fun printingIdsShareTheirCardsCopyLimitAndKeepTheArt() {
        val result = DeckListFormat.parse("2 GD01-001\n2 GD01-001_p1", cards, printings = printings)
        assertEquals(4, result.snapshot.quantityOf("GD01-001", DeckSection.MAIN))
        assertTrue(result.problems.isEmpty(), result.problems.toString())
        // Without the printings, an unknown printing still counts as its card.
        val bare = DeckListFormat.parse("2 GD01-001_P9", cards)
        assertEquals(2, bare.snapshot.quantityOf("GD01-001", DeckSection.MAIN))
    }
}
