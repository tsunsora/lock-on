package app.lockon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lockon.ui.theme.SuitColors

// Gundam-inspired decoration (UX rule V7). These are original shapes, not official marks, and they
// never carry meaning on their own.

/**
 * The Lock On mark: a V-fin over a targeting reticle with a red crest. Drawn on a 108-unit grid
 * shared with the Android launcher icon, so both stay identical.
 */
@Composable
fun LockOnMark(modifier: Modifier = Modifier, size: Dp = 32.dp, reticleColor: Color = SuitColors.armorWhite) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 108f, pivot = Offset.Zero) {
            val fin = Path().apply {
                moveTo(54f, 52f); lineTo(20f, 18f); lineTo(32f, 18f); lineTo(54f, 40f)
                lineTo(76f, 18f); lineTo(88f, 18f); close()
            }
            drawPath(fin, SuitColors.vFinYellow)
            drawCircle(reticleColor, radius = 20f, center = Offset(54f, 62f), style = Stroke(6f))
            listOf(Offset(54f, 34f) to Offset(54f, 46f), Offset(54f, 78f) to Offset(54f, 92f),
                Offset(24f, 62f) to Offset(38f, 62f), Offset(70f, 62f) to Offset(84f, 62f)).forEach { (a, b) ->
                drawLine(reticleColor, a, b, strokeWidth = 6f, cap = StrokeCap.Round)
            }
            val crest = Path().apply { moveTo(54f, 54f); lineTo(60f, 62f); lineTo(54f, 70f); lineTo(48f, 62f); close() }
            drawPath(crest, SuitColors.chestRed)
        }
    }
}

/** Thin blue, white, red and yellow band, like a mobile suit's colour blocking. Sits at nav bar edges. */
@Composable
fun TricolorStripe(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(3.dp)) {
        listOf(SuitColors.armorBlue to 5f, SuitColors.armorWhite to 3f, SuitColors.chestRed to 2f, SuitColors.vFinYellow to 1f)
            .forEach { (color, weight) -> Box(Modifier.weight(weight).fillMaxHeight().background(color)) }
    }
}

/** Yellow and black hazard stripes. Reserved for states that need attention (UX rule V7). */
@Composable
fun CautionStripes(modifier: Modifier = Modifier) {
    Canvas(modifier.clipToBounds()) {
        drawRect(SuitColors.cautionBlack)
        val band = 7.dp.toPx()
        var x = -size.height
        while (x < size.width + size.height) {
            val path = Path().apply {
                moveTo(x, size.height); lineTo(x + band, size.height); lineTo(x + band + size.height, 0f); lineTo(x + size.height, 0f); close()
            }
            drawPath(path, SuitColors.vFinYellow)
            x += band * 2
        }
    }
}

/**
 * Armour panel lines: a seam across the top-right and a short vent mark at the bottom-right,
 * drawn faintly over a plate. Purely decorative, so it is drawn rather than laid out.
 */
fun Modifier.panelLines(color: Color, vents: Boolean = true): Modifier = drawWithContent {
    drawContent()
    val line = 1.dp.toPx()
    val inset = 14.dp.toPx()
    val seamY = 4.dp.toPx()
    val start = size.width * 0.72f
    // Seam along the plate's top margin that steps down once, like a plate edge. It stays in the
    // margin so it never crosses content.
    drawLine(color, Offset(start, seamY), Offset(size.width - inset - 4.dp.toPx(), seamY), line)
    drawLine(color, Offset(size.width - inset - 4.dp.toPx(), seamY), Offset(size.width - inset, seamY + 4.dp.toPx()), line)
    if (!vents) return@drawWithContent
    // Three short vent slashes near the bottom-right corner, for plates with an empty corner there.
    val vy = size.height - 10.dp.toPx()
    repeat(3) { i ->
        val vx = size.width - inset - 26.dp.toPx() + i * 6.dp.toPx()
        drawLine(color, Offset(vx, vy), Offset(vx + 4.dp.toPx(), vy - 6.dp.toPx()), line * 1.5f)
    }
}
