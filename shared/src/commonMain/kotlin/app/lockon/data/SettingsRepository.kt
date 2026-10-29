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

/**
 * This device's preferences. They are kept with the device and never sync, so a phone and a
 * computer can each look the way their owner likes.
 */
class SettingsRepository(private val meta: MetaDao) {
    private val _theme = MutableStateFlow(ThemeChoice.SYSTEM)
    private val _motion = MutableStateFlow(MotionChoice.SYSTEM)
    private val _start = MutableStateFlow(StartChoice.DECKS)
    private val _lastTab = MutableStateFlow<String?>(null)
    private val _size = MutableStateFlow(InterfaceSize.DEFAULT)
    private val _catalogChecks = MutableStateFlow(true)
    private val _welcomed = MutableStateFlow(true)

    val theme: StateFlow<ThemeChoice> = _theme.asStateFlow()
    val motion: StateFlow<MotionChoice> = _motion.asStateFlow()
    val start: StateFlow<StartChoice> = _start.asStateFlow()
    /** The tab open when Lock On was last used, for [StartChoice.LAST]. */
    val lastTab: StateFlow<String?> = _lastTab.asStateFlow()
    val size: StateFlow<InterfaceSize> = _size.asStateFlow()
    /** Whether Lock On looks for a newer official card list once a week. On unless turned off. */
    val catalogChecks: StateFlow<Boolean> = _catalogChecks.asStateFlow()
    /** Whether the welcome tour has been seen or skipped; false only on a fresh install. */
    val welcomed: StateFlow<Boolean> = _welcomed.asStateFlow()

    suspend fun load() {
        _theme.value = enumOr(meta.get(KEY_THEME), ThemeChoice.SYSTEM)
        _motion.value = enumOr(meta.get(KEY_MOTION), MotionChoice.SYSTEM)
        _start.value = enumOr(meta.get(KEY_START), StartChoice.DECKS)
        _lastTab.value = meta.get(KEY_LAST_TAB)
        _size.value = enumOr(meta.get(KEY_SIZE), InterfaceSize.DEFAULT)
        _catalogChecks.value = meta.get(KEY_CATALOG_CHECKS) != "false"
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
    suspend fun setCatalogChecks(on: Boolean) { _catalogChecks.value = on; meta.put(MetaEntity(KEY_CATALOG_CHECKS, on.toString())) }

    private inline fun <reified E : Enum<E>> enumOr(stored: String?, default: E): E =
        stored?.let { s -> enumValues<E>().firstOrNull { it.name == s } } ?: default

    private companion object {
        const val KEY_THEME = "settings.theme"
        const val KEY_MOTION = "settings.motion"
        const val KEY_START = "settings.start"
        const val KEY_LAST_TAB = "settings.lastTab"
        const val KEY_SIZE = "settings.size"
        const val KEY_CATALOG_CHECKS = "settings.catalogChecks"
        const val KEY_WELCOMED = "settings.welcomed"
    }
}
