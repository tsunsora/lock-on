package app.lockon.ui.decks
import app.lockon.ui.components.Reveal
import app.lockon.ui.components.RevealValue
import app.lockon.ui.components.FadeSwap
import app.lockon.ui.components.settleAlpha
import app.lockon.ui.components.softThumbFill
import app.lockon.ui.components.softThumbTone
import androidx.compose.runtime.derivedStateOf
import app.lockon.ui.components.clearOfNav
import app.lockon.ui.components.revealOut
import app.lockon.ui.components.revealIn
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import app.lockon.ui.components.softThumbInk
import app.lockon.ui.components.softThumb
import app.lockon.ui.components.glassColors
import androidx.compose.foundation.shape.RoundedCornerShape
import app.lockon.ui.components.LineIcons
import app.lockon.ui.components.rememberPrefetchingGridState
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
import app.lockon.ui.components.MenuChip
import app.lockon.ui.components.GameDialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import app.lockon.model.CardColor
import app.lockon.model.Ruleset
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
import app.lockon.ui.components.lighten
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import app.lockon.ui.components.CardBack
import app.lockon.ui.components.CardImage
import app.lockon.ui.components.CardFan
import app.lockon.ui.components.ColorPips
import app.lockon.ui.components.pressWash
import androidx.compose.foundation.interaction.MutableInteractionSource
import app.lockon.ui.components.pressScale
import app.lockon.ui.theme.TabularNumbers
import app.lockon.ui.theme.cardColor
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import app.lockon.ui.components.GameMenu
import app.lockon.ui.components.GameMenuItem
import app.lockon.ui.components.GameMenuDivider
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
import app.lockon.ui.components.collectWhileShown
import app.lockon.ui.components.LocalScreenShown
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
import app.lockon.ui.io.NewDeckDialog
import app.lockon.ui.Tab
import androidx.compose.ui.focus.focusRequester
import app.lockon.ui.components.glass
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import app.lockon.platform.ageLabel
import app.lockon.ui.components.Glyphs
import app.lockon.ui.components.StateTag
import app.lockon.ui.components.selectionEdge
import app.lockon.model.Card
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import app.lockon.ui.components.CARD_ASPECT
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.SolidColor
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
    data object Paste : LibraryDialog
    data class Rename(val deck: DeckSummary) : LibraryDialog
    data class Move(val deck: DeckSummary) : LibraryDialog
    data class Export(val deck: DeckSummary) : LibraryDialog
    data class Delete(val deck: DeckSummary) : LibraryDialog
    /** [with] is a deck dropped on New folder, which goes into it. */
    data class NewFolder(val with: String? = null) : LibraryDialog
    data class RenameFolder(val folder: String) : LibraryDialog
    data class RemoveFolder(val folder: String, val decks: Int) : LibraryDialog
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
    // While a deck is open this screen is kept out of sight, and takes up the deck's saves when it shows again.
    val decks by container.decks.liveDecks.collectWhileShown<List<DeckSummary>?>(null)
    val keptFolders by container.decks.keptFolders.collectAsState(initial = emptyList())
    val cards by container.catalog.cards.collectAsState()
    val ruleset by container.catalog.ruleset.collectAsState()
    var dialog by remember { mutableStateOf<LibraryDialog?>(null) }
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
        var deckSort by rememberSaveable { mutableStateOf(DeckSort.LAST_EDITED) }
        Column(Modifier.fillMaxSize()) {
        // The title row sits in the band beside the floating navigation on wide windows (UX rule N5).
        // The deck actions. Wide windows put them beside the search bar, so the band keeps space for the
        // title; phones keep New deck in the thumb zone. New deck also takes a pasted list or a backup.
        // Backing up every deck is in Settings.
        val actions = @Composable {
            if (wide) GameButton("New deck", onClick = { dialog = LibraryDialog.New }, icon = LineIcons.Plus, compact = true)
        }
        val searchFocus = remember { androidx.compose.ui.focus.FocusRequester() }
        if (wide) ScreenHeader("Decks")
        else Row(
            Modifier.fillMaxWidth().clearOfNav().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Decks", style = MaterialTheme.typography.headlineMedium, maxLines = 1, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.width(2.dp))
            if (!list.isNullOrEmpty()) {
                PillSearchField(
                    search, { search = it }, placeholder = "Search",
                    modifier = Modifier.weight(1f).focusRequester(searchFocus),
                    onClear = if (search.isEmpty()) null else ({ search = "" }),
                )
                Box {
                    var sortMenu by remember { mutableStateOf(false) }
                    GlassIconButton(LineIcons.Sort, "Sort: ${deckSort.label}", { sortMenu = true })
                    GameMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        DeckSort.entries.forEach { option ->
                            GameMenuItem(option.label, onClick = { deckSort = option; sortMenu = false }, selected = option == deckSort)
                        }
                    }
                }
            }
        }
        // This screen stays built while hidden, so only Ctrl+F presses made while it shows count.
        val focusSeen = remember { intArrayOf(state.searchFocusRequests) }
        LaunchedEffect(state.searchFocusRequests) {
            if (state.searchFocusRequests != focusSeen[0]) {
                focusSeen[0] = state.searchFocusRequests
                if (state.current == null && state.tab == Tab.DECKS) runCatching { searchFocus.requestFocus() }
            }
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
                    // Desktop always has the search bar beside the actions; phones only once there's a list to search.
                    val searchable = wide && !list.isNullOrEmpty()
                    if (searchable) Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = if (wide) 12.dp else 16.dp, top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (searchable) PillSearchField(
                            search, { search = it }, placeholder = "Search decks",
                            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().weight(1f, fill = false).focusRequester(searchFocus),
                            onClear = if (search.isEmpty()) null else ({ search = "" }),
                            shortcut = "Ctrl F",
                        )
                        if (wide && !list.isNullOrEmpty()) Box {
                            var sortMenu by remember { mutableStateOf(false) }
                            GameButton(deckSort.label, onClick = { sortMenu = true }, style = GameButtonStyle.SECONDARY, icon = Glyphs.Sort, trailingIcon = LineIcons.Chevd, compact = true,
                                outline = if (app.lockon.platform.isMobile) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f) else null)
                            GameMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                DeckSort.entries.forEach { option ->
                                    GameMenuItem(option.label, onClick = { deckSort = option; sortMenu = false }, selected = option == deckSort)
                                }
                            }
                        }
                        if (wide) actions()
                    }
                    // Phones' folders (desktop has the rail). They pick a folder from one chip's menu, flanked
                    // by New folder and the open folder's actions; while a deck is held the folders spread out
                    // as chips again, so it can be dropped on one.
                    if (!wide && !list.isNullOrEmpty()) Row(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp).fadeEnd().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FadeSwap(drag.deck == null, contentAlignment = Alignment.CenterStart) { resting ->
                            if (resting) {
                                FolderTabs(list.size, folders, unfiled, showUnfiled, folderFilter) { folderFilter = it }
                            } else {
                                FolderChips(
                                    folders = folders, unfiled = unfiled, showUnfiled = showUnfiled, selected = folderFilter, drag = drag,
                                    onSelect = { folderFilter = it }, onNewFolder = { dialog = LibraryDialog.NewFolder() },
                                    onRename = renameFolder, onRemove = removeFolder,
                                )
                            }
                        }
                    }
                    // The folder's own actions live on the folder (its menu in the rail, or right-click) and in
                    // More actions, so the header only names it; its count is on the folder too.
                    // Phones name the open folder on its chip instead.
                    if (wide) RevealValue(folderFilter as? FolderFilter.Named) { selected ->
                        SectionHeader(selected.name, Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp))
                    }
                    when {
                        list == null -> Unit
                        list.isEmpty() -> NoDecks(
                            onNew = { dialog = LibraryDialog.New },
                            onPaste = { dialog = LibraryDialog.Paste },
                            onFromDevice = { state.select(Tab.SYNC) },
                        )
                        else -> {
                            val shown = remember(list, folderFilter, search, deckSort) {
                                list.filter {
                                    folderFilter.matches(it) &&
                                        it.snapshot.name.contains(search.trim(), ignoreCase = true)
                                }.let(deckSort::sorted)
                            }
                            Reveal(shown.isEmpty()) {
                                val emptyFolder = folderFilter is FolderFilter.Named && list.none(folderFilter::matches)
                                Text(
                                    if (emptyFolder) "This folder is empty. Create a deck while it's open, or move one here from a deck's menu."
                                    else "No decks here match. Clear the search, or pick another folder.",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                            val gridState = rememberPrefetchingGridState()
                            // A new deck (made, imported or duplicated) lands at the top, but the grid holds on to
                            // the deck it showed first and would leave the new one above the view. It scrolls up to it.
                            // Decks only revealed by a cleared search or another folder were there all along, so it
                            // tracks the whole library, not what is shown.
                            val seen = remember { mutableSetOf<String>() }
                            // A deck made while this screen was out of sight (its editor opens over it) arrives as
                            // the screen comes back, still faded out, so the grid is simply there, as it used to be.
                            val away = remember { booleanArrayOf(false) }
                            if (!LocalScreenShown.current) away[0] = true
                            LaunchedEffect(list) {
                                val hadSome = seen.isNotEmpty()
                                val added = list.map { it.deckId }.filter { it !in seen }
                                seen += added
                                val fresh = shown.indexOfFirst { it.deckId in added }
                                val wasAway = away[0]
                                away[0] = false
                                if (hadSome && fresh >= 0) { if (wasAway) gridState.scrollToItem(fresh) else gridState.animateScrollToItem(fresh) }
                            }
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Adaptive(if (wide) 320.dp else 460.dp),
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
                                    // Derived, so a drag starting recomposes only the row it picked up.
                                    val held by remember(deck.deckId) { derivedStateOf { drag.deck?.deckId == deck.deckId } }
                                    DeckRow(
                                        // The held deck stays in place, faded, while its plate follows the pointer.
                                        Modifier.animateItem().deckDragSource(deck, drag).settleAlpha(if (held) 0.45f else 1f),
                                        deck, legal, colors, ruleset, cover, compact = !wide,
                                        showFolder = folderFilter == FolderFilter.All,
                                        onOpen = { state.open(if (deck.hasConflict) Screen.Conflict(deck.deckId) else Screen.DeckEditor(deck.deckId)) },
                                        onRename = { dialog = LibraryDialog.Rename(deck) },
                                        onMove = { dialog = LibraryDialog.Move(deck) },
                                        onDuplicate = { scope.launch { container.decks.duplicate(deck.deckId); state.notify("Deck duplicated") } },
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
                // In thumb reach on phones (UX rule R3), and compact so it covers less of the last deck.
                // Wide windows have it beside the search bar instead.
            }
        }
        }
                if (!wide) {
                    val named = (folderFilter as? FolderFilter.Named)?.name
                    DeckActionsButton(
                        buildList {
                            add(DeckAction("New deck", LineIcons.Plus, primary = true) { dialog = LibraryDialog.New })
                            add(DeckAction("New folder", LineIcons.Folderplus) { dialog = LibraryDialog.NewFolder() })
                            if (named != null) {
                                add(DeckAction("Rename folder", LineIcons.Edit) { renameFolder(named) })
                                add(DeckAction("Remove folder", LineIcons.Trash) { removeFolder(named) })
                            }
                        },
                    )
                }
        DeckDragGhost(drag, { deckCover(it.snapshot, cards) }, Modifier.matchParentSize())
    }

    when (val d = dialog) {
        null -> Unit
        // A deck made while a folder is open starts in that folder.
        LibraryDialog.New, LibraryDialog.Paste -> NewDeckDialog(
            container, files, state, onDismiss = { dialog = null },
            folders = folders.map { it.first }, folder = (folderFilter as? FolderFilter.Named)?.name,
            startPasting = d == LibraryDialog.Paste,
        )
        is LibraryDialog.Rename -> TextPromptDialog("Rename deck", "Deck name", d.deck.snapshot.name, confirm = "Rename", onDismiss = { dialog = null }) { name ->
            dialog = null
            if (name.isNotBlank()) scope.launch { container.decks.save(d.deck.deckId, d.deck.snapshot.copy(name = name.trim()), d.deck.headRevisionId) }
        }
        is LibraryDialog.Move -> MoveToFolderDialog(
            current = d.deck.snapshot.folder, folders = folders.map { it.first }, onDismiss = { dialog = null },
        ) { folder ->
            dialog = null
            move(d.deck, folder)
        }
        is LibraryDialog.NewFolder -> TextPromptDialog("New folder", "Folder name", "", confirm = "Create", onDismiss = { dialog = null }) { name ->
            dialog = null
            if (name.isNotBlank()) scope.launch {
                val saved = container.decks.moveToFolder(listOfNotNull(d.with), name.trim())
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
            text = { Text("You can undo this right after.") },
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
    }
}

/** The orders the deck grid can take. */
private enum class DeckSort(val label: String) {
    LAST_EDITED("Last edited"), NAME("Name"), WIN_RATE("Win rate");

    fun sorted(decks: List<DeckSummary>): List<DeckSummary> = when (this) {
        LAST_EDITED -> decks.sortedByDescending { it.updatedAt }
        NAME -> decks.sortedBy { it.snapshot.name.lowercase() }
        WIN_RATE -> decks.sortedByDescending { MatchStats.of(it.snapshot.matches).overall.winRate ?: -1 }
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
        Modifier.padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 12.dp).width(232.dp)
            .glass(LockOnShapes.panel, strong = true).verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        FolderItem("All decks", total, selected == FolderFilter.All, dropOver = false, Modifier, icon = LineIcons.Decks) { onSelect(FolderFilter.All) }
        folders.forEach { (name, count) ->
            val spot = DeckDropSpot.Folder(name)
            FolderContextMenu(name, onRename, onRemove) { openMenu ->
                FolderItem(name, count, selected == FolderFilter.Named(name), over == spot, Modifier.deckDropSpot(drag, spot), onMenu = openMenu) {
                    onSelect(FolderFilter.Named(name))
                }
            }
        }
        Reveal(folders.isNotEmpty() && showUnfiled) {
            val spot = DeckDropSpot.Folder(null)
            FolderItem("No folder", unfiled, selected == FolderFilter.Unfiled, over == spot, Modifier.deckDropSpot(drag, spot)) {
                onSelect(FolderFilter.Unfiled)
            }
        }
        // Dropping a deck here names a new folder and puts that deck in it.
        GameButton(
            "New folder", onClick = onNewFolder, style = if (over == DeckDropSpot.NewFolder) GameButtonStyle.PRIMARY else GameButtonStyle.GHOST,
            icon = LineIcons.Plus, compact = true, modifier = Modifier.padding(top = 4.dp).deckDropSpot(drag, DeckDropSpot.NewFolder),
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
    onMenu: (() -> Unit)? = null, icon: ImageVector = Glyphs.Folder, onClick: () -> Unit,
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
    val ink by animateColorAsState(if (lit) OnAccent else scheme.onSurface, motion(Motion.SHORT), label = "folder-ink")
    val quiet by animateColorAsState(if (lit) OnAccent else scheme.onSurfaceVariant, motion(Motion.SHORT), label = "folder-count")
    Row(
        modifier.fillMaxWidth().height(ControlSize).clip(LockOnShapes.button).background(fill)
            .hoverable(interaction)
            .selectable(selected, interaction, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, Modifier.size(18.dp), tint = ink)
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = ink, modifier = Modifier.weight(1f))
        // Fades across rather than swapping under the pointer.
        FadeSwap(onMenu != null && hovered, contentAlignment = Alignment.CenterEnd) { menu ->
            if (menu && onMenu != null) {
                Icon(
                    LineIcons.More, contentDescription = "Folder actions", tint = ink,
                    modifier = Modifier.size(28.dp).clip(LockOnShapes.button).clickable(onClick = onMenu).padding(4.dp),
                )
            } else {
                Text("$count", style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), color = quiet)
            }
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
            GameMenuItem("Remove folder", onClick = { menuAt = null; onRemove(name) }, icon = LineIcons.Trash, danger = true)
        }
    }
}

/**
 * Phones' folder picker: one chip naming the open folder, whose menu lists them all with their counts.
 * New folder sits before it, and the open folder's Rename and Remove after it, as round buttons, so
 * the menu only picks.
 */
@Composable
private fun FolderMenuChip(
    total: Int,
    folders: List<Pair<String, Int>>,
    unfiled: Int,
    selected: FolderFilter,
    onSelect: (FolderFilter) -> Unit,
    onNewFolder: () -> Unit,
    onRename: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val label = when (selected) {
        FolderFilter.All -> "All decks"
        FolderFilter.Unfiled -> "No folder · $unfiled"
        is FolderFilter.Named -> "${selected.name} · ${folders.firstOrNull { it.first == selected.name }?.second ?: 0}"
    }
    GlassIconButton(LineIcons.Plus, "New folder", onNewFolder, compact = true)
    MenuChip(selected = selected != FolderFilter.All, label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp)) }) { close ->
        GameMenuItem("All decks · $total", onClick = { onSelect(FolderFilter.All); close() }, selected = selected == FolderFilter.All)
        folders.forEach { (name, count) ->
            GameMenuItem("$name · $count", onClick = { onSelect(FolderFilter.Named(name)); close() }, selected = selected == FolderFilter.Named(name))
        }
        if (unfiled > 0 && folders.isNotEmpty()) {
            GameMenuItem("No folder · $unfiled", onClick = { onSelect(FolderFilter.Unfiled); close() }, selected = selected == FolderFilter.Unfiled)
        }
    }
    (selected as? FolderFilter.Named)?.let { open ->
        GlassIconButton(LineIcons.Edit, "Rename ${open.name}", { onRename(open.name) }, compact = true)
        GlassIconButton(LineIcons.Trash, "Remove ${open.name}", { onRemove(open.name) }, tint = MaterialTheme.colorScheme.error, compact = true)
    }
}

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
    FadeSwap(over == DeckDropSpot.NewFolder, Modifier.deckDropSpot(drag, DeckDropSpot.NewFolder)) { lit ->
        if (lit) PillChip(
            selected = true, onClick = onNewFolder, label = { Text("New folder") },
            leadingIcon = { Icon(LineIcons.Plus, contentDescription = null) },
        ) else {
            val press = remember { MutableInteractionSource() }
            AssistChip(
                onClick = onNewFolder, label = { Text("New folder") },
                leadingIcon = { Icon(LineIcons.Plus, contentDescription = null) },
                modifier = Modifier.pressScale(press), interactionSource = press,
            )
        }
    }
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
                Reveal(choice == "") {
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
    if (!compact) {
        DeckTile(modifier, deck, legal, colors, ruleset, art, press, menu, { menu = it }, onOpen, onRename, onMove, onDuplicate, onExport, onDelete)
        return
    }
    // A deck on a phone: a strip of its main card's art, then its name, state and gauge.
    Row(
        modifier.fillMaxWidth().height(CompactDeckBoxHeight).clip(LockOnShapes.panel).background(glassColors().strong)
            .clickable(interactionSource = press, indication = pressWash(LockOnShapes.panel, hover = false), onClick = onOpen),
    ) {
        Box(Modifier.width(88.dp).fillMaxHeight().clipToBounds().background(deckArtFill(colors, glassColors().strong))) {
            CardArtwork(art, null, Modifier.matchParentSize().cardCrop(side = 0.18f, top = 0.05f), ContentScale.FillBounds) { Box(Modifier.fillMaxSize()) }
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(deck.snapshot.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = scheme.onSurface)
                ColorPips(colors)
                Box {
                    Icon(
                        LineIcons.More, contentDescription = "Deck actions", tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp).clip(LockOnShapes.button).clickable(role = Role.Button) { menu = true }.padding(10.dp),
                    )
                    GameMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        GameMenuItem("Rename", onClick = { menu = false; onRename() })
                        GameMenuItem("Move to folder", onClick = { menu = false; onMove() })
                        GameMenuItem("Duplicate", onClick = { menu = false; onDuplicate() })
                        GameMenuItem("Export deck", onClick = { menu = false; onExport() })
                        GameMenuDivider()
                        GameMenuItem("Delete", onClick = { menu = false; onDelete() }, icon = LineIcons.Trash, danger = true)
                    }
                }
            }
            val record = remember(deck.snapshot.matches) { MatchStats.of(deck.snapshot.matches).overall }
            Row(Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FadeSwap(legal to deck.hasConflict) { (isLegal, review) -> StateTag(isLegal, review = review) }
                Spacer(Modifier.weight(1f))
                if (record.winRate != null) {
                    Text(record.score, style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), maxLines = 1)
                    Text("${record.winRate}%", style = MaterialTheme.typography.labelMedium.merge(TabularNumbers), color = scheme.onSurfaceVariant, maxLines = 1)
                }
            }
            ProgressGauge("Main deck", deck.snapshot.count(DeckSection.MAIN), ruleset.mainDeckSize, Modifier.fillMaxWidth().padding(end = 8.dp), compact = true, hold = deck.hasConflict, inline = true)
        }
    }
}

/** Phones' folders as a strip of tabs on a soft track; the open one is a white thumb. */
@Composable
private fun FolderTabs(total: Int, folders: List<Pair<String, Int>>, unfiled: Int, showUnfiled: Boolean, selected: FolderFilter, onSelect: (FolderFilter) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.height(44.dp).clip(LockOnShapes.button).background(scheme.onSurface.copy(alpha = 0.09f)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        val tabs = listOf(Triple("All", total, FolderFilter.All as FolderFilter)) + folders.map { (name, count) -> Triple(name, count, FolderFilter.Named(name) as FolderFilter) } +
            if (folders.isNotEmpty() && showUnfiled) listOf(Triple("No folder", unfiled, FolderFilter.Unfiled as FolderFilter)) else emptyList()
        tabs.forEach { (label, count, filter) ->
            val on = selected == filter
            Row(
                Modifier.fillMaxHeight().clip(LockOnShapes.inset).background(softThumbFill(on))
                    .selectable(on, role = Role.Tab) { onSelect(filter) }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = softThumbTone(on), maxLines = 1)
                Text("$count", style = MaterialTheme.typography.labelMedium.merge(TabularNumbers), color = scheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

/** One choice in the phone's + menu. */
private class DeckAction(val label: String, val icon: ImageVector, val primary: Boolean = false, val onClick: () -> Unit)

/**
 * Phones' deck actions, in thumb reach (UX rule R3): a + that opens into a stack of labelled choices,
 * with a fade behind it so it stands clear of the decks scrolling underneath.
 */
@Composable
private fun BoxScope.DeckActionsButton(actions: List<DeckAction>) {
    var open by rememberSaveable { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val turn by animateFloatAsState(if (open) 45f else 0f, motion(Motion.SHORT), label = "deck-actions-turn")
    Box(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(170.dp + LocalNavClearance.current).background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = if (isTwilight()) 0.42f else 0.2f))),
        ),
    )
    AnimatedVisibility(open, Modifier.matchParentSize(), enter = fadeIn(motion(Motion.SHORT)), exit = fadeOut(motion(Motion.SHORT))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).clickable(interactionSource = null, indication = null) { open = false })
    }
    Column(
        Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp + LocalNavClearance.current),
        horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(open, enter = revealIn(), exit = revealOut()) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                actions.forEach { action ->
                    val fill = if (action.primary) scheme.primary else glassColors().strong
                    val ink = if (action.primary) scheme.onPrimary else scheme.onSurface
                    Row(
                        Modifier.height(48.dp).shadow(3.dp, LockOnShapes.button).clip(LockOnShapes.button).background(fill)
                            .clickable(role = Role.Button) { open = false; action.onClick() }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(action.icon, contentDescription = null, Modifier.size(20.dp), tint = ink)
                        Text(action.label, style = MaterialTheme.typography.labelLarge, color = ink, maxLines = 1)
                    }
                }
            }
        }
        Box(
            Modifier.size(60.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(scheme.primary)
                .clickable(role = Role.Button, onClickLabel = if (open) "Close deck actions" else "Open deck actions") { open = !open }
                .semantics { contentDescription = "Deck actions" },
            contentAlignment = Alignment.Center,
        ) { Icon(LineIcons.Plus, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = turn }) }
    }
}

/** Before the first deck: three card backs and the ways to get one. */
@Composable
fun NoDecks(onNew: () -> Unit, onPaste: () -> Unit, onFromDevice: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
        CardFan(emptyList(), cardWidth = 92.dp, spread = 0.72f, tilt = 12f, sideDrop = 12.dp)
        Text("No decks yet", Modifier.padding(top = 12.dp, bottom = 12.dp), style = MaterialTheme.typography.headlineSmall)
        val width = Modifier.widthIn(max = 340.dp).fillMaxWidth()
        GameButton("New deck", onClick = onNew, width, icon = LineIcons.Plus)
        GameButton("Paste a list", onClick = onPaste, width, style = GameButtonStyle.SECONDARY, icon = LineIcons.Paste)
        GameButton("Get decks from a device", onClick = onFromDevice, width, style = GameButtonStyle.SECONDARY, icon = LineIcons.Sync)
    }
}

/**
 * A deck on wide windows, laid out as on a phone: its cover card at the side, then its name and
 * colours, its state, record and age, and the main deck gauge.
 */
@Composable
private fun DeckTile(
    modifier: Modifier,
    deck: DeckSummary,
    legal: Boolean,
    colors: Set<CardColor>,
    ruleset: Ruleset,
    art: String?,
    press: MutableInteractionSource,
    menu: Boolean,
    setMenu: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val hovered by press.collectIsHoveredAsState()
    val panel = app.lockon.ui.components.glassColors().strong
    Row(
        modifier.fillMaxWidth().height(CompactDeckBoxHeight).selectionEdge(LockOnShapes.tile, scheme.primary, hovered).clip(LockOnShapes.tile).background(panel)
            .clickable(interactionSource = press, indication = pressWash(LockOnShapes.tile, hover = false), onClick = onOpen),
    ) {
        // The deck's colours lie under the art, so they show while it loads and if it never does.
        Box(Modifier.width(88.dp).fillMaxHeight().clipToBounds().background(deckArtFill(colors, panel))) {
            if (art == null && colors.isNotEmpty()) {
                Icon(LineIcons.Shield, contentDescription = null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.align(Alignment.Center).size(32.dp))
            }
            CardArtwork(art, null, Modifier.matchParentSize().cardCrop(side = 0.16f, top = 0.035f), ContentScale.FillBounds) {}
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(deck.snapshot.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurface, modifier = Modifier.weight(1f))
                ColorPips(colors)
                Box {
                    Icon(
                        LineIcons.More, contentDescription = "Deck actions", tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp).clip(LockOnShapes.button).clickable(role = Role.Button) { setMenu(true) }.padding(6.dp),
                    )
                    GameMenu(expanded = menu, onDismissRequest = { setMenu(false) }) {
                        GameMenuItem("Rename", onClick = { setMenu(false); onRename() })
                        GameMenuItem("Move to folder", onClick = { setMenu(false); onMove() })
                        GameMenuItem("Duplicate", onClick = { setMenu(false); onDuplicate() })
                        GameMenuItem("Export deck", onClick = { setMenu(false); onExport() })
                        GameMenuDivider()
                        GameMenuItem("Delete", onClick = { setMenu(false); onDelete() }, icon = LineIcons.Trash, danger = true)
                    }
                }
            }
            val count = deck.snapshot.count(DeckSection.MAIN)
            val record = remember(deck.snapshot.matches) { MatchStats.of(deck.snapshot.matches).overall }
            val over = count - ruleset.mainDeckSize
            DeckFacts(
                Modifier.fillMaxWidth().padding(end = 8.dp),
                lead = {
                    FadeSwap(Triple(legal, deck.hasConflict, over)) { (isLegal, review, past) ->
                        when {
                            review -> StateTag(false, review = true, label = "Review", icon = false)
                            past > 0 -> StateTag(false, review = true, label = "$past over", icon = false)
                            else -> StateTag(isLegal)
                        }
                    }
                },
                age = ageLabel(deck.updatedAt),
                record = record.winRate?.let { rate -> "${record.score}" to "$rate%" },
            )
            ProgressGauge("Main deck", count, ruleset.mainDeckSize, Modifier.fillMaxWidth().padding(end = 8.dp), compact = true, hold = deck.hasConflict, inline = true)
        }
    }
}

/**
 * A tile's middle line: its state on the left and its age on the right, always, with the record
 * and win rate beside the age while there's room for them.
 */
@Composable
private fun DeckFacts(modifier: Modifier, lead: @Composable () -> Unit, age: String, record: Pair<String, String>?) {
    val scheme = MaterialTheme.colorScheme
    Layout(
        content = {
            Box { lead() }
            Text(age, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, maxLines = 1, softWrap = false)
            if (record != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(record.first, style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), color = scheme.onSurface, maxLines = 1, softWrap = false)
                    Text(record.second, style = MaterialTheme.typography.labelMedium.merge(TabularNumbers), color = scheme.onSurfaceVariant, maxLines = 1, softWrap = false)
                }
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val gap = 8.dp.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val agePlaced = measurables[1].measure(loose)
        val leadPlaced = measurables[0].measure(loose.copy(maxWidth = (constraints.maxWidth - agePlaced.width - gap).coerceAtLeast(0)))
        val recordPlaced = measurables.getOrNull(2)?.measure(loose)
            ?.takeIf { leadPlaced.width + gap + it.width + gap + agePlaced.width <= constraints.maxWidth }
        val height = maxOf(leadPlaced.height, agePlaced.height, recordPlaced?.height ?: 0).coerceAtLeast(constraints.minHeight)
        layout(constraints.maxWidth, height) {
            leadPlaced.place(0, (height - leadPlaced.height) / 2)
            val ageX = constraints.maxWidth - agePlaced.width
            agePlaced.place(ageX, (height - agePlaced.height) / 2)
            recordPlaced?.place(ageX - gap - recordPlaced.width, (height - recordPlaced.height) / 2)
        }
    }
}

/**
 * Lays a whole card out larger than this space, so the space shows only its illustration: [side] of
 * the card's width is cut from each side, past the cost and the set code, and [top] of its height from
 * the top, leaving the bottom edge above the name.
 * Laying it out at that size also decodes it at that size, so the crop stays sharp.
 */
private fun Modifier.cardCrop(side: Float, top: Float): Modifier = layout { measurable, constraints ->
    val width = (constraints.maxWidth / (1f - 2f * side)).roundToInt()
    val height = (width / CARD_ASPECT).roundToInt()
    val card = measurable.measure(Constraints.fixed(width, height))
    layout(constraints.maxWidth, constraints.maxHeight) { card.place(-(width * side).roundToInt(), -(height * top).roundToInt()) }
}

/** A deck's colours at half strength over the panel, split evenly if it has two, for when there's no art to show. */
@Composable
private fun deckArtFill(colors: Set<CardColor>, panel: Color): Brush {
    val hues = colors.sortedBy { it.ordinal }.map { androidx.compose.ui.graphics.lerp(panel, cardColor(it), 0.55f) }
    return when {
        hues.isEmpty() -> SolidColor(MaterialTheme.colorScheme.surfaceVariant)
        hues.size == 1 -> SolidColor(hues[0])
        else -> Brush.horizontalGradient(0f to hues.first(), 0.5f to hues.first(), 0.5f to hues.last(), 1f to hues.last())
    }
}

private val CompactDeckBoxHeight = 108.dp

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
