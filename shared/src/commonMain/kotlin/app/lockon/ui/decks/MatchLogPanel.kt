package app.lockon.ui.decks
import app.lockon.ui.components.RevealValue
import app.lockon.ui.components.FadeSwap
import app.lockon.ui.components.softThumbFill
import app.lockon.ui.components.softThumbTone
import androidx.compose.animation.core.snap
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import app.lockon.ui.components.LineIcons
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
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
import app.lockon.ui.components.GameDialog
import app.lockon.ui.components.GlassIconButton
import app.lockon.ui.components.Motion
import app.lockon.ui.components.SoftTile
import app.lockon.ui.components.motion
import app.lockon.ui.components.softThumb
import app.lockon.ui.components.softThumbInk
import app.lockon.ui.theme.ChartSlate
import app.lockon.ui.theme.ChartSlateTwilight
import app.lockon.ui.theme.WinSlateTwilight
import app.lockon.ui.components.HoverTooltip
import app.lockon.ui.components.LocalTopBand
import app.lockon.ui.components.isTwilight
import app.lockon.ui.components.panelFill
import androidx.compose.foundation.lazy.itemsIndexed
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.components.revealOut
import app.lockon.ui.components.revealIn
import androidx.compose.animation.AnimatedVisibility
import app.lockon.ui.components.rememberPrefetchingListState
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    onEdit: (before: MatchRecord, after: MatchRecord) -> Unit,
    onDelete: (MatchRecord) -> Unit,
) {
    val stats = remember(matches) { MatchStats.of(matches) }
    var opponent by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var noting by rememberSaveable { mutableStateOf(false) }
    // 0 = not recorded, 1 = went first, 2 = went second. It starts on 1st, the switch's default, and tapping the lit one clears it.
    var turn by rememberSaveable { mutableStateOf(1) }
    val scheme = MaterialTheme.colorScheme
    // The game being edited, by id, so an edit that syncs in meanwhile shows rather than a stale copy.
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = editingId?.let { id -> matches.firstOrNull { it.id == id } }

    val listState = rememberPrefetchingListState()
    // On a phone everything sits on solid panels, and logging stays above the bar under the thumb.
    val phone = LocalTopBand.current.height == 0.dp
    // A short phone gets a slimmer record, so more games fit above the logger.
    var short by remember { mutableStateOf(false) }
    val gap = if (phone) 12.dp else 0.dp
    // A game that wasn't here a moment ago, logged or just edited, is scrolled to and lit for a moment,
    // so it's plain that the tap took.
    var fresh by remember { mutableStateOf<String?>(null) }
    // The result button just tapped reads Logged for a moment, where the eyes already are.
    var logged by remember { mutableStateOf<MatchResult?>(null) }
    LaunchedEffect(logged) { if (logged != null) { delay(1500); logged = null } }
    val seen = remember { mutableSetOf<String>().apply { matches.forEach { add(it.id) } } }
    LaunchedEffect(matches) {
        val added = matches.lastOrNull { it.id !in seen }
        matches.forEach { seen.add(it.id) }
        if (added != null) {
            fresh = added.id
            // The newest game sits just under the Games header, so it's in view whenever the header is.
            val header = if (phone) 1 else 2
            val shown = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == header }
            if (shown == null || shown.offset < 0 || shown.offset + shown.size + 60 > listState.layoutInfo.viewportEndOffset) listState.animateScrollToItem(header)
            delay(1500)
            fresh = null
        }
    }
    val logPanel = @Composable { panelModifier: Modifier ->
        val form: @Composable ColumnScope.() -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillTextField(opponent, { opponent = it }, "Opponent", Modifier.weight(1f))
                TurnChoice(turn) { turn = it }
                // Lit while a note is open or written, so a note waiting to be logged isn't missed. Icon only, named by its tooltip (I5).
                HoverTooltip("Add note") {
                    PillChip(selected = noting || notes.isNotBlank(), onClick = { noting = !noting }, tall = true,
                        label = { Icon(LineIcons.Edit, contentDescription = "Add note", Modifier.size(18.dp)) })
                }
            }
            // Earlier opponents to pick from instead of typing (UX rule C4).
            val picks = opponentSuggestions.filter { opponent.isNotBlank() && it.contains(opponent.trim(), ignoreCase = true) && !it.equals(opponent.trim(), ignoreCase = true) }.take(6)
            RevealValue(picks.takeIf { it.isNotEmpty() }) { names ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    names.forEach { PillChip(selected = false, onClick = { opponent = it }, label = { Text(it) }) }
                }
            }
            AnimatedVisibility(noting, enter = revealIn(), exit = revealOut()) {
                PillTextField(notes, { notes = it }, "Note", Modifier.fillMaxWidth())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Taller on a phone with room, for a sure one-handed tap.
                val big = phone && !short
                fun log(result: MatchResult) {
                    onLog(result, opponent.trim(), when (turn) { 1 -> true; 2 -> false; else -> null }, notes.trim())
                    opponent = ""; notes = ""; noting = false; turn = 1; logged = result
                }
                // Three equal neutral plates: Win in teal as the one most taken, Loss and Draw in ink, so red
                // stays for the losses in the data above.
                val size = Modifier.then(if (big) Modifier.heightIn(min = 48.dp) else Modifier)
                GameButton(if (logged == MatchResult.WIN) "Logged" else "Win", onClick = { log(MatchResult.WIN) }, modifier = Modifier.weight(1f).then(size), style = GameButtonStyle.SECONDARY, ink = scheme.primary)
                GameButton(if (logged == MatchResult.LOSS) "Logged" else "Loss", onClick = { log(MatchResult.LOSS) }, modifier = Modifier.weight(1f).then(size), style = GameButtonStyle.SECONDARY)
                GameButton(if (logged == MatchResult.DRAW) "Logged" else "Draw", onClick = { log(MatchResult.DRAW) }, modifier = Modifier.weight(1f).then(size), style = GameButtonStyle.SECONDARY)
            }
        }
        // A solid panel on a phone; bare on desktop, where its plates sit on the deck panel like the record above.
        if (phone) SoftTile(panelModifier.fillMaxWidth(), solid = true, padding = 12.dp, content = form)
        else Column(panelModifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), content = form)
    }
    val fill = panelFill()
    val density = androidx.compose.ui.platform.LocalDensity.current
    Column(modifier.onSizeChanged { short = phone && with(density) { it.height.toDp() } < 600.dp }) {
    Box(Modifier.weight(1f).fillMaxWidth()) {
    LazyColumn(
        Modifier.fillMaxSize(), listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = if (phone) 8.dp else 16.dp, bottom = if (phone) 8.dp else 24.dp + LocalNavClearance.current),
        verticalArrangement = Arrangement.spacedBy(if (phone) 0.dp else 12.dp),
    ) {
        item(key = "record") {
            val record = @Composable {
                // The record at title size with the rate beside it in words; no ring drawing the rate a second time.
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (phone) 4.dp else 8.dp)) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FadeSwap(if (stats.overall.games == 0) "0–0" else stats.overall.score) {
                                Text(it, style = (if (phone) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium).merge(TabularNumbers))
                            }
                            stats.overall.winRate?.let { rate ->
                                FadeSwap("$rate% won") {
                                    Text(it, Modifier.padding(bottom = 2.dp), style = MaterialTheme.typography.titleSmall.merge(TabularNumbers), color = scheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            // The bars' key, so slate and red are never the only signal (UX rule I4).
                            ResultKey(Modifier.padding(bottom = 4.dp), draws = stats.overall.draws > 0)
                        }
                        // Play order in words, and games logged without one, so the rows add up to the record (C5).
                        // The order rows share one count scale, so a bar's length is its number of games.
                        val o = stats.overall; val a = stats.goingFirst; val b = stats.goingSecond
                        val unsided = Record(o.wins - a.wins - b.wins, o.losses - a.losses - b.losses, o.draws - a.draws - b.draws)
                        val most = maxOf(a.games, b.games, unsided.games)
                        RecordRow("Went 1st", a, nameWidth = OrderWidth, most = most)
                        RecordRow("Went 2nd", b, nameWidth = OrderWidth, most = most)
                        if (unsided.games > 0) RecordRow("Order not logged", unsided, nameWidth = OrderWidth, most = most)
                }
            }
            if (phone) SoftTile(Modifier.padding(bottom = gap).fillMaxWidth(), solid = true, padding = if (short) 12.dp else 16.dp) { record() } else record()
        }
        if (!phone) item(key = "log") { logPanel(Modifier) }
        // Above the games, as a summary of them.
        if (stats.byOpponent.isNotEmpty()) {
            item(key = "opponents") {
                val opponents = @Composable {
                    SectionHeader("By opponent", readout = "${stats.byOpponent.size}")
                    Column(verticalArrangement = Arrangement.spacedBy(if (phone) 8.dp else 12.dp)) {
                        val most = stats.byOpponent.maxOf { it.second.games }
                        stats.byOpponent.forEach { (name, record) -> RecordRow(name, record, nameWidth = 152.dp, most = most) }
                    }
                }
                if (phone) SoftTile(Modifier.animateItem().padding(bottom = gap).fillMaxWidth(), solid = true) { opponents() }
                else Column(Modifier.animateItem(), verticalArrangement = Arrangement.spacedBy(12.dp)) { opponents() }
            }
        }
        // A phone's games read as one panel: the header rounds its top and the last game its bottom.
        val tile = LockOnShapes.tileRadius
        item(key = "games-header") {
            val shape = RoundedCornerShape(topStart = tile, topEnd = tile, bottomStart = if (matches.isEmpty()) tile else 0.dp, bottomEnd = if (matches.isEmpty()) tile else 0.dp)
            SectionHeader(
                "Games",
                Modifier.animateItem().then(if (phone) Modifier.background(fill, shape).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (matches.isEmpty()) 16.dp else 8.dp) else Modifier),
                readout = "${matches.size}",
            )
        }
        itemsIndexed(matches.asReversed(), key = { _, match -> match.id }) { index, match ->
            val last = index == matches.lastIndex
            val rowShape = RoundedCornerShape(bottomStart = if (last) tile else 0.dp, bottomEnd = if (last) tile else 0.dp)
            val glow by animateFloatAsState(if (match.id == fresh) 1f else 0f, if (match.id == fresh) snap() else motion(Motion.LONG * 3), label = "fresh-game")
            Box(Modifier.animateItem().then(if (phone) Modifier.background(fill, rowShape).padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = if (last) 12.dp else 2.dp) else Modifier)) {
                Row(
                    Modifier.fillMaxWidth().clip(LockOnShapes.button).background(scheme.primary.copy(alpha = 0.24f * glow))
                        .clickable(onClickLabel = "Edit this game") { editingId = match.id }.padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // A neutral tag for wins and draws and a red tint for losses, each with its word (I4).
                    val (word, tone, ink) = when (match.result) {
                        MatchResult.WIN -> Triple("Win", scheme.onSurface.copy(alpha = 0.08f), scheme.onSurface)
                        MatchResult.LOSS -> Triple("Loss", scheme.error.copy(alpha = 0.16f), scheme.error)
                        MatchResult.DRAW -> Triple("Draw", scheme.onSurface.copy(alpha = 0.08f), scheme.onSurfaceVariant)
                    }
                    Box(Modifier.size(48.dp, 28.dp).clip(RoundedCornerShape(8.dp)).background(tone), contentAlignment = Alignment.Center) {
                        Text(word, style = MaterialTheme.typography.labelMedium, color = ink, maxLines = 1)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(match.opponent.ifBlank { "Opponent not noted" }, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            color = if (match.opponent.isBlank()) scheme.onSurfaceVariant else scheme.onSurface)
                        if (match.notes.isNotBlank()) Text(match.notes, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    match.wentFirst?.let { first ->
                        // One width for both, so the badges make a tidy column.
                        Text(
                            if (first) "Went 1st" else "Went 2nd", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1,
                            modifier = Modifier.width(72.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, scheme.onSurface.copy(alpha = 0.24f), RoundedCornerShape(8.dp)).padding(vertical = 4.dp),
                        )
                    }
                    Text(dayLabel(match.playedAt), Modifier.widthIn(min = 56.dp), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, textAlign = TextAlign.End, maxLines = 1)
                }
            }
        }
    }
    VerticalScrollBar(listState, Modifier.align(Alignment.TopEnd))
    }
    // Above the nav bar, and above the keyboard while it's open: the window itself doesn't shrink for it.
    val aboveBar = WindowInsets(bottom = LocalNavClearance.current + 4.dp)
    val aboveKeyboard = WindowInsets.ime.add(WindowInsets(bottom = 8.dp))
    if (phone) logPanel(Modifier.windowInsetsPadding(aboveBar.union(aboveKeyboard)).padding(start = 16.dp, end = 16.dp, top = 8.dp))
    }
    editing?.let { match ->
        EditMatchDialog(
            match, opponentSuggestions,
            onDismiss = { editingId = null },
            onSave = { edited -> editingId = null; if (edited != match) onEdit(match, edited) },
            onDelete = { editingId = null; onDelete(match) },
        )
    }
}

/** Every part of a logged game can change: its result, opponent, turn order and note. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditMatchDialog(
    match: MatchRecord,
    opponentSuggestions: List<String>,
    onDismiss: () -> Unit,
    onSave: (MatchRecord) -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var result by rememberSaveable(match.id) { mutableStateOf(match.result) }
    var opponent by rememberSaveable(match.id) { mutableStateOf(match.opponent) }
    var notes by rememberSaveable(match.id) { mutableStateOf(match.notes) }
    var turn by rememberSaveable(match.id) { mutableStateOf(when (match.wentFirst) { true -> 1; false -> 2; null -> 0 }) }
    fun edited() = match.copy(result = result, opponent = opponent.trim(), notes = notes.trim(), wentFirst = when (turn) { 1 -> true; 2 -> false; else -> null })
    GameDialog(
        onDismissRequest = onDismiss,
        title = formatTimestamp(match.playedAt),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MatchResult.entries.forEach { option ->
                        val on = result == option
                        val (tone, ink) = when (option) {
                            MatchResult.WIN -> scheme.primary to scheme.onPrimary
                            MatchResult.LOSS -> scheme.error to scheme.onError
                            MatchResult.DRAW -> drawTone() to onDrawTone()
                        }
                        GameButton(
                            option.label, onClick = { result = option }, modifier = Modifier.weight(1f),
                            style = if (on) GameButtonStyle.PRIMARY else GameButtonStyle.SECONDARY,
                            tone = if (on) tone else null, onTone = if (on) ink else null,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillTextField(opponent, { opponent = it }, "Opponent", Modifier.weight(1f))
                    TurnChoice(turn) { turn = it }
                }
                val picks = opponentSuggestions.filter { opponent.isNotBlank() && it.contains(opponent.trim(), ignoreCase = true) && !it.equals(opponent.trim(), ignoreCase = true) }.take(4)
                RevealValue(picks.takeIf { it.isNotEmpty() }) { names ->
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        names.forEach { PillChip(selected = false, onClick = { opponent = it }, label = { Text(it) }) }
                    }
                }
                PillTextField(notes, { notes = it }, "Note", Modifier.fillMaxWidth())
                // Apart from Save, so a slip doesn't remove the game; removing still offers Undo.
                GameButton("Remove game", onClick = onDelete, style = GameButtonStyle.GHOST, compact = true, tone = scheme.error, icon = LineIcons.Trash)
            }
        },
        confirmButton = { GameButton("Save", onClick = { onSave(edited()) }, compact = true) },
        dismissButton = { GameButton("Cancel", onClick = onDismiss, style = GameButtonStyle.GHOST, compact = true) },
    )
}

/** Wins in a record's bar: the light slate at twilight, the chart slate by day. */
@Composable
private fun winTone(): Color = if (isTwilight()) WinSlateTwilight else ChartSlate

/** Draws in a record's bar: a step quieter than wins. */
@Composable
private fun drawTone(): Color = if (isTwilight()) ChartSlateTwilight else ChartSlate.copy(alpha = 0.5f)

@Composable
private fun onDrawTone(): Color = if (isTwilight()) Color(0xFF1B2147) else Color.White

/** A name, wins against losses as one bar, and the score. */
/** The name column of the play-order rows, wide enough for "Order not logged". */
private val OrderWidth = 128.dp

/** What the record bars' colours mean: Won, Lost, and Drawn when there are draws. */
@Composable
private fun ResultKey(modifier: Modifier = Modifier, draws: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOfNotNull("Won" to winTone(), ("Drawn" to drawTone()).takeIf { draws }, "Lost" to scheme.error).forEach { (word, tone) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(tone))
                Text(word, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
/** A name, its results as one bar on a scale of [most] games, and the score. */
private fun RecordRow(name: String, record: Record, nameWidth: Dp, most: Int = record.games) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(name, Modifier.width(nameWidth), style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            // Logging a game moves the split instead of redrawing it.
            val wins by animateFloatAsState(record.wins.toFloat(), motion(Motion.LONG), label = "record-wins")
            val draws by animateFloatAsState(record.draws.toFloat(), motion(Motion.LONG), label = "record-draws")
            val losses by animateFloatAsState(record.losses.toFloat(), motion(Motion.LONG), label = "record-losses")
            if (record.games == 0 && wins + draws + losses < 0.01f) Box(Modifier.fillMaxSize().background(scheme.onSurface.copy(alpha = 0.1f)))
            // Wins in the light slate and losses in red, so teal isn't spent on data; one result is one unbroken fill.
            if (wins > 0.01f) Box(Modifier.weight(wins).fillMaxHeight().background(winTone()))
            if (draws > 0.01f) Box(Modifier.weight(draws).fillMaxHeight().background(drawTone()))
            if (losses > 0.01f) Box(Modifier.weight(losses).fillMaxHeight().background(scheme.error))
            // The rest of the scale stays empty, so fewer games make a shorter bar.
            val rest = most - record.games
            if (rest > 0) Box(Modifier.weight(rest.toFloat()).fillMaxHeight())
        }
        FadeSwap(if (record.games == 0) "—" else record.score, Modifier.widthIn(min = 40.dp), contentAlignment = Alignment.CenterEnd) {
            Text(it, style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), textAlign = TextAlign.End, maxLines = 1)
        }
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
                Modifier.fillMaxHeight().clip(LockOnShapes.inset).background(softThumbFill(on))
                    .selectable(on, role = Role.RadioButton) { onChange(if (on) 0 else value) }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = MaterialTheme.typography.labelLarge, color = softThumbTone(on)) }
        }
    }
}
