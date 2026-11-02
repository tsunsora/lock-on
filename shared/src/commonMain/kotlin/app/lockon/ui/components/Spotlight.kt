package app.lockon.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * A light that follows the pointer across a group of glass tiles: the tile under it brightens toward
 * the pointer, and the rims of its neighbours catch the edge of the light, so the whole group reads
 * as one surface (after Hyperplexed's card glow). On phones it shows under a finger while pressed.
 */
@Stable
class Spotlight internal constructor(private val scope: CoroutineScope) {
    internal var area: LayoutCoordinates? = null
    /** Where the pointer is, in root coordinates, or unspecified once it has left. */
    internal var pointer by mutableStateOf(Offset.Unspecified)
    internal val strength = Animatable(0f)
    internal var reduce = false

    internal fun show(local: Offset) {
        val at = area?.takeIf { it.isAttached } ?: return
        pointer = at.localToRoot(local)
        if (strength.targetValue != 1f) scope.launch { strength.fade(1f) }
    }

    internal fun hide() {
        if (strength.targetValue != 0f) scope.launch { strength.fade(0f) }
    }

    private suspend fun Animatable<Float, *>.fade(target: Float) =
        if (reduce) snapTo(target) else animateTo(target, tween(Motion.LONG, easing = Motion.emphasized))
}

@Composable
fun rememberSpotlight(): Spotlight {
    val scope = rememberCoroutineScope()
    return remember(scope) { Spotlight(scope) }.also { it.reduce = LocalReduceMotion.current }
}

/** The area a [Spotlight] follows the pointer over. It only watches, so taps and scrolls go through. */
fun Modifier.spotlightArea(spotlight: Spotlight): Modifier =
    onGloballyPositioned { spotlight.area = it }.pointerInput(spotlight) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: continue
                val touch = change.type == PointerType.Touch
                when (event.type) {
                    PointerEventType.Exit -> spotlight.hide()
                    PointerEventType.Release -> if (touch) spotlight.hide() else spotlight.show(change.position)
                    PointerEventType.Move -> if (!touch || change.pressed) spotlight.show(change.position)
                    PointerEventType.Enter, PointerEventType.Press -> spotlight.show(change.position)
                }
            }
        }
    }

/**
 * Lights this tile from a [Spotlight]: a soft wash inside and a brighter rim near the pointer. Put it
 * after the tile's [glass], so the light sits over the glass and under the content.
 */
fun Modifier.spotlit(spotlight: Spotlight, shape: Shape, color: Color? = null): Modifier = composed {
    val tint = color ?: MaterialTheme.colorScheme.primary
    val twilight = isTwilight()
    var origin by remember { mutableStateOf(Offset.Zero) }
    // Its own place is state too, so scrolling under a still pointer moves the light across the tile.
    onGloballyPositioned { origin = it.positionInRoot() }.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val rim = 3.dp.toPx()
        val wash = 320.dp.toPx()
        val reach = 240.dp.toPx()
        onDrawBehind {
            val s = spotlight.strength.value
            val p = spotlight.pointer
            if (s <= 0f || p == Offset.Unspecified) return@onDrawBehind
            val at = p - origin
            // Within the tile it washes; outside it only reaches the rim.
            val inside = at.x in 0f..size.width && at.y in 0f..size.height
            if (inside) {
                drawRect(Brush.radialGradient(listOf(tint.copy(alpha = (if (twilight) 0.2f else 0.16f) * s), tint.copy(alpha = (if (twilight) 0.06f else 0.05f) * s), Color.Transparent), at, wash))
            }
            drawOutline(
                outline, Brush.radialGradient(listOf(tint.copy(alpha = s), tint.copy(alpha = 0.45f * s), Color.Transparent), at, reach),
                style = Stroke(rim),
            )
        }
    }
}

/**
 * While [active], a band of light runs round the tile's rim, so a transfer in progress shows on the
 * tile itself and not only in its bar. With reduced motion the rim is lit evenly instead.
 */
fun Modifier.workingRim(active: Boolean, shape: Shape, color: Color? = null): Modifier = if (!active) this else composed {
    val tint = color ?: MaterialTheme.colorScheme.primary
    val reduce = LocalReduceMotion.current
    // Read only while drawing, so the band turns without recomposing the tile.
    val angle: State<Float>? = if (reduce) null else {
        val t = rememberInfiniteTransition(label = "working-rim")
        t.animateFloat(0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "working-rim-angle")
    }
    drawWithCache {
        val width = 2.dp.toPx()
        val outer = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        val inner = Path().apply {
            addOutline(shape.createOutline(Size(size.width - width * 2, size.height - width * 2), layoutDirection, this@drawWithCache))
            translate(Offset(width, width))
        }
        val band = Brush.sweepGradient(
            0f to Color.Transparent, 0.62f to Color.Transparent, 0.86f to tint.copy(alpha = 0.55f), 0.97f to tint, 1f to Color.Transparent,
            center = Offset(size.width / 2, size.height / 2),
        )
        val radius = hypot(size.width, size.height) / 2
        onDrawWithContent {
            drawContent()
            clipPath(outer) {
                clipPath(inner, ClipOp.Difference) {
                    if (angle == null) drawRect(tint.copy(alpha = 0.6f))
                    else rotate(angle.value) { drawCircle(band, radius, center) }
                }
            }
        }
    }
}
