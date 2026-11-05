package app.lockon.android

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceControl
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.LayoutDirection
import app.lockon.ui.BootClock
import app.lockon.ui.BootCover
import app.lockon.ui.BootPictures
import app.lockon.ui.BootStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * The boot screen on a surface of its own over the app, drawn on a thread of its own, so the screens
 * building underneath can't hold it up. It draws the pictures the app takes of its boot screen
 * ([app.lockon.ui.BootPictureTaker]) with the targeting field between them, drawn each frame with the
 * boot screen's own drawing code. Until it has pictures the app's own first frame holds still, and it
 * steps aside if anything goes wrong.
 *
 * It draws with the CPU, not the surface's hardware canvas: every hardware canvas in an app shares one
 * render thread, which the screens warming up underneath keep busy with their first frames.
 */
class AndroidBootCover(private val activity: Activity, override val clock: BootClock) : BootCover, SurfaceHolder.Callback {
    private val _showing = MutableStateFlow(false)
    override val showing: StateFlow<Boolean> = _showing.asStateFlow()
    private val _settled = MutableStateFlow(false)
    /** True once it is up, or has given up; until then the app waits to start building (MainActivity). */
    val settled: StateFlow<Boolean> = _settled.asStateFlow()
    @Volatile private var step = BootStep.DECKS
    @Volatile private var art: Art? = null
    @Volatile private var surface: SurfaceHolder? = null
    @Volatile private var liftedAt = 0L
    // Well ahead of the threads loading and decoding underneath, as a phone has few fast cores to go round.
    private val thread = HandlerThread("Boot cover", Process.THREAD_PRIORITY_URGENT_DISPLAY).apply { start() }
    private val handler = Handler(thread.looper)
    private val view = SurfaceView(activity).apply {
        // Over the app's own window, with see-through pixels so it can fade out over it.
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        holder.addCallback(this@AndroidBootCover)
    }
    private val draw = CanvasDrawScope()
    @Suppress("DEPRECATION")
    private val refresh = 1_000_000_000.0 / (activity.windowManager.defaultDisplay.refreshRate.takeIf { it > 0f } ?: 60f)
    private val surfaceFade = if (Build.VERSION.SDK_INT >= 29) SurfaceFade(view) else null
    private var last = 0L
    private var drawnAt = 0L
    private var work = 0.0
    @Volatile private var ended = false

    init {
        activity.addContentView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        handler.post { Choreographer.getInstance().postFrameCallback(::frame) }
    }

    override fun step(step: BootStep) { this.step = step }

    override fun lift() {
        if (liftedAt == 0L) liftedAt = System.nanoTime()
    }

    /** Takes the app's pictures of its boot screen; it comes up once it has them. */
    fun pictures(pictures: BootPictures) {
        // Its words are laid out here, on the main thread, so the drawing thread only reads them back.
        val measurer = TextMeasurer(createFontFamilyResolver(activity), pictures.density, LayoutDirection.Ltr).also { pictures.targeting.warm(it) }
        handler.post {
            runCatching { Art(pictures, measurer) }
                .onSuccess { art = it }
                .onFailure { Log.w("LockOn", "Boot cover stepped aside", it); end() }
        }
    }

    /** Stops it where it is, for an activity going away before it has lifted. */
    fun close() {
        if (!ended) handler.post { end() }
    }

    override fun surfaceCreated(holder: SurfaceHolder) { surface = holder }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { surface = holder }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // The surface may only go once nothing draws on it, so wait for the drawing thread's frame to end.
        surface = null
        val done = java.util.concurrent.CountDownLatch(1)
        if (handler.post { done.countDown() }) done.await(500, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    private fun frame(nanos: Long) {
        if (ended) return
        try {
            val going = if (liftedAt == 0L) 0f else ((System.nanoTime() - liftedAt) / 1_000_000f / LIFT_MS).coerceAtMost(1f)
            if (going >= 1f) { end(); return }
            val holder = surface
            val art = art
            if (holder != null && art != null) {
                // The animation starts with the first frame this draws.
                if (last != 0L) clock.played += ((nanos - last) / 1_000_000f).coerceAtMost(50f)
                last = nanos
                // Each frame lasts a whole number of the display's refreshes, as few as the drawing allows,
                // so the frames stay evenly spaced. The cost of drawing is quick to come down and slow to
                // go up: the first frames are slow while everything warms up.
                val refreshes = ceil(work / refresh + 0.2).toInt().coerceAtLeast(1)
                if (nanos - drawnAt >= (refreshes - 0.5) * refresh) {
                    drawnAt = nanos
                    val started = System.nanoTime()
                    clock.handedOver = true
                    holder.lockCanvas()?.let { canvas ->
                        try {
                            art.frame(canvas, clock, step, going, draw, fadesItself = surfaceFade != null)
                        } finally {
                            holder.unlockCanvasAndPost(canvas)
                        }
                        surfaceFade?.set(going)
                        if (!_showing.value) {
                            _showing.value = true
                            _settled.value = true
                        }
                    }
                    val took = (System.nanoTime() - started).toDouble()
                    work = if (took < work || work == 0.0) took else work * 0.9 + took * 0.1
                }
            } else if (liftedAt != 0L && !_showing.value) {
                // Lifted before it ever showed: the app's own boot screen has been up all along.
                end(); return
            }
        } catch (e: Exception) {
            Log.w("LockOn", "Boot cover stepped aside", e)
            end(); return
        }
        Choreographer.getInstance().postFrameCallback(::frame)
    }

    private fun end() {
        if (ended) return
        ended = true
        art = null
        _settled.value = true
        activity.runOnUiThread {
            (view.parent as? ViewGroup)?.removeView(view)
            _showing.value = false
            thread.quitSafely()
        }
    }

    private companion object {
        /** As long as the app's own boot screen takes to fade out (Motion.SIGNATURE). */
        const val LIFT_MS = 480f
    }
}

/** Fades the whole surface as the system puts it together, which costs the drawing thread nothing. */
@androidx.annotation.RequiresApi(29)
private class SurfaceFade(private val view: SurfaceView) {
    private val transaction = SurfaceControl.Transaction()

    fun set(going: Float) {
        if (going <= 0f) return
        val control = view.surfaceControl ?: return
        if (control.isValid) transaction.setAlpha(control, 1f - going * going).apply()
    }
}

/**
 * One set of the boot screen's pictures, and how to put a frame together from them. Made on the drawing
 * thread. The parts that never move are kept ready to copy: the rings go into the sky, and the mark and
 * its labels are cut down to the area they fill, so all each frame draws afresh is the moving field.
 */
private class Art(private val pictures: BootPictures, private val measurer: TextMeasurer) {
    private val targeting = pictures.targeting
    private val density = pictures.density
    // Software copies: the CPU can't draw a picture kept on the graphics card.
    private val fronts: Map<BootStep, Bitmap>
    private val frontLeft: Int
    private val frontTop: Int

    init {
        val full = pictures.fronts.mapValues { it.value.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false) }
        val (left, top, right, bottom) = full.getValue(BootStep.entries.last()).inkedArea()
        frontLeft = left
        frontTop = top
        // The layers differ only in their step label, centred under the mark, and the last one is the longest.
        fronts = full.mapValues { (_, b) -> Bitmap.createBitmap(b, left, top, right - left, bottom - top).also { if (it !== b) b.recycle() } }
    }

    private var placedFor = -1 to -1
    private var placed = targeting
    private var sky: Bitmap? = null
    private var frontX = 0f
    private var frontY = 0f

    /**
     * Keeps the layers together on a [w]×[h] screen the way the boot screen lays them out: the sky
     * stretches to fill, the mark and its labels keep their size at the centre, and the field stays
     * centred on the mark and reaches as far as the new size gives it.
     */
    private fun place(w: Int, h: Int, draw: CanvasDrawScope) {
        val source = pictures.sky.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
        val fits = w == source.width && h == source.height
        val shift = Offset(((w - source.width) / 2f).roundToInt().toFloat(), ((h - source.height) / 2f).roundToInt().toFloat())
        val ready = if (fits) source else Bitmap.createScaledBitmap(source, w, h, true).also { if (it !== source) source.recycle() }
        placed = if (fits) targeting else targeting.moved(shift, w.toFloat(), h.toFloat())
        draw.draw(density, LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(Canvas(ready)), Size(w.toFloat(), h.toFloat())) { placed.drawRings(this) }
        // Opaque, which makes it a plain copy each frame.
        ready.setHasAlpha(false)
        sky = ready
        frontX = frontLeft + shift.x
        frontY = frontTop + shift.y
        placedFor = w to h
    }

    /** Draws the boot screen at [clock], [going] of the way through fading out, unless the surface [fadesItself]. */
    fun frame(canvas: Canvas, clock: BootClock, step: BootStep, going: Float, draw: CanvasDrawScope, fadesItself: Boolean) {
        val w = canvas.width
        val h = canvas.height
        if (placedFor != w to h) place(w, h, draw)
        val faded = if (going > 0f && !fadesItself) canvas.saveLayerAlpha(0f, 0f, w.toFloat(), h.toFloat(), ((1f - going * going) * 255).toInt()) else -1
        canvas.drawBitmap(sky!!, 0f, 0f, null)
        // Swelling a touch as it goes, as the app's own boot screen does; the sky's too smooth to show it.
        val grow = 1f + 0.06f * going
        canvas.save()
        canvas.scale(grow, grow, w / 2f, h / 2f)
        draw.draw(density, LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(canvas), Size(w.toFloat(), h.toFloat())) { placed.draw(this, clock, measurer, rings = false) }
        fronts[step]?.let { canvas.drawBitmap(it, frontX, frontY, null) }
        canvas.restore()
        if (faded >= 0) canvas.restoreToCount(faded)
    }
}

/** The smallest area holding every pixel of this picture that isn't fully clear, as left, top, right, bottom. */
private fun Bitmap.inkedArea(): List<Int> {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    // Every third pixel each way, then widened by that: the layer's edges are soft shadows and rims.
    var left = width; var top = height; var right = -1; var bottom = -1
    for (y in 0 until height step 3) {
        val row = y * width
        for (x in 0 until width step 3) {
            if (pixels[row + x] ushr 24 != 0) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                bottom = y
            }
        }
    }
    if (right < 0) return listOf(0, 0, 1, 1)
    return listOf(maxOf(0, left - 3), maxOf(0, top - 3), minOf(width, right + 4), minOf(height, bottom + 4))
}
