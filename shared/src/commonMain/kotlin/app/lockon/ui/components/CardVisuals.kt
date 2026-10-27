package app.lockon.ui.components
import androidx.compose.ui.draw.drawBehind

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.BiasAlignment

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
import androidx.compose.ui.unit.Dp
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
 * deck get a lit outline and a resting glow; a rule tag sits top-centre (S3). On desktop the card
 * lifts toward you on hover, and adding or removing a copy flashes it. With controls, a plate under
 * the card holds − count + with copy pips and 48 dp touch targets (S1, R1).
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
    limit: Int? = null,
) {
    val press = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val count = quantity ?: 0
    val outline = when {
        selected -> scheme.secondary
        count > 0 -> scheme.primary
        else -> Color.Transparent
    }
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth()
                .hoverLift(press, glow = if (count > 0) scheme.primary else lighten(cardColor(card.colors.firstOrNull()), 0.2f), CardShape,
                    resting = if (count > 0) 0.35f else 0f)
                .flashOnChange(count, scheme.primary, scheme.error, CardShape)
                .border(BorderStroke(2.dp, outline), CardShape)
                .clip(CardShape)
                .clickable(interactionSource = press, indication = LocalIndication.current, onClickLabel = "Card details", onClick = onClick),
        ) {
            CardImage(card, Modifier.fillMaxWidth())
            Box(Modifier.align(Alignment.TopCenter).padding(top = 6.dp)) { tag() }
        }
        if (onMinus != null && onPlus != null) {
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp).clip(SmallPlate)
                    .background(Brush.verticalGradient(listOf(scheme.surfaceContainerHighest.copy(alpha = 0.9f), scheme.surfaceContainerLow.copy(alpha = 0.9f))))
                    .border(BorderStroke(1.dp, if (count > 0) scheme.primary.copy(alpha = 0.5f) else scheme.outlineVariant), SmallPlate),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                TileButton("−", "Remove one ${card.name}", enabled = count > 0) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onMinus()
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RollingNumber(
                        count, MaterialTheme.typography.titleLarge.merge(TabularNumbers),
                        if (count > 0) scheme.primary else scheme.onSurfaceVariant, Modifier.bumpOnIncrease(count),
                    )
                    if (limit != null) CopyPips(count, limit, pip = 6.dp)
                }
                TileButton("+", "Add one ${card.name}", primary = true) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onPlus()
                }
            }
        }
    }
}

private val SmallPlate = RoundedCornerShape(percent = 50)

/** A chamfered tile button: 32 dp to look at, 48 dp to hit. Plus glows V-fin yellow. */
@Composable
private fun TileButton(symbol: String, description: String, primary: Boolean = false, enabled: Boolean = true, target: Dp = 48.dp, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        Modifier.size(target).semantics { contentDescription = description }
            .hoverable(interaction)
            .clickable(interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(32.dp).pressScale(interaction).clip(CircleShape)
                .background(
                    when {
                        !enabled -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                        primary -> Brush.verticalGradient(listOf(lighten(Accent, if (hovered) 0.5f else 0.25f), Accent))
                        else -> Brush.verticalGradient(listOf(scheme.onSurface.copy(alpha = if (hovered) 0.22f else 0.12f), scheme.onSurface.copy(alpha = 0.06f)))
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(symbol, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = when {
                    !enabled -> scheme.outline.copy(alpha = 0.6f)
                    primary -> OnAccent
                    else -> scheme.onSurface
                })
        }
    }
}

/**
 * A deck-list row as a card strip, the way game deck builders show a list: a cost gem in the
 * card colour, the name over a crop of the card art that fades into the plate, and the controls
 * at the right. The whole card is one tap away in its details (UX rule V4a).
 */
@Composable
fun DeckStrip(
    card: Card?,
    quantity: Int,
    printingId: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    compact: Boolean = false,
    controls: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val color = cardColor(card?.colors?.firstOrNull())
    val art = card?.let { LocalArtwork.current.urlFor(it.cardNumber, printingId) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val zoom by animateFloatAsState(if (hovered) 1.03f else 1f, motion(Motion.LONG), label = "strip-art")
    val base = if (isTwilight()) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest
    Box(
        modifier.fillMaxWidth().height(if (compact) 48.dp else 58.dp)
            .flashOnChange(quantity, scheme.primary, scheme.error, StripShape)
            .clip(StripShape)
            .background(base)
            .hoverable(interaction)
            .clickable(interaction, indication = LocalIndication.current, enabled = card != null, onClick = onClick),
    ) {
        // The art crop fills the right two-thirds, aimed at the art box in the upper half of the card.
        Box(Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.7f).fillMaxHeight()) {
            CardArtwork(
                art, contentDescription = null, contentScale = ContentScale.Crop, alignment = BiasAlignment(0f, -0.42f),
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom },
            ) {
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.12f), color.copy(alpha = 0.5f)))))
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to base, 0.32f to base, 0.55f to base.copy(alpha = 0.72f), 0.75f to base.copy(alpha = 0.3f), 1f to base.copy(alpha = 0.82f),
                ),
            ),
        )
        // A colour band on the leading edge, and a lit edge on hover.
        Box(Modifier.align(Alignment.CenterStart).width(4.dp).fillMaxHeight().background(color))
        if (hovered) Box(Modifier.fillMaxSize().border(BorderStroke(1.dp, scheme.primary.copy(alpha = 0.7f)), StripShape))
        Row(Modifier.fillMaxSize().padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            CostOrb(card?.cost, color, onCardColor(card?.colors?.firstOrNull()), size = if (compact) 30.dp else 34.dp)
            Spacer(Modifier.width(if (compact) 10.dp else 12.dp))
            Column(Modifier.weight(1f)) {
                Text(card?.name ?: "Unknown card", maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                if (subtitle != null) {
                    Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = (if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium).merge(TabularNumbers),
                        color = scheme.onSurfaceVariant)
                }
            }
            // Controls sit on their own dark plate so busy artwork never hides them.
            Row(
                Modifier.clip(SmallPlate).background(base.copy(alpha = 0.9f))
                    .border(BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.8f)), SmallPlate),
                verticalAlignment = Alignment.CenterVertically,
            ) { controls() }
        }
    }
}

private val StripShape = RoundedCornerShape(16.dp)

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
        // The card sits in a pool of its own colour, like a card raised for inspection.
        val pool = cardColor(card.colors.firstOrNull())
        val image: @Composable (Modifier) -> Unit = { m ->
            // The glow fades out within its own padding, so a scrolling column never cuts it off.
            Box(m.drawBehind {
                val r = size.height / 2
                drawCircle(Brush.radialGradient(listOf(pool.copy(alpha = 0.55f), Color.Transparent), center, r), radius = r)
            }.padding(vertical = 24.dp)) {
                CardImage(card, Modifier.fillMaxWidth(), printingId = shown)
            }
        }
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
                    SectionHeader("Printings", readout = "${printings.size}")
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
            modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 960.dp).heightIn(max = maxHeight)
                .glassFrame(),
        ) {
            Column(Modifier.padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(card.name, style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
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
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

/**
 * − ×count + on a dark plate, for deck strips and the details dialog: 48 dp targets, a rolling
 * count and, with a [limit], copy pips under it.
 */
@Composable
fun PlateStepper(quantity: Int, name: String, onMinus: () -> Unit, onPlus: () -> Unit, limit: Int? = null) {
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        TileButton("−", "Remove one $name", enabled = quantity > 0) { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onMinus() }
        Column(Modifier.widthIn(min = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("×", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
                RollingNumber(
                    quantity, MaterialTheme.typography.titleLarge.merge(TabularNumbers),
                    if (quantity > 0) scheme.primary else scheme.onSurfaceVariant, Modifier.bumpOnIncrease(quantity),
                )
            }
            if (limit != null) CopyPips(quantity, limit, pip = 5.dp)
        }
        TileButton("+", "Add one $name", primary = true) { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onPlus() }
    }
}

/**
 * A deck card shown small: the whole card at its real shape (UX rule V4a), so cost and AP/HP stay
 * readable, with − ×count + and copy pips underneath (S1). It lifts on hover and flashes when its
 * count changes, like the card list's tiles. [target] is the buttons' hit size: 48 dp on phones (R1).
 */
@Composable
fun DeckCard(
    card: Card,
    quantity: Int,
    printingId: String?,
    onClick: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    limit: Int? = null,
    target: Dp = 48.dp,
) {
    val press = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth()
                .hoverLift(press, glow = lighten(cardColor(card.colors.firstOrNull()), 0.2f), CardShape, lift = 2.dp)
                .flashOnChange(quantity, scheme.primary, scheme.error, CardShape)
                .clip(CardShape)
                .clickable(interactionSource = press, indication = LocalIndication.current, onClickLabel = "Card details", onClick = onClick),
        ) {
            CardImage(card, Modifier.fillMaxWidth(), printingId)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TileButton("−", "Remove one ${card.name}", enabled = quantity > 0, target = target) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onMinus()
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("×", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 1.dp))
                    RollingNumber(quantity, MaterialTheme.typography.titleMedium.merge(TabularNumbers), scheme.primary, Modifier.bumpOnIncrease(quantity))
                }
                if (limit != null) CopyPips(quantity, limit, pip = 5.dp)
            }
            TileButton("+", "Add one ${card.name}", primary = true, target = target) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onPlus()
            }
        }
    }
}
