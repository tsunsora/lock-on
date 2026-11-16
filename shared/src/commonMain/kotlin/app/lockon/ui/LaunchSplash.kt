package app.lockon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.lockon.ui.components.LockOnMark
import app.lockon.ui.theme.LocalTones

/**
 * The launch screen, drawn the way Android's own splash screen draws Lock On: the app icon, its white
 * mark on a teal disc, centred on the app's own ground, and nothing else. It covers the app while
 * the screens underneath fill in, then fades. Nothing in it moves, so the frames the screens building
 * underneath hold up never show. On Android 12 and later the system's own splash usually stays up
 * instead (MainActivity), and this shows only where there is none.
 *
 * The background is the app's ground in the current theme, Material You colours included (UX rule V3a),
 * so the fade into the app is only the icon going. [trailing] holds the window's controls, where the
 * app draws them.
 */
@Composable
fun LaunchSplash(modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Box(
        modifier.fillMaxSize().background(launchSplashColor())
            // Nothing underneath takes a press while it is up.
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() } },
    ) {
        // Android's sizes for an icon with a background: a 160dp disc, and the icon's 108-unit grid
        // drawn 240dp across, which with the icon's own 0.75 scale puts the mark on a 180dp grid, a
        // little wider than the disc it sits in.
        Box(Modifier.align(Alignment.Center).size(160.dp).clip(CircleShape).background(AppIconTile), contentAlignment = Alignment.Center) {
            LockOnMark(Modifier.requiredSize(180.dp), size = 180.dp, reticleColor = AppIconMark)
        }
        trailing?.let { Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) { it() } }
    }
}

/** The launch screen's background in the current theme, for a window's title bar to match while it is up. */
@Composable
fun launchSplashColor(): Color = LocalTones.current.sky[1]

/** The app icon's tile: Sky's teal, in either theme, as in the launcher (ic_launcher_background). */
internal val AppIconTile = Color(0xFF087581)
/** The mark on the app icon: white (ic_launcher_foreground). */
internal val AppIconMark = Color.White
