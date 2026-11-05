package app.lockon.ui.decks
import androidx.compose.foundation.layout.ColumnScope
import app.lockon.ui.components.LocalTopBand
import app.lockon.ui.components.LineIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import app.lockon.ui.components.Glyphs
import app.lockon.ui.components.SoftTile
import app.lockon.ui.components.glassColors
import app.lockon.ui.components.isTwilight
import app.lockon.ui.theme.onCardColor
import app.lockon.ui.components.revealIn
import app.lockon.ui.components.revealOut
import app.lockon.ui.theme.ChartSlate
import app.lockon.ui.theme.ChartSlateTwilight
import app.lockon.ui.theme.TabularNumbers
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import app.lockon.ui.components.VerticalScrollBar
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
import app.lockon.ui.components.MiniCurve
import app.lockon.ui.components.DeckGlance
import app.lockon.ui.components.glass
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.components.SectionHeader
import app.lockon.ui.theme.cardColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeckAnalysisPanel(deck: DeckSnapshot, cards: Map<String, Card>, legality: DeckLegality, modifier: Modifier = Modifier) {
    val stats = remember(deck, cards) { DeckAnalysis.stats(deck, cards) }
    val scroll = rememberScrollState()
    Box(modifier) {
    var showLinks by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + LocalNavClearance.current),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // What keeps the deck from being legal, in words, only while something does.
        app.lockon.ui.components.RevealValue(legality.messages.takeIf { it.isNotEmpty() }) { messages -> AnalysisTile(Modifier.fillMaxWidth()) {
            messages.forEach { message ->
                Text(
                    message.text, style = MaterialTheme.typography.bodyMedium,
                    color = if (message.severity == Severity.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(LineIcons.Burst, "Burst", stats.burstCount, Modifier.weight(1f).fillMaxHeight())
            StatTile(LineIcons.Wall, "Blockers", stats.blockerCount, Modifier.weight(1f).fillMaxHeight())
            StatTile(Glyphs.Link, "Links", stats.linkPairs.size, Modifier.weight(1f).fillMaxHeight(),
                onClick = if (stats.linkPairs.isEmpty()) null else ({ showLinks = !showLinks }))
        }
        // Which Unit each Pilot links to, under the count that opens it.
        AnimatedVisibility(showLinks && stats.linkPairs.isNotEmpty(), enter = revealIn(), exit = revealOut()) {
            AnalysisTile(Modifier.fillMaxWidth()) {
                stats.linkPairs.forEach { (unit, pilots) ->
                    Text("${unit.name} ↔ ${pilots.joinToString { it.name }}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (stats.levelCurve.isNotEmpty()) AnalysisTile(Modifier.fillMaxWidth()) {
            Text("Level", style = MaterialTheme.typography.titleSmall)
            MiniCurve(stats.levelCurve, Modifier.fillMaxWidth(), height = 112.dp, caption = false, prefix = "")
        }
        if (stats.costCurve.isNotEmpty()) AnalysisTile(Modifier.fillMaxWidth()) {
            Text("Cost", style = MaterialTheme.typography.titleSmall)
            // Few cards cost more than 5, so they share a bar.
            val high = stats.costCurve.filter { it.first >= 5 }.sumOf { it.second }
            val costs = stats.costCurve.filter { it.first < 5 } + if (high > 0) listOf(5 to high) else emptyList()
            MiniCurve(costs, Modifier.fillMaxWidth(), height = 112.dp, caption = false, prefix = "", suffixLast = if (high > 0) "+" else "")
        }
        val slate = if (isTwilight()) ChartSlateTwilight else ChartSlate
        // Tokens only show when a deck has one by mistake, which legality already flags.
        val types = CardType.entries.filter { !it.isResource }.mapNotNull { type -> stats.typeCounts[type]?.takeIf { it > 0 }?.let { type to it } }
        if (types.isNotEmpty()) AnalysisTile(Modifier.fillMaxWidth()) {
            // Evenly spaced steps, and each square under the bar is its segment's exact colour.
            val ramp = listOf(1f, 0.62f, 0.36f, 0.18f)
            val under = if (isTwilight()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest
            val shades = types.indices.map { i -> slate.copy(alpha = ramp.getOrElse(i) { 0.18f }).compositeOver(under) }
            SplitBar(types.mapIndexed { i, (_, count) -> Segment("$count", count, shades[i], if (i == 0) (if (isTwilight()) Color(0xFF1B2147) else Color.White) else MaterialTheme.colorScheme.onSurface) })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                types.forEachIndexed { i, (type, _) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(shades[i]))
                        Text("${type.label}s", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        if (stats.colorCounts.isNotEmpty()) AnalysisTile(Modifier.fillMaxWidth()) {
            SplitBar(stats.colorCounts.entries.sortedBy { it.key.ordinal }.map { (color, count) ->
                Segment("${color.label.take(1)} $count", count, cardColor(color), onCardColor(color))
            })
        }
    }
    VerticalScrollBar(scroll, Modifier.align(Alignment.TopEnd))
    }
}

/** A [SoftTile], solid on a phone where it sits on the sky rather than on a panel. */
@Composable
private fun AnalysisTile(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    SoftTile(modifier, solid = LocalTopBand.current.height == 0.dp, content = content)

/** An icon and label over a big number. */
@Composable
private fun StatTile(icon: ImageVector, label: String, value: Int, modifier: Modifier, onClick: (() -> Unit)? = null) {
    AnalysisTile(modifier.clip(LockOnShapes.tile).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        app.lockon.ui.components.RollingNumber(value, MaterialTheme.typography.headlineSmall.merge(TabularNumbers))
    }
}

private class Segment(val text: String, val size: Int, val color: Color, val ink: Color)

/** One bar split by share, with each part's number on it. */
@Composable
private fun SplitBar(segments: List<Segment>) {
    Row(Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(7.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        segments.forEach { segment ->
            // A new share moves the bar's split rather than redrawing it.
            val share by androidx.compose.animation.core.animateFloatAsState(segment.size.toFloat(), app.lockon.ui.components.motion(app.lockon.ui.components.Motion.LONG), label = "split")
            Box(Modifier.weight(share.coerceAtLeast(0.01f)).fillMaxHeight().background(segment.color), contentAlignment = Alignment.Center) {
                Text(segment.text, style = MaterialTheme.typography.labelMedium.merge(TabularNumbers), color = segment.ink, maxLines = 1)
            }
        }
    }
}
