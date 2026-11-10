package app.lockon.ui

import app.lockon.ui.components.LineIcons
import app.lockon.ui.components.motion
import app.lockon.ui.components.glide
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lockon.AppContainer
import app.lockon.catalog.GcgApi
import app.lockon.data.CatalogException
import app.lockon.data.CatalogOrigin
import app.lockon.data.ThemeChoice
import app.lockon.ui.components.Accent
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.LockOnMark
import app.lockon.ui.components.CardBack
import app.lockon.ui.components.CardFan
import app.lockon.ui.components.CardImage
import app.lockon.ui.components.CardShape
import app.lockon.ui.components.DaySky
import app.lockon.ui.components.PillSegmented
import app.lockon.ui.components.TwilightSky
import app.lockon.ui.components.selectionEdge
import app.lockon.model.Card
import app.lockon.platform.isMobile
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import app.lockon.ui.components.ReticleSpinner
import app.lockon.ui.components.SlidingPages
import app.lockon.ui.components.WithContentColor
import app.lockon.ui.components.glassColors
import app.lockon.ui.components.glassFrame
import app.lockon.ui.components.isTwilight
import app.lockon.ui.theme.LockOnShapes
import kotlinx.coroutines.launch

/** One stop on the tour; [tab] is the screen shown behind the panel while it's up. */
private enum class TourStop(val tab: Tab?) { WELCOME(null), CARD_DATABASE(Tab.CARDS), DECKS(Tab.DECKS), CARDS(Tab.CARDS), SYNC(Tab.SYNC), SETTINGS(Tab.SETTINGS) }

/** Where the card database download stands. It outlives the page, so the tour can move on while it runs. */
private sealed interface Download {
    data object Idle : Download
    data object Running : Download
    data class Done(val cards: Int) : Download
    data class Failed(val message: String) : Download
}

/**
 * The first-run welcome: a dialog over the app that offers the official card database, then walks
 * through Decks, Cards, Sync and Settings, switching the screen behind it to match. Each stop is one
 * picture and one line. Skip ends it at any stop, the dots go back, and Settings can show it again.
 * [onDone] runs however it ends.
 */
@Composable
fun WelcomeTour(container: AppContainer, state: AppState, trailing: (@Composable () -> Unit)?, onDone: () -> Unit) {
    val stop = state.tourStop
    val current = TourStop.entries[stop]
    val last = stop == TourStop.entries.lastIndex
    var download by remember { mutableStateOf<Download>(Download.Idle) }
    val info by container.catalog.info.collectAsState()
    val official = info?.origin == CatalogOrigin.GCG_API
    val catalog by container.catalog.cards.collectAsState()
    // A card of each colour where there are enough, so the pictures show the range.
    val showcase = remember(catalog) {
        val playable = catalog.values.filter { !it.type.isResource && !it.type.isToken }.sortedBy { it.cardNumber }
        (playable.distinctBy { it.colors.firstOrNull() } + playable).distinct().take(6)
    }
    LaunchedEffect(current) { current.tab?.let { state.select(it) } }

    fun startDownload() {
        download = Download.Running
        // On the app's scope, so the download finishes even if the tour is skipped meanwhile.
        state.appScope.launch {
            download = try {
                val preview = container.catalogUpdater.latestCardDatabase()
                // Nothing a deck uses may vanish without the person seeing it first (UX rule C2).
                if (preview.missingFromDecks.isNotEmpty()) {
                    Download.Failed("Some of your decks use cards this list doesn't have. Review it in Settings → Card database.")
                } else {
                    container.catalogUpdater.install(preview)
                    Download.Done(preview.checked.file.cards.size)
                }
            } catch (e: CatalogException) {
                Download.Failed(e.message ?: "Couldn't download the card database.")
            } catch (e: Exception) {
                Download.Failed("Something went wrong: ${e.message}")
            }
        }
    }

    Box(
        Modifier.fillMaxSize()
            .background(if (isTwilight()) Color(0xFF060B1E).copy(alpha = 0.45f) else Color(0xFF3E5F8A).copy(alpha = 0.3f))
            // The screen behind is a preview: nothing there takes a press until the tour ends.
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() } },
        contentAlignment = Alignment.Center,
    ) {
        // Outside the app's Scaffold, so text would default to black; twilight needs it light.
        WithContentColor(MaterialTheme.colorScheme.onSurface) { Column(
            Modifier.padding(16.dp).widthIn(max = 560.dp).fillMaxWidth()
                .shadow(24.dp, LockOnShapes.panel, ambientColor = glassColors().shade, spotColor = glassColors().shade)
                .clip(LockOnShapes.panel)
                .background(glassColors().strong)
                .glassFrame(LockOnShapes.panel),
        ) {
            // Weighted so a short phone screen scrolls the page rather than pushing the buttons off.
            SlidingPages(stop, Modifier.weight(1f, fill = false).fillMaxWidth().animateContentSize(motion())) { page ->
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    when (TourStop.entries[page]) {
                        TourStop.WELCOME -> {
                            Picture { BrandTile(96.dp) }
                            Words("Welcome to Lock On", "A deck builder for the Gundam Card Game.")
                        }
                        TourStop.CARD_DATABASE -> {
                            Picture { Fan(showcase.drop(3)) }
                            Words("Get every card", "Download the official list, with images.")
                            CardDatabase(download, official, cardCount = catalog.size, ::startDownload)
                        }
                        TourStop.DECKS -> {
                            Picture { Fan(showcase) }
                            Words("Build a deck", "${if (isMobile) "Tap" else "Click"} + on a card to add a copy. The gauge turns gold at 50.")
                        }
                        TourStop.CARDS -> {
                            Picture { CardRow(showcase.drop(1)) }
                            Words("Find any card", "Search by name or text, and filter by color, type, level and cost.")
                        }
                        TourStop.SYNC -> {
                            Picture { TwoDevices(showcase.firstOrNull()) }
                            Words("Sync your devices", "Pair a phone and a computer once, over your own Wi-Fi. No account, no cloud.")
                        }
                        TourStop.SETTINGS -> {
                            Picture { ThemeTiles() }
                            Words("Make it yours", "Pick a theme. Interface size, animations and this tour are in Settings.")
                            ThemeChoices(container)
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 24.dp, top = 12.dp, bottom = 24.dp), contentAlignment = Alignment.Center) {
                app.lockon.ui.components.Reveal(!last, Modifier.align(Alignment.CenterStart), fadeOnly = true) {
                    GameButton("Skip", onClick = onDone, style = GameButtonStyle.GHOST, compact = true)
                }
                StepDots(stop, TourStop.entries.size) { state.tourStop = it }
                val notNow = current == TourStop.CARD_DATABASE && download == Download.Idle && !official
                GameButton(
                    when {
                        last -> "Start building"
                        notNow -> "Not now"
                        else -> "Next"
                    },
                    onClick = { if (last) onDone() else state.tourStop++ }, compact = true,
                    // The download is this stop's main action, so moving past it stays quiet.
                    style = if (notNow) GameButtonStyle.SECONDARY else GameButtonStyle.PRIMARY,
                    trailingIcon = if (last || notNow) null else LineIcons.Chev,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        } }
        trailing?.let { Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) { it() } }
    }
}

/** Where the tour is: a pill for the current stop, dots for the rest. A dot goes straight to its stop. */
@Composable
private fun StepDots(current: Int, count: Int, onSelect: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            // The pill slides along as the tour moves, rather than jumping from dot to dot.
            val width by animateDpAsState(if (i == current) 24.dp else 8.dp, glide(), label = "step-width")
            val fill by animateColorAsState(
                if (i == current) Accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f), motion(), label = "step-fill",
            )
            Box(
                Modifier.clip(CircleShape).clickable(onClickLabel = "Go to step ${i + 1}") { onSelect(i) }.padding(horizontal = 4.dp, vertical = 12.dp),
            ) { Box(Modifier.size(width = width, height = 8.dp).clip(CircleShape).background(fill)) }
        }
    }
}

/** The stop's picture, in a band that washes down from teal into the dialog. */
@Composable
private fun Picture(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(216.dp).clipToBounds()
            .background(Brush.verticalGradient(listOf(Accent.copy(alpha = 0.16f), Color.Transparent))),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Words(title: String, line: String) {
    Text(title, Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(
        line, Modifier.padding(horizontal = 32.dp).widthIn(max = 460.dp), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
    )
}

@Composable
private fun Fan(cards: List<Card>) {
    CardFan(cards.take(3).let { three -> listOfNotNull(three.getOrNull(1), three.getOrNull(0), three.getOrNull(2)) }.map { it to null }, cardWidth = 96.dp, middleOnTop = false)
}

/** Three cards laid out like the card list, with the selection outline on the middle one. */
@Composable
private fun CardRow(cards: List<Card>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val card = cards.getOrNull(i)
            val place = Modifier.width(96.dp).then(if (i == 1) Modifier.selectionEdge(CardShape, MaterialTheme.colorScheme.primary) else Modifier)
            if (card != null) CardImage(card, place) else CardBack(place)
        }
    }
}

/** A computer and a phone showing the same card, with dots running between them. */
@Composable
private fun TwoDevices(card: Card?) {
    val body = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    @Composable
    fun Screen(modifier: Modifier, shape: RoundedCornerShape, cardWidth: androidx.compose.ui.unit.Dp) {
        Box(modifier.clip(shape).background(body).padding(6.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(5.dp)).background(glassColors().strong), contentAlignment = Alignment.Center) {
                if (card != null) CardImage(card, Modifier.width(cardWidth), shadow = false) else CardBack(Modifier.width(cardWidth))
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Screen(Modifier.size(width = 168.dp, height = 112.dp), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp), 58.dp)
            Box(Modifier.size(width = 196.dp, height = 8.dp).clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)).background(body))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { Box(Modifier.size(8.dp).clip(CircleShape).background(Accent)) }
        }
        Screen(Modifier.size(width = 72.dp, height = 132.dp), RoundedCornerShape(14.dp), 44.dp)
    }
}

/** The mark on each theme's sky. */
@Composable
private fun ThemeTiles() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(DaySky to Color(0xFF0E3A5E), TwilightSky to Color.White).forEach { (sky, ink) ->
            Box(
                Modifier.size(width = 148.dp, height = 112.dp).clip(LockOnShapes.tile).background(Brush.verticalGradient(sky)),
                contentAlignment = Alignment.Center,
            ) { LockOnMark(size = 56.dp, reticleColor = ink) }
        }
    }
}

@Composable
private fun CardDatabase(download: Download, official: Boolean, cardCount: Int, onDownload: () -> Unit) {
    Spacer(Modifier.height(16.dp))
    app.lockon.ui.components.FadeSwap(download, contentAlignment = Alignment.Center, contentKey = { it::class to official }) { download ->
    Row(Modifier.heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            download is Download.Done || (official && download == Download.Idle) -> {
                Icon(LineIcons.Check, contentDescription = null, tint = Accent)
                Text("${(download as? Download.Done)?.cards ?: cardCount} cards installed", style = MaterialTheme.typography.titleSmall)
            }
            download == Download.Running -> {
                ReticleSpinner(size = 22.dp)
                Text("Downloading…", style = MaterialTheme.typography.titleSmall)
            }
            else -> GameButton(if (download is Download.Failed) "Try again" else "Download card database", onClick = onDownload)
        }
    }
    }
    app.lockon.ui.components.RevealValue(download as? Download.Failed) {
        Text(it.message, Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(12.dp))
    Text(
        GcgApi.ATTRIBUTION, Modifier.padding(horizontal = 32.dp), style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center,
    )
}

/** The last stop picks a theme on the spot, since that's what most people change first. */
@Composable
private fun ThemeChoices(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val theme by container.settings.theme.collectAsState()
    Spacer(Modifier.height(16.dp))
    PillSegmented(
        ThemeChoice.entries.map { it.label }, ThemeChoice.entries.indexOf(theme),
        { i -> scope.launch { container.settings.setTheme(ThemeChoice.entries[i]) } },
        Modifier.padding(horizontal = 32.dp).widthIn(max = 340.dp).fillMaxWidth(),
    )
}
