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
import app.lockon.ui.components.Accent
import app.lockon.ui.components.Gold
import app.lockon.ui.components.LocalReduceMotion
import app.lockon.ui.components.LockOnMark
import app.lockon.ui.components.SkyBackground
import app.lockon.ui.components.glass
import app.lockon.ui.components.glassColors
import app.lockon.ui.components.isTwilight
import app.lockon.ui.theme.SuitColors
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
    @Volatile var played: Float = FIRST_PASS_FROM * LOOP_MS
    /** Where in the loop it is, from 0 to 1. */
    val loop: Float get() = played / LOOP_MS % 1f
    /** How far the field has faded in on the first pass. */
    val intro: Float get() = ((played - FIRST_PASS_FROM * LOOP_MS) / INTRO_MS).coerceIn(0f, 1f)
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
    /** True for a cover that draws pictures the app takes of its boot screen ([BootPictureTaker]) rather than taking its own. */
    val wantsPictures: Boolean get() = false
    /** The app's latest pictures of its boot screen, when [wantsPictures]; taken again whenever its theme or size changes. */
    fun pictures(pictures: BootPictures) {}
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
class BootTargeting(val center: Offset, val reach: Float, val target: Float, val scan: Color, val ink: Color, val labelStyle: TextStyle) {
    fun draw(scope: DrawScope, clock: BootClock, labels: TextMeasurer) = with(scope) {
        translate(this@BootTargeting.center.x - size.width / 2, this@BootTargeting.center.y - size.height / 2) {
            drawTargeting(clock.loop, clock.intro, reach, target, scan, ink, labels, labelStyle)
        }
    }
}

/**
 * The launch screen: a targeting system locking on to the Lock On mark. A seeker reticle hunts
 * across a radar field, catches the mark, closes its brackets on it and locks in V-fin yellow with
 * a pulse, then lets go and hunts again. It covers the app while the screens underneath fill in,
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
            clock.played += step
            frames.intValue++
        }
    }
    val time = { frames.intValue; if (reduce) 0.82f else clock.loop }
    val shown = { if (reduce) 1f else clock.intro }
    val scan = if (isTwilight()) SuitColors.teal else Accent
    val ink = MaterialTheme.colorScheme.onSurface
    val labels = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    val density = LocalDensity.current
    val screen = modifier.fillMaxSize()
        // Nothing underneath takes a press while it boots.
        .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() } }
    val content: @Composable BoxScope.() -> Unit = content@{
        if (layers == BootLayers.SKY) return@content
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val reach = minOf(maxWidth, maxHeight * 1.4f) * 0.34f
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    // Sized to the mark but drawn well past its edges, so the field centres on the mark.
                    Canvas(
                        Modifier.size(176.dp).then(
                            if (onTargeting == null) Modifier else Modifier.onGloballyPositioned { c ->
                                val center = c.positionInRoot() + Offset(c.size.width / 2f, c.size.height / 2f)
                                with(density) { onTargeting(BootTargeting(center, reach.toPx(), 62.dp.toPx(), scan, ink, labelStyle)) }
                            },
                        ),
                    ) {
                        if (layers == BootLayers.ALL) drawTargeting(time(), shown(), reach.toPx(), 62.dp.toPx(), scan, ink, labels, labelStyle)
                    }
                    Box(
                        Modifier.size(124.dp).shadow(16.dp, CircleShape, ambientColor = glassColors().shade, spotColor = glassColors().shade)
                            .clip(CircleShape).glass(CircleShape, strong = true),
                        contentAlignment = Alignment.Center,
                    ) { LockOnMark(size = 84.dp, reticleColor = MaterialTheme.colorScheme.onSurface) }
                }
                Spacer(Modifier.height(20.dp))
                Text("Lock On", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.clip(CircleShape).glass(CircleShape).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BootStep.entries.forEach { s ->
                        val done = s.ordinal < step.ordinal
                        val now = s == step
                        Box(
                            Modifier.size(width = if (now) 18.dp else 8.dp, height = 8.dp).clip(CircleShape)
                                .background(if (done || now) Accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(step.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        trailing?.let { Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) { it() } }
    }
    // The front layer goes over a sky and targeting drawn elsewhere, so it has no sky of its own.
    if (layers == BootLayers.FRONT) Box(screen, content = content) else SkyBackground(screen, onSkyPainted, content = content)
}

/** One loop: hunt, close in, lock at [LOCK_AT], hold, let go. The boot screen stays until the first lock has landed. */
private const val LOOP_MS = 3_400f
private const val LOCK_AT = 0.52f
/** The most of the loop one frame moves on, about two frames at 60 Hz, and how fast the clock makes up time lost to slow frames. */
private const val MAX_STEP_MS = 40f
private const val CATCH_UP = 3f
/** Where in the loop the first pass starts, with the seeker already drifting in so the lock lands within about two seconds, and how long the field takes to fade in then. */
private const val FIRST_PASS_FROM = 0.22f
private const val INTRO_MS = 220f

private fun smooth(from: Float, to: Float, x: Float): Float {
    val k = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return k * k * (3 - 2 * k)
}

/** Faint contacts on the radar, as (bearing in turns, range as a share of reach); the sweep lights each as it passes. */
private val Contacts = listOf(0.08f to 0.82f, 0.27f to 1.25f, 0.41f to 0.58f, 0.63f to 1.05f, 0.79f to 1.42f, 0.93f to 0.7f)

/**
 * Draws the whole targeting pass around the canvas centre (the mark), [reach] out, at loop time [t],
 * faded in as far as [intro]. [target] is the mark's radius, where the brackets come to rest.
 */
private fun DrawScope.drawTargeting(t: Float, intro: Float, reach: Float, target: Float, scan: Color, ink: Color, labels: TextMeasurer, labelStyle: TextStyle) {
    val c = center
    val line = 2.dp.toPx()
    // The radar field: dashed range rings, a sweeping wedge, and contacts that glow as it passes.
    val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 8.dp.toPx()))
    listOf(0.55f, 1.05f, 1.55f).forEach { drawCircle(scan.copy(alpha = 0.16f), reach * it, c, style = Stroke(1.dp.toPx(), pathEffect = dash)) }
    repeat(12) { i ->
        val a = i / 12f * 2 * PI.toFloat()
        val d = Offset(cos(a), sin(a))
        drawLine(scan.copy(alpha = 0.22f), c + d * (reach * 1.55f - 6.dp.toPx()), c + d * (reach * 1.55f + 6.dp.toPx()), line)
    }
    // Two sweeps per loop, so the wedge comes round again where it started.
    val sweep = t * 2 * 360f
    rotate(sweep, pivot = c) {
        val r = reach * 1.6f
        drawArc(
            Brush.sweepGradient(0.88f to Color.Transparent, 1f to scan.copy(alpha = 0.14f), center = c),
            startAngle = -43f, sweepAngle = 43f, useCenter = true, topLeft = c - Offset(r, r), size = Size(r * 2, r * 2),
        )
        drawLine(scan.copy(alpha = 0.45f), c, c + Offset(r, 0f), line)
    }
    Contacts.forEach { (bearing, range) ->
        val since = (((sweep / 360f) - bearing) % 1f + 1f) % 1f
        val glow = (1f - since).let { it * it * it }
        val a = bearing * 2 * PI.toFloat()
        val p = c + Offset(cos(a), sin(a)) * (reach * range)
        val s = 4.dp.toPx()
        rotate(45f, pivot = p) { drawRect(scan.copy(alpha = 0.15f + 0.6f * glow), p - Offset(s, s), Size(s * 2, s * 2)) }
    }

    val fadeIn = smooth(0f, 0.06f, t) * smooth(0f, 1f, intro)
    val release = smooth(0.86f, 1f, t)
    val seen = fadeIn * (1f - release)
    // Catching the mark: the seeker drifts in from its hunt and the brackets close.
    val catch = smooth(0.18f, LOCK_AT, t)
    val locked = t >= LOCK_AT && release < 1f
    val sinceLock = if (t >= LOCK_AT) (t - LOCK_AT) / (1f - LOCK_AT) else -1f
    val hunt = 2 * PI * t
    val wander = Offset(cos(hunt * 1.3 + 0.6).toFloat() * reach * 0.95f, sin(hunt * 2.1 + 0.2).toFloat() * reach * 0.6f)
    val at = c + wander * (1f - catch)
    // The brackets squeeze a hair as they lock, then spring back out wide as it lets go.
    val squeeze = if (sinceLock in 0f..0.12f) 1f - 0.12f * sin(PI.toFloat() * sinceLock / 0.12f) else 1f
    val half = (target * 1.25f + (1f - catch) * target * 0.9f + release * target * 1.4f) * squeeze
    val color = if (locked) lerp(scan, Gold, smooth(0f, 0.08f, sinceLock)) else scan

    // The ring round the mark fills as the seeker closes, the lock's progress.
    val ring = target + 7.dp.toPx()
    drawCircle(ink.copy(alpha = 0.1f * seen), ring, c, style = Stroke(3.dp.toPx()))
    drawArc(
        color.copy(alpha = seen), -90f, 360f * smooth(0.28f, LOCK_AT, t), useCenter = false,
        topLeft = c - Offset(ring, ring), size = Size(ring * 2, ring * 2), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
    )

    // A pulse rolls out from the mark the moment it locks.
    if (sinceLock in 0f..0.45f) {
        val k = sinceLock / 0.45f
        drawCircle(Gold.copy(alpha = 0.7f * (1f - k)), ring + k * reach * 1.1f, c, style = Stroke((4f - 3f * k).dp.toPx()))
    }

    // The seeker: four corner brackets, turned while it hunts and square once it holds.
    rotate((1f - catch) * 35f + release * 20f, pivot = at) {
        val arm = half * 0.42f
        val shadow = SuitColors.cautionBlack.copy(alpha = 0.22f * seen)
        listOf(Offset(-1f, -1f), Offset(1f, -1f), Offset(-1f, 1f), Offset(1f, 1f)).forEach { d ->
            val corner = at + Offset(d.x * half, d.y * half)
            val bracket = Path().apply {
                moveTo(corner.x - d.x * arm, corner.y)
                lineTo(corner.x, corner.y)
                lineTo(corner.x, corner.y - d.y * arm)
            }
            drawPath(bracket, shadow, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
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
    // Red chevrons point in on the mark while the lock holds.
    if (locked) {
        val show = smooth(0f, 0.06f, sinceLock) * (1f - release)
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
                drawPath(chevron, SuitColors.chestRed.copy(alpha = show))
            }
        }
    }
    // A readout under the seeker's bottom-right bracket says what it is doing.
    val (word, wordColor) = when {
        locked -> "LOCKED" to SuitColors.chestRed
        catch > 0.35f -> "TRACKING" to ink.copy(alpha = 0.7f)
        else -> "SEARCHING" to ink.copy(alpha = 0.55f)
    }
    if (seen > 0f) {
        val laid = labels.measure(word, labelStyle.copy(color = wordColor))
        val corner = at + Offset(half, half)
        drawText(laid, topLeft = corner + Offset(-laid.size.width.toFloat(), 8.dp.toPx()), alpha = seen)
    }
}
