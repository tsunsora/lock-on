package app.lockon

import androidx.room.Room
import app.lockon.catalog.CatalogUpdater
import app.lockon.data.CatalogException
import app.lockon.data.CatalogOrigin
import app.lockon.data.CatalogRepository
import app.lockon.data.DeckRepository
import app.lockon.data.db.LockOnDatabase
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogTest {
    private val db = LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>())
    private val resources = File("src/commonMain/composeResources/files")
    private val sample = File(resources, "catalog/sample-catalog.json").readText()
    private val rules = File(resources, "rulesets/rulesets.json").readText()
    private val decks = DeckRepository(db, "me")

    private fun catalog(id: String = "real", version: Int = 1, extra: String = "", cards: String = DEFAULT_CARDS) = """
        { "format": "lock-on-catalog", "formatVersion": 1, "catalogId": "$id", "version": $version,
          "name": "Real $version", "cards": [$cards] $extra }
    """

    @AfterTest fun close() = db.close()

    @Test fun firstLaunchInstallsTheSample() = runTest {
        val repo = CatalogRepository(db).apply { load(sample, rules) }
        assertEquals(20, repo.cards.value.size)
        assertEquals(CatalogOrigin.BUNDLED, repo.info.value!!.origin)
        assertEquals(2, repo.rulesets.value.size)
    }

    @Test fun anImportedCatalogSurvivesRestartsAndCanBeUndoneToTheSample() = runTest {
        val repo = CatalogRepository(db).apply { load(sample, rules) }
        repo.install(repo.check(catalog()).file, CatalogOrigin.FILE, null)

        val restarted = CatalogRepository(db).apply { load(sample, rules) }
        assertEquals(setOf("GD01-001", "GD01-002"), restarted.cards.value.keys)
        assertEquals("Real 1", restarted.info.value!!.name)

        restarted.restoreBundled(sample)
        assertEquals(20, restarted.cards.value.size)
        assertEquals(CatalogOrigin.BUNDLED, restarted.info.value!!.origin)
    }

    @Test fun rulesetsInACatalogReplaceTheRulesUntilTheSampleReturns() = runTest {
        val repo = CatalogRepository(db).apply { load(sample, rules) }
        val withRules = catalog(extra = """, "rulesets": [{ "id": "event", "name": "Event", "region": "EN", "effectiveDate": "2026-11-01", "maxCopies": 3 }]""")
        repo.install(repo.check(withRules).file, CatalogOrigin.FILE, null)
        assertEquals(listOf("event"), repo.rulesets.value.map { it.id })
        assertEquals(3, CatalogRepository(db).apply { load(sample, rules) }.ruleset.value.maxCopies)
        repo.restoreBundled(sample)
        assertEquals(2, repo.rulesets.value.size)
    }

    @Test fun validationExplainsWhatIsWrong() = runTest {
        val repo = CatalogRepository(db)
        assertFailsWith<CatalogException> { repo.check("{ not json") }
        assertFailsWith<CatalogException> { repo.check("""{"format":"something-else","catalogId":"x","version":1,"name":"x","cards":[]}""") }
        assertFailsWith<CatalogException> { repo.check(catalog(cards = "")) }
        val dupe = assertFailsWith<CatalogException> { repo.check(catalog(cards = "$CARD_1, $CARD_1")) }
        assertTrue("GD01-001" in dupe.message!!)
        val newer = catalog().replace("\"formatVersion\": 1", "\"formatVersion\": 9")
        assertTrue("newer version" in assertFailsWith<CatalogException> { repo.check(newer) }.message!!)
        val stray = repo.check(catalog(extra = """, "printings": [{ "printingId": "X", "cardNumber": "NOPE-1", "setCode": "X", "rarity": "C" }]"""))
        assertTrue(stray.warnings.single().contains("1 printings"))
        assertTrue(stray.file.printings.isEmpty())
    }

    @Test fun previewSaysUpdateVersusReplacementAndWhatDecksWouldLose() = runTest {
        val repo = CatalogRepository(db).apply { load(sample, rules) }
        decks.create(DeckSnapshot("Mine", entries = listOf(DeckEntry("SMP-001", 4), DeckEntry("GD01-001", 2))))
        val served = mutableMapOf("https://example.test/catalog.json" to catalog(version = 1))
        val updater = CatalogUpdater(repo, decks, fetch = { served.getValue(it) })

        val first = updater.fromUrl("https://example.test/catalog.json")
        assertFalse(first.isUpdate, "a different catalog ID replaces the sample")
        assertEquals(setOf("SMP-001"), first.missingFromDecks)
        updater.install(first)
        assertEquals("https://example.test/catalog.json", repo.info.value!!.updateUrl)

        served["https://example.test/catalog.json"] = catalog(version = 2)
        val second = updater.fromUrl(repo.info.value!!.updateUrl!!)
        assertTrue(second.isUpdate && second.isNewer)
        val same = updater.fromText(catalog(version = 1))
        assertFalse(same.isNewer)
        // Decks are never touched by catalog changes.
        updater.install(second)
        assertEquals(2, decks.decks.value().single().snapshot.quantityOf("GD01-001", app.lockon.model.DeckSection.MAIN))
    }

    @Test fun onlyHttpsLinksAreFetched() = runTest {
        val repo = CatalogRepository(db).apply { load(sample, rules) }
        val updater = CatalogUpdater(repo, decks, fetch = { error("should not be called") })
        assertTrue("https" in assertFailsWith<CatalogException> { updater.fromUrl("http://example.test/c.json") }.message!!)
    }

    private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.value(): T = first()

    private companion object {
        const val CARD_1 = """{ "cardNumber": "GD01-001", "name": "Unit A", "type": "UNIT", "colors": ["BLUE"], "level": 2, "cost": 1, "setCode": "GD01" }"""
        const val CARD_2 = """{ "cardNumber": "GD01-002", "name": "Pilot B", "type": "PILOT", "colors": ["BLUE"], "level": 3, "cost": 1, "setCode": "GD01" }"""
        const val DEFAULT_CARDS = "$CARD_1, $CARD_2"
    }
}
