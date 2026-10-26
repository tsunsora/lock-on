package app.lockon.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope

enum class Tab(val label: String) { DECKS("Decks"), CARDS("Cards"), SYNC("Sync") }

sealed interface Screen {
    data class DeckEditor(val deckId: String) : Screen
    data class Conflict(val deckId: String) : Screen
}

/**
 * Navigation state. [appScope] outlives screens, so a save started as someone leaves the editor
 * still finishes.
 */
class AppState(val appScope: CoroutineScope) {
    var tab by mutableStateOf(Tab.DECKS)
    val stack = mutableStateListOf<Screen>()
    var snackbar by mutableStateOf<String?>(null)

    val current: Screen? get() = stack.lastOrNull()
    val canGoBack: Boolean get() = stack.isNotEmpty()

    fun open(screen: Screen) { stack.add(screen) }
    fun goBack() { stack.removeLastOrNull() }
    fun select(tab: Tab) { this.tab = tab; stack.clear() }
    fun notify(message: String) { snackbar = message }
}
