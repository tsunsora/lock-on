package app.lockon.model

import kotlinx.serialization.Serializable

/**
 * Deck construction rules for one region and date. Shipped as JSON so restrictions can be updated
 * without an app release.
 */
@Serializable
data class Ruleset(
    val id: String,
    val name: String,
    val region: String,
    val effectiveDate: String,
    val mainDeckSize: Int = 50,
    val resourceDeckSize: Int = 10,
    val maxColors: Int = 2,
    val maxCopies: Int = 4,
    /** Copy limit for resource cards; null means unlimited. */
    val resourceMaxCopies: Int? = null,
    val banned: List<String> = emptyList(),
    /** Card number to the lower copy limit it is restricted to. */
    val restricted: Map<String, Int> = emptyMap(),
    /** Cards that are legal alone but cannot share a deck. */
    val bannedCombinations: List<BannedCombination> = emptyList(),
    val notes: String = "",
)

@Serializable
data class BannedCombination(val cardNumbers: List<String>, val reason: String = "")
