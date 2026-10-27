package app.lockon

import app.lockon.io.DeckListFormat
import app.lockon.io.ExportOrder
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.platform.encodeShareQr
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The deck image's QR code holds a list that scans back and imports as the same deck. */
class ShareQrTest {
    private val cards = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        .decodeFromString(app.lockon.data.CatalogFile.serializer(), java.io.File("src/commonMain/composeResources/files/catalog/sample-catalog.json").readText())
        .cards.associateBy { it.cardNumber }
    private val deck = DeckSnapshot(
        name = "Blue Red Aggro · ü",
        entries = cards.keys.sorted().take(12).map { DeckEntry(it, 4) } + DeckEntry(cards.keys.sorted()[12], 2) +
            DeckEntry(cards.keys.sorted().last(), 10, DeckSection.RESOURCE) +
            DeckEntry(cards.keys.sorted()[13], 1, DeckSection.CONSIDERING),
    )

    @Test fun theQrScansBackToADeckThatImports() {
        val text = DeckListFormat.export(deck, cards, ExportOrder.NUMBER, compact = true)
        val qr = encodeShareQr(text)
        // Draw it with a quiet zone, four pixels a module, and read it like a camera would.
        val scale = 4
        val side = (qr.size + 8) * scale
        val pixels = IntArray(side * side) { i ->
            val x = i % side / scale - 4
            val y = i / side / scale - 4
            if (x in 0 until qr.size && y in 0 until qr.size && qr[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val read = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(side, side, pixels)))).text
        assertEquals(text, read)
        val imported = DeckListFormat.parse(read, cards).snapshot
        assertEquals(deck.name, imported.name)
        assertEquals(deck.entries.filter { it.section != DeckSection.CONSIDERING }.toSet(), imported.entries.toSet(),
            "the QR carries the main and resource decks, not Considering")
    }

    @Test fun everyOrderReadsBackToTheSameDeck() {
        for (order in ExportOrder.entries) {
            val text = DeckListFormat.export(deck, cards, order)
            assertEquals(deck.entries.toSet(), DeckListFormat.parse(text, cards).snapshot.entries.toSet(), "order $order")
        }
        // Every order lists all the Units, then Pilots, Commands and Bases, each under its own line.
        val lines = DeckListFormat.export(deck, cards, ExportOrder.LEVEL).lines()
        val headers = lines.filter { it.startsWith("// ") && it.contains("s (") }.map { it.removePrefix("// ").substringBefore(" (") }
        assertEquals(listOf("Units", "Pilots", "Commands", "Bases"), headers.filter { it in setOf("Units", "Pilots", "Commands", "Bases") })
        val units = lines.dropWhile { !it.startsWith("// Units") }.drop(1).takeWhile { !it.startsWith("//") }
            .map { cards.getValue(it.split(" ")[1]).level ?: 0 }
        assertEquals(units.sorted(), units, "units run by level")
    }
}
