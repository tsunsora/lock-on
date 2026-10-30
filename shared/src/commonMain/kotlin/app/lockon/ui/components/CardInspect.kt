package app.lockon.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lockon.model.Card
import app.lockon.ui.theme.cardColor
import kotlin.math.hypot

/** How far the card turns toward the pointer at its edge, in degrees. */
private const val MAX_TILT = 14f

/**
 * One card as large as the window allows, over a dimmed screen, to look at like a card in the hand:
 * it turns toward the pointer (or a finger dragged across it) and catches the light where it points.
 * Clicking around it or Esc closes it.
 */
@Composable
fun CardInspectDialog(card: Card, printingId: String?, onDismiss: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val spec = motion<Float>(Motion.LONG)
    val appear = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spec) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().graphicsLayer { alpha = appear.value }, contentAlignment = Alignment.Center) {
            // Clicks around the card land on this backdrop, which sits behind it.
            Spacer(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f))
                .clickable(interactionSource = null, indication = null, onClick = onDismiss))
            // Room is left under the card for its name.
            val width = minOf(maxWidth * 0.9f, (maxHeight - 112.dp) * CARD_ASPECT)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                TiltingCard(card, printingId, Modifier.width(width).graphicsLayer {
                    val s = 0.94f + 0.06f * appear.value
                    scaleX = s; scaleY = s
                })
                Text(card.name, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

/**
 * A card that turns toward the pointer while it's over the card, and back to flat when it leaves or
 * a finger lifts. [tilt], from −1 to 1 on each axis, holds it at a fixed angle instead (for previews).
 * With reduced motion it stays flat.
 */
@Composable
fun TiltingCard(card: Card, printingId: String?, modifier: Modifier = Modifier, tilt: Offset? = null) {
    val reduce = LocalReduceMotion.current
    var aim by remember { mutableStateOf(Offset.Zero) }
    val target = if (reduce) Offset.Zero else tilt ?: aim
    // A soft spring, so the card follows the hand with a little weight instead of snapping to it.
    val follow = if (tilt != null) snap() else spring<Float>(dampingRatio = 0.8f, stiffness = 180f)
    val x by animateFloatAsState(target.x, follow, label = "tilt-x")
    val y by animateFloatAsState(target.y, follow, label = "tilt-y")
    val pool = cardColor(card.colors.firstOrNull())
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier.aspectRatio(CARD_ASPECT)
            // Before the layer, so positions are read on the flat card rather than the turned one.
            .pointerInput(reduce) {
                if (reduce) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        val touch = change.type == PointerType.Touch
                        aim = if (event.type == PointerEventType.Exit || (touch && event.type == PointerEventType.Release)) {
                            Offset.Zero
                        } else {
                            Offset(
                                (change.position.x / size.width * 2f - 1f).coerceIn(-1f, 1f),
                                (change.position.y / size.height * 2f - 1f).coerceIn(-1f, 1f),
                            )
                        }
                        // A finger dragging the card turns it rather than scrolling anything behind.
                        if (touch) change.consume()
                    }
                }
            }
            .graphicsLayer {
                // The side under the pointer comes toward you.
                rotationY = -x * MAX_TILT
                rotationX = y * MAX_TILT
                cameraDistance = 14f * density
            }
            .clearAndSetSemantics {},
    ) {
        // The drawn placeholder face is partly see-through, so it gets a solid backing.
        CardImage(card, Modifier.fillMaxSize().shadow(32.dp, CardShape, ambientColor = pool, spotColor = pool).background(surface, CardShape),
            printingId, shadow = false)
        // Light catches the card where the pointer is, brighter the further it turns, and its rim glows in its colour.
        Box(
            Modifier.matchParentSize().clip(CardShape).drawBehind {
                val strength = hypot(x, y).coerceAtMost(1f)
                drawRect(Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.06f + 0.24f * strength), Color.White.copy(alpha = 0f)),
                    center = Offset((x + 1f) / 2f * size.width, (y + 1f) / 2f * size.height),
                    radius = size.maxDimension * 0.65f,
                ))
            }.border(BorderStroke(2.dp, Brush.linearGradient(listOf(lighten(pool, 0.55f), pool.copy(alpha = 0.5f), lighten(pool, 0.3f)))), CardShape),
        )
    }
}
