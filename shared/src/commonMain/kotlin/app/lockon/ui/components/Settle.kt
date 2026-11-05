package app.lockon.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Tile states that ease rather than switch, for things a grid holds dozens of. Each is a modifier node
 * that keeps a plain number at rest and starts its one coroutine only when its value actually changes:
 * `animate*AsState` starts one per value per tile, and on desktop every coroutine a tile starts is
 * posted to the window's event queue, which costs frames while a grid scrolls. A lazy grid reusing a
 * node for another card starts at rest, since that card's state is new rather than changed.
 */
private class Settle(var rest: Float) {
    private var moving: Animatable<Float, AnimationVector1D>? = null
    private var job: Job? = null

    /** Read while drawing: the moving value is observed, so a running animation redraws by itself. */
    val value: Float get() = moving?.value ?: rest

    fun snap(to: Float) {
        job?.cancel()
        moving = null
        rest = to
    }

    fun go(scope: CoroutineScope, to: Float, durationMillis: Int) {
        val from = value
        job?.cancel()
        val motion = Animatable(from).also { moving = it }
        rest = to
        job = scope.launch {
            motion.animateTo(to, tween(durationMillis, easing = Motion.emphasized))
            if (moving === motion) moving = null
        }
    }
}

/**
 * Draws with a number that eases to [target] whenever it changes, for things like an outline's strength
 * or how many pips are lit. [draw] must call `drawContent()` itself.
 */
fun Modifier.drawSettled(target: Float, durationMillis: Int = Motion.MEDIUM, draw: ContentDrawScope.(Float) -> Unit): Modifier =
    this then DrawSettledElement(target, durationMillis, draw)

private data class DrawSettledElement(val target: Float, val durationMillis: Int, val draw: ContentDrawScope.(Float) -> Unit) :
    ModifierNodeElement<DrawSettledNode>() {
    override fun create() = DrawSettledNode(target, draw)
    override fun update(node: DrawSettledNode) = node.update(target, durationMillis, draw)
}

private class DrawSettledNode(private var target: Float, private var drawBlock: ContentDrawScope.(Float) -> Unit) :
    Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val settle = Settle(target)
    private var reused = false

    override fun onReset() { reused = true }
    override fun onDetach() = settle.snap(target)

    fun update(target: Float, durationMillis: Int, draw: ContentDrawScope.(Float) -> Unit) {
        drawBlock = draw
        if (target != this.target) {
            this.target = target
            if (reused || !isAttached || currentValueOf(LocalReduceMotion)) settle.snap(target)
            else settle.go(coroutineScope, target, durationMillis)
        }
        reused = false
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() = drawBlock(settle.value)
}

/** Draws with up to three colours that ease to new values together, such as a button's fill and glyph. */
fun Modifier.drawTones(
    a: Color,
    b: Color = a,
    c: Color = a,
    durationMillis: Int = Motion.SHORT,
    draw: ContentDrawScope.(Color, Color, Color) -> Unit,
): Modifier = this then DrawTonesElement(a, b, c, durationMillis, draw)

private data class DrawTonesElement(
    val a: Color,
    val b: Color,
    val c: Color,
    val durationMillis: Int,
    val draw: ContentDrawScope.(Color, Color, Color) -> Unit,
) : ModifierNodeElement<DrawTonesNode>() {
    override fun create() = DrawTonesNode(a, b, c, draw)
    override fun update(node: DrawTonesNode) = node.update(a, b, c, durationMillis, draw)
}

private class DrawTonesNode(
    private var toA: Color,
    private var toB: Color,
    private var toC: Color,
    private var drawBlock: ContentDrawScope.(Color, Color, Color) -> Unit,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private var fromA = toA
    private var fromB = toB
    private var fromC = toC
    private val settle = Settle(1f)
    private var reused = false

    override fun onReset() { reused = true }
    override fun onDetach() = settle.snap(1f)

    fun update(a: Color, b: Color, c: Color, durationMillis: Int, draw: ContentDrawScope.(Color, Color, Color) -> Unit) {
        drawBlock = draw
        if (a != toA || b != toB || c != toC) {
            val p = settle.value
            fromA = lerp(fromA, toA, p); fromB = lerp(fromB, toB, p); fromC = lerp(fromC, toC, p)
            toA = a; toB = b; toC = c
            if (reused || !isAttached || currentValueOf(LocalReduceMotion)) settle.snap(1f)
            else {
                settle.snap(0f)
                settle.go(coroutineScope, 1f, durationMillis)
            }
        }
        reused = false
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val p = settle.value
        if (p >= 1f) drawBlock(toA, toB, toC)
        else drawBlock(lerp(fromA, toA, p), lerp(fromB, toB, p), lerp(fromC, toC, p))
    }
}

/** This at an opacity that eases to [target], without the coroutine a grid's tile would otherwise start to hold still. */
fun Modifier.settleAlpha(target: Float, durationMillis: Int = Motion.SHORT): Modifier = this then SettleAlphaElement(target, durationMillis)

private data class SettleAlphaElement(val target: Float, val durationMillis: Int) : ModifierNodeElement<SettleAlphaNode>() {
    override fun create() = SettleAlphaNode(target)
    override fun update(node: SettleAlphaNode) = node.update(target, durationMillis)
}

private class SettleAlphaNode(private var target: Float) : Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
    private val settle = Settle(target)
    private var reused = false

    override fun onReset() { reused = true }
    override fun onDetach() = settle.snap(target)

    fun update(target: Float, durationMillis: Int) {
        if (target != this.target) {
            this.target = target
            if (reused || !isAttached || currentValueOf(LocalReduceMotion)) {
                settle.snap(target)
                invalidatePlacement()
            } else settle.go(coroutineScope, target, durationMillis)
        }
        reused = false
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) { alpha = settle.value }
        }
    }
}

/**
 * Fades this in when [key] changes, for a tag on a tile that is replaced by another or newly appears.
 * Nothing runs while the key holds or when a grid reuses the tile for another card.
 */
fun Modifier.fadeOnChange(key: Any?, durationMillis: Int = Motion.MEDIUM): Modifier = this then FadeOnChangeElement(key, durationMillis)

private data class FadeOnChangeElement(val key: Any?, val durationMillis: Int) : ModifierNodeElement<FadeOnChangeNode>() {
    override fun create() = FadeOnChangeNode(key)
    override fun update(node: FadeOnChangeNode) = node.update(key, durationMillis)
}

private class FadeOnChangeNode(private var key: Any?) : Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
    private val settle = Settle(1f)
    private var reused = false

    override fun onReset() { reused = true }
    override fun onDetach() = settle.snap(1f)

    fun update(key: Any?, durationMillis: Int) {
        if (key != this.key) {
            this.key = key
            if (reused || !isAttached || currentValueOf(LocalReduceMotion)) settle.snap(1f)
            else {
                settle.snap(0f)
                settle.go(coroutineScope, 1f, durationMillis)
            }
        }
        reused = false
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            // Read in the layer, so a fade redraws without recomposing or relaying out.
            placeable.placeWithLayer(0, 0) { alpha = settle.value }
        }
    }
}
