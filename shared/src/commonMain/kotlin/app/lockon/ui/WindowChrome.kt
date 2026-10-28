package app.lockon.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier

/**
 * A window's title bar, drawn by the app inside the sky instead of by the system (UX rules N5, V9).
 * Desktop passes one to [LockOnApp]; phones pass none, so nothing here reaches Android.
 *
 * - [dragArea] fills the empty part of the top band. It sits under every screen, so it only gets the
 *   presses no button, field or card takes, and it moves the window or, on a double-click, maximises it.
 * - [controls] are the minimise, maximise and close buttons, which float beside the navigation pill.
 */
@Immutable
class WindowChrome(
    val dragArea: @Composable (Modifier) -> Unit,
    val controls: @Composable () -> Unit,
)
