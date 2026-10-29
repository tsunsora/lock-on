package app.lockon.ui.update
import app.lockon.ui.components.PillSwitch
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.GlassCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lockon.BuildInfo
import app.lockon.platform.formatTimestamp
import app.lockon.ui.components.SectionHeader
import app.lockon.ui.components.ReticleSpinner
import app.lockon.update.AppUpdater
import app.lockon.update.UpdatePlatform
import app.lockon.update.UpdateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The app's own version and updates, at the foot of Settings. This is the only place
 * updates show up (UX rule C1). [scope] outlives the screen, so a download keeps going after
 * leaving it.
 */
@Composable
fun AppUpdateCard(updater: AppUpdater?, scope: CoroutineScope) {
    GlassCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("Lock On")
            Text("Version ${BuildInfo.VERSION}", style = MaterialTheme.typography.titleSmall)
            if (updater == null || !updater.enabled) {
                Text("This build doesn't check for updates.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                UpdateBody(updater, scope)
            }
        }
    }
}

@Composable
private fun UpdateBody(updater: AppUpdater, scope: CoroutineScope) {
    val state by updater.state.collectAsState()
    val automatic by updater.automatic.collectAsState()
    val checkedAt by updater.lastCheckedAt.collectAsState()
    val windows = updater.installer.platform == UpdatePlatform.WINDOWS
    val canInstall = updater.installer.canInstall
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun Line(text: String, working: Boolean = false, error: Boolean = false) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (working) ReticleSpinner(size = 18.dp)
            Text(text, style = MaterialTheme.typography.bodyMedium,
                color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        }
    }

    @Composable
    fun Notes(notes: String?) {
        notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = muted) }
    }

    when (val s = state) {
        UpdateState.Idle -> Line(checkedAt?.let { "Last checked ${formatTimestamp(it)}" } ?: "Not checked for updates yet.")
        UpdateState.Checking -> Line("Checking for updates…", working = true)
        UpdateState.UpToDate -> Line("You have the latest version.")
        is UpdateState.Available -> {
            Line("Version ${s.version} is available.")
            Notes(s.notes)
            if (!canInstall) Text("Install it from the release page; this copy can't update itself.", style = MaterialTheme.typography.bodySmall, color = muted)
        }
        is UpdateState.Downloading -> {
            Line("Downloading version ${s.version}…")
            val progress = s.progress
            if (progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
            else LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
        is UpdateState.Ready -> {
            Line(
                if (windows && automatic) "Version ${s.version} is ready. It installs when you close Lock On."
                else "Version ${s.version} is ready to install.",
            )
            Notes(s.notes)
            s.problem?.let { Line(it, error = true) }
        }
        is UpdateState.Installing -> Line(if (windows) "Closing Lock On to install version ${s.version}…" else "Installing version ${s.version}…", working = true)
        is UpdateState.Failed -> Line(s.message, error = true)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (state) {
            is UpdateState.Available -> if (canInstall) GameButton("Download", onClick = { scope.launch { updater.downloadNow() } })
            is UpdateState.Ready -> GameButton(if (windows) "Restart and update" else "Install update", onClick = { scope.launch { updater.installNow() } })
            else -> Unit
        }
        val idle = state is UpdateState.Idle || state is UpdateState.UpToDate || state is UpdateState.Failed || state is UpdateState.Available
        if (idle) GameButton("Check for updates", onClick = { scope.launch { updater.checkNow() } }, style = GameButtonStyle.SECONDARY)
    }

    if (canInstall) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Update automatically", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (windows) "New versions download in the background and install when you close Lock On."
                    else "New versions download over Wi-Fi. Installing takes one tap here.",
                    style = MaterialTheme.typography.bodySmall, color = muted,
                )
            }
            PillSwitch(checked = automatic, onCheckedChange = { on -> scope.launch { updater.setAutomatic(on) } })
        }
    }
}
