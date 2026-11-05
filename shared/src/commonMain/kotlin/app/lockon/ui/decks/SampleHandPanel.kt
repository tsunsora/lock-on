package app.lockon.ui.decks

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
import app.lockon.ui.components.GameIcons
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
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
import app.lockon.ui.components.LocalNavClearance
import app.lockon.ui.components.Motion
import app.lockon.ui.components.SectionHeader
import app.lockon.ui.components.VerticalScrollBar
import app.lockon.ui.components.dealIn
import app.lockon.ui.components.motion
import app.lockon.ui.theme.TabularNumbers

/** Hand cards grow with the panel up to this width. */
private val MAX_HAND_CARD = 176.dp
/** Smaller than this, a card in hand can't be read, so the hand scrolls instead. */
private val MIN_HAND_CARD = 96.dp
/** Past this many cards a fan can't show each one, so later draws start a new fan under it. */
private const val FAN_SIZE = 10
/** How far apart neighbouring cards tilt, in degrees, and sit lower toward the ends. */
private const val FAN_TILT = 4f
private val FAN_STEP_DROP = 6.dp
private val STATS_HEIGHT = 24.dp
private val HEADER_HEIGHT = 30.dp
/** Shields are face down most of the time, so on a wide panel they stay small and the hand gets the height. */
private val MAX_SHIELD = 64.dp
private val GAP = 8.dp
private val SHIELD_GAP = 6.dp

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

    Column(modifier) {
    BoxWithConstraints(Modifier.weight(1f)) {
    val inner = maxWidth - 32.dp
    // All six shields share one row under the hand, like the shield zone on the board.
    val shieldWidth = minOf(MAX_SHIELD, (inner - SHIELD_GAP * (SampleHand.SHIELDS - 1)) / SampleHand.SHIELDS)
    // Cards take about a third of the width, but no taller than lets the opening hand's fan and the
    // shields fit without scrolling.
    val besideHand = 32.dp + STATS_HEIGHT + HEADER_HEIGHT * 2 + 12.dp * 5 + shieldWidth / CARD_ASPECT + 36.dp + 44.dp
    val fitWidth = (maxHeight - besideHand) * CARD_ASPECT
    val handWidth = minOf(inner * 0.3f, MAX_HAND_CARD, fitWidth.coerceAtLeast(MIN_HAND_CARD))
    val scroll = rememberScrollState()
    val newest = remember { BringIntoViewRequester() }
    // A drawn card lands at the end of the hand, which may be below the fold: bring it into view.
    LaunchedEffect(game.drawsTaken) { if (game.drawsTaken > 0) newest.bringIntoView() }
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HandStats(game)
        SectionHeader("Hand", readout = "${game.hand.size}")
        handCards.withIndex().chunked(FAN_SIZE).forEach { fan ->
            HandFan(fan.size, handWidth, inner) { at, place ->
                val (i, card) = fan[at]
                val drawn = i - SampleHand.HAND_SIZE + 1
                val stagger = if (drawn <= 0) i else 0
                val last = i == handCards.lastIndex && drawn > 0
                Box(place.then(if (last) Modifier.bringIntoViewRequester(newest) else Modifier).dealIn("$dealId-$i", stagger)) {
                    CardFace(card, Modifier.fillMaxWidth().clickable(enabled = card != null) { detail = card })
                    // Cards drawn after the opening hand say which draw brought them; the corner stays in view under the next card.
                    if (drawn > 0) {
                        Badge("Draw $drawn", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, Modifier.align(Alignment.BottomStart).padding(6.dp))
                    }
                }
            }
        }
        SectionHeader("Shields", readout = "${SampleHand.SHIELDS}") {
            if (revealShields) Badge("Burst $bursts", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.padding(start = 6.dp))
            // Revealing keeps the hand, since that is when the shields are set.
            GameButton(if (revealShields) "Hide" else "Reveal", onClick = { game = game.keep(); revealShields = !revealShields }, style = GameButtonStyle.GHOST, compact = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SHIELD_GAP)) {
            repeat(SampleHand.SHIELDS) { i ->
                val card = game.shields.getOrNull(i)?.let { cards[it] }
                FlippingShield(
                    card, revealed = revealShields, index = i,
                    modifier = Modifier.width(shieldWidth).dealIn("$dealId-shield-$i", i).clickable(enabled = revealShields && card != null) { detail = card },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp, bottom = LocalNavClearance.current),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The first draw keeps the hand and sets the shields, as the rules go.
            GameButton("Redraw", onClick = { game = game.redraw(); dealId++ }, Modifier.weight(1f), style = GameButtonStyle.SECONDARY, enabled = game.canRedraw, icon = LineIcons.Hand, compact = true)
            GameButton(if (game.canDraw) "Draw" else "Deck empty", onClick = { game = game.draw() }, Modifier.weight(1f), enabled = game.canDraw, icon = LineIcons.Draw, compact = true)
            GameButton("New game", onClick = { game = SampleHand.deal(deck); revealShields = false; dealId++ }, Modifier.weight(1f), style = GameButtonStyle.SECONDARY, icon = if (app.lockon.ui.components.LocalTopBand.current.height == 0.dp) null else LineIcons.Shuffle, compact = true)
        }
    }
    VerticalScrollBar(scroll, Modifier.align(Alignment.TopEnd))
    }
    }
    detail?.let { CardDetailDialog(it, emptyList(), onDismiss = { detail = null }) }
}

/**
 * A hand held in a fan, centred in [width]: each card overlaps the one before and tilts from a point
 * below the hand. The card under the pointer comes to the front, so a covered card can be read.
 */
@Composable
private fun HandFan(count: Int, cardWidth: Dp, width: Dp, card: @Composable (Int, Modifier) -> Unit) {
    val cardHeight = cardWidth / CARD_ASPECT
    val middle = (count - 1) / 2f
    val tilt = if (count > 1) minOf(FAN_TILT, 16f / middle) else 0f
    val widest = tilt * middle
    // The end cards' top corners swing out past their own width.
    val swing = cardHeight * 1.2f * sin(widest * PI.toFloat() / 180f)
    val step = if (count > 1) minOf(cardWidth * 0.62f, (width - cardWidth - swing * 2) / (count - 1)).coerceAtLeast(0.dp) else 0.dp
    val start = (width - cardWidth - step * (count - 1)) / 2
    val rim = cardWidth / 2 * sin(widest * PI.toFloat() / 180f)
    Box(Modifier.fillMaxWidth().height(cardHeight + FAN_STEP_DROP * middle + rim * 2)) {
        repeat(count) { i ->
            val from = i - middle
            val hover = remember { MutableInteractionSource() }
            val hovered by hover.collectIsHoveredAsState()
            card(
                i,
                Modifier.zIndex(if (hovered) 1f else 0f).width(cardWidth)
                    .offset(start + step * i, rim + FAN_STEP_DROP * abs(from))
                    .hoverable(hover)
                    .graphicsLayer { rotationZ = tilt * from; transformOrigin = TransformOrigin(0.5f, 1.2f) },
            )
        }
    }
}

/** The game so far: cards left once the shields are set, and which turn it is. */
@Composable
private fun HandStats(game: SampleHand) {
    val scheme = MaterialTheme.colorScheme
    val left = game.library.size - if (game.shieldsPlaced) 0 else SampleHand.SHIELDS
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(LineIcons.Decks to "$left left", LineIcons.Flag to "Turn ${game.drawsTaken + 1}").forEach { (icon, text) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
