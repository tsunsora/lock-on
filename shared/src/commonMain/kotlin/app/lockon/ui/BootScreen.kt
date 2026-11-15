package app.lockon.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import app.lockon.ui.components.Gold
import app.lockon.ui.components.LocalReduceMotion
import app.lockon.ui.components.LockOnMark
import app.lockon.ui.components.SkyBackground
import app.lockon.ui.components.isTwilight
import kotlinx.coroutines.flow.StateFlow
import kotlin.concurrent.Volatile

/** What the boot screen is waiting on, in order; each finished step fills one pip. */
enum class BootStep(val label: String) {
    DECKS("Loading decks"),
    CARDS("Laying out cards"),
    EDITOR("Warming up the deck editor"),
}

/**
 * How far the boot animation has played, in ms of its loop. Whoever plays it moves it on: the boot
 * screen itself, or on desktop a [BootCover], so handing over between them carries on where it was.
 */
class BootClock {
    @Volatile var played: Float = 0f
    /**
     * Set by a [BootCover] as it starts drawing. From then on only the cover moves the clock: the boot
     * screen beneath stops at once, rather than in its next frame, which could come late and make up
     * the time it lost on top of the cover's.
     */
    @Volatile var handedOver: Boolean = false
    /** How far the hunt has got, from 0 to [HOLD_AT]: it plays once, then holds the lock until the boot screen goes. */
    val stage: Float get() = minOf(played / HUNT_MS, HOLD_AT)
    /** True once the first lock has landed, so the boot screen may go. */
    val locked: Boolean get() = played >= HUNT_MS * (LOCK_AT + 0.04f)

    /** Jumps to just past the first lock: when the app is ready before the ring has filled, and for tests whose frames come in steps too big for this clock to keep up with. */
    fun skipToLock() { played = HUNT_MS * (LOCK_AT + 0.04f) }
}

/**
 * Plays the boot screen somewhere the screens building underneath can't hold it up. Each screen
 * built under the boot screen holds the app's frames up for a few hundred ms, and a boot screen
 * drawn in those frames froze each time; on desktop this is a window of its own over the app's,
 * drawn on a thread of its own.
 */
interface BootCover {
    val clock: BootClock
    /** True while it is up over the window. Until then, or if it never comes up, the window's own boot screen plays. */
    val showing: StateFlow<Boolean>
    fun step(step: BootStep)
    /** Fades it out, showing the app beneath, once the app has booted. */
    fun lift()
}

/** The boot screen's still layers as pictures, all the size of the screen, and where the targeting goes between them. */
class BootPictures(val sky: ImageBitmap, val fronts: Map<BootStep, ImageBitmap>, val targeting: BootTargeting, val density: Density)

/**
 * Takes [BootPictures] of the boot screen as it would draw here, for a [BootCover] that draws them
 * itself, and hands each new set to [onPictures]. It draws nothing on screen.
 */
@Composable
fun BootPictureTaker(clock: BootClock, onPictures: (BootPictures) -> Unit) {
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val sky = rememberGraphicsLayer()
    val fronts = listOf(rememberGraphicsLayer(), rememberGraphicsLayer(), rememberGraphicsLayer())
    var skyPainted by remember { mutableStateOf(false) }
    var targeting by remember { mutableStateOf<BootTargeting?>(null) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val take by rememberUpdatedState(onPictures)
    // Each layer is recorded rather than drawn; its own graphics layer keeps the boot screen's frames
    // from recording them again.
    fun record(layer: GraphicsLayer) = Modifier.drawWithContent { layer.record { this@drawWithContent.drawContent() } }
    Box(Modifier.fillMaxSize().graphicsLayer().onSizeChanged { size = it }) {
        BootScreen(BootStep.DECKS, clock, record(sky), playing = false, layers = BootLayers.SKY, onSkyPainted = { skyPainted = true })
        BootStep.entries.forEachIndexed { i, s ->
            BootScreen(s, clock, record(fronts[i]), playing = false, layers = BootLayers.FRONT, onTargeting = { targeting = it })
        }
    }
    LaunchedEffect(skyPainted, targeting, size, density, colors) {
        val placed = targeting ?: return@LaunchedEffect
        if (!skyPainted || size == IntSize.Zero) return@LaunchedEffect
        // Two frames, so every layer has drawn at this size and look.
        repeat(2) { withFrameNanos { } }
        // Should a picture fail, no cover gets one and the boot screen here plays instead.
        runCatching {
            val pictures = BootStep.entries.mapIndexed { i, s -> s to fronts[i].toImageBitmap() }.toMap()
            BootPictures(sky.toImageBitmap(), pictures, placed, density)
        }.getOrNull()?.let(take)
    }
}

/** Which parts of the boot screen to draw: all of it, or one layer, for a [BootCover] to put back together around the targeting. */
enum class BootLayers { ALL, SKY, FRONT }

/** Where the targeting field sits on the boot screen and how it looks, for a [BootCover] that draws it itself. */
class BootTargeting(val center: Offset, val reach: Float, val target: Float, val scan: Color, val ink: Color, val casing: Color, val lockInk: Color, val labelStyle: TextStyle) {
    /** Draws the field at [clock]; with [rings] false, without the parts that never move ([drawRings]), for a cover that keeps those in a picture. */
    fun draw(scope: DrawScope, clock: BootClock, labels: TextMeasurer, rings: Boolean = true) = with(scope) {
        translate(this@BootTargeting.center.x - size.width / 2, this@BootTargeting.center.y - size.height / 2) {
            if (rings) drawRadarRings(reach, scan)
            drawTargeting(clock.stage, reach, target, scan, ink, casing)
        }
    }

    /** Draws the part of the field that never moves: its faint ring. */
    fun drawRings(scope: DrawScope) = with(scope) {
        translate(this@BootTargeting.center.x - size.width / 2, this@BootTargeting.center.y - size.height / 2) { drawRadarRings(reach, scan) }
    }

    /** Once laid out the seeker's words ahead of time; the simpler field has none, so there is nothing to do. */
    @Suppress("UNUSED_PARAMETER")
    fun warm(labels: TextMeasurer) {}

    /**
     * The field on a screen [width]×[height] pixels whose boot screen sits [by] from where it was: the
     * boot screen keeps its mark centred, so the field moves with it, and its reach follows the size.
     */
    fun moved(by: Offset, width: Float, height: Float) = BootTargeting(center + by, bootReach(width, height), target, scan, ink, casing, lockInk, labelStyle)
}

/**
 * The launch screen: the Lock On mark with a ring round it that fills, locks in gold with one pulse,
 * and holds the lock until it fades out. It covers the app while the screens underneath fill in,
 * so the first look at decks and cards doesn't stutter. With reduced motion it holds the lock.
 * While [playing] is false a [BootCover] plays it, and this one holds still underneath.
 */
@Composable
fun BootScreen(
    step: BootStep,
    clock: BootClock,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    layers: BootLayers = BootLayers.ALL,
    onTargeting: ((BootTargeting) -> Unit)? = null,
    /** Hears when the sky has its painted look, for a [BootCover] taking a picture of it. */
    onSkyPainted: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val reduce = LocalReduceMotion.current
    // The hunt, the lock and the letting go are all fractions of one loop. Here the screens building
    // underneath hold up some frames by a few hundred ms, and on the wall clock the ring jumped
    // across the field each time. So the clock moves at most [MAX_STEP_MS] a frame and makes up the
    // rest at up to [CATCH_UP] times speed once frames are quick again, and the ring only glides.
    val frames = remember { mutableIntStateOf(0) }
    if (playing && !reduce && layers == BootLayers.ALL) LaunchedEffect(clock) {
        var last = withFrameNanos { it }
        var owed = 0f
        while (true) withFrameNanos { now ->
            val frame = (now - last) / 1_000_000f
            last = now
            owed += frame
            val step = minOf(owed, MAX_STEP_MS, frame * CATCH_UP)
            owed -= step
            if (!clock.handedOver) clock.played += step
            frames.intValue++
        }
    }
    val time = { frames.intValue; if (reduce) HOLD_AT else clock.stage }
    val look = if (isTwilight()) BootLookTwilight else BootLookDay
    val scan = look.scan
    val ink = look.ink
    val labelStyle = MaterialTheme.typography.labelMedium
    val density = LocalDensity.current
    val screen = modifier.fillMaxSize()
        // Nothing underneath takes a press while it boots.
        .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() } }
    val content: @Composable BoxScope.() -> Unit = content@{
        if (layers == BootLayers.SKY) return@content
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val reach = bootReach(maxWidth.value, maxHeight.value).dp
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    // Sized to the mark but drawn well past its edges, so the field centres on the mark.
                    Canvas(
                        Modifier.size(176.dp).then(
                            if (onTargeting == null) Modifier else Modifier.onGloballyPositioned { c ->
                                val center = c.positionInRoot() + Offset(c.size.width / 2f, c.size.height / 2f)
                                with(density) { onTargeting(BootTargeting(center, reach.toPx(), 62.dp.toPx(), scan, ink, look.casing, look.lockInk, labelStyle)) }
                            },
                        ),
                    ) {
                        if (layers == BootLayers.ALL) {
                            drawRadarRings(reach.toPx(), scan)
                            drawTargeting(time(), reach.toPx(), 62.dp.toPx(), scan, ink, look.casing)
                        }
                    }
                    Box(
                        Modifier.size(124.dp).shadow(8.dp, CircleShape, ambientColor = look.shade, spotColor = look.shade)
                            .clip(CircleShape).background(look.tile),
                        contentAlignment = Alignment.Center,
                    ) { LockOnMark(size = 84.dp, reticleColor = look.mark) }
                }
                Spacer(Modifier.height(20.dp))
                Text("Lock On", style = MaterialTheme.typography.headlineMedium, color = ink)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.clip(CircleShape).background(look.panel).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BootStep.entries.forEach { s ->
                        val done = s.ordinal < step.ordinal
                        val now = s == step
                        Box(
                            Modifier.size(width = if (now) 16.dp else 8.dp, height = 8.dp).clip(CircleShape)
                                .background(if (done || now) look.pip else look.panelText.copy(alpha = 0.5f)),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(step.label, style = MaterialTheme.typography.labelLarge, color = look.panelText)
                }
            }
        }
        // The window's controls sit on the violet here, so they take its ink.
        trailing?.let {
            MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(onSurface = ink, onSurfaceVariant = ink)) {
                Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) { it() }
            }
        }
    }
    // The front layer goes over a sky and targeting drawn elsewhere, so it has no sky of its own.
    if (layers == BootLayers.FRONT) Box(screen, content = content) else SkyBackground(screen, onSkyPainted, boot = true, content = content)
}

/**
 * The launch screen's colours: the logo as it looks in that theme's header, on a sky leaning violet.
 * Gold is kept for the lock; on the pale day sky it needs a dark [casing] to show at all.
 */
private class BootLook(
    val scan: Color,
    val ink: Color,
    val tile: Color,
    val mark: Color,
    val shade: Color,
    val panel: Color,
    val panelText: Color,
    val pip: Color,
    val casing: Color,
    val lockInk: Color,
)

private val BootLookDay = BootLook(
    scan = Color(0xFF5A3CC8), ink = Color(0xFF2A1C6B),
    tile = LogoTileDay, mark = Color.White, shade = Color(0xFF2A1675),
    panel = Color.White, panelText = Color(0xFF2A1C6B), pip = Color(0xFF087581),
    casing = Color(0xFF2A1C6B), lockInk = Color(0xFF2A1C6B),
)
private val BootLookTwilight = BootLook(
    scan = Color(0xFFB8A2F8), ink = Color(0xFFF3EEFF),
    tile = LogoTileTwilight, mark = LogoInkTwilight, shade = Color(0xFF0B0726),
    panel = Color(0xFF343C72), panelText = Color.White, pip = Color(0xFF5EE0E6),
    casing = Color(0xFF100C33), lockInk = Gold,
)

/**
 * One pass: the ring fills, locks at [LOCK_AT] and holds from [HOLD_AT] until the boot screen goes, so it
 * fades out locked on. The lock lands about a second in; should the app be ready sooner, it jumps there.
 */
private const val HUNT_MS = 1_800f
private const val LOCK_AT = 0.52f
private const val HOLD_AT = 0.8f
/** The most of the loop one frame moves on, about two frames at 60 Hz, and how fast the clock makes up time lost to slow frames. */
private const val MAX_STEP_MS = 40f
private const val CATCH_UP = 3f

private fun smooth(from: Float, to: Float, x: Float): Float {
    val k = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return k * k * (3 - 2 * k)
}

/** How far the radar field reaches on a screen [width]×[height], in the same units. */
private fun bootReach(width: Float, height: Float) = minOf(width, height * 1.4f) * 0.34f

/** The faint ring the field rests on, around the canvas centre, [reach] out. It never moves. */
private fun DrawScope.drawRadarRings(reach: Float, scan: Color) {
    drawCircle(scan.copy(alpha = 0.25f), reach * 0.75f, center, style = Stroke(1.dp.toPx()))
}

/**
 * Draws the lock around the canvas centre (the mark) at pass time [t]: a ring round the mark fills, turns
 * gold as it locks, and sends one pulse out towards [reach]. [target] is the mark's radius.
 */
private fun DrawScope.drawTargeting(t: Float, reach: Float, target: Float, scan: Color, ink: Color, casing: Color) {
    val c = center
    val seen = smooth(0f, 0.06f, t)
    val sinceLock = if (t >= LOCK_AT) (t - LOCK_AT) / (1f - LOCK_AT) else -1f
    val gilt = if (sinceLock >= 0f) smooth(0f, 0.08f, sinceLock) else 0f
    val color = lerp(scan, Gold, gilt)
    val ring = target + 7.dp.toPx()
    // The track, then the lock's progress filling it from the top.
    drawCircle(ink.copy(alpha = 0.18f * seen), ring, c, style = Stroke(3.dp.toPx()))
    if (gilt > 0f) drawCircle(casing.copy(alpha = gilt), ring, c, style = Stroke(6.dp.toPx()))
    drawArc(
        color.copy(alpha = seen), -90f, 360f * smooth(0.04f, LOCK_AT, t), useCenter = false,
        topLeft = c - Offset(ring, ring), size = Size(ring * 2, ring * 2), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
    )
    // One pulse rolls out from the mark the moment it locks.
    if (sinceLock in 0f..0.45f) {
        val k = sinceLock / 0.45f
        drawCircle(casing.copy(alpha = 0.6f * (1f - k)), ring + k * reach * 0.8f, c, style = Stroke((6f - 3f * k).dp.toPx()))
        drawCircle(Gold.copy(alpha = 0.6f * (1f - k)), ring + k * reach * 0.8f, c, style = Stroke((3.5f - 2.5f * k).dp.toPx()))
    }
}
