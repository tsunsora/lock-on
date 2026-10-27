package app.lockon.rules

import app.lockon.model.MatchRecord
import app.lockon.model.MatchResult
import kotlin.math.roundToInt

/** Wins, losses and draws, with the win rate over every game including draws. */
data class Record(val wins: Int = 0, val losses: Int = 0, val draws: Int = 0) {
    val games: Int get() = wins + losses + draws
    /** Whole percent, or null before the first game. */
    val winRate: Int? get() = if (games == 0) null else (wins * 100.0 / games).roundToInt()

    /** "7–3" or "7–3–1" when there are draws. */
    val score: String get() = if (draws == 0) "$wins–$losses" else "$wins–$losses–$draws"

    operator fun plus(result: MatchResult) = when (result) {
        MatchResult.WIN -> copy(wins = wins + 1)
        MatchResult.LOSS -> copy(losses = losses + 1)
        MatchResult.DRAW -> copy(draws = draws + 1)
    }
}

data class MatchStats(
    val overall: Record,
    val goingFirst: Record,
    val goingSecond: Record,
    /** Opponents by games played, most first. Unnamed opponents are left out. */
    val byOpponent: List<Pair<String, Record>>,
) {
    companion object {
        fun of(matches: List<MatchRecord>): MatchStats {
            fun tally(list: List<MatchRecord>) = list.fold(Record()) { record, match -> record + match.result }
            return MatchStats(
                overall = tally(matches),
                goingFirst = tally(matches.filter { it.wentFirst == true }),
                goingSecond = tally(matches.filter { it.wentFirst == false }),
                byOpponent = matches.filter { it.opponent.isNotBlank() }
                    .groupBy { it.opponent.trim().lowercase() }
                    .map { (_, games) -> games.last().opponent.trim() to tally(games) }
                    .sortedWith(compareByDescending<Pair<String, Record>> { it.second.games }.thenBy { it.first.lowercase() }),
            )
        }
    }
}
