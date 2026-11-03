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
import org.jetbrains.skia.SurfaceProps
import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme
import java.awt.Color
import java.awt.Frame
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import java.awt.image.BufferStrategy
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
    @Volatile private var step = BootStep.DECKS
    @Volatile private var lifted = false
    @Volatile private var target: Window? = null
    private val log = org.slf4j.LoggerFactory.getLogger("LockOn")
    /** When each of its frames went up, in [System.nanoTime], for BootProbe. */
    val frames: MutableList<Long> = java.util.Collections.synchronizedList(mutableListOf())

    init {
        thread(name = "Boot cover", isDaemon = true) {
            runCatching { play() }.onFailure { log.warn("Boot cover stepped aside", it) }
            _showing.value = false
        }
    }

    /** The app's window, which it covers as soon as that shows. */
    fun attach(window: Window) { target = window }

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
        val started = System.nanoTime()
        var art = Art.paint(this, Layout.sized(pane) ?: return)
        val painted = System.nanoTime()
        while (!lifted && !pane.isShowing) Thread.sleep(2)
        if (lifted) return
        var layout = Layout.of(pane) ?: return
        log.info("Boot cover: painted in %d ms, then waited %d ms for the window to show".format(
            (painted - started) / 1_000_000, (System.nanoTime() - painted) / 1_000_000))
        var repainting: Future<Art>? = null
        // Owned by the app's window, so it stays just above that one, behind anything brought in front.
        val owner = target ?: return
        val cover = Window(owner).apply {
            focusableWindowState = false
            ignoreRepaint = true
            background = Color(art.fill)
            bounds = layout.bounds
        }
        val surface = java.awt.Canvas().apply { ignoreRepaint = true }
        cover.add(surface)
        try {
            if (lifted) return
            cover.isVisible = true
            surface.createBufferStrategy(2)
            val strategy = surface.bufferStrategy
            val draw = CanvasDrawScope()
            val hz = cover.graphicsConfiguration.device.displayMode.refreshRate.takeIf { it > 0 } ?: 60
            // Frames at up to about 100 a second, each a whole number of the display's refreshes.
            val refreshesPerFrame = ceil(hz / 100.0).toInt().coerceAtLeast(1)
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
                val nowLayout = Layout.of(pane) ?: return
                if (nowLayout.bounds != layout.bounds) cover.bounds = nowLayout.bounds
                // A new size is painted in the background; meanwhile the last pictures stretch to fit.
                if (nowLayout.pixels != layout.pixels || nowLayout.density != layout.density) {
                    repainting?.cancel(false)
                    // On a thread of its own, since it waits for the UI thread to paint.
                    repainting = java.util.concurrent.FutureTask { Art.paint(this, nowLayout) }.also { thread(isDaemon = true, name = "Boot cover repaint") { it.run() } }
                }
                layout = nowLayout
                repainting?.takeIf { it.isDone }?.let { done ->
                    repainting = null
                    runCatching { done.get() }.getOrNull()?.let { fresh -> art.close(); art = fresh }
                }
                art.frame(clock, step, going, draw)
                val g = strategy.drawGraphics
                g.drawImage(art.image, 0, 0, layout.bounds.width, layout.bounds.height, null)
                g.dispose()
                strategy.show()
                Toolkit.getDefaultToolkit().sync()
                if (going > 0f) runCatching { cover.opacity = 1f - going * going }
                if (!_showing.value) _showing.value = true
                frames += now
                pace(strategy, refreshesPerFrame)
            }
        } finally {
            cover.dispose()
            art.close()
        }
    }

    /** Waits for the display's next refreshes, or about a frame where Windows can't say when they come. */
    private fun pace(strategy: BufferStrategy, refreshes: Int) {
        val dwm = Dwm.api
        if (dwm == null) { Thread.sleep(10); return }
        repeat(refreshes) { if (dwm.DwmFlush() != 0) Thread.sleep(8) }
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
        fun of(pane: java.awt.Component): Layout? = runCatching {
            val at = pane.locationOnScreen
            val scale = pane.graphicsConfiguration.defaultTransform.scaleX.toFloat()
            val bounds = Rectangle(at.x, at.y, pane.width, pane.height)
            Layout(bounds, (pane.width * scale).roundToInt() to (pane.height * scale).roundToInt(), Density(scale))
        }.getOrNull()?.takeIf { it.bounds.width > 0 && it.bounds.height > 0 }

        /** As [of], for a window that isn't on screen yet: only its size is known. */
        fun sized(pane: java.awt.Component): Layout? {
            val scale = pane.graphicsConfiguration?.defaultTransform?.scaleX?.toFloat() ?: return null
            return Layout(Rectangle(0, 0, pane.width, pane.height), (pane.width * scale).roundToInt() to (pane.height * scale).roundToInt(), Density(scale))
                .takeIf { pane.width > 0 && pane.height > 0 }
        }
    }
}

/** The pictures for one size of window, and the frame they are put together in. */
private class Art(val sky: Image, val fronts: Map<BootStep, Image>, val targeting: BootTargeting, private val density: Density, w: Int, h: Int) {
    /** The sky's top-left colour, to fill behind it while it swells. */
    val fill = sky.peekColor()
    // Its words are laid out here, on the UI thread, so the cover's thread only reads them back.
    private val measurer = TextMeasurer(createFontFamilyResolver(), density, LayoutDirection.Ltr).also { targeting.warm(it) }
    private val bitmap = Bitmap().apply { allocPixels(ImageInfo.makeN32Premul(w, h)) }
    private val canvas = Canvas(bitmap, SurfaceProps())
    private val size = Size(w.toFloat(), h.toFloat())
    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    private val pixels = (image.raster.dataBuffer as DataBufferInt).data

    /** Draws the boot screen at [clock], [going] of the way through fading out, into [image]. */
    fun frame(clock: BootClock, step: BootStep, going: Float, draw: CanvasDrawScope) {
        canvas.clear(fill)
        canvas.save()
        // Swelling a touch as it goes, as the app's own boot screen does.
        val grow = 1f + 0.06f * going
        canvas.scale(grow, grow)
        canvas.translate(size.width * (1f / grow - 1f) / 2f, size.height * (1f / grow - 1f) / 2f)
        canvas.drawImage(sky, 0f, 0f)
        draw.draw(density, LayoutDirection.Ltr, canvas.asComposeCanvas(), size) { targeting.draw(this, clock, measurer) }
        canvas.drawImage(fronts.getValue(step), 0f, 0f)
        canvas.restore()
        val pixmap = bitmap.peekPixels() ?: return
        // Skia's pixels are BGRA bytes, which read as little-endian ints are what TYPE_INT_RGB wants.
        Pointer(pixmap.addr).getByteBuffer(0, pixels.size * 4L).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(pixels)
    }

    fun close() {
        sky.close()
        fronts.values.forEach { it.close() }
        canvas.close()
        bitmap.close()
    }

    companion object {
        fun paint(cover: DesktopBootCover, layout: Layout): Art {
            val (w, h) = layout.pixels
            val density = Density(layout.density.density * cover.interfaceScale())
            return onUiThread {
                var targeting: BootTargeting? = null
                val sky = cover.render(w, h, density, BootLayers.SKY, BootStep.DECKS)
                val fronts = BootStep.entries.associateWith { s -> cover.render(w, h, density, BootLayers.FRONT, s) { targeting = it } }
                Art(sky, fronts, checkNotNull(targeting) { "The boot screen never placed its targeting field" }, density, w, h)
            }
        }
    }
}

private fun Image.peekColor(): Int {
    val bitmap = Bitmap().apply { allocPixels(ImageInfo.makeN32Premul(1, 1)) }
    return try {
        readPixels(bitmap, 0, 0)
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
