package app.lockon.desktop

import androidx.compose.ui.unit.dp
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

fun main() {
    val database = LockOnDatabase.build(
        Room.databaseBuilder<LockOnDatabase>(name = File(AppDirs.data, LockOnDatabase.FILE_NAME).absolutePath),
    )
    val hostName = runCatching { InetAddress.getLocalHost().hostName }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Desktop"
    val container = runBlocking { AppContainer.create(database, defaultDeviceName = hostName) }
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val sync = DesktopSyncController(container, appScope).also { it.start() }
    val state = AppState(appScope)

    application {
        Window(
            onCloseRequest = { sync.stop(); exitApplication() },
            title = "Lock On",
            state = rememberWindowState(width = 1320.dp, height = 860.dp),
        ) {
            LockOnApp(container, state, sync, DesktopFileBridge(window))
        }
    }
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
