package app.lockon.ui.io
import app.lockon.ui.components.GameDialog
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.GameMenu
import app.lockon.ui.components.GameMenuItem

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
import app.lockon.io.BackupFormat
import app.lockon.io.DeckListFormat
import app.lockon.platform.FileBridge
import app.lockon.ui.AppState
import app.lockon.ui.Screen
import kotlinx.coroutines.launch

@Composable
fun ImportDialog(
    container: AppContainer,
    files: FileBridge,
    state: AppState,
    onDismiss: () -> Unit,
    folders: List<String> = emptyList(),
    folder: String? = null,
) {
    var text by remember { mutableStateOf("") }
    var problems by remember { mutableStateOf<List<String>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    // null is "No folder" and "" is the typed new folder; anything else names an existing folder.
    var folderChoice by remember { mutableStateOf(folder) }
    var newFolder by remember { mutableStateOf("") }
    var folderMenu by remember { mutableStateOf(false) }
    val scope = state.appScope
    // A backup carries its own names and folders, so the deck options apply to deck lists only.
    val isBackup = text.removePrefix("﻿").trimStart().startsWith("{")

    fun import() {
        val input = text.removePrefix("﻿").trim()
        if (input.isEmpty()) return
        scope.launch {
            if (input.startsWith("{")) {
                val result = runCatching { container.decks.importBackup(BackupFormat.decode(input)) }
                result.onSuccess { count -> state.notify("Imported $count decks."); onDismiss() }
                result.onFailure { problems = listOf(it.message ?: "Could not read the backup.") }
            } else {
                val parsed = DeckListFormat.parse(input, container.catalog.cards.value, printings = container.catalog.printings.value)
                if (parsed.snapshot.entries.isEmpty()) {
                    problems = parsed.problems.ifEmpty { listOf("No cards found. Use lines like \"4 SMP-001\".") }
                    return@launch
                }
                // Reuse an existing folder's spelling when only the case differs, as moving a deck does.
                val typed = (if (folderChoice == "") newFolder else folderChoice)?.trim()?.takeIf { it.isNotEmpty() }
                val target = typed?.let { t -> folders.firstOrNull { it.equals(t, ignoreCase = true) } ?: t }
                val snapshot = parsed.snapshot.copy(name = name.trim().ifEmpty { parsed.snapshot.name }, folder = target)
                val id = container.decks.create(snapshot)
                state.notify(if (parsed.problems.isEmpty()) "Deck imported." else "Deck imported with ${parsed.problems.size} skipped or unknown lines.")
                onDismiss()
                state.open(Screen.DeckEditor(id))
            }
        }
    }

    GameDialog(
        onDismissRequest = onDismiss,
        title = "Import",
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Paste a deck list or a Lock On backup, or open a file.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    text, { text = it; problems = emptyList() },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp),
                    placeholder = { Text("# Deck name\n4 SMP-001\n10 SMP-019") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
                GameButton("Open file", onClick = {
                    scope.launch { files.openText(listOf("text/plain", "application/json"))?.let { text = it } }
                }, style = GameButtonStyle.SECONDARY)
                if (!isBackup) {
                    OutlinedTextField(
                        name, { name = it }, label = { Text("Deck name") }, singleLine = true,
                        placeholder = { Text("From the list") }, modifier = Modifier.fillMaxWidth(),
                    )
                    Box {
                        GameButton(
                            when (folderChoice) { null -> "Folder: None"; "" -> "Folder: New folder"; else -> "Folder: $folderChoice" },
                            onClick = { folderMenu = true }, style = GameButtonStyle.SECONDARY, icon = Icons.Default.ArrowDropDown,
                        )
                        GameMenu(folderMenu, onDismissRequest = { folderMenu = false }) {
                            (listOf("No folder" to null) + folders.map { it to it } + ("New folder" to "")).forEach { (label, value) ->
                                GameMenuItem(label, onClick = { folderChoice = value; folderMenu = false }, selected = folderChoice == value)
                            }
                        }
                    }
                    if (folderChoice == "") {
                        OutlinedTextField(newFolder, { newFolder = it }, label = { Text("Folder name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
                problems.take(6).forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { GameButton("Import", onClick = ::import, enabled = text.isNotBlank(), compact = true) },
        dismissButton = { GameButton("Cancel", onClick = onDismiss, style = GameButtonStyle.GHOST, compact = true) },
    )
}
