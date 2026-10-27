package app.lockon.ui.decks
import app.lockon.ui.components.GameButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lockon.AppContainer
import app.lockon.model.Card as GameCard
import app.lockon.model.DeckSection
import app.lockon.model.DeckVersion
import app.lockon.platform.formatTimestamp
import app.lockon.sync.PairedDevice
import app.lockon.ui.AppState
import app.lockon.ui.Screen
import kotlinx.coroutines.launch

/** Shows every independent version of a deck side by side and lets the person keep one. */
@Composable
fun ConflictScreen(container: AppContainer, state: AppState, deckId: String, wide: Boolean) {
    val summary by container.decks.deck(deckId).collectAsState(initial = null)
    val cards by container.catalog.cards.collectAsState()
    val devices by container.devices.pairedDevices.collectAsState(initial = emptyList())
    var versions by remember { mutableStateOf<List<DeckVersion>>(emptyList()) }
    LaunchedEffect(summary) { versions = container.decks.versions(deckId) }

    // Once resolved (here or by sync), continue in the editor.
    LaunchedEffect(summary?.hasConflict) {
        if (summary != null && summary?.hasConflict == false) {
            state.goBack()
            state.open(Screen.DeckEditor(deckId))
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = state::goBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Column {
                Text("Compare versions", style = MaterialTheme.typography.titleLarge)
                Text("This deck changed on more than one device before they synced. Keep the version you want; the others stay in history.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val body: @Composable (DeckVersion, Modifier) -> Unit = { version, modifier ->
            VersionCard(
                version = version,
                others = versions - version,
                cards = cards,
                deviceLabel = deviceLabel(version.originDeviceId, container.identity.deviceId, devices),
                modifier = modifier,
                onKeep = {
                    state.appScope.launch {
                        container.decks.resolve(deckId, version.snapshot)
                        state.notify(if (version.snapshot.deleted) "Deck deleted." else "Version kept.")
                        if (version.snapshot.deleted) state.goBack()
                    }
                },
            )
        }
        if (wide) {
            Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                versions.forEach { body(it, Modifier.weight(1f)) }
            }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                versions.forEach { body(it, Modifier.fillMaxWidth()) }
            }
        }
    }
}

internal fun deviceLabel(originId: String, localId: String, devices: List<PairedDevice>): String =
    if (originId == localId) "This device" else devices.firstOrNull { it.deviceId == originId }?.name ?: "Another device"

@Composable
private fun VersionCard(
    version: DeckVersion,
    others: List<DeckVersion>,
    cards: Map<String, GameCard>,
    deviceLabel: String,
    modifier: Modifier,
    onKeep: () -> Unit,
) {
    val snapshot = version.snapshot
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(deviceLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Saved ${formatTimestamp(version.createdAt)}", style = MaterialTheme.typography.bodySmall)
            if (snapshot.deleted) {
                Text("Deleted the deck", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            } else {
                Text(snapshot.name, style = MaterialTheme.typography.titleMedium)
                Text("Main ${snapshot.count(DeckSection.MAIN)} · Resource ${snapshot.count(DeckSection.RESOURCE)}",
                    style = MaterialTheme.typography.bodySmall)
                // Differences against the other versions, so people can see what they would lose.
                val other = others.firstOrNull()?.snapshot
                if (other != null && !other.deleted) {
                    val keys = (snapshot.entries + other.entries).map { it.cardNumber to it.section }.distinct()
                    val diffs = keys.mapNotNull { (number, section) ->
                        val mine = snapshot.quantityOf(number, section)
                        val theirs = other.quantityOf(number, section)
                        if (mine == theirs) null else Triple(number to section, mine, theirs)
                    }
                    if (other.name != snapshot.name) Text("Name differs: \"${snapshot.name}\"", style = MaterialTheme.typography.bodySmall)
                    if (other.notes != snapshot.notes) Text("Notes differ", style = MaterialTheme.typography.bodySmall)
                    if (diffs.isEmpty() && other.name == snapshot.name) Text("Same cards as the other version.", style = MaterialTheme.typography.bodySmall)
                    diffs.forEach { (key, mine, theirs) ->
                        val (number, section) = key
                        val where = if (section == DeckSection.CONSIDERING) " (considering)" else ""
                        Text(
                            "${cards[number]?.name ?: number}$where: $mine here, $theirs in the other",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (mine > theirs) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            GameButton(if (snapshot.deleted) "Delete the deck" else "Keep this version", onClick = onKeep, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
