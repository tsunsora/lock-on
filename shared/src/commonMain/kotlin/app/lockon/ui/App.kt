package app.lockon.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lockon.AppContainer
import app.lockon.platform.FileBridge
import app.lockon.sync.SyncController
import app.lockon.ui.cards.CardBrowserScreen
import app.lockon.ui.components.Reticle
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
        val syncStatus by sync.status.collectAsState()
        val rootFocus = remember { FocusRequester() }
        LaunchedEffect(state.notice) {
            val notice = state.notice ?: return@LaunchedEffect
            state.notice = null
            val result = snackbars.showSnackbar(
                notice.message, actionLabel = notice.actionLabel,
                duration = if (notice.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) notice.action?.invoke()
        }
        LaunchedEffect(Unit) { runCatching { rootFocus.requestFocus() } }

        BoxWithConstraints(
            Modifier.fillMaxSize()
                // Desktop shortcuts (UX rule R2): Ctrl+F searches cards, Esc goes back.
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.key == Key.F && (event.isCtrlPressed || event.isMetaPressed) -> {
                            if (state.current == null && state.tab != Tab.CARDS) state.select(Tab.CARDS)
                            state.requestSearchFocus(); true
                        }
                        event.key == Key.Escape && state.canGoBack -> { state.goBack(); true }
                        else -> false
                    }
                }
                .focusRequester(rootFocus)
                .focusable(),
        ) {
            val wide = maxWidth >= 840.dp
            @Composable
            fun TabIcon(tab: Tab) {
                // The only status signal outside its screen (UX rule C1): a dot when sync failed.
                if (tab == Tab.SYNC && syncStatus.isError) {
                    BadgedBox(badge = { Badge() }) { Icon(tab.icon(), contentDescription = null) }
                } else {
                    Icon(tab.icon(), contentDescription = null)
                }
            }
            Scaffold(
                snackbarHost = { SnackbarHost(snackbars) },
                // Desktop navigates from a top bar; phones keep it at the bottom, in thumb reach (UX rule N5).
                topBar = {
                    if (wide) TopNav {
                        Tab.entries.forEach { tab ->
                            TopNavItem(tab = tab, selected = state.tab == tab, onClick = { state.select(tab) }, icon = { TabIcon(tab) })
                        }
                    }
                },
                bottomBar = {
                    if (!wide) NavigationBar {
                        Tab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = state.tab == tab,
                                onClick = { state.select(tab) },
                                icon = { TabIcon(tab) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                },
            ) { padding ->
                Row(Modifier.fillMaxSize().padding(padding)) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.fillMaxSize()) {
                            when (val screen = state.current) {
                                is Screen.DeckEditor -> DeckEditorScreen(container, state, files, screen.deckId, wide, screen.startTab)
                                is Screen.Conflict -> ConflictScreen(container, state, screen.deckId, wide)
                                null -> Column(Modifier.fillMaxSize()) {
                                    if (!wide) {
                                        Text(state.tab.label, style = MaterialTheme.typography.headlineSmall,
                                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
                                    }
                                    when (state.tab) {
                                        Tab.DECKS -> DeckLibraryScreen(container, state, files)
                                        Tab.CARDS -> CardBrowserScreen(container.catalog, state)
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

/** Desktop navigation: brand on the left, then the three destinations (UX rules N1, N5). */
@Composable
private fun TopNav(items: @Composable RowScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).height(56.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Reticle()
                Spacer(Modifier.width(8.dp))
                Text("LOCK ON", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(32.dp))
                items()
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** A destination with icon and label, and an amber underline when selected (UX rules I5, V3). */
@Composable
private fun TopNavItem(
    tab: Tab,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val underline = MaterialTheme.colorScheme.primary
    Box(
        modifier.fillMaxHeight().widthIn(min = 112.dp)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            // Drawn rather than laid out, so the underline never changes the item's width.
            .drawBehind {
                if (selected) {
                    val w = size.width * 0.7f
                    drawRect(underline, Offset((size.width - w) / 2, size.height - 3.dp.toPx()), Size(w, 3.dp.toPx()))
                }
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompositionLocalProvider(LocalContentColor provides color) { icon() }
            Text(tab.label, color = color, style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}
