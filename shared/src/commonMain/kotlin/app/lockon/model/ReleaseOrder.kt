package app.lockon.model

/**
 * Card numbers in release order: by the set in the number's prefix (GD01 in GD01-001), oldest set
 * first, then by number within the set. Plain text order would put EB01 before ST01 and every
 * starter deck after every booster.
 */
object ReleaseOrder {
    /**
     * Sets by English release date (gcg-api products.json). Sets out on the same day keep their
     * code order. Newer sets not listed yet come after these, then promos, resources and tokens.
     */
    private val released = listOf(
        "ST01", "ST02", "ST03", "ST04", // 2025-07-11
        "GD01", // 2025-07-25
        "ST05", // 2025-09-26
        "GD02", "ST06", // 2025-10-24
        "ST07", "ST08", // 2026-01-16
        "GD03", // 2026-01-30
        "ST09", // 2026-03-27
        "GD04", // 2026-04-24
        "EB01", "ST10", // 2026-06-26
        "GD05", "SC01", // 2026-07-24
        "ST11", "ST12", "ST13", "ST14", // 2026-09-25
        "GD06", // 2026-10-30
        "GD07", // 2027-01-29
        "EB02", // 2027-02-26
        "ST15", // 2027-04-16
    ).withIndex().associate { (index, set) -> set to index }

    /** The set part of a card number, such as GD01 for GD01-001 or T for T-014. */
    fun setCode(cardNumber: String): String = cardNumber.substringBefore('-')

    /** Listed sets, then unlisted numbered sets (newer releases), then sets without a number. */
    private fun rank(set: String): Int = released[set] ?: if (set.any { it.isDigit() }) released.size else released.size + 1

    /** Each side's set is cut out once per comparison; sorting the whole catalog runs this tens of thousands of times. */
    val comparator: Comparator<String> = Comparator { a, b ->
        val setA = setCode(a)
        val setB = setCode(b)
        compareValues(rank(setA), rank(setB)).takeIf { it != 0 } ?: setA.compareTo(setB).takeIf { it != 0 } ?: a.compareTo(b)
    }
}
