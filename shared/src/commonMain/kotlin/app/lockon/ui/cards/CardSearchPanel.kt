package app.lockon.ui.cards

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
import app.lockon.ui.components.CardTile
import app.lockon.ui.components.CompactStepper
import app.lockon.ui.components.CardRow
import app.lockon.ui.components.EmptyState
import app.lockon.ui.theme.TabularNumbers

enum class CardSort(val label: String, val comparator: Comparator<Card>) {
    NUMBER("Number", compareBy { it.cardNumber }),
    LEVEL("Level", compareBy<Card>({ it.level ?: 99 }, { it.cost ?: 99 }, { it.cardNumber })),
    COST("Cost", compareBy<Card>({ it.cost ?: 99 }, { it.level ?: 99 }, { it.cardNumber })),
    NAME("Name", compareBy { it.name.lowercase() }),
}

/** Deck controls for search results in the editor: copies in the deck, a rule tag, and one-tap changes. */
class DeckControls(
    val quantity: (Card) -> Int,
    val tag: @Composable (Card) -> Unit,
    val change: (Card, Int) -> Unit,
)

/**
 * Search and filters over the catalog. Results update as you type (UX rule S5). The grid shows
 * whole cards; the list is denser. With [controls], each card gets the deck's − / + and tags.
 *
 * @param deckColors when set, offers "Deck colors only" so a two-color deck can hide the rest.
 * @param selected highlights the card shown in a side panel.
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

    Column(modifier) {
        OutlinedTextField(
            value = query.text,
            onValueChange = { query = query.copy(text = it) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).focusRequester(focus),
            placeholder = { Text(if (query.isEmpty) "Search ${cards.size} cards" else "Name, card number, or text") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (!query.isEmpty) IconButton(onClick = { query = CardQuery() }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search and filters")
                }
            },
            singleLine = true,
        )
        // Wide panes wrap the quick filters so all are visible; phones scroll one row to keep results in view.
        val chipModifier = if (wide) Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        else Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp)
        FlowRow(
            chipModifier,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Results, filters, sort and view share the chip row, so no separate toolbar is needed.
            Text(
                "${results.size}", style = MaterialTheme.typography.titleSmall.merge(TabularNumbers),
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterVertically).padding(end = 2.dp),
            )
            FilterChip(selected = showMore, onClick = { showMore = !showMore }, label = { Text("Filters") })
            Box {
                FilterChip(selected = false, onClick = { sortMenu = true }, label = { Text("Sort: ${sort.label}") })
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    CardSort.entries.forEach { option ->
                        DropdownMenuItem(text = { Text(option.label) }, onClick = { sort = option; sortMenu = false })
                    }
                }
            }
            FilterChip(selected = false, onClick = { grid = !grid }, label = { Text(if (grid) "List" else "Grid") })
            if (deckColors != null && deckColors.isNotEmpty()) {
                FilterChip(
                    selected = deckColorsOnly,
                    onClick = { deckColorsOnly = !deckColorsOnly },
                    label = { Text("Deck colors only") },
                )
            }
            CardColor.entries.forEach { color ->
                FilterChip(
                    selected = color in query.colors,
                    onClick = { query = query.copy(colors = query.colors.toggle(color)) },
                    label = { Text(color.label) },
                )
            }
            CardType.entries.forEach { type ->
                FilterChip(
                    selected = type in query.types,
                    onClick = { query = query.copy(types = query.types.toggle(type)) },
                    label = { Text(type.label) },
                )
            }
        }
        if (showMore) {
            Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
        HorizontalDivider(Modifier.padding(top = 8.dp))
        when {
            results.isEmpty() -> EmptyState("No cards match", "Try fewer filters or a shorter search.")
            grid -> LazyVerticalGrid(
                columns = GridCells.Adaptive(if (wide) 160.dp else 104.dp),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(if (wide) 16.dp else 10.dp),
                verticalArrangement = Arrangement.spacedBy(if (wide) 18.dp else 12.dp),
            ) {
                items(results, key = { it.cardNumber }) { card ->
                    CardTile(
                        card,
                        onClick = { onCardClick(card) },
                        modifier = Modifier.animateItem(),
                        quantity = controls?.quantity?.invoke(card),
                        tag = { controls?.tag?.invoke(card) },
                        onMinus = controls?.let { c -> { c.change(card, -1) } },
                        onPlus = controls?.let { c -> { c.change(card, 1) } },
                        selected = card.cardNumber == selected,
                    )
                }
            }
            else -> LazyColumn(Modifier.fillMaxWidth()) {
                items(results, key = { it.cardNumber }) { card ->
                    CardRow(card, Modifier.animateItem(), onClick = { onCardClick(card) }, trailing = {
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
            FilterChip(selected = value in selected, onClick = { onToggle(value) }, label = { Text("$value") })
        }
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
