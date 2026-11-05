package app.lockon.ui.settings

import androidx.compose.ui.geometry.Offset
import app.lockon.ui.components.motion
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
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

/** One group of settings. */
private class SettingsSection(val title: String, val content: @Composable ColumnScope.() -> Unit)

/**
 * This device's preferences, in a few named groups ordered from general to the app itself, each a
 * list of rows with the setting's name on the left and its control at the end. Wide windows show two
 * centred columns; phones stack the groups and put wide controls under their names.
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
    val deckAsCards by settings.deckAsCards.collectAsState()
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
                SettingRow("Sort cards by") {
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
                    value = deviceName,
                    // A pending name is the one place a note earns its line: it isn't live yet.
                    note = if (deviceName == container.identity.deviceName) null else "From the next launch",
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
                Toggle("Card artwork", showArtwork) { scope.launch { container.catalog.setShowArtwork(it) } }
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
                Toggle("Open decks as card images", deckAsCards) { scope.launch { settings.setDeckAsCards(it) } }
                Toggle("Deck colors only", autoDeckColors) {
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
                Toggle("Weekly card list check", catalogChecks) { scope.launch { settings.setCatalogChecks(it) } }
                SettingRow("Back up all decks") {
                    GameButton("Save", style = GameButtonStyle.SECONDARY, compact = true, onClick = {
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
            // Two centred columns: how the app looks and behaves on the left, its data and itself on the right.
            CompositionLocalProvider(LocalRoomyRows provides true) {
                Box(Modifier.fillMaxSize().verticalScroll(scrollState), contentAlignment = Alignment.TopCenter) {
                    Row(
                        Modifier.widthIn(max = 1080.dp).fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = bottom),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        val left = setOf("General", "Appearance")
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            sections.filter { it.title in left }.forEach { SettingsGroup(it) }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            listOf("Data", "Deck editor", "About").mapNotNull { title -> sections.firstOrNull { it.title == title } }.forEach { SettingsGroup(it) }
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

/** A comfortable reading width: wider and a row's name drifts too far from its control. */
private val CONTENT_WIDTH = 760.dp
/** How wide a choice of a few options is when it sits beside its name. */
private val SEGMENTED_WIDTH = 290.dp

/** Whether rows have room for every control beside the setting's name, rather than under it. */
internal val LocalRoomyRows = compositionLocalOf { false }

@Composable
private fun SettingsGroup(section: SettingsSection, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(section.title, style = MaterialTheme.typography.titleMedium)
            RuledRows { section.content(this) }
        }
    }
}

/** Spaces rows evenly and notes where the hairlines between them go, skipping anything with no height, such as a closed dialog. */
private class RuledArrangement(private val gap: androidx.compose.ui.unit.Dp) : Arrangement.Vertical {
    var rules: List<Int> = emptyList()
        private set
    override val spacing get() = gap
    override fun androidx.compose.ui.unit.Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        val space = gap.roundToPx()
        var y = 0
        val lines = mutableListOf<Int>()
        sizes.forEachIndexed { i, size ->
            if (size > 0 && y > 0) { lines += y + space / 2; y += space }
            outPositions[i] = y
            y += size
        }
        rules = lines
    }
}

/** Rows with a hairline between each. */
@Composable
private fun RuledRows(content: @Composable ColumnScope.() -> Unit) {
    val line = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val arrangement = remember { RuledArrangement(16.dp) }
    Column(
        Modifier.fillMaxWidth().drawBehind {
            arrangement.rules.forEach { y -> drawLine(line, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1.dp.toPx()) }
        },
        verticalArrangement = arrangement, content = content,
    )
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
    value: String? = null,
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
            value?.let { Text(it, style = MaterialTheme.typography.bodyMedium.merge(app.lockon.ui.theme.TabularNumbers), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
            control()
        }
    }
}

/** A switch whose whole row toggles it, so the target is the row and not only the switch (UX rule R1). */
@Composable
private fun Toggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    SettingRow(
        title,
        Modifier.hoverable(interaction).toggleable(checked, interaction, indication = null, role = Role.Switch, onValueChange = onChange),
    ) {
        PillSwitch(checked = checked, onCheckedChange = null, interactionSource = interaction)
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
