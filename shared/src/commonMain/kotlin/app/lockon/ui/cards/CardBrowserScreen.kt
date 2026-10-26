package app.lockon.ui.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import app.lockon.model.Card
import app.lockon.platform.FileBridge
import app.lockon.ui.AppState
import app.lockon.ui.components.CardDetailDialog

@Composable
fun CardBrowserScreen(container: AppContainer, state: AppState, files: FileBridge) {
    val catalog = container.catalog
    val cards by catalog.cards.collectAsState()
    val printings by catalog.printings.collectAsState()
    val info by catalog.info.collectAsState()
    var selected by remember { mutableStateOf<Card?>(null) }
    var managing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                info?.let { "${it.name} · v${it.version}" }.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { managing = true }) { Text("Catalog") }
        }
        CardSearchPanel(
            cards.values, onCardClick = { selected = it }, modifier = Modifier.fillMaxSize(),
            focusRequests = state.searchFocusRequests,
        )
    }
    selected?.let { card ->
        CardDetailDialog(card, printings[card.cardNumber].orEmpty(), onDismiss = { selected = null })
    }
    if (managing) CatalogDialog(container, state, files, onDismiss = { managing = false })
}
