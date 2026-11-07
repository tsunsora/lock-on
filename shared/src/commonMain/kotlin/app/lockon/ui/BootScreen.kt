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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lockon.ui.components.Gold
import app.lockon.ui.components.LocalReduceMotion
import app.lockon.ui.components.LockOnMark
import app.lockon.ui.components.SkyBackground
import app.lockon.ui.components.isTwilight
import kotlinx.coroutines.flow.StateFlow
import kotlin.concurrent.Volatile
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    val stage: Float get() = minOf(played / LOOP_MS, HOLD_AT)
    /** Where the radar sweep is in its loop, from 0 to 1; it keeps turning while the lock holds. */
    val loop: Float get() = played / LOOP_MS % 1f
    /** True once the first lock has landed, so the boot screen may go. */
    val locked: Boolean get() = played >= LOOP_MS * (LOCK_AT + 0.04f)

    /** Jumps to just past the first lock, for tests whose frames come in steps too big for this clock to keep up with. */
    fun skipToLock() { played = LOOP_MS * (LOCK_AT + 0.04f) }
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
            drawTargeting(clock.stage, clock.loop, reach, target, scan, ink, casing, lockInk, labels, labelStyle)
        }
    }

    /** Draws the parts of the field that never move: the range rings and the ticks round them. */
    fun drawRings(scope: DrawScope) = with(scope) {
        translate(this@BootTargeting.center.x - size.width / 2, this@BootTargeting.center.y - size.height / 2) { drawRadarRings(reach, scan) }
    }

    /** Lays out the seeker's readouts ahead of time, so drawing on another thread later only reads them from [labels]' cache. */
    fun warm(labels: TextMeasurer) = readouts(ink, lockInk).toList().forEach { (word, color) -> labels.measure(word, labelStyle.copy(color = color)) }

    /**
     * The field on a screen [width]×[height] pixels whose boot screen sits [by] from where it was: the
     * boot screen keeps its mark centred, so the field moves with it, and its reach follows the size.
     */
    fun moved(by: Offset, width: Float, height: Float) = BootTargeting(center + by, bootReach(width, height), target, scan, ink, casing, lockInk, labelStyle)
}

/**
 * The launch screen: a targeting system locking on to the Lock On mark. A seeker reticle hunts
 * across a radar field, catches the mark, closes its brackets on it and locks in gold with a pulse, and holds the lock until it fades out. It covers the app while the screens underneath fill in,
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
    // underneath hold up some frames by a few hundred ms, and on the wall clock the seeker jumped
    // across the field each time. So the clock moves at most [MAX_STEP_MS] a frame and makes up the
    // rest at up to [CATCH_UP] times speed once frames are quick again, and the seeker only glides.
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
    val sweep = { frames.intValue; if (reduce) HOLD_AT else clock.loop }
    val look = if (isTwilight()) BootLookTwilight else BootLookDay
    val scan = look.scan
    val ink = look.ink
    val labels = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
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
                            drawTargeting(time(), sweep(), reach.toPx(), 62.dp.toPx(), scan, ink, look.casing, look.lockInk, labels, labelStyle)
                        }
                    }
                    Box(
                        Modifier.size(124.dp).shadow(8.dp, CircleShape, ambientColor = look.shade, spotColor = look.shade)
                            .clip(CircleShape).background(Brush.linearGradient(look.tile)),
                        contentAlignment = Alignment.Center,
                    ) { LockOnMark(size = 84.dp, reticleColor = look.mark) }
                }
                Spacer(Modifier.height(20.dp))
                Text("Lock On", style = MaterialTheme.typography.headlineMedium, color = ink)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.clip(CircleShape).background(look.panel).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BootStep.entries.forEach { s ->
                        val done = s.ordinal < step.ordinal
                        val now = s == step
                        Box(
                            Modifier.size(width = if (now) 18.dp else 8.dp, height = 8.dp).clip(CircleShape)
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
    val tile: List<Color>,
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
 * One pass: hunt, close in, lock at [LOCK_AT] and hold from [HOLD_AT] until the boot screen goes, so it
 * fades out locked on rather than hunting again. The boot screen stays until the lock has landed.
 */
private const val LOOP_MS = 3_400f
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

/** The seeker's readouts, searching, tracking and locked, each in its colour. */
private fun readouts(ink: Color, lockInk: Color) = Triple("SEARCHING" to ink.copy(alpha = 0.85f), "TRACKING" to ink.copy(alpha = 0.95f), "LOCKED" to lockInk)

/** Faint contacts on the radar, as (bearing in turns, range as a share of reach); the sweep lights each as it passes. */
private val Contacts = listOf(0.08f to 0.82f, 0.27f to 1.25f, 0.41f to 0.58f, 0.63f to 1.05f, 0.79f to 1.42f, 0.93f to 0.7f)

/** The radar field's dashed range rings and the ticks round them, around the canvas centre, [reach] out. */
private fun DrawScope.drawRadarRings(reach: Float, scan: Color) {
    val c = center
    val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 8.dp.toPx()))
    listOf(0.55f, 1.05f, 1.55f).forEach { drawCircle(scan.copy(alpha = 0.3f), reach * it, c, style = Stroke(1.dp.toPx(), pathEffect = dash)) }
    repeat(12) { i ->
        val a = i / 12f * 2 * PI.toFloat()
        val d = Offset(cos(a), sin(a))
        drawLine(scan.copy(alpha = 0.4f), c + d * (reach * 1.55f - 6.dp.toPx()), c + d * (reach * 1.55f + 6.dp.toPx()), 2.dp.toPx())
    }
}

/**
 * Draws the whole targeting pass around the canvas centre (the mark), [reach] out, at pass time [t],
 * with the radar sweep at [loop] of its own loop. [target] is the mark's radius, where the brackets come to rest.
 */
private fun DrawScope.drawTargeting(t: Float, loop: Float, reach: Float, target: Float, scan: Color, ink: Color, casing: Color, lockInk: Color, labels: TextMeasurer, labelStyle: TextStyle) {
    val c = center
    val line = 2.dp.toPx()
    // Over the rings: a sweeping wedge, and contacts that glow as it passes.
    // Two sweeps per loop, so the wedge comes round again where it started.
    val sweep = loop * 2 * 360f
    rotate(sweep, pivot = c) {
        val r = reach * 1.6f
        drawArc(
            Brush.sweepGradient(0.88f to Color.Transparent, 1f to scan.copy(alpha = 0.1f), center = c),
            startAngle = -43f, sweepAngle = 43f, useCenter = true, topLeft = c - Offset(r, r), size = Size(r * 2, r * 2),
        )
        drawLine(scan.copy(alpha = 0.35f), c, c + Offset(r, 0f), line)
    }
    Contacts.forEach { (bearing, range) ->
        val since = (((sweep / 360f) - bearing) % 1f + 1f) % 1f
        val glow = (1f - since).let { it * it * it }
        val a = bearing * 2 * PI.toFloat()
        val p = c + Offset(cos(a), sin(a)) * (reach * range)
        val s = 4.dp.toPx()
        rotate(45f, pivot = p) { drawRect(scan.copy(alpha = 0.3f + 0.5f * glow), p - Offset(s, s), Size(s * 2, s * 2)) }
    }

    val seen = smooth(0f, 0.06f, t)
    // Catching the mark: the seeker drifts in from its hunt and the brackets close.
    val catch = smooth(0.18f, LOCK_AT, t)
    val locked = t >= LOCK_AT
    val sinceLock = if (t >= LOCK_AT) (t - LOCK_AT) / (1f - LOCK_AT) else -1f
    val hunt = 2 * PI * t
    val wander = Offset(cos(hunt * 1.3 + 0.6).toFloat() * reach * 0.95f, sin(hunt * 2.1 + 0.2).toFloat() * reach * 0.6f)
    val at = c + wander * (1f - catch)
    // The brackets squeeze a hair as they lock.
    val squeeze = if (sinceLock in 0f..0.12f) 1f - 0.12f * sin(PI.toFloat() * sinceLock / 0.12f) else 1f
    val half = (target * 1.25f + (1f - catch) * target * 0.9f) * squeeze
    // How far the seeker has turned gold, and with that how much of its dark casing shows.
    val gilt = if (locked) smooth(0f, 0.08f, sinceLock) else 0f
    val color = lerp(scan, Gold, gilt)

    // The ring round the mark fills as the seeker closes, the lock's progress.
    val ring = target + 7.dp.toPx()
    drawCircle(ink.copy(alpha = 0.18f * seen), ring, c, style = Stroke(3.dp.toPx()))
    if (gilt > 0f) drawCircle(casing.copy(alpha = gilt), ring, c, style = Stroke(6.dp.toPx()))
    drawArc(
        color.copy(alpha = seen), -90f, 360f * smooth(0.28f, LOCK_AT, t), useCenter = false,
        topLeft = c - Offset(ring, ring), size = Size(ring * 2, ring * 2), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
    )

    // A pulse rolls out from the mark the moment it locks.
    if (sinceLock in 0f..0.45f) {
        val k = sinceLock / 0.45f
        drawCircle(casing.copy(alpha = 0.7f * (1f - k)), ring + k * reach * 1.1f, c, style = Stroke((7f - 3f * k).dp.toPx()))
        drawCircle(Gold.copy(alpha = 0.7f * (1f - k)), ring + k * reach * 1.1f, c, style = Stroke((4f - 3f * k).dp.toPx()))
    }

    // The seeker: four corner brackets, turned while it hunts and square once it holds.
    rotate((1f - catch) * 35f, pivot = at) {
        val arm = half * 0.42f
        val keyline = casing.copy(alpha = gilt * seen)
        listOf(Offset(-1f, -1f), Offset(1f, -1f), Offset(-1f, 1f), Offset(1f, 1f)).forEach { d ->
            val corner = at + Offset(d.x * half, d.y * half)
            val bracket = Path().apply {
                moveTo(corner.x - d.x * arm, corner.y)
                lineTo(corner.x, corner.y)
                lineTo(corner.x, corner.y - d.y * arm)
            }
            if (gilt > 0f) drawPath(bracket, keyline, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(bracket, color.copy(alpha = seen), style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
    // A crosshair rides the seeker while it hunts and gives way to the mark's own reticle as it closes.
    val cross = seen * (1f - smooth(0.3f, LOCK_AT, t))
    if (cross > 0f) {
        listOf(Offset(0f, -1f), Offset(0f, 1f), Offset(-1f, 0f), Offset(1f, 0f)).forEach { d ->
            drawLine(color.copy(alpha = 0.8f * cross), at + d * (half * 0.2f), at + d * (half * 0.6f), line, StrokeCap.Round)
        }
        drawCircle(color.copy(alpha = 0.8f * cross), 3.dp.toPx(), at)
    }
    // Chevrons point in on the mark while the lock holds.
    if (locked) {
        val show = smooth(0f, 0.06f, sinceLock)
        val tip = ring + 10.dp.toPx() + 4.dp.toPx() * (1f - show)
        val s = 6.dp.toPx()
        repeat(4) { i ->
            rotate(45f + i * 90f, pivot = c) {
                val chevron = Path().apply {
                    moveTo(c.x, c.y - tip)
                    lineTo(c.x - s, c.y - tip - s * 1.3f)
                    lineTo(c.x + s, c.y - tip - s * 1.3f)
                    close()
                }
                drawPath(chevron, lockInk.copy(alpha = show))
            }
        }
    }
    // A readout under the seeker's bottom-right bracket says what it is doing.
    val (searching, tracking, lockedOn) = readouts(ink, lockInk)
    val (word, wordColor) = when {
        locked -> lockedOn
        catch > 0.35f -> tracking
        else -> searching
    }
    if (seen > 0f) {
        val laid = labels.measure(word, labelStyle.copy(color = wordColor))
        // Under the bottom-right bracket's corner as the seeker turns it, centred while that corner is the
        // lowest point, so the bracket never crosses the word, and ending at the corner's right edge once square.
        val turn = (1f - catch) * 35f * PI.toFloat() / 180f
        val corner = at + Offset(cos(turn) - sin(turn), sin(turn) + cos(turn)) * half
        val width = laid.size.width.toFloat()
        val topLeft = corner + Offset(-width * (0.5f + 0.5f * catch), 8.dp.toPx())
        // It comes back in full as the seeker settles, so the lock's word is never dimmed.
        val clear = maxOf(readoutClear(topLeft - c, width, laid.size.height.toFloat()), smooth(0.85f, 1f, catch))
        drawText(laid, topLeft = topLeft, alpha = seen * clear)
    }
}

/**
 * How visible a readout may be with its corner at [at] from the mark's centre, going by its gap to the
 * title and the progress pill below the mark, which lie over it: gone when it would touch them, full
 * once clear.
 */
private fun DrawScope.readoutClear(at: Offset, width: Float, height: Float): Float {
    val across = 112.dp.toPx()
    val gapX = maxOf(at.x - across, -across - (at.x + width))
    val gapY = maxOf(at.y - 196.dp.toPx(), 108.dp.toPx() - (at.y + height))
    return smooth(0f, 10.dp.toPx(), maxOf(gapX, gapY))
}
