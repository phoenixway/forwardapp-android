package com.romankozak.forwardappmobile.features.activitytracker

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

internal enum class JournalLiveEntryVisibility {
    UNKNOWN,
    VISIBLE,
    HIDDEN,
}

internal data class JournalVisibleItemBounds(
    val key: Any,
    val offset: Int,
    val size: Int,
)

private data class JournalViewportSnapshot(
    val viewportStart: Int,
    val viewportEnd: Int,
    val visibleItems: List<JournalVisibleItemBounds>,
)

private const val ENTER_VISIBLE_FRACTION = 0.45f
private const val LEAVE_VISIBLE_FRACTION = 0.20f

internal fun resolveLiveEntryVisibility(
    activeKey: Any?,
    knownItemKeys: List<Any>,
    visibleItems: List<JournalVisibleItemBounds>,
    viewportStart: Int,
    viewportEnd: Int,
    previous: JournalLiveEntryVisibility,
): JournalLiveEntryVisibility {
    if (activeKey == null) return JournalLiveEntryVisibility.UNKNOWN
    if (activeKey !in knownItemKeys) return JournalLiveEntryVisibility.HIDDEN
    if (viewportEnd <= viewportStart) return JournalLiveEntryVisibility.UNKNOWN

    val activeItem = visibleItems.firstOrNull { it.key == activeKey } ?: return JournalLiveEntryVisibility.HIDDEN
    if (activeItem.size <= 0) return previous

    val visibleStart = maxOf(activeItem.offset, viewportStart)
    val visibleEnd = minOf(activeItem.offset + activeItem.size, viewportEnd)
    val visibleFraction = ((visibleEnd - visibleStart).coerceAtLeast(0)).toFloat() / activeItem.size
    val threshold =
        if (previous == JournalLiveEntryVisibility.VISIBLE) {
            LEAVE_VISIBLE_FRACTION
        } else {
            ENTER_VISIBLE_FRACTION
        }
    return if (visibleFraction >= threshold) {
        JournalLiveEntryVisibility.VISIBLE
    } else {
        JournalLiveEntryVisibility.HIDDEN
    }
}

@Composable
internal fun ObserveLiveEntryVisibility(
    lazyListState: LazyListState,
    activeKey: Any?,
    knownItemKeys: List<Any>,
    onVisibilityChanged: (JournalLiveEntryVisibility) -> Unit,
) {
    val currentCallback by rememberUpdatedState(onVisibilityChanged)
    LaunchedEffect(lazyListState, activeKey, knownItemKeys) {
        var previous = JournalLiveEntryVisibility.UNKNOWN
        snapshotFlow {
            val layoutInfo = lazyListState.layoutInfo
            JournalViewportSnapshot(
                viewportStart = layoutInfo.viewportStartOffset,
                viewportEnd = layoutInfo.viewportEndOffset,
                visibleItems =
                    layoutInfo.visibleItemsInfo.map { item ->
                        JournalVisibleItemBounds(item.key, item.offset, item.size)
                    },
            )
        }.collect { viewport ->
            val current =
                resolveLiveEntryVisibility(
                    activeKey = activeKey,
                    knownItemKeys = knownItemKeys,
                    visibleItems = viewport.visibleItems,
                    viewportStart = viewport.viewportStart,
                    viewportEnd = viewport.viewportEnd,
                    previous = previous,
                )
            if (current != previous) {
                previous = current
                currentCallback(current)
            }
        }
    }
}

@Composable
internal fun ScrollToLiveEntryEffect(
    lazyListState: LazyListState,
    activeKey: Any?,
    knownItemKeys: List<Any>,
    request: Int,
) {
    var handledRequest by remember { mutableIntStateOf(0) }
    LaunchedEffect(lazyListState, request, activeKey, knownItemKeys) {
        if (request == 0 || request <= handledRequest) return@LaunchedEffect
        val activeIndex = knownItemKeys.indexOf(activeKey)
        if (activeIndex >= 0) {
            lazyListState.animateScrollToItem(activeIndex)
            handledRequest = request
        }
    }
}
