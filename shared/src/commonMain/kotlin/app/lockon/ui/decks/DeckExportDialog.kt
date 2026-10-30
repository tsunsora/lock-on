package app.lockon.ui.decks
import app.lockon.ui.components.fadeEnd
import app.lockon.ui.components.DeckGlance
import app.lockon.io.ExportOrder
import app.lockon.platform.encodeShareQr
import app.lockon.rules.DeckAnalysis
import app.lockon.ui.components.MiniCurve
import app.lockon.ui.components.QrCodeView
import app.lockon.ui.components.CenteredGlyph
import app.lockon.ui.components.PillChip
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.components.Gold
import app.lockon.ui.components.ProgressGauge
import app.lockon.ui.components.GlassPanel
import app.lockon.ui.components.glass
import app.lockon.ui.components.SkyBackground
import app.lockon.ui.components.PillTabs
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import app.lockon.ui.components.LockOnMark
import app.lockon.ui.theme.TabularNumbers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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
@Suppress("DEPRECATION") // LocalClipboard needs ClipEntry, which differs per platform.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeckExportDialog(
    deck: DeckSnapshot,
    cards: Map<String, Card>,
    ruleset: Ruleset,
    files: FileBridge,
    state: AppState,
    onDismiss: () -> Unit,
    startTab: Int = 0,
) {
    var tab by rememberSaveable { mutableStateOf(startTab.coerceIn(0, 1)) }
    // One order for both formats, so the list and image match.
    var order by rememberSaveable { mutableStateOf(ExportOrder.LEVEL) }
    // The image preview is shrunk to fit; a click opens it large over the dialog.
    var viewing by remember { mutableStateOf(false) }
    val text = remember(deck, cards, order) { DeckListFormat.export(deck, cards, order) }
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
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }
    }
    val tabs = @Composable { PillTabs(listOf("List", "Image"), tab, { tab = it }, fill = true) }
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
    val buttons = @Composable {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tab == 0) {
                GameButton("Copy", onClick = {
                    clipboard.setText(AnnotatedString(text))
                    toast("Deck list copied.")
                }, Modifier.weight(1f), style = GameButtonStyle.SECONDARY)
                GameButton("Save file", onClick = {
                    state.appScope.launch { if (files.saveText("$baseName.txt", "text/plain", text)) toast("Deck list saved.") }
                }, Modifier.weight(1f))
            } else {
                GameButton("Save image", onClick = {
                    state.appScope.launch {
                        // The layer is drawn on the UI thread, so it's captured there; encoding a deck
                        // image takes seconds, so that happens off it and the window keeps responding.
                        val bitmap = layer.toImageBitmap()
                        val png = withContext(Dispatchers.Default) { encodePng(bitmap) }
                        if (files.saveBytes("$baseName.png", "image/png", png)) toast("Deck image saved.")
                    }
                }, Modifier.weight(1f))
            }
        }
    }
    val pane = Modifier.clip(LockOnShapes.tile).background(scheme.surfaceContainerLow)

    // Esc closes the full-size view first, then the dialog.
    Dialog(onDismissRequest = { if (viewing) viewing = false else onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.Center) {
            // The dialog fills the window, so clicks around the panel land on this backdrop. It sits
            // behind the panel rather than around it, so clicks inside (selecting the list) never reach it.
            Spacer(Modifier.fillMaxSize().clickable(interactionSource = null, indication = null, onClick = onDismiss))
            val wide = maxWidth >= 720.dp && maxHeight >= 480.dp
            val viewWidth = minOf(maxWidth - 48.dp, 1080.dp)
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = if (isTwilight()) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest,
                modifier = Modifier.padding(if (wide) 24.dp else 12.dp)
                    .then(if (wide) Modifier else Modifier.fillMaxSize())
                    .glassFrame(MaterialTheme.shapes.extraLarge),
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
                    val height = minOf(maxHeight - 48.dp, 720.dp)
                    Column(Modifier.width(previewWidth + 24.dp + controlsWidth + 44.dp).height(height).padding(start = 24.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        header()
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            if (tab == 0) listText(pane.then(previewBox))
                            else Box(previewBox, contentAlignment = Alignment.TopCenter) {
                                // The whole picture in view at once, as it will be saved; a click shows it large.
                                CaptureAtFixedWidth(
                                    layer,
                                    Modifier.clip(LockOnShapes.tile).clickable(onClickLabel = "View full size") { viewing = true },
                                    fit = true,
                                ) { DeckImage(deck, cards, ruleset, order) }
                            }
                            Column(Modifier.width(controlsWidth).fillMaxHeight().padding(end = 4.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                tabs()
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SectionHeader("Order")
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        orderChips()
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                buttons()
                            }
                        }
                    }
                } else {
                    Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        header()
                        Column(Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            tabs()
                            // One scrolling line of order chips, so the preview keeps the rows. Its edge fades
                            // to show there is more.
                            Row(
                                Modifier.fillMaxWidth().fadeEnd().horizontalScroll(rememberScrollState()).padding(end = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
                            ) { orderChips() }
                            val preview = Modifier.weight(1f).fillMaxWidth()
                            if (tab == 0) listText(pane.then(preview))
                            else Box(pane.then(preview).verticalScroll(rememberScrollState())) {
                                CaptureAtFixedWidth(layer, Modifier.clickable(onClickLabel = "View full size") { viewing = true }) {
                                    DeckImage(deck, cards, ruleset, order)
                                }
                            }
                            buttons()
                        }
                    }
                }
            }
            GameToastHost(toasts, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
            if (viewing) {
                ZoomView(viewWidth, onClose = { viewing = false }) { DeckImage(deck, cards, ruleset, order) }
            }
        }
    }
}

private const val MAX_ZOOM = 4f
private const val CLICK_ZOOM = 2.5f

/**
 * The deck image large over everything, [fitWidth] wide at first. A click on it zooms in there (and
 * back out), as do pinching and Ctrl+scroll; dragging or scrolling moves it, and a click beside it
 * puts it away. It records into its own layer so the saved image is untouched.
 */
@Composable
private fun ZoomView(fitWidth: Dp, onClose: () -> Unit, content: @Composable () -> Unit) {
    val viewLayer = rememberGraphicsLayer()
    var zoom by remember { mutableFloatStateOf(1f) }
    // Where the image's top left sits in the view; kept in bounds by [placed] when drawn.
    var offset by remember { mutableStateOf(Offset(0f, Float.MAX_VALUE)) }
    var base by remember { mutableStateOf(IntSize.Zero) }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f))) {
        val view = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val margin = with(LocalDensity.current) { 24.dp.toPx() }
        val step = with(LocalDensity.current) { 64.dp.toPx() }
        // An axis that fits is centred; one that doesn't can move until its edge meets the margin.
        fun axis(o: Float, length: Float, room: Float) =
            if (length + 2 * margin <= room) (room - length) / 2 else o.coerceIn(room - margin - length, margin)
        fun placed(o: Offset = offset, z: Float = zoom) =
            Offset(axis(o.x, base.width * z, view.width), axis(o.y, base.height * z, view.height))
        // Zooms to [to], keeping the point of the image under [at] where it is.
        fun zoomAt(to: Float, at: Offset) {
            val next = to.coerceIn(1f, MAX_ZOOM)
            val o = placed()
            offset = at - (at - o) * (next / zoom)
            zoom = next
            offset = placed()
        }
        fun onImage(at: Offset) = placed().let { o ->
            at.x in o.x..(o.x + base.width * zoom) && at.y in o.y..(o.y + base.height * zoom)
        }
        val center = Offset(view.width / 2, view.height / 2)
        Box(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { at ->
                        if (!onImage(at)) onClose() else zoomAt(if (zoom > 1f) 1f else CLICK_ZOOM, at)
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        if (gestureZoom != 1f) zoomAt(zoom * gestureZoom, centroid)
                        offset = placed(placed() + pan)
                    }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type != PointerEventType.Scroll) continue
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
                        val o = placed()
                        translationX = o.x; translationY = o.y
                        scaleX = zoom; scaleY = zoom
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .clip(LockOnShapes.tile),
            ) { content() }
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { Row(
                Modifier.align(Alignment.TopEnd).padding(12.dp).glass(LockOnShapes.button, strong = true),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { zoomAt(zoom / 1.5f, center) }, enabled = zoom > 1f) {
                    CenteredGlyph("−", LocalContentColor.current, 24.sp, Modifier.semantics { contentDescription = "Zoom out" })
                }
                Text("${(zoom * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge.merge(TabularNumbers))
                IconButton(onClick = { zoomAt(zoom * 1.5f, center) }, enabled = zoom < MAX_ZOOM) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom in")
                }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close full size") }
            } }
        }
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

/** The picture people share: name, colors and legality, then every card with its count. */
@Composable
private fun DeckImage(deck: DeckSnapshot, cards: Map<String, Card>, ruleset: Ruleset, order: ExportOrder) {
    val scheme = MaterialTheme.colorScheme
    val legal = remember(deck, cards, ruleset) { DeckValidator.validate(deck, cards, ruleset).isLegal }
    val main = deck.entries.filter { it.section == DeckSection.MAIN }
    val colors = main.flatMap { cards[it.cardNumber]?.colors.orEmpty() }.toSet()
    val stats = remember(deck, cards) { DeckAnalysis.stats(deck, cards) }
    // The QR holds the whole deck as a compact list (main and resource), so a scan rebuilds it exactly.
    val qr = remember(deck, cards) { runCatching { encodeShareQr(DeckListFormat.export(deck, cards, ExportOrder.NUMBER, compact = true)) }.getOrNull() }
    // The same sky and glass as the app, so a shared deck looks like Lock On (UX rule V1).
    SkyBackground(Modifier.width(IMAGE_WIDTH)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth().glass(LockOnShapes.panel, strong = true).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                deckCover(deck, cards)?.let { CardImage(it.card, Modifier.width(64.dp), it.printingId) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(deck.name, style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ColorPips(colors)
                        if (legal) Badge("Legal · ${ruleset.name}", scheme.secondaryContainer, scheme.onSecondaryContainer)
                        else Badge("Draft", scheme.surfaceVariant, scheme.onSurfaceVariant)
                    }
                    ProgressGauge("Main deck", deck.count(DeckSection.MAIN), ruleset.mainDeckSize, Modifier.fillMaxWidth(0.6f), compact = true)
                }
            }
            // At a glance: what a reader would otherwise count by hand.
            if (stats.levelCurve.isNotEmpty()) {
                DeckGlance(stats, Modifier.glass(LockOnShapes.tile).padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp))
            }
            GlassPanel(Modifier.fillMaxWidth(), title = "Main deck", readout = "${main.sumOf { it.quantity }}") {
                Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // One continuous grid: the groups keep their order but get no headings, so no row is left half empty.
                    order.groups(main, cards).flatMap { (_, group) -> group }.chunked(CARDS_PER_ROW).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { entry ->
                                // The count sits under the card, so cost and AP/HP stay visible (UX rule V4a).
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    CardImage(cards[entry.cardNumber], Modifier.fillMaxWidth(), entry.printingId)
                                    Text(
                                        "×${entry.quantity}",
                                        style = MaterialTheme.typography.labelLarge.merge(TabularNumbers),
                                        color = scheme.onPrimary,
                                        modifier = Modifier.clip(LockOnShapes.button).background(scheme.primary).padding(horizontal = 12.dp, vertical = 1.dp),
                                    )
                                }
                            }
                            repeat(CARDS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            // Sharing: the QR and how to use it, for someone who only has this picture.
            Row(
                Modifier.fillMaxWidth().glass(LockOnShapes.tile, strong = true).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (qr != null) QrCodeView(qr, Modifier.size(124.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Get this deck", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    HelpStep("1", "Scan the code with a phone camera. It holds the deck list as text.")
                    HelpStep("2", "Copy the text, then in Lock On open Decks › ⋮ › Import and paste it.")
                    HelpStep("3", "Other builders that read \"4 GD01-001\" lines take it too.")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                LockOnMark(size = 18.dp, reticleColor = scheme.onSurface)
                Spacer(Modifier.width(6.dp))
                Text("Built with Lock On", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HelpStep(number: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 1.dp).size(18.dp).clip(LockOnShapes.button).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))) {
            CenteredGlyph(number, MaterialTheme.colorScheme.primary, 11.sp)
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
