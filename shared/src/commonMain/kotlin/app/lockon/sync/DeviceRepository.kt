package app.lockon.sync

import app.lockon.data.db.LockOnDatabase
import app.lockon.data.db.MetaEntity
import app.lockon.data.db.PairedDeviceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.uuid.Uuid

enum class DeviceRole { HOST, CLIENT }

data class PairedDevice(
    val deviceId: String,
    val name: String,
    val role: DeviceRole,
    val host: String?,
    val port: Int?,
    val certFingerprint: String?,
    val pairedAt: Long,
    val lastSyncAt: Long?,
    val lastError: String?,
    val pushCursor: Long,
    val pullCursor: Long,
)

data class LocalIdentity(val deviceId: String, val deviceName: String)

class DeviceRepository(private val db: LockOnDatabase) {
    private val dao = db.deviceDao()

    val pairedDevices: Flow<List<PairedDevice>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    /** The stable ID this install syncs under. Created on first launch. */
    suspend fun identity(defaultName: String): LocalIdentity {
        val meta = db.metaDao()
        val id = meta.get(KEY_ID) ?: Uuid.random().toString().also { meta.put(MetaEntity(KEY_ID, it)) }
        val name = meta.get(KEY_NAME) ?: defaultName.also { meta.put(MetaEntity(KEY_NAME, it)) }
        return LocalIdentity(id, name)
    }

    suspend fun rename(name: String) = db.metaDao().put(MetaEntity(KEY_NAME, name))

    /** Whether phones sync on their own while Lock On is open. On unless turned off. */
    suspend fun autoSyncEnabled(): Boolean = db.metaDao().get(KEY_AUTO_SYNC) != "false"

    suspend fun setAutoSync(enabled: Boolean) = db.metaDao().put(MetaEntity(KEY_AUTO_SYNC, enabled.toString()))

    suspend fun device(deviceId: String): PairedDevice? = dao.device(deviceId)?.toModel()

    suspend fun byTokenHash(tokenHash: String): PairedDevice? = dao.byTokenHash(tokenHash)?.toModel()

    /** Adds or re-pairs a device. Re-pairing keeps cursors so nothing is sent twice. */
    suspend fun savePairing(
        deviceId: String, name: String, role: DeviceRole,
        host: String? = null, port: Int? = null, certFingerprint: String? = null, tokenHash: String? = null,
    ) {
        val existing = dao.device(deviceId)
        dao.upsert(
            PairedDeviceEntity(
                deviceId = deviceId, name = name, role = role.name, host = host, port = port,
                certFingerprint = certFingerprint, tokenHash = tokenHash,
                pairedAt = existing?.pairedAt ?: Clock.System.now().toEpochMilliseconds(),
                lastSyncAt = existing?.lastSyncAt, lastError = null,
                pushCursor = existing?.pushCursor ?: 0, pullCursor = existing?.pullCursor ?: 0,
            ),
        )
    }

    suspend fun updateAddress(deviceId: String, host: String, port: Int) {
        val existing = dao.device(deviceId) ?: return
        dao.upsert(existing.copy(host = host, port = port))
    }

    suspend fun advancePush(deviceId: String, cursor: Long) = dao.advancePush(deviceId, cursor)
    suspend fun advancePull(deviceId: String, cursor: Long) = dao.advancePull(deviceId, cursor)
    suspend fun markSynced(deviceId: String) = dao.markSynced(deviceId, Clock.System.now().toEpochMilliseconds())
    suspend fun markError(deviceId: String, error: String?) = dao.markError(deviceId, error)
    suspend fun forget(deviceId: String) = dao.delete(deviceId)

    private fun PairedDeviceEntity.toModel() = PairedDevice(
        deviceId, name, DeviceRole.valueOf(role), host, port, certFingerprint, pairedAt, lastSyncAt, lastError,
        pushCursor, pullCursor,
    )

    private companion object {
        const val KEY_ID = "deviceId"
        const val KEY_NAME = "deviceName"
        const val KEY_AUTO_SYNC = "autoSync"
    }
}
