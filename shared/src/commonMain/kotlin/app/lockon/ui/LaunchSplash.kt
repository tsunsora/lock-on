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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.lockon.ui.components.LockOnMark
import app.lockon.ui.components.bootSkyStops
import app.lockon.ui.components.isTwilight
import app.lockon.ui.theme.LocalThemeSeed

/**
 * The launch screen, drawn the way Android's own splash screen draws Lock On: the app icon, its violet
 * disc with the white mark, centred on a plain background, and nothing else. It covers the app while
 * the screens underneath fill in, then fades. Nothing in it moves, so the frames the screens building
 * underneath hold up never show. On Android 12 and later the system's own splash usually stays up
 * instead (MainActivity), and this shows only where there is none.
 *
 * The background is the launch screen's violet, by day or at twilight, and leans toward the accent with
 * Material You colours (UX rule V3a). [trailing] holds the window's controls, where the app draws them.
 */
@Composable
fun LaunchSplash(modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    val twilight = isTwilight()
    val background = launchSplashColor()
    Box(
        modifier.fillMaxSize().background(background)
            // Nothing underneath takes a press while it is up.
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() } },
    ) {
        // Android's sizes for an icon with a background: a 160dp disc, and the icon's 108-unit grid
        // drawn 240dp across, which with the icon's own 0.75 scale puts the mark on a 180dp grid, a
        // little wider than the disc it sits in.
        Box(Modifier.align(Alignment.Center).size(160.dp).clip(CircleShape).background(SplashIconDisc), contentAlignment = Alignment.Center) {
            LockOnMark(Modifier.requiredSize(180.dp), size = 180.dp, reticleColor = Color.White)
        }
        // The window's controls sit on the splash here, so they take an ink that reads on it.
        trailing?.let {
            val ink = if (twilight) Color(0xFFF3EEFF) else Color(0xFF2A1C6B)
            MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(onSurface = ink, onSurfaceVariant = ink)) {
                Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) { it() }
            }
        }
    }
}

/** The launch screen's background in the current theme, for a window's title bar to match while it is up. */
@Composable
fun launchSplashColor(): Color {
    val twilight = isTwilight()
    val seed = LocalThemeSeed.current
    return remember(twilight, seed) { bootSkyStops(twilight, seed)[1] }
}

/** The disc behind the mark: the launcher icon's violet in either theme, as on Android's splash. */
private val SplashIconDisc = LogoTileDay
