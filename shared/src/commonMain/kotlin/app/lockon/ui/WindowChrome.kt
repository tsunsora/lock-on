package app.lockon.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * A window's title bar, drawn by the app inside the sky instead of by the system (UX rules N5, V9).
 * Desktop passes one to [LockOnApp]; phones pass none, so nothing here reaches Android.
 *
 * - [dragArea] goes on the app's root and watches the top band, [height] tall. It acts only on the
 *   presses no button, field or card takes, and moves the window or, on a double-click, maximises it.
 *   A parent rather than a layer under the screens: a layer under them never hears a press there.
 * - [controls] are the minimise, maximise and close buttons, which float beside the navigation pill.
 */
@Immutable
class WindowChrome(
    val dragArea: Modifier.(height: Dp) -> Modifier,
    val controls: @Composable () -> Unit,
)
