package app.lockon.ui.settings

import app.lockon.ui.components.VerticalScrollBar
import androidx.compose.foundation.layout.BoxWithConstraints
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

    // Wide windows set the groups side by side, so the panels fill the width instead of leaving most
    // of each one empty: this device and how the app looks on the left, cards and updates on the right.
    val left = @Composable {
        SettingsGroup("Appearance") {
            Choice("Theme", null, ThemeChoice.entries, theme, { it.label }) {
                scope.launch { settings.setTheme(it) }
            }
            Choice("Interface size", null, InterfaceSize.entries, size, { it.label }) {
                scope.launch { settings.setSize(it) }
            }
            Choice("Animations", null, MotionChoice.entries, motion, { it.label }) {
                scope.launch { settings.setMotion(it) }
            }
        }

        // The group's title names the choice, so the choice needs no second label under it.
        SettingsGroup("Open on") {
            Choice(null, null, StartChoice.entries, start, { it.label }) {
                scope.launch { settings.setStart(it) }
            }
        }

        SettingsGroup("This device") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Device name", style = MaterialTheme.typography.titleSmall)
                    Text(
                        // A pending name is the one place a note earns its line: it isn't live yet.
                        if (deviceName == container.identity.deviceName) deviceName
                        else "$deviceName · from the next launch",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                GameButton("Rename", onClick = { renaming = true }, style = GameButtonStyle.SECONDARY, compact = true)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Welcome tour", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                GameButton("Show again", onClick = { state.tourStop = 0; state.touring = true }, style = GameButtonStyle.SECONDARY, compact = true)
            }
        }
    }
    val right = @Composable {
        SettingsGroup("Cards") {
            Toggle("Show card artwork", showArtwork) {
                scope.launch { container.catalog.setShowArtwork(it) }
            }
            Toggle("Check weekly for card list updates", catalogChecks) {
                scope.launch { settings.setCatalogChecks(it) }
            }
        }
        CardDatabaseCard(container, state, files)
        AppUpdateCard(container.updater, state.appScope)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()
        val scroll = Modifier.fillMaxSize().verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp + LocalNavClearance.current)
        if (maxWidth >= 900.dp) {
            Row(scroll, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) { left() }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) { right() }
            }
        } else {
            Column(scroll, verticalArrangement = Arrangement.spacedBy(14.dp)) { left(); right() }
        }
        VerticalScrollBar(scrollState, Modifier.align(Alignment.TopEnd))
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
private fun Toggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        PillSwitch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Choice(title: String?, description: String?, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        title?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
        description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                PillChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
            }
        }
    }
}
