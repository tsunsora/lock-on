package app.lockon.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.lockon.model.Card
import app.lockon.model.CardColor
import app.lockon.model.CardType
import app.lockon.ui.components.CardImage
import app.lockon.ui.components.DeckStrip
import app.lockon.ui.components.LineIcons
import app.lockon.ui.theme.LockOnTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * The drawn faces of cards without artwork (UX rule V4), as the sample catalog shows them: one of
 * each type and colour at grid size, a long name at copy limit, and one card at every size a face is
 * drawn at, from card details down to a deck row's thumbnail. Writes both themes to build/screenshots.
 */
class PlaceholderCardScreenshotTest {
    private val cards = listOf(
        Card("SMP-002", "Bastion Heavy Armor", CardType.UNIT, listOf(CardColor.BLUE), level = 4, cost = 3, ap = 3, hp = 5, setCode = "SMP"),
        Card("SMP-005", "Captain Mira Sol", CardType.PILOT, listOf(CardColor.BLUE), level = 4, cost = 1, ap = 1, hp = 1, setCode = "SMP"),
        Card("SMP-013", "Scorched Earth", CardType.COMMAND, listOf(CardColor.RED), level = 4, cost = 2, setCode = "SMP"),
        Card("SMP-014", "Forward Assault Base", CardType.BASE, listOf(CardColor.RED), level = 2, cost = 1, hp = 3, setCode = "SMP"),
        Card("SMP-015", "Verdant Sentinel", CardType.UNIT, listOf(CardColor.GREEN), level = 3, cost = 2, ap = 2, hp = 4, setCode = "SMP"),
        Card("SMP-017", "Halo Paladin", CardType.UNIT, listOf(CardColor.WHITE), level = 4, cost = 3, ap = 3, hp = 4, setCode = "SMP"),
        Card("SMP-021", "Twin Drive Vanguard", CardType.UNIT, listOf(CardColor.PURPLE, CardColor.BLUE), level = 5, cost = 4, ap = 4, hp = 4, setCode = "SMP"),
        Card("SMP-019", "Standard Resource", CardType.RESOURCE, setCode = "SMP"),
    )

    @Test fun renderBothThemes() {
        listOf(false, true).forEach { dark -> render(dark) }
    }

    private fun render(dark: Boolean) {
        ImageComposeScene(1680, 980, Density(1.5f)) {
            LockOnTheme(dark = dark) {
                Column(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        cards.forEachIndexed { i, card -> CardImage(card, Modifier.width(118.dp), shadow = false, max = i == 3) }
                    }
                    // The type icons at filter chip size, as the card list shows them.
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        listOf(CardType.UNIT, CardType.PILOT, CardType.COMMAND, CardType.BASE, CardType.RESOURCE).forEach {
                            Icon(LineIcons.forType(it), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
                        val card = cards.first()
                        listOf(220, 160, 95, 64, 40).forEach { CardImage(card, Modifier.width(it.dp)) }
                        Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            cards.take(3).forEach { DeckStrip(it, 4, null, onClick = {}, subtitle = "Lv ${it.level}") {} }
                        }
                    }
                }
            }
        }.use { scene ->
            var image = scene.render()
            repeat(3) { image = scene.render((it + 1) * 100_000_000L) }
            val name = if (dark) "placeholder-cards-dark.png" else "placeholder-cards-light.png"
            File(File("build/screenshots").apply { mkdirs() }, name).writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }
}
