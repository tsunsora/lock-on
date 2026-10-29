package app.lockon.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT * FROM cards ORDER BY cardNumber")
    suspend fun cards(): List<CardEntity>

    @Query("SELECT * FROM printings ORDER BY printingId")
    suspend fun printings(): List<PrintingEntity>

    @Query("DELETE FROM cards")
    suspend fun clearCards()

    @Query("DELETE FROM printings")
    suspend fun clearPrintings()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrintings(printings: List<PrintingEntity>)
}

@Dao
interface MetaDao {
    @Query("SELECT value FROM meta WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM meta WHERE `key` = :key")
    fun observe(key: String): Flow<String?>

    @Upsert
    suspend fun put(entry: MetaEntity)
}

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks WHERE deleted = 0 ORDER BY updatedAt DESC")
    fun observeDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE deckId = :deckId")
    fun observeDeck(deckId: String): Flow<DeckEntity?>

    @Query("SELECT * FROM decks WHERE deckId = :deckId")
    suspend fun deck(deckId: String): DeckEntity?

    @Query("SELECT * FROM decks WHERE deleted = 0 AND folder = :folder")
    suspend fun inFolder(folder: String): List<DeckEntity>

    @Query("SELECT DISTINCT folder FROM decks WHERE deleted = 0 AND folder IS NOT NULL")
    suspend fun folders(): List<String>

    @Query("SELECT * FROM deck_entries")
    fun observeAllEntries(): Flow<List<DeckEntryEntity>>

    @Upsert
    suspend fun upsert(deck: DeckEntity)

    @Query("DELETE FROM deck_entries WHERE deckId = :deckId")
    suspend fun clearEntries(deckId: String)

    @Insert
    suspend fun insertEntries(entries: List<DeckEntryEntity>)
}

@Dao
interface RevisionDao {
    /** Returns -1 when the revision is already known, which makes retried transfers harmless. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(revision: RevisionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertParents(parents: List<RevisionParentEntity>)

    @Query("SELECT * FROM revisions WHERE revisionId = :revisionId")
    suspend fun revision(revisionId: String): RevisionEntity?

    @Query("SELECT * FROM revisions WHERE deckId = :deckId")
    suspend fun forDeck(deckId: String): List<RevisionEntity>

    @Query(
        "SELECT p.* FROM revision_parents p JOIN revisions r ON r.revisionId = p.revisionId WHERE r.deckId = :deckId",
    )
    suspend fun parentsForDeck(deckId: String): List<RevisionParentEntity>

    @Query("SELECT * FROM revision_parents WHERE revisionId IN (:revisionIds)")
    suspend fun parentsOf(revisionIds: List<String>): List<RevisionParentEntity>

    @Query("SELECT * FROM revisions WHERE seq > :after ORDER BY seq LIMIT :limit")
    suspend fun after(after: Long, limit: Int): List<RevisionEntity>

    @Query("UPDATE revisions SET shared = 1 WHERE seq IN (:seqs)")
    suspend fun markShared(seqs: List<Long>)

    /** Freezes a revision so the next edit starts a new version. */
    @Query("UPDATE revisions SET shared = 1 WHERE revisionId = :revisionId")
    suspend fun freeze(revisionId: String)

    @Query("UPDATE revisions SET snapshotJson = :snapshotJson, deleted = :deleted, createdAt = :createdAt WHERE seq = :seq AND shared = 0")
    suspend fun amend(seq: Long, snapshotJson: String, deleted: Boolean, createdAt: Long): Int

    /** Removes a revision no other device has seen. Returns 0 if it was already shared. */
    @Query("DELETE FROM revisions WHERE revisionId = :revisionId AND shared = 0")
    suspend fun discard(revisionId: String): Int

    @Query("DELETE FROM revision_parents WHERE revisionId = :revisionId")
    suspend fun discardParents(revisionId: String)

    @Query("SELECT COALESCE(MAX(seq), 0) FROM revisions")
    suspend fun maxSeq(): Long

    @Query("SELECT COALESCE(MAX(seq), 0) FROM revisions")
    fun observeMaxSeq(): Flow<Long>

    @Query("SELECT COUNT(*) FROM revisions WHERE seq > :after AND (receivedFrom IS NULL OR receivedFrom != :peer)")
    fun observePendingFor(peer: String, after: Long): Flow<Int>
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM paired_devices ORDER BY pairedAt")
    fun observeAll(): Flow<List<PairedDeviceEntity>>

    @Query("SELECT * FROM paired_devices WHERE deviceId = :deviceId")
    suspend fun device(deviceId: String): PairedDeviceEntity?

    @Query("SELECT * FROM paired_devices WHERE tokenHash = :tokenHash")
    suspend fun byTokenHash(tokenHash: String): PairedDeviceEntity?

    @Upsert
    suspend fun upsert(device: PairedDeviceEntity)

    @Query("UPDATE paired_devices SET pushCursor = :cursor WHERE deviceId = :deviceId AND pushCursor < :cursor")
    suspend fun advancePush(deviceId: String, cursor: Long)

    @Query("UPDATE paired_devices SET pullCursor = :cursor WHERE deviceId = :deviceId AND pullCursor < :cursor")
    suspend fun advancePull(deviceId: String, cursor: Long)

    @Query("UPDATE paired_devices SET lastSyncAt = :at, lastError = NULL WHERE deviceId = :deviceId")
    suspend fun markSynced(deviceId: String, at: Long)

    @Query("UPDATE paired_devices SET lastError = :error WHERE deviceId = :deviceId")
    suspend fun markError(deviceId: String, error: String?)

    @Query("DELETE FROM paired_devices WHERE deviceId = :deviceId")
    suspend fun delete(deviceId: String)
}
