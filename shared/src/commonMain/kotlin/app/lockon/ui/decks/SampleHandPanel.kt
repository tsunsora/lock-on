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
/** The test hand is always three cards across (UX rule N4), so the opening five take two rows. */
private const val HAND_COLUMNS = 3
private const val OPENING_ROWS = 2
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
    // A phone's panel sits 8 dp out from its cards, so three whole cards keep their names even at 360 dp.
    val panelPad = if (phone) 8.dp else 0.dp
    val inner = maxWidth - PAGE * 2 - panelPad * 2
    val byWidth = (inner - GAP * (HAND_COLUMNS - 1)) / HAND_COLUMNS
    // All six shields share one row under the hand, like the shield zone on the board.
    val shieldsAcross = { row: Dp -> (row - GAP * (SampleHand.SHIELDS - 1)) / SampleHand.SHIELDS }
    val firstShield = if (phone) shieldsAcross(inner) else minOf(MAX_SHIELD, shieldsAcross(inner))
    // Everything on the page but the hand's two rows, so the opening hand and the shields fit without scrolling.
    val around = (if (phone) PAGE_TOP + 8.dp + panelPad * 2 else PAGE * 2) + HEADER_HEIGHT * 2 + GAP * 2 + 12.dp +
        GAP * (OPENING_ROWS - 1) + firstShield / CARD_ASPECT
    val byHeight = (maxHeight - around) / OPENING_ROWS * CARD_ASPECT
    val cardWidth = minOf(byWidth, byHeight, MAX_HAND_CARD).coerceAtLeast(MIN_HAND_CARD)
    val gridWidth = cardWidth * HAND_COLUMNS + GAP * (HAND_COLUMNS - 1)
    val shieldWidth = if (phone) shieldsAcross(inner) else minOf(MAX_SHIELD, shieldsAcross(gridWidth))
    val scroll = rememberScrollState()
    val newest = remember { BringIntoViewRequester() }
    // A drawn card lands at the end of the hand, which may be below the fold: bring it into view.
    LaunchedEffect(game.drawsTaken) { if (game.drawsTaken > 0) newest.bringIntoView() }
    val hand = @Composable {
        Column(Modifier.width(gridWidth), verticalArrangement = Arrangement.spacedBy(GAP)) {
            handCards.withIndex().chunked(HAND_COLUMNS).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
                    row.forEach { (i, card) ->
                        val drawn = i - SampleHand.HAND_SIZE + 1
                        val stagger = if (drawn <= 0) i else 0
                        val last = i == handCards.lastIndex && drawn > 0
                        Box(Modifier.width(cardWidth).then(if (last) Modifier.bringIntoViewRequester(newest) else Modifier).dealIn("$dealId-$i", stagger)) {
                            CardFace(card, Modifier.fillMaxWidth().clickable(enabled = card != null) { detail = card })
                            // Cards drawn after the opening hand say which draw brought them, in the corner opposite the cost.
                            if (drawn > 0) {
                                Badge("Draw $drawn", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, Modifier.align(Alignment.TopEnd).padding(6.dp))
                            }
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
