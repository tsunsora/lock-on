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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.lockon.AppContainer
import app.lockon.platform.FileBridge
import app.lockon.ui.AppState
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

class MainActivity : ComponentActivity() {
    private var pendingSave: Pair<ByteArray, CompletableDeferred<Boolean>>? = null
    private var pendingOpen: CompletableDeferred<String?>? = null
    private var syncController: AndroidSyncController? = null

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
                AppState(app.appScope).also { s -> if (!reduceMotion) s.bootCover = AndroidBootCover(this@MainActivity, s.bootClock) }
            }
            val c = container
            val s = sync
            if (c == null || s == null) {
                // The boot screen starts while the decks open, in the phone's own light or dark until
                // Settings can say which; the app's boot screen then carries on from the same moment.
                val covered by (state.bootCover?.showing ?: remember { MutableStateFlow(false) }).collectAsState()
                CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
                    LockOnTheme(dark = isSystemInDarkTheme()) {
                        state.bootCover?.let { cover -> BootPictureTaker(state.bootClock, cover::pictures) }
                        BootScreen(BootStep.DECKS, state.bootClock, playing = !covered)
                    }
                }
            } else {
                BackHandler(enabled = state.canGoBack) { state.goBack() }
                LockOnApp(
                    c, state, s, files,
                    artworkDir = filesDir.resolve("artwork").absolutePath,
                    reduceMotion = reduceMotion,
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
