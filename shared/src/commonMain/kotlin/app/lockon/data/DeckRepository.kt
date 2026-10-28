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
import app.lockon.model.MatchRecord
import app.lockon.sync.RevisionDto
import app.lockon.sync.RevisionGraph
import app.lockon.sync.SyncProtocol
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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

    /**
     * [decks] for screens: one read per change shared by every screen, decoded off the main thread.
     * It can trail a write by a moment, so code that reads straight after writing uses [decks].
     */
    val liveDecks: Flow<List<DeckSummary>> =
        decks.shareIn(CoroutineScope(SupervisorJob() + Dispatchers.Default), SharingStarted.WhileSubscribed(5_000), replay = 1)

    /** One deck from [liveDecks]; it emits only when that deck changes. */
    fun liveDeck(deckId: String): Flow<DeckSummary?> =
        liveDecks.map { list -> list.firstOrNull { it.deckId == deckId } }.distinctUntilChanged()

    /** Highest local revision seq. It rises on every save and every received revision. */
    val latestSeq: Flow<Long> = revisions.observeMaxSeq()

    fun deck(deckId: String): Flow<DeckSummary?> = decks.map { list -> list.firstOrNull { it.deckId == deckId } }

    suspend fun create(snapshot: DeckSnapshot, deckId: String = Uuid.random().toString()): String {
        transaction {
            insertRevision(deckId, parents = emptyList(), snapshot = snapshot)
            rebuild(deckId)
        }
        return deckId
    }

    /**
     * Saves the deck's new content. Consecutive autosaves fold into one revision, so each version in
     * the history is an editing session. A version closes when it is sent to another device, kept
     * with [keepVersion], or left alone for [VERSION_IDLE_MS]. Shared data is never changed.
     *
     * [base] is the revision the edit started from. If another device's revision has arrived since,
     * the edit branches from [base] instead, so both show as a conflict rather than one overwriting the other.
     */
    suspend fun save(deckId: String, snapshot: DeckSnapshot, base: String? = null) =
        transaction { saveInTransaction(deckId, snapshot, base) }

    /** Saves [snapshot] and closes the version, so the next edit starts a new one. */
    suspend fun keepVersion(deckId: String, snapshot: DeckSnapshot) = transaction {
        saveInTransaction(deckId, snapshot)
        decksDao.deck(deckId)?.let { revisions.freeze(it.headRevisionId) }
    }

    /** Every earlier version of the deck's current content, newest first. Deletions are left out. */
    suspend fun history(deckId: String): List<DeckVersion> = withContext(Dispatchers.Default) {
        val deck = decksDao.deck(deckId) ?: return@withContext emptyList()
        val all = revisions.forDeck(deckId)
        val byId = all.associateBy { it.revisionId }
        RevisionGraph.history(nodes(deckId, all), deck.headRevisionId).filterNot { it.deleted }.map { node ->
            val revision = byId.getValue(node.revisionId)
            DeckVersion(revision.revisionId, revision.originDeviceId, revision.createdAt, decode(revision.snapshotJson))
        }
    }

    // --- Folders ---
    // A folder is only a name on each deck, so it syncs with the deck and needs no record of its own.

    /**
     * Puts decks in [folder], or takes them out of any folder when it is blank. Returns the folder
     * name as saved, which reuses an existing folder's spelling.
     */
    suspend fun moveToFolder(deckIds: Collection<String>, folder: String?): String? = transaction {
        val target = canonicalFolder(folder)
        for (id in deckIds) {
            val current = currentInTransaction(id) ?: continue
            if (current.folder != target) saveInTransaction(id, current.copy(folder = target))
        }
        target
    }

    /**
     * Renames a folder on every deck in it and returns the name as saved. Renaming onto an existing
     * folder merges the two.
     */
    suspend fun renameFolder(from: String, to: String): String? = transaction {
        val target = canonicalFolder(to, ignoring = from) ?: return@transaction null
        for (deck in decksDao.inFolder(from)) {
            val current = currentInTransaction(deck.deckId) ?: continue
            saveInTransaction(deck.deckId, current.copy(folder = target))
        }
        target
    }

    /** Takes every deck out of [folder] and returns their IDs, so the change can be undone. */
    suspend fun removeFolder(folder: String): List<String> = transaction {
        decksDao.inFolder(folder).mapNotNull { deck ->
            val current = currentInTransaction(deck.deckId) ?: return@mapNotNull null
            saveInTransaction(deck.deckId, current.copy(folder = null))
            deck.deckId
        }
    }

    /** Deletes the deck. A conflicted deck is deleted on every version, so another one can't take its place. */
    suspend fun delete(deckId: String) = transaction {
        val deck = decksDao.deck(deckId) ?: return@transaction
        val current = currentInTransaction(deckId) ?: return@transaction
        if (!deck.hasConflict) return@transaction saveInTransaction(deckId, current.copy(deleted = true))
        val heads = RevisionGraph.heads(nodes(deckId, revisions.forDeck(deckId))).map { it.revisionId }
        insertRevision(deckId, parents = heads, snapshot = current.copy(deleted = true))
        rebuild(deckId)
    }

    /**
     * Undoes a delete. Deleting a conflicted deck adds a revision of its own, so while no other device
     * has seen it, it is simply removed and every version comes back. Otherwise the earlier content
     * is saved on top of the deletion.
     */
    suspend fun restore(deckId: String, snapshot: DeckSnapshot) = transaction {
        val deck = decksDao.deck(deckId) ?: return@transaction
        val head = revisions.revision(deck.headRevisionId)
        val endedConflict = head != null && head.deleted && head.originDeviceId == localDeviceId &&
            revisions.parentsOf(listOf(head.revisionId)).size > 1
        if (endedConflict && revisions.discard(head.revisionId) == 1) {
            revisions.discardParents(head.revisionId)
            return@transaction rebuild(deckId)
        }
        saveInTransaction(deckId, snapshot.copy(deleted = false))
    }

    /** The copy keeps the folder and cover but starts with an empty match log. */
    suspend fun duplicate(deckId: String): String? {
        val current = currentSnapshot(deckId) ?: return null
        return create(current.copy(name = "${current.name} (copy)", matches = emptyList()))
    }

    /** All heads of a deck. More than one means devices edited it independently. */
    suspend fun versions(deckId: String): List<DeckVersion> = withContext(Dispatchers.Default) {
        val all = revisions.forDeck(deckId)
        val nodes = nodes(deckId, all)
        val byId = all.associateBy { it.revisionId }
        RevisionGraph.heads(nodes).map { node ->
            val revision = byId.getValue(node.revisionId)
            DeckVersion(revision.revisionId, revision.originDeviceId, revision.createdAt, decode(revision.snapshotJson))
        }
    }

    /**
     * Ends a conflict with a merge revision that supersedes every head. The chosen version wins,
     * except for matches: games logged on any device are all kept, and games removed on any device stay removed.
     */
    suspend fun resolve(deckId: String, chosen: DeckSnapshot) = transaction {
        val all = revisions.forDeck(deckId)
        val nodes = nodes(deckId, all)
        val heads = RevisionGraph.heads(nodes).map { it.revisionId }
        insertRevision(deckId, parents = heads, snapshot = chosen.copy(matches = mergedMatches(all, nodes, heads, chosen.matches)))
        rebuild(deckId)
    }

    /**
     * Matches across [heads]: every game any of them logged, minus games that were there before the
     * branches split and that some head (or [extra]) has since removed.
     */
    private fun mergedMatches(
        all: List<RevisionEntity>,
        nodes: List<RevisionGraph.Node>,
        heads: List<String>,
        extra: List<MatchRecord>? = null,
    ): List<MatchRecord> {
        val byId = all.associateBy { it.revisionId }
        val lists = heads.map { decode(byId.getValue(it).snapshotJson).matches } + listOfNotNull(extra)
        val before = RevisionGraph.splitPoints(nodes, heads)
            .flatMap { decode(byId.getValue(it).snapshotJson).matches.map { m -> m.id } }.toSet()
        val removed = before.filter { id -> lists.any { list -> list.none { it.id == id } } }.toSet()
        return mergeMatches(lists.flatten().filter { it.id !in removed })
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

    suspend fun currentSnapshot(deckId: String): DeckSnapshot? = currentInTransaction(deckId)

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
        touched.forEach { joinIdenticalHeads(it); rebuild(it) }
        alsoInTransaction()
        Applied(received, incoming.size - received)
    }

    /**
     * Two devices reaching the same content separately, such as a backup restored on a new phone
     * meeting the desktop's copy, isn't a real conflict, so the heads are joined without asking.
     */
    private suspend fun joinIdenticalHeads(deckId: String) {
        val all = revisions.forDeck(deckId)
        val nodes = nodes(deckId, all)
        val heads = RevisionGraph.heads(nodes)
        if (heads.size < 2 || heads.any { it.deleted }) return
        val byId = all.associateBy { it.revisionId }
        val snapshots = heads.map { decode(byId.getValue(it.revisionId).snapshotJson) }
        if (snapshots.map { it.copy(matches = emptyList()) }.distinct().size != 1) return
        val ids = heads.map { it.revisionId }
        insertRevision(deckId, parents = ids, snapshot = snapshots.first().copy(matches = mergedMatches(all, nodes, ids)))
    }

    // --- Internals ---

    /** Runs off the main thread: a sync batch encodes and decodes hundreds of snapshots. */
    private suspend fun <T> transaction(block: suspend () -> T): T =
        withContext(Dispatchers.Default) { db.useWriterConnection { it.immediateTransaction { block() } } }

    private suspend fun currentInTransaction(deckId: String): DeckSnapshot? {
        val deck = decksDao.deck(deckId) ?: return null
        return revisions.revision(deck.headRevisionId)?.let { decode(it.snapshotJson) }
    }

    private suspend fun saveInTransaction(deckId: String, snapshot: DeckSnapshot, base: String? = null) {
        val deck = decksDao.deck(deckId) ?: error("Deck $deckId does not exist.")
        if (base != null && base != deck.headRevisionId && arrivedSince(deckId, base, deck.headRevisionId)) {
            insertRevision(deckId, parents = listOf(base), snapshot = snapshot)
            return rebuild(deckId)
        }
        val head = revisions.revision(deck.headRevisionId)
        // Saving what is already there changes nothing, so keeping a version twice adds no empty version.
        if (head != null && !deck.hasConflict && decode(head.snapshotJson) == snapshot) return
        val amended = head != null && !deck.hasConflict && head.originDeviceId == localDeviceId && !head.shared &&
            now() - head.createdAt < VERSION_IDLE_MS &&
            revisions.amend(head.seq, encode(snapshot), snapshot.deleted, now()) == 1
        if (!amended) insertRevision(deckId, parents = listOf(deck.headRevisionId), snapshot = snapshot)
        rebuild(deckId)
    }

    /**
     * True when [head] builds on another device's revision that [base] doesn't have, so an edit made
     * on [base] would overwrite it. This device's own later saves don't count, and neither does a
     * change that left the content as it was.
     */
    private suspend fun arrivedSince(deckId: String, base: String, head: String): Boolean {
        val all = revisions.forDeck(deckId)
        val byId = all.associateBy { it.revisionId }
        val baseRevision = byId[base] ?: return false
        val nodes = nodes(deckId, all)
        val since = RevisionGraph.ancestors(nodes, head) - RevisionGraph.ancestors(nodes, base)
        if (since.all { byId[it]?.originDeviceId == localDeviceId }) return false
        return byId[head]?.snapshotJson?.let(::decode) != decode(baseRevision.snapshotJson)
    }

    /** Trims [name] and reuses an existing folder's spelling when only the case differs. */
    private suspend fun canonicalFolder(name: String?, ignoring: String? = null): String? {
        val trimmed = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return decksDao.folders().firstOrNull { it != ignoring && it.equals(trimmed, ignoreCase = true) } ?: trimmed
    }

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
                tags = json.encodeToString(tagSerializer, snapshot.tags), folder = snapshot.folder,
                coverCard = snapshot.coverCard, matches = json.encodeToString(matchSerializer, snapshot.matches),
                headRevisionId = revision.revisionId,
                hasConflict = conflict, deleted = snapshot.deleted && !conflict, updatedAt = revision.createdAt,
            ),
        )
        decksDao.clearEntries(deckId)
        decksDao.insertEntries(
            snapshot.entries.filter { it.quantity > 0 }
                .groupBy { it.section to it.cardNumber }
                .map { (key, e) -> DeckEntryEntity(deckId, key.first.storedName, key.second, e.first().printingId, e.sumOf { it.quantity }) },
        )
    }

    private fun DeckEntity.toSummary(entries: List<DeckEntryEntity>) = DeckSummary(
        deckId = deckId,
        snapshot = DeckSnapshot(
            name = name, notes = notes, format = format,
            tags = runCatching { json.decodeFromString(tagSerializer, tags) }.getOrDefault(emptyList()),
            folder = folder,
            coverCard = coverCard,
            matches = runCatching { json.decodeFromString(matchSerializer, matches) }.getOrDefault(emptyList()),
            entries = entries.map {
                DeckEntry(it.cardNumber, it.quantity, DeckSection.fromStored(it.section), it.printingId)
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

    companion object {
        /** An edit after this long without one starts a new version. */
        const val VERSION_IDLE_MS = 30 * 60_000L

        private val tagSerializer = ListSerializer(String.serializer())
        private val matchSerializer = ListSerializer(MatchRecord.serializer())

        /** One copy of each match, oldest first. */
        fun mergeMatches(matches: List<MatchRecord>): List<MatchRecord> =
            matches.distinctBy { it.id }.sortedWith(compareBy({ it.playedAt }, { it.id }))
    }
}
