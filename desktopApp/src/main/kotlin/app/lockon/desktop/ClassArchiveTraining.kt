package app.lockon.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import app.lockon.ui.AppState
import app.lockon.ui.Tab
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * The run packaging records the class archive from (`trainClassArchive` in build.gradle.kts): it
 * waits for the boot screen to go, visits every tab and closes. The archive holds the classes that
 * loaded, already parsed and checked, so installed copies start without loading them one by one.
 * Recording makes the app run several times slower, hence the long waits.
 */
@Composable
fun ClassArchiveTraining(state: AppState, exit: () -> Unit) {
    LaunchedEffect(Unit) {
        snapshotFlow { state.booted }.first { it }
        delay(2_000)
        for (tab in Tab.entries + Tab.entries.first()) {
            state.select(tab)
            delay(2_500)
        }
        exit()
    }
}
