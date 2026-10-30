package app.lockon.ui.decks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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

/** Hand cards grow with the panel up to this width, then another column is added. */
private val MAX_HAND_CARD = 176.dp
/** Smaller than this, a card in hand can't be read, so the hand scrolls instead. */
private val MIN_HAND_CARD = 96.dp
private val STATS_HEIGHT = 52.dp
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
    val earlyUnits = handCards.count { it?.type == CardType.UNIT && (it.level ?: 99) <= 2 }
    val bursts = game.shields.count { cards[it]?.displayText?.contains("Burst】") == true }

    Column(modifier) {
    BoxWithConstraints(Modifier.weight(1f)) {
    val inner = maxWidth - 32.dp
    // All six shields share one row under the hand, like the shield zone on the board.
    val shieldWidth = minOf(MAX_SHIELD, (inner - SHIELD_GAP * (SampleHand.SHIELDS - 1)) / SampleHand.SHIELDS)
    // Hand cards fill the width, three across on a phone (UX rule N4) and more as the panel widens,
    // but no taller than lets the opening hand and the shields fit without scrolling.
    val columns = maxOf(3, ((inner + GAP) / (MAX_HAND_CARD + GAP)).toInt() + 1)
    val rows = (SampleHand.HAND_SIZE + columns - 1) / columns
    val besideHand = 32.dp + STATS_HEIGHT + HEADER_HEIGHT * 2 + 12.dp * 4 + shieldWidth / CARD_ASPECT
    val fitWidth = ((maxHeight - besideHand - GAP * (rows - 1)) / rows) * CARD_ASPECT
    val handWidth = minOf((inner - GAP * (columns - 1)) / columns, fitWidth.coerceAtLeast(MIN_HAND_CARD))
    val scroll = rememberScrollState()
    val newest = remember { BringIntoViewRequester() }
    // A drawn card lands at the end of the hand, which may be below the fold: bring it into view.
    LaunchedEffect(game.drawsTaken) { if (game.drawsTaken > 0) newest.bringIntoView() }
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HandStats(game, earlyUnits)
        SectionHeader("Hand", readout = "${game.hand.size}")
        if (!game.shieldsPlaced) {
            Text(
                if (game.canRedraw) "Keep this hand, or redraw once: it goes to the bottom and you draw 5 new cards."
                else "Redrawn. Keep it to set your 6 shields.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(GAP), verticalArrangement = Arrangement.spacedBy(GAP), maxItemsInEachRow = columns) {
            handCards.forEachIndexed { i, card ->
                val drawn = i - SampleHand.HAND_SIZE + 1
                val stagger = if (drawn <= 0) i else 0
                val last = i == handCards.lastIndex && drawn > 0
                Box(Modifier.width(handWidth).then(if (last) Modifier.bringIntoViewRequester(newest) else Modifier).dealIn("$dealId-$i", stagger)) {
                    CardFace(card, Modifier.fillMaxWidth().clickable(enabled = card != null) { detail = card })
                    // Cards drawn after the opening hand say which draw brought them.
                    if (drawn > 0) {
                        Badge("Draw $drawn", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, Modifier.align(Alignment.TopEnd).padding(6.dp))
                    }
                }
            }
        }
        if (game.shieldsPlaced) {
            SectionHeader("Shields", readout = "${game.shields.size}") {
                if (revealShields) Badge("Burst $bursts", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.padding(start = 6.dp))
                GameButton(if (revealShields) "Hide" else "Reveal", onClick = { revealShields = !revealShields }, style = GameButtonStyle.GHOST, compact = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(SHIELD_GAP)) {
                game.shields.forEachIndexed { i, number ->
                    val card = cards[number]
                    FlippingShield(
                        card, revealed = revealShields, index = i,
                        modifier = Modifier.width(shieldWidth).dealIn("$dealId-shield-$i", i).clickable(enabled = revealShields && card != null) { detail = card },
                    )
                }
            }
        }
    }
    VerticalScrollBar(scroll, Modifier.align(Alignment.TopEnd))
    }
    // Actions stay pinned in the thumb zone instead of scrolling away under the cards (UX rule R3).
    HorizontalDivider()
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp + LocalNavClearance.current),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!game.shieldsPlaced) {
            GameButton("Keep", onClick = { game = game.keep() }, Modifier.weight(1f))
            GameButton(if (game.canRedraw) "Redraw" else "Redrawn", onClick = { game = game.redraw(); dealId++ }, Modifier.weight(1f),
                style = GameButtonStyle.SECONDARY, enabled = game.canRedraw)
        } else {
            GameButton(if (game.canDraw) "Draw" else "Deck empty", onClick = { game = game.draw() }, Modifier.weight(1f), enabled = game.canDraw)
        }
        GameButton("New game", onClick = { game = SampleHand.deal(deck); revealShields = false; dealId++ }, style = GameButtonStyle.GHOST)
    }
    }
    detail?.let { CardDetailDialog(it, emptyList(), onDismiss = { detail = null }) }
}

/**
 * The game so far as instrument readouts (UX rule I1): cards left in the deck, draws taken, and units
 * at level 2 or lower in hand, which turns red with none, since that hand has nothing to play early.
 */
@Composable
private fun HandStats(game: SampleHand, earlyUnits: Int) {
    val scheme = MaterialTheme.colorScheme
    val stats = listOf(
        Triple("${game.library.size}", "In deck", scheme.onSurface),
        Triple("${game.drawsTaken}", "Draws", scheme.onSurface),
        Triple("$earlyUnits", "Units Lv.1–2", if (earlyUnits == 0) scheme.error else scheme.primary),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        stats.forEach { (value, label, color) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, style = MaterialTheme.typography.titleMedium.merge(TabularNumbers), color = color, maxLines = 1)
                Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
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
