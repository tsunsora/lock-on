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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import app.lockon.ui.components.CardDetailContent
import app.lockon.ui.components.CardDetailDialog
import app.lockon.ui.components.Motion
import app.lockon.ui.components.Panel
import app.lockon.ui.components.motion

@Composable
fun CardBrowserScreen(container: AppContainer, state: AppState, files: FileBridge, wide: Boolean = false) {
    val catalog = container.catalog
    val cards by catalog.cards.collectAsState()
    val printings by catalog.printings.collectAsState()
    val info by catalog.info.collectAsState()
    val update by catalog.updateAvailable.collectAsState()
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
            // A found update is a quiet label here, never a pop-up (UX rule C1).
            TextButton(onClick = { managing = true }) { Text(update ?: "Catalog") }
        }
        Row(Modifier.fillMaxSize()) {
            CardSearchPanel(
                cards.values, onCardClick = { selected = if (wide && selected == it) null else it },
                modifier = Modifier.weight(1f).fillMaxSize(),
                focusRequests = state.searchFocusRequests, selected = selected?.cardNumber, wide = wide,
            )
            if (wide) {
                AnimatedVisibility(
                    visible = selected != null,
                    enter = expandHorizontally(motion(Motion.LONG)) + fadeIn(motion(Motion.MEDIUM)),
                    exit = shrinkHorizontally(motion(Motion.MEDIUM)) + fadeOut(motion(Motion.SHORT)),
                ) {
                    val card = selected
                    if (card != null) {
                        Panel(Modifier.width(460.dp).fillMaxHeight().padding(end = 12.dp, bottom = 12.dp)) {
                            Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(card.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f),
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    IconButton(onClick = { selected = null }) { Icon(Icons.Default.Close, contentDescription = "Close details") }
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
        CardDetailDialog(card, printings[card.cardNumber].orEmpty(), onDismiss = { selected = null })
    }
    if (managing) CatalogDialog(container, state, files, onDismiss = { managing = false })
}
