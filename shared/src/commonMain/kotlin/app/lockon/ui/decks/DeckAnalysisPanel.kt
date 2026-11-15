package app.lockon.ui.decks
import androidx.compose.foundation.layout.ColumnScope
import app.lockon.ui.components.LocalTopBand
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import app.lockon.ui.components.PillSegmented
import kotlin.math.roundToInt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
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
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Burst", stats.burstCount, Modifier.weight(1f).fillMaxHeight())
            StatTile("Blockers", stats.blockerCount, Modifier.weight(1f).fillMaxHeight())
            StatTile("Links", stats.linkPairs.size, Modifier.weight(1f).fillMaxHeight(),
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
        // One main chart, the tallest thing on the screen, switching between the cost and level curves, so
        // neither loses its numbers and two equal charts don't compete. Every bar is the same slate (V3).
        if (stats.costCurve.isNotEmpty() || stats.levelCurve.isNotEmpty()) AnalysisTile(Modifier.fillMaxWidth()) {
            var byLevel by rememberSaveable { mutableStateOf(false) }
            // Few cards cost more than 5, so they share a bar.
            val high = stats.costCurve.filter { it.first >= 5 }.sumOf { it.second }
            val costs = stats.costCurve.filter { it.first < 5 } + if (high > 0) listOf(5 to high) else emptyList()
            val curve = if (byLevel) stats.levelCurve else stats.costCurve
            val average = curve.sumOf { it.first * it.second }.toFloat() / curve.sumOf { it.second }.coerceAtLeast(1)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PillSegmented(listOf("Cost", "Level"), if (byLevel) 1 else 0, { byLevel = it == 1 }, Modifier.width(168.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    "Average ${if (byLevel) "level" else "cost"} ${(average * 10).roundToInt() / 10f}",
                    style = MaterialTheme.typography.labelMedium.merge(TabularNumbers), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                )
            }
            androidx.compose.animation.Crossfade(byLevel, animationSpec = app.lockon.ui.components.motion(), label = "curve") { level ->
                if (level) MiniCurve(stats.levelCurve, Modifier.fillMaxWidth().padding(top = 8.dp), height = 176.dp, caption = false, prefix = "", axis = true)
                else MiniCurve(costs, Modifier.fillMaxWidth().padding(top = 8.dp), height = 176.dp, caption = false, prefix = "", suffixLast = if (high > 0) "+" else "", axis = true)
            }
        }
        val slate = if (isTwilight()) ChartSlateTwilight else ChartSlate
        // Tokens only show when a deck has one by mistake, which legality already flags.
        val types = CardType.entries.filter { !it.isResource }.mapNotNull { type -> stats.typeCounts[type]?.takeIf { it > 0 }?.let { type to it } }
        if (types.isNotEmpty() || stats.colorCounts.isNotEmpty()) AnalysisTile(Modifier.fillMaxWidth()) {
            if (types.isNotEmpty()) {
                Text("Card types", style = MaterialTheme.typography.titleSmall)
                // Four distinct slate steps; the darkest gets an ink line so it holds on the panel. No text sits on a fill.
                val shades = if (isTwilight()) TwilightTypeShades
                    else listOf(1f, 0.62f, 0.36f, 0.18f).map { slate.copy(alpha = it).compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest) }
                val line = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                SplitBar(types.mapIndexed { i, (_, count) -> Segment(count, shades.getOrElse(i) { shades.last() }, line.takeIf { i == 3 }) })
                Legend(types.mapIndexed { i, (type, count) -> Triple("$count ${type.label}s", shades.getOrElse(i) { shades.last() }, line.takeIf { i == 3 }) })
            }
            if (stats.colorCounts.isNotEmpty()) {
                Text("Colors", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = if (types.isNotEmpty()) 8.dp else 0.dp))
                val colors = stats.colorCounts.entries.sortedBy { it.key.ordinal }
                SplitBar(colors.map { (color, count) -> Segment(count, cardColor(color)) })
                Legend(colors.map { (color, count) -> Triple("${color.label} $count", cardColor(color), null) })
            }
        }
    }
    VerticalScrollBar(scroll, Modifier.align(Alignment.TopEnd))
    }
}

/** A [SoftTile], solid on a phone where it sits on the sky rather than on a panel. */
@Composable
private fun AnalysisTile(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    SoftTile(modifier, solid = LocalTopBand.current.height == 0.dp, content = content)

/** A muted label over a big number, flat with no rim, and nothing else: an icon beside each label only filled the tile. */
@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier, onClick: (() -> Unit)? = null) {
    AnalysisTile(modifier.clip(LockOnShapes.tile).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = app.lockon.ui.components.mutedInk(), maxLines = 1)
        app.lockon.ui.components.RollingNumber(value, MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp).merge(TabularNumbers))
    }
}

/** The card type split's slate steps at twilight, lightest first. */
private val TwilightTypeShades = listOf(Color(0xFFB4BEE0), Color(0xFF8090BF), Color(0xFF56639A), Color(0xFF3A4672))

private class Segment(val size: Int, val color: Color, val line: Color? = null)

/** One 12 dp bar split by share with 2 dp gaps. The legend under it names each part, so no text sits on a fill. */
@Composable
private fun SplitBar(segments: List<Segment>) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(12.dp).clip(RoundedCornerShape(6.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        segments.forEach { segment ->
            // A new share moves the bar's split rather than redrawing it.
            val share by androidx.compose.animation.core.animateFloatAsState(segment.size.toFloat(), app.lockon.ui.components.motion(app.lockon.ui.components.Motion.LONG), label = "split")
            Box(
                Modifier.weight(share.coerceAtLeast(0.01f)).fillMaxHeight().background(segment.color)
                    .then(if (segment.line != null) Modifier.border(1.dp, segment.line) else Modifier),
            )
        }
    }
}

/** A split bar's legend, two to a row: a swatch in the part's exact colour, then its words and count. */
@Composable
private fun Legend(entries: List<Triple<String, Color, Color?>>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { (text, color, line) ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(color).then(if (line != null) Modifier.border(1.dp, line, RoundedCornerShape(4.dp)) else Modifier))
                        Text(text, style = MaterialTheme.typography.labelMedium.merge(TabularNumbers), maxLines = 1)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
