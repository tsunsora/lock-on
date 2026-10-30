package app.lockon.ui.sync

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import app.lockon.sync.IncomingShare
import app.lockon.ui.components.Accent
import app.lockon.ui.components.ControlSize
import app.lockon.ui.components.Gold
import app.lockon.ui.components.Motion
import app.lockon.ui.components.lighten
import app.lockon.ui.components.motion
import kotlin.math.roundToInt
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

/** A page's scrolling column, centred and no wider than [maxWidth] so it reads the same everywhere. */
@Composable
private fun SyncPage(centered: Boolean = false, maxWidth: Dp = 620.dp, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()
        val clearance = LocalNavClearance.current
        val height = this.maxHeight
        Box(Modifier.fillMaxSize().verticalScroll(scrollState), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = maxWidth).fillMaxWidth()
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

/**
 * Send: what to send, then where. Tapping a device sends; a device tapped first asks which decks.
 * Wide windows put the two side by side, so the choice and the devices are both in view.
 */
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
    // A device tapped before any decks were chosen: once they are, they go straight to it.
    var sendTo by remember { mutableStateOf<NearbyDevice?>(null) }
    var showPaired by remember { mutableStateOf(false) }
    var showOther by remember { mutableStateOf(false) }
    var troubleshoot by remember { mutableStateOf(false) }
    // Decks deleted since they were chosen drop out of the selection.
    val selected = chosen.mapNotNull { id -> decks.firstOrNull { it.deckId == id } }
    val isHost = controller is HostSyncController

    fun pair(device: NearbyDevice) = when (controller) {
        is HostSyncController -> controller.invite(device)
        is ClientSyncController -> controller.pair(PairingInvite(device.host, device.port, "", device.fingerprint, device.name))
    }

    val where: @Composable () -> Unit = {
        SectionHeader("Send to", Modifier.heightIn(min = ControlSize)) {
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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sideBySide = sharing != null && maxWidth >= 840.dp
        SyncPage(maxWidth = if (sideBySide) 980.dp else 620.dp) {
            StatusLine(status)
            val what: @Composable () -> Unit = {
                WhatToSend(decks, selected, onChoose = { choosing = true }, onChosen = onChosen)
            }
            when {
                sideBySide -> Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) { what() }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) { where() }
                }
                sharing != null -> { what(); where() }
                else -> where()
            }
        }
    }

    if (choosing) {
        ChooseDecksDialog(decks, chosen.toSet(), sendingTo = sendTo?.name, onDismiss = { choosing = false; sendTo = null }, onDone = {
            onChosen(it)
            choosing = false
            sendTo?.let { device -> if (it.isNotEmpty()) sharing?.send(device, it) }
            sendTo = null
        })
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

private fun deckCount(n: Int) = if (n == 1) "1 deck" else "$n decks"

/**
 * What to send: pick decks, take them all, or tap a folder to add or drop its decks in one go.
 * Folder chips stay below the selection, so a second folder is one more tap.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WhatToSend(decks: List<DeckSummary>, selected: List<DeckSummary>, onChoose: () -> Unit, onChosen: (List<String>) -> Unit) {
    val ids = selected.map { it.deckId }
    val folders = remember(decks) {
        decks.groupBy { it.snapshot.folder }.mapNotNull { (folder, inIt) -> folder?.let { it to inIt.map { d -> d.deckId } } }
            .sortedBy { it.first.lowercase() }
    }
    // As tall as Send to's header, so the two line up side by side.
    SectionHeader("What to send", Modifier.heightIn(min = ControlSize), readout = if (selected.isEmpty()) null else "${selected.size}") {
        if (selected.isNotEmpty()) GameButton("Clear", onClick = { onChosen(emptyList()) }, style = GameButtonStyle.GHOST, compact = true)
    }
    if (selected.isEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val tile = Modifier.weight(1f)
            PickTile("Choose decks", Icons.Default.Add, tile, enabled = decks.isNotEmpty(), onClick = onChoose)
            if (decks.size in 1..ShareInbox.MAX_DECKS) PickTile("All ${decks.size} decks", Icons.Default.Done, tile) { onChosen(decks.map { it.deckId }) }
        }
        if (decks.isEmpty()) Text("Build a deck first, then send it from here.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        SelectionCard(selected, onEdit = onChoose)
    }
    if (folders.isNotEmpty()) {
        Text("Folders", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            folders.forEach { (folder, inFolder) ->
                val whole = inFolder.all { it in ids }
                PillChip(
                    selected = whole,
                    onClick = { onChosen(if (whole) ids - inFolder.toSet() else (ids + inFolder).distinct().take(ShareInbox.MAX_DECKS)) },
                    label = { Text("$folder · ${inFolder.size}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = if (whole) ({ Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }) else null,
                    modifier = Modifier.widthIn(max = 260.dp),
                )
            }
        }
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

/** The chosen decks grouped by folder, so a selection built from folders reads back the same way. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectionCard(selected: List<DeckSummary>, onEdit: () -> Unit) {
    val groups = selected.groupBy { it.snapshot.folder }.entries
        .sortedWith(compareBy({ it.key == null }, { it.key?.lowercase() }))
    GlassCard {
        Column(Modifier.animateContentSize(glide()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(deckCount(selected.size), style = MaterialTheme.typography.titleMedium)
                    Text("Tap a device to send ${if (selected.size == 1) "it" else "them"}.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                GameButton("Edit", onClick = onEdit, style = GameButtonStyle.SECONDARY, compact = true)
            }
            groups.forEach { (folder, inGroup) ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // A selection with no folders at all needs no "No folder" heading.
                    if (folder != null || groups.size > 1) {
                        Text(
                            "${folder ?: "No folder"} · ${inGroup.size}", style = MaterialTheme.typography.labelMedium.merge(TabularNumbers),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        inGroup.take(8).forEach { deck ->
                            Text(
                                deck.snapshot.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 220.dp).clip(LockOnShapes.button)
                                    .background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 10.dp, vertical = 5.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        if (inGroup.size > 8) Text("+${inGroup.size - 8} more", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 5.dp))
                    }
                }
            }
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

/** What a send's line under the device name says. */
private fun OutgoingShare.label(): String = when (state) {
    OutgoingShare.State.SENDING -> progress?.let { "Sending ${deckCount(deckCount)}… ${(it * 100).roundToInt()}%" }
        ?: "Sending ${deckCount(deckCount)}…"
    OutgoingShare.State.WAITING -> "Sent. Waiting for $deviceName to accept…"
    OutgoingShare.State.SAVED -> "Done: ${deckCount(deckCount)} saved there"
    else -> message.orEmpty()
}

/** A nearby device. Tap it to send; the heart pairs it for automatic sync. A send shows its progress under it. */
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
    val scheme = MaterialTheme.colorScheme
    val busy = send?.active == true || inviting
    val celebrate = if (isTwilight()) Gold else scheme.tertiary
    Column(
        Modifier.fillMaxWidth().pressScale(interaction).glass(LockOnShapes.tile)
            .clickable(interaction, indication = null, enabled = device.compatible && !busy, role = Role.Button, onClick = onTap)
            .animateContentSize(glide())
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                DeviceGlyph(device.isDesktop)
                if (send?.state == OutgoingShare.State.SAVED) DoneBadge(Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                when {
                    !device.compatible -> Text("Needs a Lock On update", style = MaterialTheme.typography.bodySmall, color = scheme.error)
                    inviting -> Text("Waiting for ${device.name} to accept pairing…", style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant)
                    send != null -> Text(
                        send.label(),
                        style = MaterialTheme.typography.bodySmall.merge(TabularNumbers),
                        color = when (send.state) {
                            OutgoingShare.State.SAVED -> celebrate
                            OutgoingShare.State.FAILED -> scheme.error
                            else -> scheme.onSurfaceVariant
                        },
                    )
                    else -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Tag(if (device.isDesktop) "Computer" else "Phone")
                        if (paired) Tag("Paired")
                    }
                }
            }
            if (busy) {
                if (inviting) ReticleSpinner(size = 18.dp)
                GameButton("Cancel", onClick = onCancel, style = GameButtonStyle.GHOST, compact = true)
            } else {
                GlassIconButton(
                    if (paired) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    if (paired) "Paired: syncs automatically" else "Pair to sync automatically",
                    onClick = onHeart,
                    tint = if (paired) scheme.primary else scheme.onSurfaceVariant,
                )
            }
        }
        if (send != null) {
            TransferBar(
                fraction = when (send.state) {
                    OutgoingShare.State.SENDING -> send.progress
                    OutgoingShare.State.WAITING -> null
                    else -> 1f
                },
                tone = when (send.state) {
                    OutgoingShare.State.SAVED -> BarTone.DONE
                    OutgoingShare.State.DECLINED -> BarTone.STOPPED
                    OutgoingShare.State.FAILED -> BarTone.FAILED
                    else -> BarTone.WORKING
                },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

private enum class BarTone { WORKING, DONE, STOPPED, FAILED }

/**
 * How far a transfer has got. A [fraction] fills the bar; null sweeps it while the other side
 * works or decides (still and half-filled with reduced motion). Done turns it gold, like a full gauge.
 */
@Composable
private fun TransferBar(fraction: Float?, tone: BarTone, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val reduce = LocalReduceMotion.current
    val track = scheme.onSurface.copy(alpha = if (isTwilight()) 0.12f else 0.1f)
    val accent = Accent
    val fill = when (tone) {
        BarTone.WORKING -> Brush.horizontalGradient(listOf(lighten(accent, 0.3f), accent))
        BarTone.DONE -> Brush.horizontalGradient(listOf(Color(0xFFFFE08A), Gold, Color(0xFFFFA94D)))
        BarTone.STOPPED -> SolidColor(scheme.outline)
        BarTone.FAILED -> SolidColor(scheme.error)
    }
    val filled by animateFloatAsState(fraction ?: 0f, motion(Motion.LONG), label = "transfer")
    // Read only while drawing, so the sweep redraws the bar without recomposing it.
    val sweep: State<Float>? = if (fraction != null || reduce) null else {
        val t = rememberInfiniteTransition(label = "transfer-sweep")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "sweep")
    }
    Canvas(
        modifier.fillMaxWidth().height(6.dp).clip(LockOnShapes.button).semantics {
            progressBarRangeInfo = if (fraction == null) ProgressBarRangeInfo.Indeterminate else ProgressBarRangeInfo(fraction, 0f..1f)
        },
    ) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        when {
            fraction != null -> if (filled > 0f) drawRoundRect(fill, size = Size(size.width * filled, size.height), cornerRadius = r)
            sweep == null -> drawRoundRect(fill, size = Size(size.width / 2, size.height), cornerRadius = r, alpha = 0.6f)
            else -> {
                val segment = size.width * 0.35f
                val x = -segment + (size.width + segment) * sweep.value
                drawRoundRect(fill, topLeft = Offset(x, 0f), size = Size(segment, size.height), cornerRadius = r)
            }
        }
    }
}

/** A gold tick that pops in when decks have arrived. */
@Composable
private fun DoneBadge(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    val reduce = LocalReduceMotion.current
    val scale = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)) }
    Box(
        modifier.size(size).graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(CircleShape).background(MaterialTheme.colorScheme.tertiary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Check, contentDescription = "Done", tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(size * 0.7f))
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

/**
 * Picks decks, grouped by folder. A folder's own box takes or drops all of its decks; searching
 * matches deck or folder names and only hides decks, so ones already ticked stay chosen.
 */
@Composable
private fun ChooseDecksDialog(
    decks: List<DeckSummary>, preselected: Set<String>, sendingTo: String?, onDismiss: () -> Unit, onDone: (List<String>) -> Unit,
) {
    val sorted = remember(decks) { decks.sortedBy { it.snapshot.name.lowercase() } }
    val groups = remember(sorted) {
        sorted.groupBy { it.snapshot.folder }.entries.sortedWith(compareBy({ it.key == null }, { it.key?.lowercase() }))
    }
    var chosen by remember { mutableStateOf(preselected) }
    var search by remember { mutableStateOf("") }
    var listHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val q = search.trim()
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
                    var any = false
                    groups.forEach { (folder, inGroup) ->
                        val folderMatches = folder?.contains(q, ignoreCase = true) == true
                        val shown = if (folderMatches) inGroup else inGroup.filter { it.snapshot.name.contains(q, ignoreCase = true) }
                        if (shown.isEmpty()) return@forEach
                        any = true
                        // Decks with no folder only get a heading when there are folders to tell them apart from.
                        if (folder != null || groups.size > 1) {
                            val ids = inGroup.map { it.deckId }
                            val count = ids.count { it in chosen }
                            val box = when (count) {
                                0 -> ToggleableState.Off
                                ids.size -> ToggleableState.On
                                else -> ToggleableState.Indeterminate
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(top = 4.dp).clip(LockOnShapes.thumb)
                                    .triStateToggleable(box, role = Role.Checkbox) {
                                        chosen = if (box == ToggleableState.On) chosen - ids.toSet() else chosen + ids
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TriStateCheckbox(box, onClick = null, modifier = Modifier.padding(12.dp))
                                Text(folder ?: "No folder", style = MaterialTheme.typography.titleSmall, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("$count/${ids.size}", style = MaterialTheme.typography.labelMedium.merge(TabularNumbers),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
                            }
                        }
                        val indent = if (folder != null || groups.size > 1) 20.dp else 0.dp
                        shown.forEach { deck ->
                            val checked = deck.deckId in chosen
                            Row(
                                Modifier.fillMaxWidth().padding(start = indent).clip(LockOnShapes.thumb)
                                    .toggleable(checked, role = Role.Checkbox) {
                                        chosen = if (it) chosen + deck.deckId else chosen - deck.deckId
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(deck.snapshot.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Main ${deck.snapshot.count(DeckSection.MAIN)}", style = MaterialTheme.typography.bodySmall.merge(TabularNumbers),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (!any) Text("No decks match.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
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
                    else -> "Send ${deckCount(ids.size)}"
                },
                onClick = { onDone(ids) }, compact = true, enabled = sendingTo == null || ids.size in 1..ShareInbox.MAX_DECKS,
            )
        },
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

/**
 * Receive: decks another device is sending, LocalSend-style, saved only if accepted. Once accepted
 * it fills a bar as they save, then shows a gold tick and the count until closed.
 */
@Composable
private fun IncomingShareDialog(sharing: DeckSharing, fromDesktop: Boolean) {
    val share by sharing.incoming.collectAsState()
    val s = share ?: return
    val asking = s.state == IncomingShare.State.ASKING
    val done = s.state == IncomingShare.State.SAVED
    Dialog(
        onDismissRequest = { if (done) sharing.dismiss(s.shareId) },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = done, dismissOnBackPress = done),
    ) {
        val scheme = MaterialTheme.colorScheme
        Column(
            Modifier.padding(20.dp).widthIn(max = 460.dp).fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(if (isTwilight()) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest)
                .glassFrame(MaterialTheme.shapes.extraLarge)
                .animateContentSize(glide())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box {
                DeviceGlyph(fromDesktop, size = 72)
                if (done) DoneBadge(Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp), size = 28.dp)
            }
            Text(s.senderName, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                when (s.state) {
                    IncomingShare.State.ASKING -> "wants to send you ${if (s.decks.size == 1) "a deck" else "${s.decks.size} decks"}"
                    IncomingShare.State.SAVING -> "Saving ${s.saved} of ${deckCount(s.decks.size)}…"
                    IncomingShare.State.SAVED -> "Saved ${deckCount(s.saved)}"
                },
                style = MaterialTheme.typography.bodyLarge.merge(TabularNumbers),
                color = if (done) (if (isTwilight()) Gold else scheme.tertiary) else scheme.onSurface,
            )
            if (!asking) {
                TransferBar(
                    fraction = s.saved.toFloat() / s.decks.size.coerceAtLeast(1),
                    tone = if (done) BarTone.DONE else BarTone.WORKING,
                )
            }
            Column(
                Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())
                    .clip(LockOnShapes.tile).background(scheme.onSurface.copy(alpha = 0.05f)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                s.decks.forEachIndexed { i, deck ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(deck.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (!asking && i < s.saved) {
                            Icon(Icons.Default.Check, contentDescription = "Saved", tint = scheme.primary, modifier = Modifier.size(16.dp))
                        }
                        Text("Main ${deck.count(DeckSection.MAIN)}", style = MaterialTheme.typography.bodyMedium.merge(TabularNumbers),
                            color = scheme.onSurfaceVariant)
                    }
                }
            }
            Text(
                if (done) "They're in your decks now." else "They're added as new decks. Nothing else changes.",
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                when (s.state) {
                    IncomingShare.State.ASKING -> {
                        GameButton("Decline", onClick = { sharing.answer(s.shareId, false) }, style = GameButtonStyle.SECONDARY)
                        GameButton("Accept", onClick = { sharing.answer(s.shareId, true) }, icon = Icons.Default.Check)
                    }
                    IncomingShare.State.SAVING -> GameButton("Saving…", onClick = {}, enabled = false)
                    IncomingShare.State.SAVED -> GameButton("Done", onClick = { sharing.dismiss(s.shareId) })
                }
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
