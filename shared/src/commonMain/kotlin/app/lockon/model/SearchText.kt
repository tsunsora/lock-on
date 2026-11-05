package app.lockon.model

/**
 * Reduces text to what someone can type: Üso Ewin reads "uso ewin", Zaku Ⅱ reads "zaku ii",
 * ∀ Gundam reads "turn a gundam" and Z’Gok E reads "z'gok e". Both sides of a comparison go through
 * this, so pasting the printed spelling still works.
 */
internal object SearchText {
    private val replacements: Map<Char, String> = buildMap {
        fun letters(chars: String, plain: String) = chars.forEach { put(it, plain) }
        letters("àáâãäåāăą", "a"); letters("æ", "ae"); letters("çćĉċč", "c"); letters("ðďđ", "d")
        letters("èéêëēĕėęě", "e"); letters("ĝğġģ", "g"); letters("ĥħ", "h"); letters("ìíîïĩīĭįı", "i")
        letters("ĵ", "j"); letters("ķ", "k"); letters("ĺļľŀł", "l"); letters("ñńņň", "n")
        letters("òóôõöøōŏő", "o"); letters("œ", "oe"); letters("ŕŗř", "r"); letters("śŝşš", "s")
        letters("ß", "ss"); letters("ţťŧ", "t"); letters("þ", "th"); letters("ùúûüũūŭůűų", "u")
        letters("ŵ", "w"); letters("ýÿŷ", "y"); letters("źżž", "z")
        // Names printed with a symbol, spelled the way players say them. Lowercase, as Ξ and Ⅱ have cases.
        put('ξ', "xi"); put('ν', "nu"); put('∀', "turn a")
        listOf("i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x", "xi", "xii")
            .forEachIndexed { index, numeral -> put('ⅰ' + index, numeral) }
        letters("‘’ʼ", "'"); letters("“”", "\""); letters("‐‑–—", "-")
    }

    fun fold(text: String): String = buildString(text.length) {
        for (char in text) {
            when {
                char.code < 0x80 -> append(char.lowercaseChar())
                isInvisible(char) -> Unit
                else -> {
                    val lower = char.lowercaseChar()
                    append(replacements[lower] ?: lower.toString())
                }
            }
        }
    }

    /** Letters and digits only, so "mkii", "mk ii" and "Mk-Ⅱ" are all one spelling. Takes [fold]ed text. */
    fun compact(folded: String): String = folded.filter { it.isLetterOrDigit() }

    /** Zero-width characters (one card name has a stray one) and the accents of decomposed letters. */
    private fun isInvisible(char: Char) = char in '​'..'‍' || char == '­' || char == '﻿' || char in '̀'..'ͯ'
}

/** A card's searchable text, worked out on the first search and kept, as typing checks every card per letter. */
internal class CardSearchText(card: Card) {
    private val everything = SearchText.fold(listOf(card.name, card.cardNumber, card.traits.joinToString(" "), card.displayText).joinToString("\n"))
    private val runs = listOf(card.name, card.cardNumber, card.traits.joinToString(" ")).flatMap { compactRuns(SearchText.fold(it)) }

    /**
     * Anywhere as written, or ignoring spaces and punctuation from the start of a word of the name,
     * number or traits. Only from a word's start, or "is" would find "Zaku III Sleeves"; never in
     * rules text, where "uso" would find "thus o…"; and not for a lone character, or "+1" would find
     * every 1.
     */
    fun contains(folded: String, compact: String): Boolean =
        everything.contains(folded) || (compact.length >= 2 && runs.any { it.startsWith(compact) })

    /** "z'gok e" gives "zgoke" and "e"; an apostrophe doesn't start a word, so "chars" finds Char's. */
    private fun compactRuns(folded: String): List<String> = folded.indices.filter { index ->
        folded[index].isLetterOrDigit() && (index == 0 || !(folded[index - 1].isLetterOrDigit() || folded[index - 1] == '\''))
    }.map { SearchText.compact(folded.substring(it)) }
}
