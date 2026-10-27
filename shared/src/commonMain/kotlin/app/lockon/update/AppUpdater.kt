package app.lockon.update

import app.lockon.BuildInfo
import app.lockon.data.db.MetaDao
import app.lockon.data.db.MetaEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

enum class UpdatePlatform { WINDOWS, ANDROID }

/** How each platform puts a downloaded update in place. */
interface UpdateInstaller {
    val platform: UpdatePlatform
    /** False when this copy can't replace itself, such as a build run from Gradle. */
    val canInstall: Boolean
    /** Folder for downloaded installers. Lock On owns it and clears out old ones. */
    val downloadDir: String
    /** Whether a download nobody asked for is fine right now; phones wait for Wi-Fi. */
    fun backgroundDownloadAllowed(): Boolean = true
    /**
     * Hands a verified installer to the system. On Windows, Lock On closes and the installer runs,
     * then Lock On reopens when [reopen]. On Android, the system installs it, asking first when it
     * needs to.
     */
    suspend fun install(path: String, reopen: Boolean)
}

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val version: String, val notes: String?) : UpdateState
    /** [progress] is 0 to 1, or null when the server didn't say how big the file is. */
    data class Downloading(val version: String, val progress: Float?) : UpdateState
    /** Downloaded and verified. [problem] is set when an install attempt didn't go through. */
    data class Ready(val version: String, val notes: String?, val path: String, val problem: String? = null) : UpdateState
    data class Installing(val version: String) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/**
 * Keeps Lock On itself up to date from the update feed in [BuildInfo.UPDATE_FEED]. While the app is
 * open it checks at most daily. With [automatic] on, a newer version downloads in the background;
 * Windows installs it when Lock On closes, and Android installs with one tap. Background checks
 * never show errors or pop-ups (UX rule C1); they report only on the Sync screen.
 */
class AppUpdater(
    private val meta: MetaDao,
    val installer: UpdateInstaller,
    private val feedUrl: String = BuildInfo.UPDATE_FEED,
    val currentVersion: String = BuildInfo.VERSION,
    private val fetch: suspend (url: String) -> String = ::httpGetText,
    private val download: suspend (url: String, dest: String, sha256: String, onProgress: (Float?) -> Unit) -> Unit = ::downloadVerified,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    private val _automatic = MutableStateFlow(true)
    private val _checkedAt = MutableStateFlow<Long?>(null)
    private val busy = Mutex()
    private var latest: UpdateManifest? = null
    private var loaded = false
    private var background: Job? = null

    val state: StateFlow<UpdateState> = _state.asStateFlow()
    /** Download and install new versions without being asked. On unless turned off. */
    val automatic: StateFlow<Boolean> = _automatic.asStateFlow()
    val lastCheckedAt: StateFlow<Long?> = _checkedAt.asStateFlow()

    /** False when this build has no update feed, so there is nothing to check. */
    val enabled: Boolean get() = feedUrl.startsWith("https://")

    /** Loads settings and any update downloaded earlier, then checks at most daily while [scope] lives. */
    fun start(scope: CoroutineScope) {
        if (background != null) return
        background = scope.launch {
            load()
            while (enabled) {
                backgroundCheck()
                delay(HOUR_MS)
            }
        }
    }

    suspend fun load() {
        if (loaded) return
        loaded = true
        _automatic.value = meta.get(KEY_AUTOMATIC)?.toBooleanStrictOrNull() ?: true
        _checkedAt.value = meta.get(KEY_CHECKED_AT)?.toLongOrNull()
        val stored = meta.get(KEY_READY)?.let { runCatching { json.decodeFromString(StoredDownload.serializer(), it) }.getOrNull() }
        val usable = stored != null && compareVersions(stored.version, currentVersion) > 0 && SystemFileSystem.exists(Path(stored.path))
        if (usable) {
            _state.value = UpdateState.Ready(stored.version, stored.notes, stored.path)
        } else if (stored != null) {
            meta.put(MetaEntity(KEY_READY, ""))
        }
        // Installers from updates already applied, or abandoned downloads.
        deleteDownloadsExcept(if (usable) stored.path else null)
    }

    suspend fun setAutomatic(on: Boolean) {
        meta.put(MetaEntity(KEY_AUTOMATIC, on.toString()))
        _automatic.value = on
    }

    /** A check the person asked for: errors are shown, and with [automatic] on the update downloads right away. */
    suspend fun checkNow() {
        if (!enabled || !busy.tryLock()) return
        try {
            _state.value = UpdateState.Checking
            val manifest = try {
                fetchManifest()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = UpdateState.Failed((e as? UpdateException)?.message ?: "Couldn't check for updates. Check your internet connection.")
                return
            }
            val file = newerFile(manifest)
            if (file == null) {
                _state.value = UpdateState.UpToDate
                return
            }
            _state.value = UpdateState.Available(manifest.version, manifest.notes)
            if (_automatic.value && installer.canInstall) downloadLocked(manifest, file, quiet = false)
        } finally {
            busy.unlock()
        }
    }

    /** Downloads the newer version found by the last check. */
    suspend fun downloadNow() {
        val manifest = latest ?: return
        val file = newerFile(manifest) ?: return
        if (!installer.canInstall || !busy.tryLock()) return
        try {
            downloadLocked(manifest, file, quiet = false)
        } finally {
            busy.unlock()
        }
    }

    /** Installs the downloaded update now. On Windows, Lock On closes and reopens on the new version. */
    suspend fun installNow() {
        val ready = _state.value as? UpdateState.Ready ?: return
        if (!installer.canInstall) return
        _state.value = UpdateState.Installing(ready.version)
        try {
            installer.install(ready.path, reopen = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            installProblem("The installer didn't start. Try again, or install it from the release page.")
        }
    }

    /**
     * Called as the desktop app exits: with [automatic] on, a downloaded update installs now, so the
     * next launch is the new version. Returns whether it started.
     */
    suspend fun installOnExit(): Boolean {
        val ready = _state.value as? UpdateState.Ready ?: return false
        if (!_automatic.value || !installer.canInstall || installer.platform != UpdatePlatform.WINDOWS) return false
        return runCatching { installer.install(ready.path, reopen = false) }.isSuccess
    }

    /** The system reported that an install didn't go through, e.g. the person declined it. */
    fun installProblem(message: String) {
        val current = _state.value
        val stored = (current as? UpdateState.Ready) ?: (current as? UpdateState.Installing)?.let { installing ->
            latest?.let { m -> readyFrom(m, installing.version) }
        }
        _state.value = stored?.copy(problem = message) ?: UpdateState.Failed(message)
    }

    /** At most daily, and silent: a failure leaves the screen as it was. */
    internal suspend fun backgroundCheck() {
        if (!enabled) return
        when (_state.value) {
            is UpdateState.Ready, is UpdateState.Downloading, is UpdateState.Installing -> return
            else -> Unit
        }
        if (now() - (_checkedAt.value ?: 0) < DAY_MS) return
        if (!busy.tryLock()) return
        try {
            val manifest = try {
                fetchManifest()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return // The next round tries again.
            }
            val file = newerFile(manifest)
            if (file == null) {
                _state.value = UpdateState.UpToDate
                return
            }
            _state.value = UpdateState.Available(manifest.version, manifest.notes)
            if (_automatic.value && installer.canInstall && installer.backgroundDownloadAllowed()) {
                downloadLocked(manifest, file, quiet = true)
            }
        } finally {
            busy.unlock()
        }
    }

    private suspend fun fetchManifest(): UpdateManifest {
        val text = try {
            fetch(feedUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: UpdateException) {
            throw e
        } catch (e: Exception) {
            throw UpdateException("Couldn't reach the update server. Check your internet connection.")
        }
        val manifest = try {
            UpdateManifest.parse(text)
        } catch (e: Exception) {
            throw UpdateException("The update server's version file wasn't readable. Try again later.")
        }
        latest = manifest
        _checkedAt.value = now()
        meta.put(MetaEntity(KEY_CHECKED_AT, now().toString()))
        return manifest
    }

    /** The installer for this platform when [manifest] is newer than this app, else null. */
    private fun newerFile(manifest: UpdateManifest): UpdateFile? =
        manifest.fileFor(installer.platform)?.takeIf { compareVersions(manifest.version, currentVersion) > 0 }

    private suspend fun downloadLocked(manifest: UpdateManifest, file: UpdateFile, quiet: Boolean) {
        val url = resolveUpdateUrl(feedUrl, file.file)
        val ready = readyFrom(manifest, manifest.version)
        if (ready == null || !url.startsWith("https://")) {
            if (!quiet) _state.value = UpdateState.Failed("This update can't be downloaded safely. Install it from the release page.")
            return
        }
        var shown = -1
        _state.value = UpdateState.Downloading(manifest.version, null)
        try {
            download(url, ready.path, file.sha256) { progress ->
                // A step per percent is plenty for a progress bar.
                val percent = progress?.let { (it * 100).toInt() } ?: -1
                if (percent != shown) {
                    shown = percent
                    _state.value = UpdateState.Downloading(manifest.version, progress)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = if (quiet) UpdateState.Available(manifest.version, manifest.notes)
            else UpdateState.Failed((e as? UpdateException)?.message ?: "The download stopped. Check your connection and try again.")
            return
        }
        meta.put(MetaEntity(KEY_READY, json.encodeToString(StoredDownload.serializer(), StoredDownload(ready.version, ready.notes, ready.path))))
        deleteDownloadsExcept(ready.path)
        _state.value = ready
    }

    /** Where [manifest]'s installer for this platform is saved, named after the file it came from. */
    private fun readyFrom(manifest: UpdateManifest, version: String): UpdateState.Ready? {
        val name = manifest.fileFor(installer.platform)?.file?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() && it.all { c -> c.isLetterOrDigit() || c in "._-" } } ?: return null
        return UpdateState.Ready(version, manifest.notes, Path(installer.downloadDir, name).toString())
    }

    private fun deleteDownloadsExcept(keep: String?) {
        runCatching {
            val dir = Path(installer.downloadDir)
            if (!SystemFileSystem.exists(dir)) return
            SystemFileSystem.list(dir).filter { it.toString() != keep }.forEach { SystemFileSystem.delete(it, mustExist = false) }
        }
    }

    @Serializable
    private data class StoredDownload(val version: String, val notes: String?, val path: String)

    private companion object {
        const val HOUR_MS = 60L * 60 * 1000
        const val DAY_MS = 24 * HOUR_MS
        const val KEY_AUTOMATIC = "update.automatic"
        const val KEY_CHECKED_AT = "update.checkedAt"
        const val KEY_READY = "update.ready"
    }
}
