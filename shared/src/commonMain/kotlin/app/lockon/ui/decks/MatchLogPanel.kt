package app.lockon.ui.decks
import app.lockon.ui.components.LineIcons
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import app.lockon.platform.dayLabel
import app.lockon.ui.components.Accent
import app.lockon.ui.components.GameMenu
import app.lockon.ui.components.GameMenuItem
import app.lockon.ui.components.GlassIconButton
import app.lockon.ui.components.Motion
import app.lockon.ui.components.SoftTile
import app.lockon.ui.components.motion
import app.lockon.ui.components.softThumb
import app.lockon.ui.components.softThumbInk
import app.lockon.ui.theme.ChartSlate
import app.lockon.ui.theme.ChartSlateTwilight
import app.lockon.ui.components.LocalTopBand
import app.lockon.ui.components.isTwilight
import app.lockon.ui.components.panelFill
import androidx.compose.foundation.lazy.itemsIndexed
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.components.revealOut
import app.lockon.ui.components.revealIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import app.lockon.ui.components.PillChip
import app.lockon.ui.components.PillTextField
import app.lockon.ui.components.VerticalScrollBar
import app.lockon.ui.components.LocalNavClearance
import app.lockon.ui.components.PillSegmented
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle

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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import app.lockon.ui.components.SectionHeader
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
    var noting by rememberSaveable { mutableStateOf(false) }
    // 0 = not recorded, 1 = went first, 2 = went second.
    var turn by rememberSaveable { mutableStateOf(0) }
    val scheme = MaterialTheme.colorScheme

    val listState = rememberLazyListState()
    // On a phone everything sits on solid panels, and logging stays above the bar under the thumb.
    val phone = LocalTopBand.current.height == 0.dp
    val gap = if (phone) 12.dp else 0.dp
    val logPanel = @Composable { panelModifier: Modifier ->
        SoftTile(panelModifier.fillMaxWidth(), solid = phone, padding = if (phone) 12.dp else 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillTextField(opponent, { opponent = it }, "Opponent", Modifier.weight(1f))
                TurnChoice(turn) { turn = it }
                GlassIconButton(LineIcons.Edit, "Add a note", { noting = !noting }, compact = true)
            }
            // Earlier opponents to pick from instead of typing (UX rule C4).
            val picks = opponentSuggestions.filter { opponent.isNotBlank() && it.contains(opponent.trim(), ignoreCase = true) && !it.equals(opponent.trim(), ignoreCase = true) }.take(6)
            if (picks.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    picks.forEach { PillChip(selected = false, onClick = { opponent = it }, label = { Text(it) }) }
                }
            }
            AnimatedVisibility(noting, enter = revealIn(), exit = revealOut()) {
                PillTextField(notes, { notes = it }, "Note", Modifier.fillMaxWidth())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fun log(result: MatchResult) {
                    onLog(result, opponent.trim(), when (turn) { 1 -> true; 2 -> false; else -> null }, notes.trim())
                    opponent = ""; notes = ""; noting = false; turn = 0
                }
                GameButton("Win", onClick = { log(MatchResult.WIN) }, modifier = Modifier.weight(1f))
                GameButton("Loss", onClick = { log(MatchResult.LOSS) }, modifier = Modifier.weight(1f), tone = scheme.error, onTone = scheme.onError)
                GameButton("Draw", onClick = { log(MatchResult.DRAW) }, modifier = Modifier.weight(1f), tone = drawTone(), onTone = onDrawTone())
            }
        }
    }
    val fill = panelFill()
    Column(modifier) {
    Box(Modifier.weight(1f).fillMaxWidth()) {
    LazyColumn(
        Modifier.fillMaxSize(), listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = if (phone) 8.dp else 16.dp, bottom = if (phone) 8.dp else 24.dp + LocalNavClearance.current),
        verticalArrangement = Arrangement.spacedBy(if (phone) 0.dp else 12.dp),
    ) {
        item(key = "record") {
            val record = @Composable {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    WinRing(stats.overall.winRate)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (stats.overall.games == 0) "0–0" else stats.overall.score, style = MaterialTheme.typography.headlineLarge.merge(TabularNumbers))
                        RecordRow("1st", stats.goingFirst, nameWidth = 36.dp)
                        RecordRow("2nd", stats.goingSecond, nameWidth = 36.dp)
                    }
                }
            }
            if (phone) SoftTile(Modifier.padding(bottom = gap).fillMaxWidth(), solid = true) { record() } else record()
        }
        if (!phone) item(key = "log") { logPanel(Modifier) }
        if (stats.byOpponent.isNotEmpty()) {
            item(key = "opponents") {
                val opponents = @Composable {
                    SectionHeader("By opponent")
                    Column(verticalArrangement = Arrangement.spacedBy(if (phone) 12.dp else 20.dp)) {
                        stats.byOpponent.forEach { (name, record) -> RecordRow(name, record, nameWidth = 150.dp) }
                    }
                }
                if (phone) SoftTile(Modifier.animateItem().padding(bottom = gap).fillMaxWidth(), solid = true) { opponents() }
                else Column(Modifier.animateItem(), verticalArrangement = Arrangement.spacedBy(20.dp)) { opponents() }
            }
        }
        // A phone's games read as one panel: the header rounds its top and the last game its bottom.
        val tile = LockOnShapes.tileRadius
        item(key = "games-header") {
            val shape = RoundedCornerShape(topStart = tile, topEnd = tile, bottomStart = if (matches.isEmpty()) tile else 0.dp, bottomEnd = if (matches.isEmpty()) tile else 0.dp)
            SectionHeader(
                "Games",
                Modifier.animateItem().then(if (phone) Modifier.background(fill, shape).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (matches.isEmpty()) 16.dp else 6.dp) else Modifier),
                readout = "${matches.size}",
            )
        }
        itemsIndexed(matches.asReversed(), key = { _, match -> match.id }) { index, match ->
            var menu by remember { mutableStateOf(false) }
            val last = index == matches.lastIndex
            val rowShape = RoundedCornerShape(bottomStart = if (last) tile else 0.dp, bottomEnd = if (last) tile else 0.dp)
            Box(Modifier.animateItem().then(if (phone) Modifier.background(fill, rowShape).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = if (last) 16.dp else 4.dp) else Modifier)) {
                Row(
                    Modifier.fillMaxWidth().clip(LockOnShapes.button).clickable(onClickLabel = "Match options") { menu = true }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val (letter, tone, ink) = when (match.result) {
                        MatchResult.WIN -> Triple("W", scheme.primary, scheme.onPrimary)
                        MatchResult.LOSS -> Triple("L", scheme.error, scheme.onError)
                        MatchResult.DRAW -> Triple("D", drawTone(), onDrawTone())
                    }
                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(tone), contentAlignment = Alignment.Center) {
                        Text(letter, style = MaterialTheme.typography.labelLarge, color = ink)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(match.opponent.ifBlank { "Opponent not noted" }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (match.opponent.isBlank()) scheme.onSurfaceVariant else scheme.onSurface)
                        if (match.notes.isNotBlank()) Text(match.notes, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    match.wentFirst?.let { first ->
                        // One width for both, so the badges make a tidy column.
                        Text(
                            if (first) "1st" else "2nd", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                            modifier = Modifier.width(40.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, scheme.onSurface.copy(alpha = 0.24f), RoundedCornerShape(8.dp)).padding(vertical = 3.dp),
                        )
                    }
                    Text(dayLabel(match.playedAt), Modifier.widthIn(min = 58.dp), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, textAlign = TextAlign.End, maxLines = 1)
                }
                GameMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    GameMenuItem(formatTimestamp(match.playedAt), onClick = { menu = false })
                    GameMenuItem("Remove this match", onClick = { menu = false; onDelete(match) }, icon = LineIcons.Trash, danger = true)
                }
            }
        }
    }
    VerticalScrollBar(listState, Modifier.align(Alignment.TopEnd))
    }
    if (phone) logPanel(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = LocalNavClearance.current + 4.dp))
    }
}

/** Draw's colour: the deep slate reads as disabled on twilight's navy, so there it takes the lighter one. */
@Composable
private fun drawTone(): Color = if (isTwilight()) ChartSlateTwilight else ChartSlate

@Composable
private fun onDrawTone(): Color = if (isTwilight()) Color(0xFF1B2147) else Color.White

/** The win rate as a ring, with a dash before any games. */
@Composable
private fun WinRing(rate: Int?) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val fill = Accent
    val sweep by animateFloatAsState((rate ?: 0) / 100f * 360f, motion(Motion.LONG), label = "win-ring")
    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = Stroke(9.dp.toPx(), cap = StrokeCap.Round)
            val inset = 4.5.dp.toPx()
            val box = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), box, style = stroke)
            if (sweep > 0f) drawArc(fill, -90f, sweep, false, Offset(inset, inset), box, style = stroke)
        }
        Text(rate?.let { "$it%" } ?: "—", style = MaterialTheme.typography.titleMedium.merge(TabularNumbers))
    }
}

/** A name, wins against losses as one bar, and the score. */
@Composable
private fun RecordRow(name: String, record: Record, nameWidth: Dp) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(name, Modifier.width(nameWidth), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (record.games == 0) Box(Modifier.fillMaxSize().background(scheme.onSurface.copy(alpha = 0.1f)))
            if (record.wins > 0) Box(Modifier.weight(record.wins.toFloat()).fillMaxHeight().background(scheme.primary))
            if (record.draws > 0) Box(Modifier.weight(record.draws.toFloat()).fillMaxHeight().background(drawTone()))
            if (record.losses > 0) Box(Modifier.weight(record.losses.toFloat()).fillMaxHeight().background(scheme.error.copy(alpha = 0.7f)))
        }
        Text(if (record.games == 0) "—" else record.score, Modifier.widthIn(min = 40.dp), style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), textAlign = TextAlign.End, maxLines = 1)
    }
}

/** Went first or second; tapping the lit one clears it, since turn order is optional. */
@Composable
private fun TurnChoice(turn: Int, onChange: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.height(44.dp).clip(LockOnShapes.button).background(scheme.onSurface.copy(alpha = 0.09f)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf("1st" to 1, "2nd" to 2).forEach { (label, value) ->
            val on = turn == value
            Box(
                Modifier.fillMaxHeight().clip(RoundedCornerShape(11.dp)).background(if (on) softThumb() else Color.Transparent)
                    .selectable(on, role = Role.RadioButton) { onChange(if (on) 0 else value) }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) softThumbInk() else scheme.onSurfaceVariant) }
        }
    }
}
