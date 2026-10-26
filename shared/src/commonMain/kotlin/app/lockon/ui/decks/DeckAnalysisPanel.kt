package app.lockon.ui.decks

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
import app.lockon.ui.theme.cardColor

@Composable
fun DeckAnalysisPanel(deck: DeckSnapshot, cards: Map<String, Card>, legality: DeckLegality, modifier: Modifier = Modifier) {
    val stats = remember(deck, cards) { DeckAnalysis.stats(deck, cards) }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Legality", style = MaterialTheme.typography.titleSmall)
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
        BarChart("Level curve (main deck)", stats.levelCurve)
        BarChart("Cost curve (main deck)", stats.costCurve, color = MaterialTheme.colorScheme.secondary)
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Card types", style = MaterialTheme.typography.titleSmall)
            CardType.entries.forEach { type ->
                CountLine(type.label, stats.typeCounts[type] ?: 0)
            }
        }
        if (stats.colorCounts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Colors", style = MaterialTheme.typography.titleSmall)
                stats.colorCounts.forEach { (color, count) ->
                    CountLine(color.label, count, accent = cardColor(color))
                }
            }
        }
    }
}

@Composable
private fun CountLine(label: String, count: Int, accent: androidx.compose.ui.graphics.Color? = null) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = accent ?: MaterialTheme.colorScheme.onSurface)
        Text("$count", style = MaterialTheme.typography.bodyMedium)
    }
}
