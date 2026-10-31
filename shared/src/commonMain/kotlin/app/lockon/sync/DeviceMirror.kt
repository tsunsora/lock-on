package app.lockon.sync

import app.lockon.data.CatalogRepository
import app.lockon.data.DeckRepository
import app.lockon.data.SettingsRepository
import app.lockon.model.DeckSnapshot

/**
 * Makes one device match another: decks with their history, empty folders, preferences, the chosen
 * ruleset and the card list. Device names, pairings and where Lock On was left stay with each device.
 */
class DeviceMirror(
    private val identity: LocalIdentity,
    private val decks: DeckRepository,
    private val settings: SettingsRepository,
    private val catalog: CatalogRepository,
) {
    suspend fun state(): DeviceState = DeviceState(
        deviceId = identity.deviceId,
        deviceName = identity.deviceName,
        decks = decks.heads(),
        keptFolders = decks.keptFolderList(),
        settings = settings.synced() + catalog.synced(),
        catalog = catalog.installedKey(),
    )

    suspend fun catalogKey(): String? = catalog.installedKey()

    suspend fun exportCatalog(): CatalogTransfer = catalog.export()

    /** True when [state]'s card list differs from this device's, so it has to come along. */
    suspend fun needsCatalog(state: DeviceState): Boolean = state.catalog != null && state.catalog != catalog.installedKey()

    /** The decks shown here now, to count what a match changed once revisions have arrived. */
    suspend fun shown(): Map<String, DeckSnapshot> = decks.heads().associate { it.deckId to it.snapshot }

    /**
     * Takes on [state]. Its deck revisions must already be here. The card list goes first, since the
     * ruleset chosen there may only exist in it. [before] is what this device showed before those
     * revisions arrived, which the summary counts against.
     */
    suspend fun apply(state: DeviceState, transfer: CatalogTransfer?, before: Map<String, DeckSnapshot>? = null): MirrorSummary {
        val shownBefore = before ?: shown()
        require(state.dataFormatVersion <= SyncProtocol.DATA_FORMAT_VERSION) {
            "${state.deviceName} stores decks in a newer format. Update Lock On on this device."
        }
        val newCatalog = transfer != null && "${transfer.file.catalogId}@${transfer.file.version}" != catalog.installedKey()
        if (newCatalog) catalog.installTransfer(transfer)
        val before = settings.synced() + catalog.synced()
        settings.applySynced(state.settings)
        catalog.applySynced(state.settings)
        decks.mirror(state.decks, state.keptFolders)
        val source = state.decks.associate { it.deckId to it.snapshot.copy(deleted = false) }
        return MirrorSummary(
            updated = source.count { (id, snapshot) -> shownBefore[id]?.let { it != snapshot } == true },
            added = source.keys.count { it !in shownBefore },
            removed = shownBefore.keys.count { it !in source },
            settings = before != settings.synced() + catalog.synced(), catalog = newCatalog,
        )
    }
}

/** How a finished match reads: what changed on the device that was made to match. */
fun matchedMessage(target: String, source: String, summary: MirrorSummary): String {
    fun decks(n: Int) = if (n == 1) "1 deck" else "$n decks"
    val changes = listOfNotNull(
        summary.added.takeIf { it > 0 }?.let { "${decks(it)} added" },
        summary.updated.takeIf { it > 0 }?.let { "${decks(it)} updated" },
        summary.removed.takeIf { it > 0 }?.let { "${decks(it)} removed" },
        "settings".takeIf { summary.settings },
        "card list".takeIf { summary.catalog },
    )
    return "$target now matches $source" + (if (changes.isEmpty()) ": nothing needed changing." else ": ${changes.joinToString(", ")}.")
}
