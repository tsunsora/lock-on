package app.lockon.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.util.prefs.Preferences

/** Holds an OS file lock in the data folder for as long as the app runs. */
object SingleInstance {
    private var lock: FileLock? = null

    fun acquire(dir: File): Boolean {
        val channel = RandomAccessFile(File(dir, "lock-on.lock"), "rw").channel
        lock = runCatching { channel.tryLock() }.getOrNull()
        if (lock == null) channel.close()
        return lock != null
    }
}

/** Remembers the window's size, position and maximised state between launches. */
object WindowMemory {
    data class Saved(val placement: WindowPlacement, val position: WindowPosition, val size: DpSize)

    private val prefs = Preferences.userRoot().node("app/lockon/window")

    fun load(): Saved {
        val width = prefs.getFloat("width", 1320f).coerceAtLeast(480f)
        val height = prefs.getFloat("height", 860f).coerceAtLeast(480f)
        val x = prefs.getFloat("x", Float.NaN)
        val y = prefs.getFloat("y", Float.NaN)
        return Saved(
            placement = if (prefs.getBoolean("maximized", false)) WindowPlacement.Maximized else WindowPlacement.Floating,
            position = if (x.isNaN() || y.isNaN()) WindowPosition.PlatformDefault else WindowPosition(x.dp, y.dp),
            size = DpSize(width.dp, height.dp),
        )
    }

    fun save(state: WindowState) {
        runCatching {
            prefs.putBoolean("maximized", state.placement == WindowPlacement.Maximized)
            if (state.placement == WindowPlacement.Floating) {
                prefs.putFloat("width", state.size.width.value)
                prefs.putFloat("height", state.size.height.value)
                (state.position as? WindowPosition.Absolute)?.let {
                    prefs.putFloat("x", it.x.value)
                    prefs.putFloat("y", it.y.value)
                }
            }
            prefs.flush()
        }
    }
}
