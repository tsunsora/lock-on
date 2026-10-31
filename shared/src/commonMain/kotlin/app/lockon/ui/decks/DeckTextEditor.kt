package app.lockon.ui.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lockon.io.DeckListFormat
import app.lockon.io.ExportOrder
import app.lockon.model.Card
import app.lockon.model.DeckSnapshot
import app.lockon.model.Printing
import app.lockon.ui.components.GameButton
import app.lockon.ui.components.GameButtonStyle
import app.lockon.ui.components.GameToastHost
import app.lockon.ui.components.LocalNavClearance
import app.lockon.ui.theme.LockOnShapes
import kotlinx.coroutines.launch

/**
 * The deck as a text list to edit in place, a page of the phone deck editor. The deck read back
 * from the edited list goes to [onApply]; lines it can't read are named before it is applied.
 */
@Composable
fun DeckTextEditor(
    deck: DeckSnapshot,
    cards: Map<String, Card>,
    printings: Map<String, List<Printing>>,
    onApply: (DeckSnapshot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listed = remember(deck, cards) { DeckListFormat.export(deck, cards, ExportOrder.LEVEL) }
    // It starts over from the deck whenever the deck changes, applied edits included.
    var edited by remember(listed) { mutableStateOf(TextFieldValue(listed)) }
    val dirty = edited.text != listed
    val parsed = remember(edited.text, cards, printings) {
        if (dirty) DeckListFormat.parse(edited.text, cards, defaultName = deck.name, printings = printings) else null
    }
    val scheme = MaterialTheme.colorScheme
    val toasts = remember { SnackbarHostState() }
    val toastScope = rememberCoroutineScope()

    Box(modifier) {
        Column(
            Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp + LocalNavClearance.current),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f).fillMaxWidth().clip(LockOnShapes.tile).background(scheme.surfaceContainerLow)) {
                BasicTextField(
                    edited, { edited = it },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = scheme.onSurface),
                    cursorBrush = SolidColor(scheme.primary),
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp).semantics { contentDescription = "Deck list" },
                )
                // Lines the deck can't read back say so before the list is applied, so nothing goes missing unnoticed.
                parsed?.problems?.takeIf { it.isNotEmpty() }?.let { problems ->
                    Text(
                        if (problems.size == 1) problems.first() else "${problems.first()} (+${problems.size - 1} more)",
                        style = MaterialTheme.typography.bodySmall, color = scheme.error, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().background(scheme.errorContainer.copy(alpha = 0.4f)).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameButton("Undo edits", onClick = { edited = TextFieldValue(listed) }, Modifier.weight(1f),
                    enabled = dirty, style = GameButtonStyle.SECONDARY)
                GameButton("Apply to deck", onClick = {
                    parsed?.let { onApply(it.snapshot) }
                    toastScope.launch {
                        toasts.currentSnackbarData?.dismiss()
                        toasts.showSnackbar("Deck updated from the list.")
                    }
                }, Modifier.weight(1f), enabled = parsed != null)
            }
        }
        GameToastHost(toasts, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp + LocalNavClearance.current))
    }
}
