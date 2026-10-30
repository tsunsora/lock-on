package app.lockon

import androidx.room.Room
import app.lockon.catalog.CatalogUpdater
import app.lockon.catalog.GcgApi
import app.lockon.data.CatalogOrigin
import app.lockon.data.CatalogRepository
import app.lockon.data.DeckRepository
import app.lockon.data.db.LockOnDatabase
import app.lockon.model.CardColor
import app.lockon.model.CardType
import app.lockon.rules.DeckAnalysis
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GcgApiTest {
    private val db = LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>())
    private val resources = File("src/commonMain/composeResources/files")
    private val decks = DeckRepository(db, "me")
    private var now = 1_000_000_000L
    private val repo = CatalogRepository(db) { now }

    @AfterTest fun close() = db.close()

    private fun manifest(run: Int) = """{ "schema_version": 1, "dataset_version": "$run-abc", "built_at": "2026-10-26T12:26:52Z", "card_count": 5 }"""

    /** Shaped like real gcg-api entries: a standard printing, a parallel, a reprint, a trait link and a token. */
    private val cards = """[
      { "product_id": "GD01-001", "card_number": "GD01-001", "name": "Gundam", "set_code": "GD01", "rarity": "LR", "card_type": "UNIT",
        "color": "Blue", "level": 4, "cost": 3, "ap": 3, "hp": 3, "zone": "Space Earth", "link": "[Amuro Ray]",
        "traits": ["Earth Federation", "White Base Team"], "link_refs": ["Amuro Ray"], "effect": "【When Paired】Draw 1.",
        "where_to_get": "Newtype Rising [GD01]", "image_url": "https://www.gundam-gcg.com/en/images/cards/card/GD01-001.webp" },
      { "product_id": "GD01-001_p1", "card_number": "GD01-001", "name": "Gundam", "set_code": "GD01", "rarity": "LR +", "card_type": "UNIT",
        "color": "Blue", "level": 4, "cost": 3, "ap": 3, "hp": 3, "effect": "older wording", "where_to_get": "Newtype Rising [GD01]",
        "image_url": "https://www.gundam-gcg.com/en/images/cards/card/GD01-001_p1.webp" },
      { "product_id": "GD01-001_p3", "card_number": "GD01-001", "name": "Gundam", "set_code": "GD01", "rarity": "LR", "card_type": "UNIT",
        "color": "Blue", "where_to_get": "GUNDAM CARD GAME 1st Anniversary Set [PB03]" },
      { "product_id": "GD01-010", "card_number": "GD01-010", "name": "Guncannon", "set_code": "GD01", "rarity": "C", "card_type": "UNIT",
        "color": "Blue", "level": 3, "cost": 2, "link": "(White Base Team) Trait", "link_refs": ["White Base Team"], "zone": "-" },
      { "product_id": "GD01-090", "card_number": "GD01-090", "name": "Amuro Ray", "set_code": "GD01", "rarity": "R", "card_type": "PILOT",
        "color": "Blue", "level": 4, "cost": 1, "traits": ["Earth Federation", "White Base Team"] },
      { "product_id": "R-001", "card_number": "R-001", "name": "Resource", "set_code": "R", "rarity": "C", "card_type": "RESOURCE", "color": null },
      { "product_id": "T-001", "card_number": "T-001", "name": "EX Base", "set_code": "T", "rarity": "-", "card_type": "EX BASE", "color": null }
    ]"""

    @Test fun convertsPrintingsLinksAndTokens() {
        val converted = GcgApi.toCatalog(GcgApi.parseManifest(manifest(30)), GcgApi.parseCards(cards))
        val catalog = converted.catalog
        assertEquals(0, converted.skippedUnknown)
        assertEquals(listOf("GD01-001", "GD01-010", "GD01-090", "R-001", "T-001"), catalog.cards.map { it.cardNumber })
        assertEquals(CardType.EX_BASE, catalog.cards.last().type, "tokens are listed, under their own type")
        assertEquals(30, catalog.version)
        val gundam = catalog.cards.first()
        assertEquals("【When Paired】Draw 1.", gundam.text, "gameplay comes from the standard printing")
        assertEquals(listOf(CardColor.BLUE), gundam.colors)
        assertEquals(listOf("Space", "Earth"), gundam.zones)
        assertEquals("LR", gundam.rarity)
        assertTrue(catalog.cards.single { it.cardNumber == "GD01-010" }.zones.isEmpty())
        val resource = catalog.cards.single { it.cardNumber == "R-001" }
        assertEquals(CardType.RESOURCE, resource.type)
        assertTrue(resource.colors.isEmpty())

        val printings = catalog.printings.filter { it.cardNumber == "GD01-001" }.associateBy { it.printingId }
        assertEquals("Standard", printings.getValue("GD01-001").label)
        assertEquals("Parallel LR+", printings.getValue("GD01-001_p1").label)
        assertEquals("Reprint: GUNDAM CARD GAME 1st Anniversary Set", printings.getValue("GD01-001_p3").label)
        assertNull(printings.getValue("GD01-001_p3").artworkUrl)

        // Both link styles are understood by deck analysis.
        val pairs = DeckAnalysis.linkPairs(catalog.cards).associate { it.first.cardNumber to it.second.map { p -> p.cardNumber } }
        assertEquals(mapOf("GD01-001" to listOf("GD01-090"), "GD01-010" to listOf("GD01-090")), pairs)
        assertTrue(GcgApi.ATTRIBUTION in catalog.source)
    }

    @Test fun downloadThenCheckForUpdatesWithoutRedownloading() = runTest {
        repo.load(File(resources, "catalog/sample-catalog.json").readText(), File(resources, "rulesets/rulesets.json").readText())
        val served = mutableMapOf(GcgApi.MANIFEST_URL to manifest(30), GcgApi.CARDS_URL to cards)
        val fetched = mutableListOf<String>()
        val updater = CatalogUpdater(repo, decks, fetch = { fetched += it; served.getValue(it) })

        val preview = updater.latestCardDatabase()
        updater.install(preview)
        assertEquals(CatalogOrigin.GCG_API, repo.info.value!!.origin)
        assertEquals(5, repo.cards.value.size)
        assertEquals(CardType.EX_BASE, repo.cards.value.getValue("T-001").type, "token types survive the database")
        // The original rules stay, since gcg-api ships no ban list.
        assertEquals(2, repo.rulesets.value.size)

        fetched.clear()
        assertIs<CatalogUpdater.UpdateCheck.UpToDate>(updater.checkCardDatabase())
        assertEquals(listOf(GcgApi.MANIFEST_URL), fetched, "checking only reads the small version file")

        served[GcgApi.MANIFEST_URL] = manifest(31)
        assertEquals(31, assertIs<CatalogUpdater.UpdateCheck.Available>(updater.checkCardDatabase()).version)
    }

    private val baseToken = """{ "product_id": "T-037", "card_number": "T-037", "name": "Axis", "set_code": "GD06", "rarity": "C", "card_type": "BASE", "color": null }"""

    @Test fun baseTokensListedAsBasesBecomeBaseTokens() {
        val entries = GcgApi.parseCards(cards) + GcgApi.parseCards("[$baseToken]")
        val axis = GcgApi.toCatalog(GcgApi.parseManifest(manifest(30)), entries).catalog.cards.single { it.cardNumber == "T-037" }
        assertEquals(CardType.BASE_TOKEN, axis.type)
        assertTrue(axis.type.isToken)
    }

    @Test fun anOfficialListWithABaseTokenAsABaseIsOfferedAgain() = runTest {
        repo.load(File(resources, "catalog/sample-catalog.json").readText(), File(resources, "rulesets/rulesets.json").readText())
        val all = GcgApi.parseCards(cards) + GcgApi.parseCards("[$baseToken]")
        val old = GcgApi.toCatalog(GcgApi.parseManifest(manifest(30)), all).catalog
        repo.install(old.copy(cards = old.cards.map { if (it.cardNumber == "T-037") it.copy(type = CardType.BASE) else it }), CatalogOrigin.GCG_API, GcgApi.MANIFEST_URL)
        val updater = CatalogUpdater(repo, decks, fetch = { mapOf(GcgApi.MANIFEST_URL to manifest(30), GcgApi.CARDS_URL to "[${cards.trim().removePrefix("[").removeSuffix("]")}, $baseToken]").getValue(it) })

        assertIs<CatalogUpdater.UpdateCheck.Available>(updater.checkCardDatabase(), "same run, but Axis was converted as a base")
        updater.install(updater.latestCardDatabase())
        assertEquals(CardType.BASE_TOKEN, repo.cards.value.getValue("T-037").type)
        assertIs<CatalogUpdater.UpdateCheck.UpToDate>(updater.checkCardDatabase())
    }

    @Test fun anOfficialListWithoutTokensIsOfferedAgain() = runTest {
        repo.load(File(resources, "catalog/sample-catalog.json").readText(), File(resources, "rulesets/rulesets.json").readText())
        val withoutTokens = GcgApi.parseCards(cards).filter { CardType.parse(it.cardType)?.isToken != true }
        val old = GcgApi.toCatalog(GcgApi.parseManifest(manifest(30)), withoutTokens).catalog
        repo.install(old, CatalogOrigin.GCG_API, GcgApi.MANIFEST_URL)
        val updater = CatalogUpdater(repo, decks, fetch = { mapOf(GcgApi.MANIFEST_URL to manifest(30), GcgApi.CARDS_URL to cards).getValue(it) })

        assertIs<CatalogUpdater.UpdateCheck.Available>(updater.checkCardDatabase(), "same run, but converted before tokens were kept")
        val preview = updater.latestCardDatabase()
        assertTrue(preview.isNewer)
        updater.install(preview)
        assertIs<CatalogUpdater.UpdateCheck.UpToDate>(updater.checkCardDatabase())
    }

    @Test fun backgroundCheckIsWeeklyAndOnlyForTheOfficialList() = runTest {
        repo.load(File(resources, "catalog/sample-catalog.json").readText(), File(resources, "rulesets/rulesets.json").readText())
        val served = mutableMapOf(GcgApi.MANIFEST_URL to manifest(30), GcgApi.CARDS_URL to cards)
        val updater = CatalogUpdater(repo, decks, fetch = { served.getValue(it) })

        updater.quietWeeklyCheck(now)
        assertNull(repo.updateAvailable.value, "sample-catalog users are never nudged")

        updater.install(updater.latestCardDatabase())
        updater.checkCardDatabase()
        served[GcgApi.MANIFEST_URL] = manifest(31)
        now += 3L * 24 * 60 * 60 * 1000
        updater.quietWeeklyCheck(now)
        assertNull(repo.updateAvailable.value, "checked three days ago, so it waits")
        now += 5L * 24 * 60 * 60 * 1000
        updater.quietWeeklyCheck(now)
        assertEquals("Card database update available", repo.updateAvailable.value)
    }
}
