package app.lockon.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import androidx.room.Room
import app.lockon.AppContainer
import app.lockon.data.db.LockOnDatabase
import app.lockon.platform.FileBridge
import app.lockon.sync.HostInfo
import app.lockon.sync.HostSyncController
import app.lockon.sync.PairingPrompt
import app.lockon.sync.SyncStatus
import app.lockon.ui.AppState
import app.lockon.ui.LockOnApp
import app.lockon.ui.Tab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test

/**
 * How long a frame of scrolling the full card grid takes, at the user's 2576×1456 window. The
 * scene renders on the CPU, so the numbers compare changes rather than predict on-screen timings.
 * Needs the network for the card list, so it only runs with LOCKON_BENCH=1.
 */
class ScrollBenchmarkTest {
    @Test fun scrollTheCardGrid() = runBlocking {
        if (System.getenv("LOCKON_BENCH") != "1") return@runBlocking
        val container = AppContainer.create(LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>()), "Desktop")
        container.catalogUpdater.install(container.catalogUpdater.latestCardDatabase())
        val sync = object : HostSyncController {
            override val pairedDevices = container.devices.pairedDevices
            override val status = MutableStateFlow(SyncStatus())
            override val hostInfo = MutableStateFlow<HostInfo?>(null)
            override val pairingPrompt = MutableStateFlow<PairingPrompt?>(null)
            override val liveClients = MutableStateFlow(emptySet<String>())
            override val nearby = MutableStateFlow(emptyList<app.lockon.sync.NearbyDevice>())
            override val invited = MutableStateFlow(emptySet<String>())
            override fun answerPairing(requestId: String, accept: Boolean) = Unit
            override fun newPairingCode() = Unit
            override fun selectAddress(address: String) = Unit
            override fun invite(device: app.lockon.sync.NearbyDevice) = Unit
            override fun cancelInvite(deviceId: String) = Unit
            override fun forget(deviceId: String) = Unit
        }
        val files = object : FileBridge {
            override suspend fun saveText(suggestedName: String, mimeType: String, content: String) = false
            override suspend fun saveBytes(suggestedName: String, mimeType: String, content: ByteArray) = false
            override suspend fun openText(mimeTypes: List<String>): String? = null
        }
        val state = AppState(CoroutineScope(Dispatchers.Default)).apply { select(Tab.CARDS) }
        val artwork = File("build/artwork-cache").absolutePath
        // LOCKON_BENCH_SCALE shrinks the pixels but keeps the layout, so drawing costs little and the
        // timings show composition and layout, the main-thread work that drops frames on a GPU.
        val scale = System.getenv("LOCKON_BENCH_SCALE")?.toFloatOrNull() ?: 1f
        ImageComposeScene((2576 * scale).toInt(), (1456 * scale).toInt(), Density(1.25f * scale)) { LockOnApp(container, state, sync, files, artworkDir = artwork, dark = true) }.use { scene ->
            var clock = 0L
            fun frame(): Long {
                clock += 16_666_667L
                val start = System.nanoTime()
                scene.render(clock)
                return System.nanoTime() - start
            }
            // Let the first screen of images arrive and every animation settle.
            repeat(60) { Thread.sleep(50); frame() }
            // LOCKON_JFR names a file to save a Flight Recorder profile of the scrolling to.
            val profile = System.getenv("LOCKON_JFR")?.let {
                jdk.jfr.Recording().apply {
                    enable("jdk.ExecutionSample").withPeriod(java.time.Duration.ofMillis(1))
                    enable("jdk.NativeMethodSample").withPeriod(java.time.Duration.ofMillis(1))
                    start()
                }
            }
            val times = (1..300).map {
                scene.sendPointerEvent(PointerEventType.Scroll, Offset(800f * scale, 800f * scale), scrollDelta = Offset(0f, 1f))
                Thread.sleep(8)
                frame()
            }.map { it / 1_000_000.0 }.sorted()
            profile?.run { stop(); dump(java.nio.file.Path.of(System.getenv("LOCKON_JFR"))); close() }
            val report = "scale $scale scroll frames: mean %.1f ms, median %.1f ms, p90 %.1f ms, worst %.1f ms".format(
                times.average(), times[times.size / 2], times[times.size * 9 / 10], times.last(),
            )
            println(report)
            File("build/scroll-benchmark.txt").appendText(report + "\n")
        }
    }
}
