package app.lockon.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lockon.AppContainer
import app.lockon.platform.FileBridge
import app.lockon.sync.SyncController
import app.lockon.ui.cards.CardBrowserScreen
import app.lockon.ui.decks.ConflictScreen
import app.lockon.ui.decks.DeckEditorScreen
import app.lockon.ui.decks.DeckLibraryScreen
import app.lockon.ui.sync.SyncScreen
import app.lockon.ui.theme.LockOnTheme

private fun Tab.icon(): ImageVector = when (this) {
    Tab.DECKS -> Icons.Default.List
    Tab.CARDS -> Icons.Default.Search
    Tab.SYNC -> Icons.Default.Refresh
}

/** Shared root for Android and desktop. Wide windows get a side rail and multi-pane editors. */
@Composable
fun LockOnApp(container: AppContainer, state: AppState, sync: SyncController, files: FileBridge) {
    LockOnTheme {
        val snackbars = remember { SnackbarHostState() }
        LaunchedEffect(state.snackbar) {
            state.snackbar?.let { snackbars.showSnackbar(it); state.snackbar = null }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 840.dp
            Scaffold(
                snackbarHost = { SnackbarHost(snackbars) },
                bottomBar = {
                    if (!wide) NavigationBar {
                        Tab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = state.tab == tab && state.current == null,
                                onClick = { state.select(tab) },
                                icon = { Icon(tab.icon(), contentDescription = null) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                },
            ) { padding ->
                Row(Modifier.fillMaxSize().padding(padding)) {
                    if (wide) NavigationRail(header = {
                        Text("LOCK ON", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 12.dp))
                    }) {
                        Tab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = state.tab == tab,
                                onClick = { state.select(tab) },
                                icon = { Icon(tab.icon(), contentDescription = null) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.fillMaxSize()) {
                            when (val screen = state.current) {
                                is Screen.DeckEditor -> DeckEditorScreen(container, state, files, screen.deckId, wide)
                                is Screen.Conflict -> ConflictScreen(container, state, screen.deckId, wide)
                                null -> Column(Modifier.fillMaxSize()) {
                                    if (!wide) {
                                        Text(state.tab.label, style = MaterialTheme.typography.headlineSmall,
                                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
                                    }
                                    when (state.tab) {
                                        Tab.DECKS -> DeckLibraryScreen(container, state, files)
                                        Tab.CARDS -> CardBrowserScreen(container.catalog)
                                        Tab.SYNC -> SyncScreen(sync, container.identity.deviceName)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
