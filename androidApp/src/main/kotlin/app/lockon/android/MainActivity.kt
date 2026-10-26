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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.lockon.AppContainer
import app.lockon.platform.FileBridge
import app.lockon.ui.AppState
import app.lockon.ui.LockOnApp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var pendingSave: Pair<String, CompletableDeferred<Boolean>>? = null
    private var pendingOpen: CompletableDeferred<String?>? = null
    private var syncController: AndroidSyncController? = null

    private val createDocument = registerForActivityResult(CreateTextDocument()) { uri -> finishSave(uri) }
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> finishOpen(uri) }
    private val scan = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { contents -> syncController?.onScanned(contents) }
    }

    private val files = object : FileBridge {
        override suspend fun saveText(suggestedName: String, mimeType: String, content: String): Boolean {
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
        setContent {
            val container by produceState<AppContainer?>(null) { value = app.container.await() }
            val sync by produceState<AndroidSyncController?>(null) {
                value = app.sync.await().also { it.scanner = ::launchScanner; syncController = it }
            }
            val state = remember { AppState(app.appScope) }
            val c = container
            val s = sync
            if (c == null || s == null) {
                Box(Modifier.fillMaxSize().background(Color(0xFF0B1220)))
            } else {
                BackHandler(enabled = state.canGoBack) { state.goBack() }
                LockOnApp(c, state, s, files)
            }
        }
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
            val ok = runCatching { contentResolver.openOutputStream(uri, "wt")?.use { it.write(content.toByteArray()) } != null }.getOrDefault(false)
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
