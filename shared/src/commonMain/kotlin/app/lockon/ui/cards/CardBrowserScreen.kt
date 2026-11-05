package app.lockon.ui.cards

import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.motion
import androidx.compose.animation.Crossfade
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
import app.lockon.ui.AppState
import app.lockon.ui.Tab
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import app.lockon.ui.components.CardDetailContent
import app.lockon.ui.components.CardDetailDialog
import app.lockon.ui.components.EmptyState
import app.lockon.ui.components.Panel
import app.lockon.ui.components.ScreenHeader
import androidx.compose.ui.unit.sp

@Composable
fun CardBrowserScreen(container: AppContainer, state: AppState, wide: Boolean = false) {
    val catalog = container.catalog
    val cards by catalog.cards.collectAsState()
    val printings by catalog.printings.collectAsState()
    val defaultSort by container.settings.cardSort.collectAsState()
    // Kept by card number, so the details panel still shows the same card after a visit elsewhere.
    var selectedNumber by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = selectedNumber?.let { cards[it] }
    // Wide windows always show a card in the details panel, the first one until another is picked,
    // so the panel is never an empty third of the window.
    val shown = selected ?: if (wide) cards.values.minByOrNull { it.cardNumber } else null
    var detailsOpen by rememberSaveable { mutableStateOf(true) }

    Column(Modifier.fillMaxSize()) {
        // In the band beside the floating navigation on wide windows (UX rule N5).
        // The catalog is managed from Settings, so the header carries no actions.
        ScreenHeader(if (wide) "Card database" else "Cards")
        // This screen stays composed while hidden (KeptTabs), so only Ctrl+F presses made while it shows
        // reach its search; a deck's own search takes the rest.
        val focusSeen = remember { intArrayOf(state.searchFocusRequests) }
        if (state.current == null && state.tab == Tab.CARDS) focusSeen[0] = state.searchFocusRequests
        val focusRequests = focusSeen[0]
        Row(Modifier.fillMaxSize().padding(start = if (wide) 10.dp else 0.dp, top = if (wide) 8.dp else 0.dp)) {
            val search: @Composable (Modifier) -> Unit = { m ->
                CardSearchPanel(
                    cards.values, onCardClick = { selectedNumber = it.cardNumber; detailsOpen = true },
                    modifier = m, focusRequests = focusRequests, selected = shown?.cardNumber, wide = wide, defaultSort = defaultSort,
                )
            }
            if (wide) {
                search(Modifier.weight(1f).fillMaxHeight().padding(end = 6.dp))
            } else {
                search(Modifier.weight(1f).fillMaxSize())
            }
            if (wide && detailsOpen) {
                // The details panel stays while cards are picked, so the grid never changes width under the pointer.
                Panel(Modifier.width(420.dp).fillMaxHeight().padding(end = 12.dp, bottom = 12.dp)) {
                    // Picking another card fades its details in over the last one's.
                    Crossfade(shown, animationSpec = motion(), label = "card-details") { card ->
                    if (card == null) {
                        EmptyState("No card selected", "Pick a card to see its effect, stats and printings here.", Modifier.padding(top = 24.dp))
                    } else {
                        Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp)) {
                            Row(Modifier.padding(top = 8.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(card.name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                GameButton("Close", onClick = { detailsOpen = false }, style = GameButtonStyle.SECONDARY, compact = true)
                            }
                            CardDetailContent(card, printings[card.cardNumber].orEmpty(), Modifier.padding(end = 12.dp, top = 8.dp, bottom = 12.dp))
                        }
                    }
                    }
                }
            }
        }
    }
    if (!wide) selected?.let { card ->
        CardDetailDialog(card, printings[card.cardNumber].orEmpty(), onDismiss = { selectedNumber = null })
    }
}
