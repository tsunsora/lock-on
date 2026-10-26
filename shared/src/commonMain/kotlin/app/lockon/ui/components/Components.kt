package app.lockon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    val colors = card?.colors.orEmpty()
    val brush = when (colors.size) {
        0 -> Brush.verticalGradient(listOf(cardColor(null), cardColor(null)))
        1 -> Brush.verticalGradient(listOf(cardColor(colors[0]), cardColor(colors[0]).copy(alpha = 0.75f)))
        else -> Brush.verticalGradient(colors.map { cardColor(it) })
    }
    val on = onCardColor(colors.firstOrNull())
    Box(
        modifier.size(width = 40.dp, height = 56.dp).clip(LockOnShapes.thumb).background(brush),
    ) {
        Text(
            card?.cost?.toString() ?: "",
            color = on, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
        )
        Text(
            card?.level?.let { "Lv$it" } ?: card?.type?.label?.take(3) ?: "?",
            color = on, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp),
        )
    }
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

@Composable
fun CardDetailDialog(card: Card, printings: List<Printing>, onDismiss: () -> Unit, extraActions: @Composable () -> Unit = {}) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = extraActions,
        title = { Text(card.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CardThumb(card)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("${card.cardNumber} · ${card.rarity}", style = MaterialTheme.typography.labelLarge)
                        Text(
                            listOfNotNull(
                                card.type.label,
                                card.colors.joinToString("/") { it.label }.ifEmpty { null },
                                card.level?.let { "Level $it" },
                                card.cost?.let { "Cost $it" },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (card.ap != null || card.hp != null) {
                            Text("AP ${card.ap ?: "-"} · HP ${card.hp ?: "-"}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (card.traits.isNotEmpty()) Text("Traits: ${card.traits.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                if (card.zones.isNotEmpty()) Text("Zones: ${card.zones.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                card.link?.let { Text("Link: $it", style = MaterialTheme.typography.bodySmall) }
                Text(card.displayText, style = MaterialTheme.typography.bodyMedium)
                if (card.correctedText != null) {
                    Text("Shows corrected text. Printed: ${card.text}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (printings.isNotEmpty()) {
                    Text("Printings", style = MaterialTheme.typography.titleSmall)
                    printings.forEach {
                        Text("• ${it.label} (${it.printingId}, ${it.rarity}, ${it.language.uppercase()})", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
    )
}

/** Simple vertical bar chart for level and cost curves. */
@Composable
fun BarChart(title: String, bars: List<Pair<Int, Int>>, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.secondary) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        if (bars.isEmpty()) {
            Text("No main-deck cards yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        val max = bars.maxOf { it.second }.coerceAtLeast(1)
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            for ((label, count) in bars) {
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text(if (count > 0) "$count" else "", style = MaterialTheme.typography.labelSmall)
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight(fraction = (count.toFloat() / max).coerceAtLeast(0.02f) * 0.8f)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (count > 0) color else MaterialTheme.colorScheme.outlineVariant),
                    )
                    Text("$label", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        style = MaterialTheme.typography.labelSmall,
        color = foreground,
        modifier = modifier.clip(MaterialTheme.shapes.extraSmall).background(background).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        action()
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.heightIn(min = 0.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, LockOnShapes.panel)
            .panelLines(MaterialTheme.colorScheme.outlineVariant, vents = false),
        shape = LockOnShapes.panel,
        color = MaterialTheme.colorScheme.surface,
        content = content,
    )
}

/** Section header in the HUD style (UX rule V2): amber tick, uppercase label, optional readout. */
@Composable
fun HudHeader(label: String, modifier: Modifier = Modifier, readout: String? = null, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 3.dp, height = 14.dp).background(MaterialTheme.colorScheme.primary))
        Spacer(Modifier.width(8.dp))
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelLarge.merge(TabularNumbers),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        if (readout != null) {
            Text(readout, style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), color = MaterialTheme.colorScheme.onSurface)
        }
        trailing()
    }
}

/** Small color squares with initials, so color is never the only signal (UX rule I4). */
@Composable
fun ColorPips(colors: Collection<app.lockon.model.CardColor>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        colors.sortedBy { it.ordinal }.forEach { color ->
            Box(
                Modifier.size(18.dp).clip(LockOnShapes.thumb).background(cardColor(color)),
                contentAlignment = Alignment.Center,
            ) {
                Text(color.label.take(1), style = MaterialTheme.typography.labelSmall, color = app.lockon.ui.theme.onCardColor(color),
                    fontWeight = FontWeight.Bold)
            }
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

/** Larger card face for grid views and sample hands. */
@Composable
fun CardFace(card: Card?, modifier: Modifier = Modifier, faceDown: Boolean = false, footer: @Composable () -> Unit = {}) {
    val colors = card?.colors.orEmpty()
    val band = if (faceDown) MaterialTheme.colorScheme.surfaceVariant else cardColor(colors.firstOrNull())
    val on = if (faceDown) MaterialTheme.colorScheme.onSurfaceVariant else onCardColor(colors.firstOrNull())
    Column(
        modifier.clip(LockOnShapes.tile).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, LockOnShapes.tile),
    ) {
        Box(
            Modifier.fillMaxWidth().height(64.dp).background(
                if (colors.size > 1 && !faceDown) Brush.horizontalGradient(colors.map { cardColor(it) }) else Brush.verticalGradient(listOf(band, band.copy(alpha = 0.8f))),
            ),
        ) {
            if (faceDown) {
                LockOnMark(Modifier.align(Alignment.Center), reticleColor = MaterialTheme.colorScheme.outline)
            } else {
                Text(card?.cost?.toString() ?: "", color = on, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
                Text(card?.level?.let { "Lv$it" } ?: "", color = on, style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
                Text(card?.type?.label?.uppercase() ?: "", color = on, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp))
            }
        }
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(if (faceDown) "Shield" else card?.name ?: "Unknown card", style = MaterialTheme.typography.labelLarge,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.heightIn(min = 36.dp))
            if (!faceDown && card != null) {
                Text(card.cardNumber, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                if (card.ap != null || card.hp != null) {
                    Text(
                        "AP ${card.ap ?: "–"} · HP ${card.hp ?: "–"}",
                        style = MaterialTheme.typography.labelSmall.merge(TabularNumbers),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    )
                }
            }
            footer()
        }
    }
}
