package app.lockon.ui.cards
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.window.Dialog
import app.lockon.platform.LightSystemBarIcons
import app.lockon.platform.fullWindowDialogProperties
import app.lockon.platform.isMobile
import app.lockon.ui.components.ControlSize
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.isTwilight
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.components.LineIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import app.lockon.ui.components.Glyphs
import app.lockon.ui.components.ViewToggle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.lockon.ui.components.Accent
import app.lockon.ui.components.GlassIconButton
import app.lockon.ui.components.OnAccent
import app.lockon.ui.components.PillShape
import app.lockon.ui.components.glass
import app.lockon.ui.components.motion
import app.lockon.ui.components.LocalNavClearance
import androidx.compose.ui.graphics.Color
import app.lockon.ui.components.PillChip
import app.lockon.ui.components.MenuChip
import app.lockon.ui.components.fadeEnd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import app.lockon.ui.components.PillTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import app.lockon.ui.components.GameMenu
import app.lockon.ui.components.GameMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import app.lockon.model.Card
import app.lockon.model.CardColor
import app.lockon.model.CardQuery
import app.lockon.model.CardSort
import app.lockon.model.CardType
import app.lockon.model.ReleaseOrder
import app.lockon.ui.components.CardTile
import app.lockon.ui.components.CompactStepper
import app.lockon.ui.components.CardRow
import app.lockon.ui.components.EmptyState
import app.lockon.ui.theme.TabularNumbers
import app.lockon.ui.theme.cardColor
import app.lockon.ui.theme.onCardColor
import app.lockon.ui.components.ColorOrbToggle
import app.lockon.ui.components.PillSearchField
import app.lockon.ui.components.VerticalScrollBar
import androidx.compose.foundation.layout.width
import app.lockon.ui.components.ColorDot
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import app.lockon.ui.components.LocalReduceMotion
import app.lockon.ui.components.Motion
import app.lockon.ui.components.glide

/** Deck controls for search results in the editor: copies in the deck, a rule tag, and one-tap changes. */
class DeckControls(
    val quantity: (Card) -> Int,
    val tag: @Composable (Card) -> Unit,
    val change: (Card, Int) -> Unit,
    /** The copy limit shown as pips under the count; null when the card has none. */
    val limit: (Card) -> Int? = { null },
    /** When set, cards can be dragged out of the list and dropped onto the deck. */
    val drag: CardDrag? = null,
)

private const val TILES_PER_FRAME = 4

/**
 * Search and filters over the catalog. Results update as you type (UX rule S5). The grid shows
 * whole cards; the list is denser. With [controls], each card gets the deck's − / + and tags.
 *
 * @param deckColors when set, offers "Deck colors only" so a two-color deck can hide the rest.
 * @param maxColors the ruleset's color limit; reaching it turns "Deck colors only" on.
 * @param selected highlights the card shown in a side panel.
 * @param scrollToTop goes up by one each time the results should go back to the top.
 * @param jumpToTop goes up by one each time the results should be at the top at once, without scrolling there.
 * @param leading extra controls at the start of the search row, such as the editor's Deck and Side switch.
 * @param defaultSort the order the list starts in, and goes back to when filters are reset.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardSearchPanel(
    cards: Collection<Card>,
    onCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier,
    focusRequests: Int = 0,
    deckColors: Set<CardColor>? = null,
    maxColors: Int? = null,
    controls: DeckControls? = null,
    selected: String? = null,
    wide: Boolean = false,
    scrollToTop: Int = 0,
    jumpToTop: Int = 0,
    leading: (@Composable RowScope.() -> Unit)? = null,
    defaultSort: CardSort = CardSort.NUMBER,
) {
    var query by remember { mutableStateOf(CardQuery()) }
    var showMore by rememberSaveable { mutableStateOf(false) }
    // A new default from Settings takes over at once.
    var sort by rememberSaveable(defaultSort) { mutableStateOf(defaultSort) }
    // Whole-card grid by default: the card images are the clearest way to browse.
    var grid by rememberSaveable { mutableStateOf(true) }
    var deckColorsOnly by rememberSaveable { mutableStateOf(false) }
    // The filter turns itself on once, when the deck reaches its color limit; turning it off sticks.
    var deckColorsAutoOn by rememberSaveable { mutableStateOf(false) }
    val deckColorsFull = maxColors != null && deckColors != null && deckColors.size >= maxColors
    LaunchedEffect(deckColorsFull) {
        if (deckColorsFull && !deckColorsAutoOn) {
            deckColorsOnly = true
            deckColorsAutoOn = true
        }
    }
    var sortMenu by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(focusRequests) { if (focusRequests > 0) runCatching { focus.requestFocus() } }

    val colorLimit = deckColors?.takeIf { deckColorsOnly && it.isNotEmpty() }
    // Sorted once per order, then filtered per keystroke: filtering keeps the order, and sorting the
    // whole catalog again on every letter was most of what typing in search cost.
    val sorted = remember(cards, sort) { cards.sortedWith(sort.comparator) }
    val results = remember(sorted, query, colorLimit) {
        sorted.filter { card ->
            query.matches(card) && (colorLimit == null || card.colors.isEmpty() || card.colors.all { it in colorLimit })
        }
    }
    // A screen of tiles is a large first composition, over 80 ms and several times that before the JIT
    // has warmed up, so the list starts with a few and adds more each frame until the screen is full.
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTop) {
        if (scrollToTop > 0) { if (grid) gridState.animateScrollToItem(0) else listState.animateScrollToItem(0) }
    }
    // A new order starts at its top. The grids keep their place by the first card showing, which a
    // new order moves, so AP would open part way down with the strongest cards out of sight above.
    val sortShown = remember { arrayOf(sort) }
    LaunchedEffect(sort) {
        if (sortShown[0] != sort) { sortShown[0] = sort; gridState.scrollToItem(0); listState.scrollToItem(0) }
    }
    var backToTop by remember { mutableIntStateOf(0) }
    LaunchedEffect(backToTop) {
        if (backToTop > 0) { if (grid) gridState.animateScrollToItem(0) else listState.animateScrollToItem(0) }
    }
    LaunchedEffect(jumpToTop) {
        if (jumpToTop > 0) { gridState.requestScrollToItem(0); listState.requestScrollToItem(0) }
    }
    var shown by remember { mutableIntStateOf(TILES_PER_FRAME) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { }
            val (total, last) = if (grid) gridState.layoutInfo.run { totalItemsCount to visibleItemsInfo.lastOrNull()?.index }
            else listState.layoutInfo.run { totalItemsCount to visibleItemsInfo.lastOrNull()?.index }
            // Done once every result is listed, or the screen is full with some left below it.
            if (total < shown || (last != null && last < total - 1)) break
            shown += TILES_PER_FRAME
        }
        shown = Int.MAX_VALUE
    }
    val listed = if (shown < results.size) results.subList(0, shown) else results
    // Every value from the lowest to the highest, so a strip has no holes for a drag to skip over.
    val levels = remember(cards) { cards.mapNotNull { it.level }.span() }
    val costs = remember(cards) { cards.mapNotNull { it.cost }.span() }
    val aps = remember(cards) { cards.mapNotNull { it.ap }.span() }
    val hps = remember(cards) { cards.mapNotNull { it.hp }.span() }
    val sets = remember(cards) { cards.map { it.setCode }.distinct().sortedWith(ReleaseOrder.newestFirst) }
    // Only the types these cards have, so a list without resources offers no Resource chip.
    val types = remember(cards) { CardType.entries.filter { type -> cards.any { it.type == type } } }

    @Composable
    fun ViewControls() {
        PillChip(
            selected = showMore, onClick = { showMore = !showMore }, label = { Text("Filters") },
            leadingIcon = { Icon(LineIcons.Sliders, contentDescription = null, Modifier.size(16.dp)) },
        )
        Box {
            PillChip(
                selected = false, onClick = { sortMenu = true },
                label = { Text(sort.label); Icon(LineIcons.Chevd, contentDescription = null, Modifier.size(18.dp)) },
                leadingIcon = { Icon(Glyphs.Sort, contentDescription = "Sort by", Modifier.size(16.dp)) },
            )
            GameMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                CardSort.entries.forEach { option ->
                    GameMenuItem(option.label, onClick = { sort = option; sortMenu = false }, selected = option == sort)
                }
            }
        }
        // The editor keeps this in Filters, so the row stays clear beside its Deck and Side switch.
        if (deckColors == null) ViewToggle(grid, { grid = it }, cardsFirst = true)
    }

    // Filters set beyond the quick row, so a closed Filters chip still says something is on.
    val drawerFilters = query.levels.size + query.costs.size + query.aps.size + query.hps.size + query.setCodes.size +
        (if (query.trait.isBlank()) 0 else 1)
    val hiddenFilters = drawerFilters + (if (!wide && sort != defaultSort) 1 else 0) +
        (if (!wide && deckColors != null) query.colors.size + query.types.size else 0)
    // Clears every filter and the sort but keeps the search text.
    fun resetFilters() {
        query = CardQuery(text = query.text)
        deckColorsOnly = false
        sort = defaultSort
    }
    val anyFilterSet = query != CardQuery(text = query.text) || deckColorsOnly || sort != defaultSort
    Column(modifier) {
        // Two rows instead of a field and wrapped chips: the search with its count and view
        // controls, then colours as orbs and types as chips (UX rules N4, S5). On phones the view
        // controls move into Filters, and colours and types fold into a menu chip each.
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = if (wide) 10.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (wide) leading?.invoke(this)
            PillSearchField(
                query.text, { query = query.copy(text = it) },
                placeholder = "Search ${results.size} cards",
                modifier = Modifier.weight(1f).focusRequester(focus),
                onClear = if (query.isEmpty) null else ({ query = CardQuery() }),
                shortcut = "Ctrl F",
            )
            // The placeholder has the count until a search is typed over it.
            if (query.text.isNotEmpty()) Text(
                "${results.size}", style = MaterialTheme.typography.titleMedium.merge(TabularNumbers),
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp),
            )
            if (wide) ViewControls()
            // Teal while anything in the sheet is set, so a closed sheet still says so. Holding it resets them.
            else PillChip(selected = hiddenFilters > 0, onClick = { showMore = true }, modifier = Modifier.height(ControlSize),
                label = {
                    Icon(LineIcons.Sliders, contentDescription = "Filters", Modifier.size(18.dp))
                    if (hiddenFilters > 0) Text("$hiddenFilters", Modifier.padding(start = 4.dp))
                },
                onLongClick = { resetFilters() },
                onLongClickLabel = "Reset filters")
        }
        val deckColorsChip = deckColors != null && deckColors.isNotEmpty()
        if (wide) FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            CardColor.entries.forEach { color ->
                Box(Modifier.align(Alignment.CenterVertically)) {
                    ColorOrbToggle(color.label, cardColor(color), onCardColor(color), color in query.colors || (colorLimit != null && color in colorLimit),
                        outlined = color == CardColor.WHITE, dimmed = query.colors.isNotEmpty() || colorLimit != null) {
                        query = query.copy(colors = query.colors.toggle(color))
                    }
                }
            }
            Box(Modifier.align(Alignment.CenterVertically).padding(horizontal = 6.dp).size(width = 1.dp, height = 24.dp).background(MaterialTheme.colorScheme.outlineVariant))
            types.forEach { type ->
                PillChip(
                    selected = type in query.types,
                    onClick = { query = query.copy(types = query.types.toggle(type)) },
                    label = { Text(type.label) },
                    leadingIcon = { Icon(type.chipIcon(), contentDescription = null, Modifier.size(16.dp)) },
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
            if (deckColorsChip) {
                PillChip(
                    selected = deckColorsOnly,
                    onClick = { deckColorsOnly = !deckColorsOnly },
                    label = { Text("Deck colors") },
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
        // Phones: in a deck, where + goes and Deck colors, with colours and types in Filters; in Cards,
        // colours and types themselves, scrolling sideways with a fade at the edge.
        else if (deckColors != null) Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke(this)
            Spacer(Modifier.weight(1f))
            if (deckColorsChip) PillChip(
                selected = deckColorsOnly, onClick = { deckColorsOnly = !deckColorsOnly }, label = { Text("Deck colors") }, modifier = Modifier.height(ControlSize),
                leadingIcon = if (deckColorsOnly) ({ Icon(LineIcons.Check, contentDescription = null, Modifier.size(16.dp)) }) else null,
            )
        } else Row(
            Modifier.fillMaxWidth().fadeEnd().horizontalScroll(rememberScrollState()).padding(start = 10.dp, end = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardColor.entries.forEach { color ->
                ColorOrbToggle(color.label, cardColor(color), onCardColor(color), color in query.colors || (colorLimit != null && color in colorLimit), outlined = color == CardColor.WHITE, dimmed = query.colors.isNotEmpty() || colorLimit != null) {
                    query = query.copy(colors = query.colors.toggle(color))
                }
            }
            Spacer(Modifier.width(4.dp))
            types.forEach { type ->
                PillChip(
                    selected = type in query.types, onClick = { query = query.copy(types = query.types.toggle(type)) }, label = { Text(type.label) },
                    leadingIcon = { Icon(type.chipIcon(), contentDescription = null, Modifier.size(16.dp)) },
                )
            }
        }
        // The drawer unfolds from under the quick row and its lines deal in one after another, top
        // first, so opening it reads as the row growing more filters rather than the grid jumping.
        // Closing folds it away in one quick move.
        val reduce = LocalReduceMotion.current
        AnimatedVisibility(
            visible = wide && showMore,
            enter = if (reduce) fadeIn(tween(90))
            else expandVertically(glide(), expandFrom = Alignment.Top) + fadeIn(tween(Motion.SHORT)),
            exit = if (reduce) fadeOut(tween(90))
            else shrinkVertically(tween(Motion.MEDIUM, easing = Motion.emphasizedAccelerate), shrinkTowards = Alignment.Top) +
                fadeOut(tween(Motion.SHORT)),
        ) {
            var line = 0
            fun Modifier.dealt(): Modifier {
                val delay = Motion.STAGGER * line++
                return if (reduce) this else animateEnterExit(
                    enter = slideInVertically(tween(Motion.LONG, delay, Motion.emphasizedDecelerate)) { -it / 2 } +
                        fadeIn(tween(Motion.MEDIUM, delay)),
                    exit = ExitTransition.None,
                )
            }
            Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (deckColors != null) PillChip(selected = false, onClick = { grid = !grid }, label = { Text(if (grid) "Show as list" else "Show as grid") })
                Row(Modifier.dealt(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NumberStrip("Level", levels, query.levels, Modifier.weight(1f)) { query = query.copy(levels = it) }
                    NumberStrip("Cost", costs, query.costs, Modifier.weight(1f)) { query = query.copy(costs = it) }
                }
                Row(Modifier.dealt(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NumberStrip("AP", aps, query.aps, Modifier.weight(1f)) { query = query.copy(aps = it) }
                    NumberStrip("HP", hps, query.hps, Modifier.weight(1f)) { query = query.copy(hps = it) }
                }
                Row(
                    Modifier.fillMaxWidth().dealt(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LineLabel("Set")
                    MenuChip(
                        selected = query.setCodes.isNotEmpty(),
                        label = {
                            val picked = sets.filter { it in query.setCodes }
                            Text(if (picked.isEmpty()) "Any" else picked.first() + if (picked.size > 1) " +${picked.size - 1}" else "")
                        },
                    ) {
                        sets.forEach { set ->
                            GameMenuItem(set, onClick = { query = query.copy(setCodes = query.setCodes.toggle(set)) }, selected = set in query.setCodes)
                        }
                    }
                    PillTextField(query.trait, { query = query.copy(trait = it) }, "Trait", Modifier.weight(1f))
                    if (anyFilterSet) PillChip(selected = false, onClick = { resetFilters() }, label = { Text("Clear") })
                }
            }
        }
        // Phones: a sheet over the screen. Results follow every tap behind it, the button says how many
        // there are, and closing it never undoes anything, so there is no Apply or Cancel (UX rule S5).
        if (!wide) FilterSheet(
            visible = showMore,
            onClose = { showMore = false },
            actions = {
                GameButton("Clear", { resetFilters() }, Modifier.height(48.dp), style = GameButtonStyle.SECONDARY, enabled = anyFilterSet)
                GameButton(
                    if (results.size == 1) "Show 1 card" else "Show ${results.size} cards",
                    { showMore = false }, Modifier.weight(1f).height(48.dp),
                )
            },
        ) {
            // The Cards screen keeps colours and types on the screen itself; a deck's row holds where + goes.
            if (deckColors != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CardColor.entries.forEach { color ->
                        ColorOrbToggle(color.label, cardColor(color), onCardColor(color), color in query.colors || (colorLimit != null && color in colorLimit),
                            target = ControlSize, outlined = color == CardColor.WHITE, dimmed = query.colors.isNotEmpty() || colorLimit != null) {
                            query = query.copy(colors = query.colors.toggle(color))
                        }
                    }
                }
                ChoiceStrip(null, types, query.types, { it.label }, Modifier.fillMaxWidth(), onPanel = true) { query = query.copy(types = it) }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                MenuChip(selected = sort != defaultSort, chipModifier = Modifier.height(ControlSize), label = {
                    Icon(Glyphs.Sort, contentDescription = "Sort by", Modifier.size(16.dp))
                    Text(sort.label)
                }) { close ->
                    CardSort.entries.forEach { option ->
                        GameMenuItem(option.label, onClick = { sort = option; close() }, selected = option == sort)
                    }
                }
                ViewToggle(grid, { grid = it }, Modifier.height(ControlSize), cardsFirst = true)
            }
            NumberStrip("Level", levels, query.levels, onPanel = true) { query = query.copy(levels = it) }
            NumberStrip("Cost", costs, query.costs, onPanel = true) { query = query.copy(costs = it) }
            NumberStrip("AP", aps, query.aps, onPanel = true) { query = query.copy(aps = it) }
            NumberStrip("HP", hps, query.hps, onPanel = true) { query = query.copy(hps = it) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MenuChip(selected = query.setCodes.isNotEmpty(), chipModifier = Modifier.height(ControlSize), label = {
                    val picked = sets.filter { it in query.setCodes }
                    Text(if (picked.isEmpty()) "All sets" else picked.first() + if (picked.size > 1) " +${picked.size - 1}" else "")
                }) {
                    sets.forEach { set ->
                        GameMenuItem(set, onClick = { query = query.copy(setCodes = query.setCodes.toggle(set)) }, selected = set in query.setCodes)
                    }
                }
                PillTextField(query.trait, { query = query.copy(trait = it) }, "Trait", Modifier.weight(1f))
            }
        }
        // Grid and list, and results and "no cards match", fade through each other instead of swapping.
        val view = when { results.isEmpty() -> ResultsView.EMPTY; grid -> ResultsView.GRID; else -> ResultsView.LIST }
        Crossfade(view, Modifier.weight(1f).fillMaxWidth(), animationSpec = motion(), label = "results") { view ->
        if (view == ResultsView.EMPTY) EmptyState("No cards match", "Try fewer filters or a shorter search.")
        else Box(Modifier.fillMaxSize()) {
            if (view == ResultsView.GRID) LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(if (wide) 136.dp else 104.dp),
                // The wider end padding keeps the last column clear of the scroll bar's lane.
                contentPadding = PaddingValues(start = 12.dp, end = if (wide) 24.dp else 12.dp, top = if (wide) 10.dp else 4.dp, bottom = 20.dp + LocalNavClearance.current),
                horizontalArrangement = Arrangement.spacedBy(if (wide) 12.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (wide) 12.dp else 8.dp),
            ) {
                items(listed, key = { it.cardNumber }) { card ->
                    CardTile(
                        card,
                        onClick = { onCardClick(card) },
                        // A filter can swap most of the results at once, so cards only fade in: sliding the kept ones
                        // across the grid while dropped ones fade out in place stacks cards on top of each other.
                        modifier = Modifier.animateItem(placementSpec = null, fadeOutSpec = null).cardDragSource(card, controls?.drag),
                        quantity = controls?.quantity?.invoke(card),
                        tag = { controls?.tag?.invoke(card) },
                        onMinus = controls?.let { c -> { c.change(card, -1) } },
                        onPlus = controls?.let { c -> { c.change(card, 1) } },
                        selected = card.cardNumber == selected,
                        limit = controls?.limit?.invoke(card),
                    )
                }
            }
            else LazyColumn(Modifier.fillMaxWidth(), listState, contentPadding = PaddingValues(end = if (wide) 14.dp else 0.dp, bottom = LocalNavClearance.current)) {
                items(listed, key = { it.cardNumber }) { card ->
                    CardRow(card, Modifier.animateItem(placementSpec = null, fadeOutSpec = null).cardDragSource(card, controls?.drag), onClick = { onCardClick(card) }, trailing = {
                        if (controls != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                controls.tag(card)
                                CompactStepper(controls.quantity(card), { controls.change(card, -1) }, { controls.change(card, 1) })
                            }
                        }
                    })
                }
            }
            if (view == ResultsView.GRID) VerticalScrollBar(gridState, Modifier.align(Alignment.TopEnd))
            else VerticalScrollBar(listState, Modifier.align(Alignment.TopEnd))
            // Back to top once half a screen of results has scrolled away.
            val scrolledAway by remember {
                derivedStateOf {
                    if (view == ResultsView.GRID) gridState.firstVisibleItemIndex.let { it > 0 && it * 2 >= gridState.layoutInfo.visibleItemsInfo.size }
                    else listState.firstVisibleItemIndex.let { it > 0 && it * 2 >= listState.layoutInfo.visibleItemsInfo.size }
                }
            }
            // Named in full: inside the Column, the bare name resolves to ColumnScope's version, which can't be used in this Box.
            androidx.compose.animation.AnimatedVisibility(
                visible = scrolledAway,
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = if (wide) 30.dp else 16.dp, bottom = 16.dp + LocalNavClearance.current),
                enter = if (reduce) fadeIn(tween(90)) else fadeIn(tween(Motion.SHORT)) + scaleIn(glide(), initialScale = 0.7f),
                exit = if (reduce) fadeOut(tween(90)) else fadeOut(tween(Motion.SHORT)) + scaleOut(tween(Motion.SHORT), targetScale = 0.7f),
            ) {
                GlassIconButton(Icons.Default.KeyboardArrowUp, "Back to top", onClick = { backToTop++ },
                    modifier = Modifier.shadow(6.dp, CircleShape))
            }
        }
        }
    }
}

private enum class ResultsView { EMPTY, GRID, LIST }

/** The name at the start of a filter line, a fixed width on phones so the lines' chips start together. */
@Composable
private fun LineLabel(label: String, modifier: Modifier = Modifier) {
    Text(label, style = MaterialTheme.typography.labelLarge, modifier = modifier.width(46.dp))
}

/**
 * A solid panel that rises from the bottom of a phone over a plain dim. The dim and Back close it as
 * well as the button in [actions]. The middle scrolls only when the panel can't fit, as with a large
 * font or the keyboard open, and the buttons stay in view.
 */
@Composable
private fun FilterSheet(
    visible: Boolean,
    onClose: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    val state = remember { MutableTransitionState(false) }.apply { targetState = visible }
    if (!state.currentState && !state.targetState) return
    val reduce = LocalReduceMotion.current
    val scheme = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onClose, properties = fullWindowDialogProperties()) {
        LightSystemBarIcons()
        androidx.compose.animation.AnimatedVisibility(
            state, Modifier.fillMaxSize(),
            enter = fadeIn(tween(if (reduce) 90 else Motion.SHORT)),
            exit = fadeOut(tween(if (reduce) 90 else Motion.SHORT)),
        ) {
            Box(Modifier.fillMaxSize()) {
                // Phones get no system dim under a full-window dialog, so this paints one.
                Spacer(Modifier.fillMaxSize().then(if (isMobile) Modifier.background(Color.Black.copy(alpha = 0.45f)) else Modifier)
                    .clickable(interactionSource = null, indication = null, onClick = onClose))
                Column(
                    Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(8.dp)
                        .widthIn(max = 480.dp).fillMaxWidth()
                        .then(if (reduce) Modifier else Modifier.animateEnterExit(
                            enter = slideInVertically(tween(Motion.LONG, easing = Motion.emphasizedDecelerate)) { it / 2 },
                            exit = slideOutVertically(tween(Motion.MEDIUM, easing = Motion.emphasizedAccelerate)) { it / 2 },
                        ))
                        .clip(LockOnShapes.panel)
                        .background(if (isTwilight()) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest)
                        // Taps on the panel's own gaps stay on the panel.
                        .clickable(interactionSource = null, indication = null) { }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) { content() }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
                }
            }
        }
    }
}

/** A stat's values as a strip of numbers: see [ChoiceStrip]. */
@Composable
private fun NumberStrip(label: String, values: List<Int>, selected: Set<Int>, modifier: Modifier = Modifier, onPanel: Boolean = false, onChange: (Set<Int>) -> Unit) =
    ChoiceStrip(label, values, selected, { "$it" }, modifier, onPanel, onChange)

/**
 * Choices as one strip. Tapping one toggles it; dragging across the strip picks the run under the
 * drag. Picked neighbours join into one teal pill, so a run reads as a range rather than a row of
 * separate chips. [onPanel] sets it into a solid panel, as a soft track rather than glass.
 */
@Composable
private fun <T> ChoiceStrip(label: String?, values: List<T>, selected: Set<T>, text: (T) -> String, modifier: Modifier = Modifier, onPanel: Boolean = false, onChange: (Set<T>) -> Unit) {
    if (values.isEmpty()) return
    val accent = Accent
    val onAccent = OnAccent
    val ink = MaterialTheme.colorScheme.onSurface
    val picked = values.map { value ->
        animateFloatAsState(if (value in selected) 1f else 0f, motion(Motion.SHORT), label = "strip-cell").value
    }
    val change by rememberUpdatedState(onChange)
    val cells by rememberUpdatedState(values)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (label != null) LineLabel(label)
        Box(
            Modifier.weight(1f)
                .then(if (onPanel) Modifier.height(40.dp).clip(PillShape).background(ink.copy(alpha = 0.08f)) else Modifier.height(34.dp).glass(PillShape, strong = true))
                .padding(3.dp)
                .pointerInput(Unit) {
                    var from = 0
                    fun cellAt(x: Float) = (x / size.width * cells.size).toInt().coerceIn(0, cells.size - 1)
                    detectHorizontalDragGestures(
                        onDragStart = { from = cellAt(it.x); change(setOf(cells[from])) },
                        onHorizontalDrag = { pointer, _ ->
                            val to = cellAt(pointer.position.x)
                            change(cells.subList(minOf(from, to), maxOf(from, to) + 1).toSet())
                        },
                    )
                }
                .drawBehind {
                    val w = size.width / values.size
                    val r = size.height / 2
                    picked.forEachIndexed { i, a ->
                        if (a <= 0f) return@forEachIndexed
                        // Rounded only where the run ends, so neighbours join into one pill.
                        val joinsLeft = picked.getOrElse(i - 1) { 0f } > 0f
                        val joinsRight = picked.getOrElse(i + 1) { 0f } > 0f
                        val left = if (joinsLeft) 0f else r
                        val right = if (joinsRight) 0f else r
                        // A pixel of overlap where cells join, or their anti-aliased edges leave a seam.
                        val seam = 1f
                        drawPath(Path().apply {
                            addRoundRect(RoundRect(i * w - (if (joinsLeft) seam else 0f), 0f, (i + 1) * w, size.height,
                                CornerRadius(left), CornerRadius(right), CornerRadius(right), CornerRadius(left)))
                        }, accent.copy(alpha = a))
                    }
                },
        ) {
            Row(Modifier.fillMaxSize()) {
                values.forEachIndexed { i, value ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(PillShape)
                            .toggleable(value in selected, interactionSource = null, indication = null, role = Role.Checkbox) {
                                onChange(selected.toggle(value))
                            }
                            .semantics { contentDescription = if (label == null) text(value) else "$label ${text(value)}" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text(value), style = MaterialTheme.typography.labelLarge.merge(TabularNumbers),
                            color = lerp(ink, onAccent, picked[i]), maxLines = 1)
                    }
                }
            }
        }
    }
}

private fun List<Int>.span(): List<Int> = if (isEmpty()) emptyList() else (min()..max()).toList()

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

/** A small picture for a card type on its filter chip. */
private fun CardType.chipIcon(): androidx.compose.ui.graphics.vector.ImageVector = when (this) {
    CardType.PILOT -> LineIcons.Pilot
    CardType.COMMAND -> LineIcons.Command
    CardType.BASE -> LineIcons.Base
    else -> LineIcons.Unit
}
