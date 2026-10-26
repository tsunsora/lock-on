package app.lockon.io

import app.lockon.model.DeckSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Versioned JSON backup of personal decks. The card catalog is never included. */
@Serializable
data class Backup(
    val format: String = FORMAT,
    val formatVersion: Int = VERSION,
    val exportedAt: Long,
    val decks: List<BackupDeck>,
) {
    companion object {
        const val FORMAT = "lock-on-backup"
        const val VERSION = 1
    }
}

@Serializable
data class BackupDeck(val deckId: String, val snapshot: DeckSnapshot)

object BackupFormat {
    val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(backup: Backup): String = json.encodeToString(Backup.serializer(), backup)

    fun decode(text: String): Backup {
        val backup = runCatching { json.decodeFromString(Backup.serializer(), text) }
            .getOrElse { throw IllegalArgumentException("This file is not a Lock On backup.", it) }
        require(backup.format == Backup.FORMAT) { "This file is not a Lock On backup." }
        require(backup.formatVersion <= Backup.VERSION) {
            "This backup was made by a newer version of Lock On (format ${backup.formatVersion}). Update the app to import it."
        }
        return backup
    }
}
