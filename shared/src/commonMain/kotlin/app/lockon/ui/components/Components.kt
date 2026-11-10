package app.lockon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lockon.model.Card
import app.lockon.model.Printing
import app.lockon.sync.QrMatrix
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.theme.TabularNumbers
import app.lockon.ui.theme.cardColor
import app.lockon.ui.theme.onCardColor

/** Placeholder card face until artwork is licensed: color band with cost and level. */
@Composable
fun CardThumb(card: Card?, modifier: Modifier = Modifier) {
    CardImage(card, modifier.width(48.dp))
}

@Composable
fun CardRow(
    card: Card,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardThumb(card)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(card.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    card.cardNumber,
                    card.type.label,
                    card.colors.joinToString("/") { it.label }.ifEmpty { null },
                    card.ap?.let { "AP $it" },
                    card.hp?.let { "HP $it" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}


@Composable
fun QrCodeView(matrix: QrMatrix, modifier: Modifier = Modifier) {
    Surface(color = Color.White, shape = LockOnShapes.panel, modifier = modifier) {
        Canvas(Modifier.padding(12.dp).fillMaxSize()) {
            val cell = size.minDimension / matrix.size
            for (y in 0 until matrix.size) for (x in 0 until matrix.size) {
                if (matrix[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}

@Composable
fun Badge(text: String, background: Color, foreground: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = foreground,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.clip(PillShape).background(background).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    // Empty screens get presence, not a lone line of text: the open reticle, a real title, and a
    // comfortable measure on wide windows.
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 520.dp).padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LockIndicator(locked = false, size = 88.dp)
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            action()
        }
    }
}

/** A large solid panel over the sky; see [GlassPanel]. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    GlassPanel(modifier, content = content)
}

/**
 * Section header (UX rule V2): the label, and beside it the count in the quieter ink when the
 * section has one. No rule, pill or mark.
 */
@Composable
fun SectionHeader(label: String, modifier: Modifier = Modifier, readout: String? = null, trailing: @Composable () -> Unit = {}) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
        if (readout != null) {
            // A count rolls as it changes, like the others.
            val number = readout.toIntOrNull()
            val style = MaterialTheme.typography.titleSmall.merge(TabularNumbers)
            if (number != null) RollingNumber(number, style, scheme.onSurfaceVariant, Modifier.padding(start = 8.dp))
            else Text(readout, Modifier.padding(start = 8.dp), style = style, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

/** Small flat colour dots with initials, so colour is never the only signal (UX rule I4). */
@Composable
fun ColorPips(colors: Collection<app.lockon.model.CardColor>, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 18.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        colors.sortedBy { it.ordinal }.forEach { color ->
            ColorDot(cardColor(color), app.lockon.ui.theme.onCardColor(color), color.label.take(1), size = size,
                outlined = color == app.lockon.model.CardColor.WHITE)
        }
    }
}

/**
 * A card in the test hand or shield row: the whole card, or its back when face down. The footer
 * sits underneath for any extra controls.
 */
@Composable
fun CardFace(card: Card?, modifier: Modifier = Modifier, faceDown: Boolean = false, footer: @Composable () -> Unit = {}) {
    Column(modifier) {
        if (faceDown) CardBack(Modifier.fillMaxWidth()) else CardImage(card, Modifier.fillMaxWidth())
        footer()
    }
}

/**
 * A compact level curve for the top of the deck list: one lit column per level with its count,
 * like the curve a game deck builder keeps under the deck. With another [prefix] it draws any curve,
 * such as the cost curve, in the same style.
 */
@Composable
fun MiniCurve(bars: List<Pair<Int, Int>>, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 64.dp, caption: Boolean = true, prefix: String = "Lv", suffixLast: String = "") {
    val scheme = MaterialTheme.colorScheme
    val max = bars.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    // Bars grow from the floor the first time the curve shows (UX rule V5: charts), then follow changes.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    Column(modifier.fillMaxWidth()) {
        if (caption) Text("Level curve", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            bars.forEachIndexed { index, (level, count) ->
                val grow by animateFloatAsState(if (shown) count.toFloat() / max else 0f, motion(Motion.LONG, delayMillis = index * Motion.STAGGER), label = "curve")
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    if (count > 0) RollingNumber(count, MaterialTheme.typography.labelMedium.merge(TabularNumbers), scheme.onSurface)
                    else Text("", style = MaterialTheme.typography.labelMedium)
                    Box(
                        // The bar gets the room left after its count and label, so heights compare honestly.
                        Modifier.widthIn(max = 40.dp).fillMaxWidth(0.72f).weight(1f, fill = false).fillMaxHeight(fraction = grow.coerceAtLeast(0.06f))
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (isTwilight()) app.lockon.ui.theme.ChartSlateTwilight else app.lockon.ui.theme.ChartSlate),
                    )
                    Text("$prefix$level${if (index == bars.lastIndex) suffixLast else ""}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}
