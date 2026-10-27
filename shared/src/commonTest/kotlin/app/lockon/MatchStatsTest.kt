package app.lockon

import app.lockon.model.MatchRecord
import app.lockon.model.MatchResult
import app.lockon.rules.MatchStats
import app.lockon.rules.Record
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MatchStatsTest {
    private fun match(result: MatchResult, opponent: String = "", first: Boolean? = null) =
        MatchRecord(id = "$result$opponent$first", playedAt = 0, result = result, opponent = opponent, wentFirst = first)

    @Test fun countsTheRecordAndSplitsByTurnOrder() {
        val stats = MatchStats.of(
            listOf(
                match(MatchResult.WIN, first = true), match(MatchResult.WIN, first = true),
                match(MatchResult.LOSS, first = false), match(MatchResult.DRAW),
            ),
        )
        assertEquals(Record(wins = 2, losses = 1, draws = 1), stats.overall)
        assertEquals("2–1–1", stats.overall.score)
        assertEquals(50, stats.overall.winRate)
        assertEquals(Record(wins = 2), stats.goingFirst)
        assertEquals(Record(losses = 1), stats.goingSecond)
    }

    @Test fun groupsOpponentsIgnoringCase() {
        val stats = MatchStats.of(
            listOf(match(MatchResult.WIN, "Wing"), match(MatchResult.LOSS, "wing "), match(MatchResult.WIN, "Zaku"), match(MatchResult.WIN)),
        )
        assertEquals(listOf("wing" to Record(1, 1), "Zaku" to Record(wins = 1)), stats.byOpponent)
    }

    @Test fun noGamesHasNoWinRate() = assertNull(MatchStats.of(emptyList()).overall.winRate)
}
