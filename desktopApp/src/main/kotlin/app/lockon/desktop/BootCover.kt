package app.lockon.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import app.lockon.data.SettingsRepository
import app.lockon.data.ThemeChoice
import app.lockon.ui.BootClock
import app.lockon.ui.BootCover
import app.lockon.ui.BootLayers
import app.lockon.ui.BootScreen
import app.lockon.ui.BootStep
import app.lockon.ui.BootTargeting
import app.lockon.ui.components.LocalReduceMotion
import app.lockon.ui.theme.LockOnTheme
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Rect
import androidx.compose.ui.geometry.Offset
import org.jetbrains.skia.SurfaceProps
import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme
import java.awt.Color
import java.awt.Frame
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.nio.ByteOrder
import java.util.concurrent.Future
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * The boot screen in a window of its own, laid exactly over the app's and drawn on a thread of its
 * own, so the screens building underneath can't hold it up. The sky and the mark with its labels are
 * pictures taken once with Compose, on the app's UI thread ([onUiThread]); only the targeting field is
 * drawn each frame, with the boot screen's own drawing code. Until it is up the app's own boot screen
 * plays, and if anything goes wrong it steps aside and that one carries on from the same moment.
 */
class DesktopBootCover(private val settings: SettingsRepository) : BootCover {
    override val clock = BootClock()
    private val _showing = MutableStateFlow(false)
    override val showing: StateFlow<Boolean> = _showing.asStateFlow()
    private val _settled = MutableStateFlow(false)
    /** True once it is up, or has given up; until then the app's window shows only the boot screen's first frame (Main.kt). */
    val settled: StateFlow<Boolean> = _settled.asStateFlow()
    @Volatile private var step = BootStep.DECKS
    @Volatile private var lifted = false
    @Volatile private var target: Window? = null
    private val log = org.slf4j.LoggerFactory.getLogger("LockOn")
    /** When each of its frames went up, in [System.nanoTime], for BootProbe. */
    val frames: MutableList<Long> = java.util.Collections.synchronizedList(mutableListOf())
    /** How far the animation had played at each of those frames, in ms, for BootProbe. */
    val playedAt: MutableList<Float> = java.util.Collections.synchronizedList(mutableListOf())
    /** How long each frame took to draw and put up, in ms: Skia, then Java2D, then presenting, for BootProbe. */
    val work: MutableList<FloatArray> = java.util.Collections.synchronizedList(mutableListOf())

    init {
        thread(name = "Boot cover", isDaemon = true) {
            runCatching { play() }.onFailure { log.warn("Boot cover stepped aside", it) }
            _settled.value = true
            _showing.value = false
        }
    }

    @Volatile private var windowSky = false

    /** How far down the window the app starts, in window units: below the title bar, which the cover leaves showing. */
    @Volatile var topInset = 0

    /** The app's window, which it covers as soon as that shows. */
    fun attach(window: Window) { target = window }

    /** Hears that the window's first frame has its painted sky, so the cover goes up looking just like it. */
    fun windowSkyPainted() { windowSky = true }

    override fun step(step: BootStep) { this.step = step }
    /** The interface size chosen in Settings, which the app's own screens are drawn at. */
    internal fun interfaceScale(): Float = settings.size.value.scale
    override fun lift() { lifted = true }

    private fun play() {
        // The pictures are painted as soon as the window has a size, so the cover can go up soon after
        // the window shows.
        var pane: java.awt.Component? = null
        while (!lifted && pane == null) {
            pane = target?.let { (it as? javax.swing.RootPaneContainer)?.contentPane ?: it }?.takeIf { it.width > 0 && it.height > 0 }
            if (pane == null) Thread.sleep(5)
        }
        if (lifted || pane == null) return
        // Painting takes the UI thread, so it waits for the window's sky first: otherwise the window
        // would hold its plain stand-in sky until the cover came up with the painted one.
        val until = System.nanoTime() + 1_000_000_000L
        while (!lifted && !windowSky && System.nanoTime() < until) Thread.sleep(5)
        // And for the window's size to settle: one remembered as maximised opens at its floating size
        // and maximises a moment later, and pictures for the size it is about to leave would only
        // stretch while new ones were painted.
        var size = pane.size
        var steadySince = System.nanoTime()
        while (!lifted && System.nanoTime() - steadySince < 60_000_000L && System.nanoTime() < until) {
            Thread.sleep(5)
            if (pane.size != size) { size = pane.size; steadySince = System.nanoTime() }
        }
        val started = System.nanoTime()
        var art = Art.paint(this, Layout.sized(pane, topInset) ?: return)
        val painted = System.nanoTime()
        while (!lifted && !pane.isShowing) Thread.sleep(2)
        if (lifted) return
        var layout = Layout.of(pane, topInset) ?: return
        log.info("Boot cover: painted in %d ms, then waited %d ms for the window to show".format(
            (painted - started) / 1_000_000, (System.nanoTime() - painted) / 1_000_000))
        var repainting: Future<Art>? = null
        var shown = FramePixels(layout.pixels)
        // Owned by the app's window, so it stays just above that one, behind anything brought in front.
        val owner = target ?: return
        val cover = Window(owner).apply {
            focusableWindowState = false
            ignoreRepaint = true
            background = Color(art.fill)
            bounds = layout.bounds
        }
        // Sized by hand below, not by a layout: laying out waits for the app's UI thread, which is busy
        // while it boots, and until then a grown window showed the cover in a strip of its old width.
        cover.layout = null
        val surface = java.awt.Canvas().apply { ignoreRepaint = true; setBounds(0, 0, layout.bounds.width, layout.bounds.height) }
        cover.add(surface)
        try {
            if (lifted) return
            cover.isVisible = true
            surface.createBufferStrategy(2)
            var strategy = surface.bufferStrategy
            val draw = CanvasDrawScope()
            val hz = cover.graphicsConfiguration.device.displayMode.refreshRate.takeIf { it > 0 } ?: 60
            val pacing = Pacing(hz)
            var last = System.nanoTime()
            var liftedAt = 0L
            while (true) {
                val now = System.nanoTime()
                clock.played += ((now - last) / 1_000_000f).coerceAtMost(50f)
                last = now
                if (lifted && liftedAt == 0L) liftedAt = now
                val going = if (liftedAt == 0L) 0f else ((now - liftedAt) / 1_000_000f / LIFT_MS).coerceAtMost(1f)
                if (going >= 1f) break
                // Follow the app's window as it opens maximised, moves or changes size.
                val app = target ?: return
                if (!app.isShowing || (app as? Frame)?.extendedState?.and(Frame.ICONIFIED) != 0) return
                val nowLayout = Layout.of(pane, topInset) ?: return
                if (nowLayout.bounds != layout.bounds) cover.bounds = nowLayout.bounds
                if (surface.width != nowLayout.bounds.width || surface.height != nowLayout.bounds.height) {
                    surface.setBounds(0, 0, nowLayout.bounds.width, nowLayout.bounds.height)
                    surface.createBufferStrategy(2)
                    strategy = surface.bufferStrategy
                }
                layout = nowLayout
                // Each frame is drawn at the window's own size. A new size's pictures are painted in the
                // background, one size at a time; meanwhile the last ones are laid out to fit (Art.frame).
                repainting?.takeIf { it.isDone }?.let { done ->
                    repainting = null
                    runCatching { done.get() }.getOrNull()?.let { fresh ->
                        if (fresh.fits(layout)) { art.close(); art = fresh } else fresh.close()
                    }
                }
                if (repainting == null && !art.fits(layout)) {
                    // On a thread of its own, since it waits for the UI thread to paint.
                    val wanted = layout
                    repainting = java.util.concurrent.FutureTask { Art.paint(this, wanted) }.also { thread(isDaemon = true, name = "Boot cover repaint") { it.run() } }
                }
                if (shown.pixels != layout.pixels) { shown.close(); shown = FramePixels(layout.pixels) }
                val drawn = System.nanoTime()
                art.frame(shown, clock, step, going, draw)
                val skia = System.nanoTime()
                // The sky is a picture Java2D keeps on the graphics card, swelling with the rest as it goes;
                // only the moving field and the mark over it go up from Skia each frame.
                val (bw, bh) = layout.bounds.width to layout.bounds.height
                val grow = 1f + 0.06f * going
                fun put() {
                    val g = strategy.drawGraphics as java.awt.Graphics2D
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                    g.drawImage(art.skyPicture, (bw * (1f - grow) / 2f).roundToInt(), (bh * (1f - grow) / 2f).roundToInt(), (bw * grow).roundToInt(), (bh * grow).roundToInt(), null)
                    g.drawImage(shown.image, 0, 0, bw, bh, null)
                    g.dispose()
                }
                // Java2D drops the buffers when a window changes size, sometimes a moment after it
                // does; a frame drawn into dropped ones never shows, so it is drawn again, and new
                // buffers made if they stay lost.
                var tries = 0
                do {
                    do put() while (strategy.contentsRestored() && ++tries < 3)
                    strategy.show()
                } while (strategy.contentsLost() && ++tries < 3)
                if (strategy.contentsLost()) {
                    surface.createBufferStrategy(2)
                    strategy = surface.bufferStrategy
                }
                val blitted = System.nanoTime()
                Toolkit.getDefaultToolkit().sync()
                work += floatArrayOf((skia - drawn) / 1e6f, (blitted - skia) / 1e6f, (System.nanoTime() - blitted) / 1e6f)
                if (going > 0f) runCatching { cover.opacity = 1f - going * going }
                if (!_showing.value) {
                    clock.handedOver = true
                    _showing.value = true
                    _settled.value = true
                }
                frames += now
                playedAt += clock.played
                pacing.hold(now)
            }
        } finally {
            cover.dispose()
            art.close()
            shown.close()
        }
    }


    /** One of the boot screen's layers as a picture, [w]×[h] pixels; [onTargeting] hears where the field goes. */
    internal fun render(w: Int, h: Int, density: Density, layers: BootLayers, step: BootStep, onTargeting: ((BootTargeting) -> Unit)? = null): Image {
        var painted = false
        val scene = ImageComposeScene(w, h, density) {
            Themed {
                BootScreen(step, clock, playing = false, layers = layers, onTargeting = onTargeting, onSkyPainted = { painted = true })
            }
        }
        try {
            var image = scene.render()
            // The sky paints off the thread a moment after it knows its size.
            val until = System.nanoTime() + 2_000_000_000L
            while (layers == BootLayers.SKY && !painted && System.nanoTime() < until) {
                androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
                Thread.sleep(4)
                image.close()
                image = scene.render()
            }
            return image
        } finally {
            scene.close()
        }
    }

    @Composable
    private fun Themed(content: @Composable () -> Unit) {
        val dark = when (settings.theme.value) {
            ThemeChoice.SYSTEM -> currentSystemTheme == SystemTheme.DARK
            ThemeChoice.LIGHT -> false
            ThemeChoice.DARK -> true
        }
        CompositionLocalProvider(LocalReduceMotion provides false) { LockOnTheme(dark) { content() } }
    }

    private companion object {
        /** As long as the app's own boot screen takes to fade out (Motion.SIGNATURE). */
        const val LIFT_MS = 480f
    }
}

/** Where the app's window content is on screen, in window units and in pixels, and its density. */
private data class Layout(val bounds: Rectangle, val pixels: Pair<Int, Int>, val density: Density) {
    companion object {
        /** The part of [pane] below its top [inset], where the app is. */
        fun of(pane: java.awt.Component, inset: Int = 0): Layout? = runCatching {
            val at = pane.locationOnScreen
            val scale = pane.graphicsConfiguration.defaultTransform.scaleX.toFloat()
            val height = pane.height - inset
            val bounds = Rectangle(at.x, at.y + inset, pane.width, height)
            Layout(bounds, (pane.width * scale).roundToInt() to (height * scale).roundToInt(), Density(scale))
        }.getOrNull()?.takeIf { it.bounds.width > 0 && it.bounds.height > 0 }

        /** As [of], for a window that isn't on screen yet: only its size is known. */
        fun sized(pane: java.awt.Component, inset: Int = 0): Layout? {
            val scale = pane.graphicsConfiguration?.defaultTransform?.scaleX?.toFloat() ?: return null
            val height = pane.height - inset
            return Layout(Rectangle(0, 0, pane.width, height), (pane.width * scale).roundToInt() to (height * scale).roundToInt(), Density(scale))
                .takeIf { pane.width > 0 && height > 0 }
        }
    }
}

/**
 * Spaces frames evenly on a [hz] display: each frame lasts a whole number of refreshes, as few as the
 * drawing allows, up to about 100 frames a second. Each frame starts just after a refresh; once drawn,
 * it sleeps to just before the last refresh it is due, then waits for that one, so every frame lands on
 * the same beat whatever the drawing took.
 */
private class Pacing(hz: Int) {
    private val refresh = 1_000_000_000.0 / hz
    private val fewest = ceil(hz / 100.0).toInt().coerceAtLeast(1)
    private var work = 0.0

    /** Waits for the end of the frame that began at [started], in [System.nanoTime]. */
    fun hold(started: Long) {
        val took = (System.nanoTime() - started).toDouble()
        // Quick to come down and slow to go up: the first frames are slow while everything warms up,
        // and spacing the frames after them by those would stretch the animation's opening.
        work = if (took < work) took else work * 0.9 + took * 0.1
        val refreshes = maxOf(fewest, ceil(work / refresh + 0.2).toInt())
        val lastRefresh = started + ((refreshes - 1) * refresh - 1_500_000).toLong()
        while (true) {
            val left = lastRefresh - System.nanoTime()
            if (left <= 0) break
            java.util.concurrent.locks.LockSupport.parkNanos(left)
        }
        val dwm = Dwm.api
        if (dwm == null || dwm.DwmFlush() != 0) java.util.concurrent.locks.LockSupport.parkNanos(refresh.toLong())
    }
}

/** One frame's moving layers, the size of the window: Skia draws them and the window shows them over the sky. */
private class FramePixels(val pixels: Pair<Int, Int>) {
    private val bitmap = Bitmap().apply { allocPixels(ImageInfo.makeN32Premul(pixels.first, pixels.second)) }
    val canvas = Canvas(bitmap, SurfaceProps())
    val image = BufferedImage(pixels.first, pixels.second, BufferedImage.TYPE_INT_ARGB_PRE)
    private val ints = (image.raster.dataBuffer as DataBufferInt).data

    /** Copies what Skia drew into [image]. */
    fun show() {
        val pixmap = bitmap.peekPixels() ?: return
        // Skia's pixels are premultiplied BGRA bytes, which read as little-endian ints are what TYPE_INT_ARGB_PRE wants.
        Pointer(pixmap.addr).getByteBuffer(0, ints.size * 4L).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(ints)
    }

    fun close() {
        canvas.close()
        bitmap.close()
    }
}

/** The boot screen's pictures as painted for one size of window. */
private class Art(val sky: Image, val fronts: Map<BootStep, Image>, private val targeting: BootTargeting, private val density: Density, private val paintedFor: Layout) {
    /** The sky's top-left colour, to fill behind it while it swells. */
    val fill = sky.peekColor()
    /** The sky for Java2D, which keeps a picture that isn't written to again on the graphics card. */
    val skyPicture: BufferedImage = sky.toBufferedImage()
    // Its words are laid out here, on the UI thread, so the cover's thread only reads them back.
    private val measurer = TextMeasurer(createFontFamilyResolver(), density, LayoutDirection.Ltr).also { targeting.warm(it) }
    // The front layers differ only in their step label, centred under the mark; the last, longest one
    // holds the others' inked areas too.
    private val inkedArea = fronts.getValue(BootStep.entries.last()).inkedArea().let { area -> fronts.mapValues { area } }
    private var placedFor: Pair<Int, Int>? = null
    private var placed = targeting
    private var frontAt = Offset.Zero

    /** True when these pictures were painted for [layout]'s size, so they need no laying out to fit. */
    fun fits(layout: Layout) = layout.pixels == paintedFor.pixels && layout.density == paintedFor.density

    /**
     * Draws the boot screen at [clock], [going] of the way through fading out, into [into]. At a size
     * other than the one painted for, the layers keep together the way the boot screen lays them out:
     * the sky stretches to fill, the mark and its labels keep their size at the centre, and the field
     * stays centred on the mark and reaches as far as the new size gives it.
     */
    fun frame(into: FramePixels, clock: BootClock, step: BootStep, going: Float, draw: CanvasDrawScope) {
        val (w, h) = into.pixels
        if (placedFor != into.pixels) {
            val (pw, ph) = paintedFor.pixels
            frontAt = Offset(((w - pw) / 2f).roundToInt().toFloat(), ((h - ph) / 2f).roundToInt().toFloat())
            placed = if (into.pixels == paintedFor.pixels) targeting else targeting.moved(frontAt, w.toFloat(), h.toFloat())
            placedFor = into.pixels
        }
        val canvas = into.canvas
        canvas.clear(0)
        canvas.save()
        // Swelling a touch from the centre as it goes, as the app's own boot screen does.
        val grow = 1f + 0.06f * going
        canvas.translate(w / 2f, h / 2f)
        canvas.scale(grow, grow)
        canvas.translate(-w / 2f, -h / 2f)
        draw.draw(density, LayoutDirection.Ltr, canvas.asComposeCanvas(), Size(w.toFloat(), h.toFloat())) { placed.draw(this, clock, measurer) }
        // Only the part of the front layer with anything in it: blending a whole window of clear pixels
        // over the rest cost large windows a good share of each frame.
        val front = fronts.getValue(step)
        val inked = inkedArea.getValue(step)
        canvas.drawImageRect(front, inked, inked.offset(frontAt.x, frontAt.y))
        canvas.restore()
        into.show()
    }

    fun close() {
        sky.close()
        skyPicture.flush()
        fronts.values.forEach { it.close() }
    }

    companion object {
        fun paint(cover: DesktopBootCover, layout: Layout): Art {
            val (w, h) = layout.pixels
            val density = Density(layout.density.density * cover.interfaceScale())
            return onUiThread {
                var targeting: BootTargeting? = null
                val sky = cover.render(w, h, density, BootLayers.SKY, BootStep.DECKS)
                val fronts = BootStep.entries.associateWith { s -> cover.render(w, h, density, BootLayers.FRONT, s) { targeting = it } }
                Art(sky, fronts, checkNotNull(targeting) { "The boot screen never placed its targeting field" }, density, layout)
            }
        }
    }
}

/** The smallest rectangle holding every pixel of this picture that isn't fully clear. */
private fun Image.inkedArea(): Rect {
    val (w, h) = width to height
    val bitmap = Bitmap().apply { allocPixels(ImageInfo.makeN32Premul(w, h)) }
    try {
        val bytes = bitmap.drawn(this)
        // Every other pixel each way, then widened by that: the layer's edges are soft shadows and
        // rims, so this finds them at a quarter of the work, which counts while the UI thread waits.
        var left = w; var top = h; var right = -1; var bottom = -1
        for (y in 0 until h step 2) {
            val row = y * w * 4
            for (x in 0 until w step 2) {
                if (bytes[row + x * 4 + 3].toInt() != 0) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    bottom = y
                }
            }
        }
        if (right < 0) return Rect.makeWH(0f, 0f)
        return Rect.makeLTRB(maxOf(0, left - 2).toFloat(), maxOf(0, top - 2).toFloat(), minOf(w, right + 3).toFloat(), minOf(h, bottom + 3).toFloat())
    } finally {
        bitmap.close()
    }
}

/** This picture, opaque, as a Java2D image written once through its raster, so Java2D may keep it on the graphics card. */
private fun Image.toBufferedImage(): BufferedImage {
    val (w, h) = width to height
    val bitmap = Bitmap().apply { allocPixels(ImageInfo.makeN32Premul(w, h)) }
    try {
        val bytes = bitmap.drawn(this)
        val ints = IntArray(w * h)
        java.nio.ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(ints)
        return BufferedImage(w, h, BufferedImage.TYPE_INT_RGB).apply { raster.setDataElements(0, 0, w, h, ints) }
    } finally {
        bitmap.close()
    }
}

/** Draws [image] onto this bitmap and returns its pixels as BGRA bytes, row after row. */
private fun Bitmap.drawn(image: Image): ByteArray {
    Canvas(this, SurfaceProps()).use { it.clear(0); it.drawImage(image, 0f, 0f) }
    val pixmap = checkNotNull(peekPixels()) { "Couldn't reach a bitmap's pixels" }
    return ByteArray(width * height * 4).also { Pointer(pixmap.addr).read(0, it, 0, it.size) }
}

private fun Image.peekColor(): Int {
    val bitmap = Bitmap().apply { allocPixels(ImageInfo.makeN32Premul(1, 1)) }
    return try {
        Canvas(bitmap, SurfaceProps()).use { it.drawImage(this, 0f, 0f) }
        bitmap.getColor(0, 0)
    } finally {
        bitmap.close()
    }
}

/**
 * Runs [block] on the app's UI thread and waits for it. Compose keeps caches that every composition
 * shares and that only one thread may touch at a time: composing pictures on the cover's thread while
 * the app composed its window once wedged both in an endless loop at launch. So the cover composes
 * there, between the app's own frames, and its own thread only draws.
 */
private fun <T> onUiThread(block: () -> T): T {
    if (java.awt.EventQueue.isDispatchThread()) return block()
    var result: Result<T>? = null
    java.awt.EventQueue.invokeAndWait { result = runCatching(block) }
    return checkNotNull(result).getOrThrow()
}

/** Windows' compositor, which says when the display next refreshes. */
private interface Dwm : Library {
    @Suppress("FunctionName")
    fun DwmFlush(): Int

    companion object {
        val api: Dwm? by lazy { runCatching { Native.load("dwmapi", Dwm::class.java) }.getOrNull() }
    }
}
