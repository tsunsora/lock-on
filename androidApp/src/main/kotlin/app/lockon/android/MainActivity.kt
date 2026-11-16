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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import app.lockon.data.MotionChoice
import app.lockon.data.ThemeChoice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
import app.lockon.ui.LaunchSplash
import app.lockon.ui.LockOnApp
import app.lockon.ui.theme.LockOnTheme
import app.lockon.ui.theme.themeLook
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var pendingSave: Pair<ByteArray, CompletableDeferred<Boolean>>? = null
    private var pendingOpen: CompletableDeferred<String?>? = null
    private var syncController: AndroidSyncController? = null
    private lateinit var state: AppState
    /** The system's splash screen, held up over the app until it has booted (Android 12 and later). */
    private var heldSplash: android.view.View? = null
    private var systemReduceMotion = false
    private var appContainer: AppContainer? = null

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
        readSystemAccent()
        // Card images used to sit in the cache folder; they now live in files for offline use.
        kotlin.concurrent.thread(isDaemon = true) { cacheDir.resolve("artwork").deleteRecursively() }
        systemReduceMotion = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        state = AppState(app.appScope)
        if (android.os.Build.VERSION.SDK_INT >= 31) holdSystemSplash()
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
            val container by produceState<AppContainer?>(null) { value = app.container.await().also { appContainer = it } }
            val sync by produceState<AndroidSyncController?>(null) {
                value = app.sync.await().also {
                    it.scanner = ::launchScanner
                    syncController = it
                    // The controller may arrive after onStart on a cold launch.
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) it.setForeground(true)
                }
            }
            val c = container
            val s = sync
            // The system's splash comes down once the screens underneath have filled in.
            LaunchedEffect(state.booted) { if (state.booted) releaseSystemSplash() }
            if (c == null || s == null) {
                // Until the app can start, its own launch screen, in the theme and size Settings chose once
                // they are in. The system's splash is usually up over it anyway.
                val choice = c?.settings
                val systemDark = isSystemInDarkTheme()
                val dark = when (choice?.theme?.value) {
                    ThemeChoice.LIGHT -> false
                    ThemeChoice.DARK -> true
                    else -> systemDark
                }
                val base = LocalDensity.current
                val density = Density(base.density * (choice?.size?.value?.scale ?: 1f), base.fontScale)
                CompositionLocalProvider(LocalDensity provides density) {
                    // Material You colours unless Settings turned them off (UX rule V3a); the accent is read in onCreate.
                    LockOnTheme(dark, themeLook(choice)) { LaunchSplash() }
                }
            } else {
                BackHandler(enabled = state.canGoBack) { state.goBack() }
                LockOnApp(
                    c, state, s, files,
                    artworkDir = filesDir.resolve("artwork").absolutePath,
                    reduceMotion = systemReduceMotion,
                    // From here the window behind the app is the app's colour, not the launch screen's.
                    windowBackground = { window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(it.toArgb())) },
                )
            }
        }
    }

    /**
     * Keeps the system's splash screen up over the app, rather than letting it go at the app's first
     * frame, until the tabs and recent decks have built and drawn their cards underneath (App.kt). The
     * app's own launch screen then has nothing to cover and goes at once, unseen.
     */
    @androidx.annotation.RequiresApi(31)
    private fun holdSystemSplash() {
        splashScreen.setOnExitAnimationListener { view ->
            heldSplash = view
            state.systemSplash = true
            if (state.booted) releaseSystemSplash()
        }
    }

    /** Fades the system's splash into the app and lets it go, if it is still held. */
    private fun releaseSystemSplash() {
        val view = heldSplash ?: return
        heldSplash = null
        val reduce = systemReduceMotion || appContainer?.settings?.motion?.value == MotionChoice.REDUCED
        view.animate().alpha(0f)
            // As quick as the app's own launch screen fades on desktop (Motion.LONG).
            .setDuration(if (reduce) 90L else 280L)
            .setInterpolator(android.view.animation.PathInterpolator(0.3f, 0f, 0.8f, 0.15f))
            .withEndAction { removeSystemSplash(view) }
            .start()
    }

    private fun removeSystemSplash(view: android.view.View) {
        if (android.os.Build.VERSION.SDK_INT >= 31) (view as android.window.SplashScreenView).remove()
        state.systemSplash = false
    }

    // Discovery and auto-sync run only while Lock On is on screen.
    override fun onStart() {
        super.onStart()
        readSystemAccent()
        syncController?.setForeground(true)
    }

    /**
     * The wallpaper's colours for Material You colours (UX rule V3a), on Android 12 and later: its accent,
     * the system's own light and dark Material You schemes, and on Android 14 and later the contrast
     * setting. Older versions have none, and Lock On's own Sky and Twilight show. Read on every start,
     * since the wallpaper or the contrast may have changed while Lock On was away.
     */
    private fun readSystemAccent() {
        val accent = app.lockon.ui.theme.SystemAccent
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            accent.schemes.value = app.lockon.ui.theme.SystemSchemes(
                androidx.compose.material3.dynamicLightColorScheme(this),
                androidx.compose.material3.dynamicDarkColorScheme(this),
            )
            accent.color.value = Color(getColor(android.R.color.system_accent1_500))
        } else {
            accent.schemes.value = null
            accent.color.value = null
        }
        accent.contrast.value =
            if (android.os.Build.VERSION.SDK_INT >= 34) getSystemService(android.app.UiModeManager::class.java).contrast else 0f
    }

    override fun onStop() {
        syncController?.setForeground(false)
        super.onStop()
    }

    override fun onDestroy() {
        heldSplash?.let { heldSplash = null; removeSystemSplash(it) }
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
