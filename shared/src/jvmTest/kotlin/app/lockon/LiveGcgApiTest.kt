package app.lockon

import androidx.room.Room
import app.lockon.catalog.CatalogUpdater
import app.lockon.data.CatalogRepository
import app.lockon.data.DeckRepository
import app.lockon.data.db.LockOnDatabase
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Downloads the real gcg-api dataset and installs it into a throwaway database. Needs the network,
 * so it only runs with LOCKON_LIVE=1:  LOCKON_LIVE=1 ./gradlew :shared:jvmTest --tests "*LiveGcgApiTest*"
 */
class LiveGcgApiTest {
    @Test fun realDatabaseDownloadsConvertsAndInstalls() = runBlocking {
        if (System.getenv("LOCKON_LIVE") != "1") return@runBlocking
        val db = LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>())
        try {
            val resources = File("src/commonMain/composeResources/files")
            val repo = CatalogRepository(db).apply {
                load(File(resources, "catalog/sample-catalog.json").readText(), File(resources, "rulesets/rulesets.json").readText())
            }
            val updater = CatalogUpdater(repo, DeckRepository(db, "live"))
            val preview = updater.latestCardDatabase()
            updater.install(preview)
            val cards = repo.cards.value
            println("Installed ${cards.size} cards, ${repo.printings.value.values.sumOf { it.size }} printings, version ${repo.info.value?.version}")
            preview.checked.warnings.forEach { println("warning: $it") }
            assertTrue(cards.size > 1000, "expected the full card list")
            assertTrue(cards.values.any { it.name == "Gundam" })
        } finally {
            db.close()
        }
    }
}
