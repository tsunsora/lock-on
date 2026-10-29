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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** What the boot screen is waiting on, in order; each finished step fills one pip. */
enum class BootStep(val label: String) {
    DECKS("Loading decks"),
    CARDS("Laying out cards"),
    EDITOR("Warming up the deck editor"),
}

/**
 * The launch screen: the Lock On mark under a slowly turning reticle while gunbits fly figure
 * eights around it, trailing thruster glow and now and then firing a short beam. It covers the app
 * while the screens underneath fill in, so the first look at decks and cards doesn't stutter.
 * With reduced motion the bits hold still in formation.
 */
@Composable
fun BootScreen(step: BootStep, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    val reduce = LocalReduceMotion.current
    val loop = rememberInfiniteTransition(label = "boot")
    // One shared clock; every bit's path is a whole number of turns per loop, so it never jumps.
    val clock by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(LOOP_MS, easing = LinearEasing)), label = "clock")
    val time = { if (reduce) 0.12f else clock }
    val beam = Accent
    val trail = if (isTwilight()) SuitColors.teal else Accent
    SkyBackground(
        modifier.fillMaxSize()
            // Nothing underneath takes a press while it boots.
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() } },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val t = time()
            val reach = minOf(size.width, size.height * 1.4f) * 0.34f
            Bits.forEach { bit -> drawBit(bit, t, center, reach, trail, beam) }
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(176.dp)) {
                    val turn = time() * 360f
                    val ring = Stroke(3.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18.dp.toPx(), 10.dp.toPx())))
                    rotate(turn) { drawCircle(beam.copy(alpha = 0.55f), radius = size.minDimension / 2 - 4.dp.toPx(), style = ring) }
                    rotate(-turn * 2) {
                        val r = size.minDimension / 2 - 16.dp.toPx()
                        repeat(4) { i ->
                            rotate(i * 90f) { drawLine(Gold, Offset(center.x, center.y - r), Offset(center.x, center.y - r + 10.dp.toPx()), 3.dp.toPx(), StrokeCap.Round) }
                        }
                    }
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
        trailing?.let { Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) { it() } }
    }
}

private const val LOOP_MS = 9_000

/**
 * One gunbit's flight: a Lissajous curve, [a] and [b] turns across and up per loop, offset by
 * [phase] so the bits never bunch up. [fireAt] is where in the loop it shoots.
 */
private class Bit(val a: Int, val b: Int, val phase: Float, val rx: Float, val ry: Float, val scale: Float, val fireAt: Float)

private val Bits = listOf(
    Bit(1, 2, 0.00f, 1.00f, 0.62f, 1.00f, 0.20f),
    Bit(1, 2, 0.50f, 1.00f, 0.62f, 1.00f, 0.70f),
    Bit(2, 1, 0.15f, 0.78f, 0.80f, 0.85f, 0.45f),
    Bit(2, 3, 0.35f, 1.18f, 0.72f, 0.75f, 0.05f),
    Bit(1, 1, 0.70f, 1.30f, 0.48f, 0.90f, 0.85f),
    Bit(3, 2, 0.85f, 0.92f, 0.95f, 0.70f, 0.60f),
)

private fun Bit.at(t: Float, center: Offset, reach: Float): Offset {
    val angle = 2 * PI * (t + phase)
    return Offset(
        center.x + reach * rx * cos(a * angle).toFloat(),
        center.y + reach * ry * sin(b * angle).toFloat(),
    )
}

private fun DrawScope.drawBit(bit: Bit, t: Float, center: Offset, reach: Float, trail: Color, beam: Color) {
    val here = bit.at(t, center, reach)
    val ahead = bit.at(t + 0.002f, center, reach)
    val heading = atan2(ahead.y - here.y, ahead.x - here.x)
    val unit = 1.4.dp.toPx() * bit.scale
    // Thruster trail: fading dots back along the path.
    for (k in 1..10) {
        val p = bit.at(t - k * 0.0045f, center, reach)
        drawCircle(trail.copy(alpha = 0.32f * (1f - k / 11f)), radius = unit * (3.2f - k * 0.22f), center = p)
    }
    // A short beam for a moment once per loop.
    val sinceFire = ((t - bit.fireAt) % 1f + 1f) % 1f
    if (sinceFire < 0.035f) {
        val f = sinceFire / 0.035f
        val dir = Offset(cos(heading), sin(heading))
        val from = here + dir * (unit * (14f + f * 50f))
        val to = from + dir * (unit * 34f)
        drawLine(beam.copy(alpha = 0.25f * (1f - f)), from, to, unit * 7f, StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.9f * (1f - f)), from, to, unit * 2.2f, StrokeCap.Round)
    }
    translate(here.x, here.y) {
        rotateRad(heading, pivot = Offset.Zero) {
            drawCircle(Brush.radialGradient(listOf(trail.copy(alpha = 0.7f), Color.Transparent), Offset(-unit * 9f, 0f), unit * 7f), unit * 7f, Offset(-unit * 9f, 0f))
            drawPath(BitBody.scaled(unit), SuitColors.armorWhite)
            drawPath(BitFins.scaled(unit), SuitColors.armorBlue)
            drawCircle(SuitColors.chestRed, radius = unit * 1.6f, center = Offset(unit * 3f, 0f))
            drawPath(BitBody.scaled(unit), SuitColors.cautionBlack.copy(alpha = 0.35f), style = Stroke(unit * 0.8f))
        }
    }
}

/** The bit points along +x, its nose at 14 units: a long kite of a body with two swept fins behind. */
private val BitBodyPoints = listOf(14f to 0f, 2f to -4f, -8f to -2.5f, -8f to 2.5f, 2f to 4f)
private val BitFinPoints = listOf(listOf(-1f to -3.4f, -10f to -9f, -7f to -2.6f), listOf(-1f to 3.4f, -10f to 9f, -7f to 2.6f))

private object BitBody {
    fun scaled(unit: Float) = Path().apply {
        BitBodyPoints.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * unit, y * unit) else lineTo(x * unit, y * unit) }
        close()
    }
}

private object BitFins {
    fun scaled(unit: Float) = Path().apply {
        BitFinPoints.forEach { fin ->
            fin.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * unit, y * unit) else lineTo(x * unit, y * unit) }
            close()
        }
    }
}
