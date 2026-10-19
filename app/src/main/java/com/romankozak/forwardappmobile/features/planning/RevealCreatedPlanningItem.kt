package com.romankozak.forwardappmobile.features.planning

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first

/** A committed creation is consumed only after its real rendered key becomes visible. */
@Composable
internal fun <T : Any> RevealCreatedPlanningItem(state: LazyListState, keys: List<T>, target: T?, onRevealed: () -> Unit) {
    LaunchedEffect(target, keys) {
        val index = keys.indexOf(target)
        if (target == null || index < 0) return@LaunchedEffect
        snapshotFlow { state.layoutInfo.totalItemsCount }.first { it == keys.size }
        state.animateScrollToItem(index)
        snapshotFlow { state.layoutInfo.visibleItemsInfo.any { it.key == target } }.first { it }
        com.romankozak.forwardappmobile.StartupTrace.mark("Planning.reveal.visible")
        onRevealed()
    }
}
