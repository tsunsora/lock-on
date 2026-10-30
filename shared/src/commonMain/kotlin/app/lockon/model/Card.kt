package app.lockon.model

import kotlinx.serialization.Serializable

@Serializable
enum class CardColor(val label: String) {
    BLUE("Blue"), GREEN("Green"), RED("Red"), WHITE("White"), PURPLE("Purple");

    companion object {
        fun parse(value: String): CardColor? = entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
    }
}

@Serializable
enum class CardType(val label: String) {
    UNIT("Unit"), PILOT("Pilot"), COMMAND("Command"), BASE("Base"), RESOURCE("Resource"),
    UNIT_TOKEN("Unit token"), EX_BASE("EX Base"), EX_RESOURCE("EX Resource");

    /** Resource cards live in the resource deck; everything else goes in the main deck. */
    val isResource: Boolean get() = this == RESOURCE

    /** Created during play, so they're listed with the cards but never go in a deck. */
    val isToken: Boolean get() = this == UNIT_TOKEN || this == EX_BASE || this == EX_RESOURCE

    companion object {
        /** Takes the enum name or gcg-api's spelling ("UNIT TOKEN", "EX BASE"). */
        fun parse(value: String): CardType? = value.trim().replace(' ', '_').let { key -> entries.firstOrNull { it.name.equals(key, ignoreCase = true) } }
    }
}

/**
 * Gameplay identity of a card. Every printing (alternate art, reprints, languages) shares one
 * card number, and deck rules count copies by card number.
 */
@Serializable
data class Card(
    val cardNumber: String,
    val name: String,
    val type: CardType,
    val colors: List<CardColor> = emptyList(),
    val level: Int? = null,
    val cost: Int? = null,
    val ap: Int? = null,
    val hp: Int? = null,
    val text: String = "",
    /** Official errata or clarified wording, shown in place of [text] when present. */
    val correctedText: String? = null,
    val traits: List<String> = emptyList(),
    val zones: List<String> = emptyList(),
    val link: String? = null,
    val setCode: String,
    val rarity: String = "",
) {
    val displayText: String get() = correctedText ?: text
}

@Serializable
data class Printing(
    val printingId: String,
    val cardNumber: String,
    val setCode: String,
    val rarity: String,
    val language: String = "en",
    val label: String = "Standard",
    val isAlternateArt: Boolean = false,
    val artworkUrl: String? = null,
)

/** Filters for the card browser. Empty sets mean "any". */
data class CardQuery(
    val text: String = "",
    val colors: Set<CardColor> = emptySet(),
    val types: Set<CardType> = emptySet(),
    val levels: Set<Int> = emptySet(),
    val costs: Set<Int> = emptySet(),
    val setCodes: Set<String> = emptySet(),
    val trait: String = "",
) {
    val isEmpty: Boolean
        get() = text.isBlank() && colors.isEmpty() && types.isEmpty() && levels.isEmpty() &&
            costs.isEmpty() && setCodes.isEmpty() && trait.isBlank()

    fun matches(card: Card): Boolean {
        if (colors.isNotEmpty() && card.colors.none { it in colors }) return false
        if (types.isNotEmpty() && card.type !in types) return false
        if (levels.isNotEmpty() && card.level !in levels) return false
        if (costs.isNotEmpty() && card.cost !in costs) return false
        if (setCodes.isNotEmpty() && card.setCode !in setCodes) return false
        if (trait.isNotBlank() && card.traits.none { it.contains(trait.trim(), ignoreCase = true) }) return false
        val needle = text.trim()
        if (needle.isNotEmpty()) {
            // Checked one at a time, so most cards are settled without building the trait line.
            val found = card.name.contains(needle, ignoreCase = true) || card.cardNumber.contains(needle, ignoreCase = true) ||
                card.displayText.contains(needle, ignoreCase = true) || card.traits.joinToString(" ").contains(needle, ignoreCase = true)
            if (!found) return false
        }
        return true
    }
}
