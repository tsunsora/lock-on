package app.lockon.ui
import app.lockon.ui.settings.CardDatabaseCard
import app.lockon.ui.components.FloatingPillNav
import app.lockon.ui.components.LocalNavClearance
import app.lockon.ui.components.navClearance
import app.lockon.ui.components.PillNavBar
import androidx.compose.material.icons.filled.Settings
import app.lockon.ui.components.BottomItem
import app.lockon.ui.components.glass
import app.lockon.ui.components.glassColors
import app.lockon.ui.components.Sparkle
import app.lockon.ui.components.Accent
import app.lockon.ui.components.OnAccent
import app.lockon.ui.components.Gold
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.SolidColor
import app.lockon.ui.components.TopBand
import app.lockon.ui.components.LocalTopBand
import app.lockon.ui.components.ScreenHeader
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import app.lockon.ui.components.isTwilight
import app.lockon.ui.components.lighten
import app.lockon.ui.theme.TabularNumbers
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import app.lockon.ui.components.ArtworkSource
import kotlinx.coroutines.launch
import app.lockon.ui.components.LocalReduceMotion
import app.lockon.ui.components.Motion
import app.lockon.ui.components.LocalArtwork
import app.lockon.ui.components.LockOnMark
import app.lockon.ui.components.artworkImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import app.lockon.ui.components.TricolorStripe
import app.lockon.ui.decks.ConflictScreen
import app.lockon.ui.decks.DeckEditorScreen
import app.lockon.ui.decks.DeckLibraryScreen
import app.lockon.ui.sync.SyncScreen
import app.lockon.ui.update.AppUpdateCard
import app.lockon.ui.theme.LockOnTheme
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.components.SkyBackground
import app.lockon.ui.components.PillShape
import app.lockon.ui.components.motion
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

private fun Tab.icon(): ImageVector = when (this) {
    Tab.DECKS -> Icons.Default.List
    Tab.CARDS -> Icons.Default.Search
    Tab.SETTINGS -> Icons.Default.Settings
}

/** Shared root for Android and desktop. Wide windows get a side rail and multi-pane editors. */
@Composable
fun LockOnApp(
    container: AppContainer,
    state: AppState,
    sync: SyncController,
    files: FileBridge,
    /** Folder for cached card images; images never sync and can be deleted at any time. */
    imageCacheDir: String,
    dark: Boolean? = null,
    /** From the system's "remove animations" setting; turns motion into instant changes. */
    reduceMotion: Boolean = false,
) {
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
    ArtworkProvider(container, imageCacheDir) {
    LockOnTheme(dark ?: isSystemInDarkTheme()) {
        val snackbars = remember { SnackbarHostState() }
        val syncStatus by sync.status.collectAsState()
        val deckCount = container.decks.decks.collectAsState(initial = emptyList()).value.size
        val cardCount = container.catalog.cards.collectAsState().value.size
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
        // At most weekly, and only for people on the official card list.
        LaunchedEffect(Unit) {
            state.appScope.launch {
                runCatching { container.catalogUpdater.quietWeeklyCheck(kotlin.time.Clock.System.now().toEpochMilliseconds()) }
            }
        }

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
                if (tab == Tab.SETTINGS && syncStatus.isError) {
                    BadgedBox(badge = { Badge() }) { Icon(tab.icon(), contentDescription = null) }
                } else {
                    Icon(tab.icon(), contentDescription = null)
                }
            }
            SkyBackground(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                // Snackbars sit above the floating nav bar on phones.
                snackbarHost = { SnackbarHost(snackbars, Modifier.padding(bottom = if (wide) 0.dp else navClearance() - 12.dp)) },
                // Desktop navigation floats over each screen's header (below); phones keep it at the
                // bottom, in thumb reach (UX rule N5).

            ) { padding ->
                var band by remember { mutableStateOf(TopBand()) }
                Row(Modifier.fillMaxSize().padding(padding)) {
                    Box(Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalTopBand provides if (wide) band else TopBand(), LocalNavClearance provides if (wide) 0.dp else navClearance()) {
                        // Going deeper drifts forward, coming back drifts back, and switching tabs
                        // fades through, so motion always says where you went (UX rule V5).
                        val reduce = LocalReduceMotion.current
                        AnimatedContent(
                            targetState = state.current ?: state.tab,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                val from = depthOf(initialState)
                                val to = depthOf(targetState)
                                when {
                                    reduce -> fadeIn(tween(90)) togetherWith fadeOut(tween(90))
                                    to != from -> {
                                        val dir = if (to > from) 1 else -1
                                        (slideInHorizontally(tween(Motion.LONG, easing = Motion.emphasizedDecelerate)) { dir * it / 20 } +
                                            fadeIn(tween(Motion.MEDIUM, delayMillis = Motion.SHORT / 2))) togetherWith
                                            (slideOutHorizontally(tween(Motion.SHORT, easing = Motion.emphasizedAccelerate)) { -dir * it / 30 } +
                                                fadeOut(tween(Motion.SHORT)))
                                    }
                                    else -> fadeIn(tween(Motion.MEDIUM, delayMillis = Motion.SHORT / 2)) togetherWith fadeOut(tween(Motion.SHORT))
                                }
                            },
                            label = "navigation",
                        ) { destination ->
                            when (destination) {
                                is Screen.DeckEditor -> DeckEditorScreen(container, state, files, destination.deckId, wide, destination.startTab)
                                is Screen.Conflict -> Box(Modifier.padding(top = LocalTopBand.current.height)) {
                                    ConflictScreen(container, state, destination.deckId, wide)
                                }
                                is Tab -> Column(Modifier.fillMaxSize()) {
                                    when (destination) {
                                        Tab.DECKS -> DeckLibraryScreen(container, state, files)
                                        Tab.CARDS -> CardBrowserScreen(container, state, files, wide)
                                        Tab.SETTINGS -> {
                                            ScreenHeader("Settings", subtitle = "This device: ${container.identity.deviceName}")
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                                Box(Modifier.widthIn(max = 980.dp)) {
                                                    // Sync first, then the card database, then the app itself.
                                                    SyncScreen(sync, container.identity.deviceName) {
                                                        CardDatabaseCard(container, state, files)
                                                        AppUpdateCard(container.updater, state.appScope)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Phones: the floating pill at the top level. Inside a deck the editor floats its own
                    // sections there instead, and back returns here (UX rules N3, N5).
                    if (!wide && state.current == null) {
                        FloatingPillNav(Tab.entries.map { tab -> BottomItem(tab.label) { TabIcon(tab) } }, Tab.entries.indexOf(state.tab)) {
                            state.select(Tab.entries[it])
                        }
                    }
                    if (wide) {
                        val density = LocalDensity.current
                        FloatingTopNav(deckCount, cardCount, state.tab, state::select, icon = { TabIcon(it) }) { start, end, height ->
                            band = with(density) { TopBand(height.toDp(), start.toDp(), end.toDp()) }
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
}

/** How deep a destination sits: tabs, then a deck, then a conflict comparison. */
private fun depthOf(destination: Any): Int = when (destination) {
    is Screen.Conflict -> 2
    is Screen.DeckEditor -> 1
    else -> 0
}

/** Sets up the image loader and tells every card view where its artwork is. */
@Composable
fun ArtworkProvider(container: AppContainer, imageCacheDir: String, content: @Composable () -> Unit) {
    setSingletonImageLoaderFactory { context -> artworkImageLoader(context, imageCacheDir) }
    val printings by container.catalog.printings.collectAsState()
    val showArtwork by container.catalog.showArtwork.collectAsState()
    val artwork = remember(printings, showArtwork) { ArtworkSource(showArtwork, printings) }
    CompositionLocalProvider(LocalArtwork provides artwork, content = content)
}

/**
 * Desktop navigation floats over each screen's header (UX rules N5, V9): the player card at the
 * top-left and a glass tray of destination medallions at the top-right, as on a game's home screen.
 */
@Composable
private fun FloatingTopNav(
    decks: Int,
    cards: Int,
    selected: Tab,
    onSelect: (Tab) -> Unit,
    icon: @Composable (Tab) -> Unit,
    /** Reports, in pixels, the room the plates take at the start and end, and the band's height. */
    onMeasured: (start: Int, end: Int, height: Int) -> Unit,
) {
    var start by remember { mutableStateOf(IntSize.Zero) }
    var end by remember { mutableStateOf(IntSize.Zero) }
    val margin = with(LocalDensity.current) { 12.dp.roundToPx() }
    LaunchedEffect(start, end) {
        if (start != IntSize.Zero && end != IntSize.Zero) {
            onMeasured(start.width + margin * 2, end.width + margin * 2, maxOf(start.height, end.height) + margin * 2)
        }
    }
    Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(12.dp)) {
        Box(Modifier.align(Alignment.TopStart).onSizeChanged { start = it }) { PlayerCard(decks, cards) }
        PillNavBar(
            Tab.entries.map { tab -> BottomItem(tab.label) { icon(tab) } }, Tab.entries.indexOf(selected), { onSelect(Tab.entries[it]) },
            Modifier.align(Alignment.TopEnd).onSizeChanged { end = it }, labels = true,
        )
    }
}

private val TrayShape = RoundedCornerShape(24.dp)

/**
 * The brand as a player card: a ringed badge with the Lock On mark overlapping a glass name plate,
 * with the collection's counts in pills like a game's currencies (plain counts, never prompts: C1).
 */
@Composable
private fun PlayerCard(decks: Int, cards: Int) {
    val scheme = MaterialTheme.colorScheme
    Box(contentAlignment = Alignment.CenterStart) {
        Column(
            Modifier.padding(start = 34.dp)
                .shadow(10.dp, NamePlate, ambientColor = glassColors().shade, spotColor = glassColors().shade)
                .glass(NamePlate, strong = true)
                .padding(start = 38.dp, end = 16.dp, top = 6.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Lock On", style = MaterialTheme.typography.titleLarge, color = scheme.onSurface, lineHeight = 24.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Currency(Accent, decks, "decks")
                Currency(Gold, cards, "cards")
            }
        }
        LevelBadge(decks)
    }
}

private val NamePlate = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp)

/** A count in a small pill with a coloured gem, like a game's currency counter. */
@Composable
private fun Currency(gem: Color, value: Int, label: String) {
    Row(
        Modifier.clip(LockOnShapes.button).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)).padding(start = 4.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(Brush.radialGradient(listOf(lighten(gem, 0.5f), gem))))
        Text(groupDigits(value), style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun groupDigits(n: Int): String = n.toString().reversed().chunked(3).joinToString(",").reversed()

/** The round badge: the Lock On mark inside a teal ring on a white disc, like a level badge. */
@Composable
private fun LevelBadge(decks: Int) {
    val scheme = MaterialTheme.colorScheme
    val ring = Accent
    val track = scheme.outlineVariant
    Box(
        Modifier.size(64.dp).shadow(10.dp, CircleShape, ambientColor = glassColors().shade, spotColor = glassColors().shade)
            .clip(CircleShape).background(if (isTwilight()) scheme.surfaceContainerHighest else Color.White)
            .drawBehind {
                val stroke = 4.dp.toPx()
                val inset = stroke / 2 + 3.dp.toPx()
                val arc = Size(size.width - inset * 2, size.height - inset * 2)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                drawArc(Brush.sweepGradient(listOf(lighten(ring, 0.4f), ring, lighten(ring, 0.4f))), 120f, 300f, false, Offset(inset, inset), arc,
                    style = Stroke(stroke, cap = StrokeCap.Round))
            },
        contentAlignment = Alignment.Center,
    ) {
        LockOnMark(size = 34.dp, reticleColor = scheme.onSurface)
    }
}


