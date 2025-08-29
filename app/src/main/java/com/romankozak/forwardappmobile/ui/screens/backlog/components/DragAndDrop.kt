// --- File: com/romankozak/forwardappmobile/ui/components/DragAndDrop.kt ---
package com.romankozak.forwardappmobile.ui.components

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ... (DragDropState and rememberDragDropState are unchanged)
class DragDropState<T>(
    val lazyListState: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (from: T, to: T) -> Unit,
) {
    var isDragging by mutableStateOf(false)
        private set
    var draggedItem by mutableStateOf<T?>(null)
        private set
    var draggedItemKey by mutableStateOf<Any?>(null)
        private set
    var draggedItemOffset by mutableStateOf(Offset.Zero)
        private set
    var pointerPosition by mutableStateOf(Offset.Zero)
        private set
    var targetItem by mutableStateOf<T?>(null)

    fun onDragStart(item: T, key: Any) {
        isDragging = true
        draggedItem = item
        draggedItemKey = key
    }

    fun onDrag(offset: Offset) {
        draggedItemOffset += offset
    }

    fun onPointerMove(position: Offset) {
        Log.d("DnD_Debug", "onPointerMove: Storing pointerPosition = $position")

        pointerPosition = position
    }

    fun onDragEnd() {
        val from = draggedItem
        val to = targetItem
        if (from != null && to != null && from != to) {
            onMove(from, to)
        }
        reset()
    }

    fun onDragCanceled() {
        reset()
    }

    /**
     * Запускає логіку автопрокрутки, якщо курсор знаходиться біля краю контейнера.
     */
    fun considerAutoScroll(containerCoordinates: LayoutCoordinates?, scrollThresholdPx: Float) {
        if (containerCoordinates == null || !isDragging) return
        val pointerY = pointerPosition.y
        val containerBounds = containerCoordinates.boundsInWindow()
        val scrollAmount = when {
            pointerY < containerBounds.top + scrollThresholdPx ->
                -(scrollThresholdPx - (pointerY - containerBounds.top)) * 0.3f
            pointerY > containerBounds.bottom - scrollThresholdPx ->
                ((pointerY - containerBounds.bottom) + scrollThresholdPx) * 0.3f
            else -> 0f
        }
        if (scrollAmount != 0f) {
            scope.launch {
                lazyListState.scrollBy(scrollAmount)
            }
        }
    }

    private fun reset() {
        isDragging = false
        draggedItem = null
        draggedItemKey = null
        draggedItemOffset = Offset.Zero
        pointerPosition = Offset.Zero
        targetItem = null
    }
}

@Composable
fun <T> rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (from: T, to: T) -> Unit,
): DragDropState<T> {
    val scope = rememberCoroutineScope()
    return remember(lazyListState, scope, onMove) {
        DragDropState(lazyListState, scope, onMove)
    }
}

/**
 * Composable-обгортка для елемента, який можна перетягувати.
 * Вона обробляє жести та оновлює стан.
 */
@Composable
fun <T> DraggableItem(
    dragDropState: DragDropState<T>,
    item: T,
    key: Any,
    modifier: Modifier = Modifier,
    // MODIFIED: The signature is changed to pass a drag handle modifier
    content: @Composable BoxScope.(isDragging: Boolean, dragHandleModifier: Modifier) -> Unit,
) {
    val isDragging = key == dragDropState.draggedItemKey
    val elevation by animateFloatAsState(if (isDragging) 8f else 0f, label = "elevation")

    // Create the drag gesture modifier here
    val dragHandleModifier = Modifier.pointerInput(dragDropState, item, key) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                dragDropState.onDragStart(item, key)
            },
            onDrag = { change, dragAmount ->
                change.consume()
                dragDropState.onDrag(dragAmount)
                dragDropState.onPointerMove(change.position)
            },
            onDragEnd = {
                dragDropState.onDragEnd()
            },
            onDragCancel = {
                dragDropState.onDragCanceled()
            },
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = if (isDragging) dragDropState.draggedItemOffset.y else 0f
                scaleX = if (isDragging) 1.05f else 1f
                scaleY = if (isDragging) 1.05f else 1f
                alpha = if (isDragging) 0.9f else 1f
                shadowElevation = elevation
            }
            .onGloballyPositioned { layoutCoordinates ->
                if (key != dragDropState.draggedItemKey) {
                    val pointerPos = dragDropState.pointerPosition
                    if (dragDropState.isDragging &&
                        layoutCoordinates.boundsInWindow().contains(pointerPos)
                    ) {
                        dragDropState.targetItem = item
                    }
                }
            }
        // REMOVED: The pointerInput modifier is no longer here
    ) {
        // Pass the created modifier to the content
        content(isDragging, dragHandleModifier)
    }
}


// ... (Modifier.autoScroll is unchanged)
fun Modifier.autoScroll(dragDropState: DragDropState<*>): Modifier = composed {
    var containerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val density = LocalDensity.current
    val scrollThresholdPx = remember(density) { with(density) { 80.dp.toPx() } }

    LaunchedEffect(dragDropState.isDragging) {
        if (dragDropState.isDragging) {
            while (dragDropState.isDragging) {
                dragDropState.considerAutoScroll(containerCoordinates, scrollThresholdPx)
                delay(16) // Перевірка кожного кадру (~60 fps)
            }
        }
    }
    onGloballyPositioned {
        containerCoordinates = it
    }
}