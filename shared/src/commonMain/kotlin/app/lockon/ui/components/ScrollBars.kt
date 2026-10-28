package app.lockon.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/*
 * A slim pill scroll bar for the right edge of a scrolling area, drawn over its content: faint at
 * rest, accent while hovered or dragged. Desktop shows it, so a long list says how long it is and
 * can be dragged through; phones scroll by touch and show none.
 */

@Composable
expect fun VerticalScrollBar(state: ScrollState, modifier: Modifier = Modifier)

@Composable
expect fun VerticalScrollBar(state: LazyListState, modifier: Modifier = Modifier)

@Composable
expect fun VerticalScrollBar(state: LazyGridState, modifier: Modifier = Modifier)
