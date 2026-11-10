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

// The Lock On mark and the caution stripes (UX rule V7). These are original shapes, not official
// marks, and they never carry meaning on their own.

/**
 * The Lock On mark: a bold ring with four short studs growing out of it and a big center dot, all
 * in one colour, thick enough to hold at 16 px. Drawn on a 108-unit grid shared with the app icons,
 * so they stay identical.
 */
@Composable
fun LockOnMark(modifier: Modifier = Modifier, size: Dp = 32.dp, reticleColor: Color = SuitColors.armorWhite) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 108f, pivot = Offset.Zero) {
            drawCircle(reticleColor, radius = 25f, center = Offset(54f, 54f), style = Stroke(12f))
            listOf(Offset(54f, 18f) to Offset(54f, 29f), Offset(54f, 79f) to Offset(54f, 90f),
                Offset(18f, 54f) to Offset(29f, 54f), Offset(79f, 54f) to Offset(90f, 54f)).forEach { (a, b) ->
                drawLine(reticleColor, a, b, strokeWidth = 12f, cap = StrokeCap.Round)
            }
            drawCircle(reticleColor, radius = 10f, center = Offset(54f, 54f))
        }
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
