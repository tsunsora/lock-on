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

/** A downloadable card catalog. Each device updates its own copy; catalogs are never synced. */
@Serializable
data class CatalogFile(
    val catalogId: String,
    val version: Int,
    val name: String,
    val source: String = "",
    val cards: List<Card>,
    val printings: List<Printing> = emptyList(),
)

@Serializable
data class RulesetFile(val rulesets: List<Ruleset>)

class CatalogRepository(private val db: LockOnDatabase) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _cards = MutableStateFlow<Map<String, Card>>(emptyMap())
    private val _printings = MutableStateFlow<Map<String, List<Printing>>>(emptyMap())
    private val _rulesets = MutableStateFlow<List<Ruleset>>(emptyList())
    private val _ruleset = MutableStateFlow(FALLBACK_RULESET)
    private val _catalogName = MutableStateFlow("")

    val cards: StateFlow<Map<String, Card>> = _cards.asStateFlow()
    val printings: StateFlow<Map<String, List<Printing>>> = _printings.asStateFlow()
    val rulesets: StateFlow<List<Ruleset>> = _rulesets.asStateFlow()
    val ruleset: StateFlow<Ruleset> = _ruleset.asStateFlow()
    val catalogName: StateFlow<String> = _catalogName.asStateFlow()

    /** Installs [catalogJson] when it is newer than the stored catalog, then loads cards into memory. */
    suspend fun load(catalogJson: String, rulesetsJson: String) {
        val file = json.decodeFromString(CatalogFile.serializer(), catalogJson)
        val meta = db.metaDao()
        val installed = "${file.catalogId}@${file.version}"
        if (meta.get(KEY_CATALOG) != installed) {
            db.useWriterConnection { connection ->
                connection.immediateTransaction {
                    db.catalogDao().clearCards()
                    db.catalogDao().clearPrintings()
                    db.catalogDao().insertCards(file.cards.map { it.toEntity() })
                    db.catalogDao().insertPrintings(file.printings.map { it.toEntity() })
                    meta.put(MetaEntity(KEY_CATALOG, installed))
                }
            }
        }
        _catalogName.value = file.name
        _cards.value = db.catalogDao().cards().associate { it.cardNumber to it.toModel() }
        _printings.value = db.catalogDao().printings().map { it.toModel() }.groupBy { it.cardNumber }

        val rules = json.decodeFromString(RulesetFile.serializer(), rulesetsJson).rulesets
        _rulesets.value = rules
        val chosen = meta.get(KEY_RULESET)
        _ruleset.value = rules.firstOrNull { it.id == chosen } ?: rules.firstOrNull() ?: FALLBACK_RULESET
    }

    suspend fun selectRuleset(id: String) {
        val rules = _rulesets.value.firstOrNull { it.id == id } ?: return
        db.metaDao().put(MetaEntity(KEY_RULESET, id))
        _ruleset.value = rules
    }

    companion object {
        private const val KEY_CATALOG = "catalog"
        private const val KEY_RULESET = "ruleset"
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
