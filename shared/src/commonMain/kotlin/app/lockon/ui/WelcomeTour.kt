package app.lockon.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import app.lockon.ui.components.GameIcons
import app.lockon.ui.components.LockOnMark
import app.lockon.ui.components.PillChip
import app.lockon.ui.components.ReticleSpinner
import app.lockon.ui.components.SlidingPages
import app.lockon.ui.components.WithContentColor
import app.lockon.ui.components.glass
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
 * The first-run welcome: a glass panel over the app that offers the official card database, then
 * walks through Decks, Cards, Sync and Settings, switching the screen behind it to match. Skip ends
 * it at any stop, and Settings can show it again. [onDone] runs however it ends.
 */
@Composable
fun WelcomeTour(container: AppContainer, state: AppState, trailing: (@Composable () -> Unit)?, onDone: () -> Unit) {
    val stop = state.tourStop
    val current = TourStop.entries[stop]
    val last = stop == TourStop.entries.lastIndex
    var download by remember { mutableStateOf<Download>(Download.Idle) }
    val info by container.catalog.info.collectAsState()
    val official = info?.origin == CatalogOrigin.GCG_API
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
            .background(if (isTwilight()) Color(0xFF060B1E).copy(alpha = 0.55f) else Color(0xFF3E5F8A).copy(alpha = 0.45f))
            // The screen behind is a preview: nothing there takes a press until the tour ends.
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() } },
        contentAlignment = Alignment.Center,
    ) {
        // Outside the app's Scaffold, so text would default to black; twilight needs it light.
        WithContentColor(MaterialTheme.colorScheme.onSurface) { Column(
            Modifier.padding(16.dp).widthIn(max = 520.dp).fillMaxWidth()
                .shadow(24.dp, LockOnShapes.panel, ambientColor = glassColors().shade, spotColor = glassColors().shade)
                // Solid like a dialog, since the busy screen behind would show through glass.
                .clip(LockOnShapes.panel)
                .background(if (isTwilight()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest)
                .glassFrame(LockOnShapes.panel)
                .padding(24.dp),
        ) {
            StepDots(stop, TourStop.entries.size)
            Spacer(Modifier.height(16.dp))
            // Weighted so a short phone screen scrolls the page rather than pushing the buttons off.
            SlidingPages(stop, Modifier.weight(1f, fill = false).fillMaxWidth().animateContentSize()) { page ->
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    when (TourStop.entries[page]) {
                        TourStop.WELCOME -> Welcome()
                        TourStop.CARD_DATABASE -> CardDatabase(download, official, cardCount = container.catalog.cards.value.size, ::startDownload)
                        TourStop.DECKS -> Feature(
                            GameIcons.Deck, "Decks", "Your decks live here. Create one, then tap + for each copy you want.",
                            "Each deck has Analysis, Test, Matches and Versions tabs: legality and curves, practice opening hands, a win–loss log and earlier versions to restore.",
                            "Folders and search keep a big collection tidy. Export a text list, a deck image or a registration sheet.",
                        )
                        TourStop.CARDS -> Feature(
                            Icons.Default.Search, "Cards", "Search every card by name or text, and filter by color, type, level and cost.",
                            "Tap a card for its details and every printing. On a computer, Ctrl+F jumps here from anywhere.",
                            "Max and Off-color tags show the deck rules while you build.",
                        )
                        TourStop.SYNC -> Feature(
                            Icons.Default.Refresh, "Sync", "Keep your phone and computer in step over your own Wi-Fi. There's no account and no cloud.",
                            "Pair two devices once; after that, edits reach the other one within seconds.",
                            "You can also send a single deck to a friend's device nearby.",
                        )
                        TourStop.SETTINGS -> SettingsStop(container)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!last) GameButton("Skip tour", onClick = onDone, style = GameButtonStyle.GHOST, compact = true)
                Spacer(Modifier.weight(1f))
                if (stop > 0) GameButton("Back", onClick = { state.tourStop-- }, style = GameButtonStyle.SECONDARY, compact = true)
                val next = when {
                    last -> "Start building"
                    current == TourStop.WELCOME -> "Show me around"
                    current == TourStop.CARD_DATABASE && download == Download.Idle && !official -> "Not now"
                    else -> "Next"
                }
                GameButton(next, onClick = { if (last) onDone() else state.tourStop++ }, compact = true)
            }
        } }
        trailing?.let { Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) { it() } }
    }
}

/** Where the tour is: a pill for the current stop, dots for the rest, as on the boot screen. */
@Composable
private fun StepDots(current: Int, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            Box(
                Modifier.size(width = if (i == current) 18.dp else 8.dp, height = 8.dp).clip(CircleShape)
                    .background(if (i <= current) Accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
            )
        }
    }
}

@Composable
private fun Welcome() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(96.dp).shadow(12.dp, CircleShape, ambientColor = glassColors().shade, spotColor = glassColors().shade)
                .clip(CircleShape).glass(CircleShape, strong = true),
            contentAlignment = Alignment.Center,
        ) { LockOnMark(size = 64.dp, reticleColor = MaterialTheme.colorScheme.onSurface) }
        Spacer(Modifier.height(16.dp))
        Text("Welcome to Lock On", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "A deck builder for the Gundam Card Game. First the card list, then a quick look around. It takes about a minute, and you can skip it at any point.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CardDatabase(download: Download, official: Boolean, cardCount: Int, onDownload: () -> Unit) {
    StopTitle(Icons.Default.List, "Get the full card list")
    Text(
        "Lock On comes with a few sample cards. Download the official list for every card and printing, with images. " +
            "It's a few megabytes, and a weekly check tells you when a newer one is out.",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            download is Download.Done || (official && download == Download.Idle) -> {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Accent)
                Text(
                    "Installed: ${(download as? Download.Done)?.cards ?: cardCount} cards",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            download == Download.Running -> {
                ReticleSpinner(size = 22.dp)
                Text("Downloading… you can carry on meanwhile.", style = MaterialTheme.typography.titleSmall)
            }
            else -> GameButton(
                if (download is Download.Failed) "Try again" else "Download card database",
                onClick = onDownload, large = true,
            )
        }
    }
    (download as? Download.Failed)?.let {
        Spacer(Modifier.height(8.dp))
        Text(it.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(12.dp))
    Text(GcgApi.ATTRIBUTION, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
}

/** A stop about one screen: what it's for, then two things worth knowing. */
@Composable
private fun Feature(icon: ImageVector, title: String, lead: String, vararg points: String) {
    StopTitle(icon, title)
    Text(lead, style = MaterialTheme.typography.bodyLarge)
    points.forEach { point ->
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.padding(top = 7.dp).size(6.dp).clip(CircleShape).background(Accent))
            Text(point, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The last stop picks a theme on the spot, since that's what most people change first. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsStop(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val theme by container.settings.theme.collectAsState()
    Feature(
        Icons.Default.Settings, "Settings", "Theme, interface size, animations and where Lock On opens are all here.",
        "So are the card database, app updates, and this tour if you want it again.",
    )
    Spacer(Modifier.height(14.dp))
    Text("Theme", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ThemeChoice.entries.forEach { choice ->
            PillChip(selected = choice == theme, onClick = { scope.launch { container.settings.setTheme(choice) } }, label = { Text(choice.label) })
        }
    }
}

@Composable
private fun StopTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
    Spacer(Modifier.height(12.dp))
}
