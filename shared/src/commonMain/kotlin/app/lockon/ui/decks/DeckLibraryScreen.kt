package app.lockon.ui.decks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import app.lockon.model.CardColor
import app.lockon.model.Ruleset
import app.lockon.ui.components.ColorPips
import app.lockon.ui.theme.TabularNumbers
import app.lockon.ui.theme.cardColor
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lockon.AppContainer
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.DeckSummary
import app.lockon.platform.FileBridge
import app.lockon.platform.formatTimestamp
import app.lockon.rules.DeckValidator
import app.lockon.ui.AppState
import app.lockon.ui.Screen
import app.lockon.ui.components.Badge
import app.lockon.ui.components.EmptyState
import app.lockon.ui.io.ExportDialog
import app.lockon.ui.io.ImportDialog
import app.lockon.io.BackupFormat
import app.lockon.io.DeckListFormat
import kotlinx.coroutines.launch

private sealed interface LibraryDialog {
    data object New : LibraryDialog
    data class Rename(val deck: DeckSummary) : LibraryDialog
    data class Tags(val deck: DeckSummary) : LibraryDialog
    data class Export(val deck: DeckSummary) : LibraryDialog
    data object Import : LibraryDialog
}

@Composable
fun DeckLibraryScreen(container: AppContainer, state: AppState, files: FileBridge) {
    val decks by container.decks.decks.collectAsState(initial = null)
    val cards by container.catalog.cards.collectAsState()
    val ruleset by container.catalog.ruleset.collectAsState()
    var dialog by remember { mutableStateOf<LibraryDialog?>(null) }
    var tagFilter by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    val scope = state.appScope

    Box(Modifier.fillMaxSize()) {
        val list = decks
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { dialog = LibraryDialog.Import }) { Text("Import") }
                OutlinedButton(onClick = {
                    scope.launch {
                        val backup = BackupFormat.encode(container.decks.exportBackup())
                        if (files.saveText("lock-on-backup.json", "application/json", backup)) state.notify("Backup saved.")
                    }
                }) { Text("Back up all") }
            }
            if ((list?.size ?: 0) > 3) {
                OutlinedTextField(
                    search, { search = it },
                    placeholder = { Text("Search decks") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            val tags = list.orEmpty().flatMap { it.snapshot.tags }.distinct().sorted()
            if (tags.isNotEmpty()) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(selected = tagFilter == null, onClick = { tagFilter = null }, label = { Text("All") })
                    tags.forEach { tag ->
                        FilterChip(selected = tagFilter == tag, onClick = { tagFilter = tag }, label = { Text(tag) })
                    }
                }
            }
            when {
                list == null -> Unit
                list.isEmpty() -> EmptyState(
                    "No decks yet",
                    "Create a deck, import a deck list, or pair with another device to bring your decks over.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val shown = list.filter {
                        (tagFilter == null || tagFilter in it.snapshot.tags) && it.snapshot.name.contains(search.trim(), ignoreCase = true)
                    }
                    items(shown, key = { it.deckId }) { deck ->
                        val colors = remember(deck, cards) {
                            deck.snapshot.entries.filter { it.section == DeckSection.MAIN }.flatMap { cards[it.cardNumber]?.colors.orEmpty() }.toSet()
                        }
                        val legal = remember(deck, cards, ruleset) { DeckValidator.validate(deck.snapshot, cards, ruleset).isLegal }
                        DeckRow(
                            deck, legal, colors, ruleset,
                            onOpen = { state.open(if (deck.hasConflict) Screen.Conflict(deck.deckId) else Screen.DeckEditor(deck.deckId)) },
                            onRename = { dialog = LibraryDialog.Rename(deck) },
                            onTags = { dialog = LibraryDialog.Tags(deck) },
                            onDuplicate = { scope.launch { container.decks.duplicate(deck.deckId); state.notify("Deck duplicated.") } },
                            onExport = { dialog = LibraryDialog.Export(deck) },
                            onDelete = {
                                // Undo instead of "Are you sure?" (UX rule C2).
                                scope.launch {
                                    container.decks.delete(deck.deckId)
                                    state.notify("Deleted ${deck.snapshot.name}", "Undo") {
                                        scope.launch { container.decks.restore(deck.deckId, deck.snapshot) }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { dialog = LibraryDialog.New },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("New deck") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    when (val d = dialog) {
        null -> Unit
        LibraryDialog.New -> TextPromptDialog("New deck", "Deck name", "", confirm = "Create", onDismiss = { dialog = null }) { name ->
            dialog = null
            scope.launch {
                val id = container.decks.create(DeckSnapshot(name = name.ifBlank { "Untitled deck" }))
                state.open(Screen.DeckEditor(id, startTab = 1))
            }
        }
        is LibraryDialog.Rename -> TextPromptDialog("Rename deck", "Deck name", d.deck.snapshot.name, confirm = "Rename", onDismiss = { dialog = null }) { name ->
            dialog = null
            if (name.isNotBlank()) scope.launch { container.decks.save(d.deck.deckId, d.deck.snapshot.copy(name = name.trim())) }
        }
        is LibraryDialog.Tags -> TextPromptDialog(
            "Tags", "Comma-separated, e.g. tournament, blue-red", d.deck.snapshot.tags.joinToString(", "),
            confirm = "Save", onDismiss = { dialog = null },
        ) { text ->
            dialog = null
            val tags = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            scope.launch { container.decks.save(d.deck.deckId, d.deck.snapshot.copy(tags = tags)) }
        }
        is LibraryDialog.Export -> ExportDialog(
            title = d.deck.snapshot.name,
            text = DeckListFormat.export(d.deck.snapshot, cards),
            suggestedName = d.deck.snapshot.name.filter { it.isLetterOrDigit() || it in " -_" }.ifBlank { "deck" } + ".txt",
            files = files, state = state, onDismiss = { dialog = null },
        )
        LibraryDialog.Import -> ImportDialog(container, files, state, onDismiss = { dialog = null })
    }
}

@Composable
private fun DeckRow(
    deck: DeckSummary,
    legal: Boolean,
    colors: Set<CardColor>,
    ruleset: Ruleset,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onTags: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainer),
    ) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            // Color identity band: the deck's colors at a glance, with initials beside the name (UX rules V4, I4).
            Column(Modifier.width(6.dp).fillMaxHeight()) {
                val bands = colors.sortedBy { it.ordinal }.ifEmpty { listOf(null) }
                bands.forEach { Box(Modifier.weight(1f).fillMaxWidth().background(cardColor(it))) }
            }
        Row(Modifier.weight(1f).padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(deck.snapshot.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false))
                    ColorPips(colors)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    when {
                        deck.hasConflict -> Badge("Needs review", scheme.errorContainer, scheme.onErrorContainer)
                        legal -> Badge("Legal", scheme.secondaryContainer, scheme.onSecondaryContainer)
                        else -> Badge("Draft", scheme.surfaceVariant, scheme.onSurfaceVariant)
                    }
                    Text(
                        "Main ${deck.snapshot.count(DeckSection.MAIN)}/${ruleset.mainDeckSize} · Resource ${deck.snapshot.count(DeckSection.RESOURCE)}/${ruleset.resourceDeckSize}",
                        style = MaterialTheme.typography.bodySmall.merge(TabularNumbers), color = scheme.onSurfaceVariant,
                    )
                }
                val tags = deck.snapshot.tags.joinToString(" · ")
                Text(
                    listOf(tags, "Edited ${formatTimestamp(deck.updatedAt)}").filter { it.isNotBlank() }.joinToString("  —  "),
                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(4.dp))
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Deck actions") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Edit tags") }, onClick = { menu = false; onTags() })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Export deck list") }, onClick = { menu = false; onExport() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
        }
        }
    }
}

@Composable
fun TextPromptDialog(
    title: String,
    label: String,
    initial: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it }, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
