package app.lockon.ui.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lockon.model.MatchRecord
import app.lockon.model.MatchResult
import app.lockon.platform.formatTimestamp
import app.lockon.rules.MatchStats
import app.lockon.rules.Record
import app.lockon.ui.components.Badge
import app.lockon.ui.components.HudHeader
import app.lockon.ui.theme.TabularNumbers

/**
 * The deck's win/loss log. Opponent and turn order are optional and filled in first, so a result
 * is one tap: Win, Loss or Draw logs the game at once.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchLogPanel(
    matches: List<MatchRecord>,
    opponentSuggestions: List<String>,
    modifier: Modifier = Modifier,
    onLog: (result: MatchResult, opponent: String, wentFirst: Boolean?, notes: String) -> Unit,
    onDelete: (MatchRecord) -> Unit,
) {
    val stats = remember(matches) { MatchStats.of(matches) }
    var opponent by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    // 0 = not recorded, 1 = went first, 2 = went second.
    var turn by rememberSaveable { mutableStateOf(0) }

    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "record") {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HudHeader("Record", readout = if (stats.overall.games == 0) null else stats.overall.score)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Readout("Win rate", stats.overall.winRate?.let { "$it%" } ?: "—")
                    Readout("Games", "${stats.overall.games}")
                    Readout("Going first", stats.goingFirst.short())
                    Readout("Going second", stats.goingSecond.short())
                }
            }
        }
        item(key = "log") {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HudHeader("Log a match")
                OutlinedTextField(
                    opponent, { opponent = it }, label = { Text("Opponent's deck (optional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Earlier opponents to pick from instead of typing (UX rule C4).
                val picks = opponentSuggestions.filter { it.contains(opponent.trim(), ignoreCase = true) && !it.equals(opponent.trim(), ignoreCase = true) }.take(6)
                if (picks.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        picks.forEach { SuggestionChip(onClick = { opponent = it }, label = { Text(it) }) }
                    }
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Turn not noted", "Went first", "Went second").forEachIndexed { i, label ->
                        SegmentedButton(selected = turn == i, onClick = { turn = i }, shape = SegmentedButtonDefaults.itemShape(i, 3)) {
                            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fun log(result: MatchResult) {
                        onLog(result, opponent.trim(), when (turn) { 1 -> true; 2 -> false; else -> null }, notes.trim())
                        opponent = ""; notes = ""; turn = 0
                    }
                    Button(onClick = { log(MatchResult.WIN) }, modifier = Modifier.weight(1f)) { Text("Win") }
                    FilledTonalButton(onClick = { log(MatchResult.LOSS) }, modifier = Modifier.weight(1f)) { Text("Loss") }
                    OutlinedButton(onClick = { log(MatchResult.DRAW) }, modifier = Modifier.weight(1f)) { Text("Draw") }
                }
            }
        }
        if (stats.byOpponent.isNotEmpty()) {
            item(key = "opponents") {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HudHeader("By opponent")
                    stats.byOpponent.forEach { (name, record) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                            Text(record.short(), style = MaterialTheme.typography.bodyMedium.merge(TabularNumbers))
                        }
                    }
                }
            }
        }
        item(key = "games-header") {
            HudHeader("Games", Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp), readout = "${matches.size}")
            if (matches.isEmpty()) {
                Text(
                    "No games logged yet. Results sync with the deck, and a match logged on each of two devices is kept on both.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        items(matches.asReversed(), key = { it.id }) { match ->
            Column(Modifier.animateItem()) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    val c = MaterialTheme.colorScheme
                    when (match.result) {
                        MatchResult.WIN -> Badge("Win", c.secondaryContainer, c.onSecondaryContainer)
                        MatchResult.LOSS -> Badge("Loss", c.surfaceVariant, c.onSurfaceVariant)
                        MatchResult.DRAW -> Badge("Draw", c.surfaceContainerHighest, c.onSurface)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            listOfNotNull(
                                match.opponent.ifBlank { null }?.let { "vs $it" },
                                when (match.wentFirst) { true -> "went first"; false -> "went second"; null -> null },
                            ).joinToString(" · ").ifEmpty { "Opponent not noted" },
                            style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            listOf(formatTimestamp(match.playedAt), match.notes).filter { it.isNotBlank() }.joinToString(" — "),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { onDelete(match) }) { Icon(Icons.Default.Close, contentDescription = "Remove this match") }
                }
                HorizontalDivider(Modifier.padding(start = 16.dp, top = 4.dp))
            }
        }
        item(key = "bottom") { Spacer(Modifier.padding(bottom = 24.dp)) }
    }
}

@Composable
private fun Readout(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge.merge(TabularNumbers))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "3–1 (75%)", or a dash before any games. */
private fun Record.short(): String = winRate?.let { "$score ($it%)" } ?: "—"
