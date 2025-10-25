package com.romankozak.forwardappmobile.ui.dnd

import android.util.Log
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "DRAG_CONTAINER_DEBUG"

fun Modifier.dragContainer(
    dragDropManager: DragDropManager,
    lazyListState: LazyListState,
    scope: CoroutineScope,
    setScrollingEnabled: (Boolean) -> Unit
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = true)
        Log.d(TAG, "Down event at ${down.position}")

        setScrollingEnabled(false)
        Log.d(TAG, "Scrolling disabled")

        var scrollJob: Job? = null
        val longPress = awaitLongPressOrCancellation(down.id)

        if (longPress != null) {
            val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { item: LazyListItemInfo ->
                val yPos = longPress.position.y
                val itemY = item.offset.y.toFloat()
                val itemBottom = itemY + item.size.height.toFloat()
                yPos >= itemY && yPos <= itemBottom
            }

            if (itemInfo != null) {
                longPress.consume()
                Log.d(TAG, "Long press detected on item ${itemInfo.index}!")

                val initialItemOffset = itemInfo.offset.y
                val dragOffsetInItem = longPress.position.y - initialItemOffset

                dragDropManager.onDragStart(
                    offset = longPress.position,
                    index = itemInfo.index,
                    initialItemOffset = initialItemOffset,
                    dragOffsetInItem = dragOffsetInItem
                )

                try {
                    drag(longPress.id) { change ->
                        change.consume()
                        dragDropManager.onDrag(change.position)

                        // Autoscroll logic
                        scrollJob?.cancel()
                        val containerHeight = size.height
                        val pointerY = change.position.y
                        val topThreshold = containerHeight * 0.1f
                        val bottomThreshold = containerHeight * 0.9f

                        when {
                            pointerY < topThreshold -> {
                                scrollJob = scope.launch {
                                    while (true) {
                                        lazyListState.scrollBy(-20f)
                                        delay(16)
                                    }
                                }
                            }
                            pointerY > bottomThreshold -> {
                                scrollJob = scope.launch {
                                    while (true) {
                                        lazyListState.scrollBy(20f)
                                        delay(16)
                                    }
                                }
                            }
                            else -> {
                                scrollJob?.cancel()
                            }
                        }
                    }
                } finally {
                    Log.d(TAG, "Drag finished.")
                    scrollJob?.cancel()
                    dragDropManager.onDragEnd()
                    setScrollingEnabled(true)
                    Log.d(TAG, "Scrolling re-enabled.")
                }
            } else {
                setScrollingEnabled(true)
                Log.d(TAG, "Long press not on an item, scrolling enabled.")
            }
        } else {
            setScrollingEnabled(true)
            Log.d(TAG, "Long press cancelled, scrolling enabled.")
        }
    }
}
