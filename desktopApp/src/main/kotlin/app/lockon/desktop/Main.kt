package app.lockon.desktop

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.room.Room
import app.lockon.AppContainer
import app.lockon.data.db.LockOnDatabase
import app.lockon.platform.FileBridge
import app.lockon.ui.AppState
import app.lockon.ui.LockOnApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.InetAddress
import javax.swing.JOptionPane
import kotlin.system.exitProcess

fun main() {
    // Before anything logs: send logs to a file people can attach to a bug report.
    System.setProperty("org.slf4j.simpleLogger.logFile", File(AppDirs.data, "lock-on.log").absolutePath)
    System.setProperty("org.slf4j.simpleLogger.showDateTime", "true")
    // Two copies would share one database and fight over the sync port.
    if (!SingleInstance.acquire(AppDirs.data)) {
        JOptionPane.showMessageDialog(null, "Lock On is already open.", "Lock On", JOptionPane.INFORMATION_MESSAGE)
        return
    }
    val database = LockOnDatabase.build(
        Room.databaseBuilder<LockOnDatabase>(name = File(AppDirs.data, LockOnDatabase.FILE_NAME).absolutePath),
    )
    val hostName = runCatching { InetAddress.getLocalHost().hostName }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Desktop"
    val container = runBlocking { AppContainer.create(database, defaultDeviceName = hostName) }
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val sync = DesktopSyncController(container, appScope).also { it.start() }
    val state = AppState(appScope)

    val saved = WindowMemory.load()
    application {
        val windowState = rememberWindowState(
            placement = saved.placement,
            position = saved.position,
            size = saved.size,
        )
        Window(
            // The window closes at once; the sync server and LAN announcement shut down after it.
            onCloseRequest = { WindowMemory.save(windowState); exitApplication() },
            title = "Lock On",
            icon = painterResource("lock-on-window.png"),
            state = windowState,
        ) {
            LockOnApp(container, state, sync, DesktopFileBridge(window), imageCacheDir = File(AppDirs.data, "artwork-cache").absolutePath)
        }
    }
    sync.stop()
    exitProcess(0)
}

private class DesktopFileBridge(private val owner: Frame) : FileBridge {
    override suspend fun saveText(suggestedName: String, mimeType: String, content: String): Boolean {
        val file = withContext(Dispatchers.Main) {
            FileDialog(owner, "Save", FileDialog.SAVE).run {
                file = suggestedName
                isVisible = true
                if (file == null) null else File(directory, file)
            }
        } ?: return false
        withContext(Dispatchers.IO) { file.writeText(content) }
        return true
    }

    override suspend fun openText(mimeTypes: List<String>): String? {
        val file = withContext(Dispatchers.Main) {
            FileDialog(owner, "Open", FileDialog.LOAD).run {
                isVisible = true
                if (file == null) null else File(directory, file)
            }
        } ?: return null
        return withContext(Dispatchers.IO) { file.readText() }
    }
}
