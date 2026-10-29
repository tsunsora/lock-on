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
import androidx.compose.ui.graphics.drawscope.scale
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
 * eights around it, blade staves and wing pieces like Aerial's split-up shield, now and then firing
 * a short beam. It covers the app while the screens underneath fill in, so the first look at decks
 * and cards doesn't stutter. With reduced motion the bits hold still in formation.
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
 * [phase] so the bits never bunch up. [fireAt] is where in the loop it shoots. [stave] picks the
 * long blade over the short wing piece; Aerial's shield splits into both.
 */
private class Bit(val a: Int, val b: Int, val phase: Float, val rx: Float, val ry: Float, val scale: Float, val fireAt: Float, val stave: Boolean = true)

private val Bits = listOf(
    Bit(1, 2, 0.00f, 1.00f, 0.62f, 1.00f, 0.20f),
    Bit(1, 2, 0.50f, 1.00f, 0.62f, 1.00f, 0.70f),
    Bit(2, 1, 0.15f, 0.78f, 0.80f, 1.00f, 0.45f, stave = false),
    Bit(2, 3, 0.35f, 1.18f, 0.72f, 0.80f, 0.05f),
    Bit(1, 1, 0.70f, 1.30f, 0.48f, 0.90f, 0.85f),
    Bit(3, 2, 0.85f, 0.92f, 0.95f, 0.90f, 0.60f, stave = false),
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
    // Bits don't fly nose first: they drift sideways with their muzzles turned out from the reticle.
    val heading = atan2(here.y - center.y, here.x - center.x)
    val unit = 2.4.dp.toPx() * bit.scale
    val shape = if (bit.stave) Stave else Wing
    // A thin fading glow line that leaves the back end and streams out behind it as the bit slides.
    fun tailAt(k: Int): Offset {
        val p = bit.at(t - k * 0.004f, center, reach)
        val back = atan2(p.y - center.y, p.x - center.x) + PI.toFloat()
        return p + Offset(cos(back), sin(back)) * (unit * (shape.tail + k * 1.2f))
    }
    var prev = tailAt(0)
    for (k in 1..12) {
        val p = tailAt(k)
        drawLine(trail.copy(alpha = 0.45f * (1f - k / 13f)), prev, p, unit * (1.6f - k * 0.1f), StrokeCap.Round)
        prev = p
    }
    // A short beam for a moment once per loop.
    val sinceFire = ((t - bit.fireAt) % 1f + 1f) % 1f
    if (sinceFire < 0.035f) {
        val f = sinceFire / 0.035f
        val dir = Offset(cos(heading), sin(heading))
        val from = here + dir * (unit * (shape.tip + f * 50f))
        val to = from + dir * (unit * 34f)
        drawLine(beam.copy(alpha = 0.25f * (1f - f)), from, to, unit * 7f, StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.9f * (1f - f)), from, to, unit * 2.2f, StrokeCap.Round)
    }
    // The flat piece rolls as it flies, so it reads as a thin panel turning in space.
    val roll = 0.8f + 0.2f * cos(2 * PI * (2 * t + bit.phase)).toFloat()
    translate(here.x, here.y) {
        rotateRad(heading, pivot = Offset.Zero) {
            scale(1f, roll, pivot = Offset.Zero) {
                drawCircle(Brush.radialGradient(listOf(trail.copy(alpha = 0.3f), Color.Transparent), Offset.Zero, unit * 13f), unit * 13f, Offset.Zero)
                drawPath(shape.body.scaled(unit), SuitColors.armorWhite)
                drawPath(shape.edge.scaled(unit), SuitColors.armorBlue)
                drawPath(shape.inset.scaled(unit), SuitColors.cautionBlack.copy(alpha = 0.55f))
                drawCircle(trail, radius = unit * 0.8f, center = shape.muzzle * unit)
                drawPath(shape.body.scaled(unit), SuitColors.cautionBlack.copy(alpha = 0.35f), style = Stroke(unit * 0.6f))
            }
        }
    }
}

/**
 * A GUND-Bit as drawn from Aerial's split-up shield, pointing along +x with its clipped tip at
 * [tip] units and its back end [tail] units behind the middle: a white face, a blue strip down one
 * edge, a dark recessed panel, and a lit muzzle.
 */
private class BitShape(val tip: Float, val tail: Float, val body: Outline, val edge: Outline, val inset: Outline, val muzzle: Offset)

/** The long blade stave, about five times as long as it is wide. */
private val Stave = BitShape(
    tip = 15f,
    tail = 15f,
    body = Outline(15f to -0.8f, 10f to -3.2f, -12f to -3.2f, -15f to -0.8f, -15f to 3.2f, 9f to 3.2f),
    edge = Outline(11.4f to 1.6f, 9f to 3.2f, -15f to 3.2f, -15f to 1.6f),
    inset = Outline(7f to -1.8f, -10f to -1.8f, -11.5f to 0.8f, 5.5f to 0.8f),
    muzzle = Offset(11.5f, -1.1f),
)

/** The short wing piece from the shield's shoulders. */
private val Wing = BitShape(
    tip = 9f,
    tail = 9f,
    body = Outline(9f to -1f, 5f to -3.4f, -8f to -3.4f, -9f to -2.2f, -9f to 3.4f, 4f to 3.4f),
    edge = Outline(5.4f to 2.2f, 4f to 3.4f, -9f to 3.4f, -9f to 2.2f),
    inset = Outline(3f to -1.8f, -6f to -1.8f, -7f to 0.6f, 2f to 0.6f),
    muzzle = Offset(6.3f, -0.8f),
)

private class Outline(vararg val points: Pair<Float, Float>) {
    fun scaled(unit: Float) = Path().apply {
        points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * unit, y * unit) else lineTo(x * unit, y * unit) }
        close()
    }
}
