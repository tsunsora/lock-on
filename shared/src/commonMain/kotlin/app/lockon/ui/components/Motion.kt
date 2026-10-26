package app.lockon.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lockon.ui.theme.SuitColors
import kotlinx.coroutines.delay

/**
 * The motion system (UX rule V5). Durations and easings follow Material 3's emphasized curves.
 * Everything here checks [LocalReduceMotion] and becomes an instant change or a short fade.
 */
object Motion {
    const val SHORT = 150
    const val MEDIUM = 220
    const val LONG = 320
    /** The Lock On moment is the one longer, signature animation. It plays once per state change. */
    const val SIGNATURE = 650
    const val STAGGER = 45

    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val emphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}

/** True when the system asks for less motion (Android's "Remove animations"). */
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun <T> motion(durationMillis: Int = Motion.MEDIUM, delayMillis: Int = 0): FiniteAnimationSpec<T> =
    if (LocalReduceMotion.current) snap() else tween(durationMillis, delayMillis, Motion.emphasized)

/** A number that rolls up when it grows and down when it shrinks, so changes are noticed. */
@Composable
fun RollingNumber(value: Int, style: TextStyle, color: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = {
            if (reduce) {
                ContentTransform(fadeIn(snap()), fadeOut(snap()))
            } else {
                val up = targetState > initialState
                (slideInVertically(tween(Motion.MEDIUM, easing = Motion.emphasizedDecelerate)) { if (up) it else -it } +
                    fadeIn(tween(Motion.SHORT))) togetherWith
                    (slideOutVertically(tween(Motion.SHORT, easing = Motion.emphasizedAccelerate)) { if (up) -it else it } +
                        fadeOut(tween(Motion.SHORT))) using SizeTransform(clip = true)
            }
        },
        label = "rolling-number",
    ) { Text("$it", style = style, color = color) }
}

/**
 * A HUD gauge: label, count over the limit, and a bar that fills toward the limit. The bar is
 * armour blue when exactly full, V-fin yellow while filling, and the error colour when over.
 */
@Composable
fun DeckGauge(label: String, count: Int, limit: Int, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val fraction by animateFloatAsState((count.toFloat() / limit.coerceAtLeast(1)).coerceIn(0f, 1f), motion(Motion.LONG), label = "gauge")
    val color = when {
        count == limit -> scheme.secondary
        count > limit -> scheme.error
        else -> scheme.primary
    }
    Column(modifier.width(104.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            RollingNumber(count, MaterialTheme.typography.titleMedium, color)
            Text("/$limit", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        }
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(scheme.surfaceVariant)) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(color))
        }
    }
}

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
        // The reticle turns a quarter turn as it locks.
        rotate(degrees = (1f - p) * 45f, pivot = c) {
            drawCircle(color, radius = r, center = c, style = Stroke(stroke))
            listOf(Offset(0f, -1f), Offset(0f, 1f), Offset(-1f, 0f), Offset(1f, 0f)).forEach { d ->
                drawLine(color, c + d * (r * 0.55f), c + d * (r * 1.25f), stroke, StrokeCap.Round)
            }
        }
        // Brackets start wide and close on the target.
        val spread = this.size.minDimension * (0.5f - 0.12f * p)
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
    val angle = if (reduce) 0f else {
        val t = rememberInfiniteTransition(label = "spinner")
        t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "angle").value
    }
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * 0.12f
        val inset = stroke / 2
        drawCircle(color.copy(alpha = 0.25f), radius = this.size.minDimension / 2 - inset, style = Stroke(stroke))
        drawArc(color, startAngle = angle, sweepAngle = 90f, useCenter = false,
            topLeft = Offset(inset, inset), size = Size(this.size.width - stroke, this.size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
        drawCircle(color, radius = stroke * 0.8f)
    }
}

/** Presses sink slightly, so taps on tiles and cards feel physical. */
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, motion(Motion.SHORT), label = "press")
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Plays once when [key] first appears: rises and fades in, [index] steps after the first. Used to
 * deal cards; a new key deals again.
 */
fun Modifier.dealIn(key: Any, index: Int): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val progress = remember(key) { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(key) {
        if (!reduce) {
            delay((index * Motion.STAGGER * 2).toLong())
            progress.animateTo(1f, tween(Motion.LONG, easing = Motion.emphasizedDecelerate))
        }
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 48.dp.toPx()
        rotationZ = (1f - p) * -4f
    }
}

private fun lerp(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)

/** Shown while the app opens: the reticle locks on above the wordmark. */
@Composable
fun LaunchScreen(modifier: Modifier = Modifier) {
    var locked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(120); locked = true }
    Box(modifier.background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LockIndicator(locked, size = 96.dp)
            Text("LOCK ON", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
