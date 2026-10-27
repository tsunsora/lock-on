package app.lockon.ui.decks
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
import app.lockon.ui.components.Sparkle
import app.lockon.ui.components.ProgressGauge
import app.lockon.ui.components.GlassPanel
import app.lockon.ui.components.glass
import app.lockon.ui.components.SkyBackground
import app.lockon.ui.components.PillTabs
import app.lockon.ui.components.GameDialog
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import app.lockon.io.DeckSheetFormat
import app.lockon.model.Card
import app.lockon.model.CardType
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.Ruleset
import app.lockon.platform.FileBridge
import app.lockon.platform.encodePng
import app.lockon.platform.formatDate
import app.lockon.rules.DeckValidator
import app.lockon.ui.AppState
import app.lockon.ui.components.Badge
import app.lockon.ui.components.CardImage
import app.lockon.ui.components.ColorPips
import app.lockon.ui.components.SectionHeader
import app.lockon.ui.components.LockOnMark
import app.lockon.ui.theme.TabularNumbers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Clock

/**
 * Saved deck images are this many pixels wide on every device, whatever its screen density. With
 * five cards a row each card comes out about 375 px wide, over half the 600 px source art, so names,
 * stats and effect text stay readable.
 */
private const val IMAGE_WIDTH_PX = 2160
private val IMAGE_WIDTH = 540.dp
private const val CARDS_PER_ROW = 5

/** Every way out of the app for one deck: the text list, a shareable image, and a printable sheet. */
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
    var tab by rememberSaveable { mutableStateOf(startTab) }
    // One order for every format, so the list, image and sheet match.
    var order by rememberSaveable { mutableStateOf(ExportOrder.LEVEL) }
    val text = remember(deck, cards, order) { DeckListFormat.export(deck, cards, order) }
    val baseName = deck.name.filter { it.isLetterOrDigit() || it in " -_" }.trim().ifBlank { "deck" }
    val clipboard = LocalClipboardManager.current
    val layer = rememberGraphicsLayer()
    GameDialog(
        onDismissRequest = onDismiss,
        title = deck.name,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PillTabs(listOf("List", "Image", "Print sheet"), tab, { tab = it }, fill = true)
                // What each format is for, so the choice is easy.
                Text(
                    when (tab) {
                        0 -> "Plain text for chats, forums and other deck builders. Lock On reads it back under Decks › Import."
                        1 -> "A picture to post or send. It shows the main deck, its curve and a QR code that holds the full list."
                        else -> "For events that ask for a written deck list."
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Order", style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.CenterVertically).padding(end = 2.dp))
                    ExportOrder.entries.forEach { option ->
                        PillChip(selected = order == option, onClick = { order = option }, label = { Text(option.label) })
                    }
                }
                when (tab) {
                    0 -> SelectionContainer {
                        Text(
                            text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                        )
                    }
                    1 -> Box(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                        CaptureAtFixedWidth(layer) { DeckImage(deck, cards, ruleset, order) }
                    }
                    else -> Text(
                        "A deck registration sheet with blanks for your name and event, then every card in the main and " +
                            "resource decks with its number and count. It saves as a web page: open it in any browser and print it, " +
                            "or print to PDF.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tab) {
                    0 -> {
                        GameButton("Copy", onClick = {
                            clipboard.setText(AnnotatedString(text))
                            state.notify("Deck list copied.")
                        }, style = GameButtonStyle.SECONDARY, compact = true)
                        GameButton("Save file", onClick = {
                            state.appScope.launch { if (files.saveText("$baseName.txt", "text/plain", text)) state.notify("Deck list saved.") }
                        }, compact = true)
                    }
                    1 -> GameButton("Save image", onClick = {
                        state.appScope.launch {
                            val png = encodePng(layer.toImageBitmap())
                            if (files.saveBytes("$baseName.png", "image/png", png)) state.notify("Deck image saved.")
                        }
                    }, compact = true)
                    else -> GameButton("Save sheet", onClick = {
                        val html = DeckSheetFormat.html(deck, cards, ruleset, formatDate(Clock.System.now().toEpochMilliseconds()), order)
                        state.appScope.launch { if (files.saveText("$baseName sheet.html", "text/html", html)) state.notify("Print sheet saved.") }
                    }, compact = true)
                }
            }
        },
        dismissButton = { GameButton("Close", onClick = onDismiss, style = GameButtonStyle.GHOST, compact = true) },
    )
}

/**
 * Lays [content] out at [IMAGE_WIDTH] with a density that makes it exactly [IMAGE_WIDTH_PX] wide,
 * records it into [layer] at that size, and shows it scaled to fit the space it is given.
 */
@Composable
private fun CaptureAtFixedWidth(layer: GraphicsLayer, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scale = constraints.maxWidth.toFloat() / IMAGE_WIDTH_PX
        Box(
            Modifier.layout { measurable, _ ->
                val placeable = measurable.measure(Constraints.fixedWidth(IMAGE_WIDTH_PX))
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
                    for ((label, group) in order.groups(main, cards)) {
                        Text("$label · ${group.sumOf { it.quantity }}", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                        group.chunked(CARDS_PER_ROW).forEach { row ->
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
                Sparkle(Modifier.padding(start = 6.dp), size = 10.dp, color = Gold)
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
