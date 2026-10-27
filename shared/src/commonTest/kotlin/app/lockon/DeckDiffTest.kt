package app.lockon

import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.MatchRecord
import app.lockon.model.MatchResult
import app.lockon.rules.CardChange
import app.lockon.rules.DeckDiff
import kotlin.test.Test
import kotlin.test.assertEquals

class DeckDiffTest {
    private val before = DeckSnapshot(
        name = "Wing",
        entries = listOf(DeckEntry("SMP-001", 4), DeckEntry("SMP-002", 2), DeckEntry("SMP-014", 1, DeckSection.CONSIDERING)),
    )

    @Test fun listsEveryChangedCount() {
        val after = before.withQuantity("SMP-001", DeckSection.MAIN, 3).withQuantity("SMP-005", DeckSection.MAIN, 2)
        assertEquals(
            listOf(CardChange("SMP-001", DeckSection.MAIN, 4, 3), CardChange("SMP-005", DeckSection.MAIN, 0, 2)),
            DeckDiff.cards(before, after),
        )
        assertEquals("+2 −1 cards", DeckDiff.summary(before, after))
    }

    @Test fun summarisesEverythingElseInWords() {
        val after = before.copy(
            name = "Wing Zero", folder = "Locals",
            matches = listOf(MatchRecord("m", 1, MatchResult.WIN)),
        ).move("SMP-002", DeckSection.MAIN, DeckSection.CONSIDERING)
        assertEquals("−1 card · considering changed · renamed · moved to Locals · 1 match logged", DeckDiff.summary(before, after))
        assertEquals("No changes", DeckDiff.summary(before, before))
    }
}
