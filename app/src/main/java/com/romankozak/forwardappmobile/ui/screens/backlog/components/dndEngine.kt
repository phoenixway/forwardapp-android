// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/backlog/components/dndEngine.kt

package com.romankozak.forwardappmobile.ui.screens.backlog.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import kotlinx.coroutines.launch

// 1. Main interface for DnD operations
interface DragDropHandler<T> {
    fun onDragStart(item: T, index: Int)
    fun onDragEnd(fromIndex: Int, toIndex: Int): Boolean
    fun onDragCancel()
    fun onDragOver(fromIndex: Int, toIndex: Int) // <-- НОВИЙ МЕТОД для тактильного відгуку
    fun canDrag(item: T, index: Int): Boolean = true
    fun canDrop(fromIndex: Int, toIndex: Int): Boolean = true
}

// 2. State for DnD operations
@Stable
class DragDropState<T>(
    private val handler: DragDropHandler<T>,
) {
    var isDragging by mutableStateOf(false); private set
    var draggedIndex by mutableIntStateOf(-1); private set
    var targetIndex by mutableIntStateOf(-1); private set
    var draggedItem by mutableStateOf<T?>(null); private set

    var dragOffset by mutableStateOf(Offset.Zero); private set

    private var accumulatedDy by mutableStateOf(0f)
    private var committedDy by mutableStateOf(0f)
    var lastMoveDir by mutableIntStateOf(0); private set

    private val itemHeights = mutableStateMapOf<Int, Float>()
    var totalItems by mutableIntStateOf(0); private set

    fun reportItemMeasured(index: Int, heightPx: Float) {
        if (heightPx > 0f) itemHeights[index] = heightPx
        if (index + 1 > totalItems) totalItems = index + 1
    }
    private fun heightFor(index: Int): Float =
        itemHeights[index] ?: itemHeights[draggedIndex] ?: 1f

    fun startDrag(item: T, index: Int) {
        if (handler.canDrag(item, index)) {
            draggedItem = item
            draggedIndex = index
            targetIndex = index
            isDragging = true
            dragOffset = Offset.Zero
            accumulatedDy = 0f
            committedDy = 0f
            lastMoveDir = 0
            handler.onDragStart(item, index)
        }
    }

    fun applyDragDelta(
        delta: Offset,
        hysteresisFraction: Float = 0.35f,
        startIndexFallback: Int = draggedIndex
    ) {
        dragOffset = dragOffset + delta
        accumulatedDy += delta.y
        lastMoveDir = when {
            delta.y > 0f -> 1
            delta.y < 0f -> -1
            else -> lastMoveDir
        }

        val itemHeight = heightFor(draggedIndex).coerceAtLeast(1f)
        val threshold = itemHeight * hysteresisFraction

        var newTarget = if (targetIndex >= 0) targetIndex else startIndexFallback

        while ((accumulatedDy - committedDy) > threshold && newTarget < (totalItems - 1)) {
            newTarget += 1
            committedDy += itemHeight
        }
        while ((committedDy - accumulatedDy) > threshold && newTarget > 0) {
            newTarget -= 1
            committedDy -= itemHeight
        }

        if (newTarget != targetIndex) {
            if (handler.canDrop(draggedIndex, newTarget)) {
                targetIndex = newTarget
            }
            // Викликаємо onDragOver незалежно від canDrop, щоб дати фідбек
            handler.onDragOver(draggedIndex, newTarget)
        }
    }

    fun endDrag(): Boolean {
        val success = if (draggedIndex != -1 && targetIndex != -1 && draggedIndex != targetIndex) {
            handler.onDragEnd(draggedIndex, targetIndex)
        } else false
        resetState()
        return success
    }

    fun cancelDrag() {
        handler.onDragCancel()
        resetState()
    }

    // <-- НОВИЙ ПУБЛІЧНИЙ МЕТОД
    fun canDropAt(targetIndex: Int): Boolean {
        if (draggedIndex < 0) return false
        return handler.canDrop(draggedIndex, targetIndex)
    }

    private fun resetState() {
        isDragging = false
        draggedIndex = -1
        targetIndex = -1
        draggedItem = null
        dragOffset = Offset.Zero
        accumulatedDy = 0f
        committedDy = 0f
        lastMoveDir = 0
        itemHeights.clear()
        totalItems = 0
    }
}


// 3. Composable function to create state
@Composable
fun <T> rememberDragDropState(
    handler: DragDropHandler<T>,
): DragDropState<T> {
    return remember(handler) { DragDropState(handler) }
}

// 4. Modifier for LazyColumn elements (basic - without handle)
fun <T> Modifier.draggableItem(
    state: DragDropState<T>,
    item: T,
    index: Int,
): Modifier = composed {
    var itemBounds: Rect by remember { mutableStateOf(Rect.Zero) }

    this
        .onGloballyPositioned { coordinates ->
            val r = coordinates.boundsInParent()
            itemBounds = r
            state.reportItemMeasured(index, r.height)
        }
        .pointerInput(item, index) {
            detectDragGestures(
                onDragStart = {
                    state.startDrag(item, index)
                },
                onDragCancel = {
                    state.cancelDrag()
                },
                onDragEnd = {
                    state.endDrag()
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    if (state.isDragging && state.draggedIndex == index) {
                        state.applyDragDelta(
                            delta = dragAmount,
                            hysteresisFraction = 0.35f,
                            startIndexFallback = index
                        )
                    }
                }
            )
        }
        .applyDragVisualEffects(state, index)
}


// 4c. Helper function for visual effects
private fun <T> Modifier.applyDragVisualEffects(
    state: DragDropState<T>,
    index: Int,
): Modifier = graphicsLayer {
    if (state.isDragging && (state.draggedIndex == index)) {
        alpha = 0.5f
        scaleX = 1.02f
        scaleY = 1.02f
        rotationZ = -5f // <-- Додано легкий нахил
    }
}

// 5. Modifier for custom drag handle
fun <T> Modifier.dragHandle(
    state: DragDropState<T>,
    item: T,
    index: Int,
): Modifier = this.pointerInput(item, index) {
    detectDragGestures(
        onDragStart = {
            state.startDrag(item, index)
        },
        onDragCancel = { state.cancelDrag() },
        onDragEnd = { state.endDrag() },
        onDrag = { change: PointerInputChange, dragAmount: Offset ->
            change.consume()
            if (state.isDragging && state.draggedIndex == index) {
                state.applyDragDelta(delta = dragAmount, hysteresisFraction = 0.35f)
            }
        },
    )
}



@Composable
fun <T> DraggableItemContainer(
    state: DragDropState<T>,
    item: T,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable (dragHandleModifier: Modifier) -> Unit,
) {
    val dragHandleModifier = Modifier.dragHandle(state, item, index)

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val r = coordinates.boundsInParent()
                state.reportItemMeasured(index, r.height)
            }
            .applyDragVisualEffects(state, index),
    ) {
        content(dragHandleModifier)

        if (state.isDragging && index == state.targetIndex && index != state.draggedIndex) {
            val canDrop = state.canDropAt(state.targetIndex)
            val indicatorColor = if (canDrop) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

            val isDraggingDown = state.draggedIndex < state.targetIndex
            val align = if (isDraggingDown) Alignment.BottomCenter else Alignment.TopCenter

            Box(
                modifier = Modifier
                    .align(align)
                    .fillMaxWidth()
                    .height(2.dp)
                    .graphicsLayer {
                        shadowElevation = 8f
                    }
                    .background(indicatorColor)
            )
        }
    }
}

// 7. LazyColumn modifier for auto-scrolling
fun <T> Modifier.autoScrollDragDrop(
    state: DragDropState<T>,
    lazyListState: LazyListState,
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    LaunchedEffect(state.isDragging, state.dragOffset) {
        if (state.isDragging) {
            val layoutInfo = lazyListState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@LaunchedEffect

            val viewportTop = 0
            val viewportBottom = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
            val scrollThreshold = with(density) { 65.dp.toPx() }

            val draggedItemInfo = layoutInfo.visibleItemsInfo.find { it.index == state.draggedIndex }

            if (draggedItemInfo != null) {
                val itemTopInViewport = draggedItemInfo.offset

                val fingerOffsetOnItem = state.dragOffset.y

                val fingerPositionInViewport = itemTopInViewport + fingerOffsetOnItem

                when {
                    fingerPositionInViewport < (viewportTop + scrollThreshold) -> {
                        scope.launch { lazyListState.animateScrollBy(-50f) }
                    }
                    fingerPositionInViewport > (viewportBottom - scrollThreshold) -> {
                        scope.launch { lazyListState.animateScrollBy(50f) }
                    }
                }
            }
        }
    }

    this
}
// ... (решта файлу з прикладами залишається без змін)