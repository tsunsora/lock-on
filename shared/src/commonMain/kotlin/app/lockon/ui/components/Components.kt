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
import androidx.compose.ui.text.font.FontWeight
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
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
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


/** Simple vertical bar chart for level and cost curves. */
@Composable
fun BarChart(title: String, bars: List<Pair<Int, Int>>, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.secondary) {
    Column(modifier) {
        SectionHeader(title)
        Spacer(Modifier.height(8.dp))
        if (bars.isEmpty()) {
            Text("No main-deck cards yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        val max = bars.maxOf { it.second }.coerceAtLeast(1)
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            bars.forEachIndexed { index, (label, count) ->
                val target = if (shown) (count.toFloat() / max).coerceAtLeast(0.02f) else 0.02f
                val grow by animateFloatAsState(target, motion(Motion.LONG, delayMillis = index * Motion.STAGGER), label = "bar")
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text(if (count > 0) "$count" else "", style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), color = color)
                    // Bars are lit columns: a bright cap over a fading body, cut like armour.
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight(fraction = grow * 0.8f)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                if (count > 0) Brush.verticalGradient(listOf(color, color.copy(alpha = 0.35f)))
                                else Brush.verticalGradient(listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant)),
                            )
                            .drawBehind { if (count > 0) drawRect(Color.White.copy(alpha = 0.7f), size = Size(size.width, 2.dp.toPx())) },
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Text("$label", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
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
        fontWeight = FontWeight.Bold,
        color = foreground,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.clip(PillShape).background(background).padding(horizontal = 10.dp, vertical = 3.dp),
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

/** A large glass panel over the sky; see [GlassPanel]. */
@Composable
fun Panel(modifier: Modifier = Modifier, title: String? = null, readout: String? = null, content: @Composable () -> Unit) {
    GlassPanel(modifier, title = title, readout = readout, content = content)
}

/**
 * Section header (UX rule V2): the label in friendly bold type, a soft rule fading out,
 * and the count in a tinted pill when the section has one.
 */
@Composable
fun SectionHeader(label: String, modifier: Modifier = Modifier, readout: String? = null, trailing: @Composable () -> Unit = {}) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
        Box(
            Modifier.weight(1f).padding(horizontal = 10.dp).height(1.dp)
                .background(Brush.horizontalGradient(listOf(scheme.outlineVariant, Color.Transparent))),
        )
        if (readout != null) CountPill(readout)
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

/** The Lock On targeting reticle, drawn rather than shipped as an image. */
@Composable
fun Reticle(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier.size(28.dp)) {
        val stroke = size.minDimension * 0.09f
        val r = size.minDimension * 0.3f
        drawCircle(color, radius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawCircle(color, radius = stroke)
        val c = center
        val near = r * 0.55f
        val far = size.minDimension / 2
        listOf(Offset(1f, 0f), Offset(-1f, 0f), Offset(0f, 1f), Offset(0f, -1f)).forEach { d ->
            drawLine(color, c + d * (r - near / 2), c + d * far, strokeWidth = stroke)
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
 * like the curve a game deck builder keeps under the deck.
 */
@Composable
fun MiniCurve(bars: List<Pair<Int, Int>>, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 64.dp, caption: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    val max = bars.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Column(modifier.fillMaxWidth()) {
        if (caption) Text("Level curve", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            bars.forEachIndexed { index, (level, count) ->
                val grow by animateFloatAsState(count.toFloat() / max, motion(Motion.LONG, delayMillis = index * Motion.STAGGER), label = "curve")
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text(if (count > 0) "$count" else "", style = MaterialTheme.typography.labelMedium.merge(TabularNumbers), color = scheme.primary)
                    Box(
                        // The bar gets the room left after its count and label, so heights compare honestly.
                        Modifier.fillMaxWidth(0.72f).weight(1f, fill = false).fillMaxHeight(fraction = grow.coerceAtLeast(0.06f))
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(Brush.verticalGradient(listOf(scheme.primary, scheme.primary.copy(alpha = 0.25f)))),
                    )
                    Text("Lv$level", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * A deck at a glance: a title row, the level curve across the full width, and the counts by type,
 * Burst and Blockers as one row of even tiles. Zero counts are dimmed so the cards that are there
 * stand out. Used by the editor's stats strip and the exported deck image.
 */
@Composable
fun DeckGlance(stats: app.lockon.rules.DeckStats, modifier: Modifier = Modifier, curveHeight: androidx.compose.ui.unit.Dp = 88.dp) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Level curve", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, modifier = Modifier.weight(1f))
            Text("${stats.mainCount} cards", style = MaterialTheme.typography.labelMedium.merge(TabularNumbers), color = scheme.onSurfaceVariant)
        }
        if (stats.levelCurve.isNotEmpty()) MiniCurve(stats.levelCurve, Modifier.fillMaxWidth(), height = curveHeight, caption = false)
        Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.outlineVariant.copy(alpha = 0.6f)))
        val counts = listOf(
            "Units" to (stats.typeCounts[app.lockon.model.CardType.UNIT] ?: 0),
            "Pilots" to (stats.typeCounts[app.lockon.model.CardType.PILOT] ?: 0),
            "Commands" to (stats.typeCounts[app.lockon.model.CardType.COMMAND] ?: 0),
            "Bases" to (stats.typeCounts[app.lockon.model.CardType.BASE] ?: 0),
            "Burst" to stats.burstCount,
            "Blockers" to stats.blockerCount,
        )
        // Each tile takes its label's width and the gaps are shared, so "Commands" fits on a phone.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            counts.forEachIndexed { i, (label, n) ->
                val dim = n == 0
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$n", style = MaterialTheme.typography.titleMedium.merge(TabularNumbers),
                        color = when {
                            dim -> scheme.onSurfaceVariant.copy(alpha = 0.5f)
                            i >= 4 -> scheme.primary
                            else -> scheme.onSurface
                        },
                    )
                    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false,
                        color = scheme.onSurfaceVariant.copy(alpha = if (dim) 0.6f else 1f))
                }
            }
        }
    }
}
