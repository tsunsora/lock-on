package app.lockon.android

import android.app.Application
import android.os.Build
import android.provider.Settings
import androidx.room.Room
import app.lockon.AppContainer
import app.lockon.data.db.LockOnDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

class LockOnApplication : Application() {
    /** Outlives activities, so saves and syncs finish across rotation and leaving the screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var container: Deferred<AppContainer>
        private set
    lateinit var sync: Deferred<AndroidSyncController>
        private set

    override fun onCreate() {
        super.onCreate()
        container = appScope.async(Dispatchers.Default) {
            val database = LockOnDatabase.build(
                Room.databaseBuilder<LockOnDatabase>(this@LockOnApplication, getDatabasePath(LockOnDatabase.FILE_NAME).absolutePath),
            )
            val name = Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)?.takeIf { it.isNotBlank() } ?: Build.MODEL
            AppContainer.create(database, defaultDeviceName = name)
        }
        sync = appScope.async { AndroidSyncController(this@LockOnApplication, container.await(), SecretStore(this@LockOnApplication), appScope) }
    }
}
