package com.romankozak.forwardappmobile.ui.dnd

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlinx.coroutines.CoroutineScope

fun Modifier.dragHandle(
    state: ReorderableState,
    itemIndex: Int,
    lazyListState: LazyListState,
    scope: CoroutineScope,
    onDragStateChanged: (Boolean) -> Unit,
    lazyColumnPosition: androidx.compose.ui.geometry.Offset
): Modifier {
    var positionInRoot = androidx.compose.ui.geometry.Offset.Zero
    return this
        .onGloballyPositioned { layoutCoordinates ->
            positionInRoot = layoutCoordinates.positionInRoot()
        }
        .pointerInput(itemIndex) {
            detectDragGestures(
                onDragStart = {
                    onDragStateChanged(true)
                    val itemInfo = lazyListState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.index == itemIndex }
                    if (itemInfo != null) {
                        val itemAbsoluteY = lazyColumnPosition.y + itemInfo.offset
                        val fingerAbsoluteY = (it + positionInRoot).y
                        val dragOffsetInItem = fingerAbsoluteY - itemAbsoluteY
                        val itemHeight = itemInfo.size.toFloat()
                        state.onDragStart(
                            offset = it + positionInRoot,
                            index = itemInfo.index,
                            initialItemOffset = itemInfo.offset,
                            dragOffsetInItem = dragOffsetInItem,
                            itemHeight = itemHeight
                        )
                    }
                },
                onDragEnd = {
                    state.onDragEnd()
                    onDragStateChanged(false)
                },
                onDragCancel = {
                    state.onDragEnd()
                    onDragStateChanged(false)
                },
                onDrag = { change, dragAmount ->
                    state.onDrag(change.position + positionInRoot)
                    change.consume()
                }
            )
        }
}