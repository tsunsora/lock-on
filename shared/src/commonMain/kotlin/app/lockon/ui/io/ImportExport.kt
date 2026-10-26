package app.lockon.ui.io

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.lockon.AppContainer
import app.lockon.io.BackupFormat
import app.lockon.io.DeckListFormat
import app.lockon.platform.FileBridge
import app.lockon.ui.AppState
import app.lockon.ui.Screen
import kotlinx.coroutines.launch

@Suppress("DEPRECATION") // LocalClipboard needs ClipEntry, which differs per platform.
@Composable
fun ExportDialog(title: String, text: String, suggestedName: String, files: FileBridge, state: AppState, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            SelectionContainer {
                Text(
                    text,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(text))
                    state.notify("Deck list copied.")
                }) { Text("Copy") }
                TextButton(onClick = {
                    state.appScope.launch { if (files.saveText(suggestedName, "text/plain", text)) state.notify("Deck list saved.") }
                }) { Text("Save file") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

/** Accepts a pasted or opened deck list, or a Lock On JSON backup. */
@Composable
fun ImportDialog(container: AppContainer, files: FileBridge, state: AppState, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var problems by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = state.appScope

    fun import() {
        val input = text.trim()
        if (input.isEmpty()) return
        scope.launch {
            if (input.startsWith("{")) {
                val result = runCatching { container.decks.importBackup(BackupFormat.decode(input)) }
                result.onSuccess { count -> state.notify("Imported $count decks."); onDismiss() }
                result.onFailure { problems = listOf(it.message ?: "Could not read the backup.") }
            } else {
                val parsed = DeckListFormat.parse(input, container.catalog.cards.value)
                if (parsed.snapshot.entries.isEmpty()) {
                    problems = parsed.problems.ifEmpty { listOf("No cards found. Use lines like \"4 SMP-001\".") }
                    return@launch
                }
                val id = container.decks.create(parsed.snapshot)
                state.notify(if (parsed.problems.isEmpty()) "Deck imported." else "Deck imported with ${parsed.problems.size} skipped or unknown lines.")
                onDismiss()
                state.open(Screen.DeckEditor(id))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Paste a deck list or a Lock On backup, or open a file.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    text, { text = it; problems = emptyList() },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp),
                    placeholder = { Text("# Deck name\n4 SMP-001\n10 SMP-019") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
                OutlinedButton(onClick = {
                    scope.launch { files.openText(listOf("text/plain", "application/json"))?.let { text = it } }
                }) { Text("Open file") }
                problems.take(6).forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = ::import, enabled = text.isNotBlank()) { Text("Import") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
