package app.lockon.ui.decks
import app.lockon.ui.components.LocalNavClearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lockon.model.Card
import app.lockon.model.CardType
import app.lockon.model.DeckSnapshot
import app.lockon.rules.DeckAnalysis
import app.lockon.rules.DeckLegality
import app.lockon.rules.Severity
import app.lockon.ui.components.BarChart
import app.lockon.ui.components.DeckGlance
import app.lockon.ui.components.glass
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.components.SectionHeader
import app.lockon.ui.theme.cardColor

@Composable
fun DeckAnalysisPanel(deck: DeckSnapshot, cards: Map<String, Card>, legality: DeckLegality, modifier: Modifier = Modifier) {
    val stats = remember(deck, cards) { DeckAnalysis.stats(deck, cards) }
    Column(modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + LocalNavClearance.current),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // The level curve and the counts by type, Burst and Blocker, as the game shows them.
        if (deck.entries.isNotEmpty()) {
            DeckGlance(stats, Modifier.glass(LockOnShapes.tile, strong = true, clearing = false)
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionHeader("Legality")
            if (legality.messages.isEmpty()) {
                Text("This deck meets every construction rule.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary)
            }
            legality.messages.forEach { message ->
                Text(
                    (if (message.severity == Severity.ERROR) "• " else "◦ ") + message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.severity == Severity.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!legality.isLegal) {
                Text("Incomplete decks still save as drafts.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionHeader("Gundam readouts")
            CountLine("Burst cards (can trigger from shields)", stats.burstCount)
            CountLine("Blockers", stats.blockerCount)
            if (stats.linkPairs.isEmpty()) {
                Text("No Unit has a matching link Pilot in this deck yet.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Link pairs", style = MaterialTheme.typography.bodyMedium)
                stats.linkPairs.forEach { (unit, pilots) ->
                    Text("• ${unit.name} ↔ ${pilots.joinToString { it.name }}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        HorizontalDivider()
        BarChart("Cost curve (main deck)", stats.costCurve, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionHeader("Card types")
            CardType.entries.forEach { type ->
                CountLine(type.label, stats.typeCounts[type] ?: 0)
            }
        }
        if (stats.colorCounts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionHeader("Colors")
                stats.colorCounts.forEach { (color, count) ->
                    // White would vanish on the light sky, so it keeps the text colour; its name says which it is (I4).
                    CountLine(color.label, count, accent = if (color == app.lockon.model.CardColor.WHITE) null else cardColor(color))
                }
            }
        }
    }
}

@Composable
private fun CountLine(label: String, count: Int, accent: androidx.compose.ui.graphics.Color? = null) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = accent ?: MaterialTheme.colorScheme.onSurface)
        Text("$count", style = MaterialTheme.typography.bodyMedium.merge(app.lockon.ui.theme.TabularNumbers))
    }
}
