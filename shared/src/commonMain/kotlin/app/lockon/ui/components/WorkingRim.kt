package app.lockon.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * While [active], a band of light runs round the tile's rim, so a transfer in progress shows on the
 * tile itself and not only in its bar. With reduced motion the rim is lit evenly instead.
 */
fun Modifier.workingRim(active: Boolean, shape: Shape, color: Color? = null): Modifier = if (!active) this else composed {
    val tint = color ?: MaterialTheme.colorScheme.primary
    // Read only while drawing, so the band turns without recomposing the tile.
    val angle = loopingFloat(0f, 360f, tween(2400, easing = LinearEasing), "working-rim")
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
