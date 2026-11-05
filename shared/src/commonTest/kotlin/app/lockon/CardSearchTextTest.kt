package app.lockon

import app.lockon.io.DeckListFormat
import app.lockon.model.Card
import app.lockon.model.CardQuery
import app.lockon.model.CardType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CardSearchTextTest {
    private fun card(name: String, text: String = "", traits: List<String> = emptyList(), number: String = "GD01-001") =
        Card(number, name, CardType.UNIT, text = text, traits = traits, setCode = "GD01")

    private fun finds(search: String, card: Card) = CardQuery(text = search).matches(card)

    @Test
    fun namesAreFoundAsTheyCanBeTyped() {
        val spellings = mapOf(
            "Üso Ewin" to listOf("uso", "uso ewin", "USO EWIN", "Üso", "üso"),
            "Kämpfer" to listOf("kampfer", "Kämpfer"),
            "Gundvölva" to listOf("gundvolva"),
            "Shuji Itō" to listOf("shuji ito"),
            "Ξ Gundam" to listOf("xi gundam", "xi", "Ξ"),
            "∀ Gundam" to listOf("turn a gundam", "∀"),
            "Zaku Ⅱ" to listOf("zaku ii", "ZAKU II", "Zaku Ⅱ", "zakuii"),
            "Zaku Ⅰ" to listOf("zaku i"),
            "Gundam Mk-Ⅲ" to listOf("mk-iii", "mk iii", "mkiii"),
            "Z’Gok E" to listOf("z'gok", "zgok", "Z’Gok"),
            "Mouar’s Determination" to listOf("mouar's", "mouars"),
            "Char's Zaku Ⅱ" to listOf("chars zaku ii", "char’s zaku"),
            "Airframe​ Seizure" to listOf("airframe seizure"),
            "Hi-Nu Gundam (EX)" to listOf("hi nu", "hinu"),
            "Zaku (Four Snake Eyes') [YETI] (GQ)" to listOf("yeti", "four snake eyes"),
        )
        for ((name, searches) in spellings) for (search in searches) {
            assertTrue(finds(search, card(name)), "\"$search\" should find $name")
        }
    }

    @Test
    fun numbersAndTraitsIgnorePunctuation() {
        assertTrue(finds("gd01 001", card("Wall")))
        assertTrue(finds("GD01-001", card("Wall")))
        assertTrue(finds("cyber newtype", card("Wall", traits = listOf("Cyber-Newtype"))))
        assertTrue(finds("earth federation newtype", card("Wall", traits = listOf("Earth Federation", "Newtype"))))
    }

    @Test
    fun rulesTextKeepsItsPunctuationAndWordBreaks() {
        val rules = card("Wall", text = "【Burst】Thus one card gets AP+1.", number = "GD01-777")
        assertTrue(finds("【burst】", rules))
        assertTrue(finds("ap+1", rules))
        assertFalse(finds("+2", rules))
        assertFalse(finds("uso", rules), "shouldn't run \"Thus one\" together")
    }

    @Test
    fun searchesStillRuleCardsOut() {
        assertFalse(finds("zaku iii", card("Zaku Ⅱ")))
        assertFalse(finds("uso", card("Kämpfer")))
        assertFalse(finds("is", card("Zaku Ⅲ (Sleeves)")), "shouldn't run \"Ⅲ Sleeves\" together mid-word")
        assertFalse(finds("+1", card("Blue Destiny Unit-1")), "a lone 1 isn't what +1 asks for")
    }

    @Test
    fun punctuationOnItsOwnStillMatchesAsWritten() {
        assertTrue(finds("-", card("Wall")), "the card number has one")
        assertFalse(finds("【", card("Wall")))
    }

    @Test
    fun deckListsTellCardsApartByTheirPunctuation() {
        val cards = listOf(card("GN Armor (Type-E)", number = "GD03-057"), card("GN Armor Type-E", number = "GD04-063")).associateBy { it.cardNumber }
        assertEquals(listOf("GD03-057"), DeckListFormat.parse("1 GN Armor (Type-E)", cards).snapshot.entries.map { it.cardNumber })
        assertEquals(listOf("GD04-063"), DeckListFormat.parse("1 GN Armor Type-E", cards).snapshot.entries.map { it.cardNumber })
    }

    @Test
    fun deckListsCanNameCardsInPlainLetters() {
        val cards = listOf(
            card("Üso Ewin", number = "GD04-081"),
            card("Zaku Ⅱ", number = "GD01-035"),
            card("Z’Gok E", number = "GD03-027"),
            card("Airframe​ Seizure", number = "GD05-111"),
        ).associateBy { it.cardNumber }
        val result = DeckListFormat.parse("2 Uso Ewin\n4 Zaku II\n1 Z'Gok E\n3 Airframe Seizure", cards)
        assertEquals(emptyList(), result.problems)
        assertEquals(
            mapOf("GD04-081" to 2, "GD01-035" to 4, "GD03-027" to 1, "GD05-111" to 3),
            result.snapshot.entries.associate { it.cardNumber to it.quantity },
        )
    }
}
