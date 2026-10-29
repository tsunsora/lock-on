package app.lockon.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope

enum class Tab(val label: String) { DECKS("Decks"), CARDS("Cards"), SYNC("Sync"), SETTINGS("Settings") }

sealed interface Screen {
    /** [startTab] is the phone tab to open on: 0 Deck, 1 Add cards, 2 Analysis, 3 Test, 4 Matches, 5 Versions. */
    data class DeckEditor(val deckId: String, val startTab: Int = 0) : Screen
    data class Conflict(val deckId: String) : Screen
}

/** A snackbar message, optionally with one action such as Undo (UX rule C2). */
data class Notice(val message: String, val actionLabel: String? = null, val action: (() -> Unit)? = null)

/**
 * Navigation state. [appScope] outlives screens, so a save started as someone leaves the editor
 * still finishes.
 */
class AppState(val appScope: CoroutineScope) {
    var tab by mutableStateOf(Tab.DECKS)
    val stack = mutableStateListOf<Screen>()
    var notice by mutableStateOf<Notice?>(null)
    /** Sync shows Send, or Receive when false. */
    var syncSending by mutableStateOf(true)
    /** False while the boot screen covers the app and its screens fill in underneath. */
    var booted by mutableStateOf(false)
    /** True while the welcome tour shows over the app: on a fresh install, or replayed from Settings. */
    var touring by mutableStateOf(false)
    /** Which of the tour's stops is showing. */
    var tourStop by mutableIntStateOf(0)
    /** Bumped by Ctrl+F; the visible card search takes focus when it changes. */
    var searchFocusRequests by mutableIntStateOf(0)
        private set

    val current: Screen? get() = stack.lastOrNull()
    val canGoBack: Boolean get() = stack.isNotEmpty()

    fun open(screen: Screen) { stack.add(screen) }
    fun goBack() { stack.removeLastOrNull() }
    fun select(tab: Tab) { this.tab = tab; stack.clear() }
    fun notify(message: String) { notice = Notice(message) }
    fun notify(message: String, actionLabel: String, action: () -> Unit) { notice = Notice(message, actionLabel, action) }
    fun requestSearchFocus() { searchFocusRequests++ }
}
