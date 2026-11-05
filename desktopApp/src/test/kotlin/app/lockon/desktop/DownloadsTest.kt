package app.lockon.desktop

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DownloadsTest {
    private val folder = Files.createTempDirectory("downloads-test")

    @Test fun aNameAlreadyTakenLeavesThatFileAloneAndNumbersTheNewOne() {
        val first = writeBesideOthers(folder, "My deck.png", byteArrayOf(1))
        val second = writeBesideOthers(folder, "My deck.png", byteArrayOf(2))
        val third = writeBesideOthers(folder, "My deck.png", byteArrayOf(3))

        assertEquals(listOf("My deck.png", "My deck (1).png", "My deck (2).png"), listOf(first, second, third).map { it.fileName.toString() })
        assertContentEquals(byteArrayOf(1), Files.readAllBytes(first))
        assertContentEquals(byteArrayOf(3), Files.readAllBytes(third))
    }

    @Test fun aMissingFolderIsMade() {
        val file = writeBesideOthers(folder.resolve("Downloads"), "deck.png", byteArrayOf(1))
        assertTrue(Files.exists(file))
    }

    @Test fun theDownloadsFolderIsFound() {
        assertTrue(downloadsFolder().isAbsolute)
    }
}
