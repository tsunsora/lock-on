package app.lockon.desktop

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import app.lockon.ui.theme.LockOnTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalFoundationApi::class)
class AppContextMenuTest {
    private class Text(
        override val cut: (() -> Unit)? = null,
        override val copy: (() -> Unit)? = null,
        override val paste: (() -> Unit)? = null,
        override val selectAll: (() -> Unit)? = null,
    ) : TextContextMenu.TextManager {
        override val selectedText = AnnotatedString("")
        override fun selectWordAtPositionIfNotAlreadySelected(offset: Offset) = Unit
    }

    private fun List<ContextMenuItem>.action(label: String) = filterIsInstance<MenuAction>().first { it.label == label }

    @Test
    fun aFieldWithASelectionOffersEverything() {
        val menu = textMenu(Text(cut = {}, copy = {}, paste = {}, selectAll = {}), {}, {})
        assertEquals(listOf("Undo", "Redo", "", "Cut", "Copy", "Paste", "", "Select all"), menu.map { it.label })
        assertNotNull(menu.action("Cut").action)
    }

    @Test
    fun aFieldWithNothingSelectedGreysCutAndCopy() {
        val menu = textMenu(Text(paste = {}, selectAll = {}), {}, {})
        assertNull(menu.action("Cut").action)
        assertNull(menu.action("Copy").action)
        assertNotNull(menu.action("Paste").action)
        assertNotNull(menu.action("Undo").action)
    }

    @Test
    fun textThatCantBeEditedOffersOnlyCopyAndSelectAll() {
        val menu = textMenu(Text(copy = {}, selectAll = {}), {}, {})
        assertEquals(listOf("Copy", "", "Select all"), menu.map { it.label })
    }

    /** Writes the menu to build/context-menu/, in both themes with a row hovered, to look at. */
    @Test
    fun renderPreview() {
        val dir = File("build/context-menu").apply { mkdirs() }
        for (dark in listOf(true, false)) {
            val menu = textMenu(Text(paste = {}, selectAll = {}), {}, {})
            ImageComposeScene(600, 640, Density(2f)) {
                LockOnTheme(dark = dark) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MenuPanel(menu) {} }
                    }
                }
            }.use { scene ->
                scene.render(0)
                val row = Offset(300f, 120f)
                scene.sendPointerEvent(PointerEventType.Enter, row)
                scene.sendPointerEvent(PointerEventType.Move, row)
                var image = scene.render(16_000_000)
                for (i in 2..40) image = scene.render(i * 16_000_000L)
                File(dir, if (dark) "dark.png" else "light.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
    }
}
