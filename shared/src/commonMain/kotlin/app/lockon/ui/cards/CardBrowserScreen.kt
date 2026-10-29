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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lockon.AppContainer
import app.lockon.platform.FileBridge
import app.lockon.ui.AppState
import app.lockon.ui.Tab
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import app.lockon.ui.components.CardDetailContent
import app.lockon.ui.components.CardDetailDialog
import app.lockon.ui.components.EmptyState
import app.lockon.ui.components.Panel
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.ScreenHeader
import androidx.compose.ui.unit.sp

@Composable
fun CardBrowserScreen(container: AppContainer, state: AppState, files: FileBridge, wide: Boolean = false) {
    val catalog = container.catalog
    val cards by catalog.cards.collectAsState()
    val printings by catalog.printings.collectAsState()
    val info by catalog.info.collectAsState()
    val update by catalog.updateAvailable.collectAsState()
    // Kept by card number, so the details panel still shows the same card after a visit elsewhere.
    var selectedNumber by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = selectedNumber?.let { cards[it] }
    // Wide windows always show a card in the details panel, the first one until another is picked,
    // so the panel is never an empty third of the window.
    val shown = selected ?: if (wide) cards.values.minByOrNull { it.cardNumber } else null
    var managing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // In the band beside the floating navigation on wide windows (UX rule N5).
        ScreenHeader(if (wide) "Card database" else "Cards") {
            // A found update is a quiet label here, never a pop-up (UX rule C1).
            GameButton(update ?: "Catalog", onClick = { managing = true }, style = GameButtonStyle.SECONDARY, compact = true)
        }
        // This screen stays composed while hidden (KeptTabs), so only Ctrl+F presses made while it shows
        // reach its search; a deck's own search takes the rest.
        val focusSeen = remember { intArrayOf(state.searchFocusRequests) }
        if (state.current == null && state.tab == Tab.CARDS) focusSeen[0] = state.searchFocusRequests
        val focusRequests = focusSeen[0]
        Row(Modifier.fillMaxSize().padding(start = if (wide) 10.dp else 0.dp, top = if (wide) 8.dp else 0.dp)) {
            val search: @Composable (Modifier) -> Unit = { m ->
                CardSearchPanel(
                    cards.values, onCardClick = { selectedNumber = it.cardNumber },
                    modifier = m, focusRequests = focusRequests, selected = shown?.cardNumber, wide = wide,
                )
            }
            if (wide) {
                Panel(Modifier.weight(1f).fillMaxHeight().padding(end = 10.dp, bottom = 10.dp)) {
                    search(Modifier.fillMaxSize())
                }
            } else {
                search(Modifier.weight(1f).fillMaxSize())
            }
            if (wide) {
                // The details panel always stays, so the grid never changes width as cards are picked.
                Panel(Modifier.width(460.dp).fillMaxHeight().padding(end = 12.dp, bottom = 12.dp)) {
                    val card = shown
                    if (card == null) {
                        EmptyState("No card selected", "Pick a card to see its effect, stats and printings here.", Modifier.padding(top = 24.dp))
                    } else {
                        Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp)) {
                            Text(card.name, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 8.dp, end = 12.dp))
                            CardDetailContent(card, printings[card.cardNumber].orEmpty(), Modifier.padding(end = 12.dp, top = 8.dp, bottom = 12.dp))
                        }
                    }
                }
            }
        }
    }
    if (!wide) selected?.let { card ->
        CardDetailDialog(card, printings[card.cardNumber].orEmpty(), onDismiss = { selectedNumber = null })
    }
    if (managing) CatalogDialog(container, state, files, onDismiss = { managing = false })
}
