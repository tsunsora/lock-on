package app.lockon.ui.decks
import app.lockon.ui.components.SlidingPages
import app.lockon.ui.components.LocalTopBand
import app.lockon.ui.components.Accent
import androidx.compose.material.icons.filled.KeyboardArrowUp
import app.lockon.ui.components.GlassIconButton
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.lockon.rules.DeckAnalysis
import app.lockon.ui.components.FloatingPillNav
import app.lockon.ui.components.LocalNavClearance
import app.lockon.ui.components.navClearance
import androidx.compose.foundation.layout.BoxScope
import app.lockon.ui.components.BottomItem
import app.lockon.ui.components.GameIcons
import app.lockon.ui.components.ProgressGauge
import androidx.compose.material.icons.filled.MoreVert
import app.lockon.ui.components.GameMenu
import app.lockon.ui.components.GameMenuItem
import androidx.compose.foundation.layout.size
import app.lockon.ui.components.DeckCard
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import app.lockon.ui.components.glassColors
import app.lockon.ui.components.PillChip
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
import app.lockon.model.DeckVersion
import app.lockon.model.MatchRecord
import app.lockon.platform.formatTimestamp
import app.lockon.data.DeckRepository
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
import app.lockon.rules.DeckLegality
import app.lockon.ui.components.glass
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import app.lockon.data.db.MetaEntity
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.withFrameNanos
import app.lockon.platform.resizeHorizontalPointer
import app.lockon.ui.components.VerticalScrollBar
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.rememberCoroutineScope
import app.lockon.ui.components.fadeEnd

private const val AUTOSAVE_DELAY_MS = 400L

/** The card list's share of the wide editor's width, until the person drags the divider. */
private const val DEFAULT_SPLIT = 0.615f
private const val KEY_EDITOR_SPLIT = "ui.deckEditor.split"
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
    val summary by produceState(cached, deckId) { container.decks.liveDeck(deckId).collect { value = it; loaded = true } }
    val cards by container.catalog.cards.collectAsState()
    val printings by container.catalog.printings.collectAsState()
    val ruleset by container.catalog.ruleset.collectAsState()
    val allDecks by container.decks.liveDecks.collectAsState(initial = emptyList())
    val devices by container.devices.pairedDevices.collectAsState(initial = emptyList())
    var history by remember(deckId) { mutableStateOf<List<DeckVersion>>(emptyList()) }

    // The editor works on a local draft and autosaves it. Changes that arrive by sync replace the
    // draft only when nothing local is waiting to be saved.
    var draft by remember(deckId) { mutableStateOf(cached?.snapshot) }
    var saved by remember(deckId) { mutableStateOf(cached?.snapshot) }
    // The revision the draft builds on, so an edit that syncs in meanwhile becomes a conflict, not lost.
    var base by remember(deckId) { mutableStateOf(cached?.headRevisionId) }
    LaunchedEffect(summary) {
        val incoming = summary ?: return@LaunchedEffect
        if (draft == null || draft == saved) { draft = incoming.snapshot; saved = incoming.snapshot; base = incoming.headRevisionId }
    }
    val pending by rememberUpdatedState(draft?.takeIf { it != saved })
    val pendingBase by rememberUpdatedState(base)
    LaunchedEffect(draft) {
        val toSave = draft ?: return@LaunchedEffect
        if (toSave == saved) return@LaunchedEffect
        delay(AUTOSAVE_DELAY_MS)
        val from = base
        state.appScope.launch { container.decks.save(deckId, toSave, from) }
        saved = toSave
    }
    DisposableEffect(deckId) {
        onDispose { pending?.let { last -> state.appScope.launch { container.decks.save(deckId, last, pendingBase) } } }
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
    if (summary == null && deck == null) {
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

    // Where the card list's + and dropped cards go: the deck, or the side deck beside it.
    val addToSide = rememberSaveable(deckId) { mutableStateOf(false) }
    fun addTarget(card: GameCard) = if (addToSide.value) DeckSection.SIDE else DeckSection.playedIn(card)

    // Main-deck colors, kept as one object while they stay the same so the card list can skip
    // recomposing when a change doesn't touch them.
    val colorsNow = deck.entries.filter { it.section == DeckSection.MAIN }.flatMap { cards[it.cardNumber]?.colors.orEmpty() }.toSet()
    val deckColors = remember(colorsNow) { colorsNow }

    Column(Modifier.fillMaxSize()) {
        // Header: a hero banner with the cover art behind it. Who the deck is sits on the left, its
        // health is grouped on the right (UX rule I2).
        val cover = remember(deck, cards) { deckCover(deck, cards) }
        DeckBanner(cover, wide, legality.isLegal) {
            // On wide windows the whole header sits in the band between the floating navigation
            // plates, so the editor spends no extra row on it (UX rule N5).
            BoxWithConstraints(Modifier.fillMaxWidth().clearOfNav()) {
            val roomy = maxWidth >= 1000.dp
            Row(
                Modifier.fillMaxWidth().padding(start = if (wide) 0.dp else 12.dp, end = if (wide) 8.dp else 12.dp, top = if (wide) 0.dp else 8.dp, bottom = if (wide) 0.dp else 4.dp)
                    .then(if (wide) Modifier.align(Alignment.CenterStart) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // A solid round button, so the way out reads as a control rather than a stray glyph.
                GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back to decks", state::goBack)
                Spacer(Modifier.width(14.dp))
                if (wide && roomy) {
                    // A small cover that sits inside the band with air around it, not edge to edge.
                    val thumb = Modifier.padding(end = 14.dp).padding(vertical = 6.dp).height(48.dp)
                    if (cover != null) CardImage(cover.card, thumb, cover.printingId)
                    else CardBack(thumb)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // The name itself opens Rename; wide windows add a quiet pencil beside it as the hint.
                    Row(
                        Modifier.clip(MaterialTheme.shapes.small).clickable(onClickLabel = "Rename deck") { renaming = true }
                            .padding(end = if (wide && roomy) 6.dp else 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(deck.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        // A tight band gives the pencil's room to the name, which still opens Rename.
                        if (wide && roomy) Icon(Icons.Default.Edit, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val c = MaterialTheme.colorScheme
                        ColorPips(deckColors, size = 16.dp)
                        // A tight band keeps the state and drops the detail; Analysis has the rest. Phones
                        // drop the ruleset name too, so the stamp never gets cut off.
                        val tight = wide && !roomy
                        if (legality.isLegal) Badge(if (!roomy) "Legal" else "Legal · ${ruleset.name}", c.secondaryContainer, c.onSecondaryContainer)
                        else Badge(if (tight) "Draft" else "Draft · ${legality.errors.size} issues", c.surfaceVariant, c.onSurfaceVariant)
                    }
                }
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(if (roomy) 20.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        DeckGauge(if (roomy) "Main deck" else "Main", deck.count(DeckSection.MAIN), ruleset.mainDeckSize, Modifier.width(if (roomy) 200.dp else 104.dp))
                        // The Legal or Draft stamp says the deck's state in words (I4).
                        if (roomy) {
                            GameButton("Export", onClick = { exporting = true }, style = GameButtonStyle.SECONDARY, icon = Icons.Default.Share, compact = true)
                        } else {
                            GlassIconButton(Icons.Default.Share, "Export deck", { exporting = true })
                        }
                    }
                } else {
                    // Phones keep only Export here; the legality stamp says Legal or Draft in words (I4).
                    GlassIconButton(Icons.Default.Share, "Export deck", { exporting = true })
                }
            }
            }
            if (!wide) {
                ProgressGauge("Main", deck.count(DeckSection.MAIN), ruleset.mainDeckSize,
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp), compact = true)
            }
        }
        if (summary?.hasConflict == true) {
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

        // The wide deck panel's tab (Deck, Analysis, Test, Matches, Versions); phones use their own tab row.
        var deckTab by rememberSaveable(deckId) { mutableStateOf(0) }
        var phoneTab by rememberSaveable(deckId) { mutableStateOf(startTab) }
        // Kept for every deck, so the editor opens the way the person last arranged it.
        val split = remember { mutableFloatStateOf(DEFAULT_SPLIT) }
        LaunchedEffect(Unit) { container.database.metaDao().get(KEY_EDITOR_SPLIT)?.toFloatOrNull()?.let { split.floatValue = it } }
        val deckList: @Composable (Modifier) -> Unit = { modifier ->
            DeckList(
                deck, cards, ruleset, legality, modifier, compact = wide,
                onOpenAnalysis = { if (wide) deckTab = 1 else phoneTab = 2 },
                onExport = { exporting = true },
                onAddCards = if (wide) null else ({ phoneTab = 1 }),
                onCardClick = { detail = it },
                onChange = { card, delta -> change(card, delta) },
                onSideChange = { card, delta -> change(card, delta, DeckSection.SIDE) },
                onMoveToDeck = { move(it, toSide = false) },
                onNotes = { notes -> draft = draft?.copy(notes = notes) },
            )
        }
        // Resource cards stay out of the card list: the editor doesn't track the resource deck.
        val addable = remember(cards) { cards.values.filter { !it.type.isResource } }
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
                fun AddTo(tabs: Modifier, label: Modifier = Modifier) {
                    Text("Add to", label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PillTabs(listOf("Deck", "Side deck"), if (addToSide.value) 1 else 0, { addToSide.value = it == 1 }, tabs)
                }
                // A wide card list fits the switch at the end of the search row, so the cards get
                // that row's height back (UX rule N4, space goes to cards).
                val inline = wide && maxWidth >= 720.dp
                Column {
                    if (!inline) Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        AddTo(Modifier.weight(1f))
                    }
                    CardSearchPanel(
                        addable, onCardClick = { detail = it }, modifier = Modifier.weight(1f),
                        focusRequests = focusSeen[0], deckColors = deckColors, wide = wide,
                        controls = controls,
                        trailing = if (inline) ({ AddTo(Modifier.width(IntrinsicSize.Max), Modifier.padding(start = 14.dp)) }) else null,
                    )
                }
            }
        }
        val analysis: @Composable (Modifier) -> Unit = { modifier -> DeckAnalysisPanel(deck, cards, legality, modifier) }
        val test: @Composable (Modifier) -> Unit = { modifier -> SampleHandPanel(deck, cards, modifier) }
        // Opponents named on any deck, most played first.
        val opponents = remember(allDecks) {
            allDecks.flatMap { d -> d.snapshot.matches.map { it.opponent.trim() } }.filter { it.isNotEmpty() }
                .groupBy { it.lowercase() }.values.sortedByDescending { it.size }.map { it.last() }
        }
        val matchLog: @Composable (Modifier) -> Unit = { modifier ->
            MatchLogPanel(
                deck.matches, opponents, modifier,
                onLog = { result, opponent, wentFirst, notes ->
                    val match = MatchRecord(Uuid.random().toString(), Clock.System.now().toEpochMilliseconds(), result, opponent, wentFirst, notes)
                    draft = deck.copy(matches = deck.matches + match)
                },
                onDelete = { match ->
                    draft = deck.copy(matches = deck.matches - match)
                    state.notify("Removed a ${match.result.label.lowercase()}", "Undo") {
                        draft = draft?.let { it.copy(matches = DeckRepository.mergeMatches(it.matches + match)) }
                    }
                },
            )
        }
        val versions: @Composable (Modifier) -> Unit = { modifier ->
            // Loaded only while the panel shows: reading the history decodes every version.
            LaunchedEffect(summary) { history = container.decks.history(deckId) }
            VersionsPanel(
                history, deck, cards, { deviceLabel(it, container.identity.deviceId, devices) }, modifier,
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
            BoxWithConstraints(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 10.dp)) {
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
            val pointerX = remember { mutableFloatStateOf(0f) }
            fun saveSplit() = state.appScope.launch { container.database.metaDao().put(MetaEntity(KEY_EDITOR_SPLIT, split.floatValue.toString())) }
            Row(Modifier.fillMaxSize().pointerHoverIcon(if (resizing) resizeHorizontalPointer else PointerIcon.Default, overrideDescendants = resizing)) {
                Panel(
                    Modifier.fillMaxHeight().layout { measurable, constraints ->
                        val width = (usable * clampSplit(split.floatValue)).roundToPx()
                        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                        layout(width, placeable.height) { placeable.place(0, 0) }
                    },
                ) { if (stage >= 2) search(Modifier.fillMaxSize()) }
                SplitHandle(
                    onDragStart = { resizing = true; pointerX.floatValue = with(density) { (usable * clampSplit(split.floatValue)).toPx() } },
                    onDrag = { dx ->
                        pointerX.floatValue += dx
                        val usablePx = with(density) { usable.toPx() }
                        if (usablePx > 0f) split.floatValue = clampSplit(pointerX.floatValue / usablePx)
                    },
                    onDragEnd = { resizing = false; saveSplit() },
                    onReset = { split.floatValue = DEFAULT_SPLIT; saveSplit() },
                )
                // The deck panel takes dropped cards, and its rim lights up while one is held over it.
                val dropRim = when {
                    cardDrag.overTarget -> MaterialTheme.colorScheme.primary
                    cardDrag.card != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else -> Color.Transparent
                }
                Panel(Modifier.weight(1f).fillMaxSize().cardDropTarget(cardDrag)
                    .border(BorderStroke(2.dp, dropRim), LockOnShapes.panel)) {
                    Column {
                        PillTabs(listOf("Deck", "Analysis", "Test", "Matches", "Versions"), deckTab, { deckTab = it }, fill = true)
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
                if (stage >= 1) SlidingPages(phoneTab, Modifier.fillMaxSize()) { page ->
                    when (page) {
                        0 -> deckList(Modifier.fillMaxSize())
                        1 -> search(Modifier.fillMaxSize())
                        2 -> analysis(Modifier.fillMaxSize())
                        3 -> test(Modifier.fillMaxSize())
                        4 -> matchLog(Modifier.fillMaxSize())
                        else -> versions(Modifier.fillMaxSize())
                    }
                }
                EditorNav(phoneTab) { phoneTab = it }
            }
        }
    }

    detail?.let { card ->
        val section = DeckSection.playedIn(card)
        val side = deck.quantityOf(card.cardNumber, DeckSection.SIDE)
        val cardPrintings = printings[card.cardNumber].orEmpty()
        val inDeck = deck.quantityOf(card.cardNumber, section)
        CardDetailDialog(card, cardPrintings, onDismiss = { detail = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailActionRow(section.label) {
                    PlateStepper(inDeck, "copy", onMinus = { change(card, -1) }, onPlus = { change(card, 1) }, limit = copyLimit(card, ruleset))
                }
                // The side deck sits beside the deck; moving keeps the total and never checks rules (UX rule S4).
                DetailActionRow("Side deck") {
                    PlateStepper(side, "side deck copy", onMinus = { change(card, -1, DeckSection.SIDE) }, onPlus = { change(card, 1, DeckSection.SIDE) })
                }
                if (cardPrintings.size > 1 && inDeck > 0) {
                    val preferred = deck.entries.firstOrNull { it.cardNumber == card.cardNumber && it.section == section }?.printingId
                    DetailActionRow("Printing used") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            cardPrintings.forEach { p ->
                                PillChip(
                                    selected = (preferred ?: cardPrintings.first().printingId) == p.printingId,
                                    onClick = { draft = deck.withQuantity(card.cardNumber, section, inDeck, p.printingId) },
                                    label = { Text(p.label.removePrefix("Reprint: ")) },
                                )
                            }
                        }
                    }
                }
                // The library shows the deck by this card instead of its highest-level Unit.
                val canCover = inDeck > 0 || deck.coverCard == card.cardNumber
                if (side > 0 || inDeck > 0 || canCover) {
                    FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        when {
                            side > 0 -> GameButton("Move 1 to deck", onClick = { move(card, toSide = false) }, style = GameButtonStyle.GHOST, compact = true)
                            inDeck > 0 -> GameButton("Move 1 to side deck", onClick = { move(card, toSide = true) }, style = GameButtonStyle.GHOST, compact = true)
                        }
                        if (canCover) {
                            val isCover = deck.coverCard == card.cardNumber
                            GameButton(if (isCover) "Stop using as deck cover" else "Use as deck cover", onClick = { draft = deck.copy(coverCard = if (isCover) null else card.cardNumber) }, style = GameButtonStyle.GHOST, compact = true)
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
        DeckExportDialog(deck, cards, ruleset, files, state, onDismiss = { exporting = false })
    }
}

@Composable
private fun DeckList(
    deck: DeckSnapshot,
    cards: Map<String, GameCard>,
    ruleset: Ruleset,
    legality: DeckLegality,
    modifier: Modifier,
    /** Slimmer strips for wide windows. */
    compact: Boolean,
    onOpenAnalysis: () -> Unit,
    onExport: () -> Unit,
    /** Opens the card list; phones only, where it is a separate tab. */
    onAddCards: (() -> Unit)?,
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
    var cardView by rememberSaveable { mutableStateOf(true) }
    val gap = if (compact) 8.dp else 6.dp
    val listState = rememberLazyListState()
    val side = remember(deck, cards) {
        deck.entries.filter { it.section == DeckSection.SIDE }.sortedWith(compareBy({ cards[it.cardNumber]?.level ?: 99 }, { it.cardNumber }))
    }
    BoxWithConstraints(modifier) {
    // The end padding clears the scroll bar's lane so section counts don't sit under it.
    // Phones keep three across down to about 390 dp, like the card list, instead of two large cards.
    val columns = ((maxWidth - 28.dp + gap) / ((if (compact) 104.dp else 112.dp) + gap)).toInt().coerceAtLeast(2)
    Column(Modifier.fillMaxSize()) {
    // Phones keep Export, the deck's first problem and the view switch above the list.
    if (!compact && deck.entries.isNotEmpty()) DeckTopBar(legality, onOpenAnalysis, onExport, cardView, { cardView = it })
    Box(Modifier.weight(1f).fillMaxWidth()) {
    LazyColumn(Modifier.fillMaxSize(), listState, contentPadding = PaddingValues(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp + LocalNavClearance.current),
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (compact && deck.entries.isNotEmpty()) {
            // Export and whether the deck is legal ride at the top of the list.
            item(key = "stats") {
                DeckStatusBar(legality, onExport, cardView, { cardView = it }, Modifier.padding(bottom = 4.dp))
            }
        }
        if (deck.entries.isEmpty()) {
            item(key = "empty-deck") {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LockIndicator(locked = false, size = 72.dp)
                    Text("No cards yet", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (compact) "Drag a card here or click + to add a copy. A deck needs 50 cards in up to two colors."
                        else "Tap + on any card to add a copy. A deck needs 50 cards in up to two colors.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (onAddCards != null) GameButton("Add cards", onClick = onAddCards, icon = Icons.Default.Add, modifier = Modifier.padding(top = 6.dp))
                }
            }
        } else if (deck.count(DeckSection.MAIN) == 0) {
            item(key = "empty-main") {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
            item(key = "header-$label") {
                SectionHeader(label, Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 2.dp),
                    readout = "${entries.sumOf { it.quantity }}")
            }
            if (cardView) {
                // Whole cards, small, a row at a time so the list stays lazy (UX rule V4a).
                items(entries.chunked(columns), key = { row -> "row-${row.first().section}-${row.first().cardNumber}" }) { row ->
                    Row(Modifier.animateItem().fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { entry ->
                            val card = cards[entry.cardNumber]
                            if (card != null) {
                                DeckCard(
                                    card, entry.quantity, entry.printingId, onClick = { onCardClick(card) },
                                    onMinus = { onChange(card, -1) }, onPlus = { onChange(card, 1) },
                                    modifier = Modifier.weight(1f), limit = copyLimit(card, ruleset), target = if (compact) 40.dp else 48.dp,
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
            } else items(entries, key = { "${it.section}-${it.cardNumber}" }) { entry ->
                val card = cards[entry.cardNumber]
                DeckStrip(
                    card, entry.quantity, entry.printingId, onClick = { card?.let(onCardClick) }, modifier = Modifier.animateItem(),
                    subtitle = listOfNotNull(card?.level?.let { "Lv.$it" }, entry.cardNumber).joinToString("  ·  "), compact = compact,
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
        item(key = "side-header") {
            SectionHeader(
                "Side deck", Modifier.padding(start = 6.dp, end = 6.dp, top = 24.dp),
                readout = "${side.sumOf { it.quantity }}",
            )
            Text(
                if (side.isEmpty()) "Cards kept beside the deck. Set the card list to Add to: Side deck, then add cards with +${if (compact) " or by dragging" else ""}."
                else "Not counted toward the deck or its legality.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
        items(side, key = { "side-${it.cardNumber}" }) { entry ->
            val card = cards[entry.cardNumber]
            DeckStrip(
                card, entry.quantity, entry.printingId, onClick = { card?.let(onCardClick) }, modifier = Modifier.animateItem().alpha(0.82f),
                subtitle = listOfNotNull(card?.level?.let { "Lv.$it" }, entry.cardNumber).joinToString("  ·  "), compact = compact,
            ) {
                if (card != null) {
                    // One tap per copy (UX rule S1): move one into the deck, or change the side deck's count.
                    // On phones an arrow, so the card's name keeps its room; its label is still read out.
                    if (LocalTopBand.current.height == 0.dp) IconButton(onClick = { onMoveToDeck(card) }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move one ${card.name} to the deck", tint = Accent)
                    }
                    else GameButton("To deck", onClick = { onMoveToDeck(card) }, style = GameButtonStyle.GHOST, compact = true)
                    PlateStepper(entry.quantity, card.name, onMinus = { onSideChange(card, -1) }, onPlus = { onSideChange(card, 1) })
                } else {
                    Text("×${entry.quantity}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 12.dp))
                }
            }
        }
        item(key = "notes") {
            var editing by remember { mutableStateOf(false) }
            if (deck.notes.isBlank() && !editing) {
                GameButton("Add notes", onClick = { editing = true }, style = GameButtonStyle.GHOST, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), compact = true)
            } else {
                OutlinedTextField(
                    value = deck.notes, onValueChange = onNotes,
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth().padding(6.dp),
                    minLines = 3,
                )
            }
        }
    }
    VerticalScrollBar(listState, Modifier.align(Alignment.TopEnd))
    }
    }
    }
}

/**
 * The phone deck list's fixed top bar: Export and the card/list switch. Under it, the first legality
 * problem in words (UX rule I3), which opens Analysis.
 */
@Composable
private fun DeckTopBar(
    legality: DeckLegality,
    onOpenAnalysis: () -> Unit,
    onExport: () -> Unit,
    cardView: Boolean,
    onCardView: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 6.dp).glass(LockOnShapes.tile, strong = true).padding(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GameButton("Export", onClick = onExport, style = GameButtonStyle.SECONDARY, icon = Icons.Default.Share, compact = true)
            Spacer(Modifier.weight(1f))
            PillChip(selected = false, onClick = { onCardView(!cardView) }, label = { Text(if (cardView) "List" else "Cards") },
                modifier = Modifier.padding(start = 4.dp))
        }
        legality.errors.firstOrNull()?.let { first ->
            Text(
                if (legality.errors.size == 1) first.text else "${first.text} (+${legality.errors.size - 1} more)",
                style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().clip(LockOnShapes.button).clickable(onClickLabel = "Open analysis", onClick = onOpenAnalysis)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
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
    when {
        card.cardNumber in ruleset.banned -> Badge("Banned", c.errorContainer, c.onErrorContainer)
        addingToSide -> if (quantity > 0) Badge("Deck $quantity", c.secondaryContainer, c.onSecondaryContainer)
        card.colors.isNotEmpty() && (deckColors + card.colors).size > ruleset.maxColors ->
            Badge("Off-color", c.surfaceVariant, c.onSurfaceVariant)
        side > 0 -> Badge("Side $side", c.secondaryContainer, c.onSecondaryContainer)
    }
}

/** One labelled line in the card details' "In this deck" panel: the label left, its control right. */
@Composable
private fun DetailActionRow(label: String, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(end = 12.dp))
        control()
    }
}

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
    Box(Modifier.fillMaxWidth().clipToBounds().background(glassFill).shineSweep(locked)) {
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.62f).fillMaxHeight()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = artAlpha))), blendMode = BlendMode.DstIn)
                    },
            ) {
                CardArtwork(art, null, Modifier.fillMaxSize(), ContentScale.Crop, BiasAlignment(0f, -0.5f)) {
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(color.copy(alpha = 0f), color.copy(alpha = 0.5f)))))
                }
            }
        }
        Column { content() }
    }
}

/**
 * A slim bar at the top of the deck list: Export, then the first legality problem in words (UX rule I3), and the card/list view switch at the end. A legal
 * deck shows no message: the Legal stamp in the header already says so.
 */
@Composable
private fun DeckStatusBar(
    legality: DeckLegality,
    onExport: () -> Unit,
    cardView: Boolean,
    onCardView: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().glass(LockOnShapes.tile, strong = true).padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameButton("Export", onClick = onExport, style = GameButtonStyle.GHOST, icon = Icons.Default.Share, compact = true)
            val first = legality.errors.firstOrNull()
            if (first == null) Spacer(Modifier.weight(1f)) else Text(
                if (legality.errors.size == 1) first.text else "${first.text} (+${legality.errors.size - 1} more)",
                style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = scheme.error, modifier = Modifier.weight(1f),
            )
            // How the deck below is shown: small whole cards, or denser strips.
            GameButton(if (cardView) "List view" else "Card view", onClick = { onCardView(!cardView) }, style = GameButtonStyle.GHOST, compact = true)
        }
    }
}

/**
 * The divider between the card list and the deck: drag to resize, double-click to reset. A pill grip
 * sits in the gap, and lights up in the accent with a soft glow while hovered or dragged.
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
    val grip by animateColorAsState(if (active) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.3f), tween(140))
    val glow by animateFloatAsState(if (active) 1f else 0f, tween(140))
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
        Box(
            Modifier.width(4.dp).height(54.dp)
                .drawBehind {
                    if (glow > 0f) drawRoundRect(
                        Brush.radialGradient(listOf(scheme.primary.copy(alpha = 0.35f * glow), scheme.primary.copy(alpha = 0f)), radius = size.height * 0.75f),
                        topLeft = Offset(-size.height / 2, -size.height / 4), size = Size(size.height + size.width, size.height * 1.5f),
                    )
                }
                .clip(RoundedCornerShape(2.dp)).background(grip),
        )
    }
}

/**
 * The editor's sections on phones, at the bottom in thumb reach (UX rules N5, R3). Building the deck
 * comes first, so the bar holds only Deck and Add cards; Analysis, Test, Matches and Versions sit
 * under More, which takes the name and icon of whichever of them is open.
 */
@Composable
private fun BoxScope.EditorNav(tab: Int, onSelect: (Int) -> Unit) {
    var more by remember { mutableStateOf(false) }
    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
        FloatingPillNav(
            listOf(
                BottomItem("Deck") { Icon(GameIcons.Deck, contentDescription = null, modifier = Modifier.size(20.dp)) },
                BottomItem("Add cards") { Icon(Icons.Default.Add, contentDescription = null) },
                BottomItem(when (tab) { 2 -> "Analysis"; 3 -> "Test"; 4 -> "Matches"; 5 -> "Versions"; else -> "More" }) {
                    when (tab) {
                        2 -> Icon(GameIcons.Curve, contentDescription = null, modifier = Modifier.size(20.dp))
                        3 -> Icon(GameIcons.Hand, contentDescription = null, modifier = Modifier.size(20.dp))
                        else -> Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                },
            ),
            selected = minOf(tab, 2),
            onSelect = { if (it == 2) more = true else onSelect(it) },
        )
        // Anchored over the right end of the pill, where More sits, so the menu opens above it.
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = navClearance() - 12.dp).fillMaxWidth(0.3f)) {
            GameMenu(expanded = more, onDismissRequest = { more = false }) {
                GameMenuItem("Analysis", onClick = { more = false; onSelect(2) }, selected = tab == 2)
                GameMenuItem("Test hand", onClick = { more = false; onSelect(3) }, selected = tab == 3)
                GameMenuItem("Matches", onClick = { more = false; onSelect(4) }, selected = tab == 4)
                GameMenuItem("Versions", onClick = { more = false; onSelect(5) }, selected = tab == 5)
            }
        }
    }
}
