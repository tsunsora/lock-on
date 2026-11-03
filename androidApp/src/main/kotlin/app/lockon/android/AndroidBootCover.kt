package app.lockon.android

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
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

/**
 * The boot screen on a surface of its own over the app, drawn on a thread of its own, so the screens
 * building underneath can't hold it up. It draws the pictures the app takes of its boot screen
 * ([app.lockon.ui.BootPictureTaker]) with the targeting field between them, drawn each frame with the
 * boot screen's own drawing code. Until it has pictures the app's own boot screen plays, and it steps
 * aside the same way if anything goes wrong.
 */
class AndroidBootCover(private val activity: Activity, override val clock: BootClock) : BootCover, SurfaceHolder.Callback {
    private val _showing = MutableStateFlow(false)
    override val showing: StateFlow<Boolean> = _showing.asStateFlow()
    override val wantsPictures get() = true
    @Volatile private var step = BootStep.DECKS
    @Volatile private var art: Art? = null
    @Volatile private var surface: SurfaceHolder? = null
    @Volatile private var liftedAt = 0L
    private val thread = HandlerThread("Boot cover").apply { start() }
    private val handler = Handler(thread.looper)
    private val view = SurfaceView(activity).apply {
        // Over the app's own window, with see-through pixels so it can fade out over it.
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        holder.addCallback(this@AndroidBootCover)
    }
    private val draw = CanvasDrawScope()
    private val fade = Paint(Paint.FILTER_BITMAP_FLAG)
    private var last = 0L
    private var ended = false

    init {
        activity.addContentView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        handler.post { Choreographer.getInstance().postFrameCallback(::frame) }
    }

    override fun step(step: BootStep) { this.step = step }

    override fun lift() {
        if (liftedAt == 0L) liftedAt = System.nanoTime()
    }

    override fun pictures(pictures: BootPictures) {
        val fresh = Art(pictures, activity)
        handler.post { art = fresh }
    }

    override fun surfaceCreated(holder: SurfaceHolder) { surface = holder }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { surface = holder }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // The surface may only go once nothing draws on it, so wait for the drawing thread's frame to end.
        surface = null
        val done = java.util.concurrent.CountDownLatch(1)
        handler.post { done.countDown() }
        done.await(500, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    private fun frame(nanos: Long) {
        if (ended) return
        try {
            val played = if (last == 0L) 0f else (nanos - last) / 1_000_000f
            last = nanos
            clock.played += played.coerceAtMost(50f)
            val going = if (liftedAt == 0L) 0f else ((System.nanoTime() - liftedAt) / 1_000_000f / LIFT_MS).coerceAtMost(1f)
            if (going >= 1f) { end(); return }
            val holder = surface
            val art = art
            if (holder != null && art != null) {
                clock.handedOver = true
                val canvas = holder.lockHardwareCanvas()
                try {
                    art.frame(canvas, clock, step, going, draw, fade)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
                if (!_showing.value) _showing.value = true
            } else if (liftedAt != 0L && !_showing.value) {
                // Lifted before it ever showed: the app's own boot screen has been playing all along.
                end(); return
            }
        } catch (e: Exception) {
            Log.w("LockOn", "Boot cover stepped aside", e)
            end(); return
        }
        Choreographer.getInstance().postFrameCallback(::frame)
    }

    private fun end() {
        ended = true
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

/** One set of the boot screen's pictures, and how to put a frame together from them. */
private class Art(pictures: BootPictures, activity: Activity) {
    private val sky: Bitmap = pictures.sky.asAndroidBitmap()
    private val fronts: Map<BootStep, Bitmap> = pictures.fronts.mapValues { it.value.asAndroidBitmap() }
    private val targeting = pictures.targeting
    private val density = pictures.density
    // Its words are laid out here, on the main thread, so the drawing thread only reads them back.
    private val measurer = TextMeasurer(createFontFamilyResolver(activity), density, LayoutDirection.Ltr).also { targeting.warm(it) }

    fun frame(canvas: Canvas, clock: BootClock, step: BootStep, going: Float, draw: CanvasDrawScope, fade: Paint) {
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        // Fading out over the app and swelling a touch as it goes, as the app's own boot screen does.
        val faded = if (going > 0f) canvas.saveLayerAlpha(0f, 0f, w, h, ((1f - going * going) * 255).toInt()) else -1
        val grow = 1f + 0.06f * going
        canvas.save()
        canvas.scale(grow, grow, w / 2f, h / 2f)
        canvas.drawBitmap(sky, 0f, 0f, fade)
        draw.draw(density, LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(canvas), Size(w, h)) { targeting.draw(this, clock, measurer) }
        fronts[step]?.let { canvas.drawBitmap(it, 0f, 0f, fade) }
        canvas.restore()
        if (faded >= 0) canvas.restoreToCount(faded)
    }
}
