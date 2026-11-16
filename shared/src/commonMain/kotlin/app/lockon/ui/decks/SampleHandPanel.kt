package app.lockon.ui.decks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import app.lockon.ui.components.glide
import app.lockon.ui.components.LineIcons
import app.lockon.ui.components.revealOut
import app.lockon.ui.components.revealIn
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lockon.model.Card
import app.lockon.model.CardType
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.rules.SampleHand
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import app.lockon.ui.components.Badge
import app.lockon.ui.components.CARD_ASPECT
import app.lockon.ui.components.CardDetailDialog
import app.lockon.ui.components.CardFace
import app.lockon.ui.components.EmptyState
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.ControlSize
import app.lockon.ui.components.LocalNavClearance
import app.lockon.ui.components.LocalTopBand
import app.lockon.ui.components.SoftTile
import androidx.compose.foundation.layout.heightIn
import app.lockon.ui.components.Motion
import app.lockon.ui.components.SectionHeader
import app.lockon.ui.components.VerticalScrollBar
import app.lockon.ui.components.dealIn
import app.lockon.ui.components.motion
import app.lockon.ui.theme.TabularNumbers

/** Hand cards grow with the panel up to this width. */
private val MAX_HAND_CARD = 200.dp
/** Below this a card face drops its name and AP / HP (see PlaceholderCard), so the hand scrolls instead. */
private val MIN_HAND_CARD = 96.dp
/** Past this many cards a fan can't show each one, so later draws start a new fan under it. */
private const val FAN_SIZE = 10
/** How far apart neighbouring cards tilt, in degrees, and sit lower toward the ends. */
private const val FAN_TILT = 4f
/** A phone's fan tilts less, so its end cards swing out less and every card shows more of itself. */
private const val PHONE_FAN_TILT = 3f
private val FAN_STEP_DROP = 6.dp
/** How far the card under the pointer rises, as a share of its height; the fan keeps this much room above it. */
private const val FAN_LIFT = 0.08f
/** Under a finger the card rises further, so its top shows above the fingertip while you slide along the fan. */
private const val FAN_TOUCH_LIFT = 0.2f
/** The raised card grows this much, and its neighbours lean this far away from it. */
private const val FAN_LIFT_SCALE = 1.04f
private val FAN_NUDGE = 10.dp
private val PAGE = 16.dp
private val PAGE_TOP = 8.dp
private val HEADER_HEIGHT = 32.dp
/** Shields are face down most of the time, so on a wide panel they stay small and the hand gets the height. */
private val MAX_SHIELD = 64.dp
private val GAP = 8.dp

/** Practice opening hands with the official redraw and shield rules. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SampleHandPanel(deck: DeckSnapshot, cards: Map<String, Card>, modifier: Modifier = Modifier) {
    val mainCount = deck.count(DeckSection.MAIN)
    if (mainCount < SampleHand.HAND_SIZE + SampleHand.SHIELDS) {
        EmptyState("Not enough cards to test", "Add at least ${SampleHand.HAND_SIZE + SampleHand.SHIELDS} main-deck cards to draw a hand and set shields.", modifier)
        return
    }
    // A new deal whenever the deck's main section changes, so the test always matches the list.
    val mainKey = deck.entries.filter { it.section == DeckSection.MAIN }
    var game by remember(mainKey) { mutableStateOf(SampleHand.deal(deck)) }
    var revealShields by remember(mainKey) { mutableStateOf(false) }
    // A new deal (new game or redraw) re-deals every card; a draw only animates the new one.
    var dealId by remember(mainKey) { mutableStateOf(0) }
    var detail by remember { mutableStateOf<Card?>(null) }

    val handCards = game.hand.map { cards[it] }
    val bursts = game.shields.count { cards[it]?.displayText?.contains("Burst】") == true }

    // On a phone the hand and shields share one solid panel; on both, the buttons stay at the bottom under the thumb.
    val phone = LocalTopBand.current.height == 0.dp
    val tall = if (phone) Modifier.height(ControlSize) else Modifier
    val buttons = @Composable { rowModifier: Modifier ->
        Row(rowModifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Until the hand is kept the choice is Keep or Redraw, as the rules go; after that it is Draw.
            GameButton("Redraw", onClick = { game = game.redraw(); dealId++ }, Modifier.weight(1f).then(tall), style = GameButtonStyle.SECONDARY, enabled = game.canRedraw, icon = if (phone) null else LineIcons.Hand, compact = true)
            if (!game.shieldsPlaced) GameButton("Keep", onClick = { game = game.keep() }, Modifier.weight(1f).then(tall), compact = true)
            else GameButton(if (game.canDraw) "Draw" else "Deck empty", onClick = { game = game.draw() }, Modifier.weight(1f).then(tall), enabled = game.canDraw, icon = LineIcons.Draw, compact = true)
            GameButton("Restart", onClick = { game = SampleHand.deal(deck); revealShields = false; dealId++ }, Modifier.weight(1f).then(tall), style = GameButtonStyle.SECONDARY, icon = if (phone) null else LineIcons.Shuffle, compact = true)
        }
    }

    Column(modifier) {
    BoxWithConstraints(Modifier.weight(1f)) {
    val panelPad = if (phone) 8.dp else 0.dp
    val inner = maxWidth - PAGE * 2 - panelPad * 2
    // All six shields share one row under the hand, like the shield zone on the board.
    val shieldsAcross = { row: Dp -> (row - GAP * (SampleHand.SHIELDS - 1)) / SampleHand.SHIELDS }
    val shieldWidth = if (phone) shieldsAcross(inner) else minOf(MAX_SHIELD, shieldsAcross(inner))
    // Everything on the page but the fan, so the opening hand and the shields fit without scrolling.
    val around = (if (phone) PAGE_TOP + 8.dp + panelPad * 2 else PAGE * 2) + HEADER_HEIGHT * 2 + GAP * 2 + 12.dp +
        shieldWidth / CARD_ASPECT
    // The opening fan is a card's height, plus its drop toward the ends and its end cards' tilted corners.
    val tilt = if (phone) PHONE_FAN_TILT else FAN_TILT
    val openingMiddle = (SampleHand.HAND_SIZE - 1) / 2f
    val swing = sin(minOf(tilt, 16f / openingMiddle) * openingMiddle * PI.toFloat() / 180f)
    val byHeight = (maxHeight - around - FAN_STEP_DROP * openingMiddle) / ((1f + FAN_LIFT) / CARD_ASPECT + swing)
    // Cards take about a third of the width (more on a phone), but no taller than lets the fan and shields fit.
    val cardWidth = minOf(inner * (if (phone) 0.4f else 0.3f), byHeight, MAX_HAND_CARD).coerceAtLeast(MIN_HAND_CARD)
    val scroll = rememberScrollState()
    val newest = remember { BringIntoViewRequester() }
    // A drawn card lands at the end of the hand, which may be below the fold: bring it into view.
    LaunchedEffect(game.drawsTaken) { if (game.drawsTaken > 0) newest.bringIntoView() }
    val hand = @Composable {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GAP)) {
            handCards.withIndex().chunked(FAN_SIZE).forEach { fan ->
                HandFan(fan.size, cardWidth, inner, tilt, onOpen = { at -> fan[at].value?.let { detail = it } }) { at, place ->
                    val (i, card) = fan[at]
                    val drawn = i - SampleHand.HAND_SIZE + 1
                    val stagger = if (drawn <= 0) i else 0
                    val last = i == handCards.lastIndex && drawn > 0
                    Box(place.then(if (last) Modifier.bringIntoViewRequester(newest) else Modifier).dealIn("$dealId-$i", stagger)) {
                        CardFace(card, Modifier.fillMaxWidth())
                        // Cards drawn after the opening hand say which draw brought them; the corner stays in view under the next card.
                        if (drawn > 0) {
                            Badge("Draw $drawn", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, Modifier.align(Alignment.BottomStart).padding(6.dp))
                        }
                    }
                }
            }
        }
    }
    val shields = @Composable {
        SectionHeader("Shields", Modifier.height(HEADER_HEIGHT), readout = "${SampleHand.SHIELDS}") {
            app.lockon.ui.components.Reveal(revealShields, sideways = true) {
                Badge("Burst $bursts", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.padding(start = 8.dp))
            }
            // Revealing keeps the hand, since that is when the shields are set.
            GameButton(if (revealShields) "Hide" else "Reveal", onClick = { game = game.keep(); revealShields = !revealShields }, style = GameButtonStyle.GHOST, compact = true)
        }
        Row(Modifier.padding(top = GAP), horizontalArrangement = Arrangement.spacedBy(GAP)) {
            repeat(SampleHand.SHIELDS) { i ->
                val card = game.shields.getOrNull(i)?.let { cards[it] }
                FlippingShield(
                    card, revealed = revealShields, index = i,
                    modifier = Modifier.width(shieldWidth).dealIn("$dealId-shield-$i", i).clickable(enabled = revealShields && card != null) { detail = card },
                )
            }
        }
    }
    val handHeader = @Composable { m: Modifier ->
        SectionHeader("Hand", m.height(HEADER_HEIGHT), readout = "${game.hand.size}") { HandStats(game) }
    }
    if (phone) Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(start = PAGE, end = PAGE, top = PAGE_TOP, bottom = 8.dp)) {
        SoftTile(Modifier.fillMaxWidth(), solid = true, padding = panelPad) {
            handHeader(Modifier.padding(horizontal = 4.dp))
            Box(Modifier.padding(top = GAP)) { hand() }
            Column(Modifier.padding(top = 12.dp)) { shields() }
        }
    } else Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(PAGE)) {
        handHeader(Modifier)
        Box(Modifier.padding(top = GAP)) { hand() }
        Column(Modifier.padding(top = 12.dp)) { shields() }
    }
    VerticalScrollBar(scroll, Modifier.align(Alignment.TopEnd))
    }
    buttons(Modifier.padding(start = PAGE, end = PAGE, top = if (phone) 0.dp else 4.dp, bottom = if (phone) LocalNavClearance.current + 4.dp else PAGE))
    }
    detail?.let { CardDetailDialog(it, emptyList(), onDismiss = { detail = null }) }
}

/**
 * A hand held in a fan, centred in [width]: each card overlaps the one before and tilts from a point
 * below the hand, and a tap opens any card whole (UX rule V4a).
 *
 * Which card the pointer is on comes from the fan at rest, not from what is drawn, so a raised card
 * never steals the pointer from its neighbours: each card answers on the part of it that shows, and
 * the card in front wins where two overlap. The card under the mouse rises and its neighbours lean
 * away. A finger raises the card it presses and can slide along the fan to raise the others, and
 * lifting it opens the raised card; a drag up or down scrolls the page as before. With a keyboard,
 * Tab raises each card in turn and Enter opens it.
 */
@Composable
private fun HandFan(count: Int, cardWidth: Dp, width: Dp, maxTilt: Float, onOpen: (Int) -> Unit, card: @Composable (Int, Modifier) -> Unit) {
    val cardHeight = cardWidth / CARD_ASPECT
    val middle = (count - 1) / 2f
    // A card joining the fan makes room: the others ease to their new places rather than jumping there.
    val easedMiddle by animateFloatAsState(middle, glide(), label = "fan-middle")
    val tiltTarget = if (count > 1) minOf(maxTilt, 16f / middle) else 0f
    val tilt by animateFloatAsState(tiltTarget, glide(), label = "fan-tilt")
    val widest = tiltTarget * middle
    // The end cards' top corners swing out past their own width.
    val swing = cardHeight * 1.2f * sin(widest * PI.toFloat() / 180f)
    val stepTarget = if (count > 1) minOf(cardWidth * 0.62f, (width - cardWidth - swing * 2) / (count - 1)).coerceAtLeast(0.dp) else 0.dp
    val startTarget = (width - cardWidth - stepTarget * (count - 1)) / 2
    val rimTarget = cardWidth / 2 * sin(widest * PI.toFloat() / 180f)
    // Room above the fan for the raised card, so it doesn't cover the header.
    val headroom = cardHeight * FAN_LIFT
    val step by animateDpAsState(stepTarget, glide(), label = "fan-step")
    val start by animateDpAsState(startTarget, glide(), label = "fan-start")
    val rim by animateDpAsState(rimTarget, glide(), label = "fan-rim")

    // The card the mouse or a finger is on, the one keyboard focus is on, and whether a finger is down.
    var pointed by remember { mutableStateOf<Int?>(null) }
    var keyed by remember { mutableStateOf<Int?>(null) }
    var touching by remember { mutableStateOf(false) }
    val raised = pointed?.takeIf { it < count } ?: keyed?.takeIf { it < count }

    val density = LocalDensity.current
    val rest = with(density) {
        FanRest(count, cardWidth.toPx(), cardHeight.toPx(), startTarget.toPx(), stepTarget.toPx(),
            (rimTarget + headroom).toPx(), FAN_STEP_DROP.toPx(), tiltTarget, middle)
    }
    val restNow by rememberUpdatedState(rest)
    val open by rememberUpdatedState(onOpen)

    Box(
        Modifier.fillMaxWidth().height(headroom + cardHeight + FAN_STEP_DROP * middle + rimTarget * 2)
            // A mouse resting on the fan raises the card under it.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.type == PointerType.Touch || change.pressed) continue
                        when (event.type) {
                            PointerEventType.Move, PointerEventType.Enter -> pointed = restNow.cardAt(change.position)
                            PointerEventType.Exit -> pointed = null
                        }
                    }
                }
            }
            // A press raises its card, sliding sideways moves along the fan, and letting go opens the raised card.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (down.type == PointerType.Mouse && !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                    val first = restNow.cardAt(down.position) ?: return@awaitEachGesture
                    val finger = down.type != PointerType.Mouse
                    touching = finger
                    pointed = first
                    var sliding = false
                    var travel = Offset.Zero
                    var opens = false
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) { opens = true; break }
                        // The page's scroll took the drag.
                        if (change.isConsumed) break
                        travel += change.positionChange()
                        if (!sliding) {
                            if (abs(travel.x) > viewConfiguration.touchSlop && abs(travel.x) > abs(travel.y)) sliding = true
                            else if (abs(travel.y) > viewConfiguration.touchSlop) break
                        }
                        if (sliding) {
                            change.consume()
                            restNow.cardAt(change.position)?.let { pointed = it }
                        }
                    }
                    val chosen = pointed
                    touching = false
                    if (finger) pointed = null
                    if (opens && chosen != null) open(chosen)
                }
            },
    ) {
        repeat(count) { i ->
            val from = i - easedMiddle
            val up = i == raised
            val lift by animateDpAsState(if (up) cardHeight * (if (touching) FAN_TOUCH_LIFT else FAN_LIFT) else 0.dp, motion(Motion.SHORT), label = "fan-lift")
            val grow by animateFloatAsState(if (up) FAN_LIFT_SCALE else 1f, motion(Motion.SHORT), label = "fan-grow")
            val lean = raised?.let { FAN_NUDGE * (i - it).coerceIn(-1, 1) } ?: 0.dp
            val nudge by animateDpAsState(lean, motion(Motion.SHORT), label = "fan-nudge")
            card(
                i,
                Modifier.zIndex(if (up) 1f else 0f).width(cardWidth)
                    .offset(start + step * i + nudge, headroom + rim + FAN_STEP_DROP * abs(from) - lift)
                    .graphicsLayer {
                        rotationZ = tilt * from
                        transformOrigin = TransformOrigin(0.5f, 1.2f)
                        scaleX = grow
                        scaleY = grow
                    }
                    .onFocusChanged { if (it.isFocused) keyed = i else if (keyed == i) keyed = null }
                    .onPreviewKeyEvent { key ->
                        val press = key.key == Key.Enter || key.key == Key.NumPadEnter || key.key == Key.Spacebar
                        if (press && key.type == KeyEventType.KeyUp) open(i)
                        press
                    }
                    .semantics { role = Role.Button; onClick { open(i); true } }
                    .focusable(),
            )
        }
    }
}

/** The fan at rest in pixels, for finding the card under a point. Card [i] sits at [start] + [step] × i. */
private class FanRest(
    val count: Int, val width: Float, val height: Float, val start: Float, val step: Float,
    val top: Float, val drop: Float, val tilt: Float, val middle: Float,
) {
    /** The front-most card whose outline holds [point] once each card's tilt is undone, or null between cards. */
    fun cardAt(point: Offset): Int? {
        for (i in count - 1 downTo 0) {
            val from = i - middle
            val left = start + step * i
            val cardTop = top + drop * abs(from)
            // Each card turns about a point 1.2 heights below its top, at its middle.
            val pivotX = left + width / 2
            val pivotY = cardTop + height * 1.2f
            val angle = -tilt * from * PI.toFloat() / 180f
            val dx = point.x - pivotX
            val dy = point.y - pivotY
            val x = dx * cos(angle) - dy * sin(angle) + width / 2
            val y = dx * sin(angle) + dy * cos(angle) + height * 1.2f
            if (x in 0f..width && y in 0f..height) return i
        }
        return null
    }
}

/** The game so far: cards left once the shields are set, and which turn it is. */
@Composable
private fun HandStats(game: SampleHand) {
    val scheme = MaterialTheme.colorScheme
    val left = game.library.size - if (game.shieldsPlaced) 0 else SampleHand.SHIELDS
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(LineIcons.Decks to "$left left", LineIcons.Flag to "Turn ${game.drawsTaken + 1}").forEach { (icon, text) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, Modifier.size(16.dp), tint = scheme.onSurfaceVariant)
                Text(text, style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), maxLines = 1)
            }
        }
    }
}

/** A shield that turns over when revealed, one after another (UX rule V5: motion shows what changed). */
@Composable
private fun FlippingShield(card: Card?, revealed: Boolean, index: Int, modifier: Modifier = Modifier) {
    val angle by animateFloatAsState(
        targetValue = if (revealed) 180f else 0f,
        animationSpec = motion(Motion.LONG, delayMillis = index * Motion.STAGGER),
        label = "shield-flip",
    )
    val showingFace = angle > 90f
    CardFace(
        card,
        modifier.graphicsLayer {
            rotationY = angle
            cameraDistance = 12f * density
        }.graphicsLayer { if (showingFace) rotationY = 180f },
        faceDown = !showingFace,
    )
}
