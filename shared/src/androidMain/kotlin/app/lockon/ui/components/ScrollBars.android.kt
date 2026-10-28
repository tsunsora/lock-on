package app.lockon.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Phones scroll by touch, so they show no scroll bar.

@Composable
actual fun VerticalScrollBar(state: ScrollState, modifier: Modifier) = Unit

@Composable
actual fun VerticalScrollBar(state: LazyListState, modifier: Modifier) = Unit

@Composable
actual fun VerticalScrollBar(state: LazyGridState, modifier: Modifier) = Unit
