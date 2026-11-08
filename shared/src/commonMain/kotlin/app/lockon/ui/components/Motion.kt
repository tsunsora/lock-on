package app.lockon.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.spring
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lockon.ui.theme.SuitColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import androidx.compose.foundation.interaction.InteractionSource
import kotlin.time.TimeSource

/**
 * The motion system (UX rule V5). Durations and easings follow Material 3's emphasized curves.
 * Motion is quiet: short distances, small scales, no bounce or overshoot, so it confirms a change
 * without drawing the eye away from the cards. Everything here checks [LocalReduceMotion] and
 * becomes an instant change or a short fade.
 */
object Motion {
    const val SHORT = 120
    const val MEDIUM = 200
    const val LONG = 280
    /** The Lock On moment is the one longer, signature animation. It plays once per state change. */
    const val SIGNATURE = 480
    const val STAGGER = 30

    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val emphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}

/** True when the system asks for less motion (Android's "Remove animations"). */
val LocalReduceMotion = staticCompositionLocalOf { false }

/**
 * False while the screen around it is composed but out of sight (a kept tab or editor). Anything that
 * loops, like the Sync beacon, holds still then: a running loop asks for a frame every vsync whether
 * it is drawn or not, so the whole window kept redrawing on every other screen.
 */
val LocalScreenShown = compositionLocalOf { true }

@Composable
fun <T> motion(durationMillis: Int = Motion.MEDIUM, delayMillis: Int = 0): FiniteAnimationSpec<T> =
    if (LocalReduceMotion.current) snap() else tween(durationMillis, delayMillis, Motion.emphasized)

/**
 * For things that move to where the pointer sent them (a sliding pill, a switch's knob): a spring,
 * after Apple's motion guidance. It has no fixed length, so a second click mid-way redirects it
 * with the speed it already had instead of starting over. Critically damped by default; [bounce]
 * adds a slight settle for things the hand moves directly.
 */
@Composable
fun <T> glide(bounce: Float = 0f): FiniteAnimationSpec<T> = glideSpec(LocalReduceMotion.current, bounce)

/** A [glide] for use outside composition, given whether motion is reduced. */
fun <T> glideSpec(reduce: Boolean, bounce: Float = 0f): FiniteAnimationSpec<T> =
    if (reduce) snap() else spring(dampingRatio = 1f - bounce, stiffness = 420f)

/**
 * Pages that sit side by side, like an editor's tabs: the new page slides in from the side of the
 * tab that was picked and the old one gives way the other way, a short distance, so the content
 * moves the way the tab's pill did. With reduced motion it is a quick fade.
 */
@Composable
fun SlidingPages(page: Int, modifier: Modifier = Modifier, content: @Composable (Int) -> Unit) {
    val reduce = LocalReduceMotion.current
    AnimatedContent(
        targetState = page,
        modifier = modifier,
        transitionSpec = {
            if (reduce) fadeIn(tween(90)) togetherWith fadeOut(tween(90))
            else {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(spring(dampingRatio = 1f, stiffness = 420f)) { dir * it / 12 } + fadeIn(tween(Motion.MEDIUM))) togetherWith
                    (slideOutHorizontally(spring(dampingRatio = 1f, stiffness = 420f)) { -dir * it / 12 } + fadeOut(tween(Motion.SHORT)))
            }
        },
        label = "pages",
    ) { content(it) }
}

/**
 * A number that rolls up when it grows and down when it shrinks, so changes are noticed. It is plain
 * text until the number first changes: a screen opens with dozens of these, and most never move.
 */
@Composable
fun RollingNumber(value: Int, style: TextStyle, color: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    val first = remember { value }
    // Not state: it only matters in the composition the new value already caused.
    val moved = remember { BooleanArray(1) }
    if (value != first) moved[0] = true
    if (!moved[0]) {
        Text("$value", modifier, style = style, color = color)
        return
    }
    val reduce = LocalReduceMotion.current
    // Starts from the first value, so the first change rolls too.
    val transition = rememberTransition(remember { MutableTransitionState(first) }.apply { targetState = value }, label = "rolling-number")
    transition.AnimatedContent(
        modifier = modifier,
        transitionSpec = {
            if (reduce) {
                ContentTransform(fadeIn(snap()), fadeOut(snap()))
            } else {
                val up = targetState > initialState
                (slideInVertically(tween(Motion.MEDIUM, easing = Motion.emphasizedDecelerate)) { if (up) it / 2 else -it / 2 } +
                    fadeIn(tween(Motion.SHORT))) togetherWith
                    (slideOutVertically(tween(Motion.SHORT, easing = Motion.emphasizedAccelerate)) { if (up) -it / 2 else it / 2 } +
                        fadeOut(tween(Motion.SHORT))) using SizeTransform(clip = true)
            }
        },
    ) { Text("$it", style = style, color = color) }
}

/** A deck gauge; drawn as a [ProgressGauge] in the HUD style. */
@Composable
fun DeckGauge(label: String, count: Int, limit: Int, modifier: Modifier = Modifier) = ProgressGauge(label, count, limit, modifier)

/**
 * The Lock On moment: when a deck becomes legal, four brackets close in on the reticle and it turns
 * from outline grey to V-fin yellow. Illegal decks show the open, dim reticle.
 */
@Composable
fun LockIndicator(locked: Boolean, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val progress = remember { Animatable(if (locked) 1f else 0f) }
    val reduce = LocalReduceMotion.current
    LaunchedEffect(locked) {
        val target = if (locked) 1f else 0f
        if (reduce) progress.snapTo(target)
        else progress.animateTo(target, tween(if (locked) Motion.SIGNATURE else Motion.MEDIUM, easing = Motion.emphasized))
    }
    val idle = MaterialTheme.colorScheme.outline
    Canvas(modifier.size(size)) {
        val p = progress.value
        val color = lerp(idle, SuitColors.vFinYellow, p)
        val c = center
        val r = this.size.minDimension * 0.22f
        val stroke = this.size.minDimension * 0.07f
        // The reticle settles with a slight turn as it locks.
        rotate(degrees = (1f - p) * 15f, pivot = c) {
            drawCircle(color, radius = r, center = c, style = Stroke(stroke))
            listOf(Offset(0f, -1f), Offset(0f, 1f), Offset(-1f, 0f), Offset(1f, 0f)).forEach { d ->
                drawLine(color, c + d * (r * 0.55f), c + d * (r * 1.25f), stroke, StrokeCap.Round)
            }
        }
        // Brackets start wide and close on the target.
        val spread = this.size.minDimension * (0.44f - 0.06f * p)
        val arm = this.size.minDimension * 0.14f
        listOf(Offset(-1f, -1f), Offset(1f, -1f), Offset(-1f, 1f), Offset(1f, 1f)).forEach { d ->
            val corner = c + Offset(d.x * spread, d.y * spread)
            drawLine(color.copy(alpha = 0.35f + 0.65f * p), corner, corner - Offset(d.x * arm, 0f), stroke, StrokeCap.Round)
            drawLine(color.copy(alpha = 0.35f + 0.65f * p), corner, corner - Offset(0f, d.y * arm), stroke, StrokeCap.Round)
        }
        if (p > 0.6f) drawCircle(SuitColors.chestRed.copy(alpha = (p - 0.6f) / 0.4f), radius = stroke * 1.1f, center = c)
    }
}

/** A reticle that sweeps while work is in progress; used instead of a plain spinner. */
@Composable
fun ReticleSpinner(modifier: Modifier = Modifier, size: Dp = 20.dp, color: Color = MaterialTheme.colorScheme.primary) {
    val reduce = LocalReduceMotion.current
    // Read only while drawing, so each frame redraws the reticle without recomposing it.
    val angle: State<Float>? = if (reduce) null else {
        val t = rememberInfiniteTransition(label = "spinner")
        t.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "angle")
    }
    Canvas(modifier.size(size)) {
        val angle = angle?.value ?: 0f
        val stroke = this.size.minDimension * 0.12f
        val inset = stroke / 2
        drawCircle(color.copy(alpha = 0.25f), radius = this.size.minDimension / 2 - inset, style = Stroke(stroke))
        drawArc(color, startAngle = angle, sweepAngle = 90f, useCenter = false,
            topLeft = Offset(inset, inset), size = Size(this.size.width - stroke, this.size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
        drawCircle(color, radius = stroke * 0.8f)
    }
}

/**
 * Presses sink a hair, so taps on tiles and cards register. A modifier node rather than composed
 * state, since every card tile carries two of these and a screen opens with dozens of tiles.
 */
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier = this then PressScaleElement(interaction)

private data class PressScaleElement(val interaction: MutableInteractionSource) : ModifierNodeElement<PressScaleNode>() {
    override fun create() = PressScaleNode(interaction)
    override fun update(node: PressScaleNode) = node.update(interaction)
}

private class PressScaleNode(private var interaction: MutableInteractionSource) :
    Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
    private val scale = Animatable(1f)
    private var watching: Job? = null

    override fun onAttach() = watch()

    // A lazy grid reuses this node for another card, which starts at rest.
    override fun onReset() {
        if (isAttached && scale.value != 1f) coroutineScope.launch { scale.snapTo(1f) }
    }

    fun update(interaction: MutableInteractionSource) {
        if (interaction == this.interaction) return
        this.interaction = interaction
        watching?.cancel()
        watch()
    }

    private fun watch() {
        // Undispatched, so a tile scrolling in subscribes now rather than posting one more event to the window's queue.
        watching = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            var presses = 0
            interaction.interactions.collect { event ->
                when (event) {
                    is PressInteraction.Press -> presses++
                    is PressInteraction.Release, is PressInteraction.Cancel -> presses = (presses - 1).coerceAtLeast(0)
                    else -> return@collect
                }
                val target = if (presses > 0) 0.985f else 1f
                launch {
                    if (currentValueOf(LocalReduceMotion)) scale.snapTo(target)
                    else scale.animateTo(target, tween(Motion.SHORT, easing = Motion.emphasized))
                }
            }
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            // Read in the layer, so a press redraws without recomposing or relaying out.
            placeable.placeWithLayer(0, 0) { scaleX = scale.value; scaleY = scale.value }
        }
    }
}

/**
 * Whether [this] is pressed, held for at least [Motion.SHORT] so a quick tap still plays its press.
 * `collectIsPressedAsState` lets go as soon as the finger does, and a tap is often over before the
 * press has drawn a frame, so the button looks as if it ignored the tap.
 */
@Composable
fun InteractionSource.collectIsPressedHeld(): State<Boolean> {
    val held = remember { mutableStateOf(false) }
    LaunchedEffect(this) {
        var presses = 0
        var since = TimeSource.Monotonic.markNow()
        // Only presses: buttons share this source with hover, and a hover ending (a menu opening over the
        // button, say) would otherwise cancel the wait below and leave the button stuck down.
        interactions.filter { it is PressInteraction }.collectLatest { event ->
            when (event) {
                is PressInteraction.Press -> { presses++; since = TimeSource.Monotonic.markNow(); held.value = true }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    presses = (presses - 1).coerceAtLeast(0)
                    if (presses == 0) {
                        // A new press cancels the wait, so the button stays down.
                        delay((Motion.SHORT - since.elapsedNow().inWholeMilliseconds).coerceAtLeast(0))
                        held.value = false
                    }
                }
            }
        }
    }
    return held
}

/**
 * Plays once when [key] first appears: fades in and settles up a short way, [index] steps after the
 * first. Used to deal cards; a new key deals again.
 */
fun Modifier.dealIn(key: Any, index: Int): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val progress = remember(key) { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(key) {
        // The stagger is the tween's own delay, timed by the frame clock. A separate delay() can
        // resume on another thread and deadlock against the renderer's frame lock.
        if (!reduce) progress.animateTo(1f, tween(Motion.LONG, index * Motion.STAGGER, Motion.emphasizedDecelerate))
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 12.dp.toPx()
    }
}

/**
 * Runs 0 to 1 once, when it first composes: for dialogs and popups, which otherwise appear at once.
 * Read it in [arrive] (the draw phase), so the arrival never recomposes the dialog.
 */
@Composable
fun rememberArrival(durationMillis: Int = Motion.MEDIUM): State<Float> {
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduce) progress.animateTo(1f, tween(durationMillis, easing = Motion.emphasizedDecelerate)) }
    return progress.asState()
}

/** A banner, hint or row opening into place: it unfolds and fades in, pushing what is below aside. */
@Composable
fun revealIn(): EnterTransition =
    if (LocalReduceMotion.current) fadeIn(tween(90))
    else expandVertically(tween(Motion.MEDIUM, easing = Motion.emphasized)) + fadeIn(tween(Motion.MEDIUM, Motion.STAGGER))

/** The way back for [revealIn], quicker, as small exits are. */
@Composable
fun revealOut(): ExitTransition =
    if (LocalReduceMotion.current) fadeOut(tween(90))
    else shrinkVertically(tween(Motion.SHORT, easing = Motion.emphasizedAccelerate)) + fadeOut(tween(Motion.SHORT))

/** [revealIn] sideways, for something that opens in a row. */
@Composable
fun revealSideIn(): EnterTransition =
    if (LocalReduceMotion.current) fadeIn(tween(90))
    else expandHorizontally(tween(Motion.MEDIUM, easing = Motion.emphasized)) + fadeIn(tween(Motion.MEDIUM, Motion.STAGGER))

/** The way back for [revealSideIn]. */
@Composable
fun revealSideOut(): ExitTransition =
    if (LocalReduceMotion.current) fadeOut(tween(90))
    else shrinkHorizontally(tween(Motion.SHORT, easing = Motion.emphasizedAccelerate)) + fadeOut(tween(Motion.SHORT))

/** Opens and closes [content], making room for it as [revealIn] does. Holds still when a screen first shows it. */
@Composable
fun Reveal(visible: Boolean, modifier: Modifier = Modifier, sideways: Boolean = false, fadeOnly: Boolean = false, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible, modifier,
        // Inside something that already eases its own size, only the fade is left to do.
        enter = if (fadeOnly) fadeIn(tween(if (LocalReduceMotion.current) 90 else Motion.MEDIUM)) else if (sideways) revealSideIn() else revealIn(),
        exit = if (fadeOnly) fadeOut(tween(if (LocalReduceMotion.current) 90 else Motion.SHORT)) else if (sideways) revealSideOut() else revealOut(),
    ) { content() }
}

/** [Reveal] for something that has a value only while it is there: it keeps showing the last one as it closes. */
@Composable
fun <T : Any> RevealValue(value: T?, modifier: Modifier = Modifier, sideways: Boolean = false, fadeOnly: Boolean = false, content: @Composable (T) -> Unit) {
    // Not state: it only matters in the composition the new value already caused.
    val last = remember { arrayOfNulls<Any>(1) }
    if (value != null) last[0] = value
    Reveal(value != null, modifier, sideways, fadeOnly) {
        @Suppress("UNCHECKED_CAST")
        (last[0] as T?)?.let { content(it) }
    }
}

/**
 * A short list of rows that open as they arrive and close as they leave, each keeping its place while
 * it does. For a handful of things like nearby devices, not for a grid.
 */
@Composable
fun <T> RevealItems(items: List<T>, key: (T) -> Any, content: @Composable (T) -> Unit) {
    // Not state: each composition rebuilds it, and a row finishing its exit recomposes through its transition.
    val kept = remember { mutableListOf<T>() }
    val first = remember { booleanArrayOf(true) }
    val live = items.mapTo(HashSet()) { key(it) }
    val merged = items.toMutableList()
    kept.forEachIndexed { i, item -> if (key(item) !in live) merged.add(i.coerceAtMost(merged.size), item) }
    kept.clear(); kept += merged
    val settled = first[0]
    merged.forEach { item ->
        val id = key(item)
        androidx.compose.runtime.key(id) {
            val shown = remember { MutableTransitionState(settled) }
            shown.targetState = id in live
            if (shown.isIdle && !shown.currentState && id !in live) kept.removeAll { key(it) == id }
            AnimatedVisibility(shown, enter = revealIn(), exit = revealOut()) { content(item) }
        }
    }
    first[0] = false
}

/**
 * Fades one thing into another when [value] changes, for a status line, a loading state or a set of
 * buttons. The old one goes quicker than the new one comes, and the space between them eases.
 */
@Composable
fun <T> FadeSwap(
    value: T,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    /** What counts as another thing: a transfer's state, say, so its rising progress fades nothing. */
    contentKey: (T) -> Any? = { it },
    content: @Composable (T) -> Unit,
) {
    val reduce = LocalReduceMotion.current
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        contentAlignment = contentAlignment,
        contentKey = contentKey,
        transitionSpec = {
            if (reduce) fadeIn(tween(90)) togetherWith fadeOut(tween(90)) using SizeTransform(clip = false) { _, _ -> snap() }
            else fadeIn(tween(Motion.MEDIUM, Motion.STAGGER)) togetherWith fadeOut(tween(Motion.SHORT)) using
                SizeTransform(clip = false) { _, _ -> tween(Motion.MEDIUM, easing = Motion.emphasized) }
        },
        label = "fade-swap",
    ) { content(it) }
}

/** Fades in and grows from just under its size as [progress] runs, so a panel arrives rather than appears. */
fun Modifier.arrive(progress: State<Float>, from: Float = 0.94f): Modifier = graphicsLayer {
    val p = progress.value
    alpha = p
    val s = from + (1f - from) * p
    scaleX = s; scaleY = s
}

private fun lerp(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)
