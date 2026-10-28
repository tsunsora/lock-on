package app.lockon.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lockon.AppContainer
import app.lockon.data.InterfaceSize
import app.lockon.data.MotionChoice
import app.lockon.data.StartChoice
import app.lockon.data.ThemeChoice
import app.lockon.platform.FileBridge
import app.lockon.ui.AppState
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.GameDialog
import app.lockon.ui.components.GlassCard
import app.lockon.ui.components.LocalNavClearance
import app.lockon.ui.components.PillChip
import app.lockon.ui.components.PillSwitch
import app.lockon.ui.components.SectionHeader
import app.lockon.ui.update.AppUpdateCard
import kotlinx.coroutines.launch

/**
 * This device's preferences: how Lock On looks and moves, where it opens, the card database, the
 * device's name and the app's updates. Sync has its own tab beside this one.
 */
@Composable
fun SettingsScreen(container: AppContainer, state: AppState, files: FileBridge) {
    val settings = container.settings
    val scope = rememberCoroutineScope()
    val theme by settings.theme.collectAsState()
    val size by settings.size.collectAsState()
    val motion by settings.motion.collectAsState()
    val start by settings.start.collectAsState()
    val catalogChecks by settings.catalogChecks.collectAsState()
    val showArtwork by container.catalog.showArtwork.collectAsState()
    // The name is read once at launch, so a new one shows here until then.
    var deviceName by remember { mutableStateOf(container.identity.deviceName) }
    var renaming by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp + LocalNavClearance.current),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SettingsGroup("Appearance") {
            Choice("Theme", "Sky is bright glass; Twilight is the same look after dark.", ThemeChoice.entries, theme, { it.label }) {
                scope.launch { settings.setTheme(it) }
            }
            Choice("Interface size", "Scales text, cards and controls together.", InterfaceSize.entries, size, { it.label }) {
                scope.launch { settings.setSize(it) }
            }
            Choice("Animations", "Reduced swaps slides and fades for instant changes.", MotionChoice.entries, motion, { it.label }) {
                scope.launch { settings.setMotion(it) }
            }
        }

        SettingsGroup("Startup") {
            Choice("Open on", null, StartChoice.entries, start, { it.label }) {
                scope.launch { settings.setStart(it) }
            }
        }

        SettingsGroup("Cards") {
            Toggle("Show card artwork", "Images download once from the card list's links and stay on this device, so cards show offline. They never sync.", showArtwork) {
                scope.launch { container.catalog.setShowArtwork(it) }
            }
            Toggle("Check for card list updates", "Once a week, only for the official card list. Nothing changes until you choose to update.", catalogChecks) {
                scope.launch { settings.setCatalogChecks(it) }
            }
        }
        CardDatabaseCard(container, state, files)

        SettingsGroup("This device") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Device name", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (deviceName == container.identity.deviceName) "$deviceName · paired devices see this name"
                        else "$deviceName · used from the next time Lock On opens",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                GameButton("Rename", onClick = { renaming = true }, style = GameButtonStyle.SECONDARY, compact = true)
            }
        }

        AppUpdateCard(container.updater, state.appScope)
    }

    if (renaming) {
        var text by remember { mutableStateOf(deviceName) }
        GameDialog(
            onDismissRequest = { renaming = false },
            title = "Rename this device",
            text = {
                OutlinedTextField(text, { text = it.take(60) }, label = { Text("Device name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                GameButton("Rename", compact = true, enabled = text.isNotBlank(), onClick = {
                    val name = text.trim()
                    renaming = false
                    deviceName = name
                    state.appScope.launch { container.devices.rename(name) }
                })
            },
            dismissButton = { GameButton("Cancel", onClick = { renaming = false }, style = GameButtonStyle.GHOST, compact = true) },
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    GlassCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader(title)
            content()
        }
    }
}

@Composable
private fun Toggle(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        PillSwitch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Choice(title: String, description: String?, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                PillChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
            }
        }
    }
}
