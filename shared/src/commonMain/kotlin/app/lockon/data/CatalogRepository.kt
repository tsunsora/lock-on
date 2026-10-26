package app.lockon.data

import app.lockon.data.db.CardEntity
import app.lockon.data.db.LockOnDatabase
import app.lockon.data.db.MetaEntity
import app.lockon.data.db.PrintingEntity
import app.lockon.model.Card
import app.lockon.model.CardColor
import app.lockon.model.CardType
import app.lockon.model.Printing
import app.lockon.model.Ruleset
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * A downloadable card catalog. Each device updates its own copy; catalogs are never synced.
 * The format is documented in docs/CATALOG_FORMAT.md.
 */
@Serializable
data class CatalogFile(
    val format: String = CATALOG_FORMAT,
    val formatVersion: Int = CATALOG_FORMAT_VERSION,
    val catalogId: String,
    val version: Int,
    val name: String,
    val source: String = "",
    val cards: List<Card>,
    val printings: List<Printing> = emptyList(),
    /** Optional: a catalog can ship the deck rules that go with it. */
    val rulesets: List<Ruleset>? = null,
) {
    companion object {
        const val CATALOG_FORMAT = "lock-on-catalog"
        const val CATALOG_FORMAT_VERSION = 1
    }
}

@Serializable
data class RulesetFile(val rulesets: List<Ruleset>)

enum class CatalogOrigin(val label: String) {
    BUNDLED("Built-in sample"),
    FILE("Imported from a file"),
    URL("Downloaded from a link"),
    GCG_API("Official card list via gcg-api"),
}

/** What is installed now, for the Catalog sheet. */
data class CatalogInfo(
    val catalogId: String,
    val version: Int,
    val name: String,
    val source: String,
    val origin: CatalogOrigin,
    val updateUrl: String?,
    val installedAt: Long?,
    val cardCount: Int,
)

/** A catalog that parsed, with anything worth telling the person before it is installed. */
data class CheckedCatalog(val file: CatalogFile, val warnings: List<String>)

class CatalogException(message: String) : Exception(message)

class CatalogRepository(private val db: LockOnDatabase, private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _cards = MutableStateFlow<Map<String, Card>>(emptyMap())
    private val _printings = MutableStateFlow<Map<String, List<Printing>>>(emptyMap())
    private val _rulesets = MutableStateFlow<List<Ruleset>>(emptyList())
    private val _ruleset = MutableStateFlow(FALLBACK_RULESET)
    private val _info = MutableStateFlow<CatalogInfo?>(null)
    private val _catalogName = MutableStateFlow("")
    private val _showArtwork = MutableStateFlow(true)
    private val _updateAvailable = MutableStateFlow<String?>(null)
    private var bundledRulesetsJson = ""

    val cards: StateFlow<Map<String, Card>> = _cards.asStateFlow()
    val printings: StateFlow<Map<String, List<Printing>>> = _printings.asStateFlow()
    val rulesets: StateFlow<List<Ruleset>> = _rulesets.asStateFlow()
    val ruleset: StateFlow<Ruleset> = _ruleset.asStateFlow()
    val info: StateFlow<CatalogInfo?> = _info.asStateFlow()
    val catalogName: StateFlow<String> = _catalogName.asStateFlow()
    /** Set when a background check found a newer card database; shown quietly in the Cards tab. */
    val updateAvailable: StateFlow<String?> = _updateAvailable.asStateFlow()

    fun setUpdateAvailable(message: String?) { _updateAvailable.value = message }

    suspend fun lastUpdateCheck(): Long = db.metaDao().get(KEY_CHECKED_AT)?.toLongOrNull() ?: 0

    suspend fun markUpdateChecked() = db.metaDao().put(MetaEntity(KEY_CHECKED_AT, now().toString()))

    /** Whether card images from printing artwork links are shown. On unless turned off. */
    val showArtwork: StateFlow<Boolean> = _showArtwork.asStateFlow()

    suspend fun setShowArtwork(show: Boolean) {
        db.metaDao().put(MetaEntity(KEY_SHOW_ARTWORK, show.toString()))
        _showArtwork.value = show
    }

    /**
     * Startup. The bundled sample is installed on first launch and refreshed when the app ships a
     * newer one, but a catalog the person imported or downloaded is never replaced by it.
     */
    suspend fun load(bundledCatalogJson: String, bundledRulesetsJson: String) {
        this.bundledRulesetsJson = bundledRulesetsJson
        val meta = db.metaDao()
        val origin = meta.get(KEY_ORIGIN)?.let { runCatching { CatalogOrigin.valueOf(it) }.getOrNull() }
        if (origin == null || origin == CatalogOrigin.BUNDLED) {
            val bundled = check(bundledCatalogJson).file
            // No origin means a first launch or an install from before catalog metadata existed.
            if (origin == null || meta.get(KEY_CATALOG) != "${bundled.catalogId}@${bundled.version}") {
                install(bundled, CatalogOrigin.BUNDLED, null)
            }
        }
        refresh()
    }

    /** Parses and validates a catalog without installing it. Throws [CatalogException] with a readable reason. */
    fun check(text: String): CheckedCatalog {
        val file = try {
            json.decodeFromString(CatalogFile.serializer(), text.trim().removePrefix("\uFEFF"))
        } catch (e: Exception) {
            val reason = e.message?.lineSequence()?.firstOrNull() ?: "unreadable"
            throw CatalogException("This isn't a Lock On card catalog, or it is damaged ($reason).")
        }
        return validate(file)
    }

    /** The checks every catalog goes through, whether it came from a file, a link or a conversion. */
    fun validate(file: CatalogFile): CheckedCatalog {
        if (file.format != CatalogFile.CATALOG_FORMAT) throw CatalogException("This file isn't a Lock On card catalog.")
        if (file.formatVersion > CatalogFile.CATALOG_FORMAT_VERSION) {
            throw CatalogException("This catalog needs a newer version of Lock On.")
        }
        if (file.catalogId.isBlank() || file.name.isBlank()) throw CatalogException("The catalog has no ID or name.")
        if (file.cards.isEmpty()) throw CatalogException("The catalog has no cards.")
        val duplicates = file.cards.groupBy { it.cardNumber }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            throw CatalogException("These card numbers appear more than once: ${duplicates.take(5).joinToString()}.")
        }
        val warnings = mutableListOf<String>()
        val numbers = file.cards.map { it.cardNumber }.toSet()
        val strayPrintings = file.printings.count { it.cardNumber !in numbers }
        if (strayPrintings > 0) warnings += "$strayPrintings printings point at cards that aren't in the catalog and will be skipped."
        val unnamed = file.cards.count { it.name.isBlank() }
        if (unnamed > 0) warnings += "$unnamed cards have no name."
        if (file.rulesets?.isEmpty() == true) warnings += "The catalog's ruleset list is empty, so the current rules are kept."
        return CheckedCatalog(file.copy(printings = file.printings.filter { it.cardNumber in numbers }), warnings)
    }

    /** Replaces the installed catalog in one transaction. Decks are never changed. */
    suspend fun install(file: CatalogFile, origin: CatalogOrigin, updateUrl: String?) {
        val meta = db.metaDao()
        db.useWriterConnection { connection ->
            connection.immediateTransaction {
                db.catalogDao().clearCards()
                db.catalogDao().clearPrintings()
                db.catalogDao().insertCards(file.cards.map { it.toEntity() })
                db.catalogDao().insertPrintings(file.printings.map { it.toEntity() })
                meta.put(MetaEntity(KEY_CATALOG, "${file.catalogId}@${file.version}"))
                meta.put(MetaEntity(KEY_NAME, file.name))
                meta.put(MetaEntity(KEY_SOURCE, file.source))
                meta.put(MetaEntity(KEY_ORIGIN, origin.name))
                meta.put(MetaEntity(KEY_URL, updateUrl.orEmpty()))
                meta.put(MetaEntity(KEY_INSTALLED_AT, now().toString()))
                when {
                    origin == CatalogOrigin.BUNDLED -> meta.put(MetaEntity(KEY_RULESETS, ""))
                    !file.rulesets.isNullOrEmpty() ->
                        meta.put(MetaEntity(KEY_RULESETS, json.encodeToString(RulesetFile.serializer(), RulesetFile(file.rulesets))))
                }
            }
        }
        refresh()
    }

    /** Goes back to the sample that ships with the app, including its rules. */
    suspend fun restoreBundled(bundledCatalogJson: String) = install(check(bundledCatalogJson).file, CatalogOrigin.BUNDLED, null)

    suspend fun selectRuleset(id: String) {
        val rules = _rulesets.value.firstOrNull { it.id == id } ?: return
        db.metaDao().put(MetaEntity(KEY_RULESET, id))
        _ruleset.value = rules
    }

    private suspend fun refresh() {
        val meta = db.metaDao()
        _cards.value = db.catalogDao().cards().associate { it.cardNumber to it.toModel() }
        _printings.value = db.catalogDao().printings().map { it.toModel() }.groupBy { it.cardNumber }
        val installed = meta.get(KEY_CATALOG).orEmpty()
        val info = CatalogInfo(
            catalogId = installed.substringBefore('@'),
            version = installed.substringAfter('@', "0").toIntOrNull() ?: 0,
            name = meta.get(KEY_NAME).orEmpty(),
            source = meta.get(KEY_SOURCE).orEmpty(),
            origin = meta.get(KEY_ORIGIN)?.let { runCatching { CatalogOrigin.valueOf(it) }.getOrNull() } ?: CatalogOrigin.BUNDLED,
            updateUrl = meta.get(KEY_URL)?.takeIf { it.isNotBlank() },
            installedAt = meta.get(KEY_INSTALLED_AT)?.toLongOrNull(),
            cardCount = _cards.value.size,
        )
        _info.value = info
        _catalogName.value = info.name
        _updateAvailable.value = null
        _showArtwork.value = meta.get(KEY_SHOW_ARTWORK) != "false"

        val rulesJson = meta.get(KEY_RULESETS)?.takeIf { it.isNotBlank() } ?: bundledRulesetsJson
        val rules = runCatching { json.decodeFromString(RulesetFile.serializer(), rulesJson).rulesets }.getOrDefault(emptyList())
        _rulesets.value = rules
        val chosen = meta.get(KEY_RULESET)
        _ruleset.value = rules.firstOrNull { it.id == chosen } ?: rules.firstOrNull() ?: FALLBACK_RULESET
    }

    companion object {
        private const val KEY_CATALOG = "catalog"
        private const val KEY_NAME = "catalogName"
        private const val KEY_SOURCE = "catalogSource"
        private const val KEY_ORIGIN = "catalogOrigin"
        private const val KEY_URL = "catalogUrl"
        private const val KEY_INSTALLED_AT = "catalogInstalledAt"
        private const val KEY_RULESETS = "rulesetsJson"
        private const val KEY_RULESET = "ruleset"
        private const val KEY_SHOW_ARTWORK = "showArtwork"
        private const val KEY_CHECKED_AT = "catalogCheckedAt"
        val FALLBACK_RULESET = Ruleset(id = "standard", name = "Standard", region = "EN", effectiveDate = "")
    }
}

private fun Card.toEntity() = CardEntity(
    cardNumber, name, type.name, colors.joinToString(",") { it.name }, level, cost, ap, hp, text, correctedText,
    traits.joinToString("|"), zones.joinToString("|"), link, setCode, rarity,
)

private fun CardEntity.toModel() = Card(
    cardNumber = cardNumber,
    name = name,
    type = CardType.parse(type) ?: CardType.UNIT,
    colors = colors.split(',').mapNotNull { CardColor.parse(it) },
    level = level, cost = cost, ap = ap, hp = hp,
    text = text, correctedText = correctedText,
    traits = traits.split('|').filter { it.isNotBlank() },
    zones = zones.split('|').filter { it.isNotBlank() },
    link = link, setCode = setCode, rarity = rarity,
)

private fun Printing.toEntity() = PrintingEntity(printingId, cardNumber, setCode, rarity, language, label, isAlternateArt, artworkUrl)

private fun PrintingEntity.toModel() = Printing(printingId, cardNumber, setCode, rarity, language, label, isAlternateArt, artworkUrl)
