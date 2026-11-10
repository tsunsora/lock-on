package app.lockon.ui.decks
import app.lockon.ui.theme.onCardColor
import app.lockon.ui.theme.cardColor
import app.lockon.ui.theme.TabularNumbers
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.sync.DeviceGlyph
import app.lockon.ui.components.glassColors
import app.lockon.ui.components.glass
import app.lockon.ui.components.clearOfNav
import app.lockon.ui.components.StateTag
import app.lockon.ui.components.ProgressGauge
import app.lockon.ui.components.CostOrb
import app.lockon.ui.components.ColorPips
import app.lockon.platform.isMobile
import app.lockon.platform.dayAndTimeLabel
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import app.lockon.ui.components.LineIcons
import app.lockon.ui.components.GlassIconButton
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
    val summary by container.decks.liveDeck(deckId).collectAsState(initial = null)
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
        Row(
            Modifier.fillMaxWidth().clearOfNav().padding(horizontal = if (wide) 0.dp else 12.dp, vertical = if (wide) 0.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Wide windows go back from the button in the corner.
            if (!wide) GlassIconButton(LineIcons.Back, "Back", state::goBack)
            Text(summary?.snapshot?.name.orEmpty(), Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            StateTag(legal = false, review = true)
        }
        val body: @Composable (DeckVersion, Modifier) -> Unit = { version, modifier ->
            VersionCard(
                version = version,
                others = versions - version,
                cards = cards,
                deviceLabel = deviceLabel(version.originDeviceId, container.identity.deviceId, devices),
                fromComputer = if (version.originDeviceId == container.identity.deviceId) !isMobile
                    else devices.firstOrNull { it.deviceId == version.originDeviceId }?.role == app.lockon.sync.DeviceRole.HOST,
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
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
                Row(Modifier.widthIn(max = 1240.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 56.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    versions.forEachIndexed { i, version ->
                        if (i > 0) Box(
                            Modifier.padding(top = 56.dp).size(40.dp).clip(CircleShape).background(glassColors().strong),
                            contentAlignment = Alignment.Center,
                        ) { Text("or", style = MaterialTheme.typography.labelLarge) }
                        body(version, Modifier.weight(1f))
                    }
                }
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
fun VersionCard(
    version: DeckVersion,
    others: List<DeckVersion>,
    cards: Map<String, GameCard>,
    deviceLabel: String,
    fromComputer: Boolean,
    modifier: Modifier,
    onKeep: () -> Unit,
) {
    val snapshot = version.snapshot
    val scheme = MaterialTheme.colorScheme
    Column(modifier.glass(LockOnShapes.panel, strong = true).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DeviceGlyph(fromComputer)
            Column(Modifier.weight(1f)) {
                Text(deviceLabel, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(dayAndTimeLabel(version.createdAt), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
        if (snapshot.deleted) {
            Text("Deleted the deck", style = MaterialTheme.typography.titleMedium, color = scheme.error)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProgressGauge("Main deck", snapshot.count(DeckSection.MAIN), 50, Modifier.weight(1f), compact = true, hold = true, inline = true)
                val colors = remember(snapshot, cards) { snapshot.entries.filter { it.section == DeckSection.MAIN }.flatMap { cards[it.cardNumber]?.colors.orEmpty() }.toSet() }
                ColorPips(colors, size = 18.dp)
            }
            // Differences against the other versions, so people can see what they would lose.
            val other = others.firstOrNull()?.snapshot
            if (other != null && !other.deleted) {
                val keys = (snapshot.entries + other.entries).map { it.cardNumber to it.section }.distinct()
                val diffs = keys.mapNotNull { (number, section) ->
                    val mine = snapshot.quantityOf(number, section)
                    val theirs = other.quantityOf(number, section)
                    if (mine == theirs) null else Triple(number to section, mine, theirs)
                }
                if (other.name != snapshot.name) Text("Named \"${snapshot.name}\"", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                if (other.notes != snapshot.notes) Text("Notes differ", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                if (diffs.isEmpty() && other.name == snapshot.name) Text("Same cards as the other version.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                diffs.forEach { (key, mine, theirs) ->
                    val (number, section) = key
                    val card = cards[number]
                    val by = mine - theirs
                    Row(
                        Modifier.fillMaxWidth().clip(LockOnShapes.button).background(scheme.onSurface.copy(alpha = 0.04f)).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CostOrb(card?.cost, cardColor(card?.colors?.firstOrNull()), onCardColor(card?.colors?.firstOrNull()), size = 24.dp)
                        Text(card?.name ?: number, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (section == DeckSection.SIDE) Text("Side", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                        Text(
                            if (by > 0) "+$by" else "−${-by}", style = MaterialTheme.typography.labelLarge.merge(TabularNumbers),
                            color = if (by > 0) scheme.primary else scheme.error,
                        )
                    }
                }
            }
        }
        GameButton(if (snapshot.deleted) "Delete the deck" else "Keep this version", onClick = onKeep, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), icon = LineIcons.Check)
    }
}
