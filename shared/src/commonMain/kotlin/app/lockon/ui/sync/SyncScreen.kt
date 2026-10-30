package app.lockon.ui.sync

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lockon.model.DeckSection
import app.lockon.model.DeckSummary
import app.lockon.platform.formatTimestamp
import app.lockon.sync.AutoSyncState
import app.lockon.sync.ClientSyncController
import app.lockon.sync.DeckSharing
import app.lockon.sync.HostSyncController
import app.lockon.sync.NearbyDevice
import app.lockon.sync.OutgoingShare
import app.lockon.sync.PairedDevice
import app.lockon.sync.PairingInvite
import app.lockon.sync.ShareInbox
import app.lockon.sync.SyncController
import app.lockon.sync.SyncProtocol
import app.lockon.sync.SyncStatus
import app.lockon.ui.components.CautionStripes
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.GameDialog
import app.lockon.ui.components.GlassCard
import app.lockon.ui.components.GlassIconButton
import app.lockon.ui.components.LocalNavClearance
import app.lockon.ui.components.LocalReduceMotion
import app.lockon.ui.components.PillChip
import app.lockon.ui.components.PillSwitch
import app.lockon.ui.components.PillSearchField
import app.lockon.ui.components.PillTabs
import app.lockon.ui.components.QrCodeView
import app.lockon.ui.components.ReticleSpinner
import app.lockon.ui.components.SectionHeader
import app.lockon.ui.components.VerticalScrollBar
import app.lockon.ui.components.glass
import app.lockon.ui.components.glassFrame
import app.lockon.ui.components.glide
import app.lockon.ui.components.isTwilight
import app.lockon.ui.components.pressScale
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.theme.TabularNumbers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock

/**
 * Sync, LocalSend-style: Receive shows this device's name, ready for nearby devices to send to it;
 * Send is choose decks, then tap a nearby device. Pairing for automatic sync sits behind the heart on
 * each device, so the everyday path has nothing else on it.
 */
@Composable
fun SyncScreen(
    controller: SyncController,
    deviceName: String,
    /** Decks that can be sent to a nearby device. */
    decks: List<DeckSummary> = emptyList(),
    /** Which page shows: Send, or Receive. Kept by the caller so it survives leaving the tab. */
    sending: Boolean = true,
    onSendingChange: (Boolean) -> Unit = {},
) {
    // The chosen decks stay chosen while flipping to Receive and back.
    var chosen by remember { mutableStateOf(emptyList<String>()) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PillTabs(listOf("Receive", "Send"), if (sending) 1 else 0, { onSendingChange(it == 1) }, Modifier.width(300.dp), fill = true)
        }
        Crossfade(sending, Modifier.fillMaxSize(), label = "sync-page") { send ->
            if (send) SendPage(controller, decks, chosen) { chosen = it }
            else ReceivePage(deviceName, isHost = controller is HostSyncController)
        }
    }
}

/** A page's scrolling column, centred and no wider than a phone so it reads the same everywhere. */
@Composable
private fun SyncPage(centered: Boolean = false, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()
        val clearance = LocalNavClearance.current
        val height = maxHeight
        Box(Modifier.fillMaxSize().verticalScroll(scrollState), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 620.dp).fillMaxWidth()
                    .heightIn(min = if (centered) height - clearance else 0.dp)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp + clearance),
                verticalArrangement = if (centered) Arrangement.Center else Arrangement.spacedBy(14.dp),
                horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
            ) { content() }
        }
        VerticalScrollBar(scrollState, Modifier.align(Alignment.TopEnd))
    }
}

/** Receive: nothing to do but be found. Offers arrive wherever you are in the app. */
@Composable
private fun ReceivePage(deviceName: String, isHost: Boolean) {
    SyncPage(centered = true) {
        Beacon(Modifier.size(190.dp))
        Spacer(Modifier.height(32.dp))
        Text(deviceName, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Ready to receive. ${if (isHost) "Phones" else "Devices"} with Lock On open on this Wi-Fi can send you decks; you'll be asked first.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 380.dp),
        )
    }
}

/** A dot in a ring of dashes that turns slowly: this device is listening. Still with reduced motion. */
@Composable
private fun Beacon(modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val color = MaterialTheme.colorScheme.primary
    // Read only while drawing, so turning redraws the ring without recomposing it.
    val angle: State<Float>? = if (reduce) null else {
        val t = rememberInfiniteTransition(label = "beacon")
        t.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart), label = "beacon-angle")
    }
    Canvas(modifier) {
        val r = size.minDimension / 2
        drawCircle(color, radius = r * 0.54f)
        val stroke = r * 0.16f
        val ring = r - stroke / 2
        rotate(angle?.value ?: 0f) {
            repeat(8) { i ->
                drawArc(
                    color, startAngle = i * 45f + 9f, sweepAngle = 27f, useCenter = false,
                    topLeft = Offset(center.x - ring, center.y - ring), size = Size(ring * 2, ring * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
    }
}

/** Send: what to send, then where. Tapping a device sends; a device tapped first asks which decks. */
@Composable
private fun SendPage(controller: SyncController, decks: List<DeckSummary>, chosen: List<String>, onChosen: (List<String>) -> Unit) {
    val status by controller.status.collectAsState()
    val paired by controller.pairedDevices.collectAsState(initial = emptyList())
    val pairedIds = paired.map { it.deviceId }.toSet()
    val sharing = controller.sharing
    val nearby by when (controller) {
        is HostSyncController -> controller.nearby
        is ClientSyncController -> controller.nearby
    }.collectAsState()
    val targets by (sharing?.targets ?: remember { MutableStateFlow(emptyList()) }).collectAsState()
    val outgoing by (sharing?.outgoing ?: remember { MutableStateFlow(emptyMap()) }).collectAsState()
    val invited by ((controller as? HostSyncController)?.invited ?: remember { MutableStateFlow(emptySet()) }).collectAsState()
    // Every device in reach once, whether it came from sending's list or pairing's.
    val devices = (targets + nearby).distinctBy { it.deviceId }

    var choosing by remember { mutableStateOf(false) }
    var pickingFolder by remember { mutableStateOf(false) }
    // A device tapped before any decks were chosen: once they are, they go straight to it.
    var sendTo by remember { mutableStateOf<NearbyDevice?>(null) }
    var showPaired by remember { mutableStateOf(false) }
    var showOther by remember { mutableStateOf(false) }
    var troubleshoot by remember { mutableStateOf(false) }
    // Decks deleted since they were chosen drop out of the selection.
    val selected = chosen.mapNotNull { id -> decks.firstOrNull { it.deckId == id } }
    val folders = remember(decks) { decks.mapNotNull { it.snapshot.folder }.distinct().sorted() }
    val isHost = controller is HostSyncController

    fun pair(device: NearbyDevice) = when (controller) {
        is HostSyncController -> controller.invite(device)
        is ClientSyncController -> controller.pair(PairingInvite(device.host, device.port, "", device.fingerprint, device.name))
    }

    SyncPage {
        StatusLine(status)
        if (sharing != null) {
            SectionHeader("Selection", readout = if (selected.isEmpty()) null else "${selected.size}")
            if (selected.isEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val tile = Modifier.weight(1f)
                    PickTile("Decks", Icons.Default.Add, tile, enabled = decks.isNotEmpty()) { choosing = true }
                    if (folders.isNotEmpty()) PickTile("Folder", Icons.Default.List, tile) { pickingFolder = true }
                    if (decks.size in 1..ShareInbox.MAX_DECKS) PickTile("All decks", Icons.Default.Done, tile) { onChosen(decks.map { it.deckId }) }
                }
                if (decks.isEmpty()) Text("Build a deck first, then send it from here.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                SelectionCard(selected, onEdit = { choosing = true }, onClear = { onChosen(emptyList()) })
            }
        }

        SectionHeader("Nearby devices") {
            GlassIconButton(Icons.Default.FavoriteBorder, "Paired devices", onClick = { showPaired = true })
            Spacer(Modifier.width(8.dp))
            GlassIconButton(Icons.Default.Add, "Connect by address or QR code", onClick = { showOther = true })
        }
        if (devices.isEmpty()) SearchingCard()
        devices.forEach { device ->
            val canSend = sharing != null && targets.any { it.deviceId == device.deviceId }
            DeviceTile(
                device = device,
                paired = device.deviceId in pairedIds,
                send = outgoing[device.deviceId],
                inviting = device.deviceId in invited,
                onTap = {
                    when {
                        !device.compatible -> Unit
                        !canSend -> if (device.deviceId !in pairedIds) pair(device)
                        selected.isEmpty() -> { sendTo = device; choosing = true }
                        else -> sharing?.send(device, selected.map { it.deckId })
                    }
                },
                onHeart = { if (device.deviceId in pairedIds) showPaired = true else pair(device) },
                onCancel = {
                    if (device.deviceId in invited) (controller as? HostSyncController)?.cancelInvite(device.deviceId)
                    else sharing?.cancelSend(device.deviceId)
                },
            )
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GameButton("Troubleshoot", onClick = { troubleshoot = true }, style = GameButtonStyle.GHOST, compact = true)
        }
        Text(
            "Make sure the other device is on the same Wi-Fi with Lock On open.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }

    if (choosing) {
        ChooseDecksDialog(decks, chosen.toSet(), sendingTo = sendTo?.name, onDismiss = { choosing = false; sendTo = null }, onDone = {
            onChosen(it)
            choosing = false
            sendTo?.let { device -> if (it.isNotEmpty()) sharing?.send(device, it) }
            sendTo = null
        })
    }
    if (pickingFolder) {
        ChooseFolderDialog(folders, decks, onDismiss = { pickingFolder = false }) { folder ->
            onChosen(decks.filter { it.snapshot.folder == folder }.map { it.deckId }.take(ShareInbox.MAX_DECKS))
            pickingFolder = false
        }
    }
    if (showPaired) PairedDevicesDialog(controller, paired) { showPaired = false }
    if (showOther) OtherWaysDialog(controller) { showOther = false }
    if (troubleshoot) {
        GameDialog(
            onDismissRequest = { troubleshoot = false },
            title = "Can't see a device?",
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Both devices need Lock On open, on the same Wi-Fi network.")
                    Text("• Guest and office networks often hide devices from each other.")
                    if (isHost) Text("• Allow Lock On through the Windows firewall for private networks.")
                    else Text("• On the computer, allow Lock On through the Windows firewall for private networks.")
                    Text("Still nothing? Connect with a QR code or the computer's address instead.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { GameButton("QR code or address", onClick = { troubleshoot = false; showOther = true }, compact = true) },
            dismissButton = { GameButton("Close", onClick = { troubleshoot = false }, style = GameButtonStyle.GHOST, compact = true) },
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
            color = if (status.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** One way to choose what to send, as a big glass tile. */
@Composable
private fun PickTile(label: String, icon: ImageVector, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val ink = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Column(
        modifier.heightIn(min = 84.dp).pressScale(interaction).glass(LockOnShapes.tile)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else ink)
    }
}

/** The chosen decks, with Edit and Clear. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectionCard(selected: List<DeckSummary>, onEdit: () -> Unit, onClear: () -> Unit) {
    GlassCard {
        Column(Modifier.animateContentSize(glide()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (selected.size == 1) "1 deck" else "${selected.size} decks", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                selected.take(12).forEach { deck ->
                    Text(
                        deck.snapshot.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 220.dp).clip(LockOnShapes.button)
                            .background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 10.dp, vertical = 5.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                if (selected.size > 12) Text("+${selected.size - 12} more", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 5.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameButton("Edit", onClick = onEdit, style = GameButtonStyle.SECONDARY, compact = true)
                GameButton("Clear", onClick = onClear, style = GameButtonStyle.GHOST, compact = true)
            }
            Text("Tap a device below to send.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** While nothing is found: the shape of a device, faded, so it's clear what will appear. */
@Composable
private fun SearchingCard() {
    GlassCard {
        Row(Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                ReticleSpinner(size = 22.dp, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Looking for devices…", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(40.dp, 64.dp).forEach {
                        Box(Modifier.width(it).height(14.dp).clip(LockOnShapes.button).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)))
                    }
                }
            }
        }
    }
}

/** A nearby device. Tap it to send; the heart pairs it for automatic sync. */
@Composable
private fun DeviceTile(
    device: NearbyDevice,
    paired: Boolean,
    send: OutgoingShare?,
    inviting: Boolean,
    onTap: () -> Unit,
    onHeart: () -> Unit,
    onCancel: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val waiting = send?.state == OutgoingShare.State.WAITING || inviting
    Row(
        Modifier.fillMaxWidth().heightIn(min = 76.dp).pressScale(interaction).glass(LockOnShapes.tile)
            .clickable(interaction, indication = null, enabled = device.compatible && !waiting, role = Role.Button, onClick = onTap)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeviceGlyph(device.isDesktop)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(device.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            when {
                !device.compatible -> Text("Needs a Lock On update", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                inviting -> Text("Waiting for ${device.name} to accept pairing…", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                send != null -> Text(
                    if (send.state == OutgoingShare.State.WAITING) "Waiting for ${device.name} to accept…" else send.message.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = when (send.state) {
                        OutgoingShare.State.SAVED -> MaterialTheme.colorScheme.secondary
                        OutgoingShare.State.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                else -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(if (device.isDesktop) "Computer" else "Phone")
                    if (paired) Tag("Paired")
                }
            }
        }
        if (waiting) {
            ReticleSpinner(size = 18.dp)
            GameButton("Cancel", onClick = onCancel, style = GameButtonStyle.GHOST, compact = true)
        } else {
            GlassIconButton(
                if (paired) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                if (paired) "Paired: syncs automatically" else "Pair to sync automatically",
                onClick = onHeart,
                tint = if (paired) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Tag(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clip(LockOnShapes.thumb).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun ChooseDecksDialog(
    decks: List<DeckSummary>, preselected: Set<String>, sendingTo: String?, onDismiss: () -> Unit, onDone: (List<String>) -> Unit,
) {
    val sorted = remember(decks) { decks.sortedBy { it.snapshot.name.lowercase() } }
    var chosen by remember { mutableStateOf(preselected) }
    var search by remember { mutableStateOf("") }
    var listHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    // Searching only hides decks: ones already ticked stay chosen while out of sight.
    val shown = sorted.filter { deck ->
        val q = search.trim()
        deck.snapshot.name.contains(q, ignoreCase = true) || deck.snapshot.folder?.contains(q, ignoreCase = true) == true
    }
    GameDialog(
        onDismissRequest = onDismiss,
        title = if (sendingTo != null) "Send to $sendingTo" else "Choose decks",
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PillSearchField(
                    search, { search = it }, placeholder = "Search decks or folders", modifier = Modifier.fillMaxWidth(),
                    onClear = if (search.isEmpty()) null else ({ search = "" }),
                )
                // The list keeps the height it opened at, so the dialog doesn't re-centre and move the field while typing.
                Column(
                    Modifier.heightIn(min = listHeight, max = 360.dp)
                        .onSizeChanged { with(density) { if (search.isEmpty()) listHeight = it.height.toDp() } }
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (shown.isEmpty()) Text("No decks match.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
                    shown.forEach { deck ->
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
            val ids = sorted.map { it.deckId }.filter { it in chosen }
            GameButton(
                when {
                    sendingTo == null -> "Done"
                    ids.size == 1 -> "Send 1 deck"
                    else -> "Send ${ids.size} decks"
                },
                onClick = { onDone(ids) }, compact = true, enabled = sendingTo == null || ids.size in 1..ShareInbox.MAX_DECKS,
            )
        },
        dismissButton = { GameButton("Cancel", onClick = onDismiss, style = GameButtonStyle.GHOST, compact = true) },
    )
}

@Composable
private fun ChooseFolderDialog(folders: List<String>, decks: List<DeckSummary>, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    GameDialog(
        onDismissRequest = onDismiss,
        title = "Send a folder",
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                folders.forEach { folder ->
                    val count = decks.count { it.snapshot.folder == folder }
                    Row(
                        Modifier.fillMaxWidth().clip(LockOnShapes.thumb).clickable { onPick(folder) }.padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(folder, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (count == 1) "1 deck" else "$count decks", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { GameButton("Cancel", onClick = onDismiss, style = GameButtonStyle.GHOST, compact = true) },
    )
}

/** Paired devices keep every deck in sync on their own; this is where they're checked and forgotten. */
@Composable
private fun PairedDevicesDialog(controller: SyncController, devices: List<PairedDevice>, onDismiss: () -> Unit) {
    val status by controller.status.collectAsState()
    val liveHosts by ((controller as? ClientSyncController)?.autoStates ?: remember { MutableStateFlow(emptyMap()) }).collectAsState()
    val liveClients by ((controller as? HostSyncController)?.liveClients ?: remember { MutableStateFlow(emptySet()) }).collectAsState()
    val autoSync by ((controller as? ClientSyncController)?.autoSync ?: remember { MutableStateFlow(true) }).collectAsState()
    var forgetting by remember { mutableStateOf<PairedDevice?>(null) }
    GameDialog(
        onDismissRequest = onDismiss,
        title = "Paired devices",
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Paired devices keep all your decks in sync automatically while Lock On is open on both. Sending works without pairing.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (devices.isEmpty()) {
                    Text("None yet. Tap the heart on a nearby device to pair it.", style = MaterialTheme.typography.bodyMedium)
                }
                if (controller is ClientSyncController && devices.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto-sync while Lock On is open", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        PillSwitch(checked = autoSync, onCheckedChange = controller::setAutoSync)
                    }
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
            }
        },
        confirmButton = { GameButton("Done", onClick = onDismiss, compact = true) },
    )
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

/** @param live auto-sync label and whether it is healthy, or null when auto-sync isn't running for this device. */
@Composable
private fun DeviceCard(device: PairedDevice, working: Boolean, live: Pair<String, Boolean>?, onSync: (() -> Unit)?, onForget: () -> Unit) {
    GlassCard {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                Text(
                    device.lastSyncAt?.let { "Last synced ${formatTimestamp(it)}" } ?: "Not synced yet",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                device.lastError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (onSync != null) GameButton("Sync now", onClick = onSync, enabled = !working, compact = true)
                GameButton("Forget", onClick = onForget, style = GameButtonStyle.GHOST, compact = true)
            }
        }
    }
}

/** For networks that hide devices: the computer shows a QR code and address, the phone scans or types it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OtherWaysDialog(controller: SyncController, onDismiss: () -> Unit) {
    GameDialog(
        onDismissRequest = onDismiss,
        title = "Connect by QR code or address",
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (controller) {
                    is HostSyncController -> HostAddress(controller)
                    is ClientSyncController -> ClientAddress(controller, onDismiss)
                }
            }
        },
        confirmButton = { GameButton("Close", onClick = onDismiss, style = GameButtonStyle.GHOST, compact = true) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HostAddress(controller: HostSyncController) {
    val info by controller.hostInfo.collectAsState()
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = Clock.System.now().toEpochMilliseconds() } }
    val current = info
    when {
        current == null -> Text("Starting the sync service…")
        current.error != null -> Text(current.error, color = MaterialTheme.colorScheme.error)
        else -> {
            Text("In Lock On on your phone, open Sync, tap + and scan this code.", style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                current.qr?.let { QrCodeView(it, Modifier.size(200.dp)) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.widthIn(max = 320.dp)) {
                    Text("Or enter it by hand:", style = MaterialTheme.typography.labelLarge)
                    if (current.addresses.size > 1) {
                        Text("Address", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    GameButton("New code", onClick = controller::newPairingCode, style = GameButtonStyle.SECONDARY, compact = true)
                }
            }
        }
    }
    Text("If a phone can't connect, allow Lock On through the Windows firewall for private networks.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ClientAddress(controller: ClientSyncController, onDone: () -> Unit) {
    val status by controller.status.collectAsState()
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf(SyncProtocol.DEFAULT_PORT.toString()) }
    var code by remember { mutableStateOf("") }
    Text("On the computer, open Sync and tap + to show its QR code and address.", style = MaterialTheme.typography.bodyMedium)
    if (controller.canScanQr) GameButton("Scan QR code", onClick = { controller.scanQr(); onDone() }, enabled = !status.working)
    OutlinedTextField(host, { host = it.trim() }, label = { Text("Computer address") }, singleLine = true,
        placeholder = { Text("192.168.1.20") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(port, { port = it.filter(Char::isDigit) }, label = { Text("Port") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(code, { code = it.trim().uppercase() }, label = { Text("Pairing code (optional)") }, singleLine = true,
        modifier = Modifier.fillMaxWidth())
    GameButton("Connect", onClick = {
        controller.pair(PairingInvite(host, port.toIntOrNull() ?: SyncProtocol.DEFAULT_PORT, code, fingerprint = null))
        onDone()
    }, style = GameButtonStyle.SECONDARY, enabled = host.isNotBlank() && !status.working)
}

@Composable
private fun Field(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
}

/**
 * Questions from other devices: decks someone wants to send, and pairing requests. Shown over
 * whatever screen is open, so a send is never left waiting because Sync wasn't the open tab.
 */
@Composable
fun SyncPrompts(controller: SyncController) {
    controller.sharing?.let { IncomingShareDialog(it, fromDesktop = controller !is HostSyncController) }
    when (controller) {
        is HostSyncController -> {
            val prompt by controller.pairingPrompt.collectAsState()
            prompt?.let { p ->
                GameDialog(
                    onDismissRequest = {},
                    title = if (p.invited) "${p.deviceName} accepted" else "${p.deviceName} wants to pair",
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
        is ClientSyncController -> {
            val invite by controller.incomingInvite.collectAsState()
            val verification by controller.pendingVerification.collectAsState()
            invite?.let { i ->
                GameDialog(
                    onDismissRequest = { controller.answerInvite(false) },
                    title = "${i.offer.hostName} wants to pair",
                    text = { Text("Pair to keep your decks in sync with this computer automatically? You'll confirm a number on both screens next.") },
                    confirmButton = { GameButton("Pair", onClick = { controller.answerInvite(true) }) },
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
    }
}

/** Receive: decks another device is sending, LocalSend-style, saved only if accepted. */
@Composable
private fun IncomingShareDialog(sharing: DeckSharing, fromDesktop: Boolean) {
    val share by sharing.incoming.collectAsState()
    val s = share ?: return
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        val scheme = MaterialTheme.colorScheme
        Column(
            Modifier.padding(20.dp).widthIn(max = 460.dp).fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(if (isTwilight()) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest)
                .glassFrame(MaterialTheme.shapes.extraLarge)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DeviceGlyph(fromDesktop, size = 72)
            Text(s.senderName, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("wants to send you ${if (s.decks.size == 1) "a deck" else "${s.decks.size} decks"}", style = MaterialTheme.typography.bodyLarge)
            Column(
                Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())
                    .clip(LockOnShapes.tile).background(scheme.onSurface.copy(alpha = 0.05f)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                s.decks.forEach { deck ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(deck.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("Main ${deck.count(DeckSection.MAIN)}", style = MaterialTheme.typography.bodyMedium.merge(TabularNumbers),
                            color = scheme.onSurfaceVariant)
                    }
                }
            }
            Text("They're added as new decks. Nothing else changes.", style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                GameButton("Decline", onClick = { sharing.answer(s.shareId, false) }, style = GameButtonStyle.SECONDARY)
                GameButton("Accept", onClick = { sharing.answer(s.shareId, true) }, icon = Icons.Default.Check)
            }
        }
    }
}

/** The number both screens show while pairing, big enough to compare at a glance. */
@Composable
private fun CheckNumber(value: String) {
    Text(value, style = MaterialTheme.typography.displaySmall.merge(TabularNumbers), color = MaterialTheme.colorScheme.primary)
}

/** A drawn monitor or phone in a circle, so devices read at a glance without an icon pack. */
@Composable
private fun DeviceGlyph(desktop: Boolean, size: Int = 44) {
    val ink = MaterialTheme.colorScheme.onSecondaryContainer
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size((size * 0.55f).dp)) {
            val stroke = Stroke(width = this.size.minDimension * 0.09f)
            val r = CornerRadius(this.size.minDimension * 0.08f)
            if (desktop) {
                drawRoundRect(ink, topLeft = Offset(0f, this.size.height * 0.08f),
                    size = Size(this.size.width, this.size.height * 0.62f), cornerRadius = r, style = stroke)
                drawLine(ink, Offset(this.size.width / 2, this.size.height * 0.7f), Offset(this.size.width / 2, this.size.height * 0.88f), stroke.width)
                drawLine(ink, Offset(this.size.width * 0.28f, this.size.height * 0.9f), Offset(this.size.width * 0.72f, this.size.height * 0.9f), stroke.width)
            } else {
                drawRoundRect(ink, topLeft = Offset(this.size.width * 0.24f, 0f),
                    size = Size(this.size.width * 0.52f, this.size.height), cornerRadius = r * 1.6f, style = stroke)
                drawCircle(ink, radius = stroke.width * 0.7f, center = Offset(this.size.width / 2, this.size.height * 0.84f))
            }
        }
    }
}
