package app.lockon.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.lockon.data.db.MetaDao
import app.lockon.data.db.MetaEntity
import app.lockon.ui.theme.LockOnTheme
import app.lockon.ui.update.AppUpdateCard
import app.lockon.update.AppUpdater
import app.lockon.update.UpdateException
import app.lockon.update.UpdateInstaller
import app.lockon.update.UpdatePlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertTrue

/** Renders the Sync screen's update card in each state to build/screenshots/updates-*.png. */
class UpdateCardScreenshotTest {
    private val out = File("build/screenshots").apply { mkdirs() }

    private fun updater(platform: UpdatePlatform, version: String = "1.1.0", failDownload: Boolean = false, automatic: Boolean = true) = AppUpdater(
        meta = object : MetaDao {
            val values = mutableMapOf<String, String>()
            override suspend fun get(key: String) = values[key]
            override suspend fun put(entry: MetaEntity) { values[entry.key] = entry.value }
        },
        installer = object : UpdateInstaller {
            override val platform = platform
            override val canInstall = true
            override val downloadDir = createTempDirectory("lock-on-updates").toString()
            override suspend fun install(path: String, reopen: Boolean) = Unit
        },
        feedUrl = "https://example.test/lock-on-update.json",
        currentVersion = "1.0.0",
        fetch = {
            """{ "version": "$version", "notes": "Deck folders, faster search, and fixes for pairing.",
                 "windows": { "file": "a.msi", "sha256": "00" }, "android": { "file": "a.apk", "sha256": "00" } }"""
        },
        download = { _, _, _, _ -> if (failDownload) throw UpdateException("The update didn't download correctly. Try again.") },
    ).also { runBlocking { it.load(); it.setAutomatic(automatic); it.checkNow() } }

    @Test fun renderStates() {
        val scope = CoroutineScope(Dispatchers.Default)
        val cards = listOf(
            updater(UpdatePlatform.WINDOWS),
            updater(UpdatePlatform.ANDROID),
            updater(UpdatePlatform.WINDOWS, automatic = false),
            updater(UpdatePlatform.WINDOWS, version = "1.0.0"),
            updater(UpdatePlatform.WINDOWS, failDownload = true),
        )
        for (dark in listOf(true, false)) {
            ImageComposeScene(620, 1500, Density(1.5f)) {
                LockOnTheme(dark) {
                    Column(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) { cards.forEach { AppUpdateCard(it, scope) } }
                }
            }.use { scene ->
                var image = scene.render()
                repeat(5) { frame -> Thread.sleep(40); image = scene.render((frame + 1) * 100_000_000L) }
                File(out, "updates-${if (dark) "dark" else "light"}.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
        assertTrue(File(out, "updates-dark.png").length() > 0)
    }
}
