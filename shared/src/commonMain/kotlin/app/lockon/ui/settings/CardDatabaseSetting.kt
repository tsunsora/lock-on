package app.lockon.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lockon.AppContainer
import app.lockon.platform.FileBridge
import app.lockon.ui.AppState
import app.lockon.ui.cards.CatalogDialog
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle

/**
 * The card database's row in Settings: which catalog is installed, a quiet note when a newer one is
 * out (UX rule C1), and the Catalog dialog for updates, other sources and artwork. This is the only
 * place the dialog opens from.
 */
@Composable
fun CardDatabaseSetting(container: AppContainer, state: AppState, files: FileBridge) {
    val info by container.catalog.info.collectAsState()
    val cards by container.catalog.cards.collectAsState()
    val update by container.catalog.updateAvailable.collectAsState()
    var managing by remember { mutableStateOf(false) }
    SettingRow(
        "Card database",
        value = if (info != null) "${cards.size} cards" else "None installed",
        // Only news earns a second line; the installed catalog above says the rest.
        note = update,
    ) {
        GameButton("Manage", onClick = { managing = true }, style = GameButtonStyle.SECONDARY, compact = true)
    }
    if (managing) CatalogDialog(container, state, files, onDismiss = { managing = false })
}
