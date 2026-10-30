package app.lockon.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lockon.ui.components.Accent
import app.lockon.ui.components.Gold
import app.lockon.ui.components.LocalReduceMotion
import app.lockon.ui.components.LockOnMark
import app.lockon.ui.components.SkyBackground
import app.lockon.ui.components.glass
import app.lockon.ui.components.glassColors
import app.lockon.ui.components.isTwilight
import app.lockon.ui.theme.SuitColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** What the boot screen is waiting on, in order; each finished step fills one pip. */
enum class BootStep(val label: String) {
    DECKS("Loading decks"),
    CARDS("Laying out cards"),
    EDITOR("Warming up the deck editor"),
}

/**
 * The launch screen: a targeting system locking on to the Lock On mark. A seeker reticle hunts
 * across a radar field, catches the mark, closes its brackets on it and locks in V-fin yellow with
 * a pulse, then lets go and hunts again. It covers the app while the screens underneath fill in,
 * so the first look at decks and cards doesn't stutter. With reduced motion it holds the lock.
 */
@Composable
fun BootScreen(step: BootStep, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    val reduce = LocalReduceMotion.current
    val loop = rememberInfiniteTransition(label = "boot")
    // One shared clock; the hunt, the lock and the letting go are all fractions of one loop.
    val clock by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(LOOP_MS, easing = LinearEasing)), label = "clock")
    val time = { if (reduce) 0.82f else clock }
    val scan = if (isTwilight()) SuitColors.teal else Accent
    val ink = MaterialTheme.colorScheme.onSurface
    val labels = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    SkyBackground(
        modifier.fillMaxSize()
            // Nothing underneath takes a press while it boots.
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() } },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val reach = minOf(maxWidth, maxHeight * 1.4f) * 0.34f
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    // Sized to the mark but drawn well past its edges, so the field centres on the mark.
                    Canvas(Modifier.size(176.dp)) {
                        drawTargeting(time(), reach.toPx(), 62.dp.toPx(), scan, ink, labels, labelStyle)
                    }
                    Box(
                        Modifier.size(124.dp).shadow(16.dp, CircleShape, ambientColor = glassColors().shade, spotColor = glassColors().shade)
                            .clip(CircleShape).glass(CircleShape, strong = true),
                        contentAlignment = Alignment.Center,
                    ) { LockOnMark(size = 84.dp, reticleColor = MaterialTheme.colorScheme.onSurface) }
                }
                Spacer(Modifier.height(20.dp))
                Text("Lock On", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.clip(CircleShape).glass(CircleShape).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BootStep.entries.forEach { s ->
                        val done = s.ordinal < step.ordinal
                        val now = s == step
                        Box(
                            Modifier.size(width = if (now) 18.dp else 8.dp, height = 8.dp).clip(CircleShape)
                                .background(if (done || now) Accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(step.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        trailing?.let { Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) { it() } }
    }
}

/** One loop: hunt, close in, lock at [LOCK_AT], hold, let go. The first lock lands inside the boot screen's shortest showing. */
private const val LOOP_MS = 3_400
private const val LOCK_AT = 0.52f

private fun smooth(from: Float, to: Float, x: Float): Float {
    val k = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return k * k * (3 - 2 * k)
}

/** Faint contacts on the radar, as (bearing in turns, range as a share of reach); the sweep lights each as it passes. */
private val Contacts = listOf(0.08f to 0.82f, 0.27f to 1.25f, 0.41f to 0.58f, 0.63f to 1.05f, 0.79f to 1.42f, 0.93f to 0.7f)

/**
 * Draws the whole targeting pass around the canvas centre (the mark), [reach] out, at loop time [t].
 * [target] is the mark's radius, where the brackets come to rest.
 */
private fun DrawScope.drawTargeting(t: Float, reach: Float, target: Float, scan: Color, ink: Color, labels: TextMeasurer, labelStyle: TextStyle) {
    val c = center
    val line = 2.dp.toPx()
    // The radar field: dashed range rings, a sweeping wedge, and contacts that glow as it passes.
    val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 8.dp.toPx()))
    listOf(0.55f, 1.05f, 1.55f).forEach { drawCircle(scan.copy(alpha = 0.16f), reach * it, c, style = Stroke(1.dp.toPx(), pathEffect = dash)) }
    repeat(12) { i ->
        val a = i / 12f * 2 * PI.toFloat()
        val d = Offset(cos(a), sin(a))
        drawLine(scan.copy(alpha = 0.22f), c + d * (reach * 1.55f - 6.dp.toPx()), c + d * (reach * 1.55f + 6.dp.toPx()), line)
    }
    // Two sweeps per loop, so the wedge comes round again where it started.
    val sweep = t * 2 * 360f
    rotate(sweep, pivot = c) {
        val r = reach * 1.6f
        drawArc(
            Brush.sweepGradient(0.88f to Color.Transparent, 1f to scan.copy(alpha = 0.14f), center = c),
            startAngle = -43f, sweepAngle = 43f, useCenter = true, topLeft = c - Offset(r, r), size = Size(r * 2, r * 2),
        )
        drawLine(scan.copy(alpha = 0.45f), c, c + Offset(r, 0f), line)
    }
    Contacts.forEach { (bearing, range) ->
        val since = (((sweep / 360f) - bearing) % 1f + 1f) % 1f
        val glow = (1f - since).let { it * it * it }
        val a = bearing * 2 * PI.toFloat()
        val p = c + Offset(cos(a), sin(a)) * (reach * range)
        val s = 4.dp.toPx()
        rotate(45f, pivot = p) { drawRect(scan.copy(alpha = 0.15f + 0.6f * glow), p - Offset(s, s), Size(s * 2, s * 2)) }
    }

    val fadeIn = smooth(0f, 0.06f, t)
    val release = smooth(0.86f, 1f, t)
    val seen = fadeIn * (1f - release)
    // Catching the mark: the seeker drifts in from its hunt and the brackets close.
    val catch = smooth(0.18f, LOCK_AT, t)
    val locked = t >= LOCK_AT && release < 1f
    val sinceLock = if (t >= LOCK_AT) (t - LOCK_AT) / (1f - LOCK_AT) else -1f
    val hunt = 2 * PI * t
    val wander = Offset(cos(hunt * 1.3 + 0.6).toFloat() * reach * 0.95f, sin(hunt * 2.1 + 0.2).toFloat() * reach * 0.6f)
    val at = c + wander * (1f - catch)
    // The brackets squeeze a hair as they lock, then spring back out wide as it lets go.
    val squeeze = if (sinceLock in 0f..0.12f) 1f - 0.12f * sin(PI.toFloat() * sinceLock / 0.12f) else 1f
    val half = (target * 1.25f + (1f - catch) * target * 0.9f + release * target * 1.4f) * squeeze
    val color = if (locked) lerp(scan, Gold, smooth(0f, 0.08f, sinceLock)) else scan

    // The ring round the mark fills as the seeker closes, the lock's progress.
    val ring = target + 7.dp.toPx()
    drawCircle(ink.copy(alpha = 0.1f * seen), ring, c, style = Stroke(3.dp.toPx()))
    drawArc(
        color.copy(alpha = seen), -90f, 360f * smooth(0.28f, LOCK_AT, t), useCenter = false,
        topLeft = c - Offset(ring, ring), size = Size(ring * 2, ring * 2), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
    )

    // A pulse rolls out from the mark the moment it locks.
    if (sinceLock in 0f..0.45f) {
        val k = sinceLock / 0.45f
        drawCircle(Gold.copy(alpha = 0.7f * (1f - k)), ring + k * reach * 1.1f, c, style = Stroke((4f - 3f * k).dp.toPx()))
    }

    // The seeker: four corner brackets, turned while it hunts and square once it holds.
    rotate((1f - catch) * 35f + release * 20f, pivot = at) {
        val arm = half * 0.42f
        val shadow = SuitColors.cautionBlack.copy(alpha = 0.22f * seen)
        listOf(Offset(-1f, -1f), Offset(1f, -1f), Offset(-1f, 1f), Offset(1f, 1f)).forEach { d ->
            val corner = at + Offset(d.x * half, d.y * half)
            val bracket = Path().apply {
                moveTo(corner.x - d.x * arm, corner.y)
                lineTo(corner.x, corner.y)
                lineTo(corner.x, corner.y - d.y * arm)
            }
            drawPath(bracket, shadow, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(bracket, color.copy(alpha = seen), style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
    // A crosshair rides the seeker while it hunts and gives way to the mark's own reticle as it closes.
    val cross = seen * (1f - smooth(0.3f, LOCK_AT, t))
    if (cross > 0f) {
        listOf(Offset(0f, -1f), Offset(0f, 1f), Offset(-1f, 0f), Offset(1f, 0f)).forEach { d ->
            drawLine(color.copy(alpha = 0.8f * cross), at + d * (half * 0.2f), at + d * (half * 0.6f), line, StrokeCap.Round)
        }
        drawCircle(color.copy(alpha = 0.8f * cross), 3.dp.toPx(), at)
    }
    // Red chevrons point in on the mark while the lock holds.
    if (locked) {
        val show = smooth(0f, 0.06f, sinceLock) * (1f - release)
        val tip = ring + 10.dp.toPx() + 4.dp.toPx() * (1f - show)
        val s = 6.dp.toPx()
        repeat(4) { i ->
            rotate(45f + i * 90f, pivot = c) {
                val chevron = Path().apply {
                    moveTo(c.x, c.y - tip)
                    lineTo(c.x - s, c.y - tip - s * 1.3f)
                    lineTo(c.x + s, c.y - tip - s * 1.3f)
                    close()
                }
                drawPath(chevron, SuitColors.chestRed.copy(alpha = show))
            }
        }
    }
    // A readout under the seeker's bottom-right bracket says what it is doing.
    val (word, wordColor) = when {
        locked -> "LOCKED" to SuitColors.chestRed
        catch > 0.35f -> "TRACKING" to ink.copy(alpha = 0.7f)
        else -> "SEARCHING" to ink.copy(alpha = 0.55f)
    }
    if (seen > 0f) {
        val laid = labels.measure(word, labelStyle.copy(color = wordColor))
        val corner = at + Offset(half, half)
        drawText(laid, topLeft = corner + Offset(-laid.size.width.toFloat(), 8.dp.toPx()), alpha = seen)
    }
}
