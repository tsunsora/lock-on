package app.lockon.ui.components
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.material3.LocalRippleConfiguration
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.unit.DpOffset
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
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
import app.lockon.ui.theme.ChartSlate
import app.lockon.ui.theme.ChartSlateTwilight
import androidx.compose.ui.graphics.drawscope.translate
import app.lockon.platform.imageBitmapOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
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
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.requireDensity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlin.concurrent.Volatile
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.onLongClick
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.floor
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
    GlassColors(Color(0xFF232A58).copy(alpha = 0.92f), Color(0xFF343C72), Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.06f), Color(0xFF060B1E))
} else {
    GlassColors(Color.White.copy(alpha = 0.96f), Color.White, Color.White, Color(0xFFD5E2F0), Color(0xFF6A88B0))
}

/** A painted sky and the theme it was painted for. */
private class SkyPaint(val width: Int, val height: Int, val image: ImageBitmap, val twilight: Boolean)

/**
 * The sky painted last, or being painted. Skies the same size share it: the boot screen, the app
 * behind it and a boot cover's picture of it would each otherwise paint their own, side by side,
 * which on a phone just starting up takes long enough to notice. Racing it only paints one twice.
 */
@Volatile private var lastSky: Deferred<SkyPaint>? = null
@Volatile private var lastSkyKey: Triple<Int, Int, Boolean>? = null

@OptIn(DelicateCoroutinesApi::class)
private suspend fun paintedSky(width: Int, height: Int, twilight: Boolean): SkyPaint {
    val key = Triple(width, height, twilight)
    val shared = lastSky?.takeIf { lastSkyKey == key }
    if (shared != null) return shared.await()
    // Not tied to whichever screen asked first, since others may be waiting for it when that one goes.
    val painting = GlobalScope.async(Dispatchers.Default) { SkyPaint(width, height, imageBitmapOf(paintSky(width, height, twilight), width, height), twilight) }
    lastSkyKey = key
    lastSky = painting
    return painting.await()
}

/**
 * The sky behind every screen: a clear gradient, soft coloured glows, a diagonal beam of light and
 * floating bokeh. It is static: nothing loops.
 * The smooth parts are painted per pixel by [paintSky] so they never band; [onPainted] hears when
 * the painted sky first draws.
 */
@Composable
fun SkyBackground(modifier: Modifier = Modifier, onPainted: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
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
        value = paintedSky(pw, ph, twilight)
    }
    Box(
        modifier.background(MaterialTheme.colorScheme.background).onSizeChanged { area = it }.drawBehind {
            val w = size.width
            val h = size.height
            val m = size.maxDimension
            val painted = sky?.takeIf { it.twilight == twilight }
            if (painted != null) {
                drawImage(painted.image, dstSize = IntSize(ceil(w).toInt(), ceil(h).toInt()), filterQuality = FilterQuality.Low)
                onPainted?.invoke()
            } else {
                drawRect(Brush.verticalGradient(if (twilight) TwilightSky else DaySky))
            }
            // Bokeh at fixed places, so every frame and screenshot is the same. Twilight's bokeh are
            // glows, painted with the sky.
            if (false) SkyBokeh.forEachIndexed { i, b ->
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
    // Calm and solid, as in Nintendo's menus: the sky frames the panels instead of showing through them.
    val fill = if (strong) SolidColor(g.strong) else SolidColor(g.fill)
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
    val style = MaterialTheme.typography.labelLarge.merge(TabularNumbers)
    val modifier = Modifier.clip(LockOnShapes.button).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 3.dp)
    // A plain count rolls when it changes, like every other count (UX rule V5: numbers).
    val number = text.toIntOrNull()
    if (number != null) RollingNumber(number, style, color, modifier)
    else Text(text, style = style, color = color, modifier = modifier)
}

/** A pill shape, for tabs, stamps and gauges. */
val PillShape: Shape = LockOnShapes.button

enum class GameButtonStyle { PRIMARY, SECONDARY, GHOST }

/**
 * A game button: a pill. Primary is a glossy teal pill with a soft glow; secondary is a white glass
 * pill with a teal rim; ghost is teal text. Hover brightens a touch, press sinks a hair. [outline] draws
 * a rim that sinks with the face; a border put on [modifier] would stay behind and leave a gap.
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
    tone: Color? = null,
    onTone: Color? = null,
    trailingIcon: ImageVector? = null,
    outline: Color? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val g = glassColors()
    val accent = tone ?: Accent
    val onAccent = onTone ?: OnAccent
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedHeld()
    val lift by animateFloatAsState(if (hovered && enabled) 1f else 0f, motion(Motion.SHORT), label = "button-hover")
    val squish by animateFloatAsState(if (pressed) (if (desktopControls) PressScale else 0.96f) else 1f, motion(Motion.SHORT), label = "button-press")
    // A press dims the face a touch as it sinks, so a tap reads even on a wide button whose scale barely shows.
    val dim by animateFloatAsState(if (pressed && enabled) 1f else 0f, motion(Motion.SHORT), label = "button-dim")
    val shape = LockOnShapes.button
    val fill: Brush
    val content: Color
    val rim: Color
    when {
        !enabled -> { fill = SolidColor(scheme.onSurface.copy(alpha = 0.06f)); content = scheme.outline; rim = Color.Transparent }
        style == GameButtonStyle.PRIMARY -> {
            fill = SolidColor(lighten(accent, 0.1f * lift))
            content = onAccent; rim = Color.Transparent
        }
        style == GameButtonStyle.SECONDARY -> {
            // A soft fill in the ink colour; hover deepens it.
            fill = SolidColor(if (desktopControls) neutralFill(hovered, pressed) else scheme.onSurface.copy(alpha = 0.07f + 0.05f * lift))
            content = scheme.onSurface; rim = Color.Transparent
        }
        else -> { fill = SolidColor(accent.copy(alpha = 0.10f * lift)); content = accent; rim = Color.Transparent }
    }
    Box(
        modifier
            .heightIn(min = if (large) 54.dp else if (desktopControls) ControlSize else if (compact) 38.dp else 44.dp)
            .graphicsLayer {
                scaleX = squish; scaleY = squish
                if (style == GameButtonStyle.PRIMARY && enabled && !desktopControls) {
                    this.shape = shape
                    shadowElevation = (2f + 3f * lift) * density
                    ambientShadowColor = accent; spotShadowColor = accent
                }
            }
            .clip(shape)
            .background(fill)
            .drawWithContent { drawContent(); if (dim > 0f) drawRect(Color.Black.copy(alpha = 0.08f * dim)) }
            .border(BorderStroke(1.dp, outline ?: rim), shape)
            .hoverable(interaction, enabled)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (large) 26.dp else if (compact) 14.dp else 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        // A label that changes (Copy to Copied, Hide to Show) fades across, so the change is seen.
        androidx.compose.animation.Crossfade(text to icon, animationSpec = motion(Motion.MEDIUM), label = "button-label") { (label, glyph) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (glyph != null) Icon(glyph, contentDescription = null, tint = content, modifier = Modifier.size(if (large) 22.dp else 18.dp))
                // Two lines at most, centred, so a long label wraps instead of being cut off.
                Text(
                    label, color = content, maxLines = 2, textAlign = TextAlign.Center,
                    style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                )
                if (trailingIcon != null) Icon(trailingIcon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * An icon-only action as a round glass button, the same 44 dp as [GameButton] and the search field,
 * so a header row of mixed controls lines up (UX rule V1). [label] is read out and is the tooltip's
 * text on desktop (I5). Over art, [onArt] makes the disc a little more solid so the icon stays clear.
 * [compact] shrinks it to a chip's height, for a row of chips.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    compact: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val g = glassColors()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedHeld()
    val squish by animateFloatAsState(if (pressed) (if (desktopControls) PressScale else 0.94f) else 1f, motion(Motion.SHORT), label = "icon-press")
    // Hover eases in rather than switching, like the buttons beside it.
    val disc by animateColorAsState(
        if (desktopControls) neutralFill(hovered, pressed) else if (hovered) lighten(g.strong, 0.08f) else g.strong,
        motion(Motion.SHORT), label = "icon-disc",
    )
    val ring by animateColorAsState(if (desktopControls) Color.Transparent else if (hovered) Accent.copy(alpha = 0.6f) else g.rimLow, motion(Motion.SHORT), label = "icon-ring")
    val glyph by animateColorAsState(if (hovered && !desktopControls) scheme.primary else tint, motion(Motion.SHORT), label = "icon-tint")
    val small = if (desktopControls) SmallControlShape else RoundedCornerShape(11.dp)
    Box(
        modifier
            .size(if (compact) 32.dp else ControlSize)
            .graphicsLayer { scaleX = squish; scaleY = squish }
            .clip(if (compact) small else LockOnShapes.button)
            .background(disc)
            .border(BorderStroke(1.dp, ring), if (compact) small else LockOnShapes.button)
            .hoverable(interaction)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = glyph, modifier = Modifier.size(if (compact) 18.dp else if (desktopControls) 20.dp else 22.dp))
    }
}

/** The one height for buttons, fields and icon buttons that share a row. A pointer needs less than a thumb. */
val ControlSize = if (app.lockon.platform.isMobile) 44.dp else 40.dp

/** Desktop draws every control from one recipe (UX rule K3); phones keep the look they were reviewed in. */
internal val desktopControls = !app.lockon.platform.isMobile

/** How far a control sinks when pressed on desktop. */
internal const val PressScale = 0.97f

/** Desktop's second control height, for chips and small icon buttons. */
val SmallControlSize = 32.dp
internal val SmallControlShape = RoundedCornerShape(10.dp)

/** The corners of a thumb or button inset 4 dp inside a [LockOnShapes.button], so the two curves stay parallel. */
internal val InsetControlShape = RoundedCornerShape(8.dp)

/**
 * Desktop's one neutral look, for anything that isn't teal: a flat wash of ink, one step deeper on hover
 * and another when pressed, with no rim. See-through, so it can sit on another wash; on its own, use [neutralFill].
 */
@Composable
fun neutralWash(hovered: Boolean = false, pressed: Boolean = false): Color {
    val dark = isTwilight()
    val alpha = when {
        pressed -> if (dark) 0.18f else 0.14f
        hovered -> if (dark) 0.14f else 0.10f
        else -> if (dark) 0.08f else 0.06f
    }
    return MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
}

/** [neutralWash] on the panel colour, so a control looks the same on a panel and stays solid directly on the sky. */
@Composable
fun neutralFill(hovered: Boolean = false, pressed: Boolean = false): Color = neutralWash(hovered, pressed).compositeOver(panelFill())

/**
 * Pill tabs on a glass track: the selected tab is a teal pill. They scroll when the track is
 * narrower than its tabs, or share the width equally with [fill].
 */
@Composable
fun PillTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, fill: Boolean = false, soft: Boolean = false, icons: List<ImageVector>? = null, flush: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    // Icons give way to the words when the row gets too narrow for both.
    val iconRoom = with(androidx.compose.ui.platform.LocalDensity.current) { 440.dp.toPx() }
    var showIcons by remember { mutableStateOf(true) }
    val accent = if (soft) softThumb() else Accent
    val onAccent = if (soft) softThumbInk() else OnAccent
    val thumbShape = if (desktopControls) InsetControlShape else LockOnShapes.button
    val thumbRadius = if (desktopControls) 8.dp else 11.dp
    val trackFill = if (desktopControls) neutralWash() else scheme.onSurface.copy(alpha = 0.09f)
    val track = if (fill) Modifier.fillMaxWidth() else Modifier.horizontalScroll(rememberScrollState())
    // Where each tab sits in the row, so one teal pill can slide between them (the tabs differ in width).
    // Only the draw reads them, so a resize moves the pill with its tab instead of recomposing the
    // tabs and springing after it.
    val spots = remember(labels.size) { Array(labels.size) { mutableStateOf<Pair<Float, Float>?>(null) } }
    // The pill glides along the tab indices; a screen opens with its tab already lit, since the slot
    // starts at the selection.
    val slot by animateFloatAsState(selected.toFloat(), glide(), label = "tab-slide")
    val placed by remember(spots, selected) { derivedStateOf { spots.getOrNull(selected)?.value != null } }
    Box(modifier.fillMaxWidth().then(if (icons != null && fill) Modifier.onSizeChanged { showIcons = it.width >= iconRoom } else Modifier).then(if (flush && desktopControls) Modifier else Modifier.padding(horizontal = 8.dp, vertical = 6.dp))) {
        Row(
            track.clip(LockOnShapes.button).background(trackFill).padding(horizontal = 4.dp).drawBehind {
                if (spots.isEmpty()) return@drawBehind
                val s = slot.coerceIn(0f, spots.lastIndex.toFloat())
                val lo = floor(s).toInt()
                val a = spots[lo].value ?: return@drawBehind
                val b = spots.getOrNull(ceil(s).toInt())?.value ?: a
                val f = s - lo
                val px = a.first + (b.first - a.first) * f
                val pw = a.second + (b.second - a.second) * f
                if (pw > 0f) drawRoundRect(accent, Offset(px, 4.dp.toPx()), Size(pw, size.height - 8.dp.toPx()), CornerRadius(thumbRadius.toPx()))
            },
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            labels.forEachIndexed { i, label ->
                val isSelected = i == selected
                val interaction = remember { MutableInteractionSource() }
                val hovered by interaction.collectIsHoveredAsState()
                val bg by animateColorAsState(
                    when {
                        // Until the sliding pill knows where the tabs are, the chosen tab lights itself.
                        isSelected && !placed -> accent
                        hovered && !isSelected -> scheme.onSurface.copy(alpha = 0.07f)
                        else -> Color.Transparent
                    },
                    motion(Motion.SHORT), label = "tab",
                )
                val text by animateColorAsState(if (isSelected) onAccent else scheme.onSurfaceVariant, motion(Motion.MEDIUM), label = "tab-text")
                Box(
                    // The whole height of the track takes the tap; the thumb inside it is 8 dp shorter.
                    (if (fill) Modifier.weight(1f) else Modifier).height(ControlSize)
                        .onPlaced { spots.getOrNull(i)?.value = it.positionInParent().x to it.size.width.toFloat() }
                        .hoverable(interaction)
                        .selectable(isSelected, interaction, indication = null, role = Role.Tab) { onSelect(i) }
                        .padding(vertical = 4.dp).clip(thumbShape).background(bg)
                        .padding(horizontal = if (icons != null && fill) 4.dp else 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        val glyph = icons?.getOrNull(i)
                        if (glyph != null && showIcons) Icon(glyph, contentDescription = null, Modifier.size(15.dp), tint = text)
                        Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, color = text)
                    }
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
fun ProgressGauge(label: String, count: Int, limit: Int, modifier: Modifier = Modifier, compact: Boolean = false, hold: Boolean = false, inline: Boolean = false, numberLast: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val fraction by animateFloatAsState((count.toFloat() / limit.coerceAtLeast(1)).coerceIn(0f, 1f), motion(Motion.LONG), label = "gauge")
    // One value for the gauge's state, 0 filling, 1 exactly full, 2 over, so the teal warms to gold
    // (and gold to red) instead of switching, with a single animation per gauge.
    val tone by animateFloatAsState(
        when { count > limit -> 2f; count == limit && !hold -> 1f; else -> 0f }, motion(Motion.MEDIUM), label = "gauge-tone",
    )
    val accent = Accent
    val gold = if (isTwilight()) Gold else scheme.tertiary
    val numberColor = if (tone <= 1f) androidx.compose.ui.graphics.lerp(scheme.onSurface, gold, tone)
        else androidx.compose.ui.graphics.lerp(gold, scheme.error, tone - 1f)
    val slate = if (isTwilight()) ChartSlateTwilight else ChartSlate
    val fillingBrush = SolidColor(if (hold) slate else accent)
    val fullBrush = SolidColor(Color(0xFFFFB84D))
    val error = scheme.error
    val track = scheme.onSurface.copy(alpha = if (isTwilight()) 0.12f else 0.1f)
    @Composable
    fun Number() {
        RollingNumber(count, (if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge).merge(TabularNumbers),
            numberColor, Modifier.bumpOnIncrease(count))
        Text("/$limit", style = MaterialTheme.typography.labelMedium.merge(TabularNumbers), color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 3.dp))
    }
    @Composable
    fun Bar(barModifier: Modifier) {
        Canvas(barModifier.height(if (compact && !inline) 6.dp else 8.dp)) {
            val r = CornerRadius(size.height / 2)
            drawRoundRect(track, cornerRadius = r)
            if (fraction > 0f) {
                val bar = Size(size.width * fraction, size.height)
                // The next state's fill is laid over the last one as the tone moves between them.
                when {
                    tone <= 0f -> drawRoundRect(fillingBrush, size = bar, cornerRadius = r)
                    tone < 1f -> {
                        drawRoundRect(fillingBrush, size = bar, cornerRadius = r)
                        drawRoundRect(fullBrush, size = bar, cornerRadius = r, alpha = tone)
                    }
                    tone == 1f -> drawRoundRect(fullBrush, size = bar, cornerRadius = r)
                    else -> {
                        drawRoundRect(fullBrush, size = bar, cornerRadius = r)
                        drawRoundRect(error, size = bar, cornerRadius = r, alpha = (tone - 1f).coerceAtMost(1f))
                    }
                }
            }
        }
    }
    if (inline) Row(
        modifier.widthIn(min = 110.dp).semantics(mergeDescendants = true) { contentDescription = "$label: $count of $limit" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!numberLast) { Row(verticalAlignment = Alignment.Bottom) { Number() }; Spacer(Modifier.width(10.dp)) }
        Bar(Modifier.weight(1f))
        if (numberLast) { Spacer(Modifier.width(10.dp)); Row(verticalAlignment = Alignment.Bottom) { Number() } }
    } else Column(modifier.widthIn(min = 110.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(bottom = 2.dp))
            Number()
        }
        Bar(Modifier.fillMaxWidth())
    }
}

/** A round cost mark, flat in the card's colour like [ColorDot]; [outlined] keeps a white card's mark visible on a light row. */
@Composable
fun CostOrb(value: Int?, color: Color, onColor: Color, modifier: Modifier = Modifier, size: Dp = 30.dp, outlined: Boolean = false, text: String? = null) {
    val line = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    Box(modifier.size(size).clip(CircleShape).background(color).then(if (outlined) Modifier.border(1.dp, line, CircleShape) else Modifier)) {
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
    val lit = Accent
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val on = i < count
            val color = if (i >= limit) scheme.error else if (on) lit else scheme.onSurface.copy(alpha = if (desktopControls) 0.38f else 0.22f)
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
    edge: Color,
    lift: Dp = 3.dp,
    resting: Float = 0f,
): Modifier = hoverable(interaction) then HoverLiftElement(interaction, glow, shape, edge, lift, resting)

private data class HoverLiftElement(
    val interaction: MutableInteractionSource,
    val glow: Color,
    val shape: Shape,
    val edge: Color,
    val lift: Dp,
    val resting: Float,
) : ModifierNodeElement<HoverLiftNode>() {
    override fun create() = HoverLiftNode(interaction, glow, shape, edge, lift, resting)
    override fun update(node: HoverLiftNode) = node.update(interaction, glow, shape, edge, lift, resting)
}

internal class HoverLiftNode(
    private var interaction: MutableInteractionSource,
    private var glow: Color,
    private var shape: Shape,
    private var edge: Color,
    private var lift: Dp,
    private var resting: Float,
) : Modifier.Node(), LayoutModifierNode, PointerInputModifierNode, CompositionLocalConsumerModifierNode, DrawModifierNode {
    private val hover = Animatable(0f)
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
    }

    override fun onCancelPointerInput() = setHovered(false)

    override fun onDetach() { watching = null; hovered = false }

    // A lazy grid reuses this node for another card, which starts at rest.
    override fun onReset() {
        hovered = false
        // Most reused tiles are already at rest, and starting a coroutine for each costs frames.
        if (isAttached && (hover.value != 0f || press.value != 1f)) coroutineScope.launch { hover.snapTo(0f); press.snapTo(1f) }
    }

    fun update(interaction: MutableInteractionSource, glow: Color, shape: Shape, edge: Color, lift: Dp, resting: Float) {
        if (interaction != this.interaction) { watching?.cancel(); watching = null }
        this.interaction = interaction; this.glow = glow; this.shape = shape; this.edge = edge; this.lift = lift; this.resting = resting
        invalidatePlacement()
        invalidateDraw()
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

    override fun ContentDrawScope.draw() {
        drawContent()
        val t = hover.value
        if (t <= 0f) return
        drawSelectionEdge(shape, edge, t)
    }
}

/** How thick a selected or hovered thing's outline is on desktop. */
val SelectionEdgeWidth = 2.dp

/**
 * The selection outline: one solid line on the inside of [shape]'s own edge, with no gap, so it follows
 * the corners exactly and never needs room around the tile.
 */
fun DrawScope.drawSelectionEdge(shape: Shape, color: Color, alpha: Float = 1f) {
    val width = SelectionEdgeWidth.toPx()
    val outline = shape.createOutline(size, layoutDirection, this)
    if (outline !is Outline.Rounded) {
        drawOutline(outline, color, alpha, Stroke(width))
        return
    }
    val outer = outline.roundRect
    fun CornerRadius.inner() = CornerRadius((x - width).coerceAtLeast(0f), (y - width).coerceAtLeast(0f))
    val inner = RoundRect(
        outer.left + width, outer.top + width, outer.right - width, outer.bottom - width,
        outer.topLeftCornerRadius.inner(), outer.topRightCornerRadius.inner(), outer.bottomRightCornerRadius.inner(), outer.bottomLeftCornerRadius.inner(),
    )
    drawPath(Path().apply { fillType = PathFillType.EvenOdd; addRoundRect(outer); addRoundRect(inner) }, color, alpha)
}

/** The selection outline on a tile that is selected, not just hovered. */
fun Modifier.selectionEdge(shape: Shape, color: Color, on: Boolean = true): Modifier = if (!on) this else drawWithContent { drawContent(); drawSelectionEdge(shape, color) }

/** Animates to [target] the way [motion] would: an emphasized tween, or at once when motion is reduced. */
private suspend fun Animatable<Float, *>.settle(target: Float, durationMillis: Int, reduce: Boolean) {
    if (reduce) snapTo(target) else animateTo(target, tween(durationMillis, easing = Motion.emphasized))
}

/**
 * A small nudge when [value] grows, so an added copy registers (UX rule V5: what changed). A modifier
 * node, like [flashOnChange]: every tile, strip and gauge carries one, and the old composed version
 * started a coroutine for each just to find its value hadn't grown.
 */
fun Modifier.bumpOnIncrease(value: Int): Modifier = this then BumpElement(value)

private data class BumpElement(val value: Int) : ModifierNodeElement<BumpNode>() {
    override fun create() = BumpNode(value)
    override fun update(node: BumpNode) = node.update(value)
}

private class BumpNode(private var value: Int) : Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
    private val scale = Animatable(1f)
    private var reused = false

    // A lazy grid reuses this node for another card: its count is new, not a change.
    override fun onReset() {
        reused = true
        if (isAttached && scale.value != 1f) coroutineScope.launch { scale.snapTo(1f) }
    }

    fun update(value: Int) {
        if (isAttached && !reused && value > this.value && !currentValueOf(LocalReduceMotion)) {
            coroutineScope.launch {
                scale.snapTo(1.08f)
                scale.animateTo(1f, tween(Motion.MEDIUM, easing = Motion.emphasizedDecelerate))
            }
        }
        reused = false
        this.value = value
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            // Read in the layer, so a bump redraws without recomposing or relaying out.
            placeable.placeWithLayer(0, 0) { scaleX = scale.value; scaleY = scale.value }
        }
    }
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
                    listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.22f), Gold.copy(alpha = 0.1f), Gold.copy(alpha = 0f)),
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
        // In the wide top band the lit nav item already names the screen, so the title steps aside.
        if (wide) Spacer(Modifier.weight(1f))
        else Text(title, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
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
    shortcut: String? = null,
) = PillField(value, onValueChange, placeholder, modifier, LineIcons.Search, onClear, shortcut)

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
    shortcut: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val g = glassColors()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    // Focus draws the rim and icon in teal over a moment, so the field visibly wakes up.
    val rimColor by animateColorAsState(if (focused) scheme.primary else if (desktopControls) Color.Transparent else g.rimLow, motion(Motion.SHORT), label = "field-rim")
    val rimWidth by animateFloatAsState(if (!focused) 1f else if (app.lockon.platform.isMobile) 1.6f else SelectionEdgeWidth.value, motion(Motion.SHORT), label = "field-rim-width")
    val iconTint by animateColorAsState(if (focused) scheme.primary else scheme.onSurfaceVariant, motion(Motion.SHORT), label = "field-icon")
    val shape = LockOnShapes.button
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interaction,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
        cursorBrush = SolidColor(scheme.primary),
        modifier = modifier.height(ControlSize),
        decorationBox = { field ->
            Row(
                Modifier.fillMaxSize().clip(shape).background(if (desktopControls) neutralFill() else g.strong)
                    .border(BorderStroke(rimWidth.dp, rimColor), shape)
                    .padding(start = if (icon != null) 14.dp else 18.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, maxLines = 1)
                    field()
                }
                if (onClear != null) {
                    IconButton(onClick = onClear, modifier = Modifier.size(40.dp)) {
                        Icon(LineIcons.X, contentDescription = "Clear search and filters", tint = scheme.onSurfaceVariant)
                    }
                } else if (shortcut != null && !app.lockon.platform.isMobile && !focused && LocalTopBand.current.height > 0.dp) {
                    Text(
                        shortcut, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 1,
                        modifier = Modifier.padding(end = 8.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, scheme.outlineVariant, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
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
fun ColorOrbToggle(label: String, color: Color, onColor: Color, selected: Boolean, target: Dp = 38.dp, outlined: Boolean = false, dimmed: Boolean = true, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val ring = Accent
    // The dot brightens and the ring draws in from a little wider, so turning a colour on reads as a
    // choice being made, not a swap.
    val lit by animateFloatAsState(if (selected || hovered || !dimmed) 1f else 0.5f, motion(Motion.SHORT), label = "orb-lit")
    val on by animateFloatAsState(if (selected) 1f else 0f, motion(Motion.MEDIUM), label = "orb-ring")
    Box(
        Modifier.size(target).hoverable(interaction)
            .toggleable(selected, interaction, indication = null, role = Role.Checkbox) { onToggle() }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.graphicsLayer { alpha = lit }) {
            ColorDot(color, onColor, label.take(1), size = 28.dp, outlined = outlined)
        }
        if (on > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                // On desktop the ring sits right on the dot's edge; phones keep it a little clear.
                val mobile = app.lockon.platform.isMobile
                val stroke = if (mobile) 2.5.dp.toPx() else SelectionEdgeWidth.toPx()
                val rest = if (mobile) 18.dp.toPx() else 14.dp.toPx() + stroke / 2
                drawCircle(ring.copy(alpha = ring.alpha * on), radius = rest + 4.dp.toPx() * (1f - on), style = Stroke(stroke))
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
    // The card fades in and grows from just under its size, so a dialog arrives rather than appears.
    val shown = remember { MutableTransitionState(false) }.apply { targetState = true }
    val appear by rememberTransition(shown, label = "dialog").animateFloat(
        { if (LocalReduceMotion.current) snap() else tween(Motion.MEDIUM, easing = Motion.emphasizedDecelerate) }, label = "dialog-in",
    ) { if (it) 1f else 0f }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        modifier = modifier
            .graphicsLayer { alpha = appear; val s = 0.94f + 0.06f * appear; scaleX = s; scaleY = s }
            .glassFrame(MaterialTheme.shapes.extraLarge),
        containerColor = if (isTwilight()) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest,
        title = title?.let {
            {
                Text(it, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        },
        text = text,
    )
}

/**
 * A drop-down menu on a glass card: Material's placement and open animation, with the rounded
 * panel shape, the glass rim and a soft shadow. Fill it with [GameMenuItem]s.
 */
@Composable
fun GameMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 6.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val g = glassColors()
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = 180.dp).padding(horizontal = 6.dp),
        offset = offset,
        shape = LockOnShapes.panel,
        // Opaque, or the shadow beneath shows through the glass as a grey box.
        containerColor = g.strong.compositeOver(MaterialTheme.colorScheme.background),
        tonalElevation = 0.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.2.dp, Brush.verticalGradient(listOf(g.rim, g.rimLow))),
        content = content,
    )
}

/**
 * One row of a [GameMenu]: a pill that lights teal on hover. [selected] marks the current choice
 * with a check; [danger] paints a destructive action in the error colour. [leading] stands in for
 * [icon] when the row needs more than a glyph, such as a colour dot.
 */
@Composable
fun GameMenuItem(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    selected: Boolean = false,
    danger: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val tone = when {
        danger -> scheme.error
        selected -> Accent
        else -> scheme.onSurface
    }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedHeld()
    val wash = when {
        pressed -> 0.20f
        hovered -> 0.12f
        selected -> 0.10f
        else -> 0f
    }
    val fill by animateColorAsState(tone.copy(alpha = wash), motion(Motion.SHORT), label = "menu-item")
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(PillShape)
            .background(fill)
            .hoverable(interaction)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (leading != null) leading()
        else if (icon != null) Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(18.dp))
        Text(
            text, color = tone, style = MaterialTheme.typography.labelLarge, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        if (selected) Icon(LineIcons.Check, contentDescription = "Selected", tint = Accent, modifier = Modifier.size(18.dp))
    }
}

/** A hairline between groups of [GameMenuItem]s, such as before a destructive action. */
@Composable
fun GameMenuDivider() {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp).height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    )
}

/**
 * Where [GameToast]s show: Material's queue and timing, drawn as the app's glass pills instead of
 * Material's dark bars.
 */
@Composable
fun GameToastHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { GameToast(it) }
}

/**
 * A short notice on an opaque glass pill: the message, and the action as a ghost
 * button. It wraps its text, so a short notice makes a short pill.
 */
@Composable
fun GameToast(data: SnackbarData) {
    val scheme = MaterialTheme.colorScheme
    val g = glassColors()
    // A pill on one line; a wrapped notice keeps the round ends without ballooning into an oval.
    val shape = RoundedCornerShape(24.dp)
    val action = data.visuals.actionLabel
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .widthIn(max = 560.dp)
            .heightIn(min = 48.dp)
            .graphicsLayer {
                this.shape = shape
                shadowElevation = 10.dp.toPx()
                ambientShadowColor = g.shade; spotShadowColor = g.shade
            }
            .clip(shape)
            // Opaque, so the screen under it doesn't muddy the text.
            .background(g.strong.compositeOver(scheme.background))
            .border(BorderStroke(1.2.dp, Brush.verticalGradient(listOf(g.rim, g.rimLow))), shape)
            .padding(start = 22.dp, end =if (action != null) 6.dp else 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            data.visuals.message, color = scheme.onSurface, style = MaterialTheme.typography.bodyMedium,
            maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).padding(vertical = 12.dp),
        )
        if (action != null) GameButton(action, onClick = { data.performAction() }, style = GameButtonStyle.GHOST, compact = true)
    }
}

/**
 * An on/off switch as a pill track with a round knob: teal when on. A 48 dp target.
 *
 * @param interactionSource when a whole row toggles the switch ([onCheckedChange] null), the row's
 * interactions, so the track lights while the pointer or keyboard focus is anywhere on the row.
 */
@Composable
fun PillSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = Accent
    // The knob is moved by the hand, so it settles with a touch of give.
    val position by animateFloatAsState(if (checked) 1f else 0f, glide(bounce = 0.25f), label = "switch")
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val responds = enabled && (onCheckedChange != null || interactionSource != null)
    // The track itself answers the pointer; a ripple over the whole 48 dp target or row reads as a stray square.
    val lit by animateFloatAsState(if ((hovered || focused) && responds) 1f else 0f, motion(Motion.SHORT), label = "switch-hover")
    Box(
        modifier.size(width = 44.dp, height = 48.dp)
            .then(
                if (onCheckedChange != null) {
                    Modifier.hoverable(interaction, enabled)
                        .toggleable(checked, interaction, indication = null, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
                } else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(width = 44.dp, height = 26.dp).clip(LockOnShapes.pill)
                .background(SolidColor(scheme.onSurface.copy(alpha = 0.12f + 0.08f * lit)))
                // The teal fills in as the knob travels, so the track and knob change together.
                .drawBehind {
                    val on = position.coerceIn(0f, 1f)
                    if (on > 0f) drawRect(lighten(accent, 0.12f * lit), alpha = on)
                },
        ) {
            Box(
                Modifier.padding(3.dp).size(20.dp)
                    .graphicsLayer { translationX = position * 18.dp.toPx(); shape = CircleShape; shadowElevation = 1.dp.toPx() }
                    .clip(CircleShape)
                    .background(if (enabled) Color.White else scheme.outline),
            )
        }
    }
}

/** The thumb of a single-choice control: white by day and a raised indigo at twilight, so solid teal is left for navigation and main actions. */
@Composable
@ReadOnlyComposable
fun softThumb(): Color = if (isTwilight()) Color(0xFF5560B0) else Color.White

/** The chosen option's text on a [softThumb]. */
@Composable
@ReadOnlyComposable
fun softThumbInk(): Color = MaterialTheme.colorScheme.primary

/** One choice among a few on a soft track; the chosen one is a white thumb with teal text. */
@Composable
fun PillSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, icons: List<ImageVector>? = null) {
    val scheme = MaterialTheme.colorScheme
    val accent = softThumb()
    val onAccent = softThumbInk()
    // One teal pill slides to the chosen option, so the eye follows the change instead of seeing one
    // pill vanish and another appear.
    val slot by animateFloatAsState(selected.toFloat(), glide(), label = "segment-slide")
    Box(modifier.height(44.dp).clip(LockOnShapes.button).background(scheme.onSurface.copy(alpha = 0.09f)).padding(4.dp)) {
        if (selected in options.indices) {
            Canvas(Modifier.matchParentSize()) {
                val gap = 3.dp.toPx()
                val w = (size.width - gap * (options.size - 1)) / options.size
                drawRoundRect(accent, Offset(slot * (w + gap), 0f), Size(w, size.height), CornerRadius(11.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            options.forEachIndexed { i, label ->
                val isSelected = i == selected
                val text by animateColorAsState(if (isSelected) onAccent else scheme.onSurfaceVariant, motion(Motion.MEDIUM), label = "segment-text")
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(LockOnShapes.button)
                        .selectable(isSelected, interactionSource = null, indication = null, role = Role.RadioButton) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        icons?.getOrNull(i)?.let { Icon(it, contentDescription = null, Modifier.size(16.dp), tint = text) }
                        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, color = text)
                    }
                }
            }
        }
    }
}

/**
 * A filter or choice chip as a pill: white glass, teal when selected, like every other selected pill (UX rule V3).
 *
 * @param onLongClick runs when the chip is held, in place of [onClick]; [onLongClickLabel] names it for screen readers.
 */
@Composable
fun PillChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    tall: Boolean = false,
) {
    // FilterChip takes no long click, so a press held past the timeout runs it and swallows the click on release.
    val interactions = remember { MutableInteractionSource() }
    val held = remember { booleanArrayOf(false) }
    if (onLongClick != null) {
        val timeout = LocalViewConfiguration.current.longPressTimeoutMillis
        val haptics = LocalHapticFeedback.current
        val longClick by rememberUpdatedState(onLongClick)
        LaunchedEffect(interactions, timeout) {
            interactions.interactions.collectLatest { interaction ->
                if (interaction is PressInteraction.Press) {
                    held[0] = false
                    delay(timeout)
                    held[0] = true
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    longClick()
                }
            }
        }
    }
    val longClickSemantics = if (onLongClick == null) Modifier
    else Modifier.semantics { onLongClick(onLongClickLabel) { onLongClick(); true } }
    val scheme = MaterialTheme.colorScheme
    val g = glassColors()
    val hovered by interactions.collectIsHoveredAsState()
    val pressed by interactions.collectIsPressedHeld()
    // Material's chip switches colour at once; these fade, and stand in for both its states. Hover and
    // press tint the chip itself: Material's grey state layer and ripple turn the teal muddy as it fades in.
    val base = if (desktopControls) panelFill() else g.strong
    val fill by animateColorAsState(
        when {
            selected -> Accent.copy(alpha = if (pressed) 0.24f else if (hovered) 0.18f else 0.13f).compositeOver(base)
            desktopControls -> neutralFill(hovered, pressed)
            else -> scheme.onSurface.copy(alpha = if (pressed) 0.14f else if (hovered) 0.11f else 0.07f).compositeOver(base)
        },
        motion(Motion.SHORT), label = "chip-fill",
    )
    val text by animateColorAsState(if (selected) Accent else scheme.onSurface, motion(Motion.SHORT), label = "chip-text")
    val rim by animateColorAsState(
        if (selected) Accent else if (desktopControls) Color.Transparent else if (hovered) Accent.copy(alpha = 0.5f) else g.rimLow,
        motion(Motion.SHORT), label = "chip-rim",
    )
    CompositionLocalProvider(
        LocalRippleConfiguration provides null,
        // A pointer needs no 48 dp target, so rows of chips sit close and line up with buttons beside them.
        androidx.compose.material3.LocalMinimumInteractiveComponentSize provides if (app.lockon.platform.isMobile) 48.dp else 0.dp,
    ) { FilterChip(
        selected = selected, onClick = { if (held[0]) held[0] = false else onClick() }, label = label,
        modifier = modifier.then(if (tall && desktopControls) Modifier.height(ControlSize) else Modifier).then(longClickSemantics).pressScale(interactions), leadingIcon = leadingIcon,
        shape = if (desktopControls && !tall) SmallControlShape else LockOnShapes.button, interactionSource = interactions,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = fill,
            labelColor = text,
            iconColor = text,
            selectedContainerColor = fill,
            selectedLabelColor = text,
            selectedLeadingIconColor = text,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = selected,
            borderColor = rim, selectedBorderColor = rim,
            selectedBorderWidth = if (app.lockon.platform.isMobile) 1.dp else SelectionEdgeWidth,
        ),
    ) }
}

/** A [PillChip] that opens a menu under it. Toggles leave it open so several can be picked; a single choice calls close. */
@Composable
fun MenuChip(
    selected: Boolean,
    label: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    chipModifier: Modifier = Modifier,
    tall: Boolean = false,
    items: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        PillChip(
            selected = selected, onClick = { open = true }, modifier = chipModifier, tall = tall,
            label = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    label()
                    Icon(LineIcons.Chevd, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            },
        )
        GameMenu(expanded = open, onDismissRequest = { open = false }) { items { open = false } }
    }
}

/**
 * One destination in a [PillNavBar]: its label and icon. [onLongClick] runs when it is held, and
 * [onLongClickLabel] names that for screen readers.
 */
class BottomItem(
    val label: String,
    val onLongClick: (() -> Unit)? = null,
    val onLongClickLabel: String? = null,
    val icon: @Composable () -> Unit,
)

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
    spread: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = Accent
    val onAccent = OnAccent
    val twilight = isTwilight()
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
    val reduce = LocalReduceMotion.current
    // With every label showing (wide windows), one teal pill slides between the destinations, as the
    // tabs do. On phones the chosen item grows into its pill instead, so each item keeps its own.
    val spots = remember(items.size) { Array(items.size) { mutableStateOf<Pair<Float, Float>?>(null) } }
    val target = if (labels) spots.getOrNull(selected)?.value else null
    val x = remember { Animatable(0f) }
    val w = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        if (target == null) return@LaunchedEffect
        if (!placed || reduce) { x.snapTo(target.first); w.snapTo(target.second); placed = true; return@LaunchedEffect }
        launch { x.animateTo(target.first, glideSpec(false)) }
        w.animateTo(target.second, glideSpec(false))
    }
    val sliding = labels && placed
    Row(
        modifier
            .graphicsLayer { this.shape = shape; shadowElevation = 16.dp.toPx(); ambientShadowColor = Color.Black.copy(alpha = 0.15f); spotShadowColor = Color.Black.copy(alpha = 0.15f) }
            .clip(shape)
            // Up top on wide windows it sits over the sky alone, so it is barely there. Floating at the
            // bottom on phones, cards scroll under it with no backdrop blur, so it keeps more body.
            .background(if (twilight) Color(0xFF343C72) else Color.White)
            .border(BorderStroke(0.8.dp, scheme.onSurface.copy(alpha = 0.12f)), shape)
            .height(PillNavHeight)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .drawBehind {
                // As tall as an item's own pill: its icon and 10 dp above and below.
                val h = minOf(size.height, 42.dp.toPx())
                if (sliding) drawRoundRect(accent, Offset(x.value, (size.height - h) / 2), Size(w.value, h), CornerRadius(h / 2))
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, item ->
            val isSelected = i == selected
            // The selected item takes more of the bar, so its label has room (AnymeX's 2.4 : 1).
            val share by animateFloatAsState(if (labels || !isSelected) 1f else 2.4f, if (reduce) snap() else PillSpring, label = "pill-share")
            val itemFill by animateColorAsState(if (isSelected) accent else Color.Transparent, motion(Motion.SHORT), label = "pill-fill")
            // Once the shared pill slides, items draw no fill of their own; before then the chosen one lights itself.
            val fill = if (!labels) itemFill else if (isSelected && !placed) accent else Color.Transparent
            val content by animateColorAsState(if (isSelected) onAccent else scheme.onSurface.copy(alpha = 0.85f), motion(Motion.LONG), label = "pill-content")
            Box(
                // With every label showing, items take their own width; otherwise they share the bar.
                (if (labels && !spread) Modifier else Modifier.weight(share)).fillMaxHeight()
                    .onPlaced { spots.getOrNull(i)?.value = it.positionInParent().x to it.size.width.toFloat() }
                    .then(
                        if (item.onLongClick == null) Modifier.selectable(isSelected, role = Role.Tab, interactionSource = null, indication = null) { onSelect(i) }
                        else Modifier.combinedClickable(
                            interactionSource = null, indication = null, role = Role.Tab,
                            onLongClickLabel = item.onLongClickLabel, onLongClick = item.onLongClick,
                        ) { onSelect(i) }.semantics { this.selected = isSelected },
                    )
                    .semantics { contentDescription = item.label },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier
                        .clip(LockOnShapes.pill).background(fill)
                        .padding(horizontal = if (labels) 16.dp else if (isSelected) 18.dp else 12.dp, vertical = 10.dp),
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

/**
 * A [PillNavBar] floating at the bottom centre of a phone screen, clear of the system's gesture area,
 * at most [widest] across (see [floatingPillWidth]).
 */
@Composable
fun BoxScope.FloatingPillNav(items: List<BottomItem>, selected: Int, widest: Dp = 440.dp, labels: Boolean = false, onSelect: (Int) -> Unit) {
    BoxWithConstraints(Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 12.dp)) {
        PillNavBar(items, selected, onSelect, Modifier.align(Alignment.BottomCenter).width(floatingPillWidth(maxWidth, widest)), labels = labels, spread = labels)
    }
}

/** How wide a [FloatingPillNav] is on a screen [screen] wide: most of it, up to [widest]. */
fun floatingPillWidth(screen: Dp, widest: Dp = 440.dp): Dp = minOf(screen - 24.dp, widest)

/** Small drawn icons for the editor's sections and the export, which Material's core icon set lacks. */
object GameIcons {
    private fun icon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(
            androidx.compose.ui.graphics.vector.PathData(block), fill = SolidColor(Color.Black),
        ).build()

    /** Two stacked cards: the deck. */
    val Deck: ImageVector get() = LineIcons.Decks

    /** One card with a diamond in it: the card list. */
    val Card: ImageVector get() = LineIcons.Cards

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

    /** A trophy: the match log. */
    val Trophy: ImageVector = icon("Trophy") {
        moveTo(6f, 3f); lineTo(18f, 3f); lineTo(18f, 9f); quadTo(18f, 14f, 12f, 14f); quadTo(6f, 14f, 6f, 9f); close()
        moveTo(6f, 4.5f); lineTo(2.5f, 4.5f); lineTo(2.5f, 7f); quadTo(2.5f, 10.5f, 6.5f, 11.2f); lineTo(6.9f, 9.6f)
        quadTo(4.2f, 9f, 4.2f, 7f); lineTo(4.2f, 6.2f); lineTo(6f, 6.2f); close()
        moveTo(18f, 4.5f); lineTo(21.5f, 4.5f); lineTo(21.5f, 7f); quadTo(21.5f, 10.5f, 17.5f, 11.2f); lineTo(17.1f, 9.6f)
        quadTo(19.8f, 9f, 19.8f, 7f); lineTo(19.8f, 6.2f); lineTo(18f, 6.2f); close()
        moveTo(10.8f, 13.5f); lineTo(13.2f, 13.5f); lineTo(13.2f, 17.5f); lineTo(10.8f, 17.5f); close()
        moveTo(7.5f, 18f); lineTo(16.5f, 18f); lineTo(16.5f, 21f); lineTo(7.5f, 21f); close()
    }

    /** Four corner brackets, like the reticle's: show something at full size. */
    val Expand: ImageVector = icon("Expand") {
        moveTo(3f, 3f); lineTo(10f, 3f); lineTo(10f, 5.2f); lineTo(5.2f, 5.2f); lineTo(5.2f, 10f); lineTo(3f, 10f); close()
        moveTo(14f, 3f); lineTo(21f, 3f); lineTo(21f, 10f); lineTo(18.8f, 10f); lineTo(18.8f, 5.2f); lineTo(14f, 5.2f); close()
        moveTo(21f, 14f); lineTo(21f, 21f); lineTo(14f, 21f); lineTo(14f, 18.8f); lineTo(18.8f, 18.8f); lineTo(18.8f, 14f); close()
        moveTo(3f, 14f); lineTo(5.2f, 14f); lineTo(5.2f, 18.8f); lineTo(10f, 18.8f); lineTo(10f, 21f); lineTo(3f, 21f); close()
    }

    /** A clock face: the saved versions. */
    val Clock: ImageVector = icon("Clock") {
        // The ring winds against the face inside it, so the face stays hollow under the hands.
        moveTo(21f, 12f); arcTo(9f, 9f, 0f, false, true, 3f, 12f); arcTo(9f, 9f, 0f, false, true, 21f, 12f); close()
        moveTo(19f, 12f); arcTo(7f, 7f, 0f, false, false, 5f, 12f); arcTo(7f, 7f, 0f, false, false, 19f, 12f); close()
        moveTo(11f, 7f); lineTo(13f, 7f); lineTo(13f, 11.4f); lineTo(16f, 13.2f); lineTo(15f, 14.9f); lineTo(11f, 12.6f); close()
    }
}

/**
 * Draws [text] centred in its box by the letters' ink, not the line box or the advance width, so a
 * capital or a digit sits in the middle of a circle. It is laid out large and scaled down because
 * a small layout rounds its baseline and width to whole pixels, which moved the letter by up to a
 * pixel depending on the font size.
 */
@Composable
fun CenteredGlyph(text: String, color: Color, fontSize: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge.merge(TabularNumbers)
        .copy(fontSize = fontSize * GlyphLayoutScale, color = color, lineHeight = fontSize * GlyphLayoutScale, letterSpacing = 0.sp, baselineShift = androidx.compose.ui.text.style.BaselineShift.None)
    Canvas(modifier.fillMaxSize()) {
        val layout = measurer.measure(text, style)
        val em = fontSize.toPx()
        val down = 1f / GlyphLayoutScale
        // Quicksand's capitals and figures stand 0.70 em tall.
        val cap = em * 0.70f
        val bias = text.singleOrNull()?.let { GlyphInkBias[it] } ?: Offset.Zero
        withTransform({
            translate((size.width - layout.getLineRight(0) * down) / 2f - bias.x * em, size.height / 2f + cap / 2f - layout.firstBaseline * down - bias.y * em)
            scale(down, down, pivot = Offset.Zero)
        }) { drawText(layout) }
    }
}

private const val GlyphLayoutScale = 8

/**
 * How far Quicksand's bold glyphs sit from the middle of their advance and cap height, in em, for
 * those far enough off to show. Measured from renders: a stem carries a wider margin than an open side.
 */
private val GlyphInkBias = mapOf(
    'B' to Offset(0.018f, 0f), 'P' to Offset(0.018f, 0f), 'R' to Offset(0.019f, 0f),
    '1' to Offset(-0.033f, 0f), '3' to Offset(-0.004f, 0.010f), '4' to Offset(-0.008f, 0f),
    '5' to Offset(0f, 0.010f), '6' to Offset(0.008f, 0.006f), '7' to Offset(0.016f, 0.006f),
)

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
