package app.lockon.ui.components
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.runtime.Stable
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.theme.SuitColors
import app.lockon.ui.theme.TabularNumbers
import app.lockon.platform.imageBitmapOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.unit.Constraints
import androidx.compose.runtime.State
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.requireDensity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.roundToInt

// The sky-glass layer (UX rules V1, V8, V9): a bright sky backdrop, frosted glass panels, pill
// buttons, chips and tabs, round orbs and medallions, and the hover and add-card feedback that makes
// building feel lively. Every animation respects reduced motion.

/** True on the twilight (dark) theme; glass and glows are tuned per theme. */
@Composable
@ReadOnlyComposable
fun isTwilight(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** The action and selection colour: teal. Lit pills use it with [OnAccent] text. */
val Accent: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val OnAccent: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onPrimary

/** Soft gold for celebration only: a full gauge, a legal deck, the Lock On moment. */
val Gold = SuitColors.vFinYellow

/** Frosted glass for the current theme: its fill, a stronger fill for small controls, and the rim. */
data class GlassColors(val fill: Color, val strong: Color, val rim: Color, val rimLow: Color, val shade: Color)

@Composable
@ReadOnlyComposable
fun glassColors(): GlassColors = if (isTwilight()) {
    GlassColors(Color.White.copy(alpha = 0.07f), Color(0xFF2A3562).copy(alpha = 0.94f), Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.06f), Color(0xFF060B1E))
} else {
    GlassColors(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.9f), Color.White, Color(0xFFBFD2E6), Color(0xFF6A88B0))
}

/** A painted sky and the theme it was painted for. */
private class SkyPaint(val image: ImageBitmap, val twilight: Boolean)

/**
 * The sky behind every screen: a clear gradient, soft coloured glows, a diagonal beam of light and
 * floating bokeh. It is static: nothing loops.
 * The smooth parts are painted per pixel by [paintSky] so they never band.
 */
@Composable
fun SkyBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val twilight = isTwilight()
    var area by remember { mutableStateOf(IntSize.Zero) }
    // Painting takes a few dozen milliseconds, so it runs off the main thread. While a window is being
    // resized the last sky stretches to fit, and the sky repaints once the size settles.
    val sky by produceState<SkyPaint?>(null, area, twilight) {
        if (area.width <= 0 || area.height <= 0) return@produceState
        if (value?.twilight == twilight) delay(80)
        val scale = minOf(1f, 4096f / maxOf(area.width, area.height))
        val pw = (area.width * scale).roundToInt().coerceAtLeast(1)
        val ph = (area.height * scale).roundToInt().coerceAtLeast(1)
        value = withContext(Dispatchers.Default) { SkyPaint(imageBitmapOf(paintSky(pw, ph, twilight), pw, ph), twilight) }
    }
    Box(
        modifier.background(MaterialTheme.colorScheme.background).onSizeChanged { area = it }.drawBehind {
            val w = size.width
            val h = size.height
            val m = size.maxDimension
            val painted = sky?.takeIf { it.twilight == twilight }
            if (painted != null) {
                drawImage(painted.image, dstSize = IntSize(ceil(w).toInt(), ceil(h).toInt()), filterQuality = FilterQuality.Low)
            } else {
                drawRect(Brush.verticalGradient(if (twilight) TwilightSky else DaySky))
            }
            // Bokeh at fixed places, so every frame and screenshot is the same. Twilight's bokeh are
            // glows, painted with the sky.
            if (!twilight) SkyBokeh.forEachIndexed { i, b ->
                val c = Offset(w * b.x, h * b.y)
                val r = m * b.radius
                drawCircle(Color.White.copy(alpha = 0.28f), r, c)
                if (i % 2 == 0) drawCircle(Color.White.copy(alpha = 0.5f), r, c, style = Stroke(1.5f))
            }
        },
        content = content,
    )
}

/**
 * A frosted glass surface: translucent fill, a bright rim fading downward, clipped to [shape].
 */
@Composable
fun Modifier.glass(shape: Shape = LockOnShapes.panel, strong: Boolean = false): Modifier {
    val g = glassColors()
    // Frosted: a touch brighter at the top, so the sky shows through more toward the bottom.
    val fill = if (strong) SolidColor(g.strong) else Brush.verticalGradient(listOf(g.fill, g.fill.copy(alpha = g.fill.alpha * 0.7f)))
    return clip(shape)
        .background(fill)
        .border(BorderStroke(1.2.dp, Brush.verticalGradient(listOf(g.rim, g.rimLow))), shape)
}

/** The glass rim on its own, for surfaces that draw their own fill (art banners, dialogs). */
@Composable
fun Modifier.glassFrame(shape: Shape = LockOnShapes.panel): Modifier {
    val g = glassColors()
    return border(BorderStroke(1.2.dp, Brush.verticalGradient(listOf(g.rim, g.rimLow))), shape)
}

/** A large glass panel with an optional title row (UX rule V1). */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    readout: String? = null,
    accent: Color = Accent,
    content: @Composable () -> Unit,
) {
    Column(modifier.glass()) {
        if (title != null) PanelTitle(title, readout, accent)
        content()
    }
}

/** A panel's title row: the title and the count in a tinted pill. */
@Composable
fun PanelTitle(title: String, readout: String? = null, accent: Color = Accent, trailing: @Composable () -> Unit = {}) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().height(42.dp).padding(start = 16.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, modifier = Modifier.weight(1f))
        trailing()
        if (readout != null) CountPill(readout, accent)
    }
}

/** A number in a soft tinted pill, for panel and section counts. */
@Composable
fun CountPill(text: String, color: Color = Accent) {
    Text(
        text, style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), color = color,
        modifier = Modifier.clip(LockOnShapes.button).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** A pill shape, for tabs, stamps and gauges. */
val PillShape: Shape = LockOnShapes.button

enum class GameButtonStyle { PRIMARY, SECONDARY, GHOST }

/**
 * A game button: a pill. Primary is a glossy teal pill with a soft glow; secondary is a white glass
 * pill with a teal rim; ghost is teal text. Hover brightens a touch, press sinks a hair.
 */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GameButtonStyle = GameButtonStyle.PRIMARY,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    large: Boolean = false,
    compact: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val g = glassColors()
    val accent = Accent
    val onAccent = OnAccent
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val lift by animateFloatAsState(if (hovered && enabled) 1f else 0f, motion(Motion.SHORT), label = "button-hover")
    val squish by animateFloatAsState(if (pressed) 0.98f else 1f, motion(Motion.SHORT), label = "button-press")
    val shape = LockOnShapes.button
    val fill: Brush
    val content: Color
    val rim: Color
    when {
        !enabled -> { fill = SolidColor(scheme.onSurface.copy(alpha = 0.06f)); content = scheme.outline; rim = Color.Transparent }
        style == GameButtonStyle.PRIMARY -> {
            fill = Brush.verticalGradient(listOf(lighten(accent, 0.22f + 0.06f * lift), lighten(accent, 0.04f * lift)))
            content = onAccent; rim = lighten(accent, 0.45f)
        }
        style == GameButtonStyle.SECONDARY -> {
            fill = SolidColor(if (hovered) lighten(g.strong, 0.06f) else g.strong)
            content = accent; rim = accent.copy(alpha = 0.55f + 0.35f * lift)
        }
        else -> { fill = SolidColor(accent.copy(alpha = 0.10f * lift)); content = accent; rim = Color.Transparent }
    }
    Box(
        modifier
            .heightIn(min = if (large) 54.dp else if (compact) 38.dp else 44.dp)
            .graphicsLayer {
                scaleX = squish; scaleY = squish
                if (style == GameButtonStyle.PRIMARY && enabled) {
                    this.shape = shape
                    shadowElevation = (2f + 3f * lift) * density
                    ambientShadowColor = accent; spotShadowColor = accent
                }
            }
            .clip(shape)
            .background(fill)
            .border(BorderStroke(1.dp, rim), shape)
            .hoverable(interaction, enabled)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (large) 26.dp else if (compact) 14.dp else 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(if (large) 22.dp else 18.dp))
            // Two lines at most, centred, so a long label wraps instead of being cut off.
            Text(
                text, color = content, maxLines = 2, textAlign = TextAlign.Center,
                style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * An icon-only action as a round glass button, the same 44 dp as [GameButton] and the search field,
 * so a header row of mixed controls lines up (UX rule V1). [label] is read out and is the tooltip's
 * text on desktop (I5). Over art, [onArt] makes the disc a little more solid so the icon stays clear.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val scheme = MaterialTheme.colorScheme
    val g = glassColors()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val squish by animateFloatAsState(if (pressed) 0.94f else 1f, motion(Motion.SHORT), label = "icon-press")
    Box(
        modifier
            .size(ControlSize)
            .graphicsLayer { scaleX = squish; scaleY = squish }
            .clip(CircleShape)
            .background(if (hovered) lighten(g.strong, 0.08f) else g.strong)
            .border(BorderStroke(1.dp, if (hovered) Accent.copy(alpha = 0.6f) else g.rimLow), CircleShape)
            .hoverable(interaction)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (hovered) scheme.primary else tint, modifier = Modifier.size(22.dp))
    }
}

/** The one height for buttons, fields and icon buttons that share a row. */
val ControlSize = 44.dp

/**
 * Pill tabs on a glass track: the selected tab is a teal pill. They scroll when the track is
 * narrower than its tabs, or share the width equally with [fill].
 */
@Composable
fun PillTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, fill: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val accent = Accent
    val onAccent = OnAccent
    val track = if (fill) Modifier.fillMaxWidth() else Modifier.horizontalScroll(rememberScrollState())
    Box(modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(track.glass(LockOnShapes.button, strong = true).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            labels.forEachIndexed { i, label ->
                val isSelected = i == selected
                val interaction = remember { MutableInteractionSource() }
                val hovered by interaction.collectIsHoveredAsState()
                val bg by animateColorAsState(
                    when {
                        isSelected -> accent
                        hovered -> scheme.onSurface.copy(alpha = 0.07f)
                        else -> Color.Transparent
                    },
                    motion(Motion.SHORT), label = "tab",
                )
                Box(
                    (if (fill) Modifier.weight(1f) else Modifier).height(36.dp).clip(LockOnShapes.button).background(bg)
                        .hoverable(interaction)
                        .selectable(isSelected, interaction, indication = null, role = Role.Tab) { onSelect(i) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
                        color = if (isSelected) onAccent else scheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * A deck gauge (UX rule I1): label, count over limit, and a rounded bar. Teal while filling, gold
 * when exactly full, red when over. The colour and the number say it; nothing is drawn on the bar.
 */
@Composable
fun ProgressGauge(label: String, count: Int, limit: Int, modifier: Modifier = Modifier, compact: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val fraction by animateFloatAsState((count.toFloat() / limit.coerceAtLeast(1)).coerceIn(0f, 1f), motion(Motion.LONG), label = "gauge")
    val full = count == limit
    val over = count > limit
    val accent = Accent
    val numberColor = when {
        over -> scheme.error
        full -> if (isTwilight()) Gold else scheme.tertiary
        else -> scheme.onSurface
    }
    val fillBrush = when {
        over -> SolidColor(scheme.error)
        full -> Brush.horizontalGradient(listOf(Color(0xFFFFE08A), Gold, Color(0xFFFFA94D)))
        else -> Brush.horizontalGradient(listOf(lighten(accent, 0.3f), accent))
    }
    val track = scheme.onSurface.copy(alpha = if (isTwilight()) 0.12f else 0.1f)
    Column(modifier.widthIn(min = 110.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(bottom = 2.dp))
            RollingNumber(count, (if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge).merge(TabularNumbers),
                numberColor, Modifier.bumpOnIncrease(count))
            Text("/$limit", style = MaterialTheme.typography.labelMedium.merge(TabularNumbers), color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
        }
        Canvas(Modifier.fillMaxWidth().height(if (compact) 6.dp else 8.dp)) {
            val r = CornerRadius(size.height / 2)
            drawRoundRect(track, cornerRadius = r)
            if (fraction > 0f) drawRoundRect(fillBrush, size = Size(size.width * fraction, size.height), cornerRadius = r)
        }
    }
}

/** A round cost orb in the card's colour, glossy like a game's element orb. */
@Composable
fun CostOrb(value: Int?, color: Color, onColor: Color, modifier: Modifier = Modifier, size: Dp = 30.dp, text: String? = null) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2
            drawCircle(Brush.radialGradient(listOf(lighten(color, 0.45f), color, darken(color, 0.25f)), Offset(r * 0.7f, r * 0.6f), r * 1.4f), r)
            drawCircle(Color.White.copy(alpha = 0.9f), r - 1.dp.toPx(), style = Stroke(1.6.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.35f), r * 0.3f, Offset(r * 0.68f, r * 0.55f))
        }
        CenteredGlyph(text ?: value?.toString() ?: "–", onColor, (size.value * 0.46f).sp)
    }
}

/**
 * Copies as dots (Runeterra-style pips): teal for each copy, gold when the limit is reached, red
 * beyond it. The count itself is always shown beside them as a number (UX rule I4).
 */
@Composable
fun CopyPips(count: Int, limit: Int, modifier: Modifier = Modifier, pip: Dp = 7.dp) {
    val scheme = MaterialTheme.colorScheme
    val total = maxOf(limit, count).coerceAtMost(6)
    val lit = if (count == limit) (if (isTwilight()) Gold else scheme.tertiary) else Accent
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val on = i < count
            val color = if (i >= limit) scheme.error else if (on) lit else scheme.onSurface.copy(alpha = 0.22f)
            Canvas(Modifier.size(pip)) {
                if (on) drawCircle(color) else drawCircle(color, style = Stroke(1.dp.toPx()))
            }
        }
    }
}

/**
 * Fades the last [width] of a sideways-scrolling row to nothing, so a row cut off at the edge
 * shows there is more instead of slicing a chip in half (UX rule N4). Put it before the scroll.
 */
fun Modifier.fadeEnd(width: Dp = 40.dp): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = size.width - width.toPx(), endX = size.width),
                blendMode = BlendMode.DstOut)
        }

/** Whether the pointer is over an element marked with [Modifier.hoverState]. */
@Stable
class HoverState {
    var hovered by mutableStateOf(false)
        internal set
}

/**
 * Tracks the pointer over this element in [state]. Unlike `collectIsHoveredAsState`, it starts no
 * coroutine until a pointer first arrives, so tiles scrolling through a grid cost nothing extra.
 */
fun Modifier.hoverState(state: HoverState): Modifier = pointerInput(state) {
    awaitPointerEventScope {
        while (true) {
            when (awaitPointerEvent().type) {
                PointerEventType.Enter -> state.hovered = true
                PointerEventType.Exit -> state.hovered = false
            }
        }
    }
}

/**
 * Hover lift for cards and tiles on desktop: rises a little and casts a soft shadow tinted with its
 * colour. Press sinks it a hair. Phones never hover, so they only get the press. A modifier node
 * rather than `composed`, so a grid of cards scrolling into view builds no extra state per card.
 */
fun Modifier.hoverLift(
    interaction: MutableInteractionSource,
    glow: Color,
    shape: Shape,
    lift: Dp = 3.dp,
    resting: Float = 0f,
): Modifier = hoverable(interaction) then HoverLiftElement(interaction, glow, shape, lift, resting)

private data class HoverLiftElement(
    val interaction: MutableInteractionSource,
    val glow: Color,
    val shape: Shape,
    val lift: Dp,
    val resting: Float,
) : ModifierNodeElement<HoverLiftNode>() {
    override fun create() = HoverLiftNode(interaction, glow, shape, lift, resting)
    override fun update(node: HoverLiftNode) = node.update(interaction, glow, shape, lift, resting)
}

internal class HoverLiftNode(
    private var interaction: MutableInteractionSource,
    private var glow: Color,
    private var shape: Shape,
    private var lift: Dp,
    private var resting: Float,
) : Modifier.Node(), LayoutModifierNode, DrawModifierNode, PointerInputModifierNode, CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode {
    private val hover = Animatable(0f)
    // Where the pointer is over the card, for the spotlight that follows it.
    private var pointer = Offset.Unspecified
    // Where the card sits in the window, to place a [SpotlightGrid]'s pointer on it.
    private var origin = Offset.Zero
    private var area = IntSize.Zero
    private var grid: GridSpotlight? = null
    // Read while drawing, so bumping it redraws this card. invalidateDraw from the grid's pointer
    // handler doesn't reach a card drawn inside its own layer.
    private var light by androidx.compose.runtime.mutableIntStateOf(0)

    fun relight() { light++ }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) { origin = coordinates.positionInRoot(); area = coordinates.size }

    override fun onAttach() { grid = currentValueOf(LocalGridSpotlight)?.also { it.cards += this } }

    /** Whether [point], in window coordinates, is close enough to light this card's rim. */
    fun within(point: Offset): Boolean {
        if (point == Offset.Unspecified || area == IntSize.Zero) return false
        val reach = with(requireDensity()) { GridSpotlightReach.toPx() }
        val dx = maxOf(origin.x - point.x, 0f, point.x - origin.x - area.width)
        val dy = maxOf(origin.y - point.y, 0f, point.y - origin.y - area.height)
        return dx * dx + dy * dy < reach * reach
    }
    private val press = Animatable(1f)
    private var hovered = false
    private var watching: Job? = null

    // Nothing starts until a pointer arrives. On desktop every coroutine a tile starts is posted to
    // the window's event queue, so a grid scrolling in rows of tiles that only watch would drop frames.
    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass != PointerEventPass.Main) return
        if (watching == null) watch()
        when (pointerEvent.type) {
            PointerEventType.Enter -> setHovered(true)
            PointerEventType.Exit -> setHovered(false)
        }
        pointerEvent.changes.firstOrNull()?.let { pointer = it.position; invalidateDraw() }
    }

    override fun onCancelPointerInput() = setHovered(false)

    // A soft light under the pointer and a brighter stretch of rim near it, so the card answers
    // where the pointer is, not only that it arrived. Drawn over the content; it never moves anything.
    override fun ContentDrawScope.draw() {
        drawContent()
        // In a [SpotlightGrid] the rims of every card near the pointer catch its light, not only the
        // card under it, so the whole grid answers the pointer.
        light
        val grid = grid
        val near = if (grid != null && within(grid.pointer)) grid.level?.value ?: 0f else 0f
        if (near > 0f && grid != null) {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(outline, Brush.radialGradient(listOf(lighten(glow, 0.3f).copy(alpha = 0.7f * near), Color.Transparent),
                grid.pointer - origin, GridSpotlightReach.toPx()), style = Stroke(1.5.dp.toPx()))
        }
        val t = hover.value
        if (t <= 0f || pointer == Offset.Unspecified) return
        val reach = size.maxDimension * 0.9f
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(outline, Brush.radialGradient(listOf(Color.White.copy(alpha = 0.16f * t), Color.Transparent), pointer, reach * 0.6f))
        drawOutline(outline, Brush.radialGradient(listOf(lighten(glow, 0.3f).copy(alpha = 0.9f * t), Color.Transparent), pointer, reach),
            style = Stroke(1.5.dp.toPx()))
    }

    override fun onDetach() { watching = null; hovered = false; pointer = Offset.Unspecified; grid?.cards?.remove(this); grid = null }

    // A lazy grid reuses this node for another card, which starts at rest.
    override fun onReset() {
        hovered = false
        // Most reused tiles are already at rest, and starting a coroutine for each costs frames.
        if (isAttached && (hover.value != 0f || press.value != 1f)) coroutineScope.launch { hover.snapTo(0f); press.snapTo(1f) }
    }

    fun update(interaction: MutableInteractionSource, glow: Color, shape: Shape, lift: Dp, resting: Float) {
        if (interaction != this.interaction) { watching?.cancel(); watching = null }
        this.interaction = interaction; this.glow = glow; this.shape = shape; this.lift = lift; this.resting = resting
        invalidatePlacement()
    }

    private fun setHovered(on: Boolean) {
        if (on == hovered || !isAttached) return
        hovered = on
        val reduce = currentValueOf(LocalReduceMotion)
        coroutineScope.launch { hover.settle(if (on) 1f else 0f, Motion.MEDIUM, reduce) }
    }

    // Presses come from the interaction source, so a long touch counts too. Until a pointer has
    // reached the tile a keyboard press doesn't sink it; the focus ripple still shows it.
    private fun watch() {
        watching = coroutineScope.launch {
            var presses = 0
            interaction.interactions.collect { event ->
                when (event) {
                    is PressInteraction.Press -> presses++
                    is PressInteraction.Release, is PressInteraction.Cancel -> presses = (presses - 1).coerceAtLeast(0)
                    else -> return@collect
                }
                launch { press.settle(if (presses > 0) 0.985f else 1f, Motion.SHORT, currentValueOf(LocalReduceMotion)) }
            }
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                // Read here, in the layer, so hovering redraws the card without recomposing or relaying it out.
                val t = hover.value
                val s = (1f + 0.01f * t) * press.value
                scaleX = s; scaleY = s
                translationY = -lift.toPx() * t
                this.shape = this@HoverLiftNode.shape
                shadowElevation = 8.dp.toPx() * maxOf(t, resting)
                ambientShadowColor = glow.copy(alpha = 0.5f); spotShadowColor = glow.copy(alpha = 0.5f)
            }
        }
    }
}

/** The pointer over a [SpotlightGrid], in window coordinates, and how strongly the grid is lit. */
@Stable
class GridSpotlight {
    // A plain field, not state: a move redraws only the cards near the pointer, not the whole grid.
    var pointer = Offset.Unspecified
        private set
    internal var level: State<Float>? = null
    internal val cards = mutableSetOf<HoverLiftNode>()

    internal fun moveTo(to: Offset) {
        val from = pointer
        pointer = to
        for (card in cards) if (card.within(from) || card.within(to)) card.relight()
    }
}

/** The grid of cards around this composable, if it is a [SpotlightGrid]. */
val LocalGridSpotlight = compositionLocalOf<GridSpotlight?> { null }

/** How far a [SpotlightGrid]'s light reaches across neighbouring cards' rims. */
private val GridSpotlightReach = 140.dp

/**
 * A grid whose [hoverLift] cards share the pointer: as it moves, the rims of the cards around it
 * light up, not only the one under it (after Hyperplexed's multi-card hover). The light fades in and
 * out over half a second as the pointer enters and leaves; with reduced motion it switches at once.
 */
@Composable
fun SpotlightGrid(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val spot = remember { GridSpotlight() }
    var inside by remember { mutableStateOf(false) }
    spot.level = animateFloatAsState(if (inside) 1f else 0f, if (LocalReduceMotion.current) snap() else tween(500), label = "grid-spotlight")
    val origin = remember { floatArrayOf(0f, 0f) }
    Box(
        modifier
            .onGloballyPositioned { val o = it.positionInRoot(); origin[0] = o.x; origin[1] = o.y }
            .pointerInput(spot) {
                awaitPointerEventScope {
                    while (true) {
                        // Seen on the way in, so the cards' own handlers still get every event.
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        when (event.type) {
                            PointerEventType.Exit -> inside = false
                            else -> {
                                inside = true
                                event.changes.firstOrNull()?.let { spot.moveTo(it.position + Offset(origin[0], origin[1])) }
                            }
                        }
                    }
                }
            },
        content = { CompositionLocalProvider(LocalGridSpotlight provides spot) { content() } },
    )
}

/** Animates to [target] the way [motion] would: an emphasized tween, or at once when motion is reduced. */
private suspend fun Animatable<Float, *>.settle(target: Float, durationMillis: Int, reduce: Boolean) {
    if (reduce) snapTo(target) else animateTo(target, tween(durationMillis, easing = Motion.emphasized))
}

/** A small nudge when [value] grows, so an added copy registers (UX rule V5: what changed). */
fun Modifier.bumpOnIncrease(value: Int): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val scale = remember { Animatable(1f) }
    var last by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (!reduce && value > last) {
            scale.snapTo(1.08f)
            scale.animateTo(1f, tween(Motion.MEDIUM, easing = Motion.emphasizedDecelerate))
        }
        last = value
    }
    graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}

/**
 * A faint tint over a card when a copy is added (or a red one when removed), fading in under half
 * a second. Drawn over the content, so it never moves anything. A modifier node, like [hoverLift].
 */
fun Modifier.flashOnChange(value: Int, add: Color, remove: Color, shape: Shape): Modifier =
    this then FlashElement(value, add, remove, shape)

private data class FlashElement(val value: Int, val add: Color, val remove: Color, val shape: Shape) : ModifierNodeElement<FlashNode>() {
    override fun create() = FlashNode(value, add, remove, shape)
    override fun update(node: FlashNode) = node.update(value, add, remove, shape)
}

private class FlashNode(
    private var value: Int,
    private var add: Color,
    private var remove: Color,
    private var shape: Shape,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val flash = Animatable(0f)
    private var tint = add
    private var reused = false

    // A lazy grid reuses this node for another card: its count is new, not a change.
    override fun onReset() {
        reused = true
        if (isAttached && flash.value != 0f) coroutineScope.launch { flash.snapTo(0f) }
    }

    fun update(value: Int, add: Color, remove: Color, shape: Shape) {
        if (isAttached && !reused && value != this.value && !currentValueOf(LocalReduceMotion)) {
            tint = if (value > this.value) add else remove
            coroutineScope.launch {
                flash.snapTo(1f)
                flash.animateTo(0f, tween(420, easing = Motion.emphasizedDecelerate))
            }
        }
        reused = false
        this.value = value; this.add = add; this.remove = remove; this.shape = shape
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val a = flash.value
        if (a > 0f) {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(outline, tint.copy(alpha = 0.12f * a))
            drawOutline(outline, tint.copy(alpha = 0.7f * a), style = Stroke(1.5.dp.toPx()))
        }
    }
}

/** Provides [color] as the content colour, for icons inside custom plates. */
@Composable
fun WithContentColor(color: Color, content: @Composable () -> Unit) = CompositionLocalProvider(LocalContentColor provides color, content = content)

fun lighten(color: Color, amount: Float) = Color(
    red = color.red + (1f - color.red) * amount,
    green = color.green + (1f - color.green) * amount,
    blue = color.blue + (1f - color.blue) * amount,
    alpha = color.alpha,
)

fun darken(color: Color, amount: Float) = Color(
    red = color.red * (1f - amount), green = color.green * (1f - amount), blue = color.blue * (1f - amount), alpha = color.alpha,
)

/** A smaller glass card for grouped content such as a device or the update status. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().glass(LockOnShapes.tile), content = content)
}

/**
 * The Lock On moment: when [locked] turns true, a soft glint crosses the content like light on a
 * new card and the rim warms to gold, then fades. It plays only on the change, never when a legal
 * deck opens (UX rule V5).
 */
fun Modifier.shineSweep(locked: Boolean, shape: Shape = RectangleShape): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val sweep = remember { Animatable(1f) }
    var last by remember { mutableStateOf(locked) }
    LaunchedEffect(locked) {
        if (locked && !last && !reduce) {
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(Motion.SIGNATURE * 2, easing = Motion.emphasized))
        }
        last = locked
    }
    drawWithContent {
        drawContent()
        val p = sweep.value
        if (p < 1f) {
            val x = -size.width * 0.3f + size.width * 1.6f * p
            val band = size.width * 0.16f
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Gold.copy(alpha = 0.1f), Color.Transparent),
                    start = Offset(x - band, 0f), end = Offset(x + band, size.height * 0.5f),
                ),
            )
            drawOutline(shape.createOutline(size, layoutDirection, this), Gold.copy(alpha = (1f - p) * 0.5f), style = Stroke(1.5.dp.toPx()))
        }
    }
}

/**
 * The band at the top of wide windows that the floating navigation shares with each screen's own
 * header (UX rule N5). [start] and [end] are taken by the player card and the icon tray; a screen
 * lays its header out between them, so no row is spent on navigation alone. Phones get an empty band.
 */
data class TopBand(val height: Dp = 0.dp, val start: Dp = 0.dp, val end: Dp = 0.dp)

val LocalTopBand = compositionLocalOf { TopBand() }

/** Pads a header row into the space the floating navigation leaves free. */
@Composable
fun Modifier.clearOfNav(): Modifier {
    val band = LocalTopBand.current
    return padding(start = band.start, end = band.end).heightIn(min = band.height)
}

/**
 * A screen's title row: a big friendly heading and actions at the right. On wide windows it sits in
 * the band between the player card and the icon tray. There is no subtitle: what a screen holds is
 * said by the screen itself (UX rule K5).
 */
@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    val wide = LocalTopBand.current.height > 0.dp
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().clearOfNav()
            .padding(start = 16.dp, end = if (wide) 8.dp else 12.dp, top = if (wide) 0.dp else 10.dp, bottom = if (wide) 0.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = scheme.onBackground, modifier = Modifier.weight(1f))
        actions()
    }
}

/** A compact search field on a glass pill: 44 dp tall, with a search icon, placeholder and clear button. */
@Composable
fun PillSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) = PillField(value, onValueChange, placeholder, modifier, Icons.Default.Search, onClear)

/**
 * A text field on the same glass pill as [PillSearchField], without the search icon, for short
 * entries on a page (a dialog keeps Material's field). Its placeholder says what goes in it.
 */
@Composable
fun PillTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) =
    PillField(value, onValueChange, placeholder, modifier, icon = null, onClear = null)

@Composable
private fun PillField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier,
    icon: ImageVector?,
    onClear: (() -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    val g = glassColors()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = LockOnShapes.button
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interaction,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
        cursorBrush = SolidColor(scheme.primary),
        modifier = modifier.height(44.dp),
        decorationBox = { field ->
            Row(
                Modifier.fillMaxSize().clip(shape).background(g.strong)
                    .border(BorderStroke(if (focused) 1.6.dp else 1.dp, if (focused) scheme.primary else g.rimLow), shape)
                    .padding(start = if (icon != null) 14.dp else 18.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = if (focused) scheme.primary else scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, maxLines = 1)
                    field()
                }
                if (onClear != null) {
                    IconButton(onClick = onClear, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search and filters", tint = scheme.onSurfaceVariant)
                    }
                }
            }
        },
    )
}

/**
 * A colour filter as a flat [ColorDot] with the colour's initial (UX rule I4), matching the deck
 * export: bright with a teal ring when on, softened when off. Five of these take the room of two
 * text chips.
 */
@Composable
fun ColorOrbToggle(label: String, color: Color, onColor: Color, selected: Boolean, target: Dp = 38.dp, outlined: Boolean = false, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val ring = Accent
    Box(
        Modifier.size(target).hoverable(interaction)
            .toggleable(selected, interaction, indication = null, role = Role.Checkbox) { onToggle() }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.graphicsLayer { alpha = if (selected || hovered) 1f else 0.6f }) {
            ColorDot(color, onColor, label.take(1), size = 28.dp, outlined = outlined)
        }
        if (selected) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(ring, radius = 18.dp.toPx(), style = Stroke(2.5.dp.toPx()))
            }
        }
    }
}

/**
 * A dialog on a rounded card: Material's dialog behaviour with the glass rim, a friendly title
 * and [GameButton] actions.
 */
@Composable
fun GameDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: String? = null,
    text: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        modifier = modifier.glassFrame(MaterialTheme.shapes.extraLarge),
        containerColor = if (isTwilight()) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest,
        title = title?.let {
            {
                Text(it, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        },
        text = text,
    )
}

/** An on/off switch as a pill track with a round knob: teal when on. A 48 dp target. */
@Composable
fun PillSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    val accent = Accent
    val position by animateFloatAsState(if (checked) 1f else 0f, motion(Motion.SHORT), label = "switch")
    Box(
        modifier.size(width = 60.dp, height = 48.dp)
            .then(
                if (onCheckedChange != null) Modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(width = 52.dp, height = 30.dp).clip(LockOnShapes.button)
                .background(if (checked) Brush.verticalGradient(listOf(lighten(accent, 0.25f), accent)) else SolidColor(scheme.onSurface.copy(alpha = 0.12f))),
        ) {
            Box(
                Modifier.padding(3.dp).size(24.dp)
                    .graphicsLayer { translationX = position * 22.dp.toPx(); shape = CircleShape; shadowElevation = 2.dp.toPx() }
                    .clip(CircleShape)
                    .background(if (enabled) Color.White else scheme.outline),
            )
        }
    }
}

/** One choice among a few, as pills on a glass track; the chosen one is teal. */
@Composable
fun PillSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val accent = Accent
    val onAccent = OnAccent
    Row(
        modifier.height(44.dp).glass(LockOnShapes.button, strong = true).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val isSelected = i == selected
            val bg by animateColorAsState(if (isSelected) accent else Color.Transparent, motion(Motion.SHORT), label = "segment")
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(LockOnShapes.button).background(bg)
                    .selectable(isSelected, role = Role.RadioButton) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) onAccent else scheme.onSurfaceVariant)
            }
        }
    }
}

/** A filter or choice chip as a pill: white glass, teal when selected, like every other selected pill (UX rule V3). */
@Composable
fun PillChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val g = glassColors()
    FilterChip(
        selected = selected, onClick = onClick, label = label, modifier = modifier, leadingIcon = leadingIcon,
        shape = LockOnShapes.button,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = g.strong,
            labelColor = scheme.onSurface,
            selectedContainerColor = Accent,
            selectedLabelColor = OnAccent,
            selectedLeadingIconColor = OnAccent,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = selected,
            borderColor = g.rimLow, selectedBorderColor = lighten(Accent, 0.4f),
        ),
    )
}

/** One destination in a [PillNavBar]: its label and icon. */
class BottomItem(val label: String, val icon: @Composable () -> Unit)

/**
 * How far content must keep clear of the bottom on phones, because the floating nav bar sits over
 * it. Scrolling lists add it to their bottom padding so their last item can scroll above the bar.
 */
val LocalNavClearance = compositionLocalOf { 0.dp }

/** The nav bar's height, without its margin and the system's gesture area. */
val PillNavHeight = 60.dp

/**
 * How tall the window controls' capsule is: the nav pill's height beside the pill, and
 * [ControlSize] when it shares a screen header's row instead (UX rule K1).
 */
val LocalChromeHeight = compositionLocalOf { PillNavHeight }

/** The room to leave under content for the floating nav bar: its height, its margin and a little air. */
@Composable
fun navClearance(): Dp {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val gesture = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    return PillNavHeight + 12.dp + gesture + 12.dp
}

/** The pill's settle: quick, with no overshoot. */
private val PillSpring = spring<Float>(dampingRatio = 1f, stiffness = 500f)

/**
 * Navigation as a floating pill (UX rules N5, V9), after AnymeX's "Dynamic Pill" bar: a
 * translucent glass capsule with a hairline rim and soft shadow, floating above the bottom edge on
 * phones. The selected destination expands into a teal pill with its icon and label; the others
 * shrink to icons, which still carry their label for screen readers (I5).
 *
 * With [labels] every item shows its label, for wide windows where there is room.
 */
@Composable
fun PillNavBar(
    items: List<BottomItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labels: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = Accent
    val onAccent = OnAccent
    val twilight = isTwilight()
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
    val reduce = LocalReduceMotion.current
    Row(
        modifier
            .graphicsLayer { this.shape = shape; shadowElevation = 16.dp.toPx(); ambientShadowColor = Color.Black.copy(alpha = 0.3f); spotShadowColor = Color.Black.copy(alpha = 0.3f) }
            .clip(shape)
            // Somewhat transparent: colour and shape show through, but there is no backdrop blur on
            // every platform, so it stays opaque enough that text underneath never competes.
            .background(if (twilight) scheme.surfaceContainerHigh.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.9f))
            .border(BorderStroke(0.8.dp, scheme.onSurface.copy(alpha = 0.12f)), shape)
            .height(PillNavHeight)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, item ->
            val isSelected = i == selected
            // The selected item takes more of the bar, so its label has room (AnymeX's 2.4 : 1).
            val share by animateFloatAsState(if (labels || !isSelected) 1f else 2.4f, if (reduce) snap() else PillSpring, label = "pill-share")
            val fill by animateColorAsState(if (isSelected) accent else Color.Transparent, motion(Motion.SHORT), label = "pill-fill")
            val content = if (isSelected) onAccent else scheme.onSurface.copy(alpha = 0.62f)
            Box(
                // With every label showing, items take their own width; otherwise they share the bar.
                (if (labels) Modifier else Modifier.weight(share)).fillMaxHeight()
                    .selectable(isSelected, role = Role.Tab, interactionSource = null, indication = null) { onSelect(i) }
                    .semantics { contentDescription = item.label },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier.clip(LockOnShapes.button).background(fill)
                        .padding(horizontal = if (isSelected) 18.dp else 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(LocalContentColor provides content) {
                        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { item.icon() }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = labels || isSelected,
                        enter = if (reduce) androidx.compose.animation.EnterTransition.None
                        else androidx.compose.animation.expandHorizontally(tween(Motion.MEDIUM, easing = Motion.emphasizedDecelerate)) +
                            androidx.compose.animation.fadeIn(tween(Motion.MEDIUM, delayMillis = Motion.SHORT / 2)),
                        exit = if (reduce) androidx.compose.animation.ExitTransition.None
                        else androidx.compose.animation.shrinkHorizontally(tween(Motion.MEDIUM)) + androidx.compose.animation.fadeOut(tween(Motion.SHORT)),
                    ) {
                        Text(item.label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1,
                            modifier = Modifier.padding(start = 8.dp).clearAndSetSemantics {})
                    }
                }
            }
        }
    }
}

/** A [PillNavBar] floating at the bottom centre of a phone screen, clear of the system's gesture area. */
@Composable
fun BoxScope.FloatingPillNav(items: List<BottomItem>, selected: Int, onSelect: (Int) -> Unit) {
    BoxWithConstraints(Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 12.dp)) {
        PillNavBar(items, selected, onSelect, Modifier.align(Alignment.BottomCenter).width(minOf(maxWidth * 0.88f, 440.dp)))
    }
}

/** Small drawn icons for the editor's sections, which Material's core icon set lacks. */
object GameIcons {
    private fun icon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(
            androidx.compose.ui.graphics.vector.PathData(block), fill = SolidColor(Color.Black),
        ).build()

    /** Two stacked cards: the deck. */
    val Deck: ImageVector = icon("Deck") {
        moveTo(8f, 3f); lineTo(19f, 3f); quadTo(21f, 3f, 21f, 5f); lineTo(21f, 17f); quadTo(21f, 19f, 19f, 19f); lineTo(18f, 19f)
        lineTo(18f, 7f); quadTo(18f, 5f, 16f, 5f); lineTo(6f, 5f); quadTo(6f, 3f, 8f, 3f); close()
        moveTo(5f, 7f); lineTo(14f, 7f); quadTo(16f, 7f, 16f, 9f); lineTo(16f, 19f); quadTo(16f, 21f, 14f, 21f); lineTo(5f, 21f)
        quadTo(3f, 21f, 3f, 19f); lineTo(3f, 9f); quadTo(3f, 7f, 5f, 7f); close()
    }

    /** Three rising bars: the analysis. */
    val Curve: ImageVector = icon("Curve") {
        moveTo(3f, 13f); lineTo(7f, 13f); lineTo(7f, 21f); lineTo(3f, 21f); close()
        moveTo(10f, 8f); lineTo(14f, 8f); lineTo(14f, 21f); lineTo(10f, 21f); close()
        moveTo(17f, 3f); lineTo(21f, 3f); lineTo(21f, 21f); lineTo(17f, 21f); close()
    }

    /** A fanned hand of cards: the test hand. */
    val Hand: ImageVector = icon("Hand") {
        moveTo(2.2f, 8.6f); lineTo(8.4f, 6.4f); lineTo(12.6f, 18.2f); lineTo(6.4f, 20.4f); close()
        moveTo(9.5f, 4f); lineTo(14.5f, 4f); lineTo(14.5f, 17f); lineTo(9.5f, 17f); close()
        moveTo(15.6f, 6.4f); lineTo(21.8f, 8.6f); lineTo(17.6f, 20.4f); lineTo(11.4f, 18.2f); close()
    }
}

/**
 * Draws [text] optically centred in its box: by the letters' cap height, not the line box, so a
 * capital or a digit sits in the middle of a circle instead of riding high.
 */
@Composable
fun CenteredGlyph(text: String, color: Color, fontSize: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge.merge(TabularNumbers).copy(fontSize = fontSize, color = color, lineHeight = fontSize)
    Canvas(modifier.fillMaxSize()) {
        val layout = measurer.measure(text, style)
        // Quicksand's capitals and figures stand 0.70 em tall.
        val cap = fontSize.toPx() * 0.70f
        drawText(layout, topLeft = Offset((size.width - layout.size.width) / 2f, size.height / 2f + cap / 2f - layout.firstBaseline))
    }
}

/**
 * A card colour as a small flat dot with its initial centred in it (UX rule I4): no gloss, rim or
 * shadow, so several sit quietly beside a deck name. White gets a hairline so it shows on white glass.
 */
@Composable
fun ColorDot(color: Color, onColor: Color, letter: String, modifier: Modifier = Modifier, size: Dp = 18.dp, outlined: Boolean = false) {
    val line = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    Box(modifier.size(size).clip(CircleShape).background(color).then(if (outlined) Modifier.border(1.dp, line, CircleShape) else Modifier)) {
        CenteredGlyph(letter, onColor, (size.value * 0.56f).sp)
    }
}
