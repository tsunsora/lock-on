package app.lockon.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalGraphicsContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * What plays behind and after dialogs on desktop: the dim, which eases in and out, and the last frame of
 * a dialog that has closed. Callers drop a dialog from composition the moment it closes, so keeping it
 * alive to play an exit would mean changing every caller and leaving its buttons live, where a second
 * click could act twice. Its last frame is only a picture, so it can fade with nothing left to click.
 *
 * Desktop only. A phone's dialogs are windows of their own, which the system animates.
 */
@Stable
class DialogStage {
    internal var open by mutableIntStateOf(0)
    internal val ghosts = mutableStateListOf<StageGhost>()

    /** While true, the next frame drawn is kept in [themeFrame], for a change of theme. */
    internal var taking by mutableStateOf(false)
    internal var themeFrame: GraphicsLayer? = null
    internal var graphics: GraphicsContext? = null
    /** Where in the window the app's root sits, which is where a picture of it belongs. */
    internal var rootPosition = Offset.Zero

    /**
     * True for the frames in which the theme itself changes, when [motion] and the tile states in Settle.kt
     * go straight to their new colours. The window's old picture is fading away over them, which is the
     * change people see; under it every button, tab and chip on every kept screen used to ease its own
     * colours too, building itself again on each frame, and the fade showed only every third frame or so.
     * Not state: whatever reads it is being built again by the change of theme anyway.
     */
    internal var turning = false
}

/** Runs [change], which swaps the theme, with colours going straight to their new values rather than easing ([DialogStage.turning]). */
suspend fun DialogStage.turnTheme(change: () -> Unit) {
    turning = true
    try {
        change()
        // One frame builds the app in its new colours, and the next two what that set off: colours that
        // go straight to a new value still arrive a frame after they are asked for.
        // Bounded, since a minimised window draws no frames.
        withTimeoutOrNull(500) { repeat(3) { withFrameNanos { } } }
    } finally {
        turning = false
    }
}

/** Whether the theme is changing under a picture of the old one, so nothing needs to ease its colours. */
@Composable
internal fun themeTurning(): Boolean = LocalDialogStage.current?.turning == true

/** What fades away: a closed dialog's last frame as recorded, or the window in its old theme as an [image]. */
internal class StageGhost(val layer: GraphicsLayer?, val position: Offset, val durationMillis: Int, val to: Float, val image: ImageBitmap? = null) {
    val progress = Animatable(0f)
}

val LocalDialogStage = staticCompositionLocalOf<DialogStage?> { null }

/** The opacity of the dim behind a dialog, which was the desktop default when the stage took it over. */
private const val DIM = 0.6f

/**
 * Put on a dialog's panel, inside anything that scales it in. While the panel is up the stage dims the
 * window, and when it goes its last frame fades out and shrinks back slightly. The picture is recorded as
 * the panel draws, which costs only the dialog itself and only while it is open.
 */
@Composable
fun Modifier.onDialogStage(): Modifier {
    val stage = LocalDialogStage.current ?: return this
    val reduce = LocalReduceMotion.current
    val context = LocalGraphicsContext.current
    val layer = remember { context.createGraphicsLayer() }
    val position = remember { arrayOf(Offset.Zero) }
    DisposableEffect(Unit) {
        stage.open++
        onDispose {
            stage.open--
            if (reduce) context.releaseGraphicsLayer(layer)
            else stage.ghosts += StageGhost(layer, position[0], Motion.SHORT, to = 0.97f)
        }
    }
    return onPlaced { position[0] = it.positionInWindow() }
        .drawWithContent { layer.record { this@drawWithContent.drawContent() }; drawLayer(layer) }
}

/** For a dialog that plays its own exit, such as the filter sheet: the stage dims the window while [on], nothing more. */
@Composable
fun DimsWindow(on: Boolean) {
    val stage = LocalDialogStage.current ?: return
    DisposableEffect(on) {
        if (on) stage.open++
        onDispose { if (on) stage.open-- }
    }
}

/**
 * Draws the stage over the whole window, under nothing but the dialogs, which are layers above this one.
 * It holds no input, so clicks pass through. It must sit beside whatever carries [takesThemeFrame], never
 * inside it: that picture holds the layers within it, and one of them drawing the picture would never end.
 */
@Composable
fun DialogStageHost(stage: DialogStage) {
    val opening = stage.open > 0
    val dim by animateFloatAsState(
        if (opening) 1f else 0f,
        motion(if (opening) Motion.MEDIUM else Motion.SHORT),
        label = "dialog-dim",
    )
    val context = LocalGraphicsContext.current
    stage.ghosts.forEach { ghost ->
        key(ghost) {
            LaunchedEffect(Unit) {
                // The old theme's picture holds until the app beneath has been built in the new one, so
                // those long frames pass behind a still picture and the fade itself plays on quick ones.
                while (stage.turning) withFrameNanos { }
                ghost.progress.animateTo(1f, tween(ghost.durationMillis, easing = Motion.emphasizedAccelerate))
                stage.ghosts.remove(ghost)
                ghost.layer?.let(context::releaseGraphicsLayer)
            }
        }
    }
    val origin = remember { arrayOf(Offset.Zero) }
    Spacer(
        Modifier.fillMaxSize().onPlaced { origin[0] = it.positionInWindow() }.drawBehind {
            if (dim > 0f) drawRect(Color.Black.copy(alpha = DIM * dim))
            for (ghost in stage.ghosts) {
                val p = ghost.progress.value
                val at = ghost.position - origin[0]
                translate(at.x, at.y) {
                    ghost.image?.let { drawImage(it, alpha = 1f - p) }
                    val layer = ghost.layer ?: return@translate
                    layer.alpha = 1f - p
                    val s = 1f + (ghost.to - 1f) * p
                    scale(s, s, Offset(layer.size.width / 2f, layer.size.height / 2f)) { drawLayer(layer) }
                }
            }
        },
    )
}

/**
 * On the app's root, so a theme change can fade across: the window's last frame in the old theme is kept
 * and fades away over the new one. Costs one check per frame until a change asks for a picture.
 */
@Composable
fun Modifier.takesThemeFrame(stage: DialogStage): Modifier {
    val context = LocalGraphicsContext.current
    stage.graphics = context
    return onPlaced { stage.rootPosition = it.positionInWindow() }.drawWithContent {
        if (!stage.taking) {
            drawContent()
            return@drawWithContent
        }
        val layer = context.createGraphicsLayer()
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
        stage.themeFrame = layer
        stage.taking = false
    }
}

/**
 * Waits for the root to take its picture of the window as it is now, and puts it up to fade, for the theme
 * to change under it. The picture becomes a bitmap first: as a recording it still draws the app's live
 * layers, which would turn to the new theme at once while the rest faded. Bounded, since a minimised
 * window draws no frames.
 */
suspend fun DialogStage.takeThemeFrame() {
    taking = true
    withTimeoutOrNull(120) { snapshotFlow { taking }.first { !it } }
    taking = false
    val layer = themeFrame ?: return
    themeFrame = null
    val image = layer.toImageBitmap()
    graphics?.releaseGraphicsLayer(layer)
    // At the root's own place in the window. Under Lock On's title bar that is not the window's corner,
    // and a picture put there sat a title bar's height too high until it had faded.
    ghosts += StageGhost(null, rootPosition, Motion.LONG, to = 1f, image)
}
