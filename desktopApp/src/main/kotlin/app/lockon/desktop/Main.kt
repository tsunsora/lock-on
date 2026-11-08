package app.lockon.desktop

import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.unit.DpSize
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.SkiaLayer
import java.awt.Component
import java.awt.Container
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.InetAddress
import javax.swing.JOptionPane
import kotlin.system.exitProcess

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun main() {
    // Windows then fills the part of the window a resize uncovers with its background colour, not black.
    System.setProperty("skiko.rendering.noerasebackground", "false")
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
    // Card images used to sit in a cache; they now live in "artwork" for offline use.
    kotlin.concurrent.thread(isDaemon = true) { File(AppDirs.data, "artwork-cache").deleteRecursively() }
    val hostName = runCatching { InetAddress.getLocalHost().hostName }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Desktop"
    // Installing an update now closes the window the normal way, so it is saved first.
    var closeWindow: () -> Unit = {}
    val updates = WindowsUpdateInstaller(requestExit = { closeWindow() })
    BootLog.mark("database")
    val container = runBlocking { AppContainer.create(database, defaultDeviceName = hostName, updateInstaller = updates) }
    BootLog.mark("container")
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    // Packaging runs the app once to record the classes it starts with (ClassArchiveTraining.kt). That run
    // stays off the network and leaves the window's remembered place alone.
    val training = System.getProperty("lockon.classArchiveTraining") == "true"
    val sync = DesktopSyncController(container, appScope).also { if (!training) it.start() }
    if (!training) container.updater?.start(appScope)
    val state = AppState(appScope)
    // For timing the app without the boot screen (TabSwitchProbe.kt).
    if (System.getProperty("lockon.skipBoot") == "true") state.booted = true
    // On Windows the boot screen plays in a window of its own, so the screens building under it can't
    // make it stutter. Reduced motion holds the boot screen still, so it has nothing to play there.
    val bootCover = if (!training && !state.booted && System.getProperty("os.name").startsWith("Windows") &&
        container.settings.motion.value != app.lockon.data.MotionChoice.REDUCED
    ) DesktopBootCover(container.settings).also { state.bootCover = it } else null

    // `-Plockon.probeSize=<w>x<h>` (dp) opens the window at that size, so probes can time a wide window.
    val saved = WindowMemory.load().let { saved ->
        System.getProperty("lockon.probeSize")?.split("x")?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 2 }
            ?.let { (w, h) -> saved.copy(placement = WindowPlacement.Floating, size = DpSize(w.dp, h.dp)) } ?: saved
    }
    application {
        val windowState = rememberWindowState(
            placement = saved.placement,
            position = saved.position,
            size = saved.size,
        )
        // The window closes at once; the sync server and LAN announcement shut down after it.
        // Probe runs leave it alone too, since they move and resize the window to time it.
        val probing = System.getProperties().stringPropertyNames().any { it.startsWith("lockon.") && it.endsWith("Probe") }
        val exit = { if (!training && !probing) WindowMemory.save(windowState); exitApplication() }
        closeWindow = exit
        Window(
            onCloseRequest = exit,
            title = "Lock On",
            icon = painterResource("lock-on-window.png"),
            state = windowState,
        ) {
            // While the window grows, Direct3D shows white where it hasn't drawn yet. Drawn through
            // DirectComposition instead, that strip shows the window's background (windowBackground below).
            // The app paints an opaque sky over the whole window, so nothing else shows through.
            remember { components(window).filterIsInstance<SkiaLayer>().firstOrNull()?.transparency = true }
            remember { bootCover?.attach(window) }
            // Which graphics API draws the window, for reports of slow scrolling: SOFTWARE means no GPU.
            LaunchedEffect(Unit) {
                org.slf4j.LoggerFactory.getLogger("LockOn").info("Rendering with ${window.renderApi}")
                BootLog.mark("window")
                snapshotFlow { state.booted }.first { it }
                BootLog.mark("booted")
            }
            // On Windows the app draws its own title bar in the sky's top band (UX rule N5). Elsewhere, or
            // if Windows refuses, the system's title bar stays and so do its controls.
            var ownTitleBar by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                while (!window.isDisplayable) delay(16)
                ownTitleBar = WindowsFrame.install(window)
                // The window opens floating even when told to open maximised (its state still claims
                // maximised, then gives in), so it is maximised once it shows.
                if (saved.placement == WindowPlacement.Maximized) window.extendedState = Frame.MAXIMIZED_BOTH
            }
            val chrome = if (ownTitleBar) rememberWindowChrome(window, windowState, exit) else null
            // Until the boot cover is up, the window shows only the boot screen's first frame, which the
            // cover's matches. The cover takes its pictures on this thread, where they must wait for
            // nothing else to be composing, so the app starts building only once the cover plays over it.
            var coverSettled by remember { mutableStateOf(bootCover == null) }
            if (bootCover != null) LaunchedEffect(Unit) {
                withTimeoutOrNull(3_000) { bootCover.settled.first { it } }
                coverSettled = true
            }
            val textContextMenu = remember { AppTextContextMenu(window) }
            CompositionLocalProvider(LocalContextMenuRepresentation provides AppContextMenu, LocalTextContextMenu provides textContextMenu) {
                Box(Modifier.fillMaxSize()) {
                    if (!coverSettled) BootFirstFrame(container, bootCover?.clock ?: state.bootClock) { bootCover?.windowSkyPainted() } else LockOnApp(
                        container, state, sync, DesktopFileBridge(window),
                        artworkDir = File(AppDirs.data, "artwork").absolutePath, chrome = chrome,
                        // Any part of the window the app hasn't drawn yet, such as while it maximises, shows the
                        // theme's sky colour rather than white.
                        windowBackground = { color ->
                            val awt = java.awt.Color(color.toArgb(), true)
                            // Every layer, down to the one Direct3D draws on, so a resize uncovers this colour too.
                            components(window).forEach { it.background = awt }
                        },
                    )
                    if (ownTitleBar && windowState.placement != WindowPlacement.Maximized) TopResizeEdge(window)
                }
            }
            System.getProperty("lockon.scrollProbe")?.let { ScrollProbe(window, state, File(it), exit) }
            System.getProperty("lockon.deckOpenProbe")?.let { DeckOpenProbe(window, container, state, File(it), exit) }
            System.getProperty("lockon.tabProbe")?.let { TabSwitchProbe(window, container, state, File(it), exit) }
            System.getProperty("lockon.dividerProbe")?.let { DividerProbe(window, container, state, File(it), exit) }
            System.getProperty("lockon.bootProbe")?.let { BootProbe(window, state, bootCover, File(it), exit) }
            if (training) ClassArchiveTraining(state, exit)
        }
    }
    sync.stop()
    // With automatic updates on, a downloaded update installs as Lock On closes.
    container.updater?.let { runBlocking { it.installOnExit() } }
    exitProcess(0)
}

/**
 * The boot screen standing at its first frame, in the theme and size Settings chose, as the window
 * shows it while the boot cover gets ready. It doesn't move: the cover starts moving it.
 */
@Composable
private fun BootFirstFrame(container: AppContainer, clock: app.lockon.ui.BootClock, onSkyPainted: () -> Unit) {
    val settings = container.settings
    val dark = when (settings.theme.value) {
        app.lockon.data.ThemeChoice.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        app.lockon.data.ThemeChoice.LIGHT -> false
        app.lockon.data.ThemeChoice.DARK -> true
    }
    val base = androidx.compose.ui.platform.LocalDensity.current
    val density = androidx.compose.ui.unit.Density(base.density * settings.size.value.scale, base.fontScale)
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides density) {
        app.lockon.ui.theme.LockOnTheme(dark) { app.lockon.ui.BootScreen(app.lockon.ui.BootStep.DECKS, clock, playing = false, onSkyPainted = onSkyPainted) }
    }
}

/** [root] and everything inside it, parents before children. */
private fun components(root: Component): Sequence<Component> = sequence {
    yield(root)
    if (root is Container) root.components.forEach { yieldAll(components(it)) }
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

    override suspend fun saveBytes(suggestedName: String, mimeType: String, content: ByteArray): Boolean {
        val file = withContext(Dispatchers.Main) {
            FileDialog(owner, "Save", FileDialog.SAVE).run {
                file = suggestedName
                isVisible = true
                if (file == null) null else File(directory, file)
            }
        } ?: return false
        withContext(Dispatchers.IO) { file.writeBytes(content) }
        return true
    }

    override suspend fun saveToDownloads(name: String, mimeType: String, content: ByteArray): Boolean =
        withContext(Dispatchers.IO) {
            try {
                writeBesideOthers(downloadsFolder(), name, content)
                true
            } catch (_: java.io.IOException) {
                false
            }
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
