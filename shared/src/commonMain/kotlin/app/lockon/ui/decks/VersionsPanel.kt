package app.lockon.ui.decks
import androidx.compose.ui.draw.rotate
import app.lockon.ui.components.panelFill
import app.lockon.ui.components.LocalTopBand
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import app.lockon.ui.components.LineIcons
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import app.lockon.platform.dayAndTimeLabel
import app.lockon.ui.components.CostOrb
import app.lockon.ui.components.Glyphs
import app.lockon.ui.theme.cardColor
import app.lockon.ui.components.glassColors
import app.lockon.ui.components.motion
import app.lockon.ui.theme.onCardColor
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.components.rememberPrefetchingListState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import app.lockon.ui.components.VerticalScrollBar
import app.lockon.ui.components.LocalNavClearance
import app.lockon.ui.components.GameDialog
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lockon.data.DeckRepository
import app.lockon.model.Card
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.DeckVersion
import app.lockon.platform.formatTimestamp
import app.lockon.rules.DeckDiff
import app.lockon.ui.components.SectionHeader
import app.lockon.ui.theme.TabularNumbers

/**
 * The deck's saved versions, newest first. The top row is the version being edited; opening an older
 * one compares it with the deck now and can bring its cards back. Name, notes, folder and matches
 * are left alone.
 */
@Composable
fun VersionsPanel(
    versions: List<DeckVersion>,
    current: DeckSnapshot,
    cards: Map<String, Card>,
    deviceLabel: (String) -> String,
    fromPhone: (String) -> Boolean,
    modifier: Modifier = Modifier,
    onKeep: () -> Unit,
    onRestore: (DeckVersion) -> Unit,
) {
    var open by remember { mutableStateOf<String?>(null) }
    val listState = rememberPrefetchingListState()
    // On a phone the versions read as one solid panel, and keeping one stays above the bar under the thumb.
    val phone = LocalTopBand.current.height == 0.dp
    val fill = panelFill()
    val headChanges = @Composable { versions.firstOrNull()?.let { head -> versions.getOrNull(1)?.let { Changes(it.snapshot, head.snapshot) } } }
    Column(modifier) {
    Box(Modifier.weight(1f).fillMaxWidth()) {
    LazyColumn(Modifier.fillMaxSize(), listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = if (phone) 8.dp else 16.dp, bottom = if (phone) 8.dp else 16.dp + LocalNavClearance.current),
        verticalArrangement = Arrangement.spacedBy(if (phone) 0.dp else 6.dp)) {
        if (!phone) item(key = "now") {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(LockOnShapes.tile).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Now", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                headChanges()
                GameButton("Keep as a version", onClick = onKeep, compact = true)
            }
        }
        itemsIndexed(versions.drop(1), key = { _, v -> v.revisionId }) { index, version ->
            val older = versions.getOrNull(index + 2)?.snapshot
            val isOpen = open == version.revisionId
            val corner = LockOnShapes.tileRadius
            val top = if (index == 0) corner else 0.dp
            val end = if (index == versions.size - 2) corner else 0.dp
            val strong = glassColors().strong
            val openFill by androidx.compose.animation.animateColorAsState(if (isOpen) strong else strong.copy(alpha = 0f), motion(), label = "version-open")
            val opening by androidx.compose.animation.core.animateFloatAsState(if (isOpen) 1f else 0f, motion(), label = "version-chevron")
            Column(
                Modifier.animateItem().fillMaxWidth()
                    .then(if (phone) Modifier.background(fill, RoundedCornerShape(top, top, end, end)).padding(top = if (index == 0) 6.dp else 0.dp, bottom = if (index == versions.size - 2) 6.dp else 0.dp) else Modifier)
                    .clip(LockOnShapes.tile)
                    .background(openFill)
                    .then(if (isOpen) Modifier.border(if (phone) 1.dp else app.lockon.ui.components.SelectionEdgeWidth, if (phone) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary, LockOnShapes.tile) else Modifier)
                    ,
            ) {
                Row(
                    Modifier.fillMaxWidth().clickable(onClickLabel = if (isOpen) "Close" else "Compare with the deck now") { open = if (isOpen) null else version.revisionId }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(if (fromPhone(version.originDeviceId)) Glyphs.Phone else Glyphs.Monitor, deviceLabel(version.originDeviceId), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    val note = when {
                        older == null -> "Created"
                        older.name != version.snapshot.name -> "Renamed"
                        else -> ""
                    }
                    // The note sits under the time, so nothing floats in the row's middle.
                    Column(Modifier.weight(1f)) {
                        Text(dayAndTimeLabel(version.createdAt), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        if (note.isNotEmpty()) Text(note, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Text("${version.snapshot.count(DeckSection.MAIN)}", style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.width(62.dp)) { if (older != null) Changes(older, version.snapshot) else Text("+${version.snapshot.count(DeckSection.MAIN)}", style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), color = MaterialTheme.colorScheme.primary) }
                    Icon(LineIcons.Chev, contentDescription = null, Modifier.size(20.dp).rotate(90f * opening), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                app.lockon.ui.components.Reveal(isOpen) { Compared(version, current, cards) { open = null; onRestore(version) } }
            }
        }
    }
    VerticalScrollBar(listState, Modifier.align(Alignment.TopEnd))
    }
    if (phone) Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = LocalNavClearance.current + 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        headChanges()
        GameButton("Keep as a version", onClick = onKeep, Modifier.weight(1f).height(48.dp))
    }
    }
}

/** Copies added and taken away between two snapshots. */
@Composable
private fun Changes(before: DeckSnapshot, after: DeckSnapshot) {
    val changes = remember(before, after) { DeckDiff.cards(before, after) }
    val added = changes.sumOf { (it.after - it.before).coerceAtLeast(0) }
    val removed = changes.sumOf { (it.before - it.after).coerceAtLeast(0) }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (added > 0) Text("+$added", style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), color = MaterialTheme.colorScheme.primary)
        if (removed > 0) Text("−$removed", style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), color = MaterialTheme.colorScheme.error)
    }
}

/** What restoring [version] would change in the deck now, and the button that does it. */
@Composable
private fun Compared(version: DeckVersion, current: DeckSnapshot, cards: Map<String, Card>, onRestore: () -> Unit) {
    val changes = remember(version, current) { DeckDiff.cards(current, version.snapshot) }
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (changes.isEmpty()) Text("Same cards as now.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        changes.forEach { change ->
            val card = cards[change.cardNumber]
            val by = change.after - change.before
            Row(
                Modifier.fillMaxWidth().clip(LockOnShapes.button).border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), LockOnShapes.button).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CostOrb(card?.cost, cardColor(card?.colors?.firstOrNull()), onCardColor(card?.colors?.firstOrNull()), size = 24.dp)
                Text(card?.name ?: change.cardNumber, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                if (change.section == DeckSection.SIDE) Text("Side", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (by > 0) "+$by" else "−${-by}", style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), color = if (by > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        }
        if (changes.isNotEmpty()) Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.CenterEnd) {
            GameButton("Restore these cards", onClick = onRestore, icon = Glyphs.Undo, compact = true)
        }
    }
}
