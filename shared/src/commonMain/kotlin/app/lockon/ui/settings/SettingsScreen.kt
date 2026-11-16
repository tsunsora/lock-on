package app.lockon.ui.settings

import app.lockon.ui.components.RevealValue
import app.lockon.ui.components.softThumbFill
import app.lockon.ui.components.softThumbTone
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import app.lockon.ui.components.softThumbInk
import app.lockon.ui.components.softThumb
import app.lockon.ui.components.SoftTile
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
import app.lockon.data.ColorStyle
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.lockon.data.hexRgb
import app.lockon.data.parseHexRgb
import app.lockon.ui.components.PillTextField

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
    val systemColors by settings.systemColors.collectAsState()
    val colorStyle by settings.colorStyle.collectAsState()
    val pureBlack by settings.pureBlack.collectAsState()
    val themeColor by settings.themeColor.collectAsState()
    val accent by app.lockon.ui.theme.SystemAccent.color.collectAsState()
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
                // Says where the colours come from, or why the theme colour shows while it's on (UX rule V3a).
                val usable = accent?.let { app.lockon.ui.theme.usableSeed(it) } == true
                val accentShown = systemColors && usable
                Toggle(
                    "Material You colors", systemColors,
                    detail = when {
                        !systemColors -> null
                        usable -> app.lockon.platform.systemColorsSource
                        accent != null -> "Your accent color is gray, so your theme color shows"
                        else -> "Not available on this device, so your theme color shows"
                    },
                ) { scope.launch { settings.setSystemColors(it) } }
                // Without the system's accent, a colour of your own to build the palette from (UX rule V3a).
                if (!accentShown) ThemeColorSetting(themeColor, colorStyle) { scope.launch { settings.setThemeColor(it) } }
                if (accentShown || themeColor != null) SettingRow("Color style") {
                    MenuChip(selected = false, label = { Text(colorStyle.label) }) { close ->
                        ColorStyle.entries.forEach { style ->
                            GameMenuItem(style.label, selected = style == colorStyle, onClick = {
                                close(); scope.launch { settings.setColorStyle(style) }
                            })
                        }
                    }
                }
                if (theme != ThemeChoice.LIGHT) Toggle("Pure black", pureBlack, detail = "Black background in the dark theme") {
                    scope.launch { settings.setPureBlack(it) }
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
                            if (!settings.setMoreShown(item, shown)) state.notify("The More menu keeps at least one section")
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
                            if (files.saveText("lock-on-backup.json", "application/json", backup)) state.notify("Backup saved")
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
private val SEGMENTED_WIDTH = 288.dp

/** Whether rows have room for every control beside the setting's name, rather than under it. */
internal val LocalRoomyRows = compositionLocalOf { false }

/** A solid panel, with space rather than hairlines between rows. */
@Composable
private fun SettingsGroup(section: SettingsSection) {
    SoftTile(Modifier.fillMaxWidth(), solid = true) {
        Text(section.title, style = MaterialTheme.typography.titleMedium)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) { section.content(this) }
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
    value: String? = null,
    wideControl: Boolean = false,
    stacked: Boolean = false,
    control: @Composable () -> Unit,
) {
    val label = @Composable { labelModifier: Modifier ->
        Column(labelModifier) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            RevealValue(note) { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
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
private fun Toggle(title: String, checked: Boolean, detail: String? = null, onChange: (Boolean) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    SettingRow(
        title,
        detail = detail,
        modifier = Modifier.hoverable(interaction).toggleable(checked, interaction, indication = null, role = Role.Switch, onValueChange = onChange),
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

/**
 * Which sections the phone editor's More menu holds, as a grid of choices on one soft track, the same
 * thumb as the app's other choices marking those in the menu.
 */
@Composable
private fun MoreMenuChoice(hidden: Set<EditorMoreItem>, onChange: (EditorMoreItem, Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    SettingRow("In the More menu", stacked = true) {
        Column(
            Modifier.fillMaxWidth().clip(LockOnShapes.button).background(scheme.onSurface.copy(alpha = 0.09f)).padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            EditorMoreItem.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    row.forEach { item ->
                        val shown = item !in hidden
                        Box(
                            Modifier.weight(1f).height(36.dp).clip(LockOnShapes.inset).background(softThumbFill(shown))
                                .toggleable(shown, role = Role.Checkbox, interactionSource = null, indication = null) { onChange(item, it) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(item.label, style = MaterialTheme.typography.labelLarge, color = softThumbTone(shown), maxLines = 1)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * Theme color (UX rule V3a): Lock On's own Sky and Twilight, a few ready-made colours, or any colour
 * typed as hex, which Material colours are built from. Each swatch shows the scheme it makes in the
 * current theme and [style]: primary over secondary and the primary container. The swatches share one
 * row across the panel's width, with the field under them at the same width.
 */
@Composable
private fun ThemeColorSetting(selected: Int?, style: ColorStyle, onSelect: (Int?) -> Unit) {
    SettingRow("Theme color", stacked = true) {
        val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val presets = app.lockon.ui.theme.ThemeColorPresets
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val count = presets.size + 1
            val gap = 8.dp
            val size = ((maxWidth - gap * (count - 1)) / count).coerceIn(32.dp, 48.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ThemeSwatch("Lock On", null, selected == null, dark, style, size) { onSelect(null) }
                presets.forEach { (name, rgb) -> ThemeSwatch(name, rgb, selected == rgb, dark, style, size) { onSelect(rgb) } }
            }
        }
        // What's typed stays as typed; a colour picked another way replaces it.
        var hex by remember { mutableStateOf(selected?.let { "#" + hexRgb(it) }.orEmpty()) }
        LaunchedEffect(selected) {
            if (parseHexRgb(hex) != selected) hex = selected?.let { "#" + hexRgb(it) }.orEmpty()
        }
        PillTextField(
            hex,
            { input ->
                val text = input.filter { it == '#' || it.isDigit() || it.lowercaseChar() in 'a'..'f' }.take(7).uppercase()
                hex = text
                parseHexRgb(text)?.let { if (it != selected) onSelect(it) }
            },
            placeholder = "Or a hex color, like #2E86C1",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** One theme colour as a round swatch of the scheme it makes, ringed in the action colour when chosen. */
@Composable
private fun ThemeSwatch(name: String, rgb: Int?, selected: Boolean, dark: Boolean, style: ColorStyle, size: Dp, onClick: () -> Unit) {
    val scheme = remember(rgb, dark, style) {
        rgb?.let { app.lockon.ui.theme.materialYouScheme(app.lockon.ui.theme.themeColor(it), dark, style) }
            ?: if (dark) app.lockon.ui.theme.DarkColors else app.lockon.ui.theme.LightColors
    }
    val ring by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, motion(app.lockon.ui.components.Motion.SHORT), label = "swatch-ring")
    Box(
        Modifier.size(size).clip(CircleShape)
            .border(if (selected) 2.5.dp else 1.dp, ring, CircleShape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name }
            .padding(if (selected) 6.dp else 4.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(scheme.primary, 180f, 180f, useCenter = true)
            drawArc(scheme.secondary, 90f, 90f, useCenter = true)
            drawArc(scheme.primaryContainer, 0f, 90f, useCenter = true)
        }
    }
}
