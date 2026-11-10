package app.lockon.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable

/*
 * State for scrolling grids and lists that builds the next row a little ahead of the scroll, between
 * frames, so the frame that scrolls it in only has to place it. Android does this by itself. Compose
 * for desktop doesn't yet: every new row was built inside the frame that showed it, which is most of
 * what made scrolling miss frames there, so desktop brings its own way of finding the time
 * (LazyPrefetch.jvm.kt).
 */

@Composable
expect fun rememberPrefetchingGridState(): LazyGridState

@Composable
expect fun rememberPrefetchingListState(): LazyListState
