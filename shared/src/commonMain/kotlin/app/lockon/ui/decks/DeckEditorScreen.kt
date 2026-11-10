package app.lockon.ui.decks
import app.lockon.ui.components.Reveal
import app.lockon.ui.components.FadeSwap
import app.lockon.ui.components.fadeOnChange
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import app.lockon.ui.components.LineIcons
import app.lockon.ui.components.revealOut
import app.lockon.ui.components.revealIn
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import app.lockon.ui.components.SlidingPages
import app.lockon.ui.components.LocalTopBand
import app.lockon.ui.components.Accent
import androidx.compose.material.icons.filled.KeyboardArrowUp
import app.lockon.ui.components.GlassIconButton
import app.lockon.ui.components.PlainIconButton
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.lockon.rules.DeckAnalysis
import app.lockon.ui.components.FloatingPillNav
import app.lockon.ui.components.floatingPillWidth
import app.lockon.ui.components.LocalNavClearance
import app.lockon.ui.components.navClearance
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import app.lockon.ui.components.BottomItem
import app.lockon.ui.components.GameIcons
import app.lockon.ui.components.ProgressGauge
import androidx.compose.material.icons.filled.MoreVert
import app.lockon.ui.components.GameMenu
import app.lockon.ui.components.GameMenuDivider
import app.lockon.ui.components.GameMenuItem
import app.lockon.ui.components.StateTag
import app.lockon.ui.components.ViewToggle
import app.lockon.ui.components.Glyphs
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import app.lockon.data.EditorMoreItem
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import app.lockon.ui.components.collectIsPressedHeld
import androidx.compose.foundation.layout.size
import app.lockon.ui.components.DeckCard
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import app.lockon.ui.components.glassColors
import app.lockon.ui.components.Motion
import app.lockon.ui.components.PillChip
import app.lockon.ui.components.motion
import app.lockon.ui.components.shineSweep

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lockon.AppContainer
import app.lockon.model.Card as GameCard
import app.lockon.model.CardColor
import app.lockon.model.CardType
import app.lockon.model.DeckEntry
import app.lockon.model.DeckSection
import app.lockon.model.Ruleset
import app.lockon.model.DeckSnapshot
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import app.lockon.model.DeckVersion
import app.lockon.model.MatchRecord
import app.lockon.platform.formatTimestamp
import app.lockon.data.DeckRepository
import app.lockon.data.EditorBarItem
import app.lockon.data.EditorBarOrder
import kotlin.time.Clock
import kotlin.uuid.Uuid
import app.lockon.platform.FileBridge
import app.lockon.rules.DeckValidator
import app.lockon.ui.AppState
import app.lockon.ui.Screen
import app.lockon.ui.cards.CardSearchPanel
import app.lockon.ui.cards.DeckControls
import app.lockon.ui.cards.CardDrag
import app.lockon.ui.cards.CardDragGhost
import app.lockon.ui.cards.cardDropTarget
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import app.lockon.ui.components.Badge
import app.lockon.ui.components.DeckGauge
import app.lockon.ui.components.LockIndicator
import app.lockon.ui.components.RollingNumber
import app.lockon.ui.components.CautionStripes
import app.lockon.ui.components.ColorPips
import app.lockon.ui.components.SectionHeader
import app.lockon.ui.components.CardDetailDialog
import app.lockon.ui.components.CardImage
import app.lockon.ui.components.CardThumb
import app.lockon.ui.components.EmptyState
import app.lockon.ui.components.Panel
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.theme.cardColor
import app.lockon.ui.components.CardArtwork
import app.lockon.ui.components.CardBack
import app.lockon.ui.components.DeckStrip
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.PillTabs
import app.lockon.ui.components.LocalArtwork
import app.lockon.ui.components.MiniCurve
import app.lockon.ui.components.PlateStepper
import app.lockon.ui.components.glassFrame
import app.lockon.rules.LegalityMessage
import app.lockon.platform.isMobile
import app.lockon.ui.components.glass
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.BoxWithConstraints
import app.lockon.ui.components.clearOfNav
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.produceState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.withFrameNanos
import app.lockon.platform.resizeHorizontalPointer
import app.lockon.ui.components.VerticalScrollBar
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import app.lockon.ui.components.rememberPrefetchingListState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.rememberCoroutineScope
import app.lockon.ui.components.fadeEnd

private const val AUTOSAVE_DELAY_MS = 400L

/** One editor save at a time, across decks, in the order they were asked for. */
private val EditorSaves = Mutex()

/** The card list's share of the wide editor's width, until the person drags the divider. */
private const val DEFAULT_SPLIT = 0.615f
private val SPLIT_HANDLE = 10.dp
private val MIN_CARDS_WIDTH = 360.dp
private val MIN_DECK_WIDTH = 380.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeckEditorScreen(container: AppContainer, state: AppState, files: FileBridge, deckId: String, wide: Boolean, startTab: Int = 0) {
    // Coming from the library the deck is already loaded, so the whole editor draws on the first frame,
    // before the screen starts sliding in, instead of halfway through it.
    val cached = remember(deckId) { container.decks.cachedDeck(deckId) }
    var loaded by remember(deckId) { mutableStateOf(cached != null) }
    // The deck as saved. Each save changes it, so nothing here reads it whole while composing: only
    // the parts that seldom change, or the editor would build itself again after every save.
    val summary = produceState(cached, deckId) { container.decks.liveDeck(deckId).collect { value = it; loaded = true } }
    val missing by remember(deckId) { derivedStateOf { summary.value == null } }
    val hasConflict by remember(deckId) { derivedStateOf { summary.value?.hasConflict == true } }
    val cards by container.catalog.cards.collectAsState()
    val printings by container.catalog.printings.collectAsState()
    val ruleset by container.catalog.ruleset.collectAsState()
    val autoDeckColors by container.settings.autoDeckColors.collectAsState()
    val barOrder by container.settings.editorBar.collectAsState()
    val moreHidden by container.settings.moreHidden.collectAsState()
    val devices by container.devices.pairedDevices.collectAsState(initial = emptyList())
    var history by remember(deckId) { mutableStateOf<List<DeckVersion>>(emptyList()) }

    // The editor works on a local draft and autosaves it. Changes that arrive by sync replace the
    // draft only when nothing local is waiting to be saved.
    var draft by remember(deckId) { mutableStateOf(cached?.snapshot) }
    var saved by remember(deckId) { mutableStateOf(cached?.snapshot) }
    // The revision the draft builds on, so an edit that syncs in meanwhile becomes a conflict, not lost.
    var base by remember(deckId) { mutableStateOf(cached?.headRevisionId) }
    LaunchedEffect(deckId) {
        snapshotFlow { summary.value }.collect { incoming ->
            if (incoming != null && (draft == null || draft == saved)) { draft = incoming.snapshot; saved = incoming.snapshot; base = incoming.headRevisionId }
        }
    }
    // Saves run one at a time in the order they were asked for. Two launched together could otherwise
    // land out of order, and the older copy, read back as the deck, would drop the newer change.
    fun persist(snapshot: DeckSnapshot, from: String?) {
        state.appScope.launch(start = CoroutineStart.UNDISPATCHED) { EditorSaves.withLock { container.decks.save(deckId, snapshot, from) } }
    }
    LaunchedEffect(draft) {
        val toSave = draft ?: return@LaunchedEffect
        if (toSave == saved) return@LaunchedEffect
        delay(AUTOSAVE_DELAY_MS)
        val from = base
        persist(toSave, from)
        saved = toSave
    }
    // Saves without the autosave pause, for changes too important to sit in the draft.
    fun saveNow(next: DeckSnapshot) {
        draft = next
        val from = base
        persist(next, from)
        saved = next
    }
    DisposableEffect(deckId) {
        onDispose { draft?.takeIf { it != saved }?.let { last -> persist(last, base) } }
    }

    // The editor fills in over its first frames: its frame, then the deck, then the card list. Each
    // is a large first composition, and together they held one frame for over 100 ms (several
    // hundred the first time), freezing the library as it faded out. The screen fades in after a
    // delay, so the steps land before it shows.
    var stage by remember(deckId) { mutableIntStateOf(0) }
    LaunchedEffect(deckId) { while (stage < 2) withFrameNanos { stage++ } }

    val deck = draft
    // Still loading: show nothing rather than a moment of "Deck not found".
    if (!loaded && deck == null) return
    if (missing && deck == null) {
        EmptyState("Deck not found", "It may have been deleted on another device.") {
            GameButton("Back", onClick = state::goBack, style = GameButtonStyle.GHOST, compact = true)
        }
        return
    }
    if (deck == null) return
    val legality = remember(deck, cards, ruleset) { DeckValidator.validate(deck, cards, ruleset) }
    // The editor stays built while hidden (KeptScreens), so only Ctrl+F presses made while this deck
    // shows reach its search.
    val focusSeen = remember(deckId) { intArrayOf(state.searchFocusRequests) }
    if ((state.current as? Screen.DeckEditor)?.deckId == deckId) focusSeen[0] = state.searchFocusRequests
    val haptics = LocalHapticFeedback.current
    var wasLegal by remember(deckId) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(legality.isLegal) {
        if (wasLegal == false && legality.isLegal) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        wasLegal = legality.isLegal
    }

    var detail by remember { mutableStateOf<GameCard?>(null) }
    var renaming by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    // Wide windows edit the deck as text in a dialog; phones have a page for it under More.
    var editingText by remember { mutableStateOf(false) }

    // Changes read the draft itself, not this composition's copy, so quick taps never lose a copy
    // and the card list's controls stay the same object from one change to the next.
    fun change(card: GameCard, delta: Int, section: DeckSection = DeckSection.playedIn(card)) {
        val current = draft ?: return
        draft = current.withQuantity(card.cardNumber, section, (current.quantityOf(card.cardNumber, section) + delta).coerceAtLeast(0))
    }

    fun move(card: GameCard, toSide: Boolean) {
        val current = draft ?: return
        val played = DeckSection.playedIn(card)
        draft = if (toSide) current.move(card.cardNumber, played, DeckSection.SIDE)
        else current.move(card.cardNumber, DeckSection.SIDE, played)
    }

    // A list edited as text replaces the deck. A text list names cards, not their art, so cards
    // still in the deck keep the printing they had.
    fun applyList(edited: DeckSnapshot) {
        val current = draft ?: return
        val art = current.entries.associate { (it.cardNumber to it.section) to it.printingId }
        draft = current.copy(
            name = edited.name,
            entries = edited.entries.map { it.copy(printingId = it.printingId ?: art[it.cardNumber to it.section]) },
        )
    }

    // Where the card list's + and dropped cards go: the deck, or the side deck beside it.
    val addToSide = rememberSaveable(deckId) { mutableStateOf(false) }
    fun addTarget(card: GameCard) = if (addToSide.value) DeckSection.SIDE else DeckSection.playedIn(card)

    // Main-deck colors, kept as one object while they stay the same so the card list can skip
    // recomposing when a change doesn't touch them.
    val colorsNow = deck.entries.filter { it.section == DeckSection.MAIN }.flatMap { cards[it.cardNumber]?.colors.orEmpty() }.toSet()
    val deckColors = remember(colorsNow) { colorsNow }

    // The phone editor's tab (0 Deck, 1 Add cards, then Analysis, Test, Matches, Versions, Export, Edit as text).
    var phoneTab by rememberSaveable(deckId) { mutableStateOf(startTab) }
    // Whole small cards or denser strips in the deck list; phones switch it from the bar's ⋮ menu.
    // Until then a deck follows the setting.
    val deckAsCards by container.settings.deckAsCards.collectAsState()
    var cardViewChoice by rememberSaveable(deckId) { mutableStateOf<Boolean?>(null) }
    val cardView = cardViewChoice ?: deckAsCards
    var deckMenu by remember { mutableStateOf(false) }
    // The wide deck panel's tab (Deck, Analysis, Test, Matches, Versions); phones use their own tab row.
    var deckTab by rememberSaveable(deckId) { mutableStateOf(if (startTab >= 2) startTab - 1 else 0) }

    Column(Modifier.fillMaxSize()) {
        // Header: a hero banner with the cover art behind it. Who the deck is sits on the left, its
        // health is grouped on the right (UX rule I2).
        val cover = remember(deck, cards) { deckCover(deck, cards) }
        DeckBanner(cover, wide, legality.isLegal) {
            // On wide windows the whole header sits in the band between the floating navigation
            // plates, so the editor spends no extra row on it (UX rule N5).
            BoxWithConstraints(Modifier.fillMaxWidth().clearOfNav()) {
            val roomy = maxWidth >= 1000.dp
            // A tight band stacks the name over the state, so the deck's problem still has room in words.
            val oneLine = wide && maxWidth >= 560.dp
            Row(
                Modifier.fillMaxWidth().padding(start = if (wide) 0.dp else 12.dp, end = if (wide) 8.dp else 12.dp, top = if (wide) 0.dp else 8.dp, bottom = if (wide) 0.dp else 4.dp)
                    .then(if (wide) Modifier.align(Alignment.CenterStart) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // A solid round button, so the way out reads as a control rather than a stray glyph.
                // Wide windows go back from the player card in the corner instead.
                if (!wide) {
                    GlassIconButton(LineIcons.Back, "Back to decks", state::goBack)
                    Spacer(Modifier.width(16.dp))
                }
                @Composable
                fun NameAndState(content: @Composable () -> Unit) =
                    if (oneLine) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { content() }
                    else Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
                NameAndState {
                    // The name itself opens Rename; wide windows add a quiet pencil beside it as the hint.
                    Row(
                        Modifier.clip(MaterialTheme.shapes.small).clickable(onClickLabel = "Rename deck") { renaming = true }
                            .padding(end = if (wide && roomy) 8.dp else 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(deck.name, style = MaterialTheme.typography.titleLarge,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        // A tight band gives the pencil's room to the name, which still opens Rename.
                        if (wide && roomy) Icon(LineIcons.Edit, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val c = MaterialTheme.colorScheme
                        FadeSwap(deckColors) { ColorPips(it, size = 22.dp) }
                        // The state alone; Analysis has the rest.
                        // The stamp turns over with the Lock On moment instead of swapping under it.
                        Crossfade(legality.isLegal, animationSpec = motion(), label = "legality") { legal -> StateTag(legal) }
                        DeckProblemLine(
                            if ((if (wide) deckTab else phoneTab) == 0 && deck.entries.isNotEmpty()) legality.errors else emptyList(),
                            onOpenAnalysis = { if (wide) deckTab = 1 else phoneTab = 2 }, Modifier.weight(1f),
                        )
                    }
                }
            }
            }
            // The row keeps one height on every tab, so the chip coming and going never moves the page.
            if (!wide) Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 2.dp).heightIn(min = 26.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProgressGauge("Main deck", deck.count(DeckSection.MAIN), ruleset.mainDeckSize, Modifier.weight(1f),
                    compact = true, hold = hasConflict, inline = true, numberLast = true)
            }
        }
        AnimatedVisibility(hasConflict, enter = revealIn(), exit = revealOut()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                    CautionStripes(Modifier.width(10.dp).fillMaxHeight())
                    Text("This deck was changed on two devices. Both versions are kept.", Modifier.weight(1f).padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    GameButton("Compare", onClick = { state.open(Screen.Conflict(deckId)) }, style = GameButtonStyle.GHOST, compact = true)
                }
            }
        }

        // Tapping the open tab again on phones takes its list back to the top.
        var deckToTop by remember(deckId) { mutableIntStateOf(0) }
        var cardsToTop by remember(deckId) { mutableIntStateOf(0) }
        // The editor stays built after you leave (KeptScreens), so each time the deck opens again both
        // lists start back at the top and it opens on the tab asked for: the deck itself, unless the deck
        // is brand new and goes to Add cards. It jumps while the screen is still faded out, so nothing shows it.
        var opened by remember(deckId) { mutableIntStateOf(0) }
        val showingAt = (state.current as? Screen.DeckEditor)?.takeIf { it.deckId == deckId }
        val showing = showingAt != null
        LaunchedEffect(showing) {
            if (showingAt == null) return@LaunchedEffect
            opened++
            phoneTab = showingAt.startTab
            deckTab = if (showingAt.startTab >= 2) showingAt.startTab - 1 else 0
        }
        // One split for every deck, kept across launches. Each editor drags its own copy so a drag only
        // relays out, and takes up the saved one whenever any editor or Settings changes it.
        val split = remember { mutableFloatStateOf(container.settings.editorSplit.value ?: DEFAULT_SPLIT) }
        LaunchedEffect(Unit) { container.settings.editorSplit.collect { split.floatValue = it ?: DEFAULT_SPLIT } }
        val deckList: @Composable (Modifier) -> Unit = { modifier ->
            DeckList(
                deck, cards, ruleset, modifier, compact = wide,
                cardView = cardView, onCardView = { cardViewChoice = it },
                onAddCards = if (wide) null else ({ phoneTab = 1 }),
                scrollToTop = deckToTop,
                jumpToTop = opened,
                onCardClick = { detail = it },
                onChange = { card, delta -> change(card, delta) },
                onSideChange = { card, delta -> change(card, delta, DeckSection.SIDE) },
                onMoveToDeck = { move(it, toSide = false) },
                onNotes = { notes -> draft = draft?.copy(notes = notes) },
            )
        }
        // Resource cards stay out of the card list: the editor doesn't track the resource deck.
        // Tokens are made during play, so they can't be added either.
        val addable = remember(cards) { cards.values.filter { !it.type.isResource && !it.type.isToken } }
        // Copies by section and card number, looked up per card instead of scanning the deck.
        val counts = remember(deck) {
            deck.entries.groupBy { it.section }.mapValues { (_, entries) -> entries.groupingBy { it.cardNumber }.fold(0) { n, e -> n + e.quantity } }
        }
        val countsNow = rememberUpdatedState(counts)
        val colorsNowState = rememberUpdatedState(deckColors)
        // One set of controls for the editor's life. They read the counts through state, so a change
        // recomposes only the cards whose numbers or tags moved, not the whole card list.
        // Wide windows show the card list and the deck side by side, so a card can be dragged across.
        val cardDrag = remember(deckId) { CardDrag { card -> change(card, 1, addTarget(card)) } }
        val controls = remember(deckId, ruleset, wide) {
            fun copies(card: GameCard, section: DeckSection = DeckSection.playedIn(card)) = countsNow.value[section]?.get(card.cardNumber) ?: 0
            DeckControls(
                quantity = { card -> copies(card, addTarget(card)) },
                // Rule limits shown where you act (UX rule S3), before anything goes wrong.
                tag = { card -> FitTags(card, copies(card), copies(card, DeckSection.SIDE), addToSide.value, colorsNowState.value, ruleset) },
                change = { card, delta -> change(card, delta, addTarget(card)) },
                // The side deck is never checked, so its counts show no limit.
                limit = { card -> if (addToSide.value) null else copyLimit(card, ruleset) },
                drag = if (wide) cardDrag else null,
            )
        }
        val search: @Composable (Modifier) -> Unit = { modifier ->
            BoxWithConstraints(modifier) {
                @Composable
                fun AddTo(tabs: Modifier) {
                    PillTabs(listOf("Deck", "Side"), if (addToSide.value) 1 else 0, { addToSide.value = it == 1 }, tabs, soft = true, icons = if (wide) listOf(Glyphs.Rows, Glyphs.SideCard) else null, flush = true)
                }
                // A wide card list fits the switch in the search row, so the cards get a row's height back (UX
                // rule N4, space goes to cards). Below this width the search would lose its placeholder to the
                // row's chips, so it rides above. Phones give it a row of its own beside Deck colors.
                CardSearchPanel(
                    addable, onCardClick = { detail = it }, modifier = Modifier.fillMaxSize(),
                    focusRequests = focusSeen[0], deckColors = deckColors, maxColors = ruleset.maxColors.takeIf { autoDeckColors }, wide = wide,
                    controls = controls, scrollToTop = cardsToTop, jumpToTop = opened,
                    leading = { AddTo(Modifier.width(IntrinsicSize.Max)) },
                    leadingAbove = wide && maxWidth < 760.dp,
                )
            }
        }
        val analysis: @Composable (Modifier) -> Unit = { modifier -> DeckAnalysisPanel(deck, cards, legality, modifier) }
        val test: @Composable (Modifier) -> Unit = { modifier -> SampleHandPanel(deck, cards, modifier) }
        val matchLog: @Composable (Modifier) -> Unit = { modifier ->
            // Opponents named on any deck, most played first. Read here, where the log shows, and not by
            // the editor itself: every deck's save would otherwise build every open editor again.
            val allDecks by container.decks.liveDecks.collectAsState(initial = emptyList())
            val opponents = remember(allDecks) {
                allDecks.flatMap { d -> d.snapshot.matches.map { it.opponent.trim() } }.filter { it.isNotEmpty() }
                    .groupBy { it.lowercase() }.values.sortedByDescending { it.size }.map { it.last() }
            }
            MatchLogPanel(
                deck.matches, opponents, modifier,
                // Games read the draft itself, not this composition's copy, and save at once rather
                // than after the autosave pause: a phone is often locked or put away right after a tap.
                onLog = { result, opponent, wentFirst, notes ->
                    val match = MatchRecord(Uuid.random().toString(), Clock.System.now().toEpochMilliseconds(), result, opponent, wentFirst, notes)
                    draft?.let { saveNow(it.copy(matches = it.matches + match)) }
                },
                // An edited game gets a new id, so syncing reads it as the old game removed and a new one
                // logged, and a device that still has the old copy can't win the merge with it.
                onEdit = { before, after ->
                    draft?.let { current ->
                        val edited = after.copy(id = Uuid.random().toString(), playedAt = before.playedAt)
                        if (current.matches.any { it.id == before.id }) saveNow(current.copy(matches = DeckRepository.mergeMatches(current.matches.filterNot { it.id == before.id } + edited)))
                    }
                },
                onDelete = { match ->
                    draft?.let { saveNow(it.copy(matches = it.matches.filterNot { m -> m.id == match.id })) }
                    state.notify("Removed a ${match.result.label.lowercase()}", "Undo") {
                        draft?.let { saveNow(it.copy(matches = DeckRepository.mergeMatches(it.matches + match))) }
                    }
                },
            )
        }
        val versions: @Composable (Modifier) -> Unit = { modifier ->
            // Loaded only while the panel shows: reading the history decodes every version.
            LaunchedEffect(summary.value) { history = container.decks.history(deckId) }
            VersionsPanel(
                history, deck, cards, { deviceLabel(it, container.identity.deviceId, devices) },
                fromPhone = { id -> if (id == container.identity.deviceId) isMobile else devices.firstOrNull { it.deviceId == id }?.role != app.lockon.sync.DeviceRole.HOST },
                modifier = modifier,
                onKeep = {
                    val keeping = deck
                    saved = keeping
                    state.appScope.launch {
                        container.decks.keepVersion(deckId, keeping)
                        state.notify("Version kept. Your next change starts a new one.")
                    }
                },
                onRestore = { version ->
                    val before = deck.entries
                    draft = deck.copy(entries = version.snapshot.entries)
                    state.notify("Restored the cards from ${formatTimestamp(version.createdAt)}", "Undo") {
                        draft = draft?.copy(entries = before)
                    }
                },
            )
        }

        if (wide) {
            // Two areas, so most of the window is cards and the deck being built (UX rule N4): the card
            // list, and one deck panel whose tabs hold the analysis, test hand, matches and versions.
            Box(Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)) {
            // The gap between the two areas is a handle: dragging it trades width between them.
            val density = LocalDensity.current
            val usable = (maxWidth - SPLIT_HANDLE).coerceAtLeast(0.dp)
            fun clampSplit(f: Float): Float {
                if (usable <= MIN_CARDS_WIDTH + MIN_DECK_WIDTH) return f
                return f.coerceIn(MIN_CARDS_WIDTH / usable, 1f - MIN_DECK_WIDTH / usable)
            }
            // Both areas follow the pointer while the divider is dragged. The split is read only when
            // the card list is measured, so a drag relays out the two areas and recomposes neither.
            // The pointer keeps the resize arrows over the whole editor until it lets go.
            var resizing by remember { mutableStateOf(false) }
            // Where the pointer has taken the divider, which may be past where the areas stop it.
            // Plain values, not state: the drag writes them several times a frame and nothing should observe that.
            val pointerX = remember { FloatArray(1) }
            val pointerMoved = remember { Channel<Unit>(Channel.CONFLATED) }
            fun saveSplit(value: Float?) = state.appScope.launch { container.settings.setEditorSplit(value) }
            fun followPointer() {
                val usablePx = with(density) { usable.toPx() }
                if (usablePx > 0f) split.floatValue = clampSplit(pointerX[0] / usablePx)
            }
            // The split follows the pointer at most once a frame, and only when it moved. On desktop each
            // pointer event lays the window out again, so moving the split on every event laid out the
            // whole editor several times a frame.
            LaunchedEffect(resizing, usable, density) {
                if (resizing) for (moved in pointerMoved) withFrameNanos { followPointer() }
            }
            Row(Modifier.fillMaxSize().pointerHoverIcon(if (resizing) resizeHorizontalPointer else PointerIcon.Default, overrideDescendants = resizing)) {
                Panel(
                    Modifier.fillMaxHeight().layout { measurable, constraints ->
                        val width = (usable * clampSplit(split.floatValue)).roundToPx()
                        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                        layout(width, placeable.height) { placeable.place(0, 0) }
                    },
                ) { if (stage >= 2) search(Modifier.fillMaxSize()) }
                SplitHandle(
                    onDragStart = { resizing = true; pointerX[0] = with(density) { (usable * clampSplit(split.floatValue)).toPx() } },
                    onDrag = { dx -> pointerX[0] += dx; pointerMoved.trySend(Unit) },
                    onDragEnd = { resizing = false; followPointer(); saveSplit(split.floatValue) },
                    onReset = { split.floatValue = DEFAULT_SPLIT; saveSplit(null) },
                )
                // The deck panel takes dropped cards, and its rim lights up while one is held over it.
                val dropRim by animateColorAsState(
                    when {
                        cardDrag.overTarget -> MaterialTheme.colorScheme.primary
                        cardDrag.card != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else -> Color.Transparent
                    },
                    motion(Motion.SHORT), label = "drop-rim",
                )
                Panel(Modifier.weight(1f).fillMaxSize().cardDropTarget(cardDrag)
                    .border(BorderStroke(2.dp, dropRim), LockOnShapes.panel)) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ProgressGauge("Main deck", deck.count(DeckSection.MAIN), ruleset.mainDeckSize, Modifier.weight(1f).padding(end = 8.dp),
                                compact = true, hold = hasConflict, inline = true)
                            GlassIconButton(LineIcons.Export, "Export", { exporting = true }, compact = true)
                            Box {
                                GlassIconButton(LineIcons.More, "More", { deckMenu = true }, compact = true)
                                GameMenu(expanded = deckMenu, onDismissRequest = { deckMenu = false }) {
                                    GameMenuItem("Edit as text", onClick = { deckMenu = false; editingText = true }, icon = LineIcons.Edit)
                                }
                            }
                        }
                        PillTabs(
                            listOf("Deck", "Analysis", "Test", "Matches", "Versions"), deckTab, { deckTab = it }, fill = true,
                            icons = listOf(LineIcons.List, LineIcons.Chart, LineIcons.Play, LineIcons.Trophy, LineIcons.Versions),
                        )
                        if (stage >= 1) SlidingPages(deckTab, Modifier.fillMaxSize()) { page ->
                            when (page) {
                                0 -> deckList(Modifier.fillMaxSize())
                                1 -> analysis(Modifier.fillMaxSize())
                                2 -> test(Modifier.fillMaxSize())
                                3 -> matchLog(Modifier.fillMaxSize())
                                else -> versions(Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
            }
            CardDragGhost(cardDrag, Modifier.matchParentSize())
            }
        } else {
            // The sections float in a pill over the content, which scrolls underneath it.
            Box(Modifier.weight(1f).fillMaxWidth()) {
                // Pages slide in the bar's order, as Settings sets it.
                val pages = phonePages(barOrder)
                if (stage >= 1) SlidingPages(pages.indexOf(phoneTab), Modifier.fillMaxSize()) { position ->
                    when (pages.getOrElse(position) { 0 }) {
                        0 -> deckList(Modifier.fillMaxSize())
                        1 -> search(Modifier.fillMaxSize())
                        2 -> Titled("Analysis") { analysis(Modifier.fillMaxSize()) }
                        3 -> Titled("Test") { test(Modifier.fillMaxSize()) }
                        4 -> Titled("Matches") { matchLog(Modifier.fillMaxSize()) }
                        5 -> Titled("Versions") { versions(Modifier.fillMaxSize()) }
                        6 -> DeckExportPanel(deck, cards, ruleset, files, state, Modifier.fillMaxSize())
                        else -> DeckTextEditor(deck, cards, printings, onApply = ::applyList, Modifier.fillMaxSize())
                    }
                }
                EditorNav(
                    phoneTab, barOrder, EditorMoreItem.entries - moreHidden, cardView,
                    onSelect = {
                        when {
                            it != phoneTab -> phoneTab = it
                            it == 0 -> deckToTop++
                            it == 1 -> cardsToTop++
                        }
                    },
                    onCardView = { cardViewChoice = it; phoneTab = 0 },
                )
            }
        }
    }

    detail?.let { card ->
        val section = DeckSection.playedIn(card)
        val side = deck.quantityOf(card.cardNumber, DeckSection.SIDE)
        val cardPrintings = printings[card.cardNumber].orEmpty()
        val inDeck = deck.quantityOf(card.cardNumber, section)
        // Once the card is in the deck, the printings in the details choose which one the deck uses.
        val choosing = cardPrintings.size > 1 && inDeck > 0
        CardDetailDialog(
            card, cardPrintings, onDismiss = { detail = null },
            chosenPrinting = deck.entries.firstOrNull { it.cardNumber == card.cardNumber && it.section == section }?.printingId,
            onChoosePrinting = if (choosing) { id -> draft = deck.withQuantity(card.cardNumber, section, inDeck, id) } else null,
        ) {
            BoxWithConstraints {
                // Phones: the deck and side deck steppers share one row under small labels, and the
                // buttons say less, so the panel takes half the height of a row per control.
                val narrow = maxWidth < 280.dp
                // Desktop's dialog: both steppers on one row, each beside its name.
                val sideBySide = maxWidth >= 440.dp
                val deckStepper: @Composable () -> Unit = {
                    PlateStepper(inDeck, "copy", onMinus = { change(card, -1) }, onPlus = { change(card, 1) }, limit = copyLimit(card, ruleset), pips = true)
                }
                // The side deck sits beside the deck; moving keeps the total and never checks rules (UX rule S4).
                val sideStepper: @Composable () -> Unit = {
                    PlateStepper(side, "side deck copy", onMinus = { change(card, -1, DeckSection.SIDE) }, onPlus = { change(card, 1, DeckSection.SIDE) })
                }
                Column(verticalArrangement = Arrangement.spacedBy(if (narrow) 2.dp else if (sideBySide) 0.dp else 8.dp)) {
                    if (narrow) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailActionCell(section.label, Modifier.weight(1f), deckStepper)
                            DetailActionCell("Side deck", Modifier.weight(1f), sideStepper)
                        }
                    } else if (sideBySide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            DetailActionRow(section.label, deckStepper, Modifier.weight(1f))
                            // The last + reaches the panel's right edge; its target runs on into the margin.
                            DetailActionRow("Side deck", sideStepper, Modifier.weight(1f).offset(x = 8.dp))
                        }
                    } else {
                        DetailActionRow(section.label, deckStepper)
                        DetailActionRow("Side deck", sideStepper)
                    }
                    // The library shows the deck by this card instead of its highest-level Unit.
                    val canCover = inDeck > 0 || deck.coverCard == card.cardNumber
                    Reveal(side > 0 || inDeck > 0 || canCover) {
                        FlowRow(if (sideBySide) Modifier.offset(x = (-14).dp) else Modifier.padding(top = if (narrow) 0.dp else 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FadeSwap(if (side > 0) 1 else if (inDeck > 0) 2 else 0) { way ->
                                when (way) {
                                    1 -> GameButton(if (narrow) "1 to deck" else "Move 1 to deck", onClick = { move(card, toSide = false) }, style = GameButtonStyle.GHOST, compact = true)
                                    2 -> GameButton(if (narrow) "1 to side" else "Move 1 to side deck", onClick = { move(card, toSide = true) }, style = GameButtonStyle.GHOST, compact = true)
                                }
                            }
                            if (canCover) {
                                val isCover = deck.coverCard == card.cardNumber
                                val label = when {
                                    narrow -> if (isCover) "Remove cover" else "Set as cover"
                                    else -> if (isCover) "Stop using as deck cover" else "Use as deck cover"
                                }
                                GameButton(label, onClick = { draft = deck.copy(coverCard = if (isCover) null else card.cardNumber) }, style = GameButtonStyle.GHOST, compact = true)
                            }
                        }
                    }
                }
            }
        }
    }
    if (renaming) {
        TextPromptDialog("Rename deck", "Deck name", deck.name, "Rename", onDismiss = { renaming = false }) {
            renaming = false
            if (it.isNotBlank()) draft = deck.copy(name = it.trim())
        }
    }
    if (exporting) {
        DeckExportDialog(
            deck, cards, ruleset, files, state, onDismiss = { exporting = false },
        )
    }
    if (editingText) {
        DeckTextDialog(deck, cards, printings, onApply = ::applyList, onDismiss = { editingText = false })
    }
}

@Composable
private fun DeckList(
    deck: DeckSnapshot,
    cards: Map<String, GameCard>,
    ruleset: Ruleset,
    modifier: Modifier,
    /** Slimmer strips for wide windows. */
    compact: Boolean,
    /** Whole small cards rather than strips. */
    cardView: Boolean,
    onCardView: (Boolean) -> Unit,
    /** Opens the card list; phones only, where it is a separate tab. */
    onAddCards: (() -> Unit)?,
    /** Goes up by one each time the list should go back to the top. */
    scrollToTop: Int,
    /** Goes up by one each time the list should be at the top at once, without scrolling there. */
    jumpToTop: Int,
    onCardClick: (GameCard) -> Unit,
    onChange: (GameCard, Int) -> Unit,
    onSideChange: (GameCard, Int) -> Unit,
    onMoveToDeck: (GameCard) -> Unit,
    onNotes: (String) -> Unit,
) {
    // Grouped by card type like a printed list, each under a HUD header with its count (UX rule V2).
    val groups = remember(deck, cards) {
        val order = compareBy<DeckEntry>({ cards[it.cardNumber]?.level ?: 99 }, { cards[it.cardNumber]?.cost ?: 99 }, { it.cardNumber })
        val main = deck.entries.filter { it.section == DeckSection.MAIN }
        val byType = CardType.entries.filter { !it.isResource }.map { type ->
            "${type.label}s" to main.filter { cards[it.cardNumber]?.type == type }.sortedWith(order)
        } + ("Unknown cards" to main.filter { cards[it.cardNumber] == null })
        byType.filter { it.second.isNotEmpty() }
    }
    val gap = 8.dp
    val listState = rememberPrefetchingListState()
    LaunchedEffect(scrollToTop) { if (scrollToTop > 0) listState.animateScrollToItem(0) }
    LaunchedEffect(jumpToTop) { if (jumpToTop > 0) listState.requestScrollToItem(0) }
    val side = remember(deck, cards) {
        deck.entries.filter { it.section == DeckSection.SIDE }.sortedWith(compareBy({ cards[it.cardNumber]?.level ?: 99 }, { it.cardNumber }))
    }
    BoxWithConstraints(modifier) {
    // The end padding clears the scroll bar's lane so section counts don't sit under it.
    // Phones keep three across down to about 390 dp, like the card list, instead of two large cards.
    val columns = ((maxWidth - 28.dp + gap) / ((if (compact) 104.dp else 112.dp) + gap)).toInt().coerceAtLeast(2)
    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize(), listState, contentPadding = PaddingValues(start = 8.dp, end = 20.dp, top = if (compact) 8.dp else 0.dp, bottom = 8.dp + LocalNavClearance.current),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (deck.entries.isEmpty()) {
            item(key = "empty-deck", contentType = "empty") {
                Column(
                    Modifier.animateItem().fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LockIndicator(locked = false, size = 72.dp)
                    Text("No cards yet", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (compact) "Drag a card here or click + to add a copy. A deck needs 50 cards in up to two colors."
                        else "Tap + on any card to add a copy. A deck needs 50 cards in up to two colors.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (onAddCards != null) GameButton("Add cards", onClick = onAddCards, icon = LineIcons.Plus, modifier = Modifier.padding(top = 6.dp))
                }
            }
        } else if (deck.count(DeckSection.MAIN) == 0) {
            item(key = "empty-main", contentType = "empty-main") {
                Row(Modifier.animateItem().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Add Units, Pilots, Commands and Bases from the card list.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (onAddCards != null) GameButton("Add cards", onClick = onAddCards, style = GameButtonStyle.GHOST, compact = true)
                }
            }
        }
        for ((label, entries) in groups) {
            item(key = "header-$label", contentType = "header") {
                SectionHeader(label, Modifier.animateItem().padding(start = 8.dp, end = 8.dp, top = if (!compact && label == groups.first().first) 0.dp else 8.dp, bottom = 4.dp),
                    readout = "${entries.sumOf { it.quantity }}") {
                    if (compact && label == groups.first().first) ViewToggle(
                        cardView, onCardView, small = true,
                        // Out to the edge of the cards below, past the header's inset for its label.
                        modifier = if (app.lockon.platform.isMobile) Modifier else Modifier.offset(x = 6.dp),
                    )
                }
            }
            if (cardView) {
                // Whole cards, small, a row at a time so the list stays lazy (UX rule V4a).
                // Rows keep their place by position: keyed by their first card, adding one card gave every
                // later row a new key, so each was thrown away and built again, images and all.
                val rows = entries.chunked(columns)
                items(rows.size, key = { i -> "row-$label-$i" }, contentType = { "card-row" }) { i ->
                    val row = rows[i]
                    Row(Modifier.animateItem().fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { entry ->
                            val card = cards[entry.cardNumber]
                            if (card != null) {
                                DeckCard(
                                    card, entry.quantity, entry.printingId, onClick = { onCardClick(card) },
                                    onMinus = { onChange(card, -1) }, onPlus = { onChange(card, 1) },
                                    modifier = Modifier.weight(1f), limit = copyLimit(card, ruleset), target = if (compact) 40.dp else 44.dp,
                                )
                            } else {
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    CardImage(null, Modifier.fillMaxWidth())
                                    Text("×${entry.quantity}", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            } else items(entries, key = { "${it.section}-${it.cardNumber}" }, contentType = { "strip" }) { entry ->
                val card = cards[entry.cardNumber]
                DeckStrip(
                    card, entry.quantity, entry.printingId, onClick = { card?.let(onCardClick) }, modifier = Modifier.animateItem(), compact = compact,
                ) {
                    if (card != null) {
                        PlateStepper(entry.quantity, card.name, onMinus = { onChange(card, -1) }, onPlus = { onChange(card, 1) },
                            limit = copyLimit(card, ruleset))
                    } else {
                        Text("×${entry.quantity}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 12.dp))
                    }
                }
            }
        }
        item(key = "side-header", contentType = "side-header") {
            Column(Modifier.animateItem()) {
            SectionHeader(
                "Side deck", Modifier.padding(start = 8.dp, end = 8.dp, top = 24.dp),
                readout = "${side.sumOf { it.quantity }}",
            )
            Text(
                if (side.isEmpty()) {
                    if (compact) "Cards kept beside the deck. Switch the card list to Side deck, then add cards with + or by dragging."
                    else "Cards kept beside the deck. In Add cards, tap Side deck beside the gauge, then add cards with +."
                }
                else "Not counted toward the deck or its legality.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            }
        }
        items(side, key = { "side-${it.cardNumber}" }, contentType = { "side-strip" }) { entry ->
            val card = cards[entry.cardNumber]
            DeckStrip(
                card, entry.quantity, entry.printingId, onClick = { card?.let(onCardClick) }, // Modulated rather than an offscreen layer, which each strip would redraw while scrolling.
                modifier = Modifier.animateItem().graphicsLayer { alpha = 0.82f; compositingStrategy = CompositingStrategy.ModulateAlpha }, compact = compact,
            ) {
                if (card != null) {
                    // One tap per copy (UX rule S1): move one into the deck, or change the side deck's count.
                    // On phones an arrow, so the card's name keeps its room; its label is still read out.
                    if (LocalTopBand.current.height == 0.dp) PlainIconButton(onClick = { onMoveToDeck(card) }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move one ${card.name} to the deck", tint = Accent)
                    }
                    else GameButton("To deck", onClick = { onMoveToDeck(card) }, style = GameButtonStyle.GHOST, compact = true)
                    PlateStepper(entry.quantity, card.name, onMinus = { onSideChange(card, -1) }, onPlus = { onSideChange(card, 1) })
                } else {
                    Text("×${entry.quantity}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 12.dp))
                }
            }
        }
        item(key = "notes", contentType = "notes") {
            var editing by remember { mutableStateOf(false) }
            // The button opens into the field rather than swapping for it.
            Crossfade(deck.notes.isBlank() && !editing, Modifier.animateItem().animateContentSize(motion()), animationSpec = motion(), label = "notes") { empty ->
                if (empty) {
                    GameButton("Add notes", onClick = { editing = true }, style = GameButtonStyle.GHOST, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), compact = true)
                } else {
                    OutlinedTextField(
                        value = deck.notes, onValueChange = onNotes,
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        minLines = 3,
                    )
                }
            }
        }
    }
    VerticalScrollBar(listState, Modifier.align(Alignment.TopEnd))
    }
    }
}

/**
 * The header's line for the deck's first problem in words (UX rule I3), which opens Analysis.
 * It is laid out even with nothing to say, so a problem coming or going never moves the list.
 */
@Composable
private fun DeckProblemLine(errors: List<LegalityMessage>, onOpenAnalysis: () -> Unit, modifier: Modifier) {
    val hasProblem = errors.isNotEmpty()
    val shown by animateFloatAsState(if (hasProblem) 1f else 0f, motion(), label = "problem")
    // The last problem stays while the line fades out.
    val last = remember { arrayOf("" to 0) }
    if (hasProblem) last[0] = errors.first().text to errors.size - 1
    val (text, more) = last[0]
    val color = MaterialTheme.colorScheme.error
    Row(
        modifier.graphicsLayer { alpha = shown }.clip(LockOnShapes.button)
            .clickable(enabled = hasProblem, onClickLabel = "Open analysis", onClick = onOpenAnalysis).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, Modifier.weight(1f, fill = false), color, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (more > 0) Text("+$more", color = color, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

/**
 * "Off-color" when a card would add a third color (UX rule S3), and the
 * count in whichever of the deck and side deck the card list isn't adding to. The copy limit has no
 * badge: the gold pips and the plain "+" under the card say it.
 */
@Composable
private fun FitTags(card: GameCard, quantity: Int, side: Int, addingToSide: Boolean, deckColors: Set<CardColor>, ruleset: Ruleset) {
    val c = MaterialTheme.colorScheme
    val banned = card.cardNumber in ruleset.banned
    val offColor = card.colors.isNotEmpty() && (deckColors + card.colors).size > ruleset.maxColors
    // The tag fades in when another takes its place or one appears; a count going up doesn't refade it.
    val kind = when {
        banned -> 1
        addingToSide -> if (quantity > 0) 2 else 0
        offColor -> 3
        side > 0 -> 4
        else -> 0
    }
    if (kind == 0) { Box(Modifier.fadeOnChange(kind)); return }
    Box(Modifier.fadeOnChange(kind)) {
        when (kind) {
            1 -> Badge("Banned", c.errorContainer, c.onErrorContainer)
            2 -> Badge("Deck $quantity", Color(0xE61A2036), Color.White)
            3 -> Badge("Off-color", c.surfaceVariant, c.onSurfaceVariant)
            else -> Badge("Side $side", Color(0xE61A2036), Color.White)
        }
    }
}

/** One labelled line in the card details' "In this deck" panel: the label left, its control right. */
@Composable
private fun DetailActionRow(label: String, control: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).padding(end = 12.dp))
        control()
    }
}

/** [DetailActionRow] for phones: the label sits over its control, so two fit side by side. */
@Composable
private fun DetailActionCell(label: String, modifier: Modifier, control: @Composable () -> Unit) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, modifier = Modifier.padding(start = 8.dp))
        control()
    }
}

/**
 * The phone editor's tabs, left to right, in the bottom bar's [order]: Add cards (tab 1), Deck
 * (tab 0) and ⋮'s sections (tabs 7 down to 2), so pages slide the way the bar reads.
 */
private fun phonePages(order: EditorBarOrder): List<Int> = order.items.flatMap {
    when (it) { EditorBarItem.MORE -> (7 downTo 2).toList(); EditorBarItem.ADD -> listOf(1); EditorBarItem.DECK -> listOf(0) }
}

/** Which of the bottom bar's buttons a phone editor tab belongs to, counted from the left in [order]. */
private fun phoneNavItem(tab: Int, order: EditorBarOrder): Int =
    order.items.indexOf(when (tab) { 0 -> EditorBarItem.DECK; 1 -> EditorBarItem.ADD; else -> EditorBarItem.MORE })

/** The copy limit for a card under [ruleset]: restricted, resource or the normal maximum. */
private fun copyLimit(card: GameCard, ruleset: Ruleset): Int? =
    ruleset.restricted[card.cardNumber] ?: if (card.type.isResource) ruleset.resourceMaxCopies else ruleset.maxCopies

/** The editor's hero banner: the deck's cover art fades in behind the header from the right. */
@Composable
private fun DeckBanner(cover: DeckCover?, wide: Boolean, locked: Boolean, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val art = cover?.let { LocalArtwork.current.urlFor(it.card.cardNumber, it.printingId) }
    val color = cardColor(cover?.card?.colors?.firstOrNull())
    val glassFill = glassColors().fill
    // The cover art is fainter on wide windows, where the strip runs behind the navigation's labels.
    val artAlpha = if (wide) 0.3f else 0.55f
    // A frosted strip the full width of the window, so the sky shows through it and the floating
    // navigation sits over its ends. The cover art fades in from the right through an alpha mask.
    // On phones it reaches up under the status bar; only the header clears it.
    Box(Modifier.fillMaxWidth().clipToBounds().shineSweep(locked)) {
        Column(Modifier.windowInsetsPadding(ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top))) { content() }
    }
}

/**
 * The divider between the card list and the deck: drag to resize, double-click to reset. A pill grip
 * sits in the gap, and turns the accent colour while hovered or dragged.
 */
@Composable
private fun SplitHandle(onDragStart: () -> Unit, onDrag: (Float) -> Unit, onDragEnd: () -> Unit, onReset: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var dragging by remember { mutableStateOf(false) }
    val start by rememberUpdatedState(onDragStart)
    val drag by rememberUpdatedState(onDrag)
    val end by rememberUpdatedState(onDragEnd)
    val reset by rememberUpdatedState(onReset)
    val scheme = MaterialTheme.colorScheme
    val active = dragging || hovered
    val grip by animateColorAsState(if (active) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.3f), motion(Motion.SHORT))
    Box(
        Modifier.width(SPLIT_HANDLE).fillMaxHeight()
            .hoverable(interaction)
            .pointerHoverIcon(resizeHorizontalPointer)
            .semantics { contentDescription = "Resize the card list and the deck" }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true; start() },
                    onDragEnd = { dragging = false; end() },
                    onDragCancel = { dragging = false; end() },
                ) { change, dx -> change.consume(); drag(dx) }
            }
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { reset() }) },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(4.dp).height(56.dp).clip(RoundedCornerShape(2.dp)).background(grip))
    }
}

/**
 * The editor's sections on phones, at the bottom in thumb reach (UX rules N5, R3). Building the deck
 * comes first, so by default the bar holds More, Add cards and Deck, left to right, with Deck under
 * the right thumb; Settings can put them in any [order]. More opens a grid of the [more] sections
 * (Analysis, Test, Matches, Versions, Export and Edit as text, less any Settings leaves out), two short
 * rows instead of a tall list, and takes the name and icon of whichever of them is open. Holding Deck switches the deck list between cards and a list.
 * Swiping along the bar moves between Add cards and Deck. [tab] and [onSelect] use the editor's tab numbers (0 Deck, 1 Add cards).
 */
@Composable
private fun BoxScope.EditorNav(
    tab: Int,
    order: EditorBarOrder,
    more: List<EditorMoreItem>,
    cardView: Boolean,
    onSelect: (Int) -> Unit,
    onCardView: (Boolean) -> Unit,
) {
    var moreOpen by remember { mutableStateOf(false) }
    val currentTab by rememberUpdatedState(tab)
    val currentOrder by rememberUpdatedState(order)
    val select by rememberUpdatedState(onSelect)
    // How far a swipe along the bar must travel before it switches.
    val swipe = with(LocalDensity.current) { 40.dp.toPx() }
    // A dim over the screen but under the bar, so the menu and the button that opened it stand out. This
    // box starts below the deck's header, so the dim is painted up past its top edge to cover that too.
    val dim by animateFloatAsState(if (moreOpen) 0.45f else 0f, motion(Motion.SHORT), label = "more-dim")
    Spacer(Modifier.matchParentSize().drawBehind {
        if (dim > 0f) {
            val above = 400.dp.toPx()
            drawRect(Color.Black.copy(alpha = dim), Offset(0f, -above), Size(size.width, size.height + above))
        }
    })
    BoxWithConstraints(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .pointerInput(Unit) {
                // One switch per swipe, pushing the pages along like a pager: a swipe left opens the
                // button to the right, a swipe right the one to the left, stepping over More (never into it).
                var travelled = 0f
                var switched = false
                detectHorizontalDragGestures(
                    onDragStart = { travelled = 0f; switched = false },
                ) { change, dx ->
                    change.consume()
                    travelled += dx
                    if (switched) return@detectHorizontalDragGestures
                    val step = when {
                        travelled < -swipe -> 1
                        travelled > swipe -> -1
                        else -> return@detectHorizontalDragGestures
                    }
                    val items = currentOrder.items
                    var slot = phoneNavItem(currentTab, currentOrder) + step
                    while (items.getOrNull(slot) == EditorBarItem.MORE) slot += step
                    val target = when (items.getOrNull(slot)) { EditorBarItem.ADD -> 1; EditorBarItem.DECK -> 0; else -> null }
                    if (target != null) { switched = true; select(target) }
                }
            },
    ) {
        val buttons = mapOf(
            // The three dots are far narrower than their box, so only their own width counts, or the label looks off centre.
            EditorBarItem.MORE to BottomItem("More") {
                Box(Modifier.width(8.dp), contentAlignment = Alignment.Center) {
                    Icon(LineIcons.More, contentDescription = null, modifier = Modifier.requiredSize(20.dp))
                }
            },
            EditorBarItem.ADD to BottomItem("Add") { Icon(LineIcons.Plus, contentDescription = null) },
            // Holding Deck flips the deck list between cards and a text list, and opens it.
            EditorBarItem.DECK to BottomItem(
                "Deck",
                onLongClick = { onCardView(!cardView); onSelect(0) },
                onLongClickLabel = if (cardView) "Show as list" else "Show as cards",
            ) { Icon(LineIcons.List, contentDescription = null, modifier = Modifier.size(20.dp)) },
        )
        FloatingPillNav(
            order.items.map { buttons.getValue(it) },
            // More lights up while its menu is open, and the pill goes back when it closes.
            selected = if (moreOpen) order.items.indexOf(EditorBarItem.MORE) else phoneNavItem(tab, order),
            onSelect = {
                when (order.items[it]) {
                    EditorBarItem.MORE -> moreOpen = true
                    EditorBarItem.ADD -> onSelect(1)
                    EditorBarItem.DECK -> onSelect(0)
                }
            },
            widest = EDITOR_NAV_WIDTH,
            labels = true,
        )
        // As wide as the pill and opening 8 dp above it, so every section is a short thumb move away.
        val pillWidth = floatingPillWidth(maxWidth, EDITOR_NAV_WIDTH)
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = navClearance() + 1.dp).width(pillWidth)) {
            GameMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }, modifier = Modifier.width(pillWidth)) {
                Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    more.chunked(MORE_COLUMNS).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { item ->
                                MoreTile(item, selected = tab == item.tab, Modifier.weight(1f)) { moreOpen = false; onSelect(item.tab) }
                            }
                            // A short last row keeps its tiles the same size as the rows above.
                            repeat(if (more.size > MORE_COLUMNS) MORE_COLUMNS - row.size else 0) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

/** A section of the phone editor under its name, since the bar always reads Deck, Add and More. */
@Composable
private fun Titled(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text(title, Modifier.padding(start = 16.dp, top = 4.dp), style = MaterialTheme.typography.titleLarge)
        content()
    }
}

/** Tiles per row in the More menu: six sections fit in two rows. */
private const val MORE_COLUMNS = 3

/** A phone editor section's icon, the same on its More tile and on the bar while it is open; the menu glyph for the others. */
@Composable
private fun MoreIcon(tab: Int) {
    when (tab) {
        2 -> Icon(LineIcons.Chart, contentDescription = null, modifier = Modifier.size(20.dp))
        3 -> Icon(LineIcons.Play, contentDescription = null, modifier = Modifier.size(20.dp))
        4 -> Icon(LineIcons.Trophy, contentDescription = null, modifier = Modifier.size(20.dp))
        5 -> Icon(LineIcons.Versions, contentDescription = null, modifier = Modifier.size(20.dp))
        6 -> Icon(LineIcons.Export, contentDescription = null)
        7 -> Icon(LineIcons.Edit, contentDescription = null)
        else -> Icon(LineIcons.More, contentDescription = null)
    }
}

/** One section in the More menu: its icon over its name, teal while it is the open one, washed on hover and press like a menu row. */
@Composable
private fun MoreTile(item: EditorMoreItem, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tone = if (selected) Accent else MaterialTheme.colorScheme.onSurface
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedHeld()
    val wash = when {
        pressed -> 0.20f
        hovered -> 0.12f
        selected -> 0.10f
        else -> 0f
    }
    val fill by animateColorAsState(tone.copy(alpha = wash), motion(Motion.SHORT), label = "more-tile")
    Column(
        modifier.heightIn(min = 72.dp).clip(LockOnShapes.tile).background(fill)
            .hoverable(interaction)
            .selectable(selected, interaction, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        // The glyphs come in two sizes, so a fixed box keeps every label on the same line.
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides tone) { MoreIcon(item.tab) }
        }
        Text(item.label, color = tone, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
    }
}

/** The editor's bar holds only three items, so it stays short enough to cross with one thumb. */
private val EDITOR_NAV_WIDTH = 480.dp
