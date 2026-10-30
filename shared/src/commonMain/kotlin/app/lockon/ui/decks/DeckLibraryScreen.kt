package app.lockon.ui.decks
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import app.lockon.ui.components.VerticalScrollBar
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.hoverable
import app.lockon.ui.components.motion
import app.lockon.ui.components.Motion
import app.lockon.ui.components.ControlSize
import app.lockon.ui.components.GlassIconButton
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
import app.lockon.ui.components.GameMenu
import app.lockon.ui.components.GameMenuItem
import app.lockon.ui.components.GameMenuDivider
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
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
import app.lockon.ui.components.glassFrame
import app.lockon.ui.components.isTwilight
import app.lockon.ui.theme.LockOnShapes
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.graphics.graphicsLayer

private sealed interface LibraryDialog {
    data object New : LibraryDialog
    data class Rename(val deck: DeckSummary) : LibraryDialog
    data class Tags(val deck: DeckSummary) : LibraryDialog
    data class Move(val deck: DeckSummary) : LibraryDialog
    data class Export(val deck: DeckSummary) : LibraryDialog
    data class Delete(val deck: DeckSummary) : LibraryDialog
    /** [with] is a deck dropped on New folder, ticked to start with. */
    data class NewFolder(val with: String? = null) : LibraryDialog
    data class RenameFolder(val folder: String) : LibraryDialog
    data class RemoveFolder(val folder: String, val decks: Int) : LibraryDialog
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
    val keptFolders by container.decks.keptFolders.collectAsState(initial = emptyList())
    val cards by container.catalog.cards.collectAsState()
    val ruleset by container.catalog.ruleset.collectAsState()
    var dialog by remember { mutableStateOf<LibraryDialog?>(null) }
    var tagFilter by remember { mutableStateOf<String?>(null) }
    var folderFilter by remember { mutableStateOf<FolderFilter>(FolderFilter.All) }
    var search by remember { mutableStateOf("") }
    val scope = state.appScope

    val list = decks
    // Folders holding decks, plus the empty ones this device keeps.
    val folders = remember(list, keptFolders) {
        val counts = list.orEmpty().mapNotNull { it.snapshot.folder }.groupingBy { it }.eachCount()
        (counts + keptFolders.filter { it !in counts }.associateWith { 0 }).toList().sortedBy { it.first.lowercase() }
    }
    val unfiled = list.orEmpty().count { it.snapshot.folder == null }
    // Every way of removing a folder asks first.
    val removeFolder: (String) -> Unit = { name ->
        dialog = LibraryDialog.RemoveFolder(name, folders.firstOrNull { it.first == name }?.second ?: 0)
    }
    val renameFolder: (String) -> Unit = { dialog = LibraryDialog.RenameFolder(it) }
    // A folder goes when it is removed, here or by its last deck moving out on another device; then show every deck.
    LaunchedEffect(folders, unfiled) {
        val f = folderFilter
        if ((f is FolderFilter.Named && folders.none { it.first == f.name }) || (f == FolderFilter.Unfiled && unfiled == 0)) {
            folderFilter = FolderFilter.All
        }
    }
    // Moves happen at once with Undo, from the Move dialog or a drop on a folder (UX rule C2).
    fun move(deck: DeckSummary, folder: String?) {
        val previous = deck.snapshot.folder
        if (folder == previous) return
        scope.launch {
            val saved = container.decks.moveToFolder(listOf(deck.deckId), folder)
            val message = if (saved == null) "Took ${deck.snapshot.name} out of its folder" else "Moved ${deck.snapshot.name} to $saved"
            state.notify(message, "Undo") { scope.launch { container.decks.moveToFolder(listOf(deck.deckId), previous) } }
        }
    }
    val drag = remember {
        DeckDrag { deck, spot ->
            when (spot) {
                is DeckDropSpot.Folder -> move(deck, spot.name)
                DeckDropSpot.NewFolder -> dialog = LibraryDialog.NewFolder(with = deck.deckId)
            }
        }
    }
    // A filed deck can be dropped on No folder even when no other deck is unfiled.
    val showUnfiled = unfiled > 0 || drag.deck?.snapshot?.folder != null

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        Column(Modifier.fillMaxSize()) {
        // The title row sits in the band beside the floating navigation on wide windows (UX rule N5).
        // The deck actions. Wide windows put them beside the search bar, so the band keeps space for the
        // title; phones keep them beside the title. New deck is the first slot of the list on both, and
        // offers Import beside a blank deck. Rarely used actions sit in a menu.
        val actions = @Composable {
            var menu by remember { mutableStateOf(false) }
            Box {
                GlassIconButton(Icons.Default.MoreVert, "More actions", { menu = true })
                GameMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    // The open folder's actions, which phones have no rail or right-click to reach.
                    (folderFilter as? FolderFilter.Named)?.let { open ->
                        GameMenuItem("Rename folder", onClick = { menu = false; renameFolder(open.name) })
                        GameMenuItem("Remove folder", onClick = { menu = false; removeFolder(open.name) }, icon = Icons.Default.Delete, danger = true)
                        GameMenuDivider()
                    }
                    GameMenuItem("Back up all decks", onClick = {
                        menu = false
                        scope.launch {
                            val backup = BackupFormat.encode(container.decks.exportBackup())
                            if (files.saveText("lock-on-backup.json", "application/json", backup)) state.notify("Backup saved.")
                        }
                    })
                }
            }
        }
        ScreenHeader(
            // Each deck box says Legal or Draft itself, so the title needs no tally under it.
            if (wide) "Deck hangar" else "Decks",
        ) {
            if (!wide) actions()
        }
        Row(Modifier.fillMaxSize()) {
            if (wide && !list.isNullOrEmpty()) {
                FolderRail(
                    total = list.size, folders = folders, unfiled = unfiled, showUnfiled = showUnfiled, selected = folderFilter, drag = drag,
                    onSelect = { folderFilter = it }, onNewFolder = { dialog = LibraryDialog.NewFolder() },
                    onRename = renameFolder, onRemove = removeFolder,
                )
            }
            Box(Modifier.weight(1f).fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    val searchable = (list?.size ?: 0) > 3
                    if (searchable || wide) Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = if (wide) 12.dp else 16.dp, top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (searchable) PillSearchField(
                            search, { search = it }, placeholder = "Search decks",
                            modifier = Modifier.weight(1f),
                            onClear = if (search.isEmpty()) null else ({ search = "" }),
                        )
                        if (wide) actions()
                    }
                    // Folders (phones only; desktop has the rail) and tags share one scrolling row.
                    val tags = list.orEmpty().flatMap { it.snapshot.tags }.distinct().sorted()
                    val showFolders = !wide && !list.isNullOrEmpty()
                    if (showFolders || tags.isNotEmpty()) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier.weight(1f).fadeEnd().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 40.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (showFolders) {
                                FolderChips(
                                    folders = folders, unfiled = unfiled, showUnfiled = showUnfiled, selected = folderFilter, drag = drag,
                                    onSelect = { folderFilter = it }, onNewFolder = { dialog = LibraryDialog.NewFolder() },
                                    onRename = renameFolder, onRemove = removeFolder,
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
                    // The folder's own actions live on the folder (its menu in the rail, or right-click) and in
                    // More actions, so the header only names it; its count is on the folder too.
                    (folderFilter as? FolderFilter.Named)?.let { selected ->
                        SectionHeader(selected.name, Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp))
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
                                val emptyFolder = folderFilter is FolderFilter.Named && list.none(folderFilter::matches)
                                Text(
                                    if (emptyFolder) "This folder is empty. Create a deck while it's open, or move one here from a deck's menu."
                                    else "No decks here match. Clear the search, or pick another folder or tag.",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                            val gridState = rememberLazyGridState()
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Adaptive(460.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp + LocalNavClearance.current),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // A slot ahead of the decks starts a new one, as in a game's deck list.
                                item(key = "new-deck-slot") {
                                    NewDeckSlot(
                                        Modifier.animateItem(), compact = !wide,
                                        onNew = { dialog = LibraryDialog.New }, onImport = { dialog = LibraryDialog.Import },
                                    )
                                }
                                items(shown, key = { it.deckId }) { deck ->
                                    val colors = remember(deck, cards) {
                                        deck.snapshot.entries.filter { it.section == DeckSection.MAIN }.flatMap { cards[it.cardNumber]?.colors.orEmpty() }.toSet()
                                    }
                                    val legal = remember(deck, cards, ruleset) { DeckValidator.validate(deck.snapshot, cards, ruleset).isLegal }
                                    val cover = remember(deck, cards) { deckCover(deck.snapshot, cards) }
                                    DeckRow(
                                        // The held deck stays in place, faded, while its plate follows the pointer.
                                        Modifier.animateItem().deckDragSource(deck, drag)
                                            .graphicsLayer { alpha = if (drag.deck?.deckId == deck.deckId) 0.45f else 1f },
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
                            }
                            VerticalScrollBar(gridState, Modifier.align(Alignment.TopEnd))
                            }
                        }
                    }
                }
            }
        }
        }
        DeckDragGhost(drag, { deckCover(it.snapshot, cards) }, Modifier.matchParentSize())
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
            move(d.deck, folder)
        }
        is LibraryDialog.NewFolder -> NewFolderDialog(
            decks = list.orEmpty(),
            preselected = if (d.with != null) setOf(d.with) else if (folderFilter == FolderFilter.Unfiled) list.orEmpty().filter { it.snapshot.folder == null }.map { it.deckId }.toSet() else emptySet(),
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
        // Removing a folder only unfiles its decks, but like deleting a deck it asks first and still offers Undo.
        is LibraryDialog.RemoveFolder -> GameDialog(
            onDismissRequest = { dialog = null },
            title = "Remove ${d.folder}?",
            text = {
                Text(
                    when (d.decks) {
                        0 -> "This folder is empty. It will be removed."
                        1 -> "The folder will be removed. Its deck stays in your library, in no folder."
                        else -> "The folder will be removed. Its ${d.decks} decks stay in your library, in no folder."
                    },
                )
            },
            confirmButton = {
                GameButton("Remove", compact = true, onClick = {
                    dialog = null
                    scope.launch {
                        val moved = container.decks.removeFolder(d.folder)
                        state.notify("Removed folder ${d.folder}", "Undo") {
                            scope.launch { container.decks.moveToFolder(moved, d.folder) }
                        }
                    }
                })
            },
            dismissButton = { GameButton("Cancel", onClick = { dialog = null }, style = GameButtonStyle.GHOST, compact = true) },
        )
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
        LibraryDialog.Import -> ImportDialog(
            container, files, state, onDismiss = { dialog = null },
            folders = folders.map { it.first }, folder = (folderFilter as? FolderFilter.Named)?.name,
        )
    }
}

/** Desktop folder list beside the decks (UX rule N4). */
@Composable
private fun FolderRail(
    total: Int,
    folders: List<Pair<String, Int>>,
    unfiled: Int,
    showUnfiled: Boolean,
    selected: FolderFilter,
    drag: DeckDrag,
    onSelect: (FolderFilter) -> Unit,
    onNewFolder: () -> Unit,
    onRename: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val over = drag.over
    Column(
        Modifier.width(240.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(start = 12.dp, top = 4.dp, end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        FolderItem("All decks", total, selected == FolderFilter.All, dropOver = false, Modifier) { onSelect(FolderFilter.All) }
        folders.forEach { (name, count) ->
            val spot = DeckDropSpot.Folder(name)
            FolderContextMenu(name, onRename, onRemove) { openMenu ->
                FolderItem(name, count, selected == FolderFilter.Named(name), over == spot, Modifier.deckDropSpot(drag, spot), onMenu = openMenu) {
                    onSelect(FolderFilter.Named(name))
                }
            }
        }
        if (folders.isNotEmpty() && showUnfiled) {
            val spot = DeckDropSpot.Folder(null)
            FolderItem("No folder", unfiled, selected == FolderFilter.Unfiled, over == spot, Modifier.deckDropSpot(drag, spot)) {
                onSelect(FolderFilter.Unfiled)
            }
        }
        // Dropping a deck here names a new folder with that deck already ticked.
        GameButton(
            "New folder", onClick = onNewFolder, style = if (over == DeckDropSpot.NewFolder) GameButtonStyle.PRIMARY else GameButtonStyle.GHOST,
            icon = Icons.Default.Add, compact = true, modifier = Modifier.padding(top = 4.dp).deckDropSpot(drag, DeckDropSpot.NewFolder),
        )
    }
}

/**
 * A folder in the rail. The selected one is a teal pill, like every other selected pill (UX rule
 * V3); the rest are bare text that tints on hover. The pill's shape alone marks it: no edge bar.
 * A deck held over it lights it the same way, so the drop target reads as the choice.
 * A folder with [onMenu] swaps its count for a menu button while hovered, as sidebars do.
 */
@Composable
private fun FolderItem(
    label: String, count: Int, selected: Boolean, dropOver: Boolean, modifier: Modifier,
    onMenu: (() -> Unit)? = null, onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val fill by animateColorAsState(
        when {
            selected || dropOver -> Accent
            hovered -> scheme.onSurface.copy(alpha = 0.07f)
            else -> Color.Transparent
        },
        motion(Motion.SHORT), label = "folder",
    )
    val lit = selected || dropOver
    val ink = if (lit) OnAccent else scheme.onSurface
    Row(
        modifier.fillMaxWidth().height(ControlSize).clip(LockOnShapes.button).background(fill)
            .hoverable(interaction)
            .selectable(selected, interaction, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = ink, modifier = Modifier.weight(1f))
        if (onMenu != null && hovered) {
            Icon(
                Icons.Default.MoreVert, contentDescription = "Folder actions", tint = ink,
                modifier = Modifier.size(28.dp).clip(LockOnShapes.button).clickable(onClick = onMenu).padding(4.dp),
            )
        } else {
            Text("$count", style = MaterialTheme.typography.labelLarge.merge(TabularNumbers),
                color = if (lit) OnAccent else scheme.onSurfaceVariant)
        }
    }
}

/**
 * Right-clicking a folder opens Rename and Remove folder where the pointer is. [content] gets a
 * way to open the same menu from a button, which hangs it under the folder's end.
 */
@Composable
private fun FolderContextMenu(name: String, onRename: (String) -> Unit, onRemove: (String) -> Unit, content: @Composable (openMenu: () -> Unit) -> Unit) {
    var menuAt by remember { mutableStateOf<Offset?>(null) }
    var width by remember { mutableStateOf(0) }
    var height by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    Box(
        Modifier.onSizeChanged { width = it.width; height = it.height }.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        event.changes.forEach { it.consume() }
                        menuAt = event.changes.first().position
                    }
                }
            }
        },
    ) {
        content { menuAt = Offset(width - with(density) { 180.dp.toPx() }, height.toFloat()) }
        val at = menuAt ?: Offset.Zero
        // The menu hangs from the item's bottom edge, so lift it back up to the click.
        GameMenu(
            expanded = menuAt != null, onDismissRequest = { menuAt = null },
            offset = with(density) { DpOffset(at.x.toDp(), (at.y - height).toDp()) },
        ) {
            GameMenuItem("Rename", onClick = { menuAt = null; onRename(name) })
            GameMenuDivider()
            GameMenuItem("Remove folder", onClick = { menuAt = null; onRemove(name) }, icon = Icons.Default.Delete, danger = true)
        }
    }
}

/** Phone folder chips: the same choices as the desktop list, at the start of the chip row. */
@Composable
private fun FolderChips(
    folders: List<Pair<String, Int>>,
    unfiled: Int,
    showUnfiled: Boolean,
    selected: FolderFilter,
    drag: DeckDrag,
    onSelect: (FolderFilter) -> Unit,
    onNewFolder: () -> Unit,
    onRename: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val over = drag.over
    if (folders.isNotEmpty()) {
        PillChip(selected = selected == FolderFilter.All, onClick = { onSelect(FolderFilter.All) }, label = { Text("All decks") })
        folders.forEach { (name, count) ->
            val spot = DeckDropSpot.Folder(name)
            FolderContextMenu(name, onRename, onRemove) {
                PillChip(
                    selected = selected == FolderFilter.Named(name) || over == spot, onClick = { onSelect(FolderFilter.Named(name)) },
                    label = { Text("$name · $count") }, modifier = Modifier.deckDropSpot(drag, spot),
                )
            }
        }
        if (showUnfiled) {
            val spot = DeckDropSpot.Folder(null)
            PillChip(
                selected = selected == FolderFilter.Unfiled || over == spot, onClick = { onSelect(FolderFilter.Unfiled) },
                label = { Text("No folder") }, modifier = Modifier.deckDropSpot(drag, spot),
            )
        }
    }
    // Stays an assist chip, lighting up only while a deck is held over it.
    if (over == DeckDropSpot.NewFolder) PillChip(
        selected = true, onClick = onNewFolder, label = { Text("New folder") },
        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }, modifier = Modifier.deckDropSpot(drag, DeckDropSpot.NewFolder),
    ) else AssistChip(
        onClick = onNewFolder, label = { Text("New folder") },
        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }, modifier = Modifier.deckDropSpot(drag, DeckDropSpot.NewFolder),
    )
}

/** Name a folder and, if you like, pick decks to put in it. A folder can start empty. */
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
                if (decks.isNotEmpty()) Text("Decks to put in it (optional)", style = MaterialTheme.typography.labelLarge)
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
            }
        },
        confirmButton = {
            GameButton("Create", onClick = { onCreate(name.trim(), chosen.toList()) }, enabled = name.isNotBlank(), compact = true)
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
    // top edge (UX rule V4).
    BoxWithConstraints(
        // Phones get a shorter box with a smaller cover, so more decks fit on one screen.
        modifier.fillMaxWidth().height(if (compact) CompactDeckBoxHeight else DeckBoxHeight)
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
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(base.copy(alpha = 0f), base.copy(alpha = if (dark) 0.6f else 0.3f)))))
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
                    // Clear of the actions button, which floats in the corner.
                    Text(deck.snapshot.name, style = MaterialTheme.typography.titleLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, color = scheme.onSurface, modifier = Modifier.padding(end = 48.dp))
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
                    if (maxWidth >= 300.dp) ProgressGauge("Main", main, ruleset.mainDeckSize, Modifier.fillMaxWidth().padding(end = 16.dp))
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
                            // "In" marks it as a folder, so a name like Testing doesn't read as the deck's state.
                            deck.snapshot.folder?.takeIf { showFolder }?.let { "In $it" },
                            deck.snapshot.tags.joinToString(" · ").ifBlank { null },
                        // Narrow boxes show one detail beside the record, so nothing is cut mid-word.
                        ).let { if (narrow) it.take(1) else it }.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodySmall.merge(TabularNumbers), color = scheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        // In the corner rather than a column of its own, so the gauge and the record run to the edge.
        Box(Modifier.align(Alignment.TopEnd).padding(top = if (compact) 8.dp else 12.dp, end = 10.dp)) {
            // A glass disc keeps the icon readable over cover art, and matches every other icon button.
            GlassIconButton(Icons.Default.MoreVert, "Deck actions", { menu = true })
            GameMenu(expanded = menu, onDismissRequest = { menu = false }) {
                GameMenuItem("Rename", onClick = { menu = false; onRename() })
                GameMenuItem("Move to folder", onClick = { menu = false; onMove() })
                GameMenuItem("Edit tags", onClick = { menu = false; onTags() })
                GameMenuItem("Duplicate", onClick = { menu = false; onDuplicate() })
                GameMenuItem("Export deck", onClick = { menu = false; onExport() })
                GameMenuDivider()
                GameMenuItem("Delete", onClick = { menu = false; onDelete() }, icon = Icons.Default.Delete, danger = true)
            }
        }
        Box(Modifier.matchParentSize().glassFrame())
    }
}

private val DeckBoxHeight = 196.dp
private val CompactDeckBoxHeight = 148.dp

/** The empty deck slot: a dashed plate with a + that lights up on hover. It asks whether to start blank or import. */
@Composable
private fun NewDeckSlot(modifier: Modifier, compact: Boolean, onNew: () -> Unit, onImport: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    val press = remember { MutableInteractionSource() }
    val hovered by press.collectIsHoveredAsState()
    val line = if (hovered) Accent else scheme.outline
    Box(
        // Phones get a short strip so the first decks stay in view.
        modifier.fillMaxWidth().height(if (compact) 64.dp else DeckBoxHeight)
            .hoverable(press)
            .clip(LockOnShapes.panel)
            .background(scheme.onSurface.copy(alpha = if (hovered) 0.06f else 0.025f))
            .drawBehind {
                // The panel's own rounded shape, dashed, so the slot reads as a box still to fill.
                val inset = 0.75.dp.toPx()
                drawRoundRect(
                    line, Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2), CornerRadius(22.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))),
                )
            }
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = { menu = true }),
        contentAlignment = Alignment.Center,
    ) {
        GameMenu(expanded = menu, onDismissRequest = { menu = false }) {
            GameMenuItem("Blank deck", onClick = { menu = false; onNew() }, icon = Icons.Default.Add)
            GameMenuItem("Import deck list", onClick = { menu = false; onImport() })
        }
        val plus = @Composable {
            val side = if (compact) 36.dp else 52.dp
            Box(Modifier.size(side).clip(LockOnShapes.button).background(if (hovered) Accent else scheme.onSurface.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, contentDescription = null, tint = if (hovered) OnAccent else scheme.onSurfaceVariant,
                    modifier = Modifier.size(if (compact) 22.dp else 28.dp))
            }
        }
        val label = @Composable {
            Text("New deck", style = MaterialTheme.typography.titleMedium, color = if (hovered) scheme.onSurface else scheme.onSurfaceVariant)
        }
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { plus(); label() }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { plus(); label() }
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
