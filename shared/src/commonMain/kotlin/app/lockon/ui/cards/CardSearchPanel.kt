package app.lockon.ui.cards
import app.lockon.ui.components.LocalNavClearance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import app.lockon.ui.components.PillChip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.foundation.layout.width

enum class CardSort(val label: String, val comparator: Comparator<Card>) {
    NUMBER("Number", compareBy(ReleaseOrder.comparator) { it.cardNumber }),
    LEVEL("Level", compareBy<Card>({ it.level ?: 99 }, { it.cost ?: 99 }, { it.cardNumber })),
    COST("Cost", compareBy<Card>({ it.cost ?: 99 }, { it.level ?: 99 }, { it.cardNumber })),
    NAME("Name", compareBy { it.name.lowercase() }),
}

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

/**
 * Search and filters over the catalog. Results update as you type (UX rule S5). The grid shows
 * whole cards; the list is denser. With [controls], each card gets the deck's − / + and tags.
 *
 * @param deckColors when set, offers "Deck colors only" so a two-color deck can hide the rest.
 * @param selected highlights the card shown in a side panel.
 * @param trailing extra controls at the end of the search row, such as the editor's Add to switch.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardSearchPanel(
    cards: Collection<Card>,
    onCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier,
    focusRequests: Int = 0,
    deckColors: Set<CardColor>? = null,
    controls: DeckControls? = null,
    selected: String? = null,
    wide: Boolean = false,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    var query by remember { mutableStateOf(CardQuery()) }
    var showMore by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf(CardSort.NUMBER) }
    // Whole-card grid by default: the card images are the clearest way to browse.
    var grid by rememberSaveable { mutableStateOf(true) }
    var deckColorsOnly by rememberSaveable { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(focusRequests) { if (focusRequests > 0) runCatching { focus.requestFocus() } }

    val colorLimit = deckColors?.takeIf { deckColorsOnly && it.isNotEmpty() }
    val results = remember(cards, query, sort, colorLimit) {
        cards.filter { card ->
            query.matches(card) && (colorLimit == null || card.colors.isEmpty() || card.colors.all { it in colorLimit })
        }.sortedWith(sort.comparator)
    }
    val levels = remember(cards) { cards.mapNotNull { it.level }.distinct().sorted() }
    val costs = remember(cards) { cards.mapNotNull { it.cost }.distinct().sorted() }
    val sets = remember(cards) { cards.map { it.setCode }.distinct().sorted() }
    // Only the types these cards have, so a list without resources offers no Resource chip.
    val types = remember(cards) { CardType.entries.filter { type -> cards.any { it.type == type } } }

    @Composable
    fun ViewControls() {
        PillChip(selected = showMore, onClick = { showMore = !showMore }, label = { Text("Filters") })
        Box {
            PillChip(selected = false, onClick = { sortMenu = true }, label = { Text("Sort: ${sort.label}") })
            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                CardSort.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(option.label) }, onClick = { sort = option; sortMenu = false })
                }
            }
        }
        PillChip(selected = false, onClick = { grid = !grid }, label = { Text(if (grid) "List" else "Grid") })
    }

    // Filters set beyond the quick row, so a closed Filters chip still says something is on.
    val hiddenFilters = query.levels.size + query.costs.size + query.setCodes.size + (if (query.trait.isBlank()) 0 else 1) +
        (if (!wide && sort != CardSort.NUMBER) 1 else 0)
    Column(modifier) {
        // Two rows instead of a field and wrapped chips: the search with its count and view
        // controls, then colours as orbs and types as chips (UX rules N4, S5). On phones the view
        // controls move into Filters, so the quick row starts with every colour in reach.
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PillSearchField(
                query.text, { query = query.copy(text = it) },
                placeholder = if (query.isEmpty) "Search ${cards.size} cards" else "Name, card number, or text",
                modifier = Modifier.weight(1f).focusRequester(focus),
                onClear = if (query.isEmpty) null else ({ query = CardQuery() }),
            )
            Text(
                "${results.size}", style = MaterialTheme.typography.titleMedium.merge(TabularNumbers),
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp),
            )
            if (wide) ViewControls()
            else PillChip(selected = showMore, onClick = { showMore = !showMore },
                label = { Text(if (hiddenFilters > 0) "Filters · $hiddenFilters" else "Filters") })
            trailing?.invoke(this)
        }
        // Phones scroll this row, and its far edge fades out to show there is more.
        val rowModifier = if (wide) Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)
        else Modifier.fillMaxWidth()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = size.width - 40.dp.toPx(), endX = size.width),
                    blendMode = BlendMode.DstOut)
            }
            .horizontalScroll(rememberScrollState()).padding(start = 6.dp, end = 32.dp, top = 2.dp, bottom = 2.dp)
        FlowRow(rowModifier, horizontalArrangement = Arrangement.spacedBy(if (wide) 4.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CardColor.entries.forEach { color ->
                Box(Modifier.align(Alignment.CenterVertically)) {
                    ColorOrbToggle(color.label, cardColor(color), onCardColor(color), color in query.colors, target = if (wide) 38.dp else 48.dp,
                        outlined = color == CardColor.WHITE) {
                        query = query.copy(colors = query.colors.toggle(color))
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            types.forEach { type ->
                PillChip(
                    selected = type in query.types,
                    onClick = { query = query.copy(types = query.types.toggle(type)) },
                    label = { Text(type.label) },
                    modifier = Modifier.align(Alignment.CenterVertically).padding(end = if (wide) 0.dp else 4.dp),
                )
            }
            if (deckColors != null && deckColors.isNotEmpty()) {
                PillChip(
                    selected = deckColorsOnly,
                    onClick = { deckColorsOnly = !deckColorsOnly },
                    label = { Text("Deck colors only") },
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
        if (showMore) {
            Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!wide) {
                    // On phones sort order and view live here, keeping the quick row for filters.
                    ChipLine("Sort", CardSort.entries.map { it.label }, setOf(sort.label)) { label -> sort = CardSort.entries.first { it.label == label } }
                    ChipLine("View", listOf("Grid", "List"), setOf(if (grid) "Grid" else "List")) { grid = it == "Grid" }
                }
                ChipLine("Level", levels, query.levels) { query = query.copy(levels = query.levels.toggle(it)) }
                ChipLine("Cost", costs, query.costs) { query = query.copy(costs = query.costs.toggle(it)) }
                ChipLine("Set", sets, query.setCodes) { query = query.copy(setCodes = query.setCodes.toggle(it)) }
                OutlinedTextField(
                    value = query.trait,
                    onValueChange = { query = query.copy(trait = it) },
                    label = { Text("Trait") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        HorizontalDivider(Modifier.padding(top = 4.dp))
        when {
            results.isEmpty() -> EmptyState("No cards match", "Try fewer filters or a shorter search.")
            grid -> LazyVerticalGrid(
                columns = GridCells.Adaptive(if (wide) 136.dp else 104.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 20.dp + LocalNavClearance.current),
                horizontalArrangement = Arrangement.spacedBy(if (wide) 12.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (wide) 12.dp else 10.dp),
            ) {
                items(results, key = { it.cardNumber }) { card ->
                    CardTile(
                        card,
                        onClick = { onCardClick(card) },
                        modifier = Modifier.animateItem().cardDragSource(card, controls?.drag),
                        quantity = controls?.quantity?.invoke(card),
                        tag = { controls?.tag?.invoke(card) },
                        onMinus = controls?.let { c -> { c.change(card, -1) } },
                        onPlus = controls?.let { c -> { c.change(card, 1) } },
                        selected = card.cardNumber == selected,
                        limit = controls?.limit?.invoke(card),
                    )
                }
            }
            else -> LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = LocalNavClearance.current)) {
                items(results, key = { it.cardNumber }) { card ->
                    CardRow(card, Modifier.animateItem().cardDragSource(card, controls?.drag), onClick = { onCardClick(card) }, trailing = {
                        if (controls != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                controls.tag(card)
                                CompactStepper(controls.quantity(card), { controls.change(card, -1) }, { controls.change(card, 1) })
                            }
                        }
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipLine(label: String, values: List<T>, selected: Set<T>, onToggle: (T) -> Unit) {
    if (values.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.Center) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.CenterVertically).padding(end = 4.dp))
        values.forEach { value ->
            PillChip(selected = value in selected, onClick = { onToggle(value) }, label = { Text("$value") })
        }
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
