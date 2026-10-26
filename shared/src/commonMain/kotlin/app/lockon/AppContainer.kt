package app.lockon

import app.lockon.data.CatalogRepository
import app.lockon.data.DeckRepository
import app.lockon.data.db.LockOnDatabase
import app.lockon.resources.Res
import app.lockon.sync.DeviceRepository
import app.lockon.sync.LocalIdentity
import app.lockon.sync.SyncClient
import app.lockon.sync.SyncHost

/** Everything the screens and sync need, created once per process. */
class AppContainer private constructor(
    val database: LockOnDatabase,
    val identity: LocalIdentity,
    val catalog: CatalogRepository,
    val decks: DeckRepository,
    val devices: DeviceRepository,
) {
    val syncClient by lazy { SyncClient(decks, devices) }
    val syncHost by lazy { SyncHost(decks, devices) }

    companion object {
        suspend fun create(database: LockOnDatabase, defaultDeviceName: String): AppContainer {
            val devices = DeviceRepository(database)
            val identity = devices.identity(defaultDeviceName)
            val catalog = CatalogRepository(database)
            catalog.load(
                catalogJson = Res.readBytes("files/catalog/sample-catalog.json").decodeToString(),
                rulesetsJson = Res.readBytes("files/rulesets/rulesets.json").decodeToString(),
            )
            return AppContainer(database, identity, catalog, DeckRepository(database, identity.deviceId), devices)
        }
    }
}
