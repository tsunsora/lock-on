package app.lockon.ui.decks
import androidx.compose.ui.Alignment
import app.lockon.ui.components.VerticalScrollBar
import app.lockon.ui.components.LocalNavClearance
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lockon.model.Card
import app.lockon.model.CardType
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.rules.SampleHand
import app.lockon.ui.components.CardDetailDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import app.lockon.ui.components.CardFace
import app.lockon.ui.components.Motion
import app.lockon.ui.components.dealIn
import app.lockon.ui.components.motion
import app.lockon.ui.components.EmptyState
import app.lockon.ui.components.SectionHeader
import app.lockon.ui.theme.TabularNumbers

/** Practice opening hands with the official redraw and shield rules. */
@OptIn(ExperimentalLayoutApi::class)
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

    Column(modifier) {
    BoxWithConstraints(Modifier.weight(1f)) {
    // Up to 132 dp a card, but always three across, so a phone shows the whole 5-card hand.
    val handWidth = minOf(132.dp, (maxWidth - 32.dp - 16.dp) / 3)
    val shieldWidth = minOf(100.dp, (maxWidth - 32.dp - 16.dp) / 3)
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Hand", readout = "${game.hand.size}")
        Text(
            when {
                !game.shieldsPlaced && game.canRedraw -> "Opening hand. Keep it, or redraw once: the hand goes to the bottom and you draw 5 new cards."
                !game.shieldsPlaced -> "Redrawn hand. Keep it to set your 6 shields."
                else -> "Turn draws taken: ${game.drawsTaken}. ${game.library.size} cards left in the deck."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Units at level 2 or lower in hand: $earlyUnits",
            style = MaterialTheme.typography.bodyMedium.merge(TabularNumbers),
            color = if (earlyUnits == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            handCards.forEachIndexed { i, card ->
                val stagger = if (i < SampleHand.HAND_SIZE) i else 0
                CardFace(card, Modifier.width(handWidth).dealIn("$dealId-$i", stagger).clickable(enabled = card != null) { detail = card })
            }
        }
        if (game.shieldsPlaced) {
            SectionHeader("Shields", readout = "${game.shields.size}") {
                GameButton(if (revealShields) "Hide" else "Reveal", onClick = { revealShields = !revealShields }, style = GameButtonStyle.GHOST, compact = true)
            }
            val bursts = game.shields.count { cards[it]?.displayText?.contains("Burst】") == true }
            if (revealShields) {
                Text("Burst cards in shields: $bursts", style = MaterialTheme.typography.bodySmall.merge(TabularNumbers))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                game.shields.forEachIndexed { i, number ->
                    FlippingShield(cards[number], revealed = revealShields, index = i, modifier = Modifier.width(shieldWidth).dealIn("$dealId-shield-$i", i))
                }
            }
        }
    }
    VerticalScrollBar(scroll, Modifier.align(Alignment.TopEnd))
    }
    // Actions stay pinned in the thumb zone instead of scrolling away under the cards (UX rule R3).
    HorizontalDivider()
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp + LocalNavClearance.current), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!game.shieldsPlaced) {
            GameButton("Keep", onClick = { game = game.keep() })
            GameButton("Redraw", onClick = { game = game.redraw(); dealId++ }, style = GameButtonStyle.SECONDARY, enabled = game.canRedraw)
        } else {
            GameButton("Draw", onClick = { game = game.draw() }, enabled = game.canDraw)
        }
        GameButton("New game", onClick = { game = SampleHand.deal(deck); revealShields = false; dealId++ }, style = GameButtonStyle.GHOST, compact = true)
    }
    }
    detail?.let { CardDetailDialog(it, emptyList(), onDismiss = { detail = null }) }
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
