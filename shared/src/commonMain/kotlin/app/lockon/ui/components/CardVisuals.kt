package app.lockon.ui.components
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.geometry.Size
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.draw.drawBehind

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.hoverable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.BiasAlignment

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import app.lockon.model.Card
import app.lockon.model.Printing
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.theme.SuitColors
import app.lockon.ui.theme.TabularNumbers
import app.lockon.ui.theme.cardColor
import app.lockon.ui.theme.onCardColor

/** Physical cards are 63 × 88 mm with corners about 5% of their width (UX rule V4: never crop a card). */
const val CARD_ASPECT = 63f / 88f
val CardShape = RoundedCornerShape(percent = 5)
private val BackShape = RoundedCornerShape(percent = 9)

/**
 * A card at its true shape: the official image when the catalog has one, otherwise a designed
 * placeholder card in the same proportions, so grids never shift when images arrive. Grid cards
 * turn off [shadow]: a shadow is a drawing layer of its own, and their hover lift casts one.
 */
@Composable
fun CardImage(card: Card?, modifier: Modifier = Modifier, printingId: String? = null, shadow: Boolean = true, max: Boolean = false) {
    val art = card?.let { LocalArtwork.current.urlFor(it.cardNumber, printingId) }
    Box(modifier.aspectRatio(CARD_ASPECT).then(if (shadow) Modifier.shadow(3.dp, CardShape) else Modifier).clip(CardShape)) {
        CardArtwork(art, contentDescription = card?.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) {
            PlaceholderCard(card, Modifier.fillMaxSize(), max = max)
        }
    }
}

/** How long the pointer rests on a card before its preview opens, so sweeping across a grid stays calm. */
private const val HOVER_PREVIEW_DELAY_MS = 350L

/**
 * The whole card at a readable size beside its thumbnail while the pointer rests on it. Touch
 * screens never hover, so this only shows with a mouse. The preview sits beside the card, never
 * over it, so it never takes the hover away, and screen readers skip it: the card already has a name.
 */
@Composable
fun CardHoverPreview(card: Card, printingId: String?, hovered: Boolean) {
    // Nothing but this flag is kept for cards the pointer isn't on, which is nearly all of a grid. It
    // stays up after the pointer leaves, for as long as the preview takes to fade.
    var open by remember { mutableStateOf(false) }
    if (hovered && !open) LaunchedEffect(Unit) { delay(HOVER_PREVIEW_DELAY_MS); open = true }
    if (!open) return
    val reduce = LocalReduceMotion.current
    val arrival = remember { Animatable(0f) }
    LaunchedEffect(hovered) {
        if (reduce) arrival.snapTo(if (hovered) 1f else 0f)
        else if (hovered) arrival.animateTo(1f, tween(Motion.MEDIUM, easing = Motion.emphasizedDecelerate))
        else arrival.animateTo(0f, tween(Motion.SHORT, easing = Motion.emphasizedAccelerate))
        if (!hovered) open = false
    }
    val gap = with(LocalDensity.current) { 12.dp.roundToPx() }
    Popup(popupPositionProvider = remember(gap) { BesideAnchor(gap) }, properties = PopupProperties(focusable = false)) {
        // As large as fits the window, up to a size where the effect text reads easily.
        Box(Modifier.widthIn(max = 300.dp).padding(vertical = 8.dp).arrive(arrival.asState(), from = 0.97f).clearAndSetSemantics {}) {
            // A solid backing under the face, so its hairline edge never shows the window through it.
            CardImage(card, Modifier.shadow(18.dp, CardShape).background(MaterialTheme.colorScheme.surface, CardShape), printingId)
        }
    }
}

/** Places a popup to the right of its anchor, or to the left when the right has no room, centred on it. */
private class BesideAnchor(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val right = anchorBounds.right + gap
        val left = anchorBounds.left - gap - popupContentSize.width
        val x = when {
            right + popupContentSize.width <= windowSize.width -> right
            left >= 0 -> left
            else -> (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        }
        val y = (anchorBounds.center.y - popupContentSize.height / 2).coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))
        return IntOffset(x, y)
    }
}

/**
 * A drawn stand-in card for a card without artwork, laid out like a printed card so it reads as one
 * at a glance: a flat face in the card's colour, a corner tab holding the cost with the level under
 * it, the type symbol as a line drawing where the art would be, and a deep band along the bottom with
 * the name on up to two lines, the colour and type in words, and AP / HP. Every word sits on the deep
 * tone, never on the bright face, so it stays readable on every colour (UX rules V4, I4).
 * Below 96 dp wide it keeps only the colour, the cost and the symbol, and below 48 dp only the colour
 * and the symbol, so small faces never crowd. With [max], the band's last line ends with "Max" in
 * quiet text: the card is at its copy limit (UX rule S3).
 */
@Composable
fun PlaceholderCard(card: Card?, modifier: Modifier = Modifier, max: Boolean = false) {
    val lead = card?.colors?.firstOrNull()
    val face = cardColor(lead)
    val deep = placeholderDeep(face)
    val white = lead == app.lockon.model.CardColor.WHITE
    // The symbol stands on the bright face: white, or the deep tone on a white card.
    val glyphInk = if (white) deep else Color.White.copy(alpha = 0.92f)
    BoxWithConstraints(modifier.background(face).border(BorderStroke(1.dp, PlaceholderEdge), CardShape)) {
        val u = maxWidth.value / 63f // one card millimetre in dp
        val full = maxWidth >= 96.dp
        val roomForCost = maxWidth >= 48.dp
        val cost = card?.cost
        val tab = cost != null && roomForCost
        Column(Modifier.fillMaxSize()) {
            // The art area: the symbol in the middle of what is left between the corner tab and the band.
            Box(Modifier.fillMaxWidth().weight(1f).padding(top = if (tab && full) (u * 20f).dp else 0.dp)) {
                Icon(
                    LineIcons.faceForType(card?.type), contentDescription = null, tint = glyphInk,
                    modifier = Modifier.align(Alignment.Center).size((u * 63f).dp * (if (full) 0.32f else if (roomForCost) 0.4f else 0.5f)),
                )
            }
            if (full) PlaceholderBand(card, max, deep, u)
        }
        // The cost tab: flush in the top-left corner like a printed card's, the level under the cost.
        if (tab && cost != null) Column(
            Modifier.align(Alignment.TopStart).width((u * 16f).dp)
                .background(deep, RoundedCornerShape(bottomEnd = (u * 4f).dp))
                .padding(top = (u * 2f).dp, bottom = (u * (if (full) 2f else 1.6f)).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((u * 0.6f).dp),
        ) {
            Box(Modifier.fillMaxWidth().height((u * 9f).dp)) {
                CenteredGlyph(cost.toString(), Color.White, (u * 9f).sp)
            }
            val level = card?.level
            if (level != null && full) Text(
                "Lv $level", color = PlaceholderQuiet, fontSize = (u * 4.2f).sp, lineHeight = (u * 4.8f).sp,
                maxLines = 1, style = MaterialTheme.typography.labelMedium.merge(TabularNumbers),
            )
        }
    }
}

/** The band along the bottom of a full-size drawn face: the name, then colour and type, Max and AP / HP. */
@Composable
private fun PlaceholderBand(card: Card?, max: Boolean, deep: Color, u: Float) {
    Column(
        // One height for every card, room for two lines of name, so the symbols and bands line up across a grid.
        Modifier.fillMaxWidth().height((u * 25f).dp).background(deep).padding(horizontal = (u * 3.4f).dp, vertical = (u * 2.6f).dp),
    ) {
        Text(
            card?.name ?: "Unknown card", color = Color.White, fontSize = (u * 5.4f).sp, lineHeight = (u * 6.4f).sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Colour in words as well as on the face, so colour is never the only signal (UX rule I4).
            val kind = listOfNotNull(
                card?.colors?.sortedBy { it.ordinal }?.joinToString(" ") { it.label }?.takeIf { it.isNotEmpty() },
                card?.type?.label?.let { if (card.colors.isEmpty()) it else it.lowercase() },
            ).joinToString(" ")
            val small = MaterialTheme.typography.labelMedium
            Text(kind, color = PlaceholderQuiet, fontSize = (u * 4.2f).sp, lineHeight = (u * 5f).sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, style = small, modifier = Modifier.weight(1f, fill = true))
            if (max) Text("Max", color = MaxInk, fontSize = (u * 4.2f).sp, lineHeight = (u * 5f).sp, style = small,
                modifier = Modifier.padding(start = (u * 1.6f).dp))
            if (card?.ap != null || card?.hp != null) Text(
                "${card.ap ?: "–"} / ${card.hp ?: "–"}", color = Color.White, fontSize = (u * 4.6f).sp, lineHeight = (u * 5f).sp,
                maxLines = 1, style = small.merge(TabularNumbers), modifier = Modifier.padding(start = (u * 2f).dp),
            )
        }
    }
}

/** The deep tone of a drawn face's tab and band: its colour taken well toward the night ink, so white text reads on every colour. */
private fun placeholderDeep(face: Color): Color = androidx.compose.ui.graphics.lerp(face, Color(0xFF0A1028), 0.55f)

/** A drawn face's edge: one ink hairline, so a white card still has an edge on a day panel (UX rule V1). */
private val PlaceholderEdge = Color(0xFF0A1028).copy(alpha = 0.14f)

/** Secondary words on a drawn face's deep tone: the level, and the colour and type. */
private val PlaceholderQuiet = Color.White.copy(alpha = 0.74f)

/** Max in a card's name strip: quiet text on the strip's dark plate, the same in both themes. */
private val MaxInk = Color(0xFFA9B3CC)

/** The small solid tag in a card's corner (Side 2, Deck 3): the same dark plate and light text in both themes. */
val CardTagFill = Color(0xFF1C2235)
val CardTagInk = Color(0xFFEEF0F5)
val CardTagLine = Color.White.copy(alpha = 0.14f)

/** The back of a card, for face-down shields. */
@Composable
fun CardBack(modifier: Modifier = Modifier) {
    Box(
        modifier.aspectRatio(CARD_ASPECT).clip(BackShape)
            .background(Brush.linearGradient(listOf(Color(0xFF4F80F0), Color(0xFF2A3FC4))))
            .drawBehind {
                val step = 9.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(Color.White.copy(alpha = 0.08f), Offset(x, size.height), Offset(x + size.height, 0f), 3.dp.toPx())
                    x += step
                }
            }
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), BackShape),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LockOnMark(size = maxWidth * 0.46f, reticleColor = Color.White.copy(alpha = 0.92f))
        }
    }
}

/**
 * A card in a grid: the whole card, never covered, so cost and AP/HP stay readable. A rule tag sits in
 * the top corner, and a card at its copy [limit] says Max in its name strip (S3), or in that corner when
 * artwork covers the strip. On desktop the card lifts toward you on hover, and adding or removing a copy
 * flashes it. With controls, the deck's own − count + plate sits under the card (S1); [target] is the
 * buttons' hit size on phones (R1).
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
    target: Dp = 44.dp,
) {
    val press = remember { MutableInteractionSource() }
    val hover = remember { HoverState() }
    val hovered = hover.hovered
    val scheme = MaterialTheme.colorScheme
    val count = quantity ?: 0
    val max = limit != null && count == limit
    val art = LocalArtwork.current.urlFor(card.cardNumber, null)
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth()
                .hoverState(hover)
                .hoverLift(press, CardShape, edge = scheme.primary)
                // Inside the lift, so a selected card's outline rises with it.
                .selectionEdge(CardShape, scheme.primary, selected)
                .flashOnChange(count, scheme.primary, scheme.error, CardShape)
                .clip(CardShape)
                .clickable(interactionSource = press, indication = pressWash(CardShape, hover = false), onClickLabel = "Card details", onClick = onClick),
        ) {
            CardImage(card, Modifier.fillMaxWidth(), shadow = false, max = max)
            Box(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp)) {
                // Artwork covers the drawn name strip, so there Max takes a small tag in the corner instead.
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (max && art != null) Badge("Max", CardTagFill, CardTagInk, outline = CardTagLine)
                    tag()
                }
            }
            CardHoverPreview(card, null, hovered)
        }
        if (onMinus != null && onPlus != null) CardCountBar(count, card.name, limit, target, onMinus, onPlus)
    }
}

/**
 * The colour of a card's count: teal while it is in the deck (UX rule V3), quiet at 0, and red past the
 * limit, where the problem bar names the card and the limit (I3).
 */
@Composable
@ReadOnlyComposable
internal fun countInk(count: Int, limit: Int?): Color = when {
    limit != null && count > limit -> MaterialTheme.colorScheme.error
    count > 0 -> MaterialTheme.colorScheme.primary
    else -> mutedInk()
}

/**
 * − count + under a card, the same in the deck and in Add cards on every device (UX rule K3): one plate
 * the height of a control holding one centred number in [countInk]. + is always teal and never dims, and
 * − keeps full ink at 0 (S4). [target] is the buttons' hit size on phones (R1), which reaches past the plate.
 */
@Composable
private fun CardCountBar(count: Int, name: String, limit: Int?, target: Dp, onMinus: () -> Unit, onPlus: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val hit = if (desktopControls) ControlSize else target
    val plate = if (desktopControls) ControlSize else 40.dp
    val fill = neutralFill()
    Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(hit), contentAlignment = Alignment.Center) {
        Spacer(Modifier.fillMaxWidth().height(plate).clip(LockOnShapes.button).background(fill))
        RollingNumber(count, MaterialTheme.typography.titleMedium.merge(TabularNumbers), countInk(count, limit), Modifier.bumpOnIncrease(count))
        Box(Modifier.align(Alignment.CenterStart)) {
            TileButton("−", "Remove one $name", enabled = count > 0, target = hit) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onMinus()
            }
        }
        Box(Modifier.align(Alignment.CenterEnd)) {
            TileButton("+", "Add one $name", target = hit) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onPlus()
            }
        }
    }
}

/**
 * A plate's − or +: a drawn glyph, teal for + and full ink for −, in a 32 dp face that takes the neutral
 * wash on hover and press, with a hit area of [target] (48 dp on phones, the control height on desktop).
 * A − at 0 has nothing to remove, so it ignores taps but keeps its ink: the quiet 0 beside it says why.
 */
@Composable
private fun TileButton(symbol: String, description: String, enabled: Boolean = true, target: Dp = 48.dp, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hover = remember { HoverState() }
    val hovered = hover.hovered && enabled
    val pressed by interaction.collectIsPressedHeld()
    Box(
        Modifier.size(if (desktopControls) ControlSize else target).semantics { contentDescription = description }
            .hoverable(interaction).hoverState(hover)
            .clickable(interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val wash = if (hovered || pressed) neutralWash(hovered, pressed) else Color.Transparent
        val ink = if (symbol == "+") Accent else scheme.onSurface
        val glyph = rememberVectorPainter(if (symbol == "+") LineIcons.Plus else LineIcons.Minus)
        // Everything is drawn here, so a plate's two buttons ease their colours with no state of their own
        // at rest. The glyph is drawn too, so it sits in the exact middle of its button; a font's − and + don't.
        Spacer(
            Modifier.size(32.dp).pressScale(interaction).clip(InsetControlShape)
                .drawTones(wash, wash, ink) { fillTop, _, tone ->
                    drawRect(fillTop)
                    val s = 18.dp.toPx()
                    translate((size.width - s) / 2, (size.height - s) / 2) {
                        with(glyph) { draw(Size(s, s), colorFilter = ColorFilter.tint(tone)) }
                    }
                },
        )
    }
}

/**
 * A deck-list row: a cost gem in the card colour, a small crop of the art, the name, and the
 * controls at the right. The whole card is one tap away in its details (UX rule V4a). On desktop, rows
 * lie flat on the deck panel like the card list's rows, with no fill or rim of their own: hover is the
 * 4% ink wash every row takes (V5), and only the gem and the art carry colour (V3).
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
    val hover = remember { HoverState() }
    val hovered = hover.hovered
    Box(
        modifier.fillMaxWidth().height(if (compact) 48.dp else 56.dp)
            .flashOnChange(quantity, scheme.primary, scheme.error, StripShape)
            .clip(StripShape)
            // Phones lay the list on the sky, so there a row sits on the same solid glass as its heading.
            .then(if (desktopControls) Modifier else Modifier.background(glassColors().strong))
            .hoverable(interaction).hoverState(hover)
            .clickable(interaction, indication = pressWash(StripShape), enabled = card != null, onClick = onClick),
    ) {
        // A strip shows only a crop of the art, so the whole card opens beside it on hover.
        if (card != null) CardHoverPreview(card, printingId, hovered)
        Row(Modifier.fillMaxSize().padding(start = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            val lead = card?.colors?.firstOrNull()
            CostOrb(card?.cost, color, onCardColor(lead), size = if (compact) 28.dp else 32.dp, outlined = lead == app.lockon.model.CardColor.WHITE)
            Spacer(Modifier.width(8.dp))
            // Aimed at the art box in the upper half of the card.
            Box(Modifier.size(if (compact) 30.dp else 34.dp, if (compact) 38.dp else 42.dp).clip(RoundedCornerShape(6.dp))) {
                CardArtwork(art, contentDescription = null, contentScale = ContentScale.Crop, alignment = BiasAlignment(0f, -0.42f), modifier = Modifier.fillMaxSize()) {
                    PlaceholderCard(card, Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(card?.name ?: "Unknown card", maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                if (subtitle != null) {
                    Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = (if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium).merge(TabularNumbers),
                        color = scheme.onSurfaceVariant)
                }
            }
            StepperTray(controls = controls)
        }
    }
}

private val StripShape = RoundedCornerShape(16.dp)

/**
 * A row's − count + plate: the one neutral fill every control that isn't teal wears (UX rule K3), with
 * no rim. The card list's rows use it too, so both lists match.
 */
@Composable
fun StepperTray(modifier: Modifier = Modifier, controls: @Composable () -> Unit) {
    Row(modifier.clip(LockOnShapes.button).background(neutralFill()), verticalAlignment = Alignment.CenterVertically) { controls() }
}

/**
 * Everything about one card. Side by side (art and printings left) when there's room, stacked otherwise.
 * Used in the details dialog and in the desktop Cards side panel. [actions], when given, sit in an
 * "In this deck" panel under the card text. With [onChoosePrinting] the printings choose which one the
 * deck uses, starting from [chosenPrinting], instead of only previewing them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardDetailContent(
    card: Card, printings: List<Printing>, modifier: Modifier = Modifier, actions: (@Composable () -> Unit)? = null,
    chosenPrinting: String? = null, onChoosePrinting: ((String) -> Unit)? = null, showName: Boolean = false,
) {
    // Starts over when choosing turns on (the card was just added), so the lit tile is the one the deck uses.
    var shown by remember(card.cardNumber, onChoosePrinting != null) { mutableStateOf(chosenPrinting) }
    var inspecting by remember(card.cardNumber) { mutableStateOf(false) }
    BoxWithConstraints(modifier) {
        val sideBySide = maxWidth >= 640.dp
        // A phone's sheet names the card itself; a side panel, however narrow, keeps the card beside its numbers.
        val roomy = !showName && maxWidth >= 300.dp
        val imageWidth = (maxWidth * 0.4f).coerceIn(260.dp, 340.dp)
        val thumbWidth = (maxWidth * 0.4f).coerceAtMost(156.dp)
        val image: @Composable (Modifier, Boolean) -> Unit = { m, tight ->
            // The card alone on the panel: nothing glows behind it. Tight keeps a little room round it.
            Box(if (!tight) m else m.padding(8.dp)) {
                // A click opens the card full size, to turn in the light.
                val press = remember { MutableInteractionSource() }
                // Another printing fades in over the last, so the art never blanks between them.
                androidx.compose.animation.Crossfade(
                    shown,
                    Modifier.fillMaxWidth().pressScale(press).pointerHoverIcon(PointerIcon.Hand)
                        .clickable(press, indication = null, onClickLabel = "View larger") { inspecting = true },
                    animationSpec = motion(), label = "printing",
                ) { CardImage(card, Modifier.fillMaxWidth(), printingId = it, shadow = false) }
            }
        }
        if (inspecting) CardInspectDialog(card, shown, onDismiss = { inspecting = false })
        // A deck's printing choice joins the other deck controls instead of trailing below them, where on
        // a phone it starts off screen.
        val printingsInPanel = onChoosePrinting != null && actions != null
        val printingStrip: @Composable () -> Unit = {
            if (printings.size > 1) {
                val shownId = shown ?: printings.firstOrNull { !it.isAlternateArt }?.printingId ?: printings.first().printingId
                // Tapping the printing already shown opens it full size, since on phones the art above
                // may have scrolled out of view.
                val tile: @Composable (Printing, Modifier) -> Unit = { p, m ->
                    PrintingTile(card, p, p.printingId == shownId, m, choosing = onChoosePrinting != null) {
                        if (p.printingId == shownId) inspecting = true else {
                            shown = p.printingId
                            onChoosePrinting?.invoke(p.printingId)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (onChoosePrinting != null && !sideBySide) SectionHeader("Printing in this deck", readout = "${printings.size}")
                    if (!roomy) {
                        // Phones: a sideways strip of tiles big enough to tell the art apart, rather than
                        // a grid of slivers. The fade only shows while there is more to scroll to.
                        val scroll = rememberScrollState()
                        val more = scroll.value < scroll.maxValue
                        Row(
                            (if (more) Modifier.fadeEnd(32.dp) else Modifier).horizontalScroll(scroll),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            printings.forEach { p -> tile(p, Modifier.width(88.dp)) }
                        }
                    } else {
                        // A grid rather than a scrolling strip, so every printing is in view at once. Five to a
                        // row wherever they fit, so every card lays out alike. Narrower columns share the
                        // printings evenly instead, so nine read as three rows of three rather than 4, 4 and 1.
                        // Either way the tiles stretch to fill the width rather than leaving a gap at the end.
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val gap = 8.dp
                            val fit = ((maxWidth + gap) / ((if (sideBySide) 92.dp else 76.dp) + gap)).toInt().coerceIn(1, 5)
                            val rows = (printings.size + fit - 1) / fit
                            val columns = if (fit == 5 || printings.size <= fit) fit else (printings.size + rows - 1) / rows
                            val tileWidth = (maxWidth - gap * (columns - 1)) / columns
                            // Fixed rows rather than a FlowRow: tile widths rounding a pixel past the line made
                            // it wrap a column early.
                            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                                printings.chunked(columns).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                        row.forEach { p -> tile(p, Modifier.width(tileWidth)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        val idLine: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorPips(card.colors)
                Text(
                    listOf(card.cardNumber, card.rarity, card.setCode).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val tags: @Composable () -> Unit = {
            if (card.traits.isNotEmpty() || card.zones.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    card.traits.forEach { Chip("($it)") }
                    card.zones.forEach { Chip(it, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer) }
                }
            }
        }
        val link: @Composable () -> Unit = {
            card.link?.let { Text("Link  $it", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
        }
        // The card text and the deck controls, the same in every layout.
        val body: @Composable () -> Unit = {
            if (card.displayText.isNotBlank()) {
                Surface(color = if (isTwilight()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerHighest, shape = MaterialTheme.shapes.small) {
                    Text(effectText(card.displayText), style = if (roomy) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp).fillMaxWidth())
                }
            }
            if (card.correctedText != null) {
                Text("Shows corrected text. Printed: ${card.text}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (actions != null) {
                Column(Modifier.padding(top = 4.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions()
                    if (printingsInPanel) printingStrip()
                }
            }
        }
        val info: @Composable (Modifier) -> Unit = { m ->
            Column(m, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // The type joins the id line, as on a phone.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorPips(card.colors)
                    Text(
                        listOf(card.type.label, card.cardNumber, card.rarity, card.setCode).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    card.level?.let { StatBox("Level", "$it", compact = true) }
                    card.cost?.let { StatBox("Cost", "$it", compact = true) }
                    card.ap?.let { StatBox("AP", "$it", compact = true) }
                    card.hp?.let { StatBox("HP", "$it", compact = true) }
                }
                // Traits, zones and the link share one line, wrapping only if they must.
                if (card.traits.isNotEmpty() || card.zones.isNotEmpty() || card.link != null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        card.traits.forEach { Box(Modifier.align(Alignment.CenterVertically)) { Chip("($it)") } }
                        card.zones.forEach { Box(Modifier.align(Alignment.CenterVertically)) { Chip(it, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer) } }
                        card.link?.let {
                            Row(Modifier.align(Alignment.CenterVertically), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Link", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(it, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
                body()
            }
        }
        if (sideBySide) {
            // The printings sit in the wider text column, where a row holds three or four of them, rather
            // than stacking two to a row under the art and leaving the text column half empty.
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.width(imageWidth).verticalScroll(rememberScrollState())) {
                    image(Modifier.fillMaxWidth(), false)
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    info(Modifier.fillMaxWidth())
                    if (!printingsInPanel) printingStrip()
                }
            }
        } else if (!roomy) {
            // Phones: the card shrinks to a thumbnail beside its numbers, so the text and the deck
            // controls fit on screen without scrolling. A tap still opens it full size.
            // The type joins the id line, traits read as a line of text rather than a chip each, and the
            // text runs a size smaller, so the card text and printings start higher up.
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    image(Modifier.width(thumbWidth), true)
                    Column(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (showName) Text(card.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ColorPips(card.colors)
                            Text(listOf(card.type.label, card.cardNumber).filter { it.isNotBlank() }.joinToString("  "), style = MaterialTheme.typography.labelLarge)
                        }
                        val stats = listOfNotNull(
                            card.cost?.let { "Cost" to it }, card.level?.let { "Level" to it },
                            card.ap?.let { "AP" to it }, card.hp?.let { "HP" to it },
                        )
                        if (stats.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                stats.forEach { (label, value) -> StatCell(label, "$value", Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                if (card.displayText.isNotBlank()) Surface(color = if (isTwilight()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerHighest, shape = MaterialTheme.shapes.small) {
                    Text(effectText(card.displayText), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp).fillMaxWidth())
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (card.traits.isNotEmpty()) LabelledLine("Traits", card.traits.joinToString(", "))
                    if (card.zones.isNotEmpty()) LabelledLine("Zones", card.zones.joinToString(", "))
                    card.link?.let { LabelledLine("Link", it) }
                    if (card.rarity.isNotBlank() || card.setCode.isNotBlank()) LabelledLine("Set", listOf(card.setCode, card.rarity).filter { it.isNotBlank() }.joinToString(" · "))
                }
                if (card.correctedText != null) {
                    Text("Shows corrected text. Printed: ${card.text}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (actions != null) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions()
                    if (printingsInPanel) printingStrip()
                }
                if (!printingsInPanel) printingStrip()
            }
        } else {
            // Stacked, the card text comes before the printings so it isn't pushed off screen.
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // In a desktop side panel a full-width card pushes its effect text out of view, so it
                // stays a readable size there; phones' dialogs keep the big card.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    image(Modifier.weight(1.25f), true)
                    Column(Modifier.weight(1f).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (showName) Text(card.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ColorPips(card.colors)
                            Text(
                                listOf(card.type.label, card.cardNumber).filter { it.isNotBlank() }.joinToString("  "),
                                style = MaterialTheme.typography.labelLarge, maxLines = 2,
                            )
                        }
                        listOfNotNull(card.cost?.let { "Cost" to it }, card.level?.let { "Level" to it }, card.ap?.let { "AP" to it }, card.hp?.let { "HP" to it })
                            .chunked(2).forEach { pair ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    pair.forEach { (label, value) -> StatBox(label, "$value", Modifier.weight(1f)) }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                    }
                }
                body()
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (card.traits.isNotEmpty()) LabelledLine("Traits", card.traits.joinToString(", "))
                    if (card.zones.isNotEmpty()) LabelledLine("Zones", card.zones.joinToString(", "))
                    card.link?.let { LabelledLine("Link", it) }
                    if (card.rarity.isNotBlank() || card.setCode.isNotBlank()) LabelledLine("Set", listOf(card.setCode, card.rarity).filter { it.isNotBlank() }.joinToString(" · "))
                }
                if (!printingsInPanel) printingStrip()
            }
        }
    }
}

/** Card details over the current screen. Wide windows get a side-by-side layout. */
@Composable
fun CardDetailDialog(
    card: Card, printings: List<Printing>, onDismiss: () -> Unit,
    chosenPrinting: String? = null, onChoosePrinting: ((String) -> Unit)? = null,
    extraActions: (@Composable () -> Unit)? = null,
) {
    val arrival = rememberArrival()
    Dialog(onDismissRequest = onDismiss, properties = app.lockon.platform.fullWindowDialogProperties()) {
        app.lockon.platform.LightSystemBarIcons()
        // The dialog fills the window, so clicks around the panel land on this backdrop. It sits behind
        // the panel rather than around it, so clicks inside never reach it. Phones, and any window narrow
        // enough for the floating sheet, get the filter sheet's plain dim (UX rule N4).
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val sheet = app.lockon.platform.isMobile || maxWidth < 600.dp
            Spacer(Modifier.fillMaxSize().graphicsLayer { alpha = arrival.value }
                .then(if (sheet) Modifier.background(Color.Black.copy(alpha = 0.45f)) else Modifier)
                .clickable(interactionSource = null, indication = null, onClick = onDismiss))
        }
        BoxWithConstraints(
            Modifier.fillMaxSize().windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center,
        ) {
        val maxHeight = maxHeight * 0.9f
        // Phones keep a slimmer frame so the card details get the width.
        val narrow = maxWidth < 600.dp
        Surface(
            shape = if (narrow) LockOnShapes.panel else MaterialTheme.shapes.extraLarge,
            color = panelFill(),
            // The cap comes first: after fillMaxWidth the width is already fixed and widthIn can't shrink it.
            // Phones get the same floating sheet as the filters, closed by its button.
            modifier = (if (narrow) Modifier.align(Alignment.BottomCenter).padding(8.dp).widthIn(max = 480.dp).fillMaxWidth() else Modifier.widthIn(max = 980.dp).fillMaxWidth(0.94f))
                .heightIn(max = maxHeight).arrive(arrival).onDialogStage().then(if (narrow) Modifier else Modifier.glassFrame()),
        ) {
            Column(Modifier.padding(start = if (narrow) 16.dp else 24.dp, end = if (narrow) 4.dp else 12.dp, top = if (narrow) 16.dp else 8.dp, bottom = if (narrow) 16.dp else 24.dp)) {
                // The frame's end is slim to leave the details their scroll bar's lane; Close keeps a proper margin.
                if (!narrow) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(card.name, style = if (narrow) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f).padding(end = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    GameButton("Close", onClick = onDismiss, Modifier.padding(end = 12.dp), style = GameButtonStyle.SECONDARY, compact = true)
                }
                CardDetailContent(card, printings, Modifier.weight(1f, fill = false).padding(end = 12.dp, top = 8.dp), actions = extraActions,
                    chosenPrinting = chosenPrinting, onChoosePrinting = onChoosePrinting, showName = narrow)
                if (narrow) GameButton("Close", onClick = onDismiss, Modifier.padding(end = 12.dp, top = 12.dp).fillMaxWidth().height(48.dp), style = GameButtonStyle.SECONDARY)
            }
        }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(
        modifier.clip(MaterialTheme.shapes.small).background(if (isTwilight()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 14.dp, vertical = if (compact) 4.dp else 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = (if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge).merge(TabularNumbers))
    }
}

/** A quiet label and its value on one line. */
@Composable
private fun LabelledLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.width(52.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

/** A narrow [StatBox] for the phone layout, where four share the width beside the card. */
@Composable
private fun StatCell(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.clip(MaterialTheme.shapes.small).background(if (isTwilight()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleSmall.merge(TabularNumbers))
    }
}

/**
 * One printing in the card details: its art and its name on a glass tile. The printing being shown
 * is lit in the accent colour and carries a check.
 */
@Composable
private fun PrintingTile(card: Card, printing: Printing, isShown: Boolean, modifier: Modifier, choosing: Boolean, onClick: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    val tileShape = LockOnShapes.thumb
    // The light moves from the last printing to the picked one instead of switching.
    val lit by animateFloatAsState(if (isShown) 1f else 0f, motion(), label = "printing-lit")
    val accent = Accent
    val label by animateColorAsState(if (isShown) accent else MaterialTheme.colorScheme.onSurface, motion(), label = "printing-label")
    Column(
        modifier.pressScale(press)
            .glass(tileShape)
            .drawBehind {
                if (lit > 0f) {
                    val outline = tileShape.createOutline(size, layoutDirection, this)
                    drawOutline(outline, accent.copy(alpha = 0.16f * lit))
                    if (app.lockon.platform.isMobile) drawOutline(outline, accent.copy(alpha = lit), style = Stroke(2.dp.toPx()))
                    else drawSelectionEdge(tileShape, accent, lit)
                }
            }
            .clickable(press, indication = null, onClickLabel = when {
                isShown -> "View larger"
                choosing -> "Use this printing in the deck"
                else -> "Show this printing"
            }, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .semantics { selected = isShown }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            CardImage(card, Modifier.fillMaxWidth(), printingId = printing.printingId, shadow = false)
            androidx.compose.animation.AnimatedVisibility(
                isShown, Modifier.align(Alignment.TopEnd),
                enter = fadeIn(motion()) + scaleIn(motion(), initialScale = 0.8f), exit = fadeOut(motion(Motion.SHORT)),
            ) {
                Box(
                    Modifier.padding(4.dp).size(20.dp).background(Accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(LineIcons.Check, contentDescription = null, tint = OnAccent, modifier = Modifier.size(14.dp))
                }
            }
        }
        // Two lines, so names like "Parallel LR++" or a promo's set name read in full.
        Text(
            printing.label.removePrefix("Reprint: "), style = MaterialTheme.typography.labelMedium,
            color = label,
            maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Chip(
    text: String, background: Color = MaterialTheme.colorScheme.surfaceVariant,
    foreground: Color = MaterialTheme.colorScheme.onSurfaceVariant, compact: Boolean = false,
) {
    Text(text, style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium, color = foreground,
        modifier = Modifier.clip(MaterialTheme.shapes.small).background(background)
            .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = 4.dp))
}

/** Effect text with 【timing】 and <keyword> markers picked out, as on the printed card. */
@Composable
private fun effectText(text: String): AnnotatedString {
    val primary = MaterialTheme.colorScheme.primary
    return remember(text, primary) {
        buildAnnotatedString {
            var last = 0
            for (m in EffectMarker.findAll(text)) {
                append(text.substring(last, m.range.first))
                withStyle(SpanStyle(color = primary, fontWeight = FontWeight.SemiBold)) { append(m.value) }
                last = m.range.last + 1
            }
            append(text.substring(last))
        }
    }
}

private val EffectMarker = Regex("【[^】]*】|<[^>]*>")

/**
 * − count + for deck strips, list rows and the details dialog: 48 dp targets and one rolling number in
 * [countInk], teal in the deck, quiet at 0 and red past [limit]. + never dims (UX rule S4).
 */
@Composable
fun PlateStepper(quantity: Int, name: String, onMinus: () -> Unit, onPlus: () -> Unit, limit: Int? = null) {
    val haptics = LocalHapticFeedback.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        TileButton("−", "Remove one $name", enabled = quantity > 0) { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onMinus() }
        Box(Modifier.widthIn(min = 30.dp), contentAlignment = Alignment.Center) {
            RollingNumber(quantity, MaterialTheme.typography.titleMedium.merge(TabularNumbers), countInk(quantity, limit), Modifier.bumpOnIncrease(quantity))
        }
        TileButton("+", "Add one $name") { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onPlus() }
    }
}

/**
 * A deck card shown small: the whole card at its real shape (UX rule V4a), so cost and AP/HP stay
 * readable, with its − count + plate underneath (S1). It lifts on hover and flashes when its
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
    val hover = remember { HoverState() }
    val hovered = hover.hovered
    val scheme = MaterialTheme.colorScheme
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth()
                .hoverState(hover)
                .hoverLift(press, CardShape, edge = scheme.primary, lift = 2.dp)
                .flashOnChange(quantity, scheme.primary, scheme.error, CardShape)
                .clip(CardShape)
                .clickable(interactionSource = press, indication = pressWash(CardShape, hover = false), onClickLabel = "Card details", onClick = onClick),
        ) {
            CardImage(card, Modifier.fillMaxWidth(), printingId, shadow = false)
            CardHoverPreview(card, printingId, hovered)
        }
        CardCountBar(quantity, card.name, limit, target, onMinus, onPlus)
    }
}
