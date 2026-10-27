package app.lockon.ui.decks

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
import app.lockon.ui.components.HudHeader
import app.lockon.ui.components.LockOnMark
import app.lockon.ui.components.TricolorStripe
import app.lockon.ui.theme.TabularNumbers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Clock

/** Saved deck images are this many pixels wide on every device, whatever its screen density. */
private const val IMAGE_WIDTH_PX = 1080
private val IMAGE_WIDTH = 540.dp

/** Every way out of the app for one deck: the text list, a shareable image, and a printable sheet. */
@Suppress("DEPRECATION") // LocalClipboard needs ClipEntry, which differs per platform.
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
    val text = remember(deck, cards) { DeckListFormat.export(deck, cards) }
    val baseName = deck.name.filter { it.isLetterOrDigit() || it in " -_" }.trim().ifBlank { "deck" }
    val clipboard = LocalClipboardManager.current
    val layer = rememberGraphicsLayer()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(deck.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryTabRow(selectedTabIndex = tab) {
                    listOf("List", "Image", "Print sheet").forEachIndexed { i, label ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label, maxLines = 1) })
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
                        CaptureAtFixedWidth(layer) { DeckImage(deck, cards, ruleset) }
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
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(text))
                            state.notify("Deck list copied.")
                        }) { Text("Copy") }
                        TextButton(onClick = {
                            state.appScope.launch { if (files.saveText("$baseName.txt", "text/plain", text)) state.notify("Deck list saved.") }
                        }) { Text("Save file") }
                    }
                    1 -> TextButton(onClick = {
                        state.appScope.launch {
                            val png = encodePng(layer.toImageBitmap())
                            if (files.saveBytes("$baseName.png", "image/png", png)) state.notify("Deck image saved.")
                        }
                    }) { Text("Save image") }
                    else -> TextButton(onClick = {
                        val html = DeckSheetFormat.html(deck, cards, ruleset, formatDate(Clock.System.now().toEpochMilliseconds()))
                        state.appScope.launch { if (files.saveText("$baseName sheet.html", "text/html", html)) state.notify("Print sheet saved.") }
                    }) { Text("Save sheet") }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
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
private fun DeckImage(deck: DeckSnapshot, cards: Map<String, Card>, ruleset: Ruleset) {
    val scheme = MaterialTheme.colorScheme
    val legal = remember(deck, cards, ruleset) { DeckValidator.validate(deck, cards, ruleset).isLegal }
    val colors = deck.entries.filter { it.section == DeckSection.MAIN }.flatMap { cards[it.cardNumber]?.colors.orEmpty() }.toSet()
    val order = compareBy<DeckEntry>(
        { cards[it.cardNumber]?.type?.let(CardType.entries::indexOf) ?: CardType.entries.size },
        { cards[it.cardNumber]?.level ?: 99 }, { cards[it.cardNumber]?.cost ?: 99 }, { it.cardNumber },
    )
    Column(Modifier.width(IMAGE_WIDTH).background(scheme.surface)) {
        TricolorStripe()
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                deckCover(deck, cards)?.let { CardImage(it.card, Modifier.width(64.dp), it.printingId) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(deck.name, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ColorPips(colors)
                        if (legal) Badge("Legal · ${ruleset.name}", scheme.secondaryContainer, scheme.onSecondaryContainer)
                        else Badge("Draft", scheme.surfaceVariant, scheme.onSurfaceVariant)
                    }
                }
            }
            for (section in listOf(DeckSection.MAIN, DeckSection.RESOURCE)) {
                val entries = deck.entries.filter { it.section == section }.sortedWith(order)
                if (entries.isEmpty()) continue
                HudHeader(section.label, readout = "${entries.sumOf { it.quantity }}")
                entries.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { entry ->
                            Box(Modifier.weight(1f)) {
                                CardImage(cards[entry.cardNumber], Modifier.fillMaxWidth(), entry.printingId)
                                Text(
                                    "×${entry.quantity}",
                                    style = MaterialTheme.typography.labelLarge.merge(TabularNumbers),
                                    color = scheme.onPrimary,
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                                        .clip(MaterialTheme.shapes.extraSmall).background(scheme.primary).padding(horizontal = 6.dp),
                                )
                            }
                        }
                        repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LockOnMark(size = 18.dp)
                Text("Built with Lock On", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}
