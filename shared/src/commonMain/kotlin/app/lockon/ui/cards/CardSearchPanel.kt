package app.lockon.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lockon.model.Card
import app.lockon.model.CardColor
import app.lockon.model.CardQuery
import app.lockon.model.CardType
import app.lockon.ui.components.CardRow
import app.lockon.ui.components.EmptyState

/** Search and filters over the catalog. [trailing] adds per-card controls, such as +/- in the editor. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardSearchPanel(
    cards: Collection<Card>,
    onCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier,
    initialTypes: Set<CardType> = emptySet(),
    trailing: @Composable (Card) -> Unit = {},
) {
    var query by remember { mutableStateOf(CardQuery(types = initialTypes)) }
    var showMore by rememberSaveable { mutableStateOf(false) }
    val results = remember(cards, query) { cards.filter(query::matches).sortedBy { it.cardNumber } }
    val levels = remember(cards) { cards.mapNotNull { it.level }.distinct().sorted() }
    val costs = remember(cards) { cards.mapNotNull { it.cost }.distinct().sorted() }
    val sets = remember(cards) { cards.map { it.setCode }.distinct().sorted() }

    Column(modifier) {
        OutlinedTextField(
            value = query.text,
            onValueChange = { query = query.copy(text = it) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = { Text("Name, card number, or text") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (!query.isEmpty) IconButton(onClick = { query = CardQuery() }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search and filters")
                }
            },
            singleLine = true,
        )
        FlowRow(
            Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
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
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { showMore = !showMore }) { Text(if (showMore) "Fewer filters" else "More filters") }
            Text(
                "${results.size} of ${cards.size} cards",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        if (results.isEmpty()) {
            EmptyState("No cards match", "Try fewer filters or a shorter search.")
        } else {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(results, key = { it.cardNumber }) { card ->
                    CardRow(card, onClick = { onCardClick(card) }, trailing = { trailing(card) })
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
