package app.lockon

import app.lockon.catalog.CatalogUpdater
import app.lockon.data.CatalogRepository
import app.lockon.data.DeckRepository
import app.lockon.data.db.LockOnDatabase
import app.lockon.resources.Res
import app.lockon.sync.DeviceRepository
import app.lockon.sync.LocalIdentity
import app.lockon.sync.SyncClient
import app.lockon.sync.SyncHost
import app.lockon.update.AppUpdater
import app.lockon.update.UpdateInstaller

/** Everything the screens and sync need, created once per process. */
class AppContainer private constructor(
    val database: LockOnDatabase,
    val identity: LocalIdentity,
    val catalog: CatalogRepository,
    val decks: DeckRepository,
    val devices: DeviceRepository,
    /** Null where this platform can't update itself. */
    val updater: AppUpdater?,
) {
    val syncClient by lazy { SyncClient(decks, devices) }
    val syncHost by lazy { SyncHost(decks, devices) }
    val catalogUpdater by lazy { CatalogUpdater(catalog, decks) }

    suspend fun restoreSampleCatalog() = catalog.restoreBundled(readBundledCatalog())

    companion object {
        private suspend fun readBundledCatalog() = Res.readBytes("files/catalog/sample-catalog.json").decodeToString()

        suspend fun create(database: LockOnDatabase, defaultDeviceName: String, updateInstaller: UpdateInstaller? = null): AppContainer {
            val devices = DeviceRepository(database)
            val identity = devices.identity(defaultDeviceName)
            val catalog = CatalogRepository(database)
            catalog.load(
                bundledCatalogJson = readBundledCatalog(),
                bundledRulesetsJson = Res.readBytes("files/rulesets/rulesets.json").decodeToString(),
            )
            val updater = updateInstaller?.let { AppUpdater(database.metaDao(), it) }
            return AppContainer(database, identity, catalog, DeckRepository(database, identity.deviceId), devices, updater)
        }
    }
}
