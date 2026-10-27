package app.lockon

import app.lockon.data.db.MetaDao
import app.lockon.data.db.MetaEntity
import app.lockon.update.AppUpdater
import app.lockon.update.UpdateException
import app.lockon.update.UpdateInstaller
import app.lockon.update.UpdatePlatform
import app.lockon.update.UpdateState
import app.lockon.update.compareVersions
import app.lockon.update.resolveUpdateUrl
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlinx.io.files.SystemTemporaryDirectory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppUpdaterTest {
    private val feed = "https://example.test/releases/latest/download/lock-on-update.json"
    private val meta = object : MetaDao {
        val values = mutableMapOf<String, String>()
        override suspend fun get(key: String) = values[key]
        override suspend fun put(entry: MetaEntity) { values[entry.key] = entry.value }
    }
    private val installs = mutableListOf<Pair<String, Boolean>>()
    private fun installer(platform: UpdatePlatform) = object : UpdateInstaller {
        override val platform = platform
        override val canInstall = true
        override val downloadDir = Path(SystemTemporaryDirectory, "lock-on-test-${Random.nextLong()}").toString()
        override suspend fun install(path: String, reopen: Boolean) { installs += path to reopen }
    }

    private var manifest = manifest("1.1.0")
    private var fetchFails = false
    private var downloadFails = false
    private val downloads = mutableListOf<String>()
    private var clock = 10L * 24 * 60 * 60 * 1000

    private fun manifest(version: String, file: String = "LockOn-$version-windows.msi") = """
        { "version": "$version", "notes": "Faster search.",
          "windows": { "file": "$file", "sha256": "ab", "size": 3 },
          "android": { "file": "LockOn-$version-android.apk", "sha256": "cd" } }
    """

    private fun updater(platform: UpdatePlatform = UpdatePlatform.WINDOWS) = AppUpdater(
        meta = meta,
        installer = installer(platform),
        feedUrl = feed,
        currentVersion = "1.0.0",
        fetch = { if (fetchFails) error("offline") else manifest },
        download = { url, _, _, onProgress ->
            onProgress(0.5f)
            if (downloadFails) throw UpdateException("The update didn't download correctly. Try again.")
            downloads += url
        },
        now = { clock },
    )

    @Test
    fun versionsCompareAsNumbers() {
        assertTrue(compareVersions("1.2.10", "1.2.9") > 0)
        assertTrue(compareVersions("1.0", "1.0.0") == 0)
        assertTrue(compareVersions("v2.0.0", "1.9.9") > 0)
        assertTrue(compareVersions("1.1.0-beta", "1.1.0") == 0)
        assertTrue(compareVersions("1.0.0", "1.0.1") < 0)
    }

    @Test
    fun filesResolveNextToTheFeed() {
        assertEquals("https://example.test/releases/latest/download/a.msi", resolveUpdateUrl(feed, "a.msi"))
        assertEquals("https://cdn.test/a.msi", resolveUpdateUrl(feed, "https://cdn.test/a.msi"))
    }

    @Test
    fun aNewerVersionDownloadsAndInstallsOnExit() = runTest {
        val updater = updater()
        updater.load()
        updater.checkNow()
        val ready = assertIs<UpdateState.Ready>(updater.state.value)
        assertEquals("1.1.0", ready.version)
        assertEquals("Faster search.", ready.notes)
        assertTrue(ready.path.endsWith("LockOn-1.1.0-windows.msi"))
        assertEquals(listOf("https://example.test/releases/latest/download/LockOn-1.1.0-windows.msi"), downloads)
        assertTrue(installs.isEmpty(), "nothing installs until Lock On closes")

        assertTrue(updater.installOnExit())
        assertEquals(listOf(ready.path to false), installs)
    }

    @Test
    fun turningAutomaticOffOnlyAnnouncesTheUpdate() = runTest {
        val updater = updater()
        updater.load()
        updater.setAutomatic(false)
        updater.checkNow()
        assertIs<UpdateState.Available>(updater.state.value)
        assertTrue(downloads.isEmpty())

        updater.downloadNow()
        assertIs<UpdateState.Ready>(updater.state.value)
        assertFalse(updater.installOnExit(), "with automatic off, closing doesn't install")
        updater.installNow()
        assertEquals(true, installs.single().second, "installing now reopens Lock On")
    }

    @Test
    fun sameVersionIsUpToDate() = runTest {
        manifest = manifest("1.0.0")
        val updater = updater()
        updater.checkNow()
        assertEquals(UpdateState.UpToDate, updater.state.value)
        assertTrue(downloads.isEmpty())
    }

    @Test
    fun backgroundChecksAreQuietAndAtMostDaily() = runTest {
        val updater = updater()
        updater.load()
        fetchFails = true
        updater.backgroundCheck()
        assertEquals(UpdateState.Idle, updater.state.value, "a failed background check shows nothing")

        fetchFails = false
        updater.backgroundCheck()
        assertIs<UpdateState.Ready>(updater.state.value)
        assertEquals(1, downloads.size)

        // Checked moments ago, so a fresh updater on the same settings waits a day.
        val next = updater()
        next.load()
        next.backgroundCheck()
        assertEquals(1, downloads.size)
    }

    @Test
    fun aFailedDownloadIsShownOnlyWhenAskedFor() = runTest {
        downloadFails = true
        val quiet = updater()
        quiet.load()
        quiet.backgroundCheck()
        assertIs<UpdateState.Available>(quiet.state.value)

        val asked = updater()
        asked.checkNow()
        assertEquals("The update didn't download correctly. Try again.", assertIs<UpdateState.Failed>(asked.state.value).message)
    }

    @Test
    fun unsafeFileNamesAreRefused() = runTest {
        manifest = manifest("1.1.0", file = "..\\\\evil.msi")
        val updater = updater()
        updater.checkNow()
        assertIs<UpdateState.Failed>(updater.state.value)
        assertTrue(downloads.isEmpty())
    }

    @Test
    fun phonesNeverInstallOnExit() = runTest {
        val updater = updater(UpdatePlatform.ANDROID)
        updater.checkNow()
        assertTrue(assertIs<UpdateState.Ready>(updater.state.value).path.endsWith("LockOn-1.1.0-android.apk"))
        assertFalse(updater.installOnExit())
        updater.installNow()
        assertEquals(1, installs.size)
        updater.installProblem("The update wasn't installed. Tap Install update to try again.")
        assertEquals("The update wasn't installed. Tap Install update to try again.", assertIs<UpdateState.Ready>(updater.state.value).problem)
    }
}
