package app.lockon.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.lockon.AppContainer
import app.lockon.catalog.CatalogPreview
import app.lockon.data.CatalogException
import app.lockon.data.CatalogOrigin
import app.lockon.platform.FileBridge
import app.lockon.platform.formatTimestamp
import app.lockon.ui.AppState
import app.lockon.ui.components.HudHeader
import app.lockon.ui.theme.TabularNumbers
import kotlinx.coroutines.launch

/**
 * Shows the installed catalog and brings in a new one from a file or a link. Every install goes
 * through a preview first, because replacing the catalog can't be undone (UX rule C2).
 */
@Composable
fun CatalogDialog(container: AppContainer, state: AppState, files: FileBridge, onDismiss: () -> Unit) {
    val info by container.catalog.info.collectAsState()
    var url by remember { mutableStateOf(info?.updateUrl.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<CatalogPreview?>(null) }
    val scope = state.appScope

    fun attempt(block: suspend () -> CatalogPreview?) {
        busy = true; problem = null
        scope.launch {
            try {
                preview = block()
            } catch (e: CatalogException) {
                problem = e.message
            } catch (e: Exception) {
                problem = "Something went wrong: ${e.message}"
            } finally {
                busy = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (preview == null) "Card catalog" else "Install this catalog?") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val pending = preview
                if (pending != null) {
                    PreviewSummary(pending)
                } else {
                    info?.let { current ->
                        HudHeader("Installed", readout = "${current.cardCount} cards")
                        Text(current.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            listOfNotNull(
                                "Version ${current.version}",
                                current.origin.label,
                                current.installedAt?.let { "installed ${formatTimestamp(it)}" },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall.merge(TabularNumbers),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (current.source.isNotBlank()) {
                            Text(current.source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val showArtwork by container.catalog.showArtwork.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Show card artwork", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Images come from the catalog's links and are cached on this device. They never sync.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = showArtwork, onCheckedChange = { scope.launch { container.catalog.setShowArtwork(it) } })
                    }
                    HorizontalDivider()
                    HudHeader("Update from a link")
                    Text(
                        "Paste the link to a Lock On catalog file. Lock On remembers it, so checking for updates later is one tap.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        url, { url = it.trim() },
                        label = { Text("Catalog link (https://)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { attempt { container.catalogUpdater.fromUrl(url) } }, enabled = url.isNotBlank() && !busy) {
                            Text(if (url == info?.updateUrl) "Check for update" else "Download")
                        }
                        OutlinedButton(onClick = {
                            attempt { files.openText(listOf("application/json", "text/*"))?.let { container.catalogUpdater.fromText(it) } }
                        }, enabled = !busy) { Text("Import file") }
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                    if (info?.origin != CatalogOrigin.BUNDLED) {
                        TextButton(onClick = {
                            busy = true
                            scope.launch {
                                container.restoreSampleCatalog()
                                busy = false
                                state.notify("Sample catalog restored.")
                            }
                        }, enabled = !busy) { Text("Go back to the sample catalog") }
                    }
                }
                problem?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = {
            val pending = preview
            if (pending != null) {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        container.catalogUpdater.install(pending)
                        busy = false
                        state.notify("Installed ${pending.checked.file.name} (${pending.checked.file.cards.size} cards).")
                        onDismiss()
                    }
                }) { Text(if (pending.isUpdate) "Update" else "Replace catalog") }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (preview != null) TextButton(onClick = { preview = null }) { Text("Back") }
        },
    )
}

@Composable
private fun PreviewSummary(preview: CatalogPreview) {
    val file = preview.checked.file
    Text(file.name, style = MaterialTheme.typography.titleMedium)
    Text(
        "Version ${file.version} · ${file.cards.size} cards · ${file.printings.size} printings" +
            (file.rulesets?.takeIf { it.isNotEmpty() }?.let { " · ${it.size} rulesets" } ?: ""),
        style = MaterialTheme.typography.bodySmall.merge(TabularNumbers),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (file.source.isNotBlank()) Text(file.source, style = MaterialTheme.typography.bodySmall)
    Text(
        when {
            !preview.isUpdate -> "This replaces the current catalog. Your decks stay exactly as they are."
            preview.isNewer -> "This is a newer version of the catalog you have."
            else -> "This is the same or an older version than the one installed."
        },
        style = MaterialTheme.typography.bodyMedium,
    )
    if (preview.missingFromDecks.isNotEmpty()) {
        Text(
            "${preview.missingFromDecks.size} cards used in your decks aren't in this catalog " +
                "(${preview.missingFromDecks.sorted().take(4).joinToString()}${if (preview.missingFromDecks.size > 4) ", …" else ""}). " +
                "They stay in your decks and show as unknown until a catalog includes them.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
    preview.checked.warnings.forEach {
        Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
