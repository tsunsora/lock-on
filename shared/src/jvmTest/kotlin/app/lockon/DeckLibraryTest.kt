package app.lockon

import androidx.room.Room
import app.lockon.data.DeckRepository
import app.lockon.data.db.LockOnDatabase
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.MatchRecord
import app.lockon.model.MatchResult
import app.lockon.sync.RevisionGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Folders, versions and the match log on one device. */
class DeckLibraryTest {
    private var clock = 1_000L
    private val db = LockOnDatabase.build(Room.inMemoryDatabaseBuilder<LockOnDatabase>())
    private val decks = DeckRepository(db, "phone") { clock++ }

    @AfterTest fun close() = db.close()

    private suspend fun folderOf(id: String) = decks.deck(id).first()!!.snapshot.folder

    @Test fun foldersReuseAnExistingSpelling() = runTest {
        val a = decks.create(DeckSnapshot(name = "A"))
        val b = decks.create(DeckSnapshot(name = "B"))
        decks.moveToFolder(listOf(a), " Tournament ")
        decks.moveToFolder(listOf(b), "tournament")
        assertEquals("Tournament", folderOf(a))
        assertEquals("Tournament", folderOf(b))

        decks.moveToFolder(listOf(b), "")
        assertEquals(null, folderOf(b))
    }

    @Test fun renamingAndRemovingAFolderTouchesOnlyItsDecks() = runTest {
        val a = decks.create(DeckSnapshot(name = "A", folder = "Locals"))
        val b = decks.create(DeckSnapshot(name = "B", folder = "Locals"))
        val c = decks.create(DeckSnapshot(name = "C", folder = "Testing"))

        decks.renameFolder("Locals", "Store")
        assertEquals(listOf("Store", "Store", "Testing"), listOf(folderOf(a), folderOf(b), folderOf(c)))

        // Renaming onto another folder's name merges them, keeping that folder's spelling.
        decks.renameFolder("Store", "testing")
        assertEquals("Testing", folderOf(a))

        val removed = decks.removeFolder("Testing")
        assertEquals(setOf(a, b, c), removed.toSet())
        assertEquals(null, folderOf(c))
    }

    @Test fun editsFoldIntoOneVersionUntilKeptOrIdle() = runTest {
        val id = decks.create(DeckSnapshot(name = "Wing"))
        val one = decks.currentSnapshot(id)!!.withQuantity("SMP-001", DeckSection.MAIN, 1)
        decks.save(id, one)
        decks.save(id, one.withQuantity("SMP-001", DeckSection.MAIN, 2))
        assertEquals(1, decks.history(id).size, "autosaves in one session are one version")

        decks.keepVersion(id, decks.currentSnapshot(id)!!)
        decks.save(id, decks.currentSnapshot(id)!!.withQuantity("SMP-002", DeckSection.MAIN, 4))
        assertEquals(2, decks.history(id).size)

        decks.keepVersion(id, decks.currentSnapshot(id)!!)
        decks.keepVersion(id, decks.currentSnapshot(id)!!)
        assertEquals(2, decks.history(id).size, "keeping an unchanged deck adds no version")

        clock += DeckRepository.VERSION_IDLE_MS
        decks.save(id, decks.currentSnapshot(id)!!.copy(notes = "after a break"))
        val history = decks.history(id)
        assertEquals(3, history.size)
        assertEquals("after a break", history.first().snapshot.notes, "newest first")
        assertEquals(2, history.last().snapshot.quantityOf("SMP-001", DeckSection.MAIN))
    }

    @Test fun keepingAVersionMergesMatchesFromEveryDevice() = runTest {
        val id = decks.create(DeckSnapshot(name = "Zeon"))
        val here = MatchRecord("m1", playedAt = 10, result = MatchResult.WIN)
        val there = MatchRecord("m2", playedAt = 5, result = MatchResult.LOSS)
        decks.resolve(id, decks.currentSnapshot(id)!!.copy(matches = listOf(here, there, here)))
        assertEquals(listOf("m2", "m1"), decks.currentSnapshot(id)!!.matches.map { it.id }, "one copy each, oldest first")
    }

    @Test fun duplicatesStartWithoutMatches() = runTest {
        val id = decks.create(
            DeckSnapshot(name = "Seed", folder = "Ranked", coverCard = "SMP-003",
                matches = listOf(MatchRecord("m", 1, MatchResult.DRAW))),
        )
        val copy = decks.currentSnapshot(decks.duplicate(id)!!)!!
        assertEquals("Ranked", copy.folder)
        assertEquals("SMP-003", copy.coverCard)
        assertTrue(copy.matches.isEmpty())
    }

    @Test fun historyListsChildrenBeforeParents() {
        fun node(id: String, at: Long, vararg parents: String) = RevisionGraph.Node(id, parents.toList(), at, false)
        // Two devices branched from "root"; "merge" joins them. The other device's clock runs ahead.
        val nodes = listOf(
            node("root", 1), node("phone", 2, "root"), node("desktop", 900, "root"), node("merge", 3, "phone", "desktop"),
            node("unrelated", 5),
        )
        assertEquals(listOf("merge", "desktop", "phone", "root"), RevisionGraph.history(nodes, "merge").map { it.revisionId })
    }
}
