package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.romankozak.forwardappmobile.BuildConfig
import com.romankozak.forwardappmobile.diagnostics.BacklogAddTrace
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect

/** Layout evidence, not just list data availability. No scrolling or UI mutation. */
@Composable
internal fun BacklogAddVisibilityTrace(state: LazyListState) {
    if (!BuildConfig.DEBUG) return
    LaunchedEffect(state) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.map { it.key } }
            .distinctUntilChanged().collect { keys ->
                BacklogAddTrace.targetObserved(null, "ui.visible") { placementId, _ -> placementId != null && placementId in keys }
            }
    }
}
