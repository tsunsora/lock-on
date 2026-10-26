package app.lockon

import app.lockon.model.Printing
import app.lockon.ui.components.ArtworkSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArtworkSourceTest {
    private val printings = mapOf(
        "C-1" to listOf(
            Printing("C-1-ALT", "C-1", "S", "R+", isAlternateArt = true, artworkUrl = "https://x/alt.png"),
            Printing("C-1", "C-1", "S", "R", artworkUrl = "https://x/std.png"),
            Printing("C-1-JP", "C-1", "S", "R", language = "ja"),
        ),
        "C-2" to listOf(Printing("C-2", "C-2", "S", "C")),
    )

    @Test fun prefersTheChosenPrintingThenTheStandardOne() {
        val source = ArtworkSource(enabled = true, printings = printings)
        assertEquals("https://x/std.png", source.urlFor("C-1"))
        assertEquals("https://x/alt.png", source.urlFor("C-1", "C-1-ALT"))
        // A chosen printing without artwork falls back instead of showing nothing.
        assertEquals("https://x/std.png", source.urlFor("C-1", "C-1-JP"))
        assertNull(source.urlFor("C-2"))
        assertNull(source.urlFor("missing"))
    }

    @Test fun turnedOffShowsNoArtwork() {
        assertNull(ArtworkSource(enabled = false, printings = printings).urlFor("C-1"))
    }
}
