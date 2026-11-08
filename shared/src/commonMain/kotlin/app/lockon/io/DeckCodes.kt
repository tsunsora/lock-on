package app.lockon.io

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlin.io.encoding.Base64

/**
 * Decks other sites hand out as data rather than as a text list, turned into a list [DeckListFormat]
 * reads:
 *
 * - whitebase.dev deck links (`whitebase.dev/db/deckview?N4Ig…`) and their bare codes, which are
 *   lz-string compressed JSON.
 * - ExBurst's JSON export (`{"slots": [["GD01-001", {"count": 4}]], "sideboardSlots": […]}`).
 * - Simulator JSON such as `{"deckName": …, "deck": [{"id": "GD01-001", "qty": 4}]}`, also when it
 *   is base64 encoded for a link.
 * - A JSON array of card numbers, one per copy, as Tabletop Simulator decks are written.
 */
object DeckCodes {
    private val json = Json { isLenient = true }

    /** The deck in [text] as a deck list, or null when [text] is none of the formats above. */
    fun toDeckList(text: String): String? {
        val input = text.removePrefix("﻿").trim()
        if (input.isEmpty()) return null
        if (input.startsWith("{") || input.startsWith("[")) return fromJson(input)
        whitebaseCode(input)?.let { code -> LzString.decompressFromEncodedUriComponent(code)?.let { fromJson(it) }?.let { return it } }
        if (base64.matches(input)) {
            val decoded = runCatching { Base64.decode(input.trimEnd('=').padBase64()).decodeToString() }.getOrNull()
            if (decoded != null && (decoded.startsWith("{") || decoded.startsWith("["))) fromJson(decoded)?.let { return it }
        }
        return null
    }

    private val base64 = Regex("""^[A-Za-z0-9+/]{16,}={0,2}$""")
    private val lzCode = Regex("""^[A-Za-z0-9+\-$]{12,}$""")

    private fun String.padBase64() = this + "=".repeat((4 - length % 4) % 4)

    /** The code of a whitebase.dev link, or [input] itself when it looks like a bare code. */
    private fun whitebaseCode(input: String): String? {
        if (input.any { it.isWhitespace() }) return null
        val query = input.substringAfter('?', missingDelimiterValue = "")
        val code = when {
            "whitebase" in input.lowercase() -> query.substringBefore('#').substringBefore('&')
            input.contains('/') || input.contains('?') -> return null
            else -> input
        }
        // Links put a space where the code has a plus, as lz-string expects.
        return code.replace(' ', '+').takeIf { lzCode.matches(it) }
    }

    private fun fromJson(input: String): String? {
        val root = runCatching { json.parseToJsonElement(input) }.getOrNull() ?: return null
        val sections = linkedMapOf<String, MutableList<Pair<String, Int>>>()
        var name: String? = null
        fun add(header: String, element: JsonElement?) {
            val cards = cardsIn(element)
            if (cards.isNotEmpty()) sections.getOrPut(header) { mutableListOf() } += cards
        }
        when (root) {
            is JsonArray -> add(MAIN, root)
            is JsonObject -> {
                // A Lock On backup has its own reader.
                if ((root["format"] as? JsonPrimitive)?.content == Backup.FORMAT) return null
                name = listOf("deckName", "name", "n").firstNotNullOfOrNull { (root[it] as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }
                for ((key, value) in root) {
                    when (key) {
                        // whitebase.dev: {"d": {"deck": {...}}}, or in its first version {"d": {...}, "s": {...}}.
                        "d" -> if (value is JsonObject && value.values.all { it is JsonPrimitive }) add(MAIN, value)
                            else (value as? JsonObject)?.forEach { (deck, cards) -> add(headerFor(deck) ?: SIDE, cards) }
                        "s" -> add(SIDE, value)
                        else -> headerFor(key)?.let { add(it, value) }
                    }
                }
            }
            else -> return null
        }
        if (sections.isEmpty()) return null
        return buildString {
            name?.trim()?.takeIf { it.isNotEmpty() }?.let { appendLine("# $it") }
            for ((header, cards) in sections) {
                appendLine("// $header")
                for ((id, count) in cards) appendLine("$count $id")
            }
        }
    }

    private const val MAIN = "Main deck"
    private const val RESOURCE = "Resource deck"
    private const val SIDE = "Side deck"

    private fun headerFor(key: String): String? = when (key.lowercase().replace("_", "").replace("-", "")) {
        "slots", "mainslots", "deck", "main", "maindeck", "cards", "list" -> MAIN
        "resource", "resources", "resourcedeck", "resourceslots" -> RESOURCE
        "sideboardslots", "sideboard", "side", "sidedeck", "sideslots", "maybeboard" -> SIDE
        else -> null
    }

    /** Card numbers and counts from a list of copies, of pairs, of objects, or a number-to-count map. */
    private fun cardsIn(element: JsonElement?): List<Pair<String, Int>> = when (element) {
        is JsonObject -> element.mapNotNull { (id, value) -> countOf(value)?.let { cardId(id)?.to(it) } }
        is JsonArray -> {
            val counts = linkedMapOf<String, Int>()
            for (item in element) {
                val (id, count) = when (item) {
                    is JsonPrimitive -> (if (item.isString) item.content else null) to 1
                    // ExBurst: ["GD01-001", {"count": 4}]
                    is JsonArray -> (item.getOrNull(0) as? JsonPrimitive)?.content to countOf(item.getOrNull(1))
                    is JsonObject -> listOf("id", "cardno", "cardNo", "cardNumber", "cardId", "code", "number").firstNotNullOfOrNull {
                        (item[it] as? JsonPrimitive)?.content
                    } to (countOf(item) ?: 1)
                    else -> null to null
                }
                val card = id?.let(::cardId) ?: continue
                if (count != null && count > 0) counts[card] = (counts[card] ?: 0) + count
            }
            counts.toList()
        }
        else -> emptyList()
    }

    private fun countOf(element: JsonElement?): Int? = when (element) {
        is JsonPrimitive -> element.intOrNull
        is JsonObject -> listOf("count", "qty", "quantity", "amount", "copies").firstNotNullOfOrNull { (element[it] as? JsonPrimitive)?.intOrNull }
        else -> null
    }

    /** Card numbers as [DeckListFormat] reads them; anything else in the data is dropped. */
    private fun cardId(raw: String): String? = raw.trim().takeIf { DeckListFormat.isCardId(it) }
}

/**
 * The decompressing half of lz-string's URI-safe encoding, which whitebase.dev uses for deck links.
 * Returns null for anything that is not a valid code.
 */
internal object LzString {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-$"

    /** A deck is a few hundred characters; this stops a hostile code from filling memory. */
    private const val MAX_LENGTH = 200_000

    fun decompressFromEncodedUriComponent(input: String): String? {
        val values = IntArray(input.length) { i -> ALPHABET.indexOf(input[i]).also { if (it < 0) return null } }
        return runCatching { decompress(values.size, 32) { i -> if (i < values.size) values[i] else 0 } }.getOrNull()
    }

    private fun decompress(length: Int, resetValue: Int, next: (Int) -> Int): String? {
        val dictionary = mutableListOf<String>("0", "1", "2")
        var enlargeIn = 4
        var numBits = 3
        var value = next(0)
        var position = resetValue
        var index = 1

        fun read(bitCount: Int): Int {
            var bits = 0
            for (power in 0 until bitCount) {
                val bit = value and position
                position = position shr 1
                if (position == 0) {
                    position = resetValue
                    value = next(index++)
                }
                if (bit > 0) bits = bits or (1 shl power)
            }
            return bits
        }

        val first = when (read(2)) {
            0 -> read(8).toChar().toString()
            1 -> read(16).toChar().toString()
            else -> return ""
        }
        dictionary += first
        var w = first
        val result = StringBuilder(first)
        while (true) {
            if (index > length) return ""
            var c = read(numBits)
            when (c) {
                0, 1 -> {
                    dictionary += read(if (c == 0) 8 else 16).toChar().toString()
                    c = dictionary.size - 1
                    enlargeIn--
                }
                2 -> return result.toString()
            }
            if (enlargeIn == 0) {
                enlargeIn = 1 shl numBits
                numBits++
            }
            val entry = when {
                c < dictionary.size && c > 2 -> dictionary[c]
                c == dictionary.size -> w + w[0]
                else -> return null
            }
            result.append(entry)
            if (result.length > MAX_LENGTH) return null
            dictionary += w + entry[0]
            enlargeIn--
            w = entry
            if (enlargeIn == 0) {
                enlargeIn = 1 shl numBits
                numBits++
            }
        }
    }
}
