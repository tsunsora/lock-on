package app.lockon.catalog

import app.lockon.data.CatalogFile
import app.lockon.model.Card
import app.lockon.model.CardColor
import app.lockon.model.CardType
import app.lockon.model.Printing
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * The unofficial gcg-api dataset (https://gcgapi.com), refreshed weekly from the official card list.
 * Each device downloads it directly and converts it here, so Lock On never hosts or redistributes
 * card data. The data is under ODbL 1.0 and must be credited ([ATTRIBUTION]); card text, images and
 * trademarks remain Bandai's.
 */
object GcgApi {
    const val CATALOG_ID = "gcg-api-en"
    const val MANIFEST_URL = "https://raw.githubusercontent.com/yzRobo/gcg-api/main/data/manifest.json"
    const val CARDS_URL = "https://raw.githubusercontent.com/yzRobo/gcg-api/main/data/cards.json"
    const val ATTRIBUTION = "Contains data from gcg-api (https://gcgapi.com), made available under the Open Database License (ODbL) v1.0. " +
        "Not affiliated with Bandai. Gundam, card text and card images are copyright Bandai."

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Serializable
    data class Manifest(
        @SerialName("dataset_version") val datasetVersion: String,
        @SerialName("built_at") val builtAt: String = "",
        @SerialName("card_count") val cardCount: Int = 0,
        val disclaimer: String = "",
    ) {
        /** The publish run number at the start of `dataset_version` ("30-5a59…" → 30). */
        val run: Int get() = datasetVersion.substringBefore('-').toIntOrNull() ?: 0
    }

    @Serializable
    data class Entry(
        @SerialName("product_id") val productId: String,
        @SerialName("card_number") val cardNumber: String,
        val name: String = "",
        @SerialName("set_code") val setCode: String = "",
        val rarity: String = "",
        @SerialName("card_type") val cardType: String = "",
        val color: String? = null,
        val level: Int? = null,
        val cost: Int? = null,
        val ap: Int? = null,
        val hp: Int? = null,
        val zone: String? = null,
        val link: String? = null,
        val traits: List<String> = emptyList(),
        @SerialName("link_refs") val linkRefs: List<String> = emptyList(),
        val effect: String? = null,
        @SerialName("where_to_get") val whereToGet: String? = null,
        @SerialName("image_url") val imageUrl: String? = null,
    )

    fun parseManifest(text: String): Manifest = json.decodeFromString(Manifest.serializer(), text)

    fun parseCards(text: String): List<Entry> = json.decodeFromString(ListSerializer(Entry.serializer()), text)

    /** Summary of what conversion left out, for the preview. */
    data class Converted(val catalog: CatalogFile, val skippedUnknown: Int)

    fun toCatalog(manifest: Manifest, entries: List<Entry>): Converted {
        // Tokens (Unit tokens, EX Base, EX Resource) are kept for the card list; decks leave them out.
        val (playable, unknown) = entries.partition { CardType.parse(it.cardType) != null }
        val byNumber = playable.groupBy { it.cardNumber }
        val cards = byNumber.map { (number, printings) ->
            // Gameplay comes from the standard printing; later reprints can carry newer wording.
            val base = printings.firstOrNull { it.productId == number } ?: printings.first()
            Card(
                cardNumber = number,
                name = base.name,
                type = CardType.parse(base.cardType)!!,
                colors = listOfNotNull(base.color?.let { CardColor.parse(it) }),
                level = base.level,
                cost = base.cost,
                ap = base.ap,
                hp = base.hp,
                text = base.effect.orEmpty().trim(),
                traits = base.traits,
                zones = base.zone.orEmpty().split(' ').filter { it.isNotBlank() && it != "-" },
                link = linkText(base),
                setCode = base.setCode,
                rarity = normalizeRarity(base.rarity),
            )
        }.sortedBy { it.cardNumber }
        val printings = playable.map { entry ->
            val standard = entry.productId == entry.cardNumber
            Printing(
                printingId = entry.productId,
                cardNumber = entry.cardNumber,
                setCode = entry.setCode,
                rarity = normalizeRarity(entry.rarity),
                label = when {
                    standard -> "Standard"
                    entry.rarity.contains('+') -> "Parallel ${normalizeRarity(entry.rarity)}"
                    else -> entry.whereToGet?.substringBefore(" [")?.takeIf { it.isNotBlank() }?.let { "Reprint: $it" } ?: "Reprint"
                },
                isAlternateArt = !standard,
                artworkUrl = entry.imageUrl?.takeIf { it.startsWith("https://") },
            )
        }.sortedBy { it.printingId }
        val catalog = CatalogFile(
            catalogId = CATALOG_ID,
            version = manifest.run,
            name = "Gundam Card Game (EN) · gcg-api",
            source = ATTRIBUTION + if (manifest.builtAt.isNotBlank()) " Dataset built ${manifest.builtAt.take(10)}." else "",
            cards = cards,
            printings = printings,
        )
        return Converted(catalog, unknown.size)
    }

    /** "[Amuro Ray]" stays a pilot name; "(White Base Team) Trait" becomes the trait form analysis reads. */
    private fun linkText(entry: Entry): String? {
        val raw = entry.link?.trim().orEmpty()
        if (raw.isEmpty() || raw == "-") return null
        if (raw.contains("Trait", ignoreCase = true) && entry.linkRefs.isNotEmpty()) {
            return entry.linkRefs.joinToString(" / ") { "Pilot with trait ($it)" }
        }
        return entry.linkRefs.takeIf { it.isNotEmpty() }?.joinToString(" / ") ?: raw.removePrefix("[").removeSuffix("]")
    }

    private fun normalizeRarity(rarity: String) = rarity.replace(" ", "")
}
