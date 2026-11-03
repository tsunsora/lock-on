package app.lockon.ui.settings

import app.lockon.ui.components.VerticalScrollBar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.lockon.AppContainer
import app.lockon.data.EditorBarOrder
import app.lockon.data.EditorMoreItem
import app.lockon.data.InterfaceSize
import app.lockon.data.MotionChoice
import app.lockon.data.StartChoice
import app.lockon.data.ThemeChoice
import app.lockon.io.BackupFormat
import app.lockon.platform.FileBridge
import app.lockon.platform.isMobile
import app.lockon.ui.AppState
import app.lockon.ui.components.Accent
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.GameDialog
import app.lockon.ui.components.GameMenuItem
import app.lockon.ui.components.GlassCard
import app.lockon.ui.components.LocalNavClearance
import app.lockon.ui.components.MenuChip
import app.lockon.ui.components.PillChip
import app.lockon.ui.components.PillSegmented
import app.lockon.ui.components.PillShape
import app.lockon.ui.components.PillSwitch
import app.lockon.ui.components.SectionHeader
import app.lockon.model.CardSort
import app.lockon.ui.components.glass
import app.lockon.ui.theme.LockOnShapes
import app.lockon.ui.update.AppUpdateSection
import kotlinx.coroutines.launch

/** One group of settings, with its name in the side rail on wide windows. */
private class SettingsSection(val title: String, val content: @Composable ColumnScope.() -> Unit)

/**
 * This device's preferences, in a few named groups ordered from general to the app itself, each a
 * list of rows with the setting's name on the left and its control at the end. Wide windows keep
 * one readable column and list the groups in a rail beside it, which jumps to a group and shows
 * where you are; phones stack the groups and put wide controls under their names.
 * Sync has its own tab beside this one.
 */
@Composable
fun SettingsScreen(container: AppContainer, state: AppState, files: FileBridge) {
    val settings = container.settings
    val scope = rememberCoroutineScope()
    val theme by settings.theme.collectAsState()
    val size by settings.size.collectAsState()
    val motion by settings.motion.collectAsState()
    val start by settings.start.collectAsState()
    val editorBar by settings.editorBar.collectAsState()
    val moreHidden by settings.moreHidden.collectAsState()
    val catalogChecks by settings.catalogChecks.collectAsState()
    val autoDeckColors by settings.autoDeckColors.collectAsState()
    val editorSplit by settings.editorSplit.collectAsState()
    val cardSort by settings.cardSort.collectAsState()
    val showArtwork by container.catalog.showArtwork.collectAsState()
    // The name is read once at launch, so a new one shows here until then.
    var deviceName by remember { mutableStateOf(container.identity.deviceName) }
    var renaming by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // The same width as the app's own split into panes (UX rule N4).
        val wide = maxWidth >= 840.dp
        // The phone editor's bar shows on phones and in narrow windows; the divider only in wide ones.
        val barShown = isMobile || !wide
        val sections = listOf(
            SettingsSection("General") {
                SettingRow("Open on", wideControl = true) {
                    Segmented(StartChoice.entries, start, { it.label }) { scope.launch { settings.setStart(it) } }
                }
                SettingRow("Sort cards by", detail = "On the Cards screen") {
                    MenuChip(selected = false, label = { Text(cardSort.label) }) { close ->
                        CardSort.entries.forEach { sort ->
                            GameMenuItem(sort.label, selected = sort == cardSort, onClick = {
                                close(); scope.launch { settings.setCardSort(sort) }
                            })
                        }
                    }
                }
                SettingRow(
                    "Device name",
                    // A pending name is the one place a note earns its line: it isn't live yet.
                    detail = if (deviceName == container.identity.deviceName) deviceName else "$deviceName · from the next launch",
                ) {
                    GameButton("Rename", onClick = { renaming = true }, style = GameButtonStyle.SECONDARY, compact = true)
                }
                SettingRow("Welcome tour") {
                    GameButton("Show again", onClick = { state.tourStop = 0; state.touring = true }, style = GameButtonStyle.SECONDARY, compact = true)
                }
            },
            SettingsSection("Appearance") {
                SettingRow("Theme", wideControl = true) {
                    Segmented(ThemeChoice.entries, theme, { it.label }) { scope.launch { settings.setTheme(it) } }
                }
                SettingRow("Interface size", wideControl = true) {
                    Segmented(InterfaceSize.entries, size, { it.label }) { scope.launch { settings.setSize(it) } }
                }
                SettingRow("Animations", wideControl = true) {
                    Segmented(MotionChoice.entries, motion, { it.label }) { scope.launch { settings.setMotion(it) } }
                }
                Toggle("Show card artwork", showArtwork) { scope.launch { container.catalog.setShowArtwork(it) } }
            },
            SettingsSection("Deck editor") {
                if (barShown) {
                    SettingRow("Bar order", wideControl = true) {
                        MenuChip(selected = false, label = { Text(editorBar.label) }) { close ->
                            EditorBarOrder.entries.forEach { order ->
                                GameMenuItem(order.label, selected = order == editorBar, onClick = {
                                    close(); scope.launch { settings.setEditorBar(order) }
                                })
                            }
                        }
                    }
                    MoreMenuChoice(moreHidden) { item, shown ->
                        scope.launch {
                            if (!settings.setMoreShown(item, shown)) state.notify("The More menu keeps at least one section.")
                        }
                    }
                }
                Toggle("Show only deck colors once a deck has all its colors", autoDeckColors) {
                    scope.launch { settings.setAutoDeckColors(it) }
                }
                if (wide) SettingRow("Divider between cards and deck") {
                    GameButton("Reset", style = GameButtonStyle.SECONDARY, compact = true, enabled = editorSplit != null, onClick = {
                        scope.launch { settings.setEditorSplit(null) }
                    })
                }
            },
            SettingsSection("Data") {
                CardDatabaseSetting(container, state, files)
                Toggle("Check weekly for card list updates", catalogChecks) { scope.launch { settings.setCatalogChecks(it) } }
                SettingRow("Back up all decks") {
                    GameButton("Back up", style = GameButtonStyle.SECONDARY, compact = true, onClick = {
                        state.appScope.launch {
                            val backup = BackupFormat.encode(container.decks.exportBackup())
                            if (files.saveText("lock-on-backup.json", "application/json", backup)) state.notify("Backup saved.")
                        }
                    })
                }
            },
            SettingsSection("About") { AppUpdateSection(container.updater, state.appScope) },
        )

        val scrollState = rememberScrollState()
        val bottom = 16.dp + LocalNavClearance.current
        if (wide) {
            // Where each group starts in the scrolled column, so the rail can jump there and follow along.
            val tops = remember { mutableStateMapOf<Int, Int>() }
            val active by remember {
                derivedStateOf {
                    if (scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue) sections.lastIndex
                    else tops.filterValues { it <= scrollState.value + 24 }.keys.maxOrNull() ?: 0
                }
            }
            CompositionLocalProvider(LocalRoomyRows provides true) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                ) {
                    SettingsRail(sections.map { it.title }, active, Modifier.width(RAIL_WIDTH).padding(top = 8.dp)) { i ->
                        scope.launch { scrollState.animateScrollTo(tops[i] ?: 0) }
                    }
                    Column(
                        Modifier.weight(1f, fill = false).widthIn(max = CONTENT_WIDTH).fillMaxHeight()
                            .verticalScroll(scrollState).padding(top = 8.dp, bottom = bottom),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        sections.forEachIndexed { i, section ->
                            SettingsGroup(section, Modifier.onGloballyPositioned { tops[i] = it.positionInParent().y.toInt() })
                        }
                    }
                }
            }
        } else {
            CompositionLocalProvider(LocalRoomyRows provides (maxWidth >= 600.dp)) {
                Box(Modifier.fillMaxSize().verticalScroll(scrollState), contentAlignment = Alignment.TopCenter) {
                    Column(
                        Modifier.widthIn(max = CONTENT_WIDTH).fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottom),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        sections.forEach { SettingsGroup(it) }
                    }
                }
            }
        }
        VerticalScrollBar(scrollState, Modifier.align(Alignment.TopEnd))
    }

    if (renaming) {
        var text by remember { mutableStateOf(deviceName) }
        GameDialog(
            onDismissRequest = { renaming = false },
            title = "Rename this device",
            text = {
                OutlinedTextField(text, { text = it.take(60) }, label = { Text("Device name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                GameButton("Rename", compact = true, enabled = text.isNotBlank(), onClick = {
                    val name = text.trim()
                    renaming = false
                    deviceName = name
                    state.appScope.launch { container.devices.rename(name) }
                })
            },
            dismissButton = { GameButton("Cancel", onClick = { renaming = false }, style = GameButtonStyle.GHOST, compact = true) },
        )
    }
}

private val RAIL_WIDTH = 200.dp
/** A comfortable reading width: wider and a row's name drifts too far from its control. */
private val CONTENT_WIDTH = 760.dp
/** How wide a choice of a few options is when it sits beside its name. */
private val SEGMENTED_WIDTH = 360.dp

/** Whether rows have room for every control beside the setting's name, rather than under it. */
internal val LocalRoomyRows = compositionLocalOf { false }

/** The groups' names down the side of a wide Settings: the one in view is teal, and a click scrolls to one. */
@Composable
private fun SettingsRail(titles: List<String>, active: Int, modifier: Modifier, onPick: (Int) -> Unit) {
    Column(modifier.glass(LockOnShapes.tile).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        titles.forEachIndexed { i, title ->
            val selected = i == active
            Box(
                Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(PillShape)
                    .background(if (selected) Accent.copy(alpha = 0.14f) else Accent.copy(alpha = 0f))
                    .selectable(selected, role = Role.Tab) { onPick(i) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = if (selected) Accent else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun SettingsGroup(section: SettingsSection, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(section.title)
            section.content(this)
        }
    }
}

/**
 * One setting: its name, and [detail] under it for what it is set to, with [control] at the end of
 * the row. A [wideControl], such as a choice of several options, drops under the name when the row
 * is narrow, and a [stacked] one, such as chips that wrap, always sits under it. [note] is news about
 * the setting, in teal.
 */
@Composable
internal fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    note: String? = null,
    wideControl: Boolean = false,
    stacked: Boolean = false,
    control: @Composable () -> Unit,
) {
    val label = @Composable { labelModifier: Modifier ->
        Column(labelModifier) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        }
    }
    if (stacked || wideControl && !LocalRoomyRows.current) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            label(Modifier)
            control()
        }
    } else {
        Row(
            modifier.fillMaxWidth().heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            label(Modifier.weight(1f))
            control()
        }
    }
}

/** A switch whose whole row toggles it, so the target is the row and not only the switch (UX rule R1). */
@Composable
private fun Toggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(title, Modifier.toggleable(checked, role = Role.Switch, onValueChange = onChange)) {
        PillSwitch(checked = checked, onCheckedChange = null)
    }
}

/** One of a few options as a sliding segmented pill: full width under its name on phones, a fixed width beside it otherwise. */
@Composable
private fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    PillSegmented(
        options.map(label), options.indexOf(selected), { onSelect(options[it]) },
        if (LocalRoomyRows.current) Modifier.width(SEGMENTED_WIDTH) else Modifier.fillMaxWidth(),
    )
}

/** Which sections the phone editor's More menu holds: each is a chip, teal while it is in the menu. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoreMenuChoice(hidden: Set<EditorMoreItem>, onChange: (EditorMoreItem, Boolean) -> Unit) {
    SettingRow("In the More menu", stacked = true) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            EditorMoreItem.entries.forEach { item ->
                val shown = item !in hidden
                PillChip(selected = shown, onClick = { onChange(item, !shown) }, label = { Text(item.label) })
            }
        }
    }
}
