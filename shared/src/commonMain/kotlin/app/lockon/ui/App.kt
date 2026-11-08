package app.lockon.ui

import app.lockon.ui.components.takeThemeFrame
import app.lockon.ui.components.DialogStage
import app.lockon.ui.components.DialogStageHost
import app.lockon.ui.components.LocalDialogStage
import app.lockon.ui.components.takesThemeFrame
import app.lockon.ui.components.LineIcons
import app.lockon.ui.components.LocalScreenShown
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
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
import app.lockon.ui.components.PillNavHeight
import androidx.compose.material.icons.filled.Settings
import app.lockon.ui.components.BottomItem
import app.lockon.ui.components.glass
import app.lockon.ui.components.glassColors
import app.lockon.ui.components.Accent
import app.lockon.ui.components.OnAccent
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
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
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarDuration
import app.lockon.ui.components.GameToastHost
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.animation.ExitTransition
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.lockon.ui.components.collectIsPressedHeld

private fun Tab.icon(): ImageVector = when (this) {
    Tab.DECKS -> app.lockon.ui.components.GameIcons.Deck
    Tab.CARDS -> app.lockon.ui.components.GameIcons.Card
    Tab.SYNC -> LineIcons.Sync
    Tab.SETTINGS -> LineIcons.Settings
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
    /** Hears the theme's background colour, for the window behind the app. */
    windowBackground: (Color) -> Unit = {},
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
    val stage = remember { if (app.lockon.platform.isMobile) null else DialogStage() }
    // A change of theme keeps the window's last frame in the old one, to fade away over the new. Waits for
    // that frame at most briefly, since a minimised window draws none.
    var shownDark by remember { mutableStateOf(useDark) }
    LaunchedEffect(useDark) {
        if (useDark == shownDark) return@LaunchedEffect
        if (stage != null && !reduce) stage.takeThemeFrame()
        shownDark = useDark
    }
    CompositionLocalProvider(LocalReduceMotion provides reduce, LocalDensity provides density, LocalDialogStage provides stage) {
    ArtworkProvider(container, artworkDir) {
    LockOnTheme(shownDark) {
        val background = MaterialTheme.colorScheme.background
        SideEffect { windowBackground(background) }
        val snackbars = remember { SnackbarHostState() }
        val syncStatus by sync.status.collectAsState()
        // Only the counts, so saving a deck or loading cards doesn't recompose the whole app.
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
        // Syncs that moved changes, auto-sync included, say so wherever you are (C1).
        LaunchedEffect(sync) { sync.notices.collect { state.notify(it) } }
        LaunchedEffect(Unit) { runCatching { rootFocus.requestFocus() } }
        // The boot screen stays up until the decks are in and the screens under it, tabs and recent
        // decks' editors, have drawn their cards, so the first look at each doesn't stutter.
        var bootStep by remember { mutableStateOf(BootStep.DECKS) }
        // Desktop plays the boot screen in a window of its own (BootCover), which shares this clock.
        val bootCover = state.bootCover
        val bootClock = bootCover?.clock ?: state.bootClock
        val coverShowing by (bootCover?.showing ?: remember { MutableStateFlow(false) }).collectAsState()
        LaunchedEffect(bootStep) { bootCover?.step(bootStep) }
        val bootReduce = LocalReduceMotion.current
        // Reduced motion holds the boot screen still; a cover that started before Settings could say so steps aside.
        LaunchedEffect(bootReduce) { if (bootReduce) bootCover?.lift() }
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
        // Screens built under the boot screen so far. All of them at once took the first frame well over
        // a second, with the window up but frozen, so they build one a frame while the boot screen plays.
        val warmed = remember { mutableStateListOf<Any>() }
        if (!state.booted) LaunchedEffect(Unit) {
            val started = TimeSource.Monotonic.markNow()
            withTimeoutOrNull(BootLimit) {
                // The boot screen draws first, so the window opens on it.
                repeat(2) { withFrameNanos { } }
                // The decks changed most recently are the likeliest to be opened: build their editors too.
                val recent = container.decks.liveDecks.first().filterNot { it.snapshot.deleted }
                    .sortedByDescending { it.updatedAt }.take(KeptDeckLimit).map { it.deckId }
                // A deck already open is kept already; a second editor would draw beneath the first.
                val fresh = recent.filter { id -> keptDecks.none { it.deckId == id } }
                recentDecks += fresh
                keptDecks += fresh.map { Screen.DeckEditor(it) }
                bootStep = BootStep.CARDS
                // The tab that opens first goes first. Each screen starts loading its card images as it builds.
                for (tab in listOf(state.tab) + (Tab.entries - state.tab)) {
                    warmed += tab
                    withFrameNanos { }
                }
                bootStep = BootStep.EDITOR
                for (id in recent) {
                    warmed += id
                    withFrameNanos { }
                }
                // Wait until no image has been loading for a moment, since more start as each screen fills in.
                while (true) {
                    ArtworkLoads.inFlight.first { it == 0 }
                    if (withTimeoutOrNull(BootQuietTime) { ArtworkLoads.inFlight.first { it > 0 } } == null) break
                }
                // Polled rather than per frame, so nothing redraws for it.
                if (bootCover?.showing?.value == true) {
                    // A cover plays in real time from before the app started building, so the lock alone
                    // says when: a further minimum would only hold finished screens back.
                    while (!bootClock.locked) delay(16)
                } else {
                    val shown = started.elapsedNow()
                    if (shown < BootMinimum) delay(BootMinimum - shown)
                    // It waits for the seeker to lock, so it seldom fades out mid-hunt, but not past
                    // [BootLockGrace]: here its own frames keep stalling, and it can fall well behind.
                    withTimeoutOrNull(BootLockGrace) { while (!bootReduce && !bootClock.locked) delay(16) }
                }
            }
            state.booted = true
            bootCover?.lift()
        }
        // At most weekly, only for people on the official card list, and only while checks are on.
        LaunchedEffect(Unit) {
            if (settings.catalogChecks.value) state.appScope.launch {
                runCatching { container.catalogUpdater.quietWeeklyCheck(kotlin.time.Clock.System.now().toEpochMilliseconds()) }
            }
        }

        Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(
            Modifier.fillMaxSize()
                // Desktop shortcuts (UX rule R2): Ctrl+F searches cards, Esc goes back.
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.key == Key.F && (event.isCtrlPressed || event.isMetaPressed) -> {
                            // Decks has its own search; everywhere else it means the cards.
                            if (state.current == null && state.tab != Tab.CARDS && state.tab != Tab.DECKS) state.select(Tab.CARDS)
                            state.requestSearchFocus(); true
                        }
                        event.key == Key.Escape && state.canGoBack -> { state.goBack(); true }
                        else -> false
                    }
                }
                .focusRequester(rootFocus)
                .focusable()
                .then(if (stage != null) Modifier.takesThemeFrame(stage) else Modifier),
        ) {
            val wide = maxWidth >= 840.dp
            @Composable
            fun TabIcon(tab: Tab) {
                // The only status signal outside its screen (UX rule C1): a dot when sync failed.
                if (tab == Tab.SYNC) {
                    BadgedBox(badge = {
                        androidx.compose.animation.AnimatedVisibility(
                            syncStatus.isError,
                            enter = fadeIn(motion()) + androidx.compose.animation.scaleIn(motion(), initialScale = 0.6f),
                            exit = fadeOut(motion(Motion.SHORT)),
                        ) { Badge() }
                    }) { Icon(tab.icon(), contentDescription = null) }
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
            val statusBarInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top)
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                // Snackbars sit above the floating nav bar on phones.
                snackbarHost = { GameToastHost(snackbars, Modifier.padding(bottom = if (wide) 0.dp else navClearance() - 12.dp)) },
                // Desktop navigation floats over each screen's header (below); phones keep it at the
                // bottom, in thumb reach (UX rule N5).
                // Screens clear the status bar themselves, so the deck banner's art can reach under it.
                contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            ) { padding ->
                Row(Modifier.fillMaxSize().padding(padding)) {
                    Box(Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalTopBand provides if (wide) band else TopBand(end = if (chrome != null) controlsWidth else 0.dp), LocalNavClearance provides if (wide) 0.dp else navClearance()) {
                        // Tabs are built once and kept: switching only shows a screen that's already laid
                        // out, with its cards drawn. They fade through each other (UX rule V5).
                        KeptScreens(Tab.entries, if (state.current == null) state.tab else null, state.booted, warmed) { tab ->
                            Column(Modifier.fillMaxSize().windowInsetsPadding(statusBarInsets)) {
                                when (tab) {
                                    Tab.DECKS -> DeckLibraryScreen(container, state, files)
                                    Tab.CARDS -> CardBrowserScreen(container, state, wide)
                                    Tab.SYNC -> {
                                        ScreenHeader("Sync")
                                        // The shared, already decoded list: this tab stays composed, and the
                                        // unshared flow decoded every deck on the UI thread after each save.
                                        val all by container.decks.liveDecks.collectAsState(initial = emptyList())
                                        val decks = remember(all) { all.filterNot { it.snapshot.deleted } }
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                            Box(Modifier.widthIn(max = 1240.dp)) { SyncScreen(
                                                sync, container.identity.deviceName, decks, container.catalog.cards.collectAsState().value,
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
                        KeptScreens(decksShown.map { it.deckId }, openDeck?.deckId, state.booted, warmed, slide = true) { id ->
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
                            if (conflict != null) Box(Modifier.windowInsetsPadding(statusBarInsets).padding(top = LocalTopBand.current.height)) {
                                ConflictScreen(container, state, conflict.deckId, wide)
                            }
                        }
                    }
                    // Phones: the floating pill at the top level. Inside a deck the editor floats its own
                    // sections there instead, and back returns here (UX rules N3, N5).
                    // It sinks away as a deck opens while the editor's own pill fades in, so there is never
                    // a moment with no pill.
                    if (!wide) {
                        val reduce = LocalReduceMotion.current
                        // Named in full: this Box sits in a Row, whose own AnimatedVisibility would be picked.
                        androidx.compose.animation.AnimatedVisibility(
                            visible = state.current == null,
                            modifier = Modifier.align(Alignment.BottomCenter),
                            enter = if (reduce) fadeIn(tween(90)) else fadeIn(tween(Motion.MEDIUM)) + slideInVertically(tween(Motion.MEDIUM, easing = Motion.emphasizedDecelerate)) { it / 4 },
                            exit = if (reduce) fadeOut(tween(90)) else fadeOut(tween(Motion.MEDIUM)) + slideOutVertically(tween(Motion.MEDIUM, easing = Motion.emphasizedAccelerate)) { it / 4 },
                        ) {
                            Box(Modifier.fillMaxWidth()) {
                                FloatingPillNav(Tab.entries.map { tab -> BottomItem(tab.label) { TabIcon(tab) } }, Tab.entries.indexOf(state.tab)) {
                                    state.select(Tab.entries[it])
                                }
                            }
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
                        FloatingTopNav(
                            state.tab, state::select, icon = { TabIcon(it) },
                            back = when (state.current) { null -> null; is Screen.Conflict -> "Back"; else -> "Decks" }, onBack = state::goBack,
                            trailing = chrome?.controls) { start, end, height ->
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
                // A cover fades out over the app itself, so this one goes at once beneath it.
                exit = if (coverShowing) ExitTransition.None
                    else if (LocalReduceMotion.current) fadeOut(tween(90))
                    else fadeOut(tween(Motion.SIGNATURE, easing = Motion.emphasizedAccelerate)) + scaleOut(tween(Motion.SIGNATURE), targetScale = 1.06f),
            ) {
                BootScreen(bootStep, bootClock, playing = !coverShowing, trailing = chrome?.controls)
            }
            }
        }
        // Beside the root rather than inside it: a theme change's picture holds the root's layers, so
        // drawing that picture from within them would draw itself forever.
        if (stage != null) DialogStageHost(stage)
        }
    }
}
}
}

/** The boot screen shows at least this long, and then until its first lock has landed, for at most [BootLockGrace] more. */
private val BootMinimum = 1.9.seconds
private val BootLockGrace = 0.6.seconds
/** And never longer than this, whatever is still loading or however far its seeker has got. */
private val BootLimit = 8.seconds
private val BootQuietTime = 250.milliseconds

/** How many deck editors stay built; opening a deck beyond these drops the one opened longest ago. */
private const val KeptDeckLimit = 4

/**
 * Screens composed once and kept: the tabs, and the editors of recently opened decks. Only [shown]
 * (and a screen still fading out) is measured, placed and drawn, so a kept screen costs nothing per
 * frame; showing it again needs no new composition, which is what made a first look at Cards or a
 * deck stutter. While not [booted], each screen in [warmed] lays out and draws under the opaque boot
 * screen, so its cards fill in and their images are decoded and on the GPU before anyone looks. They draw at
 * half opacity, and sliding screens part way through their slide, the way a crossfade draws them,
 * since the GPU sets that up slowly the first time; a fully transparent layer wouldn't draw at all. [slide] drifts screens in from the
 * side as they show and back out as they go.
 */
@Composable
private fun <K : Any> KeptScreens(keys: List<K>, shown: K?, booted: Boolean, warmed: List<Any>, slide: Boolean = false, content: @Composable (K) -> Unit) {
    val reduce = LocalReduceMotion.current
    val (shows, laidOut) = keys.map { k ->
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
            // Whether the screen takes part in layout changes only when a fade starts or ends; reading the
            // fade's value in measure instead would measure every screen again on each frame of the fade.
            show to remember { derivedStateOf { show.value > 0f } }
        }
    }.unzip()
    Layout(
        content = {
            keys.forEachIndexed { i, k ->
                key(k) {
                    Box(Modifier.fillMaxSize().graphicsLayer {
                        val p = shows[i].value
                        alpha = if (booted) p else 0.5f
                        if (slide) translationX = (1f - (if (booted) p else 0.5f)) * size.width / 20f
                    }) {
                        if (booted || k in warmed) CompositionLocalProvider(LocalScreenShown provides (k == shown)) { content(k) }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val placeables = measurables.mapIndexed { i, m -> if (!booted || laidOut[i].value) m.measure(constraints) else null }
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
    selected: Tab,
    onSelect: (Tab) -> Unit,
    icon: @Composable (Tab) -> Unit,
    /** Inside a deck: what the player card goes back to; null shows the brand. */
    back: String?,
    onBack: () -> Unit,
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
        // As tall as the nav pill, so the brand or the smaller back button lines up with its middle.
        Box(Modifier.align(Alignment.TopStart).height(PillNavHeight).onSizeChanged { start = it }, contentAlignment = Alignment.CenterStart) {
            PlayerCard(back, onBack)
        }
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
 * The brand as a player card: a ringed badge with the Lock On mark overlapping a glass name plate.
 * Inside a deck it becomes the way out ([back] names where it goes): the reticle turns into an
 * arrow, the name gives way to [back], and the whole plate is the button, so going back keeps the
 * corner people look to for it.
 */
@Composable
private fun PlayerCard(back: String?, onBack: () -> Unit) {
    val accent = Accent
    val scheme = MaterialTheme.colorScheme
    // Kept while the plate turns back into the brand, so the label fades out rather than vanishing.
    var lastBack by remember { mutableStateOf("Decks") }
    if (back != null) lastBack = back
    val turn by animateFloatAsState(if (back != null) 1f else 0f, motion(Motion.LONG), label = "player-card-back")
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedHeld()
    val live = back != null
    val squish by animateFloatAsState(if (live && pressed) 0.96f else 1f, motion(Motion.SHORT), label = "player-card-press")
    val ring by animateColorAsState(if (live && hovered && !app.lockon.ui.components.desktopControls) accent.copy(alpha = 0.7f) else Color.Transparent, motion(Motion.SHORT), label = "player-card-ring")
    val textStyle = MaterialTheme.typography.titleLarge
    Box(
        Modifier.graphicsLayer { scaleX = squish; scaleY = squish }
            .then(
                if (live) Modifier.hoverable(interaction)
                    .clickable(interaction, indication = null, role = Role.Button, onClickLabel = "Back to ${back.lowercase()}", onClick = onBack)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .semantics { contentDescription = "Back to ${back.lowercase()}" }
                else Modifier,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        // It stays while it fades, so the plate gives way to the back label rather than vanishing.
        if (turn < 1f) Box(
            Modifier
                .graphicsLayer { alpha = 1f - turn }
                .shadow(10.dp, NamePlate, ambientColor = glassColors().shade, spotColor = glassColors().shade)
                .glass(NamePlate, strong = true)
                .border(1.dp, ring, NamePlate)
                .padding(start = 62.dp, end = 22.dp, top = 12.dp, bottom = 12.dp),
        ) {
            // Both names are laid out at once, so the plate keeps one width and nothing beside it moves.
            Text(
                "Lock On", style = textStyle, color = scheme.onSurface, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.2.sp,
                modifier = Modifier.graphicsLayer { alpha = 1f - turn; translationY = -turn * 10.dp.toPx() },
            )
            Text(
                lastBack, style = textStyle, color = if (hovered && live) scheme.primary else scheme.onSurface,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 0.2.sp,
                modifier = Modifier.graphicsLayer { alpha = turn; translationY = (1f - turn) * 10.dp.toPx() },
            )
        }
        Box(Modifier.graphicsLayer { translationX = (1f - turn) * 6.dp.toPx() }) { LevelBadge(turn, ring, live && hovered, live && pressed) }
    }
}

private val NamePlate = RoundedCornerShape(18.dp)

/** The logo tile's gradient by day: violet, a colour nothing else in the header uses. */
internal val LogoTileDay = listOf(Color(0xFF9A68E6), Color(0xFF7A55DC))
/** At twilight: pale lilac. */
internal val LogoTileTwilight = listOf(Color(0xFFD2BEFF), Color(0xFFB8A2F8))
/** The mark on the tile at twilight: deep violet-navy. By day it is white. */
internal val LogoInkTwilight = Color(0xFF231A52)

/** The Lock On mark on its rounded tile, in the theme's colours. */
@Composable
internal fun BrandTile(size: Dp) {
    val twilight = isTwilight()
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        Modifier.size(size).shadow(8.dp, shape, ambientColor = glassColors().shade, spotColor = glassColors().shade)
            .clip(shape).background(Brush.linearGradient(if (twilight) LogoTileTwilight else LogoTileDay)),
        contentAlignment = Alignment.Center,
    ) { LockOnMark(size = size * 0.6f, reticleColor = if (twilight) LogoInkTwilight else Color.White) }
}

/** The logo tile in the header. As [turn] goes to 1 the mark spins away and a back arrow spins in where it was. */
@Composable
private fun LevelBadge(turn: Float, ring: Color, hovered: Boolean, pressed: Boolean) {
    val twilight = isTwilight()
    val desktop = app.lockon.ui.components.desktopControls
    val plain by animateColorAsState(
        if (desktop) app.lockon.ui.components.neutralFill(hovered, pressed) else glassColors().strong,
        motion(Motion.SHORT), label = "back-fill",
    )
    val ink = androidx.compose.ui.graphics.lerp(if (twilight) LogoInkTwilight else Color.White, MaterialTheme.colorScheme.onSurface, turn)
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.size(44.dp).shadow(4.dp, shape, ambientColor = glassColors().shade, spotColor = glassColors().shade)
            .clip(shape)
            // Going back, the tile turns to a plain button. Each layer goes once the other has fully
            // taken over: a gradient left under the fill would tint the fill's anti-aliased corners.
            .then(if (turn > 0f) Modifier.background(plain) else Modifier)
            .then(if (turn < 1f) Modifier.background(Brush.linearGradient(if (twilight) LogoTileTwilight else LogoTileDay), alpha = 1f - turn) else Modifier)
            // The logo's rim fades with it on desktop, so the back button has no outline its neighbours lack.
            .border(1.5.dp, if (ring == Color.Transparent) Color.White.copy(alpha = if (desktop) 0.4f * (1f - turn) else 0.4f) else ring, shape),
        contentAlignment = Alignment.Center,
    ) {
        LockOnMark(
            size = 28.dp, reticleColor = ink,
            modifier = Modifier.graphicsLayer { alpha = 1f - turn; rotationZ = turn * 90f; scaleX = 1f - turn * 0.4f; scaleY = 1f - turn * 0.4f },
        )
        Icon(
            LineIcons.Back, contentDescription = null, tint = ink,
            modifier = Modifier.size(22.dp).graphicsLayer { alpha = turn; rotationZ = (turn - 1f) * 90f; scaleX = 0.6f + turn * 0.4f; scaleY = 0.6f + turn * 0.4f },
        )
    }
}


