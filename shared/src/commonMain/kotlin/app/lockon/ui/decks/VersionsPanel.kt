package app.lockon.ui.decks
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
 * The deck's saved versions, newest first. Opening an older one compares it with the deck now and
 * can bring its cards back; name, notes, folder and matches are left alone.
 */
@Composable
fun VersionsPanel(
    versions: List<DeckVersion>,
    current: DeckSnapshot,
    cards: Map<String, Card>,
    deviceLabel: (String) -> String,
    modifier: Modifier = Modifier,
    onKeep: () -> Unit,
    onRestore: (DeckVersion) -> Unit,
) {
    var comparing by remember { mutableStateOf<DeckVersion?>(null) }
    LazyColumn(modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = LocalNavClearance.current)) {
        item(key = "keep") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("Versions", readout = "${versions.size}")
                Text(
                    "Changes join the current version until you keep it, it syncs to another device, or the deck sits for " +
                        "${DeckRepository.VERSION_IDLE_MS / 60_000} minutes. Open an older version to compare it or bring its cards back.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GameButton("Keep as a version", onClick = onKeep, style = GameButtonStyle.SECONDARY)
            }
        }
        itemsIndexed(versions, key = { _, v -> v.revisionId }) { index, version ->
            val older = versions.getOrNull(index + 1)?.snapshot
            Column(
                Modifier.animateItem().fillMaxWidth().clickable(enabled = index > 0) { comparing = version }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    if (index == 0) "Current · ${formatTimestamp(version.createdAt)}" else formatTimestamp(version.createdAt),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (index == 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${deviceLabel(version.originDeviceId)} · Main ${version.snapshot.count(DeckSection.MAIN)} · Resource ${version.snapshot.count(DeckSection.RESOURCE)}",
                    style = MaterialTheme.typography.bodySmall.merge(TabularNumbers), color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (older == null) "Created" else DeckDiff.summary(older, version.snapshot),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            HorizontalDivider(Modifier.padding(start = 16.dp))
        }
    }

    comparing?.let { version ->
        val changes = remember(version, current) { DeckDiff.cards(version.snapshot, current) }
        GameDialog(
            onDismissRequest = { comparing = null },
            title = "Version from ${formatTimestamp(version.createdAt)}",
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (changes.isEmpty()) Text("It has the same cards as the deck now.")
                    DeckSection.entries.forEach { section ->
                        val inSection = changes.filter { it.section == section }
                        if (inSection.isEmpty()) return@forEach
                        Text(section.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp))
                        inSection.forEach { change ->
                            Text(
                                "${cards[change.cardNumber]?.name ?: change.cardNumber}: ${change.before} then, ${change.after} now",
                                style = MaterialTheme.typography.bodyMedium.merge(TabularNumbers),
                            )
                        }
                    }
                    if (changes.isNotEmpty()) {
                        Text(
                            "Restoring brings back these cards. The name, notes, folder and match log stay as they are now.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                if (changes.isNotEmpty()) GameButton("Restore these cards", onClick = { comparing = null; onRestore(version) }, compact = true)
            },
            dismissButton = { GameButton("Close", onClick = { comparing = null }, style = GameButtonStyle.GHOST, compact = true) },
        )
    }
}
