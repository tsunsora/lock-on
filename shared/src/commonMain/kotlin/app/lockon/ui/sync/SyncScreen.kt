package app.lockon.ui.sync

import app.lockon.ui.components.HairlineWidth
import app.lockon.ui.components.glassColors
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.horizontalScroll
import app.lockon.ui.components.Reveal
import app.lockon.ui.components.RevealValue
import app.lockon.ui.components.RevealItems
import app.lockon.ui.components.FadeSwap
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import app.lockon.ui.components.LineIcons
import androidx.compose.runtime.derivedStateOf
import app.lockon.ui.components.rememberArrival
import app.lockon.ui.components.arrive
import app.lockon.ui.components.onDialogStage
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.TriStateCheckbox
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
import app.lockon.ui.components.motion
import kotlin.math.roundToInt
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import app.lockon.sync.DeviceRole
import app.lockon.sync.SyncMode
import app.lockon.sync.SyncStatus
import app.lockon.sync.SyncTransfer
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.GameDialog
import app.lockon.ui.components.panelFill
import app.lockon.ui.components.GlassIconButton
import app.lockon.ui.components.LocalNavClearance
import app.lockon.ui.components.LocalReduceMotion
import app.lockon.ui.components.loopingFloat
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
import app.lockon.ui.components.dealIn
import app.lockon.ui.components.pressWash
import app.lockon.ui.components.isTwilight
import app.lockon.ui.components.pressScale
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.theme.TabularNumbers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock

/**
 * Sync: your paired devices, each a tap from being made to match this one or the other way round,
 * which copies everything (decks with their history, folders, match logs, settings and the card
 * list). Merging both ways is the softer option in the same place. Sending a few decks to anyone
 * nearby, LocalSend-style, has its own page.
 */
@Composable
fun SyncScreen(
    controller: SyncController,
    deviceName: String,
    /** Decks that can be sent to a nearby device. */
    decks: List<DeckSummary> = emptyList(),
    /** The catalog, for showing each chosen deck's colours. */
    cards: Map<String, app.lockon.model.Card> = emptyMap(),
    /** Which page shows: Send decks, or Sync devices. Kept by the caller so it survives leaving the tab. */
    sending: Boolean = false,
    onSendingChange: (Boolean) -> Unit = {},
) {
    // The chosen decks stay chosen while flipping to Sync devices and back.
    var chosen by remember { mutableStateOf(emptyList<String>()) }
    val status by controller.status.collectAsState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val grouped = maxWidth >= SideBySide || (maxWidth >= 640.dp && maxHeight < 520.dp)
        androidx.compose.runtime.CompositionLocalProvider(LocalGrouped provides grouped) {
            // Read top to bottom: this device, the switch for what the page shows, then the page (UX rule R3:
            // a switch for a screen's own content sits directly above it; Sync now stays low on the page).
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        Modifier.widthIn(max = if (grouped) GroupedWidth else Dp.Unspecified).fillMaxWidth()
                            .padding(start = TrayInset, end = TrayInset, top = if (grouped) GroupedTabsTop else 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThisDevice(deviceName, status)
                        PillTabs(listOf("Devices", "Send decks"), if (sending) 1 else 0, { onSendingChange(it == 1) }, Modifier, fill = true, soft = true, itemHeight = ControlSize, bare = true)
                    }
                }
                Crossfade(sending, Modifier.weight(1f).fillMaxWidth(), animationSpec = motion(), label = "sync-page") { send ->
                    if (send) SendPage(controller, decks, cards, chosen) { chosen = it }
                    else DevicesPage(controller)
                }
            }
        }
    }
}

/** A panel on this screen: the solid fill of the other screens, in the phone's tile shape or the given wider one. */
@Composable
private fun Modifier.syncPanel(wide: androidx.compose.ui.graphics.Shape): Modifier =
    clip(if (app.lockon.ui.components.LocalTopBand.current.height == 0.dp) LockOnShapes.tile else wide).background(panelFill())

/** Windows at least this wide, or wide and too short for a tray, group the page into panels. */
private val SideBySide = 840.dp

/** Whether this page is grouped into panels under the tabs, rather than resting on the phone's tray. */
private val LocalGrouped = androidx.compose.runtime.compositionLocalOf { false }

/** The grouped column: its width, the tabs' place over it, and the gap between its panels. */
private val GroupedWidth = 760.dp
private val GroupedTabsWidth = 244.dp
private val GroupedTabsTop = 16.dp
private val PanelGap = 24.dp
private val PanelPad = 18.dp

/** Between one section and the next in the tray layout, and between a section's heading and rows in both. */
private val SectionGap = 28.dp
private val RowGap = 12.dp

/** The phone's screen padding. A thumb gets 48 dp on everything here. */
private val TrayInset = 16.dp

/** Under this, two buttons side by side give up their icons so the labels stay on one line. */
private val TwoButtonsWithIcons = 320.dp

/** A control's height where it takes a thumb; a pointer's controls keep their own. */
@Composable
private fun thumbSized(): Modifier =
    if (app.lockon.platform.isMobile || app.lockon.ui.components.LocalTopBand.current.height == 0.dp) Modifier.heightIn(min = 48.dp) else Modifier

/** A device row's shape: a quiet tile on its section's panel when grouped, a solid tile on the sky otherwise. */
@Composable
private fun deviceShape(): androidx.compose.ui.graphics.Shape = if (LocalGrouped.current) RoundedCornerShape(12.dp) else LockOnShapes.tile

@Composable
private fun Modifier.deviceRow(): Modifier = clip(deviceShape()).then(
    // On phones a row lies straight on its section's panel; grouped, it is a quiet tile on it.
    if (!LocalGrouped.current) Modifier
    else Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (isTwilight()) 0.06f else 0.045f)),
)

/** The line under a device's name: a size up where a grouped row has the room. */
@Composable
private fun statusStyle(): androidx.compose.ui.text.TextStyle =
    if (LocalGrouped.current) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall

/**
 * Where a page's parts go: [top] (a panel of what to send), then the [list], then [bottom], the page's
 * actions. Grouped, one centred column. On phones the list scrolls, and [bottom] is pinned 24 dp above the
 * bottom bar in thumb reach (UX rule R3), so the free space falls between the list and it; [bottomPanel]
 * puts it on a solid panel.
 */
@Composable
private fun SyncFrame(
    top: (@Composable ColumnScope.() -> Unit)? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    bottomPanel: Boolean = false,
    list: @Composable ColumnScope.() -> Unit,
) {
    val clearance = LocalNavClearance.current
    if (LocalGrouped.current) {
        Box(Modifier.fillMaxSize()) {
            val scrollState = rememberScrollState()
            Box(Modifier.fillMaxSize().verticalScroll(scrollState), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = GroupedWidth).fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp + clearance)) {
                    if (top != null) Column(
                        Modifier.fillMaxWidth().padding(top = 16.dp).dealIn("sync-controls", 0).clip(LockOnShapes.tile).background(panelFill())
                            .animateContentSize(glide()).padding(PanelPad),
                        content = top,
                    )
                    list()
                    if (bottom != null) Column(Modifier.fillMaxWidth().padding(top = PanelGap), content = bottom)
                }
            }
            VerticalScrollBar(scrollState, Modifier.align(Alignment.TopEnd))
        }
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // A window too short to pin the actions: they scroll with the list instead.
        val roomy = maxHeight >= 420.dp
        Column(Modifier.fillMaxSize()) {
            DeviceList(Modifier.weight(1f).fillMaxWidth(), PaddingValues(start = TrayInset, end = TrayInset, top = 16.dp, bottom = RowGap), fromBottom = false) {
                if (top != null && !roomy) Column(Modifier.fillMaxWidth().padding(bottom = SectionGap).clip(LockOnShapes.tile).background(panelFill()).padding(TrayInset), content = top)
                list()
                if (bottom != null && !roomy) Column(Modifier.fillMaxWidth().padding(top = SectionGap), content = bottom)
            }
            if (roomy && (bottom != null || top != null)) Column(
                Modifier.fillMaxWidth().padding(start = TrayInset, end = TrayInset, top = 8.dp, bottom = clearance + 12.dp).dealIn("sync-controls", 0)
                    .then(if (bottomPanel) Modifier.clip(LockOnShapes.tile).background(panelFill()).padding(TrayInset) else Modifier)
                    .animateContentSize(glide()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                top?.let { Column(Modifier.fillMaxWidth(), content = it) }
                bottom?.let { Column(Modifier.fillMaxWidth(), content = it) }
            }
        }
    }
}

/** The scrolling column of devices. [fromBottom] rests a short list on its end, near the thumb. */
@Composable
private fun DeviceList(modifier: Modifier, padding: PaddingValues, fromBottom: Boolean, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(modifier) {
        val scrollState = rememberScrollState()
        Column(
            Modifier.fillMaxWidth().verticalScroll(scrollState).heightIn(min = maxHeight).padding(padding),
            verticalArrangement = if (fromBottom) Arrangement.Bottom else Arrangement.Top,
            content = content,
        )
        VerticalScrollBar(scrollState, Modifier.align(Alignment.TopEnd))
    }
}

/**
 * One group of devices: its name and count, then each device. Grouped, that's a solid panel with the
 * heading inside, easing as rows come and go; otherwise a heading and rows on the sky. Each row brings
 * its own gap above, so a group with no rows ends at its heading.
 */
@Composable
private fun SyncSection(title: String, count: Int, first: Boolean, dealKey: String, content: @Composable ColumnScope.() -> Unit) {
    if (LocalGrouped.current) {
        val scheme = MaterialTheme.colorScheme
        Column(
            Modifier.padding(top = PanelGap).fillMaxWidth().dealIn(dealKey, 1).clip(LockOnShapes.tile).background(panelFill())
                .animateContentSize(glide()).padding(PanelPad),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                // The count rolls as devices come and go, like every other count.
                app.lockon.ui.components.RollingNumber(count, MaterialTheme.typography.titleMedium.merge(TabularNumbers), scheme.onSurfaceVariant, Modifier.padding(start = 8.dp))
            }
            content()
        }
    } else Column(Modifier.fillMaxWidth().padding(top = if (first) 0.dp else SectionGap)) {
        SectionHeader(title, readout = "$count")
        Column(
            Modifier.padding(top = 8.dp).fillMaxWidth().dealIn(dealKey, 1).clip(LockOnShapes.tile).background(panelFill())
                .border(HairlineWidth, glassColors().line, LockOnShapes.tile).animateContentSize(glide()).padding(bottom = 4.dp),
            content = content,
        )
    }
}

/**
 * Help when a device doesn't show, as the last row of Nearby: a 44 dp row in plain ink under a hairline.
 * Teal would call it a main action.
 */
@Composable
private fun TroubleshootRow(onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface
    Column(Modifier.fillMaxWidth().padding(top = RowGap)) {
        Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(HairlineWidth).background(glassColors().line))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).heightIn(min = 44.dp).clip(LockOnShapes.button)
                .clickable(role = Role.Button, onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(LineIcons.Info, contentDescription = null, Modifier.size(18.dp), tint = ink)
            Text("Troubleshoot", style = MaterialTheme.typography.labelLarge, color = ink)
        }
    }
}

/**
 * This device, at the top of the screen on its own panel: its mark, its name, that it's on Wi-Fi, and that
 * only decks sync with no account or server (UX rule T2). A sync that needs attention shows under it.
 */
@Composable
private fun ThisDevice(deviceName: String, status: SyncStatus) {
    val scheme = MaterialTheme.colorScheme
    val grouped = LocalGrouped.current
    Column(
        Modifier.fillMaxWidth().dealIn("sync-device", 0).clip(LockOnShapes.tile).background(panelFill())
            .border(HairlineWidth, glassColors().line, LockOnShapes.tile).animateContentSize(glide())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            app.lockon.ui.BrandTile(if (grouped) 56.dp else 48.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(deviceName, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(LineIcons.Wifi, contentDescription = null, Modifier.size(16.dp), tint = scheme.onSurfaceVariant)
                    // A non-breaking hyphen, so "Wi-Fi" never splits over two lines.
                    Text("On Wi‑Fi", style = statusStyle(), color = scheme.onSurfaceVariant, maxLines = 1)
                }
                Text("Decks only · no account, no server", style = MaterialTheme.typography.bodySmall, color = app.lockon.ui.components.mutedInk(), maxLines = 2)
            }
        }
        // Only what needs attention: a ready device says so by being here.
        RevealValue(status.takeIf { it.message != null && (it.isError || it.working) }) { StatusStrip(it, Modifier.fillMaxWidth().padding(top = 12.dp)) }
    }
}

/** The Devices page's actions: Sync now, the one solid action, over the other ways to pair. */
@Composable
private fun SyncActions(syncNow: (() -> Unit)?, qrLabel: String, onOtherWays: () -> Unit) {
    val grouped = LocalGrouped.current
    val qr: @Composable (Modifier) -> Unit = { m ->
        GameButton(qrLabel, onClick = onOtherWays, style = GameButtonStyle.SECONDARY, icon = LineIcons.Qr, compact = true, modifier = m)
    }
    val address: @Composable (Modifier) -> Unit = { m ->
        GameButton("Address", onClick = onOtherWays, style = GameButtonStyle.SECONDARY, icon = LineIcons.Text, compact = true, modifier = m)
    }
    // Side by side, the three share one height (UX rule K1); stacked on phones, the pair below keeps a thumb's 44 dp.
    if (grouped) Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (syncNow != null) GameButton("Sync now", onClick = syncNow, icon = LineIcons.Sync, modifier = Modifier.weight(1f).fillMaxHeight())
        qr(Modifier.weight(1f).fillMaxHeight())
        address(Modifier.weight(1f).fillMaxHeight())
    } else Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (syncNow != null) GameButton("Sync now", onClick = syncNow, icon = LineIcons.Sync, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            qr(Modifier.weight(1f).heightIn(min = 44.dp))
            address(Modifier.weight(1f).heightIn(min = 44.dp))
        }
    }
}

/**
 * Sync devices: paired devices first, each opening the choice of which way to sync; then devices
 * nearby that aren't paired yet, where a tap pairs and then asks the same question.
 */
@Composable
private fun DevicesPage(controller: SyncController) {
    val paired by controller.pairedDevices.collectAsState(initial = emptyList())
    val transfers by controller.transfers.collectAsState()
    val nearby by when (controller) {
        is HostSyncController -> controller.nearby
        is ClientSyncController -> controller.nearby
    }.collectAsState()
    val invited by ((controller as? HostSyncController)?.invited ?: remember { MutableStateFlow(emptySet()) }).collectAsState()
    val liveClients by ((controller as? HostSyncController)?.liveClients ?: remember { MutableStateFlow(emptySet()) }).collectAsState()
    val autoStates by ((controller as? ClientSyncController)?.autoStates ?: remember { MutableStateFlow(emptyMap()) }).collectAsState()
    val merging by ((controller as? ClientSyncController)?.merging ?: remember { MutableStateFlow(emptySet()) }).collectAsState()
    val pairedIds = paired.map { it.deviceId }.toSet()
    val unpaired = nearby.filter { it.deviceId !in pairedIds }
    val isHost = controller is HostSyncController

    var choosing by remember { mutableStateOf<String?>(null) }
    // A nearby device tapped to pair: once it is, the choice of how to sync opens by itself.
    var pairingWith by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pairingWith, pairedIds) {
        if (pairingWith != null && pairingWith in pairedIds) { choosing = pairingWith; pairingWith = null }
    }
    var showOther by remember { mutableStateOf(false) }
    var troubleshoot by remember { mutableStateOf(false) }

    fun pair(device: NearbyDevice) {
        pairingWith = device.deviceId
        when (controller) {
            is HostSyncController -> controller.invite(device)
            is ClientSyncController -> controller.pair(PairingInvite(device.host, device.port, "", device.fingerprint, device.name))
        }
    }

    val devices: @Composable () -> Unit = {
        SyncSection("Your devices", paired.size, first = true, dealKey = "paired") {
            Reveal(paired.isEmpty() && unpaired.isEmpty()) { Box(Modifier.padding(top = rowTop())) { SearchingCard() } }
            RevealItems(paired, { it.deviceId }) { device ->
                val i = paired.indexOf(device).coerceAtLeast(0)
                Box(Modifier.padding(top = rowTop())) {
                    PairedTile(
                        device = device,
                        transfer = transfers[device.deviceId],
                        live = when (controller) {
                            is ClientSyncController -> autoStates[device.deviceId]?.let { it.label to (it == AutoSyncState.LIVE) }
                            is HostSyncController -> if (device.deviceId in liveClients) "Live" to true else null
                        },
                        index = i + 1,
                        // With one device, Sync now below is its button.
                        rowSync = paired.size > 1,
                        onTap = { choosing = device.deviceId },
                        onClear = { controller.clearTransfer(device.deviceId) },
                        onRetry = { mode -> controller.sync(device.deviceId, mode) },
                    )
                }
            }
        }
        run {
            SyncSection("Nearby", unpaired.size, first = false, dealKey = "nearby") {
                RevealItems(unpaired, { it.deviceId }) { device ->
                    Box(Modifier.padding(top = rowTop())) {
                        DeviceTile(
                            device = device,
                            send = null,
                            inviting = device.deviceId in invited,
                            hint = "Not paired",
                            action = "Pair",
                            washed = true,
                            actionIcon = app.lockon.ui.components.Glyphs.Link,
                            index = paired.size + unpaired.indexOf(device).coerceAtLeast(0) + 1,
                            onTap = { if (device.compatible) pair(device) },
                            onCancel = { (controller as? HostSyncController)?.cancelInvite(device.deviceId); pairingWith = null },
                        )
                    }
                }
                TroubleshootRow { troubleshoot = true }
            }
        }
    }

    // Sync now opens the choice for the device synced most recently: the only one, nearly always.
    val syncNow = paired.maxByOrNull { it.lastSyncAt ?: 0L }?.let { device -> { choosing = device.deviceId } }
    SyncFrame(
        bottom = { SyncActions(syncNow, qrLabel = if (isHost) "QR code" else "Scan QR code", onOtherWays = { showOther = true }) },
    ) { devices() }

    paired.firstOrNull { it.deviceId == choosing }?.let { device ->
        SyncChoiceDialog(
            device = device,
            here = if (isHost) "this computer" else "this phone",
            merging = if (controller is ClientSyncController) device.deviceId in merging else null,
            onSync = { controller.sync(device.deviceId, it) },
            onMerging = { (controller as? ClientSyncController)?.setMerging(device.deviceId, it) },
            onForget = { controller.forget(device.deviceId) },
            onDismiss = { choosing = null },
        )
    }
    if (showOther) OtherWaysDialog(controller) { showOther = false }
    if (troubleshoot) TroubleshootDialog(isHost, onDismiss = { troubleshoot = false }, onOtherWays = { troubleshoot = false; showOther = true })
}

@Composable
private fun TroubleshootDialog(isHost: Boolean, onDismiss: () -> Unit, onOtherWays: (() -> Unit)?) {
    GameDialog(
        onDismissRequest = onDismiss,
        title = "Can't see a device?",
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• Both devices need Lock On open, on the same Wi-Fi network.")
                Text("• Guest and office networks often hide devices from each other.")
                if (isHost) Text("• Allow Lock On through the Windows firewall for private networks.")
                else Text("• On the computer, allow Lock On through the Windows firewall for private networks.")
                if (onOtherWays != null) {
                    Text("Still nothing? Connect with a QR code or the computer's address instead.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { if (onOtherWays != null) GameButton("QR code or address", onClick = onOtherWays, compact = true) },
        dismissButton = { GameButton("Close", onClick = onDismiss, style = GameButtonStyle.GHOST, compact = true) },
    )
}

/** When a device last synced, in words: "Last synced 3 min ago". */
private fun lastSynced(epochMillis: Long): String {
    val minutes = (kotlin.time.Clock.System.now().toEpochMilliseconds() - epochMillis) / 60_000
    return when {
        minutes < 1 -> "Last synced just now"
        minutes < 60 -> "Last synced $minutes min ago"
        minutes < 24 * 60 -> "Last synced ${minutes / 60} h ago"
        else -> "Last synced " + app.lockon.platform.dayLabel(epochMillis)
    }
}

/** What a sync's line under the device name says while it runs and once it's done. */
private fun SyncTransfer.label(): String = message ?: when (state) {
    SyncTransfer.State.WAITING -> "Waiting for $deviceName…"
    SyncTransfer.State.WORKING -> "Syncing…"
    SyncTransfer.State.DONE -> "Synced"
    SyncTransfer.State.DECLINED -> "$deviceName declined."
    SyncTransfer.State.FAILED -> "Sync failed."
}

/**
 * A paired device. Tap it to choose which way to sync; a sync shows its progress under it, then
 * how it went until cleared.
 */
@Composable
private fun PairedTile(
    device: PairedDevice,
    transfer: SyncTransfer?,
    live: Pair<String, Boolean>?,
    index: Int,
    rowSync: Boolean,
    onTap: () -> Unit,
    onClear: () -> Unit,
    onRetry: (SyncMode) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme
    val busy = transfer?.active == true
    val grouped = LocalGrouped.current
    val failed = transfer?.state == SyncTransfer.State.FAILED
    // A failed sync tints its row in the error role and says what to do next (UX rules V7, T3); no bar or stripe marks it (K8).
    val tint by animateColorAsState(if (failed) scheme.error.copy(alpha = if (isTwilight()) 0.14f else 0.08f) else Color.Transparent, motion(), label = "failed-tint")
    Column(
        Modifier.fillMaxWidth().dealIn(device.deviceId, index).pressScale(interaction).deviceRow().background(tint)
            .clickable(interaction, indication = pressWash(deviceShape()), enabled = !busy, role = Role.Button, onClick = onTap)
            .animateContentSize(glide())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.heightIn(min = if (grouped) 48.dp else 52.dp), verticalAlignment = Alignment.CenterVertically) {
            DeviceGlyph(device.role == DeviceRole.HOST)
            Spacer(Modifier.width(if (grouped) 16.dp else 12.dp))
            Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // Fades across as a transfer changes state; its rising count stays in the one line.
                FadeSwap(transfer, contentKey = { it?.state }) { shownTransfer ->
                if (shownTransfer?.state == SyncTransfer.State.FAILED) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(LineIcons.Alert, contentDescription = "Error", Modifier.padding(top = if (grouped) 2.dp else 0.dp).size(16.dp), tint = scheme.error)
                            Text(shownTransfer.label(), style = statusStyle(), color = scheme.error)
                        }
                        Text("Check that Lock On is open on ${device.name} and that both are on the same Wi-Fi.", style = statusStyle(), color = scheme.onSurfaceVariant)
                    }
                } else if (shownTransfer != null) {
                    DoneLine(shownTransfer.label(), done = shownTransfer.state == SyncTransfer.State.DONE, color = scheme.onSurfaceVariant)
                } else {
                    // How it's connected or what went wrong (Live, or Waiting for the desktop, T5), then when it last synced.
                    val problem = device.lastError
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (problem != null) Icon(LineIcons.Info, contentDescription = null, Modifier.size(14.dp), tint = scheme.error)
                            else if (live != null) Box(Modifier.size(6.dp).clip(CircleShape).background(if (live.second) scheme.onSurface else scheme.outline))
                            Text(problem ?: live?.first ?: "Paired", style = statusStyle(), color = when { problem != null -> scheme.error; live?.second == true -> scheme.onSurface; else -> scheme.onSurfaceVariant },
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Text(device.lastSyncAt?.let { lastSynced(it) } ?: "Not synced yet", style = statusStyle(), color = scheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                }
            }
            val trailing = when {
                transfer?.state == SyncTransfer.State.WAITING -> "cancel"
                busy -> "working"
                failed -> "none"
                transfer != null -> "ok"
                device.lastError != null -> "retry"
                rowSync -> "sync"
                else -> "none"
            }
            FadeSwap(trailing, contentAlignment = Alignment.CenterEnd) { action ->
                when (action) {
                    "cancel" -> GameButton("Cancel", onClick = onClear, style = GameButtonStyle.SECONDARY, compact = true, modifier = thumbSized())
                    "working" -> ReticleSpinner(size = 20.dp, modifier = Modifier.padding(end = 12.dp))
                    "ok" -> GameButton("Done", onClick = onClear, style = GameButtonStyle.SECONDARY, compact = true, modifier = thumbSized())
                    "retry" -> GameButton("Retry", onClick = onTap, style = GameButtonStyle.SECONDARY, icon = LineIcons.Sync, compact = true, onTone = scheme.error, modifier = thumbSized())
                    "sync" -> GameButton("Sync", onClick = onTap, style = GameButtonStyle.SECONDARY, icon = LineIcons.Sync.takeIf { grouped }, compact = true, modifier = thumbSized())
                    else -> Spacer(Modifier.size(0.dp))
                }
            }
        }
        // A failed sync gets its two ways on: try again the same way, or put it away.
        Reveal(failed) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                GameButton("Dismiss", onClick = onClear, style = GameButtonStyle.GHOST, compact = true, modifier = thumbSized())
                GameButton("Retry", onClick = { transfer?.let { onRetry(it.mode) } }, style = GameButtonStyle.SECONDARY, icon = LineIcons.Sync, compact = true, modifier = thumbSized())
            }
        }
        // The tile already eases its own height, so the bar only fades. A failed sync has no bar: its words and tint say it.
        RevealValue(transfer?.takeIf { busy || it.state == SyncTransfer.State.DECLINED }, fadeOnly = true) { shownTransfer ->
            TransferBar(
                fraction = if (shownTransfer.active) null else 1f,
                tone = when (shownTransfer.state) {
                    SyncTransfer.State.DONE -> BarTone.DONE
                    SyncTransfer.State.DECLINED -> BarTone.STOPPED
                    SyncTransfer.State.FAILED -> BarTone.FAILED
                    else -> BarTone.WORKING
                },
                flat = true,
            )
        }
    }
}

/**
 * Which way to sync with a paired device. Making one device match the other is the main choice and
 * asks once more before replacing anything; merging both ways keeps everything from both.
 * @param merging whether this phone keeps merging with the device on its own; null on the desktop, where phones decide.
 */
@Composable
private fun SyncChoiceDialog(
    device: PairedDevice,
    here: String,
    merging: Boolean?,
    onSync: (SyncMode) -> Unit,
    onMerging: (Boolean) -> Unit,
    onForget: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirming by remember { mutableStateOf<SyncMode?>(null) }
    var forgetting by remember { mutableStateOf(false) }
    val hereTitle = here.replaceFirstChar { it.uppercase() }
    GameDialog(
        onDismissRequest = onDismiss,
        title = "Sync with ${device.name}",
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeOption(
                    "Make $here match ${device.name}",
                    "Everything here is replaced by what's on ${device.name}.",
                    LineIcons.Chevd, primary = true,
                ) { confirming = SyncMode.TAKE }
                ModeOption(
                    "Make ${device.name} match $here",
                    "Everything on ${device.name} is replaced by what's here.",
                    LineIcons.Chevu,
                ) { confirming = SyncMode.GIVE }
                Text("Or keep both", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
                ModeOption(
                    "Merge both ways",
                    "Every deck from both ends up on both. Settings and the card list stay as they are, and a deck edited on both shows as a conflict.",
                    LineIcons.Sync,
                ) { onSync(SyncMode.MERGE); onDismiss() }
                if (merging != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Keep merging", style = MaterialTheme.typography.titleSmall)
                            Text("While Lock On is open on both, edits on either merge on their own.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        PillSwitch(checked = merging, onCheckedChange = onMerging)
                    }
                }
            }
        },
        confirmButton = { GameButton("Close", onClick = onDismiss, style = GameButtonStyle.GHOST, compact = true) },
        dismissButton = { GameButton("Forget device", onClick = { forgetting = true }, style = GameButtonStyle.GHOST, compact = true) },
    )
    confirming?.let { mode ->
        val target = if (mode == SyncMode.TAKE) hereTitle else device.name
        val source = if (mode == SyncMode.TAKE) device.name else here
        GameDialog(
            onDismissRequest = { confirming = null },
            title = "Replace everything on $target?",
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("$target will match $source: its decks, folders, match logs, settings, ruleset and card list are replaced.")
                    Text("Decks that are only on $target are removed. Earlier versions of replaced decks stay in their history.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { GameButton("Replace", onClick = { onSync(mode); confirming = null; onDismiss() }, compact = true) },
            dismissButton = { GameButton("Cancel", onClick = { confirming = null }, style = GameButtonStyle.GHOST, compact = true) },
        )
    }
    if (forgetting) {
        GameDialog(
            onDismissRequest = { forgetting = false },
            title = "Forget ${device.name}?",
            text = { Text("It will need to pair again before it can sync. Decks on both devices stay as they are.") },
            confirmButton = { GameButton("Forget", onClick = { onForget(); forgetting = false; onDismiss() }, compact = true) },
            dismissButton = { GameButton("Cancel", onClick = { forgetting = false }, style = GameButtonStyle.GHOST, compact = true) },
        )
    }
}

/** One way to sync, as a glass tile with what it does underneath. The [primary] one is tinted. */
@Composable
private fun ModeOption(title: String, detail: String, icon: ImageVector, primary: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().pressScale(interaction).glass(LockOnShapes.tile)
            .then(if (primary) Modifier.clip(LockOnShapes.tile).background(scheme.primary.copy(alpha = 0.08f)) else Modifier)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(if (primary) scheme.primary else scheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (primary) scheme.onPrimary else scheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

/**
 * Send decks: what to send, then where. Tapping a device sends; a device tapped first asks which
 * decks. Any nearby device can receive, paired or not. Wide windows put the two side by side.
 */
@Composable
private fun SendPage(controller: SyncController, decks: List<DeckSummary>, cards: Map<String, app.lockon.model.Card>, chosen: List<String>, onChosen: (List<String>) -> Unit) {
    val status by controller.status.collectAsState()
    val paired by controller.pairedDevices.collectAsState(emptyList())
    val sharing = controller.sharing
    val targets by (sharing?.targets ?: remember { MutableStateFlow(emptyList()) }).collectAsState()
    val outgoing by (sharing?.outgoing ?: remember { MutableStateFlow(emptyMap()) }).collectAsState()

    var choosing by remember { mutableStateOf(false) }
    // A device tapped before any decks were chosen: once they are, they go straight to it.
    var sendTo by remember { mutableStateOf<NearbyDevice?>(null) }
    var troubleshoot by remember { mutableStateOf(false) }
    // Decks deleted since they were chosen drop out of the selection.
    val selected = chosen.mapNotNull { id -> decks.firstOrNull { it.deckId == id } }
    val isHost = controller is HostSyncController

    val where: @Composable () -> Unit = {
        if (sharing == null) {
            Text(
                "This device can't send decks.", style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(top = if (LocalGrouped.current) PanelGap else 0.dp).deviceRow().padding(16.dp),
            )
        } else if (targets.isEmpty()) {
            SyncSection("Your devices", 0, first = true, dealKey = "send-searching") { Box(Modifier.padding(top = rowTop())) { SearchingCard() } }
        }
        val pairedIds = paired.map { it.deviceId }.toSet()
        val (yours, nearby) = targets.partition { it.deviceId in pairedIds }
        var first = true
        listOf("Your devices" to yours, "Nearby" to nearby).forEach { (title, group) ->
            if (group.isEmpty()) return@forEach
            SyncSection(title, group.size, first, dealKey = "send-$title") {
                group.forEachIndexed { i, device ->
                    Box(Modifier.padding(top = rowTop())) {
                        DeviceTile(
                            device = device,
                            send = outgoing[device.deviceId],
                            inviting = false,
                            hint = if (device.deviceId in pairedIds) "Paired" else "Not paired",
                            action = "Send",
                            actionIcon = LineIcons.Send,
                            index = i,
                            onTap = {
                                when {
                                    !device.compatible -> Unit
                                    selected.isEmpty() -> { sendTo = device; choosing = true }
                                    else -> sharing?.send(device, selected.map { it.deckId })
                                }
                            },
                            onCancel = { sharing?.cancelSend(device.deviceId) },
                        )
                    }
                }
            }
            first = false
        }
    }

    val grouped = LocalGrouped.current
    SyncFrame(
        top = if (sharing == null) null else ({ WhatToSend(decks, selected, cards, onChoose = { choosing = true }, onChosen = onChosen) }),
        bottomPanel = true,
    ) {
        where()
        Box(Modifier.fillMaxWidth().padding(top = RowGap), contentAlignment = Alignment.Center) {
            GameButton("Troubleshoot", onClick = { troubleshoot = true }, style = GameButtonStyle.GHOST, icon = LineIcons.Info, compact = true,
                tone = MaterialTheme.colorScheme.onSurface, modifier = thumbSized())
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
    if (troubleshoot) TroubleshootDialog(isHost, onDismiss = { troubleshoot = false }, onOtherWays = null)
}

/**
 * How the last sync went, on a tinted strip inside the panel: a spinner while it works, the error
 * icon on the error tint when it failed (with its words, UX rule V7), and a teal dot otherwise.
 */
@Composable
private fun StatusStrip(status: SyncStatus, modifier: Modifier = Modifier) {
    val message = status.message ?: return
    val scheme = MaterialTheme.colorScheme
    val tone by animateColorAsState(if (status.isError) scheme.error else scheme.primary, motion(), label = "status-tone")
    Row(
        modifier.height(IntrinsicSize.Min).clip(LockOnShapes.tile).background(tone.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            status.isError -> Icon(LineIcons.Alert, contentDescription = "Error", Modifier.size(16.dp), tint = scheme.error)
            status.working -> ReticleSpinner(size = 16.dp)
            else -> Box(Modifier.size(8.dp).clip(CircleShape).background(tone))
        }
        Text(
            message, style = MaterialTheme.typography.bodySmall,
            color = if (status.isError) scheme.error else scheme.onSurface, modifier = Modifier.weight(1f, fill = false),
        )
    }
}

private fun deckCount(n: Int) = if (n == 1) "1 deck" else "$n decks"

/**
 * What to send: pick decks, take them all, or tap a folder to add or drop its decks in one go. With
 * nothing chosen, Choose decks is the page's main action, so it is teal until something is. Grouped,
 * everything shares one centred line that wraps; in the tray, the buttons fill a line of their own
 * and chosen decks scroll sideways, so the tray keeps its height over the phone's bar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WhatToSend(
    decks: List<DeckSummary>, selected: List<DeckSummary>, cards: Map<String, app.lockon.model.Card>, onChoose: () -> Unit, onChosen: (List<String>) -> Unit,
) {
    val grouped = LocalGrouped.current
    val ids = selected.map { it.deckId }
    val folders = remember(decks) {
        decks.groupBy { it.snapshot.folder }.mapNotNull { (folder, inIt) -> folder?.let { it to inIt.map { d -> d.deckId } } }
            .sortedBy { it.first.lowercase() }
    }
    val chip: @Composable (DeckSummary) -> Unit = { deck -> ChosenDeck(deck, cards) { onChosen(ids - deck.deckId) } }
    val folderChip: @Composable (Modifier, Pair<String, List<String>>) -> Unit = { modifier, (folder, inFolder) ->
        val whole = inFolder.all { it in ids }
        PillChip(
            selected = whole,
            onClick = { onChosen(if (whole) ids - inFolder.toSet() else (ids + inFolder).distinct().take(ShareInbox.MAX_DECKS)) },
            label = { Text("$folder · ${inFolder.size}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(app.lockon.ui.components.Glyphs.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
            tall = true,
            modifier = modifier.then(thumbSized()).widthIn(max = 260.dp),
        )
    }
    val choose: @Composable (Modifier, Boolean) -> Unit = { modifier, withIcon ->
        GameButton(
            "Choose decks", onClick = onChoose, style = if (selected.isEmpty()) GameButtonStyle.PRIMARY else GameButtonStyle.SECONDARY,
            icon = LineIcons.Plus.takeIf { withIcon }, compact = true, modifier = thumbSized().then(modifier),
        )
    }
    // Every deck, or a folder's worth, in one tap.
    val offerAll = selected.isEmpty() && decks.size in 2..ShareInbox.MAX_DECKS
    val noDecks: @Composable () -> Unit = {
        Text("No decks to send yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (grouped) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val centred = Modifier.align(Alignment.CenterVertically)
            selected.forEach { Box(centred) { chip(it) } }
            if (decks.isNotEmpty()) choose(centred, true) else Box(centred) { noDecks() }
            if (offerAll) PillChip(
                selected = false,
                onClick = { onChosen(decks.map { it.deckId }) },
                label = { Text("All ${decks.size} decks", maxLines = 1) },
                leadingIcon = { Icon(LineIcons.Decks, contentDescription = null, modifier = Modifier.size(16.dp)) },
                tall = true,
                modifier = centred.then(thumbSized()),
            )
            folders.forEach { folderChip(centred, it) }
        }
        return
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (selected.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { selected.forEach { chip(it) } }
        if (decks.isEmpty()) noDecks()
        else BoxWithConstraints(Modifier.fillMaxWidth()) {
            val iconRoom = maxWidth >= TwoButtonsWithIcons || !offerAll
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choose(Modifier.weight(1f), iconRoom)
                if (offerAll) GameButton("All ${decks.size} decks", onClick = { onChosen(decks.map { it.deckId }) }, style = GameButtonStyle.SECONDARY, compact = true, modifier = thumbSized().weight(1f))
            }
        }
        if (folders.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            folders.forEach { folderChip(Modifier.align(Alignment.CenterVertically), it) }
        }
    }
}

/** A chosen deck: its colours, its name, and the X that leaves it out. */
@Composable
private fun ChosenDeck(deck: DeckSummary, cards: Map<String, app.lockon.model.Card>, onRemove: () -> Unit) {
    val colors = remember(deck, cards) {
        deck.snapshot.entries.filter { it.section == app.lockon.model.DeckSection.MAIN }.flatMap { cards[it.cardNumber]?.colors.orEmpty() }.toSet()
    }
    val touch = app.lockon.platform.isMobile || app.lockon.ui.components.LocalTopBand.current.height == 0.dp
    Row(
        Modifier.height(if (touch) 48.dp else ControlSize).clip(LockOnShapes.button)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)).padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        app.lockon.ui.components.ColorPips(colors, size = 18.dp)
        Text(deck.snapshot.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
        Icon(
            LineIcons.X, contentDescription = "Leave out ${deck.snapshot.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(if (touch) 48.dp else ControlSize).clip(LockOnShapes.button).clickable(onClick = onRemove).padding(if (touch) 14.dp else 11.dp),
        )
    }
}

/** One way to choose what to send, as a big glass tile. */
@Composable
private fun PickTile(
    label: String, icon: ImageVector, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val ink = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Column(
        modifier.heightIn(min = 84.dp).pressScale(interaction).glass(LockOnShapes.tile)
            .clickable(interaction, indication = pressWash(LockOnShapes.tile), enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
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
    Column(Modifier.fillMaxWidth().syncPanel(LockOnShapes.tile)) {
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // A selection with no folders at all needs no "No folder" heading.
                    if (folder != null || groups.size > 1) {
                        Text(
                            "${folder ?: "No folder"} · ${inGroup.size}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        inGroup.take(8).forEach { deck ->
                            Text(
                                deck.snapshot.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 220.dp).clip(LockOnShapes.button)
                                    .background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        if (inGroup.size > 8) Text("+${inGroup.size - 8} more", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

/** While nothing is found: the shape of a device, faded, so it's clear what will appear. */
@Composable
private fun SearchingCard() {
    Column(Modifier.fillMaxWidth().deviceRow()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                ReticleSpinner(size = 22.dp, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.width(16.dp))
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
    OutgoingShare.State.WAITING -> "Waiting for $deviceName to accept…"
    OutgoingShare.State.SAVED -> "Saved ${deckCount(deckCount)}"
    else -> message.orEmpty()
}

/**
 * A nearby device: tap it to send decks, or on Sync devices to pair. A send or an invitation shows
 * its progress under it. [hint] is the line shown while nothing is happening.
 */
@Composable
private fun DeviceTile(
    device: NearbyDevice,
    send: OutgoingShare?,
    inviting: Boolean,
    hint: String?,
    action: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    index: Int,
    onTap: () -> Unit,
    onCancel: () -> Unit,
    washed: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme
    val busy = send?.active == true || inviting
    val grouped = LocalGrouped.current
    Column(
        Modifier.fillMaxWidth().dealIn(device.deviceId, index).pressScale(interaction).deviceRow()
            .clickable(interaction, indication = pressWash(deviceShape()), enabled = device.compatible && !busy, role = Role.Button, onClick = onTap)
            .animateContentSize(glide())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.heightIn(min = if (grouped) 48.dp else 52.dp), verticalAlignment = Alignment.CenterVertically) {
            DeviceGlyph(device.isDesktop)
            Spacer(Modifier.width(if (grouped) 16.dp else 12.dp))
            Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                FadeSwap(send, contentKey = { listOf(device.compatible, inviting, it?.state) }) { shownSend ->
                    when {
                        !device.compatible -> Text("Needs a Lock On update", style = statusStyle(), color = scheme.error)
                        inviting -> Text("Waiting for ${device.name}…", style = statusStyle(), color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        shownSend != null -> DoneLine(
                            shownSend.label(), done = shownSend.state == OutgoingShare.State.SAVED,
                            color = if (shownSend.state == OutgoingShare.State.FAILED) scheme.error else scheme.onSurfaceVariant,
                        )
                        else -> hint?.let { Text(it, style = statusStyle(), color = scheme.onSurfaceVariant) }
                    }
                }
            }
            FadeSwap(busy, contentAlignment = Alignment.CenterEnd) { working ->
                if (working) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (inviting) ReticleSpinner(size = 20.dp)
                    GameButton("Cancel", onClick = onCancel, style = GameButtonStyle.SECONDARY, compact = true, modifier = thumbSized())
                // Pair is a teal wash with teal text, so Sync now stays the screen's one solid action.
                } else if (washed) GameButton(action, onClick = onTap, enabled = device.compatible, icon = actionIcon, compact = true, style = GameButtonStyle.GHOST,
                    modifier = thumbSized().clip(LockOnShapes.button).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)))
                else GameButton(action, onClick = onTap, enabled = device.compatible, icon = actionIcon, compact = true, modifier = thumbSized())
            }
        }
        // The tile already eases its own height, so the bar only fades.
        RevealValue(if (inviting) WaitingForAnswer else send?.takeIf { it.state != OutgoingShare.State.SAVED }, fadeOnly = true) { shown ->
            val shownSend = shown as? OutgoingShare
            TransferBar(
                fraction = when (shownSend?.state) {
                    null, OutgoingShare.State.WAITING -> null
                    OutgoingShare.State.SENDING -> shownSend?.progress
                    else -> 1f
                },
                tone = when (shownSend?.state) {
                    OutgoingShare.State.SAVED -> BarTone.DONE
                    OutgoingShare.State.DECLINED -> BarTone.STOPPED
                    OutgoingShare.State.FAILED -> BarTone.FAILED
                    else -> BarTone.WORKING
                },
                flat = true,
            )
        }
    }
}

/** Stands for an invitation in a tile's bar, which otherwise follows a send. */
private object WaitingForAnswer

/** A transfer's line under the device name, with a gold tick once it has finished well. */
@Composable
private fun DoneLine(text: String, done: Boolean, color: Color) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (done) Icon(LineIcons.Check, contentDescription = null, Modifier.padding(top = if (LocalGrouped.current) 2.dp else 0.dp).size(16.dp), tint = if (isTwilight()) Gold else MaterialTheme.colorScheme.tertiary)
        Text(text, style = statusStyle().merge(TabularNumbers), color = color)
    }
}

internal enum class BarTone { WORKING, DONE, STOPPED, FAILED }

/**
 * How far a transfer has got. A [fraction] fills the bar; null sweeps it while the other side
 * works or decides (still and half-filled with reduced motion). Done turns it gold, like a full gauge.
 */
@Composable
internal fun TransferBar(fraction: Float?, tone: BarTone, modifier: Modifier = Modifier, flat: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val track = scheme.onSurface.copy(alpha = if (isTwilight()) 0.12f else 0.1f)
    val accent = Accent
    // Finishing or failing, the bar's colour fades across; the two can't be blended as one brush.
    Crossfade(tone, modifier.fillMaxWidth(), animationSpec = motion(), label = "transfer-tone") { shownTone ->
    val fill = when (shownTone) {
        BarTone.WORKING -> SolidColor(accent)
        BarTone.DONE -> SolidColor(Gold)
        BarTone.STOPPED -> SolidColor(scheme.outline)
        BarTone.FAILED -> SolidColor(scheme.error)
    }
    val filled by animateFloatAsState(fraction ?: 0f, motion(Motion.LONG), label = "transfer")
    // Read only while drawing, so the sweep redraws the bar without recomposing it.
    val sweep = if (fraction != null) null else loopingFloat(0f, 1f, tween(1300, easing = FastOutSlowInEasing), "transfer-sweep")
    Canvas(
        Modifier.fillMaxWidth().height(if (flat) 8.dp else 6.dp).clip(LockOnShapes.button).semantics {
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
}

/** A gold tick that pops in when decks have arrived. */
@Composable
private fun DoneBadge(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    val reduce = LocalReduceMotion.current
    val scale = remember { Animatable(if (reduce) 1f else 0f) }
    // Settles in without overshoot (UX rule V5: no bounce); with reduced motion it is simply there.
    LaunchedEffect(Unit) { if (!reduce) scale.animateTo(1f, tween(Motion.MEDIUM, easing = Motion.emphasizedDecelerate)) }
    val from = 0.6f
    val shown = remember { derivedStateOf { from + (1f - from) * scale.value } }
    Box(
        modifier.size(size).graphicsLayer { scaleX = shown.value; scaleY = shown.value; alpha = scale.value }
            .clip(CircleShape).background(MaterialTheme.colorScheme.tertiary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(LineIcons.Check, contentDescription = "Done", tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(size * 0.7f))
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

/** For networks that hide devices: the computer shows a QR code and address, the phone scans or types it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OtherWaysDialog(controller: SyncController, onDismiss: () -> Unit) {
    GameDialog(
        onDismissRequest = onDismiss,
        title = "Connect by QR code or address",
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.widthIn(max = 320.dp)) {
                    Text("Or enter it by hand:", style = MaterialTheme.typography.labelLarge)
                    if (current.addresses.size > 1) {
                        Text("Address", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
 * Questions from other devices: decks someone wants to send, pairing requests, and a desktop asking
 * to make this phone match it. Shown over
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
                    confirmButton = {},
                    text = {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            DeviceGlyph(false, size = 64)
                            Text(if (p.invited) "${p.deviceName} accepted" else "${p.deviceName} wants to pair", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                            CheckNumber(p.verificationCode)
                            Text("Same number on ${p.deviceName}?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GameButton("Cancel", onClick = { controller.answerPairing(p.requestId, false) }, Modifier.weight(1f), style = GameButtonStyle.SECONDARY)
                                GameButton("Pair", onClick = { controller.answerPairing(p.requestId, true) }, Modifier.weight(1f), icon = LineIcons.Check)
                            }
                        }
                    },
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
                    text = { Text("Pair so this phone and the computer can sync everything with each other? You'll confirm a number on both screens next.") },
                    confirmButton = { GameButton("Pair", onClick = { controller.answerInvite(true) }) },
                    dismissButton = { GameButton("Not now", onClick = { controller.answerInvite(false) }, style = GameButtonStyle.GHOST, compact = true) },
                )
            }
            val request by controller.incomingRequest.collectAsState()
            request?.let { r ->
                GameDialog(
                    onDismissRequest = { controller.answerRequest(false) },
                    title = "Make this phone match ${r.hostName}?",
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("${r.hostName} wants to replace everything here with what's on it: decks, folders, match logs, settings, ruleset and card list.")
                            Text("Decks that are only on this phone are removed. Earlier versions of replaced decks stay in their history.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    confirmButton = { GameButton("Replace", onClick = { controller.answerRequest(true) }) },
                    dismissButton = { GameButton("Not now", onClick = { controller.answerRequest(false) }, style = GameButtonStyle.GHOST, compact = true) },
                )
            }
            verification?.let { v ->
                GameDialog(
                    onDismissRequest = {},
                    title = "Confirm on your computer",
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        properties = app.lockon.platform.plainDialogProperties(usePlatformDefaultWidth = false, dismissible = done),
    ) {
        val scheme = MaterialTheme.colorScheme
        Column(
            Modifier.padding(20.dp).widthIn(max = 460.dp).fillMaxWidth()
                .arrive(rememberArrival())
                .onDialogStage()
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
            Reveal(!asking) {
                TransferBar(
                    fraction = s.saved.toFloat() / s.decks.size.coerceAtLeast(1),
                    tone = if (done) BarTone.DONE else BarTone.WORKING,
                )
            }
            Column(
                Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())
                    .clip(LockOnShapes.tile).background(scheme.onSurface.copy(alpha = 0.05f)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                s.decks.forEachIndexed { i, deck ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(deck.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Reveal(!asking && i < s.saved, sideways = true) {
                            Icon(LineIcons.Check, contentDescription = "Saved", tint = scheme.primary, modifier = Modifier.size(16.dp))
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
            FadeSwap(s.state, Modifier.padding(top = 4.dp), contentAlignment = Alignment.Center) { shownState ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (shownState) {
                        IncomingShare.State.ASKING -> {
                            GameButton("Decline", onClick = { sharing.answer(s.shareId, false) }, style = GameButtonStyle.SECONDARY)
                            GameButton("Accept", onClick = { sharing.answer(s.shareId, true) }, icon = LineIcons.Check)
                        }
                        IncomingShare.State.SAVING -> GameButton("Saving…", onClick = {}, enabled = false)
                        IncomingShare.State.SAVED -> GameButton("Done", onClick = { sharing.dismiss(s.shareId) })
                    }
                }
            }
        }
    }
}

/** The number both screens show while pairing, big enough to compare at a glance. */
@Composable
private fun CheckNumber(code: String) {
    val digits = code.filter { !it.isWhitespace() && it != '-' && it != '–' }
    val half = digits.length / 2
    Row(Modifier.semantics { contentDescription = "Number to check: $code" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        digits.forEachIndexed { i, digit ->
            if (i == half && digits.length > 3) Text("–", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 2.dp))
            Box(Modifier.widthIn(max = 52.dp).weight(1f).height(66.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)), contentAlignment = Alignment.Center) {
                Text("$digit", style = MaterialTheme.typography.headlineMedium.merge(TabularNumbers), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/** A drawn monitor or phone in a circle, so devices read at a glance without an icon pack. */
@Composable
internal fun DeviceGlyph(desktop: Boolean, size: Int = 44) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center,
    ) { Icon(if (desktop) LineIcons.Desktop else LineIcons.Phone, contentDescription = null, Modifier.size((size * 0.5f).dp), tint = MaterialTheme.colorScheme.onSurface) }
}

/** The gap above a device row: between tiles when grouped; on a phone the rows share one panel and pad themselves. */
@Composable
private fun rowTop(): Dp = if (LocalGrouped.current) RowGap else 0.dp
