package app.lockon.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import app.lockon.data.MotionChoice
import app.lockon.data.ThemeChoice
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.lockon.AppContainer
import app.lockon.platform.FileBridge
import app.lockon.ui.AppState
import android.provider.MediaStore
import android.provider.Settings
import app.lockon.ui.LockOnApp
import app.lockon.ui.BootPictureTaker
import app.lockon.ui.BootScreen
import app.lockon.ui.BootStep
import app.lockon.ui.components.LocalReduceMotion
import app.lockon.ui.theme.LockOnTheme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var pendingSave: Pair<ByteArray, CompletableDeferred<Boolean>>? = null
    private var pendingOpen: CompletableDeferred<String?>? = null
    private var syncController: AndroidSyncController? = null
    private var bootCover: AndroidBootCover? = null

    private val createDocument = registerForActivityResult(CreateTextDocument()) { uri -> finishSave(uri) }
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> finishOpen(uri) }
    private val scan = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { contents -> syncController?.onScanned(contents) }
    }

    private val files = object : FileBridge {
        override suspend fun saveText(suggestedName: String, mimeType: String, content: String): Boolean =
            saveBytes(suggestedName, mimeType, content.toByteArray())

        override suspend fun saveBytes(suggestedName: String, mimeType: String, content: ByteArray): Boolean {
            val result = CompletableDeferred<Boolean>()
            pendingSave = content to result
            createDocument.launch(suggestedName to mimeType)
            return result.await()
        }

        override suspend fun saveToDownloads(name: String, mimeType: String, content: ByteArray): Boolean {
            // Before Android 10 this would need a storage permission, so those phones keep the picker.
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return false
            return withContext(Dispatchers.IO) {
                val details = android.content.ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, details) ?: return@withContext false
                try {
                    contentResolver.openOutputStream(uri)!!.use { it.write(content) }
                    contentResolver.update(uri, android.content.ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
                    true
                } catch (_: java.io.IOException) {
                    contentResolver.delete(uri, null, null)
                    false
                }
            }
        }

        override suspend fun openText(mimeTypes: List<String>): String? {
            val result = CompletableDeferred<String?>()
            pendingOpen = result
            openDocument.launch(arrayOf("text/*", "application/json", "application/octet-stream"))
            return result.await()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as LockOnApplication
        // Card images used to sit in the cache folder; they now live in files for offline use.
        kotlin.concurrent.thread(isDaemon = true) { cacheDir.resolve("artwork").deleteRecursively() }
        setContent {
            // Leaving for another app lets go of the search field, or coming back would bring the keyboard up again.
            val focusManager = LocalFocusManager.current
            DisposableEffect(focusManager) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) focusManager.clearFocus(force = true)
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }
            val container by produceState<AppContainer?>(null) { value = app.container.await() }
            val sync by produceState<AndroidSyncController?>(null) {
                value = app.sync.await().also {
                    it.scanner = ::launchScanner
                    syncController = it
                    // The controller may arrive after onStart on a cold launch.
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) it.setForeground(true)
                }
            }
            val reduceMotion = remember { Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
            // The boot screen plays on a surface of its own, so the screens building under it can't make it
            // stutter. Reduced motion holds it still, so then there is nothing to play.
            val state = remember {
                AppState(app.appScope).also { s ->
                    if (!reduceMotion) s.bootCover = AndroidBootCover(this@MainActivity, s.bootClock).also { bootCover = it }
                }
            }
            val c = container
            val s = sync
            val coverSettled by (bootCover?.settled ?: remember { MutableStateFlow(true) }).collectAsState()
            // Should the cover take too long, the app starts anyway and its own boot screen plays.
            var coverLate by remember { mutableStateOf(false) }
            LaunchedEffect(c != null) { if (c != null) { delay(3_000); coverLate = true } }
            if (c == null || s == null || !(coverSettled || coverLate)) {
                // Until the cover is up, the window holds the boot screen's first frame, which the cover's
                // matches. Taking its pictures holds this thread up, so nothing here moves yet, and the app
                // starts building only once the cover plays over it.
                val choice = c?.settings
                val systemDark = isSystemInDarkTheme()
                val dark = when (choice?.theme?.value) {
                    ThemeChoice.LIGHT -> false
                    ThemeChoice.DARK -> true
                    else -> systemDark
                }
                val base = LocalDensity.current
                val density = Density(base.density * (choice?.size?.value?.scale ?: 1f), base.fontScale)
                val reduce = reduceMotion || choice?.motion?.value == MotionChoice.REDUCED
                CompositionLocalProvider(LocalReduceMotion provides reduce, LocalDensity provides density) {
                    LockOnTheme(dark = dark) {
                        // Once Settings can say which theme and size, so they are taken only once, and right.
                        val cover = bootCover
                        if (choice != null && cover != null) {
                            if (reduce) LaunchedEffect(Unit) { cover.lift() }
                            else BootPictureTaker(state.bootClock, cover::pictures)
                        }
                        BootScreen(BootStep.DECKS, state.bootClock, playing = false)
                    }
                }
            } else {
                BackHandler(enabled = state.canGoBack) { state.goBack() }
                LockOnApp(
                    c, state, s, files,
                    artworkDir = filesDir.resolve("artwork").absolutePath,
                    reduceMotion = reduceMotion,
                    // From here the window behind the app is the app's colour, not the launch screen's.
                    windowBackground = { window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(it.toArgb())) },
                )
            }
        }
    }

    // Discovery and auto-sync run only while Lock On is on screen.
    override fun onStart() {
        super.onStart()
        syncController?.setForeground(true)
    }

    override fun onStop() {
        syncController?.setForeground(false)
        super.onStop()
    }

    override fun onDestroy() {
        bootCover?.close()
        syncController?.scanner = null
        super.onDestroy()
    }

    private fun launchScanner() {
        scan.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Scan the code shown in Lock On on your desktop")
                .setBeepEnabled(false)
                .setOrientationLocked(false),
        )
    }

    private fun finishSave(uri: Uri?) {
        val (content, result) = pendingSave ?: return
        pendingSave = null
        if (uri == null) { result.complete(false); return }
        (application as LockOnApplication).appScope.launch(Dispatchers.IO) {
            val ok = runCatching { contentResolver.openOutputStream(uri, "wt")?.use { it.write(content) } != null }.getOrDefault(false)
            result.complete(ok)
        }
    }

    private fun finishOpen(uri: Uri?) {
        val result = pendingOpen ?: return
        pendingOpen = null
        if (uri == null) { result.complete(null); return }
        (application as LockOnApplication).appScope.launch(Dispatchers.IO) {
            result.complete(runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull())
        }
    }
}

/** CreateDocument with the MIME type chosen per call rather than at registration. */
private class CreateTextDocument : ActivityResultContract<Pair<String, String>, Uri?>() {
    override fun createIntent(context: Context, input: Pair<String, String>) =
        Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.second).putExtra(Intent.EXTRA_TITLE, input.first)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}
