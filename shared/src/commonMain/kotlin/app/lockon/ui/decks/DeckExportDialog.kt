package app.lockon.ui.decks
import app.lockon.ui.components.LineIcons
import app.lockon.ui.components.rememberArrival
import app.lockon.ui.components.arrive
import app.lockon.ui.components.LocalNavClearance
import app.lockon.io.ExportOrder
import app.lockon.platform.encodeShareQr
import app.lockon.sync.QrMatrix
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import app.lockon.rules.DeckAnalysis
import app.lockon.ui.components.CardArtwork
import app.lockon.ui.components.LocalArtwork
import app.lockon.ui.theme.cardColor
import app.lockon.model.CardColor
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.layout.ContentScale
import app.lockon.ui.components.QrCodeView
import app.lockon.ui.components.CenteredGlyph
import app.lockon.ui.components.PillChip
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.components.Gold
import app.lockon.ui.components.glass
import app.lockon.ui.components.SkyBackground
import app.lockon.ui.components.PillTabs
import app.lockon.ui.components.Glyphs
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.GameIcons
import app.lockon.ui.components.GameMenuItem
import app.lockon.ui.components.GlassIconButton
import app.lockon.ui.components.LocalReduceMotion
import app.lockon.ui.components.MenuChip
import app.lockon.ui.components.PillSegmented
import app.lockon.ui.components.SlidingPages
import app.lockon.ui.components.dealIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lockon.ui.components.Motion
import app.lockon.ui.components.glassFrame
import app.lockon.ui.components.isTwilight
import app.lockon.ui.components.motion
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import app.lockon.ui.components.GameToastHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.lockon.io.DeckListFormat
import app.lockon.model.Card
import app.lockon.model.CardType
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.Ruleset
import app.lockon.platform.FileBridge
import app.lockon.platform.encodePng
import app.lockon.rules.DeckValidator
import app.lockon.ui.AppState
import app.lockon.ui.components.Badge
import app.lockon.ui.components.CardImage
import app.lockon.ui.components.ColorPips
import app.lockon.ui.components.SectionHeader
import app.lockon.ui.theme.TabularNumbers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import app.lockon.platform.isMobile
import kotlin.math.roundToInt
import kotlin.math.pow
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * Saved deck images are this many pixels wide on every device, whatever its screen density. With
 * five cards a row each card comes out about 375 px wide, over half the 600 px source art, so names,
 * stats and effect text stay readable.
 */
private const val IMAGE_WIDTH_PX = 2160
private val IMAGE_WIDTH = 540.dp
private const val CARDS_PER_ROW = 5

/**
 * Every way out of the app for one deck: the text list and a shareable image.
 * Wide windows put the preview beside the controls and fit the whole image in view; phones get a
 * near full-screen sheet whose preview takes every row the controls leave.
 */
@Composable
fun DeckExportDialog(
    deck: DeckSnapshot,
    cards: Map<String, Card>,
    ruleset: Ruleset,
    files: FileBridge,
    state: AppState,
    onDismiss: () -> Unit,
    startTab: Int = 0,
    qrTab: Boolean = isMobile,
) = DeckExport(deck, cards, ruleset, files, state, onDismiss, startTab, qrTab = qrTab)

/**
 * The export as a page of the phone deck editor, laid out like the dialog's phone sheet but in
 * place, with room left at the foot for the floating navigation. [qrTab] adds a third format, the
 * deck as a QR code to show another phone; it is on for phones only.
 */
@Composable
fun DeckExportPanel(
    deck: DeckSnapshot,
    cards: Map<String, Card>,
    ruleset: Ruleset,
    files: FileBridge,
    state: AppState,
    modifier: Modifier = Modifier,
    qrTab: Boolean = isMobile,
) = DeckExport(deck, cards, ruleset, files, state, onDismiss = null, modifier = modifier, qrTab = qrTab)

/** The export's body: in a dialog when [onDismiss] is given, otherwise as a page in [modifier]. */
@Suppress("DEPRECATION") // LocalClipboard needs ClipEntry, which differs per platform.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeckExport(
    deck: DeckSnapshot,
    cards: Map<String, Card>,
    ruleset: Ruleset,
    files: FileBridge,
    state: AppState,
    onDismiss: (() -> Unit)?,
    startTab: Int = 0,
    modifier: Modifier = Modifier,
    qrTab: Boolean = false,
) {
    var tab by rememberSaveable { mutableStateOf(startTab.coerceIn(0, if (qrTab) 2 else 1)) }
    val formats = if (qrTab) listOf("List", "Image", "QR code") else listOf("List", "Image")
    // One order for both formats, so the list and image match.
    var order by rememberSaveable { mutableStateOf(ExportOrder.LEVEL) }
    // The image preview is shrunk to fit; a short hold opens it large over the dialog.
    var viewing by remember { mutableStateOf(false) }
    // Where the preview is, so the large view grows out of it and shrinks back into it.
    val viewSource = remember { ViewSource() }
    val text = remember(deck, cards, order) { DeckListFormat.export(deck, cards, order) }
    // What the QR tab's code carries, the same as the code on the image.
    val qrText = remember(deck, cards) { deckQrText(deck, cards) }
    val qr = remember(qrText, qrTab) { if (qrTab) deckQr(qrText) else null }
    val baseName = deck.name.filter { it.isLetterOrDigit() || it in " -_" }.trim().ifBlank { "deck" }
    val clipboard = LocalClipboardManager.current
    val layer = rememberGraphicsLayer()
    val scheme = MaterialTheme.colorScheme
    // The app's snackbars draw in the window under this dialog, so it shows its own.
    val toasts = remember { SnackbarHostState() }
    val toastScope = rememberCoroutineScope()
    fun toast(message: String) {
        toastScope.launch {
            toasts.currentSnackbarData?.dismiss()
            toasts.showSnackbar(message)
        }
    }

    val header = @Composable {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(deck.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            if (onDismiss != null) GameButton("Close", onClick = onDismiss, style = GameButtonStyle.SECONDARY, compact = true)
        }
    }
    val tabs = @Composable { PillTabs(formats, tab, { tab = it }, fill = true, soft = true) }
    val orderChips = @Composable {
        ExportOrder.entries.forEach { option ->
            PillChip(selected = order == option, onClick = { order = option }, label = { Text(option.label) })
        }
    }
    val listText = @Composable { modifier: Modifier ->
        SelectionContainer(modifier) {
            Text(
                text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            )
        }
    }
    // Phones confirm a copy in the button itself, so the phone layout doesn't add a toast of its own.
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1600); copied = false } }
    // Encoding takes seconds, so the button says so, and a second tap can't start another.
    var saving by remember { mutableStateOf(false) }
    val buttons = @Composable { phone: Boolean ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tab == 0) {
                GameButton(if (phone && copied) "Copied" else "Copy", onClick = {
                    clipboard.setText(AnnotatedString(text))
                    if (phone) copied = true else toast("Deck list copied.")
                }, Modifier.weight(1f), style = GameButtonStyle.SECONDARY, icon = if (phone && copied) LineIcons.Check else null)
                GameButton("Save file", onClick = {
                    state.appScope.launch { if (files.saveText("$baseName.txt", "text/plain", text)) toast("Deck list saved.") }
                }, Modifier.weight(1f))
            } else if (tab == 2) {
                GameButton(if (phone && copied) "Copied" else "Copy text", onClick = {
                    clipboard.setText(AnnotatedString(qrText))
                    if (phone) copied = true else toast("Deck list copied.")
                }, Modifier.weight(1f), style = GameButtonStyle.SECONDARY, icon = if (phone && copied) LineIcons.Check else null)
                GameButton(if (saving) "Saving…" else "Save QR", onClick = {
                    val code = qr ?: return@GameButton
                    saving = true
                    state.appScope.launch {
                        try {
                            val png = withContext(Dispatchers.Default) { encodePng(qrBitmap(code)) }
                            saving = false
                            if (files.saveBytes("$baseName QR.png", "image/png", png)) toast("QR code saved.")
                        } finally { saving = false }
                    }
                }, Modifier.weight(1f), enabled = !saving && qr != null)
            } else {
                GameButton(if (saving) "Saving…" else "Save PNG", onClick = {
                    saving = true
                    state.appScope.launch {
                        try {
                            // The layer is drawn on the UI thread, so it's captured there; encoding a deck
                            // image takes seconds, so that happens off it and the window keeps responding.
                            val bitmap = layer.toImageBitmap()
                            val png = withContext(Dispatchers.Default) { encodePng(bitmap) }
                            saving = false
                            if (files.saveBytes("$baseName.png", "image/png", png)) toast("Deck image saved.")
                        } finally { saving = false }
                    }
                }, Modifier.weight(1f), enabled = !saving, icon = LineIcons.Save)
            }
        }
    }
    val pane = Modifier.clip(LockOnShapes.tile).background(scheme.surfaceContainerLow)
    // The phone layout: the format switch, one Order chip, the preview in every row they leave, then the buttons.
    val phone = @Composable { modifier: Modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PillSegmented(formats, tab, { tab = it }, Modifier.fillMaxWidth())
            // The orders fold into one chip, like the card list's Colors and Type on phones (UX rule N4),
            // so none is cut off and the preview keeps the row.
            // The QR's list always goes by card number, so it has no order to pick.
            if (tab != 2) MenuChip(selected = false, label = { Text("Order: ${order.label}") }) { close ->
                ExportOrder.entries.forEach { option ->
                    GameMenuItem(option.label, onClick = { order = option; close() }, selected = order == option)
                }
            }
            // Switching formats slides the preview the way the switch's pill went.
            SlidingPages(tab, Modifier.weight(1f).fillMaxWidth()) { page ->
                val glassPane = Modifier.fillMaxSize().glass(LockOnShapes.tile)
                if (page == 0) DeckListPreview(deck, cards, order, glassPane)
                else if (page == 2) QrPage(qr, glassPane)
                else Box(glassPane.clip(LockOnShapes.tile)) {
                    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        CaptureAtFixedWidth(layer, Modifier.viewSource(viewSource).holdToView { viewing = true }) {
                            DeckImage(deck, cards, ruleset, order)
                        }
                    }
                    // Holding the image opens it too; this shows that it opens.
                    GlassIconButton(GameIcons.Expand, "View full size", onClick = { viewing = true },
                        Modifier.align(Alignment.BottomEnd).padding(12.dp))
                }
            }
            buttons(true)
        }
    }

    // A page of the editor: the deck's name is in the banner above, so it starts at the tabs.
    if (onDismiss == null) {
        BoxWithConstraints(modifier) {
            phone(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp + LocalNavClearance.current))
            GameToastHost(toasts, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp + LocalNavClearance.current))
            // Over the whole window, so the deck's banner and the editor's bar don't sit on it.
            ZoomView(viewing, viewSource, onClose = { viewing = false }, overWindow = true) { DeckImage(deck, cards, ruleset, order) }
        }
        return
    }

    // Esc closes the full-size view first, then the dialog.
    val arrival = rememberArrival()
    app.lockon.ui.components.BlurBehind()
    Dialog(onDismissRequest = { if (viewing) viewing = false else onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.Center) {
            // The dialog fills the window, so clicks around the panel land on this backdrop. It sits
            // behind the panel rather than around it, so clicks inside (selecting the list) never reach it.
            Spacer(Modifier.fillMaxSize().clickable(interactionSource = null, indication = null, onClick = onDismiss))
            val wide = maxWidth >= 720.dp && maxHeight >= 480.dp
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = if (isTwilight()) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest,
                modifier = Modifier.padding(if (wide) 24.dp else 12.dp)
                    .then(if (wide) Modifier else Modifier.fillMaxSize())
                    .arrive(arrival).glassFrame(MaterialTheme.shapes.extraLarge),
            ) {
                if (wide) {
                    // The deck's name across the top, then the preview at full height on the left and
                    // the controls in a column on the right, with the save buttons at its foot.
                    val controlsWidth = 300.dp
                    val previewMax = (maxWidth - 48.dp - 44.dp - 24.dp - controlsWidth).coerceAtLeast(240.dp)
                    // Every tab's preview gets the same box, so switching tabs never resizes the dialog.
                    val previewWidth = minOf(440.dp, previewMax)
                    val previewBox = Modifier.width(previewWidth).fillMaxHeight()
                    // Tall windows don't stretch it: a deck list fits well within this, and the panes scroll.
                    val height = minOf(maxHeight - 48.dp, 640.dp)
                    Column(Modifier.width(previewWidth + 24.dp + controlsWidth + 44.dp).height(height).padding(start = 24.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Export", Modifier.align(Alignment.CenterStart), style = MaterialTheme.typography.titleLarge)
                            PillTabs(formats, tab, { tab = it }, Modifier.width(IntrinsicSize.Max), soft = true, icons = listOf(LineIcons.Text, LineIcons.Image, LineIcons.Qr).take(formats.size))
                        }
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            if (tab == 0) listText(pane.then(previewBox))
                            else if (tab == 2) QrPage(qr, pane.then(previewBox))
                            else Box(previewBox, contentAlignment = Alignment.TopCenter) {
                                // The whole picture in view at once, as it will be saved; a short hold shows it large.
                                CaptureAtFixedWidth(
                                    layer,
                                    Modifier.clip(LockOnShapes.tile).viewSource(viewSource).holdToView { viewing = true },
                                    fit = true,
                                ) { DeckImage(deck, cards, ruleset, order) }
                            }
                            Column(Modifier.width(controlsWidth).fillMaxHeight().padding(end = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (tab != 2) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SectionHeader("Order")
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        orderChips()
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                if (tab == 0) {
                                    GameButton("Save file", onClick = {
                                        state.appScope.launch { if (files.saveText("$baseName.txt", "text/plain", text)) toast("Deck list saved.") }
                                    }, Modifier.fillMaxWidth(), icon = LineIcons.Save)
                                    GameButton("Copy", onClick = {
                                        clipboard.setText(AnnotatedString(text))
                                        toast("Deck list copied.")
                                    }, Modifier.fillMaxWidth(), style = GameButtonStyle.SECONDARY, icon = LineIcons.Copy)
                                } else Box(Modifier.fillMaxWidth().height(44.dp)) { buttons(false) }
                                if (onDismiss != null) GameButton("Close", onClick = onDismiss, Modifier.fillMaxWidth(), style = GameButtonStyle.SECONDARY)
                            }
                        }
                    }
                } else {
                    // The sky behind the sheet, as on the editor's Export page, so the preview's glass reads.
                    SkyBackground(Modifier.fillMaxSize()) {
                        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            header()
                            phone(Modifier.weight(1f).padding(end = 8.dp))
                        }
                    }
                }
            }
            GameToastHost(toasts, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
            ZoomView(viewing, viewSource, onClose = { viewing = false }) { DeckImage(deck, cards, ruleset, order) }
        }
    }
}

/** How long the preview is held before it opens large: shorter than a long press, but not a tap. */
private const val HOLD_TO_VIEW_MS = 250L

/**
 * Runs [onView] once a press on this has been held for [HOLD_TO_VIEW_MS] without lifting. A tap does
 * nothing, and a press that turns into a scroll is let go, so the phone preview still scrolls. While
 * held, a soft teal light grows under the finger, so the hold reads as doing something.
 */
private fun Modifier.holdToView(onView: () -> Unit): Modifier = composed {
    val haptics = LocalHapticFeedback.current
    val view by rememberUpdatedState(onView)
    val reduce = LocalReduceMotion.current
    val accent = MaterialTheme.colorScheme.primary
    val glow = remember { Animatable(0f) }
    var at by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    semantics { onLongClick("View full size") { view(); true } }
        .drawWithContent {
            drawContent()
            val g = glow.value
            if (g > 0f) {
                val radius = 140.dp.toPx() * (0.4f + 0.6f * g)
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.32f * g), accent.copy(alpha = 0f)), at, radius), radius, at)
            }
        }
        .pointerInput(reduce) {
            awaitEachGesture {
                at = awaitFirstDown(requireUnconsumed = false).position
                if (!reduce) scope.launch { glow.animateTo(1f, tween(HOLD_TO_VIEW_MS.toInt(), easing = Motion.emphasizedDecelerate)) }
                // Lifting, or the press being taken by a scroll, ends it early; still down at the timeout opens.
                val ended = withTimeoutOrNull(HOLD_TO_VIEW_MS) { waitForUpOrCancellation(); true }
                if (ended == null) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    view()
                }
                scope.launch { glow.animateTo(0f, tween(Motion.MEDIUM)) }
            }
        }
}

/**
 * The phone's List preview: the same list as the saved text, set like the rest of the app. Sections
 * get headers with their counts, and each card its count in a teal pill, its name and its number.
 * Rows deal in when it opens or the order changes, so a new order is seen to land. It stays selectable.
 */
@Composable
private fun DeckListPreview(deck: DeckSnapshot, cards: Map<String, Card>, order: ExportOrder, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    // Rows past the first screenful start together, so a long list still lands quickly.
    var row = 0
    fun next() = minOf(row++, 14)
    SelectionContainer(modifier) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 16.dp)) {
            for (section in DeckSection.entries) {
                val entries = deck.entries.filter { it.section == section }
                if (entries.isEmpty()) continue
                SectionHeader(
                    section.label, Modifier.padding(top = if (row == 0) 0.dp else 16.dp, bottom = 4.dp).dealIn(order, next()),
                    readout = "${entries.sumOf { it.quantity }}",
                )
                for ((label, group) in order.groups(entries, cards)) {
                    // The main deck is long, so its types get a quiet line each, as in the saved text.
                    if (section == DeckSection.MAIN) Text(
                        "$label · ${group.sumOf { it.quantity }}", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 2.dp).dealIn(order, next()),
                    )
                    group.forEach { entry -> DeckListRow(entry, cards[entry.cardNumber], Modifier.dealIn(order, next())) }
                }
            }
        }
    }
}

@Composable
private fun DeckListRow(entry: DeckEntry, card: Card?, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "${entry.quantity}", style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), color = scheme.primary, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 32.dp).clip(LockOnShapes.button).background(scheme.primary.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Text(card?.name ?: entry.cardNumber, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (card != null) Text(entry.cardNumber, style = MaterialTheme.typography.labelSmall.merge(TabularNumbers), color = scheme.onSurfaceVariant)
    }
}

private const val MAX_ZOOM = 4f
private const val CLICK_ZOOM = 2.5f

/** Where the preview being viewed sits on screen: its whole box, and the part of it not scrolled out of sight. */
private class ViewSource {
    var bounds = Rect.Zero
    var shown = Rect.Zero
}

private fun Modifier.viewSource(source: ViewSource): Modifier = onGloballyPositioned {
    val onScreen = it.positionOnScreen()
    source.bounds = Rect(onScreen, it.size.toSize())
    source.shown = it.boundsInWindow().translate(onScreen - it.positionInWindow())
}

/** Puts a popup at the window's top left, so one that fills the window covers all of it. */
private object WindowCorner : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize) =
        IntOffset.Zero
}

/**
 * The deck image large over everything while [visible]. It grows out of the preview at [source] as
 * the backdrop darkens, and shrinks back into it when put away. [overWindow] lifts it over the whole
 * window, for the editor's Export page, which sits under the deck's banner and the editor's bar.
 */
@Composable
private fun ZoomView(visible: Boolean, source: ViewSource, onClose: () -> Unit, overWindow: Boolean = false, content: @Composable () -> Unit) {
    val reduce = LocalReduceMotion.current
    val shown = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        // Opening eases out of the preview; putting it away is a touch quicker, as things leaving are.
        shown.animateTo(
            if (visible) 1f else 0f,
            when {
                reduce -> tween(Motion.SHORT)
                visible -> tween(Motion.LONG + 80, easing = Motion.emphasized)
                else -> tween(Motion.LONG, easing = Motion.emphasized)
            },
        )
    }
    val open by remember { derivedStateOf { shown.value > 0f } }
    if (!visible && !open) return
    val frame = @Composable {
        ZoomFrame(visible, { shown.value }, reduce, source, onClose, content)
    }
    if (overWindow) Popup(WindowCorner, onDismissRequest = onClose, properties = PopupProperties(focusable = true)) { frame() }
    else frame()
}

/**
 * The view itself, at [progress] of the way out of the preview. Its image is as wide as the window
 * allows; on a computer it also shrinks until its whole height fits, and that is 100%. A click on it
 * zooms in there (and back out), as do pinching and Ctrl+scroll; dragging or scrolling moves it, and
 * a click beside it puts it away. Narrow windows keep their controls in a strip above the image and
 * zoom on a double tap, so nothing sits over the picture. It records into its own layer so the saved
 * image is untouched.
 */
@Composable
private fun ZoomFrame(
    visible: Boolean,
    progress: () -> Float,
    reduce: Boolean,
    source: ViewSource,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    val viewLayer = rememberGraphicsLayer()
    val live by rememberUpdatedState(visible)
    var zoom by remember { mutableFloatStateOf(1f) }
    // Where the image's top left sits in the view; kept in bounds by [placed] when drawn.
    var offset by remember { mutableStateOf(Offset(0f, Float.MAX_VALUE)) }
    var base by remember { mutableStateOf(IntSize.Zero) }
    // The view's own place on screen, to find the preview in it.
    var origin by remember { mutableStateOf(Offset.Zero) }
    var topBar by remember { mutableStateOf(0) }
    var bottomInset by remember { mutableStateOf(0) }
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionOnScreen() }) {
        val compact = maxWidth < 600.dp
        val density = LocalDensity.current
        val view = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val margin = with(density) { (if (compact) 12.dp else 24.dp).toPx() }
        val step = with(density) { 64.dp.toPx() }
        // On a narrow window the image starts below the strip with the close button and ends above the system bar.
        val top = if (compact) topBar.toFloat() else margin
        val bottom = if (compact) bottomInset + margin else margin
        val fitWidth = if (compact) maxWidth - 24.dp else minOf(maxWidth - 48.dp, 1080.dp)
        // Phones scroll a tall image at full width; a computer window shows the whole of it at first.
        // [zoom] is relative to this, and clicking in or zooming all the way still reaches the same real size.
        val fit = if (compact || isMobile || base.width == 0 || base.height == 0) 1f
            else minOf(1f, (view.width - 2 * margin) / base.width, (view.height - top - bottom) / base.height).coerceAtLeast(0.05f)
        // An axis that fits is centred between its margins; one that doesn't can move until its edge meets one.
        fun axis(o: Float, length: Float, room: Float, lo: Float, hi: Float) =
            if (length + lo + hi <= room) lo + (room - lo - hi - length) / 2 else o.coerceIn(room - hi - length, lo)
        fun placed(o: Offset = offset, z: Float = zoom) =
            Offset(axis(o.x, base.width * z * fit, view.width, margin, margin), axis(o.y, base.height * z * fit, view.height, top, bottom))
        // Zooms to [to], keeping the point of the image under [at] where it is.
        fun zoomAt(to: Float, at: Offset) {
            val next = to.coerceIn(1f, maxOf(1f, MAX_ZOOM / fit))
            val o = placed()
            offset = at - (at - o) * (next / zoom)
            zoom = next
            offset = placed()
        }
        fun onImage(at: Offset) = placed().let { o ->
            at.x in o.x..(o.x + base.width * zoom * fit) && at.y in o.y..(o.y + base.height * zoom * fit)
        }
        val center = Offset(view.width / 2, (view.height + top - bottom) / 2)
        // How far along the move out of the preview is; with less motion it only fades.
        fun moved() = if (reduce) 1f else progress()
        // Controls come in over the last part of the move, once the image is nearly in place.
        val controls = Modifier.graphicsLayer { alpha = ((progress() - 0.55f) / 0.45f).coerceIn(0f, 1f) }

        Box(Modifier.fillMaxSize().graphicsLayer { alpha = progress() }.background(Color.Black.copy(alpha = if (compact) 0.88f else 0.72f)))
        Box(
            Modifier.fillMaxSize()
                // The part of the preview that was in sight opens out to the whole view.
                .drawWithContent {
                    val p = moved()
                    val from = source.shown.translate(-origin)
                    if (p >= 1f || from.isEmpty) drawContent()
                    else {
                        val clip = lerp(from, Rect(Offset.Zero, size), p)
                        clipRect(clip.left, clip.top, clip.right, clip.bottom) { this@drawWithContent.drawContent() }
                    }
                }
                .pointerInput(compact) {
                    if (compact) detectTapGestures(
                        onDoubleTap = { at -> if (live && onImage(at)) zoomAt(if (zoom > 1f) 1f else CLICK_ZOOM, at) },
                        onTap = { at -> if (live && !onImage(at)) onClose() },
                    ) else detectTapGestures { at ->
                        if (!live) return@detectTapGestures
                        if (!onImage(at)) onClose() else zoomAt(if (zoom > 1f) 1f else CLICK_ZOOM / fit, at)
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        if (!live) return@detectTransformGestures
                        if (gestureZoom != 1f) zoomAt(zoom * gestureZoom, centroid)
                        offset = placed(placed() + pan)
                    }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type != PointerEventType.Scroll || !live) continue
                            val change = event.changes.first()
                            val delta = change.scrollDelta
                            if (event.keyboardModifiers.isCtrlPressed) zoomAt(zoom * 1.2f.pow(-delta.y), change.position)
                            else offset = placed(placed() - delta * step)
                            change.consume()
                        }
                    }
                },
        ) {
            CaptureAtFixedWidth(
                viewLayer,
                // Its full height, however tall, rather than squeezed into the window.
                Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).width(fitWidth).onSizeChanged { base = it }
                    .graphicsLayer {
                        val p = moved()
                        val rest = placed()
                        val restScale = zoom * fit
                        val from = source.bounds.translate(-origin)
                        // From the preview's place and size to its own, along the same path for both.
                        val (start, startScale) = if (from.isEmpty || base.width == 0) rest to restScale
                            else from.topLeft to from.width / base.width
                        val at = lerp(start, rest, p)
                        val scale = startScale + (restScale - startScale) * p
                        translationX = at.x; translationY = at.y
                        scaleX = scale; scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                        alpha = if (base == IntSize.Zero) 0f else if (reduce) progress() else 1f
                    }
                    .clip(LockOnShapes.tile),
            ) { content() }
        }
        if (compact) {
            // A strip above the image: a quiet hint for the gestures, and the way out.
            Row(
                controls.fillMaxWidth().align(Alignment.TopCenter)
                    .onSizeChanged { topBar = it.height }
                    // The image scrolls up under the strip, so it fades to dark behind the hint and button.
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Black.copy(alpha = 0f))))
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (zoom > 1f) "Double-tap to fit" else "Pinch or double-tap to zoom",
                    style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.weight(1f),
                )
                GlassIconButton(LineIcons.X, "Close full size", onClick = onClose)
            }
            Spacer(Modifier.align(Alignment.BottomCenter).windowInsetsBottomHeight(WindowInsets.safeDrawing).onSizeChanged { bottomInset = it.height })
        } else CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { Row(
            controls.align(Alignment.TopEnd).padding(12.dp).glass(LockOnShapes.button, strong = true),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { zoomAt(zoom / 1.5f, center) }, enabled = zoom > 1f) {
                CenteredGlyph("−", LocalContentColor.current, 24.sp, Modifier.semantics { contentDescription = "Zoom out" })
            }
            Text("${(zoom * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge.merge(TabularNumbers))
            IconButton(onClick = { zoomAt(zoom * 1.5f, center) }, enabled = zoom < MAX_ZOOM / fit) {
                Icon(LineIcons.Plus, contentDescription = "Zoom in")
            }
            IconButton(onClick = onClose) { Icon(LineIcons.X, contentDescription = "Close full size") }
        } }
    }
}

/**
 * Lays [content] out at [IMAGE_WIDTH] with a density that makes it exactly [IMAGE_WIDTH_PX] wide,
 * records it into [layer] at that size, and shows it scaled to the width it is given. With [fit]
 * it shrinks further until its whole height fits too, and never grows past [IMAGE_WIDTH].
 */
@Composable
private fun CaptureAtFixedWidth(layer: GraphicsLayer, modifier: Modifier = Modifier, fit: Boolean = false, content: @Composable () -> Unit) {
    Box(
        modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(Constraints.fixedWidth(IMAGE_WIDTH_PX))
            var scale = constraints.maxWidth.toFloat() / IMAGE_WIDTH_PX
            if (fit) {
                if (constraints.hasBoundedHeight) scale = minOf(scale, constraints.maxHeight.toFloat() / placeable.height)
                scale = minOf(scale, IMAGE_WIDTH.toPx() / IMAGE_WIDTH_PX)
            }
            layout((placeable.width * scale).roundToInt(), (placeable.height * scale).roundToInt()) {
                placeable.placeWithLayer(0, 0) {
                    scaleX = scale; scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
            }
        },
    ) {
        CompositionLocalProvider(LocalDensity provides Density(IMAGE_WIDTH_PX / IMAGE_WIDTH.value, fontScale = 1f)) {
            Box(Modifier.drawWithContent { layer.record { this@drawWithContent.drawContent() }; drawLayer(layer) }) { content() }
        }
    }
}

/**
 * The picture people share, each fact said once: one panel over the cover's art holds the name,
 * colors, count, legality and card types, with the QR beside them; then every card with its count
 * on its corner.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeckImage(deck: DeckSnapshot, cards: Map<String, Card>, ruleset: Ruleset, order: ExportOrder) {
    val scheme = MaterialTheme.colorScheme
    val legal = remember(deck, cards, ruleset) { DeckValidator.validate(deck, cards, ruleset).isLegal }
    val main = deck.entries.filter { it.section == DeckSection.MAIN }
    val count = main.sumOf { it.quantity }
    val colors = main.flatMap { cards[it.cardNumber]?.colors.orEmpty() }.toSet()
    val stats = remember(deck, cards) { DeckAnalysis.stats(deck, cards) }
    val qr = remember(deck, cards) { deckQr(deckQrText(deck, cards)) }
    // The same sky and glass as the app, so a shared deck looks like Lock On (UX rule V1).
    SkyBackground(Modifier.width(IMAGE_WIDTH)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth().glass(LockOnShapes.panel, strong = true).clip(LockOnShapes.panel)) {
                CoverBackdrop(deck, cards, colors.minByOrNull { it.ordinal }, Modifier.matchParentSize())
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(deck.name, style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val middle = Modifier.align(Alignment.CenterVertically)
                            ColorPips(colors, middle)
                            // The count needs no gauge: a full deck just says so, a short one shows what it lacks.
                            Text(
                                if (count == ruleset.mainDeckSize) "$count cards" else "$count / ${ruleset.mainDeckSize} cards",
                                style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), color = scheme.onSurfaceVariant, modifier = middle,
                            )
                            if (legal) Badge("Legal · ${ruleset.name}", scheme.secondaryContainer, scheme.onSecondaryContainer, middle)
                            else Badge("Draft", scheme.surfaceVariant, scheme.onSurfaceVariant, middle)
                        }
                        TypeCounts(stats)
                    }
                    if (qr != null) QrCodeView(qr, Modifier.size(100.dp))
                }
            }
            // The count is in the header, so the grid needs no title row of its own.
            Column(Modifier.fillMaxWidth().glass().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // One continuous grid: the groups keep their order but get no headings, so no row is left half empty.
                order.groups(main, cards).flatMap { (_, group) -> group }.chunked(CARDS_PER_ROW).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { entry ->
                            // The count sits near the top-right corner, just under the card's number and rarity, so
                            // those, the cost and AP/HP all stay visible (UX rule V4a).
                            Box(Modifier.weight(1f)) {
                                CardImage(cards[entry.cardNumber], Modifier.fillMaxWidth(), entry.printingId)
                                Text(
                                    "×${entry.quantity}",
                                    style = MaterialTheme.typography.labelLarge.merge(TabularNumbers),
                                    color = scheme.onPrimary,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 3.dp).clip(LockOnShapes.button).background(scheme.primary)
                                        .padding(horizontal = 7.dp, vertical = 1.dp),
                                )
                            }
                        }
                        repeat(CARDS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/**
 * The cover card's art across the whole header, faint and tinted toward the panel on the left, where
 * the text sits, and toward the deck's lead color on the right, as on the deck boxes (UX rule V4a).
 */
@Composable
private fun CoverBackdrop(deck: DeckSnapshot, cards: Map<String, Card>, lead: CardColor?, modifier: Modifier = Modifier) {
    val cover = deckCover(deck, cards) ?: return
    val art = LocalArtwork.current.urlFor(cover.card.cardNumber, cover.printingId) ?: return
    val scheme = MaterialTheme.colorScheme
    val base = if (isTwilight()) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest
    val tint = cardColor(lead)
    Box(modifier) {
        CardArtwork(art, null, Modifier.fillMaxSize(), ContentScale.Crop, BiasAlignment(0f, -0.45f)) {}
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(tint.copy(alpha = 0.12f), tint.copy(alpha = 0.4f)))))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to base.copy(alpha = 0.86f), 0.45f to base.copy(alpha = 0.7f), 1f to base.copy(alpha = 0.35f))))
    }
}

/**
 * The deck's make-up in small type under its name: each card type a number and its word. Types the
 * deck has none of are left out.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeCounts(stats: app.lockon.rules.DeckStats, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val types = listOf(
        "Units" to CardType.UNIT,
        "Pilots" to CardType.PILOT,
        "Commands" to CardType.COMMAND,
        "Bases" to CardType.BASE,
    ).map { (label, type) -> label to (stats.typeCounts[type] ?: 0) }.filter { it.second > 0 }
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        types.forEach { (label, n) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("$n", style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), color = scheme.onSurface)
                Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 1, softWrap = false)
            }
        }
    }
}

/** The QR's text: the whole deck as a compact list (main and resource), so a scan rebuilds it exactly. */
private fun deckQrText(deck: DeckSnapshot, cards: Map<String, Card>) = DeckListFormat.export(deck, cards, ExportOrder.NUMBER, compact = true)

/** The QR for [text], or null when it is too long for one. */
private fun deckQr(text: String): QrMatrix? = runCatching { encodeShareQr(text) }.getOrNull()

private const val QR_CELL_PX = 16

/** [matrix] as a picture to save: black on white with the standard quiet zone, [QR_CELL_PX] pixels a module. */
private fun qrBitmap(matrix: QrMatrix): ImageBitmap {
    val quiet = 4
    val side = (matrix.size + 2 * quiet) * QR_CELL_PX
    val bitmap = ImageBitmap(side, side)
    val canvas = Canvas(bitmap)
    canvas.drawRect(0f, 0f, side.toFloat(), side.toFloat(), Paint().apply { color = Color.White })
    val black = Paint().apply { color = Color.Black }
    for (y in 0 until matrix.size) for (x in 0 until matrix.size) {
        if (!matrix[x, y]) continue
        val left = ((x + quiet) * QR_CELL_PX).toFloat()
        val top = ((y + quiet) * QR_CELL_PX).toFloat()
        canvas.drawRect(left, top, left + QR_CELL_PX, top + QR_CELL_PX, black)
    }
    return bitmap
}

/**
 * The QR tab: the code as large as its pane allows, for another phone to scan across the table, with
 * what a scan gives under it. The deck's name is already over the tabs.
 */
@Composable
private fun QrPage(qr: QrMatrix?, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(modifier) {
        // Room is left for the lines above and below it.
        val side = minOf(maxWidth - 40.dp, maxHeight - 170.dp, 360.dp).coerceAtLeast(160.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            if (qr != null) {
                QrCodeView(qr, Modifier.size(side))
                Text(
                    "Scan with a phone camera to get the deck list, then paste it into Decks › ⋮ › Import.",
                    style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 360.dp),
                )
            } else Text(
                "This deck is too long for a QR code. Share the list or the image instead.",
                style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center,
            )
        }
    }
}
