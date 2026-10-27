package app.lockon.ui.decks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lockon.AppContainer
import app.lockon.io.DeckListFormat
import app.lockon.model.Card as GameCard
import app.lockon.model.CardColor
import app.lockon.model.CardType
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.Ruleset
import app.lockon.model.DeckSnapshot
import app.lockon.model.MatchRecord
import app.lockon.data.DeckRepository
import kotlin.time.Clock
import kotlin.uuid.Uuid
import app.lockon.platform.FileBridge
import app.lockon.rules.DeckValidator
import app.lockon.ui.AppState
import app.lockon.ui.Screen
import app.lockon.ui.cards.CardSearchPanel
import app.lockon.ui.cards.DeckControls
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import app.lockon.ui.components.Badge
import app.lockon.ui.components.DeckGauge
import app.lockon.ui.components.LockIndicator
import app.lockon.ui.components.RollingNumber
import app.lockon.ui.components.CautionStripes
import app.lockon.ui.components.ColorPips
import app.lockon.ui.components.HudHeader
import app.lockon.ui.components.CardDetailDialog
import app.lockon.ui.components.CardImage
import app.lockon.ui.components.CardThumb
import app.lockon.ui.components.EmptyState
import app.lockon.ui.components.Panel
import app.lockon.ui.io.ExportDialog
import app.lockon.ui.theme.TabularNumbers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AUTOSAVE_DELAY_MS = 400L

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeckEditorScreen(container: AppContainer, state: AppState, files: FileBridge, deckId: String, wide: Boolean, startTab: Int = 0) {
    val summary by container.decks.deck(deckId).collectAsState(initial = null)
    val cards by container.catalog.cards.collectAsState()
    val printings by container.catalog.printings.collectAsState()
    val ruleset by container.catalog.ruleset.collectAsState()
    val allDecks by container.decks.decks.collectAsState(initial = emptyList())

    // The editor works on a local draft and autosaves it. Changes that arrive by sync replace the
    // draft only when nothing local is waiting to be saved.
    var draft by remember(deckId) { mutableStateOf<DeckSnapshot?>(null) }
    var saved by remember(deckId) { mutableStateOf<DeckSnapshot?>(null) }
    LaunchedEffect(summary) {
        val incoming = summary?.snapshot ?: return@LaunchedEffect
        if (draft == null || draft == saved) { draft = incoming; saved = incoming }
    }
    val pending by rememberUpdatedState(draft?.takeIf { it != saved })
    LaunchedEffect(draft) {
        val toSave = draft ?: return@LaunchedEffect
        if (toSave == saved) return@LaunchedEffect
        delay(AUTOSAVE_DELAY_MS)
        state.appScope.launch { container.decks.save(deckId, toSave) }
        saved = toSave
    }
    DisposableEffect(deckId) {
        onDispose { pending?.let { last -> state.appScope.launch { container.decks.save(deckId, last) } } }
    }

    val deck = draft
    if (summary == null && deck == null) {
        EmptyState("Deck not found", "It may have been deleted on another device.") {
            TextButton(onClick = state::goBack) { Text("Back") }
        }
        return
    }
    if (deck == null) return
    val legality = remember(deck, cards, ruleset) { DeckValidator.validate(deck, cards, ruleset) }
    val haptics = LocalHapticFeedback.current
    var wasLegal by remember(deckId) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(legality.isLegal) {
        if (wasLegal == false && legality.isLegal) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        wasLegal = legality.isLegal
    }

    var detail by remember { mutableStateOf<GameCard?>(null) }
    var renaming by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    fun change(card: GameCard, delta: Int, section: DeckSection = DeckSection.playedIn(card)) {
        draft = deck.withQuantity(card.cardNumber, section, (deck.quantityOf(card.cardNumber, section) + delta).coerceAtLeast(0))
    }

    fun move(card: GameCard, toConsidering: Boolean) {
        val played = DeckSection.playedIn(card)
        draft = if (toConsidering) deck.move(card.cardNumber, played, DeckSection.CONSIDERING)
        else deck.move(card.cardNumber, DeckSection.CONSIDERING, played)
    }

    Column(Modifier.fillMaxSize()) {
        // Header: who the deck is on the left, its health grouped on the right (UX rule I2).
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = if (wide) 4.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = state::goBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to decks") }
            if (wide) {
                val cover = remember(deck, cards) { deckCover(deck, cards) }
                if (cover != null) CardImage(cover.card, Modifier.padding(end = 12.dp).width(40.dp), cover.printingId)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(deck.name, style = if (wide) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false))
                    IconButton(onClick = { renaming = true }) { Icon(Icons.Default.Edit, contentDescription = "Rename deck") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val c = MaterialTheme.colorScheme
                    ColorPips(deck.entries.filter { it.section == DeckSection.MAIN }.flatMap { cards[it.cardNumber]?.colors.orEmpty() }.toSet())
                    if (legality.isLegal) Badge("Legal · ${ruleset.name}", c.secondaryContainer, c.onSecondaryContainer)
                    else Badge("Draft · ${legality.errors.size} issues", c.surfaceVariant, c.onSurfaceVariant)
                }
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    DeckGauge("Main", deck.count(DeckSection.MAIN), ruleset.mainDeckSize, Modifier.width(170.dp))
                    DeckGauge("Resource", deck.count(DeckSection.RESOURCE), ruleset.resourceDeckSize, Modifier.width(150.dp))
                    LockIndicator(locked = legality.isLegal, size = 48.dp)
                    OutlinedButton(onClick = { exporting = true }) { Text("Export") }
                }
            } else {
                LockIndicator(locked = legality.isLegal)
                // A labelled icon on phones leaves room for the deck name (UX rule I5).
                IconButton(onClick = { exporting = true }) { Icon(Icons.Default.Share, contentDescription = "Export deck list") }
            }
        }
        if (!wide) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DeckGauge("Main", deck.count(DeckSection.MAIN), ruleset.mainDeckSize, Modifier.weight(1.2f))
                DeckGauge("Resource", deck.count(DeckSection.RESOURCE), ruleset.resourceDeckSize, Modifier.weight(1f))
            }
        }
        if (summary?.hasConflict == true) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                    CautionStripes(Modifier.width(10.dp).fillMaxHeight())
                    Text("This deck was changed on two devices. Both versions are kept.", Modifier.weight(1f).padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { state.open(Screen.Conflict(deckId)) }) { Text("Compare") }
                }
            }
        }

        val deckList: @Composable (Modifier) -> Unit = { modifier ->
            DeckList(
                deck, cards, modifier,
                onCardClick = { detail = it },
                onChange = { card, delta -> change(card, delta) },
                onRemoveConsidering = { change(it, -1, DeckSection.CONSIDERING) },
                onMoveToDeck = { move(it, toConsidering = false) },
                onNotes = { draft = deck.copy(notes = it) },
            )
        }
        val deckColors = remember(deck, cards) {
            deck.entries.filter { it.section == DeckSection.MAIN }.flatMap { cards[it.cardNumber]?.colors.orEmpty() }.toSet()
        }
        val search: @Composable (Modifier) -> Unit = { modifier ->
            CardSearchPanel(
                cards.values, onCardClick = { detail = it }, modifier = modifier,
                focusRequests = state.searchFocusRequests, deckColors = deckColors, wide = wide,
                controls = DeckControls(
                    quantity = { card -> deck.quantityOf(card.cardNumber, DeckSection.playedIn(card)) },
                    // Rule limits shown where you act (UX rule S3), before anything goes wrong.
                    tag = { card ->
                        FitTags(card, deck.quantityOf(card.cardNumber, DeckSection.playedIn(card)),
                            deck.quantityOf(card.cardNumber, DeckSection.CONSIDERING), deckColors, ruleset)
                    },
                    change = { card, delta -> change(card, delta) },
                ),
            )
        }
        val analysis: @Composable (Modifier) -> Unit = { modifier -> DeckAnalysisPanel(deck, cards, legality, modifier) }
        val test: @Composable (Modifier) -> Unit = { modifier -> SampleHandPanel(deck, cards, modifier) }
        // Opponents named on any deck, most played first.
        val opponents = remember(allDecks) {
            allDecks.flatMap { d -> d.snapshot.matches.map { it.opponent.trim() } }.filter { it.isNotEmpty() }
                .groupBy { it.lowercase() }.values.sortedByDescending { it.size }.map { it.last() }
        }
        val matchLog: @Composable (Modifier) -> Unit = { modifier ->
            MatchLogPanel(
                deck.matches, opponents, modifier,
                onLog = { result, opponent, wentFirst, notes ->
                    val match = MatchRecord(Uuid.random().toString(), Clock.System.now().toEpochMilliseconds(), result, opponent, wentFirst, notes)
                    draft = deck.copy(matches = deck.matches + match)
                },
                onDelete = { match ->
                    draft = deck.copy(matches = deck.matches - match)
                    state.notify("Removed a ${match.result.label.lowercase()}", "Undo") {
                        draft = draft?.let { it.copy(matches = DeckRepository.mergeMatches(it.matches + match)) }
                    }
                },
            )
        }

        if (wide) {
            Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Panel(Modifier.weight(1.6f).fillMaxSize()) { search(Modifier.fillMaxSize()) }
                Panel(Modifier.weight(1f).widthIn(min = 320.dp).fillMaxSize()) { deckList(Modifier.fillMaxSize()) }
                Panel(Modifier.weight(0.85f).widthIn(min = 300.dp).fillMaxSize()) {
                    var side by rememberSaveable { mutableStateOf(0) }
                    Column {
                        PrimaryScrollableTabRow(selectedTabIndex = side, edgePadding = 0.dp) {
                            listOf("Analysis", "Test hand", "Matches").forEachIndexed { i, label ->
                                Tab(selected = side == i, onClick = { side = i }, text = { Text(label, maxLines = 1) })
                            }
                        }
                        when (side) {
                            0 -> analysis(Modifier.fillMaxSize())
                            1 -> test(Modifier.fillMaxSize())
                            else -> matchLog(Modifier.fillMaxSize())
                        }
                    }
                }
            }
        } else {
            var tab by rememberSaveable(deckId) { mutableStateOf(startTab) }
            PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp) {
                listOf("Deck", "Add cards", "Analysis", "Test", "Matches").forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label, maxLines = 1) })
                }
            }
            when (tab) {
                0 -> deckList(Modifier.fillMaxSize())
                1 -> search(Modifier.fillMaxSize())
                2 -> analysis(Modifier.fillMaxSize())
                3 -> test(Modifier.fillMaxSize())
                else -> matchLog(Modifier.fillMaxSize())
            }
        }
    }

    detail?.let { card ->
        val section = DeckSection.playedIn(card)
        val considering = deck.quantityOf(card.cardNumber, DeckSection.CONSIDERING)
        val cardPrintings = printings[card.cardNumber].orEmpty()
        CardDetailDialog(card, cardPrintings, onDismiss = { detail = null }) {
            Column(horizontalAlignment = Alignment.End) {
                if (cardPrintings.size > 1 && deck.quantityOf(card.cardNumber, section) > 0) {
                    val preferred = deck.entries.firstOrNull { it.cardNumber == card.cardNumber && it.section == section }?.printingId
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        cardPrintings.forEach { p ->
                            FilterChip(
                                selected = (preferred ?: cardPrintings.first().printingId) == p.printingId,
                                onClick = { draft = deck.withQuantity(card.cardNumber, section, deck.quantityOf(card.cardNumber, section), p.printingId) },
                                label = { Text(p.label) },
                            )
                        }
                    }
                }
                QuantityStepper(deck.quantityOf(card.cardNumber, section), onMinus = { change(card, -1) }, onPlus = { change(card, 1) })
                // Considering is a scratch list; moving keeps the total and never checks rules (UX rule S4).
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Considering $considering",
                        style = MaterialTheme.typography.labelLarge.merge(TabularNumbers),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    when {
                        considering > 0 -> TextButton(onClick = { move(card, toConsidering = false) }) { Text("Move 1 to deck") }
                        deck.quantityOf(card.cardNumber, section) > 0 ->
                            TextButton(onClick = { move(card, toConsidering = true) }) { Text("Move 1 to Considering") }
                        else -> TextButton(onClick = { change(card, 1, DeckSection.CONSIDERING) }) { Text("Consider") }
                    }
                }
                // The library shows the deck by this card instead of its highest-level Unit.
                if (deck.quantityOf(card.cardNumber, section) > 0 || deck.coverCard == card.cardNumber) {
                    val isCover = deck.coverCard == card.cardNumber
                    TextButton(onClick = { draft = deck.copy(coverCard = if (isCover) null else card.cardNumber) }) {
                        Text(if (isCover) "Stop using as deck cover" else "Use as deck cover")
                    }
                }
            }
        }
    }
    if (renaming) {
        TextPromptDialog("Rename deck", "Deck name", deck.name, "Rename", onDismiss = { renaming = false }) {
            renaming = false
            if (it.isNotBlank()) draft = deck.copy(name = it.trim())
        }
    }
    if (exporting) {
        ExportDialog(
            title = deck.name, text = DeckListFormat.export(deck, cards),
            suggestedName = deck.name.filter { it.isLetterOrDigit() || it in " -_" }.ifBlank { "deck" } + ".txt",
            files = files, state = state, onDismiss = { exporting = false },
        )
    }
}

@Composable
private fun DeckList(
    deck: DeckSnapshot,
    cards: Map<String, GameCard>,
    modifier: Modifier,
    onCardClick: (GameCard) -> Unit,
    onChange: (GameCard, Int) -> Unit,
    onRemoveConsidering: (GameCard) -> Unit,
    onMoveToDeck: (GameCard) -> Unit,
    onNotes: (String) -> Unit,
) {
    // Grouped by card type like a printed list, each under a HUD header with its count (UX rule V2).
    val groups = remember(deck, cards) {
        val order = compareBy<DeckEntry>({ cards[it.cardNumber]?.level ?: 99 }, { cards[it.cardNumber]?.cost ?: 99 }, { it.cardNumber })
        val main = deck.entries.filter { it.section == DeckSection.MAIN }
        val byType = CardType.entries.filter { !it.isResource }.map { type ->
            "${type.label}s" to main.filter { cards[it.cardNumber]?.type == type }.sortedWith(order)
        } + ("Unknown cards" to main.filter { cards[it.cardNumber] == null })
        byType.filter { it.second.isNotEmpty() } +
            ("Resources" to deck.entries.filter { it.section == DeckSection.RESOURCE }.sortedWith(order))
    }
    LazyColumn(modifier) {
        item(key = "main-total") {
            HudHeader("Main deck", Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), readout = "${deck.count(DeckSection.MAIN)}")
        }
        if (deck.entries.isEmpty()) {
            item(key = "empty-deck") {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LockIndicator(locked = false, size = 72.dp)
                    Text("No cards yet", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Tap + on any card to add a copy. A deck needs 50 main-deck cards in up to two colors, and 10 resources.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else if (deck.count(DeckSection.MAIN) == 0) {
            item(key = "empty-main") {
                Text(
                    "Add Units, Pilots, Commands and Bases from the card list.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        for ((label, entries) in groups) {
            item(key = "header-$label") {
                Text(
                    "$label · ${entries.sumOf { it.quantity }}",
                    style = MaterialTheme.typography.labelLarge.merge(TabularNumbers),
                    color = if (label == "Resources") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = if (label == "Resources") 20.dp else 12.dp, bottom = 2.dp),
                )
            }
            if (label == "Resources" && entries.isEmpty()) {
                item(key = "empty-resource") {
                    Text(
                        "Add resource cards from the card list.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            items(entries, key = { "${it.section}-${it.cardNumber}" }) { entry ->
                val card = cards[entry.cardNumber]
                Row(
                    Modifier.animateItem().fillMaxWidth().clickable(enabled = card != null) { card?.let(onCardClick) }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CardThumb(card)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(card?.name ?: "Unknown card", maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge)
                        Text(entry.cardNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (card != null) {
                        QuantityStepper(entry.quantity, onMinus = { onChange(card, -1) }, onPlus = { onChange(card, 1) })
                    } else {
                        Text("×${entry.quantity}")
                    }
                }
            }
        }
        val considering = deck.entries.filter { it.section == DeckSection.CONSIDERING }
            .sortedWith(compareBy({ cards[it.cardNumber]?.level ?: 99 }, { it.cardNumber }))
        item(key = "considering-header") {
            HudHeader(
                "Considering", Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                readout = "${considering.sumOf { it.quantity }}",
            )
            Text(
                if (considering.isEmpty()) "Cards you're weighing up. Open a card and choose Consider to park it here."
                else "Not counted toward the deck or its legality.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        items(considering, key = { "considering-${it.cardNumber}" }) { entry ->
            val card = cards[entry.cardNumber]
            Row(
                Modifier.animateItem().fillMaxWidth().clickable(enabled = card != null) { card?.let(onCardClick) }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardThumb(card)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(card?.name ?: "Unknown card", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                    Text("${entry.cardNumber} · ×${entry.quantity}", style = MaterialTheme.typography.bodySmall.merge(TabularNumbers),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (card != null) {
                    // One tap per copy (UX rule S1): move one into the deck, or drop one.
                    TextButton(onClick = { onMoveToDeck(card) }) { Text("To deck") }
                    IconButton(onClick = { onRemoveConsidering(card) }) {
                        Text("−", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
        item(key = "notes") {
            var editing by remember { mutableStateOf(false) }
            if (deck.notes.isBlank() && !editing) {
                TextButton(onClick = { editing = true }, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) { Text("Add notes") }
            } else {
                OutlinedTextField(
                    value = deck.notes, onValueChange = onNotes,
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    minLines = 3,
                )
            }
        }
    }
}

/** "Max" at the copy limit and "Off-color" when a card would add a third color (UX rule S3). */
@Composable
private fun FitTags(card: GameCard, quantity: Int, considering: Int, deckColors: Set<CardColor>, ruleset: Ruleset) {
    val c = MaterialTheme.colorScheme
    val limit = ruleset.restricted[card.cardNumber] ?: if (card.type.isResource) ruleset.resourceMaxCopies else ruleset.maxCopies
    when {
        card.cardNumber in ruleset.banned -> Badge("Banned", c.errorContainer, c.onErrorContainer)
        limit != null && quantity >= limit -> Badge("Max", c.primaryContainer, c.onPrimaryContainer)
        card.colors.isNotEmpty() && (deckColors + card.colors).size > ruleset.maxColors ->
            Badge("Off-color", c.surfaceVariant, c.onSurfaceVariant)
        quantity == 0 && considering > 0 -> Badge("Considering", c.secondaryContainer, c.onSecondaryContainer)
    }
}

@Composable
fun QuantityStepper(quantity: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onMinus() }, enabled = quantity > 0) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Box(Modifier.widthIn(min = 20.dp), contentAlignment = Alignment.Center) {
            RollingNumber(
                quantity,
                style = MaterialTheme.typography.titleMedium.merge(TabularNumbers),
                color = if (quantity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onPlus() }) {
            Icon(Icons.Default.Add, contentDescription = "Add one")
        }
    }
}
