package app.lockon.ui.cards
import app.lockon.ui.components.Reveal
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import app.lockon.ui.components.Motion
import app.lockon.ui.components.motion
import app.lockon.ui.components.RevealValue
import app.lockon.ui.components.FadeSwap
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.components.LineIcons
import app.lockon.ui.components.glass
import app.lockon.ui.components.PillTextField
import app.lockon.platform.formatMonthDay
import app.lockon.platform.dayLabel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.padding
import app.lockon.ui.components.CountPill
import app.lockon.ui.components.LocalArtworkStore
import app.lockon.ui.components.PillSwitch
import app.lockon.ui.components.GameDialog
import app.lockon.ui.components.isTwilight
import androidx.compose.foundation.background
import app.lockon.ui.components.GlassIconButton
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
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
fun CatalogDialog(container: AppContainer, state: AppState, files: FileBridge, onDismiss: () -> Unit, showOtherSources: Boolean = false) {
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
            Column {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FadeSwap(preview, Modifier.fillMaxWidth(), contentKey = { it != null }) { pending ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (pending != null) {
                    PreviewSummary(pending)
                } else {
                    val official = info?.origin == CatalogOrigin.GCG_API
                    // Light theme's dialog is white, so its inner panels take a faint tint to stay grouped.
                    val innerPanel = if (isTwilight()) Modifier.glass(LockOnShapes.tile, strong = true)
                        else Modifier.clip(LockOnShapes.tile).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    val muted = MaterialTheme.typography.bodySmall.merge(TabularNumbers)
                    val showArtwork by container.catalog.showArtwork.collectAsState()
                    val images = LocalArtworkStore.current?.status?.collectAsState()?.value
                    fun checkOfficial() {
                        busy = true; problem = null
                        scope.launch {
                            try { check = container.catalogUpdater.checkCardDatabase() } catch (e: CatalogException) { problem = e.message }
                            busy = false
                        }
                    }
                    // When the official list was last checked, so the panel says whether it's current.
                    var lastChecked by remember { mutableStateOf<Long?>(null) }
                    // An update the weekly check already found: one tap gets it.
                    val knownUpdate by container.catalog.updateAvailable.collectAsState()
                    LaunchedEffect(check) { lastChecked = container.catalog.lastUpdateCheck() }
                    // What's installed, then the one action that keeps it current, always in the same place.
                    info?.let { current ->
                        Column(
                            Modifier.fillMaxWidth().then(innerPanel).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(if (official) "Official card list" else current.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            // An even grid: two columns on a phone, one row across a wide dialog.
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                val narrow = maxWidth < 420.dp
                                // Whether the list is current comes first; the title already names the official source.
                                val status = when {
                                    !official -> null
                                    check is CatalogUpdater.UpdateCheck.Available || (check == null && knownUpdate != null) -> "Update available"
                                    check is CatalogUpdater.UpdateCheck.UpToDate -> "Up to date"
                                    else -> lastChecked?.let { if (it > 0) "Checked ${dayLabel(it).lowercase().takeIf { d -> d == "today" || d == "yesterday" } ?: dayLabel(it)}" else "Not checked" }
                                }
                                listOfNotNull(
                                    status?.let { "Status" to it },
                                    "Version" to "${current.version}",
                                    "Cards" to "${current.cardCount}",
                                    ("Source" to current.origin.label).takeUnless { official },
                                    current.installedAt?.let { "Installed" to formatMonthDay(it) },
                                    images?.takeIf { showArtwork && it.total > 0 }?.let {
                                        "Images offline" to if (it.complete) "${it.total}" else "${it.saved} of ${it.total}"
                                    },
                                ).let { facts ->
                                    val columns = if (narrow) 2 else facts.size
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        facts.chunked(columns).forEach { row ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                row.forEach { (label, value) ->
                                                    Column(Modifier.weight(1f)) {
                                                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text(value, style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), maxLines = 1)
                                                    }
                                                }
                                                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                            }
                                        }
                                    }
                                }
                            }
                            RevealValue(check as? CatalogUpdater.UpdateCheck.Available) {
                                Text(
                                    if (it.newAltArts > 0) "${it.newAltArts} new alt arts are on Bandai's card list."
                                    else "Version ${it.version} is out: ${it.cardCount} entries, built ${it.builtAt}.",
                                    style = muted,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val wide = Modifier.weight(1f)
                                when (val result = check) {
                                    is CatalogUpdater.UpdateCheck.Available ->
                                        GameButton(if (result.newAltArts > 0) "Download update" else "Update to version ${result.version}", onClick = { attempt { container.catalogUpdater.latestCardDatabase() } }, enabled = !busy, icon = LineIcons.Download, modifier = wide)
                                    is CatalogUpdater.UpdateCheck.UpToDate ->
                                        GameButton("Up to date", onClick = {}, enabled = false, icon = LineIcons.Check, modifier = wide)
                                    else ->
                                        if (official && knownUpdate != null) GameButton("Download update", onClick = { attempt { container.catalogUpdater.latestCardDatabase() } }, enabled = !busy, icon = LineIcons.Download, modifier = wide)
                                        else if (official) GameButton("Check for updates", onClick = ::checkOfficial, enabled = !busy, icon = LineIcons.Sync, modifier = wide)
                                        else GameButton("Get the official card list", onClick = { attempt { container.catalogUpdater.latestCardDatabase() } }, enabled = !busy, icon = LineIcons.Download, modifier = wide)
                                }
                                Reveal(busy, sideways = true) { ReticleSpinner(size = 20.dp) }
                            }
                        }
                    }
                    // A link, a file or the sample: rarely needed, so folded into a panel of their own.
                    var others by remember { mutableStateOf(showOtherSources) }
                    Column(Modifier.fillMaxWidth().then(innerPanel)) {
                        Row(
                            Modifier.fillMaxWidth().clip(LockOnShapes.tile).clickable(onClickLabel = if (others) "Hide other sources" else "Show other sources") { others = !others }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Other sources", style = MaterialTheme.typography.labelLarge)
                            val turn by animateFloatAsState(if (others) 180f else 0f, motion(Motion.SHORT), label = "sources-chevron")
                            Icon(LineIcons.Chevd, null, Modifier.size(18.dp).graphicsLayer { rotationZ = turn }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Reveal(others) {
                            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // The link gets the full width so it stays readable; its button leads the row under it.
                                PillTextField(url, { url = it.trim() }, "Card list link", Modifier.fillMaxWidth())
                                // Equal widths: a column of full-width buttons on a phone, one row on a wide dialog.
                                BoxWithConstraints(Modifier.fillMaxWidth()) {
                                val stack = maxWidth < 420.dp
                                val cell = if (stack) Modifier.fillMaxWidth() else Modifier
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GameButton("Download", modifier = cell, onClick = { attempt { container.catalogUpdater.fromUrl(url) } }, enabled = url.isNotBlank() && !busy, style = GameButtonStyle.SECONDARY, icon = LineIcons.Download, compact = true)
                                    GameButton("Import file", modifier = cell, icon = LineIcons.Folder, onClick = {
                                        attempt { files.openText(listOf("application/json", "text/*"))?.let { container.catalogUpdater.fromText(it) } }
                                    }, style = GameButtonStyle.SECONDARY, enabled = !busy, compact = true)
                                    if (info?.origin != CatalogOrigin.BUNDLED) {
                                        GameButton("Switch to sample", modifier = cell, icon = LineIcons.Undo, onClick = {
                                            busy = true
                                            scope.launch {
                                                container.restoreSampleCatalog()
                                                busy = false
                                                state.notify("Sample catalog restored")
                                            }
                                        }, style = GameButtonStyle.SECONDARY, enabled = !busy, compact = true)
                                    }
                                }
                                }
                            }
                        }
                    }
                }
                }
                }
                RevealValue(problem) { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
            // The credit stays in view under whatever scrolls. The installed gcg-api source already
            // carries the ODbL credit, so it shows once.
            if (preview == null) {
                val official = info?.origin == CatalogOrigin.GCG_API
                val source = info?.source?.takeIf { it.isNotBlank() && info?.origin != CatalogOrigin.BUNDLED }
                listOfNotNull(source, GcgApi.ATTRIBUTION.takeUnless { official && source != null })
                    .forEach { Text(it, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
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
                        state.notify("Installed ${pending.checked.file.name} (${pending.checked.file.cards.size} cards)")
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
