package app.lockon.data

import app.lockon.data.db.MetaDao
import app.lockon.data.db.MetaEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Light, dark, or whatever the system uses. */
enum class ThemeChoice(val label: String) { SYSTEM("Match system"), LIGHT("Sky"), DARK("Twilight") }

/** Animations as the system asks, always reduced, or always full. */
enum class MotionChoice(val label: String) { SYSTEM("Match system"), REDUCED("Reduced"), FULL("Full") }

/** Where Lock On opens. [LAST] is wherever the person left it. */
enum class StartChoice(val label: String) { DECKS("Decks"), CARDS("Cards"), LAST("Where I left off") }

/** How big everything is drawn, as a multiple of the platform's own size. */
enum class InterfaceSize(val label: String, val scale: Float) {
    SMALL("Small", 0.9f), DEFAULT("Default", 1f), LARGE("Large", 1.12f), LARGER("Larger", 1.25f)
}

/** A button on the deck editor's bar on phones. */
enum class EditorBarItem(val label: String) { MORE("More"), ADD("Add cards"), DECK("Deck") }

/** The deck editor's bar on phones, left to right. Its pages slide in the same order. */
enum class EditorBarOrder(val items: List<EditorBarItem>) {
    MORE_ADD_DECK(listOf(EditorBarItem.MORE, EditorBarItem.ADD, EditorBarItem.DECK)),
    MORE_DECK_ADD(listOf(EditorBarItem.MORE, EditorBarItem.DECK, EditorBarItem.ADD)),
    ADD_MORE_DECK(listOf(EditorBarItem.ADD, EditorBarItem.MORE, EditorBarItem.DECK)),
    DECK_MORE_ADD(listOf(EditorBarItem.DECK, EditorBarItem.MORE, EditorBarItem.ADD)),
    ADD_DECK_MORE(listOf(EditorBarItem.ADD, EditorBarItem.DECK, EditorBarItem.MORE)),
    DECK_ADD_MORE(listOf(EditorBarItem.DECK, EditorBarItem.ADD, EditorBarItem.MORE));

    val label: String get() = items.joinToString(" · ") { it.label }
}

/**
 * This device's preferences. Merging leaves them alone, so a phone and a computer can each look the
 * way their owner likes; making one device match another copies them over with everything else.
 */
class SettingsRepository(private val meta: MetaDao) {
    private val _theme = MutableStateFlow(ThemeChoice.SYSTEM)
    private val _motion = MutableStateFlow(MotionChoice.SYSTEM)
    private val _start = MutableStateFlow(StartChoice.DECKS)
    private val _lastTab = MutableStateFlow<String?>(null)
    private val _size = MutableStateFlow(InterfaceSize.DEFAULT)
    private val _catalogChecks = MutableStateFlow(true)
    private val _autoDeckColors = MutableStateFlow(true)
    private val _editorSplit = MutableStateFlow<Float?>(null)
    private val _welcomed = MutableStateFlow(true)
    private val _editorBar = MutableStateFlow(EditorBarOrder.MORE_ADD_DECK)

    val theme: StateFlow<ThemeChoice> = _theme.asStateFlow()
    val motion: StateFlow<MotionChoice> = _motion.asStateFlow()
    val start: StateFlow<StartChoice> = _start.asStateFlow()
    /** The tab open when Lock On was last used, for [StartChoice.LAST]. */
    val lastTab: StateFlow<String?> = _lastTab.asStateFlow()
    val size: StateFlow<InterfaceSize> = _size.asStateFlow()
    /** Whether Lock On looks for a newer official card list once a week. On unless turned off. */
    val catalogChecks: StateFlow<Boolean> = _catalogChecks.asStateFlow()
    /** Whether the editor's "Deck colors only" filter turns on once a deck reaches its color limit. On unless turned off. */
    val autoDeckColors: StateFlow<Boolean> = _autoDeckColors.asStateFlow()
    /** How much of a wide deck editor the card list takes, the same for every deck; null for the default. */
    val editorSplit: StateFlow<Float?> = _editorSplit.asStateFlow()
    /** Whether the welcome tour has been seen or skipped; false only on a fresh install. */
    val welcomed: StateFlow<Boolean> = _welcomed.asStateFlow()
    val editorBar: StateFlow<EditorBarOrder> = _editorBar.asStateFlow()

    suspend fun load() {
        _theme.value = enumOr(meta.get(KEY_THEME), ThemeChoice.SYSTEM)
        _motion.value = enumOr(meta.get(KEY_MOTION), MotionChoice.SYSTEM)
        _start.value = enumOr(meta.get(KEY_START), StartChoice.DECKS)
        _lastTab.value = meta.get(KEY_LAST_TAB)
        _size.value = enumOr(meta.get(KEY_SIZE), InterfaceSize.DEFAULT)
        _catalogChecks.value = meta.get(KEY_CATALOG_CHECKS) != "false"
        _autoDeckColors.value = meta.get(KEY_AUTO_DECK_COLORS) != "false"
        _editorSplit.value = meta.get(KEY_EDITOR_SPLIT)?.toFloatOrNull()
        _editorBar.value = enumOr(meta.get(KEY_EDITOR_BAR), EditorBarOrder.MORE_ADD_DECK)
        // Lock On remembers its last tab from the first launch on, so a stored tab means someone
        // used it before the tour existed and needs no welcome.
        _welcomed.value = meta.get(KEY_WELCOMED)?.toBooleanStrictOrNull() ?: (_lastTab.value != null)
        meta.put(MetaEntity(KEY_WELCOMED, _welcomed.value.toString()))
    }

    suspend fun setTheme(choice: ThemeChoice) { _theme.value = choice; meta.put(MetaEntity(KEY_THEME, choice.name)) }
    suspend fun setMotion(choice: MotionChoice) { _motion.value = choice; meta.put(MetaEntity(KEY_MOTION, choice.name)) }
    suspend fun setStart(choice: StartChoice) { _start.value = choice; meta.put(MetaEntity(KEY_START, choice.name)) }
    suspend fun setLastTab(tab: String) {
        if (_lastTab.value == tab) return
        _lastTab.value = tab
        meta.put(MetaEntity(KEY_LAST_TAB, tab))
    }
    suspend fun setSize(size: InterfaceSize) { _size.value = size; meta.put(MetaEntity(KEY_SIZE, size.name)) }
    suspend fun setWelcomed() { _welcomed.value = true; meta.put(MetaEntity(KEY_WELCOMED, "true")) }
    suspend fun setEditorBar(order: EditorBarOrder) { _editorBar.value = order; meta.put(MetaEntity(KEY_EDITOR_BAR, order.name)) }
    suspend fun setCatalogChecks(on: Boolean) { _catalogChecks.value = on; meta.put(MetaEntity(KEY_CATALOG_CHECKS, on.toString())) }
    /** Null goes back to the default split. */
    suspend fun setEditorSplit(split: Float?) { _editorSplit.value = split; meta.put(MetaEntity(KEY_EDITOR_SPLIT, split?.toString().orEmpty())) }
    suspend fun setAutoDeckColors(on: Boolean) { _autoDeckColors.value = on; meta.put(MetaEntity(KEY_AUTO_DECK_COLORS, on.toString())) }

    /** The preferences a device takes on when it's made to match this one. Where Lock On was left and the welcome tour stay per device. */
    fun synced(): Map<String, String> = mapOf(
        KEY_THEME to _theme.value.name,
        KEY_MOTION to _motion.value.name,
        KEY_START to _start.value.name,
        KEY_SIZE to _size.value.name,
        KEY_CATALOG_CHECKS to _catalogChecks.value.toString(),
        KEY_AUTO_DECK_COLORS to _autoDeckColors.value.toString(),
    )

    /** Takes on another device's [synced] preferences; keys this version doesn't know are skipped. */
    suspend fun applySynced(values: Map<String, String>) {
        values[KEY_THEME]?.let { setTheme(enumOr(it, _theme.value)) }
        values[KEY_MOTION]?.let { setMotion(enumOr(it, _motion.value)) }
        values[KEY_START]?.let { setStart(enumOr(it, _start.value)) }
        values[KEY_SIZE]?.let { setSize(enumOr(it, _size.value)) }
        values[KEY_CATALOG_CHECKS]?.toBooleanStrictOrNull()?.let { setCatalogChecks(it) }
        values[KEY_AUTO_DECK_COLORS]?.toBooleanStrictOrNull()?.let { setAutoDeckColors(it) }
    }

    private inline fun <reified E : Enum<E>> enumOr(stored: String?, default: E): E =
        stored?.let { s -> enumValues<E>().firstOrNull { it.name == s } } ?: default

    private companion object {
        const val KEY_THEME = "settings.theme"
        const val KEY_MOTION = "settings.motion"
        const val KEY_START = "settings.start"
        const val KEY_LAST_TAB = "settings.lastTab"
        const val KEY_SIZE = "settings.size"
        const val KEY_CATALOG_CHECKS = "settings.catalogChecks"
        const val KEY_AUTO_DECK_COLORS = "settings.autoDeckColors"
        // Its key from before it moved here, so a divider already placed stays put.
        const val KEY_EDITOR_SPLIT = "ui.deckEditor.split"
        const val KEY_WELCOMED = "settings.welcomed"
        const val KEY_EDITOR_BAR = "settings.editorBar"
    }
}
