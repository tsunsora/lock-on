package app.lockon.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Catalog tables. These are downloaded per device and never synced.

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val cardNumber: String,
    val name: String,
    val type: String,
    /** Comma-separated [app.lockon.model.CardColor] names. */
    val colors: String,
    val level: Int?,
    val cost: Int?,
    val ap: Int?,
    val hp: Int?,
    val text: String,
    val correctedText: String?,
    /** Pipe-separated, because trait names can contain commas. */
    val traits: String,
    val zones: String,
    val link: String?,
    val setCode: String,
    val rarity: String,
)

@Entity(tableName = "printings", indices = [Index("cardNumber")])
data class PrintingEntity(
    @PrimaryKey val printingId: String,
    val cardNumber: String,
    val setCode: String,
    val rarity: String,
    val language: String,
    val label: String,
    val isAlternateArt: Boolean,
    val artworkUrl: String?,
)

/** Small key-value settings: device identity, catalog version, chosen ruleset. */
@Entity(tableName = "meta")
data class MetaEntity(@PrimaryKey val key: String, val value: String)

// Personal data. Revisions are the source of truth; decks and entries are rebuilt from them.

/** Current state of a deck, rebuilt from its head revision whenever revisions change. */
@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey val deckId: String,
    val name: String,
    val notes: String,
    val format: String,
    /** JSON array of tags. */
    val tags: String,
    val headRevisionId: String,
    val hasConflict: Boolean,
    val deleted: Boolean,
    val updatedAt: Long,
)

@Entity(tableName = "deck_entries", primaryKeys = ["deckId", "section", "cardNumber"], indices = [Index("cardNumber")])
data class DeckEntryEntity(
    val deckId: String,
    val section: String,
    val cardNumber: String,
    val printingId: String?,
    val quantity: Int,
)

/**
 * One deck revision. [seq] is this device's insertion order and doubles as the sync cursor; it
 * means nothing on other devices.
 */
@Entity(
    tableName = "revisions",
    indices = [Index(value = ["revisionId"], unique = true), Index("deckId")],
)
data class RevisionEntity(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val revisionId: String,
    val deckId: String,
    val originDeviceId: String,
    /** Paired device this revision arrived from, or null if made here. */
    val receivedFrom: String?,
    val createdAt: Long,
    val deleted: Boolean,
    val dataFormatVersion: Int,
    val snapshotJson: String,
    /** Set once the revision has left this device. Shared revisions are never edited again. */
    val shared: Boolean,
)

@Entity(tableName = "revision_parents", primaryKeys = ["revisionId", "parentId"], indices = [Index("parentId")])
data class RevisionParentEntity(val revisionId: String, val parentId: String)

@Entity(tableName = "paired_devices")
data class PairedDeviceEntity(
    @PrimaryKey val deviceId: String,
    val name: String,
    /** "HOST" for a desktop this device connects to, "CLIENT" for a phone that connects here. */
    val role: String,
    val host: String?,
    val port: Int?,
    /** Pinned TLS certificate fingerprint of a host. */
    val certFingerprint: String?,
    /** SHA-256 of the client's auth token, kept only on the host. */
    val tokenHash: String?,
    val pairedAt: Long,
    val lastSyncAt: Long?,
    val lastError: String?,
    /** Highest local [RevisionEntity.seq] already sent to this device. */
    val pushCursor: Long,
    /** Highest remote seq already received from this device. */
    val pullCursor: Long,
)
