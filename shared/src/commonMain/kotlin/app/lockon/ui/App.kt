package app.lockon.ui
import app.lockon.ui.settings.SettingsScreen
import app.lockon.data.MotionChoice
import app.lockon.data.StartChoice
import app.lockon.data.ThemeChoice
import androidx.compose.ui.unit.Density
import app.lockon.ui.components.FloatingPillNav
import app.lockon.ui.components.LocalNavClearance
import app.lockon.ui.components.LocalChromeHeight
import app.lockon.ui.components.ControlSize
import app.lockon.ui.components.navClearance
import app.lockon.ui.components.PillNavBar
import androidx.compose.material.icons.filled.Settings
import app.lockon.ui.components.BottomItem
import app.lockon.ui.components.glass
import app.lockon.ui.components.glassColors
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import app.lockon.ui.components.LocalReduceMotion
import app.lockon.ui.components.Motion
import app.lockon.ui.components.LocalArtwork
import app.lockon.ui.components.ArtworkLoads
import app.lockon.ui.components.LockOnMark
import app.lockon.ui.components.artworkImageLoader
import app.lockon.ui.components.artworkHttpClient
import app.lockon.ui.components.LocalArtworkStore
import app.lockon.data.ArtworkStore
import coil3.compose.setSingletonImageLoaderFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleOut
import androidx.compose.ui.layout.Layout
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import app.lockon.ui.components.TricolorStripe
import app.lockon.ui.decks.ConflictScreen
import app.lockon.ui.decks.DeckEditorScreen
import app.lockon.ui.decks.DeckLibraryScreen
import app.lockon.ui.sync.SyncScreen
import app.lockon.ui.sync.SyncPrompts
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
    Tab.SYNC -> Icons.Default.Refresh
    Tab.SETTINGS -> Icons.Default.Settings
}

/** Shared root for Android and desktop. Wide windows get a side rail and multi-pane editors. */
@Composable
fun LockOnApp(
    container: AppContainer,
    state: AppState,
    sync: SyncController,
    files: FileBridge,
    /** Folder where card images are kept for offline use; they never sync. */
    artworkDir: String,
    dark: Boolean? = null,
    /** From the system's "remove animations" setting; turns motion into instant changes. */
    reduceMotion: Boolean = false,
    /** Desktop's own title bar, drawn into the top band; null keeps the system's (and on phones). */
    chrome: WindowChrome? = null,
) {
    val settings = container.settings
    val themeChoice by settings.theme.collectAsState()
    val motionChoice by settings.motion.collectAsState()
    val interfaceSize by settings.size.collectAsState()
    // Open where the person asked to, unless the caller already chose a place.
    remember {
        if (state.tab == Tab.DECKS && state.current == null) {
            when (settings.start.value) {
                StartChoice.DECKS -> Unit
                StartChoice.CARDS -> state.tab = Tab.CARDS
                StartChoice.LAST -> Tab.entries.firstOrNull { it.name == settings.lastTab.value }?.let { state.tab = it }
            }
        }
    }
    LaunchedEffect(state.tab) { settings.setLastTab(state.tab.name) }
    val reduce = when (motionChoice) {
        MotionChoice.SYSTEM -> reduceMotion
        MotionChoice.REDUCED -> true
        MotionChoice.FULL -> false
    }
    val systemDark = isSystemInDarkTheme()
    val useDark = dark ?: when (themeChoice) {
        ThemeChoice.SYSTEM -> systemDark
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }
    val base = LocalDensity.current
    val density = remember(base, interfaceSize) { Density(base.density * interfaceSize.scale, base.fontScale) }
    CompositionLocalProvider(LocalReduceMotion provides reduce, LocalDensity provides density) {
    ArtworkProvider(container, artworkDir) {
    LockOnTheme(useDark) {
        val snackbars = remember { SnackbarHostState() }
        val syncStatus by sync.status.collectAsState()
        // Only the counts, so saving a deck or loading cards doesn't recompose the whole app.
        val deckCount by remember { container.decks.liveDecks.map { it.size }.distinctUntilChanged() }.collectAsState(initial = 0)
        val cardCount by remember { container.catalog.cards.map { it.size }.distinctUntilChanged() }.collectAsState(initial = container.catalog.cards.value.size)
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
        // The boot screen stays up until the decks are in and the screens under it, tabs and recent
        // decks' editors, have drawn their cards, so the first look at each doesn't stutter.
        var bootStep by remember { mutableStateOf(BootStep.DECKS) }
        // Deck editors kept built, so opening one of these decks shows it at once; [recentDecks] is
        // most recently opened first and decides which editor goes when there are too many.
        val keptDecks = remember { mutableStateListOf<Screen.DeckEditor>() }
        val recentDecks = remember { mutableListOf<String>() }
        val openDeck = state.current as? Screen.DeckEditor
        LaunchedEffect(openDeck) {
            val open = openDeck ?: return@LaunchedEffect
            recentDecks.remove(open.deckId)
            recentDecks.add(0, open.deckId)
            if (keptDecks.none { it.deckId == open.deckId }) keptDecks += open
            while (keptDecks.size > KeptDeckLimit) keptDecks.remove(keptDecks.maxBy { recentDecks.indexOf(it.deckId) })
        }
        if (!state.booted) LaunchedEffect(Unit) {
            val started = TimeSource.Monotonic.markNow()
            withTimeoutOrNull(BootLimit) {
                // The decks changed most recently are the likeliest to be opened: build their editors now.
                val recent = container.decks.liveDecks.first().filterNot { it.snapshot.deleted }
                    .sortedByDescending { it.updatedAt }.take(KeptDeckLimit).map { it.deckId }
                recentDecks += recent
                keptDecks += recent.map { Screen.DeckEditor(it) }
                bootStep = BootStep.CARDS
                // The hidden screens compose now and start loading their card images. Wait until no image
                // has been loading for a moment, since more start as each screen fills in.
                delay(BootCardsTime)
                bootStep = BootStep.EDITOR
                while (true) {
                    ArtworkLoads.inFlight.first { it == 0 }
                    if (withTimeoutOrNull(BootQuietTime) { ArtworkLoads.inFlight.first { it > 0 } } == null) break
                }
            }
            val shown = started.elapsedNow()
            if (shown < BootMinimum) delay(BootMinimum - shown)
            state.booted = true
        }
        // At most weekly, only for people on the official card list, and only while checks are on.
        LaunchedEffect(Unit) {
            if (settings.catalogChecks.value) state.appScope.launch {
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
                if (tab == Tab.SYNC && syncStatus.isError) {
                    BadgedBox(badge = { Badge() }) { Icon(tab.icon(), contentDescription = null) }
                } else {
                    Icon(tab.icon(), contentDescription = null)
                }
            }
            var band by remember { mutableStateOf(TopBand()) }
            // Desktop draws its own title bar (UX rule N5). The top band is the bar: its drag area wraps
            // the screens and only acts on presses nothing else takes. Narrower windows have no band, so
            // the controls share the screen header's row, which leaves them room at its end.
            val caption = if (wide) band.height.takeIf { it > 0.dp } ?: 88.dp else CaptionHeight
            SkyBackground(if (chrome != null) chrome.dragArea(Modifier.fillMaxSize(), caption) else Modifier.fillMaxSize()) {
            var controlsWidth by remember { mutableStateOf(0.dp) }
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                // Snackbars sit above the floating nav bar on phones.
                snackbarHost = { SnackbarHost(snackbars, Modifier.padding(bottom = if (wide) 0.dp else navClearance() - 12.dp)) },
                // Desktop navigation floats over each screen's header (below); phones keep it at the
                // bottom, in thumb reach (UX rule N5).

            ) { padding ->
                Row(Modifier.fillMaxSize().padding(padding)) {
                    Box(Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalTopBand provides if (wide) band else TopBand(end = if (chrome != null) controlsWidth else 0.dp), LocalNavClearance provides if (wide) 0.dp else navClearance()) {
                        // Tabs are built once and kept: switching only shows a screen that's already laid
                        // out, with its cards drawn. They fade through each other (UX rule V5).
                        KeptScreens(Tab.entries, if (state.current == null) state.tab else null, state.booted) { tab ->
                            Column(Modifier.fillMaxSize()) {
                                when (tab) {
                                    Tab.DECKS -> DeckLibraryScreen(container, state, files)
                                    Tab.CARDS -> CardBrowserScreen(container, state, files, wide)
                                    Tab.SYNC -> {
                                        ScreenHeader("Sync")
                                        val decks by container.decks.decks.collectAsState(initial = emptyList())
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                            Box(Modifier.widthIn(max = 1240.dp)) { SyncScreen(
                                                sync, container.identity.deviceName, decks.filterNot { it.snapshot.deleted },
                                                sending = state.syncSending, onSendingChange = { state.syncSending = it },
                                            ) }
                                        }
                                    }
                                    Tab.SETTINGS -> {
                                        ScreenHeader("Settings")
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                            Box(Modifier.widthIn(max = 1240.dp)) { SettingsScreen(container, state, files) }
                                        }
                                    }
                                }
                            }
                        }
                        // Decks drift in from the side and back out as you leave, so motion says where you
                        // went (UX rule V5). A deck opened for the first time builds as it arrives.
                        val decksShown = if (openDeck == null || keptDecks.any { it.deckId == openDeck.deckId }) keptDecks.toList() else keptDecks + openDeck
                        KeptScreens(decksShown.map { it.deckId }, openDeck?.deckId, state.booted, slide = true) { id ->
                            DeckEditorScreen(container, state, files, id, wide, decksShown.firstOrNull { it.deckId == id }?.startTab ?: 0)
                        }
                        val reduce = LocalReduceMotion.current
                        AnimatedContent(
                            targetState = state.current as? Screen.Conflict,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                val dir = if (targetState != null) 1 else -1
                                if (reduce) fadeIn(tween(90)) togetherWith fadeOut(tween(90))
                                else (slideInHorizontally(tween(Motion.LONG, easing = Motion.emphasizedDecelerate)) { dir * it / 20 } +
                                    fadeIn(tween(Motion.MEDIUM, delayMillis = Motion.SHORT / 2))) togetherWith
                                    (slideOutHorizontally(tween(Motion.SHORT, easing = Motion.emphasizedAccelerate)) { -dir * it / 30 } +
                                        fadeOut(tween(Motion.SHORT)))
                            },
                            label = "conflict",
                        ) { conflict ->
                            if (conflict != null) Box(Modifier.padding(top = LocalTopBand.current.height)) {
                                ConflictScreen(container, state, conflict.deckId, wide)
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
                    if (chrome != null && !wide) {
                        val density = LocalDensity.current
                        Box(Modifier.align(Alignment.TopEnd).padding(top = 11.dp, end = 8.dp)
                            .onSizeChanged { controlsWidth = with(density) { it.width.toDp() } + 8.dp }) {
                            // In the header's row, so as tall as its buttons rather than the nav pill.
                            CompositionLocalProvider(LocalChromeHeight provides ControlSize) { chrome.controls() }
                        }
                    }
                    // Another device asking to send decks or pair, over whichever screen is open.
                    SyncPrompts(sync)
                    if (wide) {
                        val density = LocalDensity.current
                        FloatingTopNav(deckCount, cardCount, state.tab, state::select, icon = { TabIcon(it) }, trailing = chrome?.controls) { start, end, height ->
                            band = with(density) { TopBand(height.toDp(), start.toDp(), end.toDp()) }
                        }
                    }
                    }
                }
            }
            // A fresh install gets the welcome tour once the boot screen has gone.
            val welcomed by settings.welcomed.collectAsState()
            LaunchedEffect(state.booted) { if (state.booted && !welcomed) state.touring = true }
            AnimatedVisibility(state.touring, enter = fadeIn(motion()), exit = fadeOut(motion(Motion.SHORT))) {
                WelcomeTour(container, state, trailing = chrome?.controls) {
                    state.touring = false
                    state.select(Tab.DECKS)
                    state.appScope.launch { settings.setWelcomed() }
                }
            }
            AnimatedVisibility(
                !state.booted,
                exit = if (LocalReduceMotion.current) fadeOut(tween(90))
                    else fadeOut(tween(Motion.SIGNATURE, easing = Motion.emphasizedAccelerate)) + scaleOut(tween(Motion.SIGNATURE), targetScale = 1.06f),
            ) {
                BootScreen(bootStep, trailing = chrome?.controls)
            }
            }
        }
    }
}
}
}

/** The boot screen shows at least this long, so it reads as a screen rather than a flash. */
private val BootMinimum = 1.2.seconds
/** And never longer than this, whatever is still loading. */
private val BootLimit = 8.seconds
private val BootCardsTime = 300.milliseconds
private val BootQuietTime = 250.milliseconds

/** How many deck editors stay built; opening a deck beyond these drops the one opened longest ago. */
private const val KeptDeckLimit = 4

/**
 * Screens composed once and kept: the tabs, and the editors of recently opened decks. Only [shown]
 * (and a screen still fading out) is measured, placed and drawn, so a kept screen costs nothing per
 * frame; showing it again needs no new composition, which is what made a first look at Cards or a
 * deck stutter. While not [booted], every screen lays out and draws under the opaque boot screen, so
 * its cards fill in and their images are decoded and on the GPU before anyone looks. They draw at
 * just under full opacity, the way a crossfade draws them, since the GPU sets that up slowly the
 * first time; a fully transparent layer wouldn't draw at all. [slide] drifts screens in from the
 * side as they show and back out as they go.
 */
@Composable
private fun <K : Any> KeptScreens(keys: List<K>, shown: K?, booted: Boolean, slide: Boolean = false, content: @Composable (K) -> Unit) {
    val reduce = LocalReduceMotion.current
    val shows = keys.map { k ->
        key(k) {
            val show = remember { Animatable(0f) }
            LaunchedEffect(k == shown, reduce) {
                show.animateTo(
                    if (k == shown) 1f else 0f,
                    when {
                        reduce -> tween(90)
                        k == shown -> tween(Motion.MEDIUM, delayMillis = Motion.SHORT / 2)
                        else -> tween(Motion.SHORT)
                    },
                )
            }
            show
        }
    }
    Layout(
        content = {
            keys.forEachIndexed { i, k ->
                key(k) {
                    Box(Modifier.fillMaxSize().graphicsLayer {
                        val p = shows[i].value
                        alpha = if (booted) p else 0.99f
                        if (slide && booted) translationX = (1f - p) * size.width / 20f
                    }) { content(k) }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val placeables = measurables.mapIndexed { i, m -> if (!booted || shows[i].value > 0f) m.measure(constraints) else null }
        layout(constraints.maxWidth, constraints.maxHeight) { placeables.forEach { it?.place(0, 0) } }
    }
}

/**
 * Sets up the image loader and tells every card view where its artwork is. With artwork on, every
 * card image not yet on this device downloads in the background, once, so cards work offline.
 */
@Composable
fun ArtworkProvider(container: AppContainer, artworkDir: String, content: @Composable () -> Unit) {
    val store = remember(artworkDir) { ArtworkStore(artworkDir, ::artworkHttpClient) }
    setSingletonImageLoaderFactory { context -> artworkImageLoader(context, store) }
    val printings by container.catalog.printings.collectAsState()
    val showArtwork by container.catalog.showArtwork.collectAsState()
    val artwork = remember(printings, showArtwork) { ArtworkSource(showArtwork, printings) }
    LaunchedEffect(store, printings, showArtwork) { if (showArtwork) store.downloadAll(printings.values.flatten()) }
    CompositionLocalProvider(LocalArtwork provides artwork, LocalArtworkStore provides store, content = content)
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
    /** The window controls on desktop, just right of the pill, so the band keeps clear of them too. */
    trailing: (@Composable () -> Unit)?,
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
        Row(
            Modifier.align(Alignment.TopEnd).onSizeChanged { end = it },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PillNavBar(
                Tab.entries.map { tab -> BottomItem(tab.label) { icon(tab) } }, Tab.entries.indexOf(selected), { onSelect(Tab.entries[it]) },
                labels = true,
            )
            trailing?.invoke()
        }
    }
}

/** The title bar's own row on desktop windows too narrow for the top band. */
/** How far down the drag area reaches on narrow windows: the screen header's row. */
private val CaptionHeight = 64.dp

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


