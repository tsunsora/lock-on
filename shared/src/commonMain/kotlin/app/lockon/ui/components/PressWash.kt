package app.lockon.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

/** How strong the wash is while pressed, and on desktop while hovered. */
private const val PRESS_ALPHA = 0.08f
private const val HOVER_ALPHA = 0.04f
private const val FADE_MS = 120

/**
 * The app's one answer to a touch (UX rule V5): a flat wash of the ink colour over the thing's own
 * shape, on at once and gone 120 ms after letting go, with no circle and nothing that follows the
 * finger. Keyboard focus is the selection outline. The theme provides it as `LocalIndication`, so a
 * plain `clickable` gets it. [shape] matters where corners should hold without a clip, and for the focus
 * line. [hover] is off where hovering already has its own answer, such as a card's lift.
 */
data class PressWash(
    val ink: Color,
    val focus: Color,
    val shape: Shape = RectangleShape,
    val hover: Boolean = true,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = PressWashNode(interactionSource, this)
}

/** A [PressWash] in the theme's colours, for a shape of its own. */
@Composable
@ReadOnlyComposable
fun pressWash(shape: Shape = RectangleShape, hover: Boolean = true, ink: Color = MaterialTheme.colorScheme.onSurface): PressWash =
    PressWash(ink, MaterialTheme.colorScheme.primary, shape, hover)

private class PressWashNode(
    private val source: InteractionSource,
    private val style: PressWash,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val press = Animatable(0f)
    private val hover = Animatable(0f)
    private var focused = false

    override fun onAttach() {
        launchInPlace {
            var presses = 0
            var hovers = 0
            source.interactions.collect { event ->
                when (event) {
                    is PressInteraction.Press -> {
                        presses++
                        launch { press.snapTo(1f) }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        presses = (presses - 1).coerceAtLeast(0)
                        if (presses == 0) launch { press.settleTo(0f) }
                    }
                    is HoverInteraction.Enter -> {
                        hovers++
                        if (style.hover) launch { hover.settleTo(1f) }
                    }
                    is HoverInteraction.Exit -> {
                        hovers = (hovers - 1).coerceAtLeast(0)
                        if (style.hover && hovers == 0) launch { hover.settleTo(0f) }
                    }
                    is FocusInteraction.Focus -> { focused = true; invalidateDraw() }
                    is FocusInteraction.Unfocus -> { focused = false; invalidateDraw() }
                }
            }
        }
    }

    private suspend fun Animatable<Float, *>.settleTo(target: Float) {
        if (currentValueOf(LocalReduceMotion)) snapTo(target)
        else animateTo(target, tween(FADE_MS, easing = Motion.emphasizedDecelerate))
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val wash = maxOf(press.value * PRESS_ALPHA, hover.value * HOVER_ALPHA)
        if (wash > 0f) {
            if (style.shape === RectangleShape) drawRect(style.ink.copy(alpha = wash))
            else drawOutline(style.shape.createOutline(size, layoutDirection, this), style.ink.copy(alpha = wash))
        }
        if (focused) {
            if (style.shape === RectangleShape) {
                val width = SelectionEdgeWidth.toPx()
                drawRect(style.focus, Offset(width / 2, width / 2), Size(size.width - width, size.height - width), style = Stroke(width))
            } else drawSelectionEdge(style.shape, style.focus)
        }
    }
}
