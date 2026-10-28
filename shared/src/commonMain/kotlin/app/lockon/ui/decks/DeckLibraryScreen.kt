package app.lockon.ui.decks
import app.lockon.ui.components.fadeEnd
import app.lockon.ui.components.LocalNavClearance
import app.lockon.ui.components.PillChip
import app.lockon.ui.components.GameDialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import app.lockon.model.CardColor
import app.lockon.model.Ruleset
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import app.lockon.ui.components.CardBack
import app.lockon.ui.components.CardImage
import app.lockon.ui.components.ColorPips
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import app.lockon.ui.components.panelLines
import app.lockon.ui.components.pressScale
import app.lockon.ui.theme.TabularNumbers
import app.lockon.ui.theme.cardColor
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lockon.AppContainer
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.DeckSummary
import app.lockon.platform.FileBridge
import app.lockon.platform.formatTimestamp
import app.lockon.rules.DeckValidator
import app.lockon.rules.MatchStats
import app.lockon.ui.AppState
import app.lockon.ui.Screen
import app.lockon.ui.components.Badge
import app.lockon.ui.components.EmptyState
import app.lockon.ui.components.SectionHeader
import app.lockon.ui.io.ImportDialog
import app.lockon.io.BackupFormat
import kotlinx.coroutines.launch
import app.lockon.ui.components.PillSearchField
import app.lockon.ui.components.ScreenHeader
import app.lockon.ui.components.Accent
import app.lockon.ui.components.OnAccent
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import app.lockon.ui.components.CardArtwork
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.LocalArtwork
import app.lockon.ui.components.ProgressGauge
import app.lockon.ui.components.hoverLift
import app.lockon.ui.components.glassFrame
import app.lockon.ui.components.isTwilight
import app.lockon.ui.components.lighten
import app.lockon.ui.theme.LockOnShapes
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp

private sealed interface LibraryDialog {
    data object New : LibraryDialog
    data class Rename(val deck: DeckSummary) : LibraryDialog
    data class Tags(val deck: DeckSummary) : LibraryDialog
    data class Move(val deck: DeckSummary) : LibraryDialog
    data class Export(val deck: DeckSummary) : LibraryDialog
    data class Delete(val deck: DeckSummary) : LibraryDialog
    data object NewFolder : LibraryDialog
    data class RenameFolder(val folder: String) : LibraryDialog
    data object Import : LibraryDialog
}

/** Which decks the library shows. A folder filters one list rather than opening a screen (UX rule N2). */
private sealed interface FolderFilter {
    data object All : FolderFilter
    data object Unfiled : FolderFilter
    data class Named(val name: String) : FolderFilter

    fun matches(deck: DeckSummary) = when (this) {
        All -> true
        Unfiled -> deck.snapshot.folder == null
        is Named -> deck.snapshot.folder == name
    }
}

@Composable
fun DeckLibraryScreen(container: AppContainer, state: AppState, files: FileBridge) {
    val decks by container.decks.liveDecks.collectAsState(initial = null)
    val cards by container.catalog.cards.collectAsState()
    val ruleset by container.catalog.ruleset.collectAsState()
    var dialog by remember { mutableStateOf<LibraryDialog?>(null) }
    var tagFilter by remember { mutableStateOf<String?>(null) }
    var folderFilter by remember { mutableStateOf<FolderFilter>(FolderFilter.All) }
    var search by remember { mutableStateOf("") }
    val scope = state.appScope

    val list = decks
    val folders = remember(list) {
        list.orEmpty().mapNotNull { it.snapshot.folder }.groupingBy { it }.eachCount().toList().sortedBy { it.first.lowercase() }
    }
    val unfiled = list.orEmpty().count { it.snapshot.folder == null }
    // A folder empties when its last deck moves out, here or on another device; then show every deck.
    LaunchedEffect(folders, unfiled) {
        val f = folderFilter
        if ((f is FolderFilter.Named && folders.none { it.first == f.name }) || (f == FolderFilter.Unfiled && unfiled == 0)) {
            folderFilter = FolderFilter.All
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        Column(Modifier.fillMaxSize()) {
        // The title row sits in the band beside the floating navigation on wide windows (UX rule N5).
        val n = list?.size ?: 0
        ScreenHeader(
            if (wide) "Deck hangar" else "Decks",
            subtitle = if (wide) (if (n == 1) "1 deck in the hangar" else "$n decks in the hangar") else null,
        ) {
            GameButton("Import", onClick = { dialog = LibraryDialog.Import }, style = GameButtonStyle.SECONDARY, compact = true)
            if (wide) {
                GameButton("Back up all", onClick = {
                    scope.launch {
                    val backup = BackupFormat.encode(container.decks.exportBackup())
                    if (files.saveText("lock-on-backup.json", "application/json", backup)) state.notify("Backup saved.")
                }
                }, style = GameButtonStyle.SECONDARY, compact = true)
            } else {
                // Phones keep the title row for the title and Import: rarely used actions sit in a menu.
                var menu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More actions") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Back up all decks") }, onClick = {
                            menu = false
                            scope.launch {
                    val backup = BackupFormat.encode(container.decks.exportBackup())
                    if (files.saveText("lock-on-backup.json", "application/json", backup)) state.notify("Backup saved.")
                }
                        })
                    }
                }
            }
        }
        Row(Modifier.fillMaxSize()) {
            if (wide && !list.isNullOrEmpty()) {
                FolderRail(
                    total = list.size, folders = folders, unfiled = unfiled, selected = folderFilter,
                    onSelect = { folderFilter = it }, onNewFolder = { dialog = LibraryDialog.NewFolder },
                )
            }
            Box(Modifier.weight(1f).fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    if ((list?.size ?: 0) > 3) {
                        PillSearchField(
                            search, { search = it }, placeholder = "Search decks",
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            onClear = if (search.isEmpty()) null else ({ search = "" }),
                        )
                    }
                    // Folders (phones only; desktop has the rail) and tags share one scrolling row.
                    val tags = list.orEmpty().flatMap { it.snapshot.tags }.distinct().sorted()
                    val showFolders = !wide && !list.isNullOrEmpty()
                    if (showFolders || tags.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().fadeEnd().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 40.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (showFolders) {
                                FolderChips(
                                    folders = folders, unfiled = unfiled, selected = folderFilter,
                                    onSelect = { folderFilter = it }, onNewFolder = { dialog = LibraryDialog.NewFolder },
                                )
                                if (tags.isNotEmpty()) Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
                            }
                            if (tags.isNotEmpty()) {
                                PillChip(selected = tagFilter == null, onClick = { tagFilter = null }, label = { Text("All tags") })
                                tags.forEach { tag ->
                                    PillChip(selected = tagFilter == tag, onClick = { tagFilter = tag }, label = { Text(tag) })
                                }
                            }
                        }
                    }
                    (folderFilter as? FolderFilter.Named)?.let { selected ->
                        val count = folders.firstOrNull { it.first == selected.name }?.second ?: 0
                        SectionHeader(selected.name, Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp), readout = "$count") {
                            GameButton("Rename", onClick = { dialog = LibraryDialog.RenameFolder(selected.name) }, style = GameButtonStyle.GHOST, compact = true)
                            GameButton("Remove folder", onClick = {
                                // Removing a folder only unfiles its decks, so it happens at once with Undo (UX rule C2).
                                scope.launch {
                                    val moved = container.decks.removeFolder(selected.name)
                                    state.notify("Removed folder ${selected.name}", "Undo") {
                                        scope.launch { container.decks.moveToFolder(moved, selected.name) }
                                    }
                                }
                            }, style = GameButtonStyle.GHOST, compact = true)
                        }
                    }
                    when {
                        list == null -> Unit
                        list.isEmpty() -> EmptyState(
                            "No decks yet",
                            "Create a deck, import a deck list, or pair with another device to bring your decks over.",
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GameButton("New deck", onClick = { dialog = LibraryDialog.New }, icon = Icons.Default.Add)
                                GameButton("Import", onClick = { dialog = LibraryDialog.Import }, style = GameButtonStyle.SECONDARY)
                            }
                        }
                        else -> {
                            val shown = list.filter {
                                folderFilter.matches(it) && (tagFilter == null || tagFilter in it.snapshot.tags) &&
                                    it.snapshot.name.contains(search.trim(), ignoreCase = true)
                            }
                            if (shown.isEmpty()) {
                                Text(
                                    "No decks here match. Clear the search, or pick another folder or tag.",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(460.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp + LocalNavClearance.current),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(shown, key = { it.deckId }) { deck ->
                                    val colors = remember(deck, cards) {
                                        deck.snapshot.entries.filter { it.section == DeckSection.MAIN }.flatMap { cards[it.cardNumber]?.colors.orEmpty() }.toSet()
                                    }
                                    val legal = remember(deck, cards, ruleset) { DeckValidator.validate(deck.snapshot, cards, ruleset).isLegal }
                                    val cover = remember(deck, cards) { deckCover(deck.snapshot, cards) }
                                    DeckRow(
                                        Modifier.animateItem(),
                                        deck, legal, colors, ruleset, cover, compact = !wide,
                                        showFolder = folderFilter == FolderFilter.All,
                                        onOpen = { state.open(if (deck.hasConflict) Screen.Conflict(deck.deckId) else Screen.DeckEditor(deck.deckId)) },
                                        onRename = { dialog = LibraryDialog.Rename(deck) },
                                        onTags = { dialog = LibraryDialog.Tags(deck) },
                                        onMove = { dialog = LibraryDialog.Move(deck) },
                                        onDuplicate = { scope.launch { container.decks.duplicate(deck.deckId); state.notify("Deck duplicated.") } },
                                        onExport = { dialog = LibraryDialog.Export(deck) },
                                        onDelete = { dialog = LibraryDialog.Delete(deck) },
                                    )
                                }
                                // On desktop, an empty slot after the last deck starts a new one, as in a game's deck list.
                                // Phones already have New deck in the thumb zone (UX rule R3).
                                if (wide) item(key = "new-deck-slot") { NewDeckSlot(Modifier.animateItem()) { dialog = LibraryDialog.New } }
                            }
                        }
                    }
                }
                // In thumb reach on phones (UX rule R3), and compact there so it covers less of the last deck.
                GameButton(
                    if (wide) "New deck" else "New", onClick = { dialog = LibraryDialog.New }, icon = Icons.Default.Add, large = wide,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = if (wide) 20.dp else 14.dp,
                        bottom = (if (wide) 20.dp else 14.dp) + LocalNavClearance.current),
                )
            }
        }
        }
    }

    when (val d = dialog) {
        null -> Unit
        LibraryDialog.New -> TextPromptDialog("New deck", "Deck name", "", confirm = "Create", onDismiss = { dialog = null }) { name ->
            dialog = null
            // A deck made while a folder is open goes into that folder.
            val folder = (folderFilter as? FolderFilter.Named)?.name
            scope.launch {
                val id = container.decks.create(DeckSnapshot(name = name.ifBlank { "Untitled deck" }, folder = folder))
                state.open(Screen.DeckEditor(id, startTab = 1))
            }
        }
        is LibraryDialog.Rename -> TextPromptDialog("Rename deck", "Deck name", d.deck.snapshot.name, confirm = "Rename", onDismiss = { dialog = null }) { name ->
            dialog = null
            if (name.isNotBlank()) scope.launch { container.decks.save(d.deck.deckId, d.deck.snapshot.copy(name = name.trim()), d.deck.headRevisionId) }
        }
        is LibraryDialog.Tags -> TextPromptDialog(
            "Tags", "Comma-separated, e.g. tournament, blue-red", d.deck.snapshot.tags.joinToString(", "),
            confirm = "Save", onDismiss = { dialog = null },
        ) { text ->
            dialog = null
            val tags = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            scope.launch { container.decks.save(d.deck.deckId, d.deck.snapshot.copy(tags = tags), d.deck.headRevisionId) }
        }
        is LibraryDialog.Move -> MoveToFolderDialog(
            current = d.deck.snapshot.folder, folders = folders.map { it.first }, onDismiss = { dialog = null },
        ) { folder ->
            dialog = null
            val previous = d.deck.snapshot.folder
            scope.launch {
                val saved = container.decks.moveToFolder(listOf(d.deck.deckId), folder)
                val message = if (saved == null) "Took ${d.deck.snapshot.name} out of its folder" else "Moved ${d.deck.snapshot.name} to $saved"
                state.notify(message, "Undo") { scope.launch { container.decks.moveToFolder(listOf(d.deck.deckId), previous) } }
            }
        }
        LibraryDialog.NewFolder -> NewFolderDialog(
            decks = list.orEmpty(),
            preselected = if (folderFilter == FolderFilter.Unfiled) list.orEmpty().filter { it.snapshot.folder == null }.map { it.deckId }.toSet() else emptySet(),
            onDismiss = { dialog = null },
        ) { name, ids ->
            dialog = null
            scope.launch {
                val saved = container.decks.moveToFolder(ids, name)
                folderFilter = saved?.let { FolderFilter.Named(it) } ?: FolderFilter.All
            }
        }
        is LibraryDialog.RenameFolder -> TextPromptDialog("Rename folder", "Folder name", d.folder, confirm = "Rename", onDismiss = { dialog = null }) { name ->
            dialog = null
            scope.launch { container.decks.renameFolder(d.folder, name)?.let { folderFilter = FolderFilter.Named(it) } }
        }
        // Asks first, and still offers Undo afterwards.
        is LibraryDialog.Delete -> GameDialog(
            onDismissRequest = { dialog = null },
            title = "Delete ${d.deck.snapshot.name}?",
            text = { Text("Are you sure you want to delete this deck?") },
            confirmButton = {
                GameButton("Delete", compact = true, onClick = {
                    dialog = null
                    scope.launch {
                        container.decks.delete(d.deck.deckId)
                        state.notify("Deleted ${d.deck.snapshot.name}", "Undo") {
                            scope.launch { container.decks.restore(d.deck.deckId, d.deck.snapshot) }
                        }
                    }
                })
            },
            dismissButton = { GameButton("Cancel", onClick = { dialog = null }, style = GameButtonStyle.GHOST, compact = true) },
        )
        is LibraryDialog.Export -> DeckExportDialog(d.deck.snapshot, cards, ruleset, files, state, onDismiss = { dialog = null })
        LibraryDialog.Import -> ImportDialog(container, files, state, onDismiss = { dialog = null })
    }
}

/** Desktop folder list beside the decks (UX rule N4). */
@Composable
private fun FolderRail(
    total: Int,
    folders: List<Pair<String, Int>>,
    unfiled: Int,
    selected: FolderFilter,
    onSelect: (FolderFilter) -> Unit,
    onNewFolder: () -> Unit,
) {
    Column(
        Modifier.width(240.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(start = 12.dp, top = 4.dp, end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SectionHeader("Folders", Modifier.padding(start = 4.dp, bottom = 6.dp))
        FolderItem("All decks", total, selected == FolderFilter.All) { onSelect(FolderFilter.All) }
        folders.forEach { (name, count) ->
            FolderItem(name, count, selected == FolderFilter.Named(name)) { onSelect(FolderFilter.Named(name)) }
        }
        if (folders.isNotEmpty() && unfiled > 0) {
            FolderItem("No folder", unfiled, selected == FolderFilter.Unfiled) { onSelect(FolderFilter.Unfiled) }
        }
        GameButton("New folder", onClick = onNewFolder, style = GameButtonStyle.GHOST, icon = Icons.Default.Add, compact = true,
            modifier = Modifier.padding(top = 4.dp))
        if (folders.isEmpty()) {
            Text(
                "Group decks by event, color or anything else. Folders sync with your decks.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

/** A folder in the rail: the selected one is a lit plate with a V-fin edge. */
@Composable
private fun FolderItem(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().height(48.dp).clip(LockOnShapes.button)
            .background(if (selected) Brush.horizontalGradient(listOf(scheme.primary.copy(alpha = 0.28f), scheme.primary.copy(alpha = 0.04f)))
                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
            .selectable(selected, role = Role.Tab, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(if (selected) scheme.primary else Color.Transparent))
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant, modifier = Modifier.weight(1f).padding(start = 14.dp))
        Text("$count", style = MaterialTheme.typography.titleMedium.merge(TabularNumbers),
            color = if (selected) scheme.primary else scheme.onSurfaceVariant, modifier = Modifier.padding(end = 14.dp))
    }
}

/** Phone folder chips: the same choices as the desktop list, at the start of the chip row. */
@Composable
private fun FolderChips(
    folders: List<Pair<String, Int>>,
    unfiled: Int,
    selected: FolderFilter,
    onSelect: (FolderFilter) -> Unit,
    onNewFolder: () -> Unit,
) {
    if (folders.isNotEmpty()) {
        PillChip(selected = selected == FolderFilter.All, onClick = { onSelect(FolderFilter.All) }, label = { Text("All decks") })
        folders.forEach { (name, count) ->
            PillChip(
                selected = selected == FolderFilter.Named(name), onClick = { onSelect(FolderFilter.Named(name)) },
                label = { Text("$name · $count") },
            )
        }
        if (unfiled > 0) {
            PillChip(selected = selected == FolderFilter.Unfiled, onClick = { onSelect(FolderFilter.Unfiled) }, label = { Text("No folder") })
        }
    }
    AssistChip(
        onClick = onNewFolder, label = { Text("New folder") },
        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
    )
}

/** Name a folder and pick its decks in one step, since a folder exists only while it holds a deck. */
@Composable
private fun NewFolderDialog(
    decks: List<DeckSummary>,
    preselected: Set<String>,
    onDismiss: () -> Unit,
    onCreate: (String, List<String>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf(preselected) }
    GameDialog(
        onDismissRequest = onDismiss,
        title = "New folder",
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Folder name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Decks to put in it", style = MaterialTheme.typography.labelLarge)
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    decks.sortedBy { it.snapshot.name.lowercase() }.forEach { deck ->
                        val checked = deck.deckId in chosen
                        Row(
                            Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox) {
                                chosen = if (it) chosen + deck.deckId else chosen - deck.deckId
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(deck.snapshot.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                deck.snapshot.folder?.let {
                                    Text("Now in $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                if (chosen.isEmpty()) {
                    Text("Pick at least one deck. A folder shows while it holds a deck.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            GameButton("Create", onClick = { onCreate(name.trim(), chosen.toList()) }, enabled = name.isNotBlank() && chosen.isNotEmpty(), compact = true)
        },
        dismissButton = { GameButton("Cancel", onClick = onDismiss, style = GameButtonStyle.GHOST, compact = true) },
    )
}

/** Pick a folder already in use, or type a new one (UX rule C4). */
@Composable
private fun MoveToFolderDialog(current: String?, folders: List<String>, onDismiss: () -> Unit, onMove: (String?) -> Unit) {
    // null is "No folder" and "" is the typed new folder; anything else names an existing folder.
    var choice by remember { mutableStateOf(current) }
    var typed by remember { mutableStateOf("") }
    GameDialog(
        onDismissRequest = onDismiss,
        title = "Move to folder",
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                (listOf("No folder" to null) + folders.map { it to it } + ("New folder" to "")).forEach { (label, value) ->
                    Row(
                        Modifier.fillMaxWidth().selectable(choice == value, role = Role.RadioButton) { choice = value },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(choice == value, onClick = null, modifier = Modifier.padding(12.dp))
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (choice == "") {
                    OutlinedTextField(
                        typed, { typed = it }, label = { Text("Folder name") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            GameButton("Move", onClick = { onMove(if (choice == "") typed else choice) }, enabled = choice != "" || typed.isNotBlank(), compact = true)
        },
        dismissButton = { GameButton("Cancel", onClick = onDismiss, style = GameButtonStyle.GHOST, compact = true) },
    )
}

@Composable
private fun DeckRow(
    modifier: Modifier,
    deck: DeckSummary,
    legal: Boolean,
    colors: Set<CardColor>,
    ruleset: Ruleset,
    cover: DeckCover?,
    compact: Boolean,
    showFolder: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onTags: () -> Unit,
    onMove: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val press = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme
    val dark = isTwilight()
    val base = if (dark) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest
    val lead = colors.minByOrNull { it.ordinal }
    val art = cover?.let { LocalArtwork.current.urlFor(it.card.cardNumber, it.printingId) }
    // A deck box: the cover art fills the plate behind the deck's name, its colours run along the
    // top edge, and the hover lift glows in the deck's lead colour (UX rules V4, I4).
    BoxWithConstraints(
        // Phones get a shorter box with a smaller cover, so more decks fit on one screen.
        modifier.fillMaxWidth().height(if (compact) CompactDeckBoxHeight else DeckBoxHeight)
            .hoverLift(press, glow = lighten(cardColor(lead), 0.15f), shape = LockOnShapes.panel)
            .clip(LockOnShapes.panel)
            .background(base)
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onOpen),
    ) {
        // Narrow boxes spread the art across the whole box, so the crop stays in the art and out of the rules text.
        val narrow = maxWidth < 440.dp
        Box(Modifier.align(Alignment.CenterEnd).fillMaxWidth(if (narrow) 1f else 0.62f).fillMaxHeight()) {
            CardArtwork(art, null, Modifier.fillMaxSize(), ContentScale.Crop, BiasAlignment(0f, if (narrow) -0.62f else -0.45f)) {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(cardColor(lead).copy(alpha = 0.1f), cardColor(lead).copy(alpha = 0.55f)))))
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to base, 0.4f to base, 0.68f to base.copy(alpha = 0.7f), 1f to base.copy(alpha = 0.25f))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, base.copy(alpha = if (dark) 0.6f else 0.3f)))))
        // Colour identity band along the top edge, one segment per deck colour.
        Row(Modifier.fillMaxWidth().height(4.dp)) {
            colors.sortedBy { it.ordinal }.ifEmpty { listOf(null) }.forEach { Box(Modifier.weight(1f).fillMaxHeight().background(cardColor(it))) }
        }
        val coverWidth = if (compact) 76.dp else 104.dp
        Row(Modifier.fillMaxSize().padding(start = if (compact) 14.dp else 16.dp, top = if (compact) 14.dp else 18.dp, bottom = if (compact) 10.dp else 14.dp, end = 4.dp)) {
            if (cover != null) CardImage(cover.card, Modifier.width(coverWidth).align(Alignment.CenterVertically), cover.printingId)
            else CardBack(Modifier.width(coverWidth).align(Alignment.CenterVertically))
            Column(Modifier.weight(1f).fillMaxHeight().padding(start = if (compact) 14.dp else 16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(deck.snapshot.name, style = MaterialTheme.typography.titleLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, color = scheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ColorPips(colors)
                        when {
                            deck.hasConflict -> Badge("Needs review", scheme.errorContainer, scheme.onErrorContainer)
                            legal -> Badge("Legal", scheme.secondaryContainer, scheme.onSecondaryContainer)
                            else -> Badge("Draft", scheme.surfaceVariant, scheme.onSurfaceVariant)
                        }
                    }
                }
                // Full gauge when the box is wide enough, compact on phones so the label never wraps.
                BoxWithConstraints {
                    val main = deck.snapshot.count(DeckSection.MAIN)
                    if (maxWidth >= 300.dp) ProgressGauge("Main", main, ruleset.mainDeckSize, Modifier.widthIn(max = 240.dp).fillMaxWidth())
                    else ProgressGauge("Main", main, ruleset.mainDeckSize, Modifier.fillMaxWidth(), compact = true)
                }
                val record = remember(deck.snapshot.matches) { MatchStats.of(deck.snapshot.matches).overall }
                // As tall with or without a record, so boxes side by side keep their gauges in line.
                Row(Modifier.heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (record.winRate != null) {
                        Text(record.score, style = MaterialTheme.typography.titleMedium.merge(TabularNumbers), color = scheme.primary)
                        Text("${record.winRate}% WIN", style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), color = scheme.onSurface)
                    }
                    Text(
                        // The record leads, so narrow tiles cut the edit time rather than the results.
                        listOfNotNull(
                            deck.snapshot.folder?.takeIf { showFolder },
                            deck.snapshot.tags.joinToString(" · ").ifBlank { null },
                            // Beside a record the edit time no longer fits, and would end the line in "·…".
                            "Edited ${formatTimestamp(deck.updatedAt)}".takeIf { record.winRate == null },
                        // Narrow boxes show one detail beside the record, so nothing is cut mid-word.
                        ).let { if (narrow) it.take(1) else it }.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodySmall.merge(TabularNumbers), color = scheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                }
            }
            Box {
                IconButton(
                    onClick = { menu = true },
                    modifier = Modifier.padding(end = 8.dp).clip(LockOnShapes.button).background(base.copy(alpha = 0.85f)),
                ) { Icon(Icons.Default.MoreVert, contentDescription = "Deck actions", tint = scheme.onSurface) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Move to folder") }, onClick = { menu = false; onMove() })
                    DropdownMenuItem(text = { Text("Edit tags") }, onClick = { menu = false; onTags() })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Export list, image or sheet") }, onClick = { menu = false; onExport() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
        }
        Box(Modifier.matchParentSize().glassFrame(clearing = false))
    }
}

private val DeckBoxHeight = 196.dp
private val CompactDeckBoxHeight = 148.dp

/** The empty deck slot: a dashed plate with a + that lights up on hover. */
@Composable
private fun NewDeckSlot(modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val press = remember { MutableInteractionSource() }
    val hovered by press.collectIsHoveredAsState()
    val line = if (hovered) Accent else scheme.outline
    Box(
        modifier.fillMaxWidth().height(DeckBoxHeight)
            .hoverLift(press, glow = Accent, shape = LockOnShapes.panel)
            .clip(LockOnShapes.panel)
            .background(scheme.onSurface.copy(alpha = if (hovered) 0.06f else 0.025f))
            .drawBehind {
                val cut = 14.dp.toPx()
                val outline = Path().apply {
                    moveTo(cut, 1f); lineTo(size.width - 1f, 1f); lineTo(size.width - 1f, size.height - cut)
                    lineTo(size.width - cut, size.height - 1f); lineTo(1f, size.height - 1f); lineTo(1f, cut); close()
                }
                drawPath(outline, line, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))))
            }
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(52.dp).clip(LockOnShapes.button).background(if (hovered) Accent else scheme.onSurface.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, contentDescription = null, tint = if (hovered) OnAccent else scheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
            }
            Text("New deck", style = MaterialTheme.typography.titleMedium, color = if (hovered) scheme.onSurface else scheme.onSurfaceVariant)
            Text("Name it, then add cards", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
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
    GameDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = { OutlinedTextField(text, { text = it }, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { GameButton(confirm, onClick = { onConfirm(text) }, compact = true) },
        dismissButton = { GameButton("Cancel", onClick = onDismiss, style = GameButtonStyle.GHOST, compact = true) },
    )
}
