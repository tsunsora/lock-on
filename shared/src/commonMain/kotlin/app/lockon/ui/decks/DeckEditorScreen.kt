package app.lockon.ui.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
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
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.platform.FileBridge
import app.lockon.rules.DeckValidator
import app.lockon.ui.AppState
import app.lockon.ui.Screen
import app.lockon.ui.cards.CardSearchPanel
import app.lockon.ui.components.Badge
import app.lockon.ui.components.CardDetailDialog
import app.lockon.ui.components.CardThumb
import app.lockon.ui.components.EmptyState
import app.lockon.ui.components.Panel
import app.lockon.ui.io.ExportDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AUTOSAVE_DELAY_MS = 400L

@Composable
fun DeckEditorScreen(container: AppContainer, state: AppState, files: FileBridge, deckId: String, wide: Boolean) {
    val summary by container.decks.deck(deckId).collectAsState(initial = null)
    val cards by container.catalog.cards.collectAsState()
    val printings by container.catalog.printings.collectAsState()
    val ruleset by container.catalog.ruleset.collectAsState()

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

    var detail by remember { mutableStateOf<GameCard?>(null) }
    var renaming by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    fun change(card: GameCard, delta: Int) {
        val section = if (card.type.isResource) DeckSection.RESOURCE else DeckSection.MAIN
        draft = deck.withQuantity(card.cardNumber, section, (deck.quantityOf(card.cardNumber, section) + delta).coerceAtLeast(0))
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = state::goBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to decks") }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(deck.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false))
                    IconButton(onClick = { renaming = true }) { Icon(Icons.Default.Edit, contentDescription = "Rename deck") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val c = MaterialTheme.colorScheme
                    if (legality.isLegal) Badge("Legal · ${ruleset.name}", c.secondaryContainer, c.onSecondaryContainer)
                    else Badge("Draft · ${legality.errors.size} issues", c.surfaceVariant, c.onSurfaceVariant)
                    Text(
                        "Main ${deck.count(DeckSection.MAIN)}/${ruleset.mainDeckSize} · Resource ${deck.count(DeckSection.RESOURCE)}/${ruleset.resourceDeckSize}",
                        style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = { exporting = true }) { Text("Export") }
        }
        if (summary?.hasConflict == true) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("This deck was changed on two devices. Both versions are kept.", Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { state.open(Screen.Conflict(deckId)) }) { Text("Compare") }
                }
            }
        }

        val deckList: @Composable (Modifier) -> Unit = { modifier ->
            DeckList(deck, cards, modifier, onCardClick = { detail = it }, onChange = ::change, onNotes = { draft = deck.copy(notes = it) })
        }
        val search: @Composable (Modifier) -> Unit = { modifier ->
            CardSearchPanel(cards.values, onCardClick = { detail = it }, modifier = modifier) { card ->
                val section = if (card.type.isResource) DeckSection.RESOURCE else DeckSection.MAIN
                QuantityStepper(deck.quantityOf(card.cardNumber, section), onMinus = { change(card, -1) }, onPlus = { change(card, 1) })
            }
        }
        val analysis: @Composable (Modifier) -> Unit = { modifier -> DeckAnalysisPanel(deck, cards, legality, modifier) }

        if (wide) {
            Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Panel(Modifier.weight(1.1f).fillMaxSize()) { search(Modifier.fillMaxSize()) }
                Panel(Modifier.weight(1f).fillMaxSize()) { deckList(Modifier.fillMaxSize()) }
                Panel(Modifier.weight(0.9f).fillMaxSize()) { analysis(Modifier.fillMaxSize()) }
            }
        } else {
            var tab by rememberSaveable { mutableStateOf(0) }
            PrimaryTabRow(selectedTabIndex = tab) {
                listOf("Deck", "Add cards", "Analysis").forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                }
            }
            when (tab) {
                0 -> deckList(Modifier.fillMaxSize())
                1 -> search(Modifier.fillMaxSize())
                else -> analysis(Modifier.fillMaxSize())
            }
        }
    }

    detail?.let { card ->
        val section = if (card.type.isResource) DeckSection.RESOURCE else DeckSection.MAIN
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
    onNotes: (String) -> Unit,
) {
    LazyColumn(modifier) {
        for (section in DeckSection.entries) {
            val entries = deck.entries.filter { it.section == section }
                .sortedWith(compareBy({ cards[it.cardNumber]?.level ?: 99 }, { cards[it.cardNumber]?.cost ?: 99 }, { it.cardNumber }))
            item(key = "header-$section") {
                Text(
                    "${section.label} (${entries.sumOf { it.quantity }})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            if (entries.isEmpty()) {
                item(key = "empty-$section") {
                    Text(
                        if (section == DeckSection.MAIN) "Add Units, Pilots, Commands and Bases from the card list."
                        else "Add resource cards from the card list.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            items(entries, key = { "${section}-${it.cardNumber}" }) { entry ->
                val card = cards[entry.cardNumber]
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CardThumb(card)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        TextButton(onClick = { card?.let(onCardClick) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                            Text(card?.name ?: "Unknown card", maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
                        }
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
        item(key = "notes") {
            OutlinedTextField(
                value = deck.notes, onValueChange = onNotes,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                minLines = 3,
            )
        }
    }
}

@Composable
fun QuantityStepper(quantity: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMinus, enabled = quantity > 0) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            "$quantity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (quantity > 0) FontWeight.Bold else FontWeight.Normal,
            color = if (quantity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 20.dp),
        )
        IconButton(onClick = onPlus) { Icon(Icons.Default.Add, contentDescription = "Add one") }
    }
}
