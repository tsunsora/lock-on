package app.lockon.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lockon.model.Card
import app.lockon.model.Printing
import app.lockon.ui.theme.SuitColors
import app.lockon.ui.theme.TabularNumbers
import app.lockon.ui.theme.cardColor
import app.lockon.ui.theme.onCardColor

/** Physical cards are 63 × 88 mm with corners about 5% of their width (UX rule V4: never crop a card). */
const val CARD_ASPECT = 63f / 88f
val CardShape = RoundedCornerShape(percent = 5)

/**
 * A card at its true shape: the official image when the catalog has one, otherwise a designed
 * placeholder card in the same proportions, so grids never shift when images arrive.
 */
@Composable
fun CardImage(card: Card?, modifier: Modifier = Modifier, printingId: String? = null) {
    val art = card?.let { LocalArtwork.current.urlFor(it.cardNumber, printingId) }
    Box(modifier.aspectRatio(CARD_ASPECT).shadow(3.dp, CardShape).clip(CardShape)) {
        CardArtwork(art, contentDescription = card?.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) {
            PlaceholderCard(card, Modifier.fillMaxSize())
        }
    }
}

/** A drawn stand-in card: colour frame, cost, level, type, name, and AP/HP, scaled to its width. */
@Composable
fun PlaceholderCard(card: Card?, modifier: Modifier = Modifier) {
    val color = cardColor(card?.colors?.firstOrNull())
    val on = onCardColor(card?.colors?.firstOrNull())
    BoxWithConstraints(
        modifier.background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.55f), MaterialTheme.colorScheme.surfaceContainerHigh)))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.8f)), CardShape),
    ) {
        val u = maxWidth.value / 63f // one card millimetre in dp
        if (u < 1.1f) {
            // Thumbnail size: cost only, large enough to read.
            Text(card?.cost?.toString() ?: "", color = on, fontWeight = FontWeight.Bold, fontSize = (u * 22).sp,
                modifier = Modifier.align(Alignment.TopStart).padding((u * 4).dp))
            return@BoxWithConstraints
        }
        LockOnMark(Modifier.align(Alignment.Center), size = (u * 30).dp, reticleColor = Color.White.copy(alpha = 0.12f))
        Column(Modifier.align(Alignment.TopStart).padding((u * 3).dp)) {
            Text(card?.cost?.toString() ?: "", color = on, fontWeight = FontWeight.Bold, fontSize = (u * 9).sp, lineHeight = (u * 9).sp)
            card?.level?.let { Text("Lv.$it", color = on, fontSize = (u * 4).sp, fontWeight = FontWeight.Bold) }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding((u * 3).dp)) {
            // The type sits with the name, leaving the top edge clear for rule tags.
            Text(card?.type?.label?.uppercase().orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = (u * 3.2).sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(card?.name ?: "Unknown card", color = MaterialTheme.colorScheme.onSurface, fontSize = (u * 4.6).sp,
                lineHeight = (u * 5.4).sp, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height((u * 2).dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(card?.cardNumber.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = (u * 3).sp, modifier = Modifier.weight(1f))
                if (card?.ap != null || card?.hp != null) {
                    Text("${card.ap ?: "–"} | ${card.hp ?: "–"}", color = MaterialTheme.colorScheme.onSurface, fontSize = (u * 4.4).sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** The back of a card, for face-down shields. */
@Composable
fun CardBack(modifier: Modifier = Modifier) {
    Box(
        modifier.aspectRatio(CARD_ASPECT).clip(CardShape)
            .background(Brush.linearGradient(listOf(Color(0xFF1C2A48), Color(0xFF0D1320), Color(0xFF1C2A48))))
            .border(BorderStroke(2.dp, SuitColors.vFinYellow.copy(alpha = 0.5f)), CardShape),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LockOnMark(size = maxWidth * 0.5f, reticleColor = SuitColors.armorWhite.copy(alpha = 0.7f))
        }
    }
}

/**
 * A card in a grid: the whole card, never covered, so cost and AP/HP stay readable. Cards in the
 * deck get an outline; a rule tag sits top-centre (S3). With controls, a slim bar under the card
 * holds − count + with 48 dp touch targets (S1, R1).
 */
@Composable
fun CardTile(
    card: Card,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    quantity: Int? = null,
    tag: @Composable () -> Unit = {},
    onMinus: (() -> Unit)? = null,
    onPlus: (() -> Unit)? = null,
    selected: Boolean = false,
) {
    val press = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val count = quantity ?: 0
    val outline = when {
        selected -> MaterialTheme.colorScheme.secondary
        count > 0 -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth().pressScale(press)
                .border(BorderStroke(2.dp, outline), CardShape)
                .clickable(interactionSource = press, indication = LocalIndication.current, onClickLabel = "Card details", onClick = onClick),
        ) {
            CardImage(card, Modifier.fillMaxWidth())
            Box(Modifier.align(Alignment.TopCenter).padding(top = 6.dp)) { tag() }
        }
        if (onMinus != null && onPlus != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TileButton("−", "Remove one ${card.name}", enabled = count > 0) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onMinus()
                }
                RollingNumber(
                    count, MaterialTheme.typography.titleMedium.merge(TabularNumbers),
                    if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TileButton("+", "Add one ${card.name}", primary = true) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onPlus()
                }
            }
        }
    }
}

/** A round tile button: 32 dp to look at, 48 dp to hit. */
@Composable
private fun TileButton(symbol: String, description: String, primary: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).semantics { contentDescription = description }.clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape)
                .background(
                    when {
                        !enabled -> MaterialTheme.colorScheme.surfaceContainerHigh
                        primary -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(symbol, style = MaterialTheme.typography.titleLarge,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.outline
                    primary -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurface
                })
        }
    }
}

/** − count + sized for tiles; 48 dp touch targets with a light tick on phones. */
@Composable
fun CompactStepper(quantity: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onMinus() }, enabled = quantity > 0) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Box(Modifier.widthIn(min = 22.dp), contentAlignment = Alignment.Center) {
            RollingNumber(
                quantity, MaterialTheme.typography.titleMedium.merge(TabularNumbers),
                if (quantity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onPlus() }) {
            Icon(Icons.Default.Add, contentDescription = "Add one")
        }
    }
}

/**
 * Everything about one card. Side by side (image left) when there's room, stacked otherwise.
 * Used in the details dialog and in the desktop Cards side panel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardDetailContent(card: Card, printings: List<Printing>, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    var shown by remember(card.cardNumber) { mutableStateOf<String?>(null) }
    BoxWithConstraints(modifier) {
        val sideBySide = maxWidth >= 640.dp
        val imageWidth = (maxWidth * 0.42f).coerceAtMost(380.dp)
        val image: @Composable (Modifier) -> Unit = { m -> CardImage(card, m, printingId = shown) }
        val info: @Composable (Modifier) -> Unit = { m ->
            Column(m, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorPips(card.colors)
                    Text(
                        listOf(card.cardNumber, card.rarity, card.setCode).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox("Type", card.type.label)
                    card.level?.let { StatBox("Level", "$it") }
                    card.cost?.let { StatBox("Cost", "$it") }
                    card.ap?.let { StatBox("AP", "$it") }
                    card.hp?.let { StatBox("HP", "$it") }
                }
                if (card.traits.isNotEmpty() || card.zones.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        card.traits.forEach { Chip("($it)") }
                        card.zones.forEach { Chip(it, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer) }
                    }
                }
                card.link?.let { Text("Link  $it", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
                if (card.displayText.isNotBlank()) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = MaterialTheme.shapes.small) {
                        Text(effectText(card.displayText), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(14.dp).fillMaxWidth())
                    }
                }
                if (card.correctedText != null) {
                    Text("Shows corrected text. Printed: ${card.text}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                actions()
                if (printings.size > 1) {
                    HudHeader("Printings", readout = "${printings.size}")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        printings.forEach { p ->
                            val isShown = (shown ?: printings.firstOrNull { !it.isAlternateArt }?.printingId ?: printings.first().printingId) == p.printingId
                            Column(
                                Modifier.width(88.dp).clip(MaterialTheme.shapes.small).clickable { shown = p.printingId }.padding(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CardImage(card, Modifier.fillMaxWidth().border(
                                    BorderStroke(2.dp, if (isShown) MaterialTheme.colorScheme.primary else Color.Transparent), CardShape,
                                ), printingId = p.printingId)
                                Text(p.label.removePrefix("Reprint: "), style = MaterialTheme.typography.labelSmall, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                                    color = if (isShown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        if (sideBySide) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                image(Modifier.width(imageWidth))
                info(Modifier.weight(1f).verticalScroll(rememberScrollState()))
            }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                image(Modifier.fillMaxWidth(0.82f).align(Alignment.CenterHorizontally))
                info(Modifier.fillMaxWidth())
            }
        }
    }
}

/** Card details over the current screen. Wide windows get a side-by-side layout. */
@Composable
fun CardDetailDialog(card: Card, printings: List<Printing>, onDismiss: () -> Unit, extraActions: @Composable () -> Unit = {}) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val maxHeight = maxHeight * 0.9f
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 960.dp).heightIn(max = maxHeight),
        ) {
            Column(Modifier.padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(card.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }
                CardDetailContent(card, printings, Modifier.weight(1f, fill = false).padding(end = 12.dp, top = 8.dp), actions = extraActions)
            }
        }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(
        Modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge.merge(TabularNumbers))
    }
}

@Composable
private fun Chip(text: String, background: Color = MaterialTheme.colorScheme.surfaceVariant, foreground: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = foreground,
        modifier = Modifier.clip(MaterialTheme.shapes.small).background(background).padding(horizontal = 10.dp, vertical = 5.dp))
}

/** Effect text with 【timing】 and <keyword> markers picked out, as on the printed card. */
@Composable
private fun effectText(text: String) = buildAnnotatedString {
    val marker = Regex("【[^】]*】|<[^>]*>")
    var last = 0
    for (m in marker.findAll(text)) {
        append(text.substring(last, m.range.first))
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) { append(m.value) }
        last = m.range.last + 1
    }
    append(text.substring(last))
}
