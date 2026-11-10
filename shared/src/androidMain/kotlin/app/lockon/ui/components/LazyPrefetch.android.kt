package app.lockon.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable

// Android builds rows ahead of a scroll by itself, between frames.

@Composable
actual fun rememberPrefetchingGridState(): LazyGridState = rememberLazyGridState()

@Composable
actual fun rememberPrefetchingListState(): LazyListState = rememberLazyListState()
