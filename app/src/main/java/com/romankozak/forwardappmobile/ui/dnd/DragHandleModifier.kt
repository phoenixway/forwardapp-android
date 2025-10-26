package com.romankozak.forwardappmobile.ui.dnd

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlinx.coroutines.CoroutineScope

fun Modifier.dragHandle(
    dragDropManager: DragDropManager,
    itemIndex: Int,
    lazyListState: LazyListState,
    scope: CoroutineScope,
    onDragStateChanged: (Boolean) -> Unit
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
                        val initialItemOffset = itemInfo.offset
                        val dragOffsetInItem = (it + positionInRoot).y - initialItemOffset
                        val itemHeight = itemInfo.size.toFloat()
                        dragDropManager.onDragStart(
                            offset = it + positionInRoot,
                            index = itemInfo.index,
                            initialItemOffset = initialItemOffset,
                            dragOffsetInItem = dragOffsetInItem,
                            itemHeight = itemHeight
                        )
                    }
                },
                onDragEnd = {
                    dragDropManager.onDragEnd()
                    onDragStateChanged(false)
                },
                onDragCancel = {
                    dragDropManager.onDragEnd()
                    onDragStateChanged(false)
                },
                onDrag = { change, dragAmount ->
                    dragDropManager.onDrag(change.position + positionInRoot)
                    change.consume()
                }
            )
        }
}