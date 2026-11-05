package app.lockon.ui.io
import app.lockon.ui.components.Reveal
import app.lockon.ui.components.LineIcons
import app.lockon.ui.components.GameDialog
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.GameMenu
import app.lockon.ui.components.GameMenuItem

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.lockon.AppContainer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import app.lockon.model.DeckSnapshot
import app.lockon.ui.components.PillChip
import app.lockon.ui.components.PillSegmented
import app.lockon.ui.theme.TabularNumbers
import androidx.compose.material3.Icon
import app.lockon.io.BackupFormat
import app.lockon.io.DeckListFormat
import app.lockon.platform.FileBridge
import app.lockon.ui.AppState
import app.lockon.ui.Screen
import kotlinx.coroutines.launch

/**
 * Makes a deck: name it, pick a folder, and start empty or from a pasted list. The list can also be a
 * Lock On backup, whose decks bring their own names and folders.
 *
 * @param folder the folder chosen to begin with, such as the one open behind the dialog.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewDeckDialog(
    container: AppContainer,
    files: FileBridge,
    state: AppState,
    onDismiss: () -> Unit,
    folders: List<String> = emptyList(),
    folder: String? = null,
    startPasting: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    var name by remember { mutableStateOf("") }
    var pasting by remember { mutableStateOf(startPasting) }
    var text by remember { mutableStateOf("") }
    var problems by remember { mutableStateOf<List<String>>(emptyList()) }
    // null is "No folder" and "" is the typed new folder; anything else names an existing folder.
    var folderChoice by remember { mutableStateOf(folder) }
    var newFolder by remember { mutableStateOf("") }
    val scope = state.appScope
    val input = text.removePrefix("\uFEFF").trim()
    // A backup carries its own names and folders, so the deck options apply to deck lists only.
    val isBackup = pasting && input.startsWith("{")
    val catalog by container.catalog.cards.collectAsState()
    val printings by container.catalog.printings.collectAsState()
    val parsed = remember(input, catalog, printings) {
        if (input.isEmpty() || input.startsWith("{")) null else DeckListFormat.parse(input, catalog, printings = printings)
    }

    fun create() {
        // Reuse an existing folder's spelling when only the case differs, as moving a deck does.
        val typed = (if (folderChoice == "") newFolder else folderChoice)?.trim()?.takeIf { it.isNotEmpty() }
        val target = typed?.let { t -> folders.firstOrNull { it.equals(t, ignoreCase = true) } ?: t }
        scope.launch {
            when {
                !pasting -> {
                    val id = container.decks.create(DeckSnapshot(name = name.trim().ifEmpty { "Untitled deck" }, folder = target))
                    onDismiss()
                    state.open(Screen.DeckEditor(id, startTab = 1))
                }
                isBackup -> {
                    val result = runCatching { container.decks.importBackup(BackupFormat.decode(input)) }
                    result.onSuccess { count -> state.notify("Imported $count decks."); onDismiss() }
                    result.onFailure { problems = listOf(it.message ?: "Could not read the backup.") }
                }
                parsed == null || parsed.snapshot.entries.isEmpty() ->
                    problems = parsed?.problems.orEmpty().ifEmpty { listOf("No cards found. Use lines like \"4 SMP-001\".") }
                else -> {
                    val id = container.decks.create(parsed.snapshot.copy(name = name.trim().ifEmpty { parsed.snapshot.name }, folder = target))
                    if (parsed.problems.isNotEmpty()) state.notify("Deck made with ${parsed.problems.size} skipped or unknown lines.")
                    onDismiss()
                    state.open(Screen.DeckEditor(id))
                }
            }
        }
    }

    GameDialog(
        onDismissRequest = onDismiss,
        title = "New deck",
        text = {
            // Scrolls so the buttons stay in reach when the keyboard leaves too little room for it all.
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (!isBackup) {
                    val focus = remember { FocusRequester() }
                    LaunchedEffect(Unit) { focus.requestFocus() }
                    OutlinedTextField(
                        name, { name = it }, placeholder = { Text("Deck name") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (folders.map { it to it } + ("No folder" to null) + ("New folder" to "")).forEach { (label, value) ->
                            PillChip(
                                selected = folderChoice == value, onClick = { folderChoice = value }, label = { Text(label) },
                                leadingIcon = { Icon(if (value == "") LineIcons.Folderplus else LineIcons.Folder, contentDescription = null, Modifier.size(16.dp)) },
                            )
                        }
                    }
                    Reveal(folderChoice == "") {
                        OutlinedTextField(newFolder, { newFolder = it }, placeholder = { Text("Folder name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
                PillSegmented(
                    listOf("Empty", "Paste a list"), if (pasting) 1 else 0, { pasting = it == 1; problems = emptyList() },
                    Modifier.fillMaxWidth(), icons = listOf(LineIcons.Plus, LineIcons.Paste),
                )
                Reveal(pasting) { Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        text, { text = it; problems = emptyList() },
                        modifier = Modifier.fillMaxWidth().height(190.dp),
                        placeholder = { Text("4 SMP-001\n4 SMP-002", fontFamily = FontFamily.Monospace) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val read = parsed?.snapshot?.entries?.sumOf { it.quantity } ?: 0
                        val skipped = parsed?.problems?.size ?: 0
                        when {
                            isBackup -> Text("Lock On backup", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                            read > 0 -> {
                                Icon(LineIcons.Check, contentDescription = null, Modifier.size(16.dp), tint = scheme.onSurfaceVariant)
                                Text(
                                    "$read cards read" + if (skipped > 0) ", $skipped lines skipped" else "",
                                    style = MaterialTheme.typography.labelLarge.merge(TabularNumbers), color = scheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        GameButton("Open file", onClick = {
                            scope.launch { files.openText(listOf("text/plain", "application/json"))?.let { text = it; problems = emptyList() } }
                        }, style = GameButtonStyle.GHOST, compact = true)
                    }
                    problems.take(6).forEach { Text(it, color = scheme.error, style = MaterialTheme.typography.bodySmall) }
                } }
            }
        },
        confirmButton = { GameButton(if (isBackup) "Import" else "Create", onClick = ::create, enabled = !pasting || input.isNotEmpty(), compact = true) },
        dismissButton = { GameButton("Cancel", onClick = onDismiss, style = GameButtonStyle.SECONDARY, compact = true) },
    )
}
