package app.lockon.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        CardEntity::class, PrintingEntity::class, MetaEntity::class,
        DeckEntity::class, DeckEntryEntity::class, RevisionEntity::class, RevisionParentEntity::class,
        PairedDeviceEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(LockOnDatabaseConstructor::class)
abstract class LockOnDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun metaDao(): MetaDao
    abstract fun deckDao(): DeckDao
    abstract fun revisionDao(): RevisionDao
    abstract fun deviceDao(): DeviceDao

    companion object {
        const val FILE_NAME = "lock-on.db"

        fun build(builder: Builder<LockOnDatabase>): LockOnDatabase = builder
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}

// Room generates the actual implementations for each platform.
@Suppress("KotlinNoActualForExpect")
expect object LockOnDatabaseConstructor : RoomDatabaseConstructor<LockOnDatabase> {
    override fun initialize(): LockOnDatabase
}
