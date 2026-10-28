package app.lockon.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lockon.AppContainer
import app.lockon.platform.FileBridge
import app.lockon.ui.AppState
import app.lockon.ui.cards.CatalogDialog
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.GlassCard
import app.lockon.ui.components.SectionHeader

/**
 * The card database in Settings: which catalog is installed, a quiet note when a newer one is out
 * (UX rule C1), and the Catalog dialog for updates, other sources and artwork. Cards keeps its own
 * Catalog button for the same dialog.
 */
@Composable
fun CardDatabaseCard(container: AppContainer, state: AppState, files: FileBridge) {
    val info by container.catalog.info.collectAsState()
    val cards by container.catalog.cards.collectAsState()
    val update by container.catalog.updateAvailable.collectAsState()
    var managing by remember { mutableStateOf(false) }
    GlassCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Card database", readout = "${cards.size}")
            Text(
                info?.let { "${it.name} · version ${it.version}" } ?: "No catalog installed",
                style = MaterialTheme.typography.titleSmall,
            )
            // Only news earns a line here; the installed catalog's name above says the rest.
            update?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameButton("Manage card database", onClick = { managing = true }, style = GameButtonStyle.SECONDARY, compact = true)
            }
        }
    }
    if (managing) CatalogDialog(container, state, files, onDismiss = { managing = false })
}
