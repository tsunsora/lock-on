package app.lockon.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.lockon.ui.components.GameIcons
import app.lockon.ui.components.GameMenu
import app.lockon.ui.components.GameMenuDivider
import app.lockon.ui.components.GameMenuItem
import app.lockon.ui.components.GlassIconButton
import app.lockon.ui.components.PillChip
import app.lockon.ui.components.SkyBackground
import app.lockon.ui.theme.LockOnTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Renders an open sort menu and deck-actions menu to build/screenshots/menus-*.png. */
class GameMenuScreenshotTest {
    private val out = File("build/screenshots").apply { mkdirs() }

    @Test fun renderMenus() {
        for (dark in listOf(true, false)) {
            ImageComposeScene(900, 640, Density(1.5f)) {
                LockOnTheme(dark) {
                    SkyBackground(Modifier.fillMaxSize()) {
                        Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(260.dp)) {
                            Box {
                                PillChip(selected = false, onClick = {}, label = { Text("Sort: Number") })
                                GameMenu(expanded = true, onDismissRequest = {}) {
                                    listOf("Number", "Name", "Level", "Cost").forEach { GameMenuItem(it, onClick = {}, selected = it == "Number") }
                                }
                            }
                            Box {
                                GlassIconButton(GameIcons.Deck, "Deck actions", {})
                                GameMenu(expanded = true, onDismissRequest = {}) {
                                    listOf("Rename", "Move to folder", "Edit tags", "Duplicate", "Export list, image or sheet").forEach { GameMenuItem(it, onClick = {}) }
                                    GameMenuDivider()
                                    GameMenuItem("Delete", onClick = {}, icon = GameIcons.Deck, danger = true)
                                }
                            }
                        }
                    }
                }
            }.use { scene ->
                var image = scene.render()
                repeat(5) { frame -> Thread.sleep(40); image = scene.render((frame + 1) * 200_000_000L) }
                File(out, "menus-${if (dark) "dark" else "light"}.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
        assertTrue(File(out, "menus-light.png").length() > 0)
    }
}
