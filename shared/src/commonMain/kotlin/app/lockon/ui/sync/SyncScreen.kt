package app.lockon.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.lockon.platform.formatTimestamp
import app.lockon.sync.ClientSyncController
import app.lockon.sync.HostSyncController
import app.lockon.sync.PairedDevice
import app.lockon.sync.PairingInvite
import app.lockon.sync.SyncController
import app.lockon.sync.SyncProtocol
import app.lockon.sync.SyncStatus
import app.lockon.sync.verificationCode
import app.lockon.ui.components.QrCodeView
import kotlinx.coroutines.delay
import kotlin.time.Clock

@Composable
fun SyncScreen(controller: SyncController, deviceName: String) {
    val status by controller.status.collectAsState()
    val devices by controller.pairedDevices.collectAsState(initial = emptyList())
    var forgetting by remember { mutableStateOf<PairedDevice?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("This device: $deviceName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        StatusLine(status)
        when (controller) {
            is HostSyncController -> HostSection(controller)
            is ClientSyncController -> ClientSection(controller, devices.isEmpty())
        }
        Text("Paired devices", style = MaterialTheme.typography.titleMedium)
        if (devices.isEmpty()) {
            Text(
                if (controller is HostSyncController) "No phones paired yet." else "No desktop paired yet.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        devices.forEach { device ->
            DeviceCard(
                device = device,
                working = status.working,
                onSync = (controller as? ClientSyncController)?.let { c -> { c.syncNow(device.deviceId) } },
                onForget = { forgetting = device },
            )
        }
        Text(
            "Only decks sync. Each device keeps its own card catalog, and no account or cloud server is involved.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    forgetting?.let { device ->
        AlertDialog(
            onDismissRequest = { forgetting = null },
            title = { Text("Forget ${device.name}?") },
            text = { Text("It will need to pair again before it can sync. Decks on both devices stay as they are.") },
            confirmButton = { TextButton(onClick = { controller.forget(device.deviceId); forgetting = null }) { Text("Forget") } },
            dismissButton = { TextButton(onClick = { forgetting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StatusLine(status: SyncStatus) {
    val message = status.message ?: return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (status.working) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(
            message,
            color = if (status.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun HostSection(controller: HostSyncController) {
    val info by controller.hostInfo.collectAsState()
    val prompt by controller.pairingPrompt.collectAsState()
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = Clock.System.now().toEpochMilliseconds() } }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Pair a phone", style = MaterialTheme.typography.titleMedium)
            val current = info
            when {
                current == null -> Text("Starting the sync service…")
                current.error != null -> Text(current.error, color = MaterialTheme.colorScheme.error)
                else -> {
                    Text("In Lock On on your phone, open Sync and scan this code. Keep both devices on the same network.",
                        style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
                        current.qr?.let { QrCodeView(it, Modifier.size(220.dp)) }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.widthIn(max = 360.dp)) {
                            Text("Or enter it by hand:", style = MaterialTheme.typography.labelLarge)
                            if (current.addresses.size > 1) {
                                Text("Address", style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    current.addresses.forEach { a ->
                                        FilterChip(selected = a == current.addresses.first(), onClick = { controller.selectAddress(a) }, label = { Text(a) })
                                    }
                                }
                            } else {
                                Field("Address", current.addresses.firstOrNull() ?: "No network found")
                            }
                            Field("Port", "${current.port}")
                            Field("Pairing code", current.pairingCode)
                            Field("Verification", verificationCode(current.fingerprint))
                            val seconds = ((current.codeExpiresAt - now) / 1000).coerceAtLeast(0)
                            Text("Code changes in ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = controller::newPairingCode) { Text("New code") }
                        }
                    }
                    Text(
                        "If your phone cannot connect, allow Lock On through the desktop firewall for private networks.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    prompt?.let { p ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Pair with ${p.deviceName}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Check that the phone shows the same verification code:")
                    Text(p.verificationCode, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)
                }
            },
            confirmButton = { Button(onClick = { controller.answerPairing(p.requestId, true) }) { Text("Pair") } },
            dismissButton = { TextButton(onClick = { controller.answerPairing(p.requestId, false) }) { Text("Reject") } },
        )
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ClientSection(controller: ClientSyncController, firstPairing: Boolean) {
    val verification by controller.pendingVerification.collectAsState()
    val status by controller.status.collectAsState()
    var manual by remember { mutableStateOf(!controller.canScanQr) }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf(SyncProtocol.DEFAULT_PORT.toString()) }
    var code by remember { mutableStateOf("") }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (firstPairing) "Pair with your desktop" else "Pair another desktop", style = MaterialTheme.typography.titleMedium)
            Text("Open Lock On on the desktop, go to Sync, and scan the code it shows.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (controller.canScanQr) Button(onClick = controller::scanQr, enabled = !status.working) { Text("Scan QR code") }
                OutlinedButton(onClick = { manual = !manual }) { Text(if (manual) "Hide manual entry" else "Enter address") }
            }
            if (manual) {
                OutlinedTextField(host, { host = it.trim() }, label = { Text("Desktop address") }, singleLine = true,
                    placeholder = { Text("192.168.1.20") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(port, { port = it.filter(Char::isDigit) }, label = { Text("Port") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(code, { code = it.trim().uppercase() }, label = { Text("Pairing code") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = { controller.pair(PairingInvite(host, port.toIntOrNull() ?: SyncProtocol.DEFAULT_PORT, code, fingerprint = null)) },
                    enabled = host.isNotBlank() && code.isNotBlank() && !status.working,
                ) { Text("Pair") }
            }
        }
    }

    verification?.let { v ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Confirm on the desktop") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Accept the request on the desktop if it shows this code:")
                    Text(v, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun DeviceCard(device: PairedDevice, working: Boolean, onSync: (() -> Unit)?, onForget: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
                if (device.host != null) Text("${device.host}:${device.port}", style = MaterialTheme.typography.bodySmall)
                Text(
                    device.lastSyncAt?.let { "Last successful sync ${formatTimestamp(it)}" } ?: "Not synced yet",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                device.lastError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (onSync != null) Button(onClick = onSync, enabled = !working) { Text("Sync now") }
                TextButton(onClick = onForget) { Text("Forget") }
            }
        }
    }
}
