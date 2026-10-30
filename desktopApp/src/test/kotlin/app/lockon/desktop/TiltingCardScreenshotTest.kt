package app.lockon.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.lockon.model.Card
import app.lockon.model.CardColor
import app.lockon.model.CardType
import app.lockon.ui.components.TiltingCard
import app.lockon.ui.theme.LockOnTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/** Renders the full-size card held flat, turned toward the top-left, and toward the bottom-right, to build/screenshots/tilting-card.png. */
class TiltingCardScreenshotTest {
    @Test fun renderTilts() {
        val card = Card("GD01-001", "Gundam", CardType.UNIT, listOf(CardColor.BLUE), level = 4, cost = 3, ap = 4, hp = 4, setCode = "GD01")
        ImageComposeScene(1000, 520, Density(1.5f)) {
            LockOnTheme(dark = true) {
                Row(
                    Modifier.fillMaxSize().background(Color(0xFF202020)).padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(Offset.Zero, Offset(-1f, -1f), Offset(1f, 1f)).forEach { TiltingCard(card, null, Modifier.width(180.dp), tilt = it) }
                }
            }
        }.use { scene ->
            var image = scene.render()
            repeat(3) { image = scene.render((it + 1) * 100_000_000L) }
            File(File("build/screenshots").apply { mkdirs() }, "tilting-card.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }
}
