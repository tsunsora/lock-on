package app.lockon.data

import app.lockon.data.db.DeckEntity
import app.lockon.data.db.DeckEntryEntity
import app.lockon.data.db.LockOnDatabase
import app.lockon.data.db.RevisionEntity
import app.lockon.data.db.RevisionParentEntity
import app.lockon.io.Backup
import app.lockon.io.BackupDeck
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.DeckSnapshot
import app.lockon.model.DeckSummary
import app.lockon.model.DeckVersion
import app.lockon.sync.RevisionDto
import app.lockon.sync.RevisionGraph
import app.lockon.sync.SyncProtocol
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * Personal decks. Every save becomes a revision with a parent, and the `decks` table is only a
 * cache rebuilt from revision heads, so syncing never has to merge rows in place.
 */
class DeckRepository(
    private val db: LockOnDatabase,
    val localDeviceId: String,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val decksDao = db.deckDao()
    private val revisions = db.revisionDao()

    val decks: Flow<List<DeckSummary>> =
        combine(decksDao.observeDecks(), decksDao.observeAllEntries()) { decks, entries ->
            val byDeck = entries.groupBy { it.deckId }
            decks.map { it.toSummary(byDeck[it.deckId].orEmpty()) }
        }

    fun deck(deckId: String): Flow<DeckSummary?> = decks.map { list -> list.firstOrNull { it.deckId == deckId } }

    suspend fun create(snapshot: DeckSnapshot, deckId: String = Uuid.random().toString()): String {
        transaction {
            insertRevision(deckId, parents = emptyList(), snapshot = snapshot)
            rebuild(deckId)
        }
        return deckId
    }

    /**
     * Saves the deck's new content. Consecutive autosaves fold into one revision until that revision
     * is sent to another device, which keeps history short without ever changing shared data.
     */
    suspend fun save(deckId: String, snapshot: DeckSnapshot) = transaction {
        val deck = decksDao.deck(deckId) ?: error("Deck $deckId does not exist.")
        val head = revisions.revision(deck.headRevisionId)
        val amended = head != null && !deck.hasConflict && head.originDeviceId == localDeviceId && !head.shared &&
            revisions.amend(head.seq, encode(snapshot), snapshot.deleted, now()) == 1
        if (!amended) insertRevision(deckId, parents = listOf(deck.headRevisionId), snapshot = snapshot)
        rebuild(deckId)
    }

    suspend fun delete(deckId: String) {
        val current = currentSnapshot(deckId) ?: return
        save(deckId, current.copy(deleted = true))
    }

    /** Undoes a delete by saving the earlier content on top of the deletion record. */
    suspend fun restore(deckId: String, snapshot: DeckSnapshot) = save(deckId, snapshot.copy(deleted = false))

    suspend fun duplicate(deckId: String): String? {
        val current = currentSnapshot(deckId) ?: return null
        return create(current.copy(name = "${current.name} (copy)"))
    }

    /** All heads of a deck. More than one means devices edited it independently. */
    suspend fun versions(deckId: String): List<DeckVersion> {
        val all = revisions.forDeck(deckId)
        val nodes = nodes(deckId, all)
        val byId = all.associateBy { it.revisionId }
        return RevisionGraph.heads(nodes).map { node ->
            val revision = byId.getValue(node.revisionId)
            DeckVersion(revision.revisionId, revision.originDeviceId, revision.createdAt, decode(revision.snapshotJson))
        }
    }

    /** Ends a conflict with a merge revision that supersedes every head. */
    suspend fun resolve(deckId: String, chosen: DeckSnapshot) = transaction {
        val heads = RevisionGraph.heads(nodes(deckId, revisions.forDeck(deckId))).map { it.revisionId }
        insertRevision(deckId, parents = heads, snapshot = chosen)
        rebuild(deckId)
    }

    suspend fun exportBackup(): Backup {
        val decks = decksDao.observeDecks().first()
        return Backup(exportedAt = now(), decks = decks.mapNotNull { deck ->
            currentSnapshot(deck.deckId)?.let { BackupDeck(deck.deckId, it) }
        })
    }

    /** Imports decks; a deck that already exists here comes in as a copy rather than overwriting. */
    suspend fun importBackup(backup: Backup): Int {
        var imported = 0
        for (deck in backup.decks) {
            if (deck.snapshot.deleted) continue
            if (decksDao.deck(deck.deckId) == null) create(deck.snapshot, deck.deckId)
            else create(deck.snapshot.copy(name = "${deck.snapshot.name} (imported)"))
            imported++
        }
        return imported
    }

    suspend fun currentSnapshot(deckId: String): DeckSnapshot? {
        val deck = decksDao.deck(deckId) ?: return null
        return revisions.revision(deck.headRevisionId)?.let { decode(it.snapshotJson) }
    }

    // --- Sync support ---

    data class Outgoing(val revisions: List<RevisionDto>, val cursor: Long, val hasMore: Boolean)

    /**
     * Revisions after local seq [after], skipping ones that came from [peer]. Everything returned is
     * marked shared first, so a revision is frozen before it can reach another device.
     */
    suspend fun outgoing(after: Long, limit: Int, peer: String): Outgoing = transaction {
        val rows = revisions.after(after, limit)
        val sending = rows.filter { it.receivedFrom != peer }
        if (sending.isNotEmpty()) revisions.markShared(sending.map { it.seq })
        val parents = if (sending.isEmpty()) emptyMap() else
            revisions.parentsOf(sending.map { it.revisionId }).groupBy({ it.revisionId }, { it.parentId })
        Outgoing(
            revisions = sending.map { it.toDto(parents[it.revisionId].orEmpty()) },
            cursor = rows.lastOrNull()?.seq ?: after,
            hasMore = rows.size == limit,
        )
    }

    data class Applied(val received: Int, val alreadyKnown: Int)

    /**
     * Stores revisions from [peer] in one transaction. Known revision IDs are skipped, so a retried
     * batch changes nothing. [alsoInTransaction] lets the caller advance its cursor atomically.
     */
    suspend fun applyIncoming(
        incoming: List<RevisionDto>,
        peer: String,
        alsoInTransaction: suspend () -> Unit = {},
    ): Applied = transaction {
        var received = 0
        val touched = linkedSetOf<String>()
        for (dto in incoming) {
            require(dto.dataFormatVersion <= SyncProtocol.DATA_FORMAT_VERSION) {
                "A deck uses a newer data format. Update Lock On on this device."
            }
            val seq = revisions.insert(
                RevisionEntity(
                    revisionId = dto.revisionId, deckId = dto.deckId, originDeviceId = dto.originDeviceId,
                    receivedFrom = peer, createdAt = dto.createdAt, deleted = dto.snapshot.deleted,
                    dataFormatVersion = dto.dataFormatVersion, snapshotJson = encode(dto.snapshot), shared = true,
                ),
            )
            if (seq == -1L) continue
            revisions.insertParents(dto.parents.map { RevisionParentEntity(dto.revisionId, it) })
            touched += dto.deckId
            received++
        }
        touched.forEach { rebuild(it) }
        alsoInTransaction()
        Applied(received, incoming.size - received)
    }

    // --- Internals ---

    private suspend fun <T> transaction(block: suspend () -> T): T =
        db.useWriterConnection { it.immediateTransaction { block() } }

    private suspend fun insertRevision(deckId: String, parents: List<String>, snapshot: DeckSnapshot) {
        val revisionId = Uuid.random().toString()
        revisions.insert(
            RevisionEntity(
                revisionId = revisionId, deckId = deckId, originDeviceId = localDeviceId, receivedFrom = null,
                createdAt = now(), deleted = snapshot.deleted, dataFormatVersion = SyncProtocol.DATA_FORMAT_VERSION,
                snapshotJson = encode(snapshot), shared = false,
            ),
        )
        revisions.insertParents(parents.map { RevisionParentEntity(revisionId, it) })
    }

    private suspend fun nodes(deckId: String, all: List<RevisionEntity>): List<RevisionGraph.Node> {
        val parents = revisions.parentsForDeck(deckId).groupBy({ it.revisionId }, { it.parentId })
        return all.map { RevisionGraph.Node(it.revisionId, parents[it.revisionId].orEmpty(), it.createdAt, it.deleted) }
    }

    /** Rebuilds the cached deck row and entries from the revision heads. */
    private suspend fun rebuild(deckId: String) {
        val all = revisions.forDeck(deckId)
        val state = RevisionGraph.state(nodes(deckId, all)) ?: return
        val (shown, conflict) = when (state) {
            is RevisionGraph.DeckState.Resolved -> state.head to false
            // Show the newest surviving edit while the conflict waits; nothing is discarded.
            is RevisionGraph.DeckState.Conflicted -> (state.heads.firstOrNull { !it.deleted } ?: state.heads.first()) to true
        }
        val revision = all.first { it.revisionId == shown.revisionId }
        val snapshot = decode(revision.snapshotJson)
        decksDao.upsert(
            DeckEntity(
                deckId = deckId, name = snapshot.name, notes = snapshot.notes, format = snapshot.format,
                tags = json.encodeToString(tagSerializer, snapshot.tags), headRevisionId = revision.revisionId,
                hasConflict = conflict, deleted = snapshot.deleted && !conflict, updatedAt = revision.createdAt,
            ),
        )
        decksDao.clearEntries(deckId)
        decksDao.insertEntries(
            snapshot.entries.filter { it.quantity > 0 }
                .groupBy { it.section to it.cardNumber }
                .map { (key, e) -> DeckEntryEntity(deckId, key.first.name, key.second, e.first().printingId, e.sumOf { it.quantity }) },
        )
    }

    private fun DeckEntity.toSummary(entries: List<DeckEntryEntity>) = DeckSummary(
        deckId = deckId,
        snapshot = DeckSnapshot(
            name = name, notes = notes, format = format,
            tags = runCatching { json.decodeFromString(tagSerializer, tags) }.getOrDefault(emptyList()),
            entries = entries.map {
                DeckEntry(it.cardNumber, it.quantity, DeckSection.valueOf(it.section), it.printingId)
            }.sortedWith(compareBy({ it.section }, { it.cardNumber })),
            deleted = deleted,
        ),
        headRevisionId = headRevisionId,
        hasConflict = hasConflict,
        updatedAt = updatedAt,
    )

    private fun RevisionEntity.toDto(parents: List<String>) = RevisionDto(
        revisionId = revisionId, deckId = deckId, parents = parents, originDeviceId = originDeviceId,
        createdAt = createdAt, dataFormatVersion = dataFormatVersion, snapshot = decode(snapshotJson),
    )

    private fun encode(snapshot: DeckSnapshot) = json.encodeToString(DeckSnapshot.serializer(), snapshot)
    private fun decode(text: String) = json.decodeFromString(DeckSnapshot.serializer(), text)

    private companion object {
        val tagSerializer = ListSerializer(String.serializer())
    }
}
