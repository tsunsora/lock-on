package app.lockon.ui.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lockon.data.CatalogRepository
import app.lockon.model.Card
import app.lockon.ui.components.CardDetailDialog

@Composable
fun CardBrowserScreen(catalog: CatalogRepository) {
    val cards by catalog.cards.collectAsState()
    val printings by catalog.printings.collectAsState()
    val catalogName by catalog.catalogName.collectAsState()
    var selected by remember { mutableStateOf<Card?>(null) }

    Column(Modifier.fillMaxSize()) {
        Text(
            catalogName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        )
        CardSearchPanel(cards.values, onCardClick = { selected = it }, modifier = Modifier.fillMaxSize())
    }
    selected?.let { card ->
        CardDetailDialog(card, printings[card.cardNumber].orEmpty(), onDismiss = { selected = null })
    }
}
