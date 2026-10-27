package app.lockon.android

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.ConnectivityManager
import android.os.Build
import app.lockon.update.UpdateInstaller
import app.lockon.update.UpdatePlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Installs a downloaded APK through the system's package installer. Android asks the person to
 * allow installs from Lock On the first time and to confirm the update; after Lock On has
 * installed itself once, Android 12 and later can update it without asking. Android only accepts
 * an APK signed with the same key as the installed app.
 */
class AndroidUpdateInstaller(private val context: Context) : UpdateInstaller {
    override val platform = UpdatePlatform.ANDROID
    override val canInstall = true
    override val downloadDir: String = File(context.noBackupFilesDir, "updates").absolutePath

    /** Background downloads wait for Wi-Fi or another unmetered network. */
    override fun backgroundDownloadAllowed(): Boolean {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return false
        return !connectivity.isActiveNetworkMetered
    }

    override suspend fun install(path: String, reopen: Boolean) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            val apk = File(path)
            session.openWrite("lock-on.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            // The system fills in the result, so the intent has to stay mutable.
            val callback = PendingIntent.getBroadcast(
                context, sessionId, Intent(context, UpdateInstallReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(callback.intentSender)
        }
    }
}

/** Hears back from the package installer: shows its confirmation, or reports why it failed. */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as LockOnApplication
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirm?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            // Android stops Lock On to replace it, so success is seen as the new version opening.
            PackageInstaller.STATUS_SUCCESS -> Unit
            else -> {
                val message = when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                    PackageInstaller.STATUS_FAILURE_ABORTED -> "The update wasn't installed. Tap Install update to try again."
                    PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                        "Android couldn't install this update over the current app. It may be signed with a different key."
                    PackageInstaller.STATUS_FAILURE_STORAGE -> "There isn't enough storage to install the update."
                    else -> "The update didn't install. Try again later."
                }
                app.appScope.launch { app.container.await().updater?.installProblem(message) }
            }
        }
    }
}
