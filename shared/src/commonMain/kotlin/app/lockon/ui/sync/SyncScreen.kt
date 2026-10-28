package app.lockon.ui.sync
import androidx.compose.foundation.layout.BoxWithConstraints
import app.lockon.ui.components.LocalNavClearance
import app.lockon.ui.components.PillChip
import app.lockon.ui.components.PillSwitch
import app.lockon.ui.components.GameDialog
import app.lockon.ui.components.GlassCard
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import app.lockon.ui.components.CautionStripes
import app.lockon.ui.components.ReticleSpinner
import app.lockon.ui.components.QrCodeView
import app.lockon.ui.theme.TabularNumbers
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import app.lockon.ui.components.SectionHeader
import app.lockon.sync.AutoSyncState
import app.lockon.sync.NearbyDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Switch
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlin.time.Clock
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.ui.semantics.Role
import app.lockon.model.DeckSection
import app.lockon.model.DeckSummary
import app.lockon.sync.DeckSharing
import app.lockon.sync.OutgoingShare

@Composable
fun SyncScreen(
    controller: SyncController,
    deviceName: String,
    /** Decks that can be sent to a nearby device. */
    decks: List<DeckSummary> = emptyList(),
    /** Last item on the screen: the app's version and updates. */
    footer: @Composable () -> Unit = {},
) {
    val status by controller.status.collectAsState()
    val devices by controller.pairedDevices.collectAsState(initial = emptyList())
    var forgetting by remember { mutableStateOf<PairedDevice?>(null) }
    val liveHosts by ((controller as? ClientSyncController)?.autoStates ?: remember { MutableStateFlow(emptyMap()) }).collectAsState()
    val liveClients by ((controller as? HostSyncController)?.liveClients ?: remember { MutableStateFlow(emptySet()) }).collectAsState()

    val pairedIds = devices.map { it.deviceId }.toSet()
    // What is happening now and the devices nearby, then the devices already paired. Wide windows set
    // them side by side, so paired devices are in view instead of below the pairing code.
    val now = @Composable {
        // The screen header names the device; this says how syncing is going right now.
        SectionHeader("Status")
        StatusLine(status)
        controller.sharing?.let { SendCard(it, decks, pairedIds, isHost = controller is HostSyncController) }
        when (controller) {
            is HostSyncController -> HostSection(controller, pairedIds)
            is ClientSyncController -> ClientSection(controller, pairedIds)
        }
    }
    val paired = @Composable {
        SectionHeader("Paired devices")
        if (devices.isEmpty()) {
            Text(
                if (controller is HostSyncController) "No phones paired yet." else "No computer paired yet.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        devices.forEach { device ->
            DeviceCard(
                device = device,
                working = status.working,
                live = when (controller) {
                    is ClientSyncController -> liveHosts[device.deviceId]?.let { it.label to (it == AutoSyncState.LIVE) }
                    is HostSyncController -> if (device.deviceId in liveClients) "Connected now: changes sync automatically" to true else null
                },
                onSync = (controller as? ClientSyncController)?.let { c -> { c.syncNow(device.deviceId) } },
                onForget = { forgetting = device },
            )
        }
        Text(
            "Only decks sync. Each device keeps its own card catalog, and no account or cloud server is involved.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        footer()
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scroll = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp + LocalNavClearance.current)
        if (maxWidth >= 900.dp) {
            Row(scroll, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(14.dp)) { now() }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) { paired() }
            }
        } else {
            Column(scroll, verticalArrangement = Arrangement.spacedBy(14.dp)) { now(); paired() }
        }
    }

    controller.sharing?.let { IncomingShareDialog(it) }

    forgetting?.let { device ->
        GameDialog(
            onDismissRequest = { forgetting = null },
            title = "Forget ${device.name}?",
            text = { Text("It will need to pair again before it can sync. Decks on both devices stay as they are.") },
            confirmButton = { GameButton("Forget", onClick = { controller.forget(device.deviceId); forgetting = null }, compact = true) },
            dismissButton = { GameButton("Cancel", onClick = { forgetting = null }, style = GameButtonStyle.GHOST, compact = true) },
        )
    }
}

@Composable
private fun StatusLine(status: SyncStatus) {
    val message = status.message ?: return
    Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (status.isError) CautionStripes(Modifier.width(8.dp).fillMaxHeight())
        if (status.working) ReticleSpinner(size = 18.dp)
        Text(
            message,
            color = if (status.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Nearby devices, LocalSend-style: everything with Lock On open on this network, with one tap to
 * connect. Seeing a device grants nothing; a confirmed pairing does (UX rule T1).
 */
@Composable
private fun NearbyCard(
    title: String,
    devices: List<NearbyDevice>,
    paired: Set<String>,
    emptyHint: String,
    rowAction: @Composable (NearbyDevice) -> Unit,
) {
    GlassCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionHeader(title, readout = if (devices.isEmpty()) null else "${devices.size}") {
                Spacer(Modifier.width(8.dp))
                ReticleSpinner(size = 16.dp, color = MaterialTheme.colorScheme.outline)
            }
            if (devices.isEmpty()) {
                Text(emptyHint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp))
            }
            devices.forEach { device ->
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                    DeviceGlyph(device.isDesktop)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(device.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when {
                                !device.compatible -> "Needs a Lock On update to sync with this device"
                                device.deviceId in paired -> "Paired"
                                else -> if (device.isDesktop) "Desktop · ${device.host}" else "Phone · ${device.host}"
                            },
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    rowAction(device)
                }
            }
        }
    }
}

/**
 * Send, LocalSend-style: choose decks, then tap a nearby device. Its owner accepts or declines, and
 * accepted decks arrive as copies, so this works with a friend's device as well as your own.
 */
@Composable
private fun SendCard(sharing: DeckSharing, decks: List<DeckSummary>, paired: Set<String>, isHost: Boolean) {
    val targets by sharing.targets.collectAsState()
    val outgoing by sharing.outgoing.collectAsState()
    var chosen by remember { mutableStateOf(emptyList<String>()) }
    var choosing by remember { mutableStateOf(false) }
    // A device tapped before any decks were chosen: once they are, they go straight to it.
    var sendTo by remember { mutableStateOf<NearbyDevice?>(null) }
    // Decks deleted since they were chosen drop out of the selection.
    val selected = chosen.mapNotNull { id -> decks.firstOrNull { it.deckId == id } }

    GlassCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Send decks", readout = if (selected.isEmpty()) null else "${selected.size}")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (selected.isEmpty()) "Choose decks, then a nearby device. It asks before saving them. No pairing needed."
                    else selected.joinToString(" · ") { it.snapshot.name },
                    style = MaterialTheme.typography.bodyMedium, maxLines = if (selected.isEmpty()) 4 else 2, overflow = TextOverflow.Ellipsis,
                    color = if (selected.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                GameButton(if (selected.isEmpty()) "Choose decks" else "Change", onClick = { choosing = true },
                    style = GameButtonStyle.SECONDARY, compact = true, enabled = decks.isNotEmpty())
            }
            if (targets.isEmpty()) {
                Text(
                    if (isHost) "Phones with Lock On open on the same Wi-Fi appear here."
                    else "Computers with Lock On open on the same Wi-Fi appear here.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            targets.forEach { device ->
                val send = outgoing[device.deviceId]
                Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                    DeviceGlyph(device.isDesktop)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(device.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when {
                                send == null -> if (device.deviceId in paired) "Paired" else if (device.isDesktop) "Desktop · ${device.host}" else "Phone · ${device.host}"
                                send.state == OutgoingShare.State.WAITING -> "Waiting for ${device.name} to accept…"
                                else -> send.message.orEmpty()
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (send?.state) {
                                OutgoingShare.State.SAVED -> MaterialTheme.colorScheme.secondary
                                OutgoingShare.State.FAILED -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    if (send?.state == OutgoingShare.State.WAITING) {
                        ReticleSpinner(size = 18.dp)
                        GameButton("Cancel", onClick = { sharing.cancelSend(device.deviceId) }, style = GameButtonStyle.GHOST, compact = true)
                    } else {
                        // Never greyed out: with nothing chosen yet, Send asks which decks first.
                        GameButton("Send", onClick = {
                            if (selected.isEmpty()) { sendTo = device; choosing = true } else sharing.send(device, selected.map { it.deckId })
                        }, enabled = decks.isNotEmpty())
                    }
                }
            }
        }
    }

    if (choosing) {
        ChooseDecksDialog(decks, chosen.toSet(), onDismiss = { choosing = false; sendTo = null }, onDone = {
            chosen = it
            choosing = false
            sendTo?.let { device -> if (it.isNotEmpty()) sharing.send(device, it) }
            sendTo = null
        })
    }
}

@Composable
private fun ChooseDecksDialog(decks: List<DeckSummary>, preselected: Set<String>, onDismiss: () -> Unit, onDone: (List<String>) -> Unit) {
    val sorted = remember(decks) { decks.sortedBy { it.snapshot.name.lowercase() } }
    var chosen by remember { mutableStateOf(preselected) }
    GameDialog(
        onDismissRequest = onDismiss,
        title = "Choose decks to send",
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    sorted.forEach { deck ->
                        val checked = deck.deckId in chosen
                        Row(
                            Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox) {
                                chosen = if (it) chosen + deck.deckId else chosen - deck.deckId
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(deck.snapshot.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    listOfNotNull("Main ${deck.snapshot.count(DeckSection.MAIN)}", deck.snapshot.folder).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Text("They arrive as new decks with their cards, notes and tags. Folders and match logs stay here.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            GameButton("Done", onClick = { onDone(sorted.map { it.deckId }.filter { it in chosen }) }, compact = true)
        },
        dismissButton = { GameButton("Cancel", onClick = onDismiss, style = GameButtonStyle.GHOST, compact = true) },
    )
}

/** Receive: decks another device is sending, saved only if accepted. */
@Composable
private fun IncomingShareDialog(sharing: DeckSharing) {
    val share by sharing.incoming.collectAsState()
    val s = share ?: return
    GameDialog(
        onDismissRequest = {},
        title = "${s.senderName} wants to send you ${if (s.decks.size == 1) "a deck" else "${s.decks.size} decks"}",
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    s.decks.forEach { deck ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(deck.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("Main ${deck.count(DeckSection.MAIN)}", style = MaterialTheme.typography.bodyMedium.merge(TabularNumbers),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text("Accepted decks are added as new decks. Nothing else on this device changes.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { GameButton("Accept", onClick = { sharing.answer(s.shareId, true) }) },
        dismissButton = { GameButton("Decline", onClick = { sharing.answer(s.shareId, false) }, style = GameButtonStyle.GHOST, compact = true) },
    )
}

/** The number both screens show while pairing, big enough to compare at a glance. */
@Composable
private fun CheckNumber(value: String) {
    Text(value, style = MaterialTheme.typography.displaySmall.merge(TabularNumbers), color = MaterialTheme.colorScheme.primary)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HostSection(controller: HostSyncController, paired: Set<String>) {
    val info by controller.hostInfo.collectAsState()
    val prompt by controller.pairingPrompt.collectAsState()
    val nearby by controller.nearby.collectAsState()
    val invited by controller.invited.collectAsState()
    // QR codes are the fallback once phones can find this desktop by themselves.
    var showQr by remember { mutableStateOf(paired.isEmpty()) }
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = Clock.System.now().toEpochMilliseconds() } }

    NearbyCard(
        title = "Nearby phones",
        devices = nearby,
        paired = paired,
        emptyHint = "Open Lock On on your phone on the same Wi-Fi and it appears here. On the phone, tap this computer to connect.",
    ) { phone ->
        when {
            phone.deviceId in paired -> Unit
            phone.deviceId in invited -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Waiting for the phone…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                GameButton("Cancel", onClick = { controller.cancelInvite(phone.deviceId) }, style = GameButtonStyle.GHOST, compact = true)
            }
            else -> GameButton("Connect", onClick = { controller.invite(phone) }, enabled = phone.compatible)
        }
    }

    GlassCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("Other ways to connect") {
                GameButton(if (showQr) "Hide" else "QR code or address", onClick = { showQr = !showQr }, style = GameButtonStyle.GHOST, compact = true)
            }
            val current = info
            when {
                current == null -> Text("Starting the sync service…")
                current.error != null -> Text(current.error, color = MaterialTheme.colorScheme.error)
                !showQr -> Text(
                    "If a phone can't see this computer (some networks block discovery), it can scan a QR code or type the address instead.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    Text("In Lock On on your phone, open Sync and scan this code.", style = MaterialTheme.typography.bodyMedium)
                    // Details wrap under the QR code in a narrow window instead of squeezing beside it.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        current.qr?.let { QrCodeView(it, Modifier.size(220.dp)) }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.widthIn(max = 360.dp)) {
                            Text("Or enter it by hand:", style = MaterialTheme.typography.labelLarge)
                            if (current.addresses.size > 1) {
                                Text("Address", style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    current.addresses.forEach { a ->
                                        PillChip(selected = a == current.addresses.first(), onClick = { controller.selectAddress(a) }, label = { Text(a) })
                                    }
                                }
                            } else {
                                Field("Address", current.addresses.firstOrNull() ?: "No network found")
                            }
                            Field("Port", "${current.port}")
                            Field("Pairing code", current.pairingCode)
                            val seconds = ((current.codeExpiresAt - now) / 1000).coerceAtLeast(0)
                            Text("Code changes in ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            GameButton("New code", onClick = controller::newPairingCode, style = GameButtonStyle.SECONDARY)
                        }
                    }
                }
            }
            Text(
                "If a phone can't connect, allow Lock On through the Windows firewall for private networks.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    prompt?.let { p ->
        GameDialog(
            onDismissRequest = {},
            title = if (p.invited) "${p.deviceName} accepted" else "${p.deviceName} wants to connect",
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pair only if ${p.deviceName} shows this same number:")
                    CheckNumber(p.verificationCode)
                    Text("Paired devices sync decks automatically while Lock On is open on both.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { GameButton("Pair", onClick = { controller.answerPairing(p.requestId, true) }) },
            dismissButton = { GameButton("Decline", onClick = { controller.answerPairing(p.requestId, false) }, style = GameButtonStyle.GHOST, compact = true) },
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
private fun ClientSection(controller: ClientSyncController, paired: Set<String>) {
    val autoSync by controller.autoSync.collectAsState()
    val nearby by controller.nearby.collectAsState()
    val invite by controller.incomingInvite.collectAsState()
    val verification by controller.pendingVerification.collectAsState()
    val status by controller.status.collectAsState()
    var other by remember { mutableStateOf(false) }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf(SyncProtocol.DEFAULT_PORT.toString()) }
    var code by remember { mutableStateOf("") }

    // One tap on a nearby computer connects; the computer asks its owner to confirm (UX rule C4).
    NearbyCard(
        title = "Nearby computers",
        devices = nearby,
        paired = paired,
        emptyHint = "Open Lock On on your computer on the same Wi-Fi and it appears here.",
    ) { desktop ->
        GameButton("Connect", onClick = { controller.pair(PairingInvite(desktop.host, desktop.port, "", desktop.fingerprint, desktop.name)) }, enabled = desktop.compatible && !status.working)
    }

    if (paired.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Auto-sync while Lock On is open", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Edits sync within seconds when your computer is open on the same network. Nothing runs in the background.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PillSwitch(checked = autoSync, onCheckedChange = controller::setAutoSync)
        }
    }

    GlassCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("Other ways to connect") {
                GameButton(if (other) "Hide" else "Show", onClick = { other = !other }, style = GameButtonStyle.GHOST, compact = true)
            }
            if (other) {
                Text("If your computer doesn't appear above, scan the QR code under Sync on the computer, or type its address.",
                    style = MaterialTheme.typography.bodyMedium)
                if (controller.canScanQr) GameButton("Scan QR code", onClick = controller::scanQr, enabled = !status.working)
                OutlinedTextField(host, { host = it.trim() }, label = { Text("Computer address") }, singleLine = true,
                    placeholder = { Text("192.168.1.20") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(port, { port = it.filter(Char::isDigit) }, label = { Text("Port") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(code, { code = it.trim().uppercase() }, label = { Text("Pairing code (optional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                GameButton("Connect", onClick = { controller.pair(PairingInvite(host, port.toIntOrNull() ?: SyncProtocol.DEFAULT_PORT, code, fingerprint = null)) }, style = GameButtonStyle.SECONDARY, enabled = host.isNotBlank() && !status.working)
            }
        }
    }

    invite?.let { i ->
        GameDialog(
            onDismissRequest = { controller.answerInvite(false) },
            title = "${i.offer.hostName} wants to connect",
            text = { Text("Connect to sync your decks with this computer? You'll confirm a number on both screens next.") },
            confirmButton = { GameButton("Connect", onClick = { controller.answerInvite(true) }) },
            dismissButton = { GameButton("Not now", onClick = { controller.answerInvite(false) }, style = GameButtonStyle.GHOST, compact = true) },
        )
    }

    verification?.let { v ->
        GameDialog(
            onDismissRequest = {},
            title = "Confirm on your computer",
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pair on the computer only if it shows this same number:")
                    CheckNumber(v)
                }
            },
            confirmButton = {},
        )
    }
}

/** @param live auto-sync label and whether it is healthy, or null when auto-sync isn't running for this device. */
@Composable
private fun DeviceCard(device: PairedDevice, working: Boolean, live: Pair<String, Boolean>?, onSync: (() -> Unit)?, onForget: () -> Unit) {
    GlassCard {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
                live?.let { (label, healthy) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier.size(8.dp).clip(CircleShape)
                                .background(if (healthy) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline),
                        )
                        Text(label, style = MaterialTheme.typography.bodySmall,
                            color = if (healthy) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (device.host != null) Text("${device.host}:${device.port}", style = MaterialTheme.typography.bodySmall)
                Text(
                    device.lastSyncAt?.let { "Last successful sync ${formatTimestamp(it)}" } ?: "Not synced yet",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                device.lastError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (onSync != null) GameButton("Sync now", onClick = onSync, enabled = !working)
                GameButton("Forget", onClick = onForget, style = GameButtonStyle.GHOST, compact = true)
            }
        }
    }
}

/** A drawn monitor or phone in a circle, so devices read at a glance without an icon pack. */
@Composable
private fun DeviceGlyph(desktop: Boolean) {
    val ink = MaterialTheme.colorScheme.onSecondaryContainer
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = size.minDimension * 0.09f)
            val r = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.08f)
            if (desktop) {
                drawRoundRect(ink, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.08f),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.62f), cornerRadius = r, style = stroke)
                drawLine(ink, androidx.compose.ui.geometry.Offset(size.width / 2, size.height * 0.7f),
                    androidx.compose.ui.geometry.Offset(size.width / 2, size.height * 0.88f), stroke.width)
                drawLine(ink, androidx.compose.ui.geometry.Offset(size.width * 0.28f, size.height * 0.9f),
                    androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height * 0.9f), stroke.width)
            } else {
                drawRoundRect(ink, topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.24f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.52f, size.height), cornerRadius = r * 1.6f, style = stroke)
                drawCircle(ink, radius = stroke.width * 0.7f, center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height * 0.84f))
            }
        }
    }
}
