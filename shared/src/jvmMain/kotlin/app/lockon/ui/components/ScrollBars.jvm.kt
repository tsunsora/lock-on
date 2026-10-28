package app.lockon.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun VerticalScrollBar(state: ScrollState, modifier: Modifier) = ScrollBar(rememberScrollbarAdapter(state), modifier)

@Composable
actual fun VerticalScrollBar(state: LazyListState, modifier: Modifier) = ScrollBar(rememberScrollbarAdapter(state), modifier)

@Composable
actual fun VerticalScrollBar(state: LazyGridState, modifier: Modifier) = ScrollBar(rememberScrollbarAdapter(state), modifier)

@Composable
private fun ScrollBar(adapter: ScrollbarAdapter, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    VerticalScrollbar(
        adapter,
        // Inset from the panel's rounded corners, and a hair from its edge.
        modifier.fillMaxHeight().padding(top = 10.dp, bottom = 10.dp, end = 3.dp),
        style = ScrollbarStyle(
            minimalHeight = 40.dp,
            thickness = 6.dp,
            shape = PillShape,
            hoverDurationMillis = 160,
            unhoverColor = scheme.onSurfaceVariant.copy(alpha = if (isTwilight()) 0.36f else 0.34f),
            hoverColor = scheme.primary.copy(alpha = 0.8f),
        ),
    )
}
