package app.lockon.ui.cards
import app.lockon.ui.components.Reveal
import app.lockon.ui.components.RevealValue
import app.lockon.ui.components.FadeSwap
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.components.LineIcons
import app.lockon.ui.components.glass
import app.lockon.ui.components.PillTextField
import app.lockon.platform.formatMonthDay
import androidx.compose.foundation.layout.padding
import app.lockon.ui.components.CountPill
import app.lockon.ui.components.LocalArtworkStore
import app.lockon.ui.components.PillSwitch
import app.lockon.ui.components.GameDialog
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle

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
import app.lockon.catalog.CatalogUpdater
import app.lockon.catalog.GcgApi
import app.lockon.data.CatalogException
import app.lockon.data.CatalogOrigin
import app.lockon.platform.FileBridge
import app.lockon.platform.formatTimestamp
import app.lockon.ui.AppState
import app.lockon.ui.components.SectionHeader
import app.lockon.ui.components.ReticleSpinner
import app.lockon.ui.theme.TabularNumbers
import kotlinx.coroutines.launch

/**
 * Shows the installed catalog and brings in a new one from a file or a link. Every install goes
 * through a preview first, because replacing the catalog can't be undone (UX rule C2).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatalogDialog(container: AppContainer, state: AppState, files: FileBridge, onDismiss: () -> Unit) {
    val info by container.catalog.info.collectAsState()
    var url by remember { mutableStateOf(info?.updateUrl.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<CatalogPreview?>(null) }
    var check by remember { mutableStateOf<CatalogUpdater.UpdateCheck?>(null) }
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

    GameDialog(
        onDismissRequest = onDismiss,
        title = if (preview == null) "Card database" else "Install this catalog?",
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FadeSwap(preview, Modifier.fillMaxWidth(), contentKey = { it != null }) { pending ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pending != null) {
                    PreviewSummary(pending)
                } else {
                    val official = info?.origin == CatalogOrigin.GCG_API
                    val muted = MaterialTheme.typography.bodySmall.merge(TabularNumbers)
                    info?.let { current ->
                        Column(
                            Modifier.fillMaxWidth().glass(LockOnShapes.tile, strong = true).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(if (official) "Official card list" else current.name, style = MaterialTheme.typography.titleMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOfNotNull(
                                    "Version" to "${current.version}",
                                    "Cards" to "${current.cardCount}",
                                    "Source" to if (official) "gcg-api" else current.origin.label,
                                    current.installedAt?.let { "Installed" to formatMonthDay(it) },
                                ).forEach { (label, value) ->
                                    Column {
                                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(value, style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    if (!official) {
                        Text(
                            "Get every card from the official card list. Your decks stay as they are.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        when (val result = check) {
                            is CatalogUpdater.UpdateCheck.Available ->
                                GameButton("Download update", onClick = { attempt { container.catalogUpdater.latestCardDatabase() } }, enabled = !busy, compact = true)
                            else -> if (official) {
                                GameButton("Check for updates", onClick = {
                                    busy = true; problem = null
                                    scope.launch {
                                        try { check = container.catalogUpdater.checkCardDatabase() } catch (e: CatalogException) { problem = e.message }
                                        busy = false
                                    }
                                }, enabled = !busy, icon = LineIcons.Sync, compact = true)
                                Reveal(result is CatalogUpdater.UpdateCheck.UpToDate, sideways = true) {
                                    Text("Up to date", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                                }
                            } else {
                                GameButton("Download card database", onClick = { attempt { container.catalogUpdater.latestCardDatabase() } }, enabled = !busy, compact = true)
                            }
                        }
                        Reveal(busy, sideways = true) { ReticleSpinner(size = 20.dp) }
                    }
                    RevealValue(check as? CatalogUpdater.UpdateCheck.Available) {
                        Text(
                            if (it.newAltArts > 0) "${it.newAltArts} new alt arts are on Bandai's card list."
                            else "Version ${it.version} is out: ${it.cardCount} entries, built ${it.builtAt}.",
                            style = muted,
                        )
                    }
                    val showArtwork by container.catalog.showArtwork.collectAsState()
                    val images = LocalArtworkStore.current?.status?.collectAsState()?.value
                    if (showArtwork && images != null && images.total > 0) Text(
                        when {
                            images.complete -> "${images.total} images saved offline"
                            images.downloading -> "Saving images: ${images.saved} of ${images.total}"
                            else -> "${images.saved} of ${images.total} images saved, the rest when online"
                        },
                        style = muted, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("Other sources", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    run {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            PillTextField(url, { url = it.trim() }, "https://", Modifier.weight(1f))
                            GameButton(if (url == info?.updateUrl) "Check for update" else "Download", onClick = { attempt { container.catalogUpdater.fromUrl(url) } }, enabled = url.isNotBlank() && !busy, style = GameButtonStyle.SECONDARY, icon = LineIcons.Download, compact = true)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            GameButton("Import file", icon = LineIcons.Folder, onClick = {
                                attempt { files.openText(listOf("application/json", "text/*"))?.let { container.catalogUpdater.fromText(it) } }
                            }, style = GameButtonStyle.SECONDARY, enabled = !busy, compact = true)
                            if (info?.origin != CatalogOrigin.BUNDLED) {
                                GameButton("Restore sample", onClick = {
                                    busy = true
                                    scope.launch {
                                        container.restoreSampleCatalog()
                                        busy = false
                                        state.notify("Sample catalog restored.")
                                    }
                                }, style = GameButtonStyle.SECONDARY, enabled = !busy, icon = LineIcons.Undo, compact = true)
                            }
                        }
                    }
                    // The installed gcg-api source already carries the ODbL credit, so it shows once.
                    val source = info?.source?.takeIf { it.isNotBlank() && info?.origin != CatalogOrigin.BUNDLED }
                    listOfNotNull(source, GcgApi.ATTRIBUTION.takeUnless { official && source != null })
                        .forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
                }
                }
                }
                RevealValue(problem) { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = {
            val pending = preview
            if (pending != null) {
                GameButton(if (pending.isUpdate) "Update" else "Replace catalog", onClick = {
                    busy = true
                    scope.launch {
                        container.catalogUpdater.install(pending)
                        busy = false
                        state.notify("Installed ${pending.checked.file.name} (${pending.checked.file.cards.size} cards).")
                        onDismiss()
                    }
                }, enabled = !busy, compact = true)
            } else {
                GameButton("Close", onClick = onDismiss, style = GameButtonStyle.SECONDARY, compact = true)
            }
        },
        dismissButton = {
            if (preview != null) GameButton("Back", onClick = { preview = null }, style = GameButtonStyle.GHOST, compact = true)
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
