// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/backlog/components/dndEngine.kt

package com.romankozak.forwardappmobile.ui.screens.backlog.components

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// 1. Main interface for DnD operations
interface DragDropHandler<T> {
    fun onDragStart(item: T, index: Int)
    fun onDragEnd(fromIndex: Int, toIndex: Int): Boolean
    fun onDragCancel()
    fun canDrag(item: T, index: Int): Boolean = true
    fun canDrop(fromIndex: Int, toIndex: Int): Boolean = true
}

// 2. State for DnD operations
@Stable
class DragDropState<T>(
    private val handler: DragDropHandler<T>,
) {
    var isDragging by mutableStateOf(false)
        private set

    var draggedIndex by mutableIntStateOf(-1)
        private set

    var targetIndex by mutableIntStateOf(-1)
        private set

    var draggedItem by mutableStateOf<T?>(null)
        private set

    var dragOffset by mutableStateOf(Offset.Zero)
        private set

    fun startDrag(item: T, index: Int) {
        if (handler.canDrag(item, index)) {
            draggedItem = item
            draggedIndex = index
            isDragging = true
            handler.onDragStart(item, index)
        }
    }

    fun updateDragPosition(offset: Offset) {
        dragOffset = offset
    }

    fun updateTargetIndex(index: Int) {
        if ((draggedIndex != -1) && handler.canDrop(draggedIndex, index)) {
            targetIndex = index
        }
    }

    fun endDrag(): Boolean {
        val success = if ((draggedIndex != -1) && (targetIndex != -1) && (draggedIndex != targetIndex)) {
            handler.onDragEnd(draggedIndex, targetIndex)
        } else {
            false
        }

        resetState()
        return success
    }

    fun cancelDrag() {
        handler.onDragCancel()
        resetState()
    }

    private fun resetState() {
        isDragging = false
        draggedIndex = -1
        targetIndex = -1
        draggedItem = null
        dragOffset = Offset.Zero
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
    val density = LocalDensity.current
    var itemBounds: Rect by remember { mutableStateOf(Rect.Zero) }

    this
        .onGloballyPositioned { coordinates ->
            itemBounds = coordinates.boundsInParent()
        }
        .pointerInput(item, index) {
            detectDragGestures(
                onDragStart = { _ ->
                    state.startDrag(item, index)
                },
                onDragEnd = {
                    state.endDrag()
                },
                onDrag = { _, dragAmount ->
                    if (state.isDragging && (state.draggedIndex == index)) {
                        state.updateDragPosition(dragAmount)

                        val itemHeight = with(density) { itemBounds.height.toDp() }
                        val draggedDistance = dragAmount.y
                        val estimatedTargetIndex = index + (draggedDistance / itemHeight.toPx()).roundToInt()

                        state.updateTargetIndex(estimatedTargetIndex.coerceAtLeast(0))
                    }
                },
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
        alpha = 0.8f
        scaleX = 1.05f
        scaleY = 1.05f
        translationX = state.dragOffset.x
        translationY = state.dragOffset.y
    }
}

// 5. Modifier for custom drag handle
fun <T> Modifier.dragHandle(
    state: DragDropState<T>,
    item: T,
    index: Int,
): Modifier = this.pointerInput(item, index) {
    detectDragGestures(
        onDragStart = { _ ->
            state.startDrag(item, index)
        },
        onDragEnd = {
            state.endDrag()
        },
        onDrag = { _, dragAmount ->
            if (state.isDragging && (state.draggedIndex == index)) {
                state.updateDragPosition(dragAmount)

                // Additional logic for determining target index
                // can be added here based on the entire list position
            }
        },
    )
}

// 5b. Composable wrapper for elements with drag handle
@Composable
fun <T> DraggableItemContainer(
    state: DragDropState<T>,
    item: T,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable (dragHandleModifier: Modifier) -> Unit,
) {
    var itemBounds: Rect by remember { mutableStateOf(Rect.Zero) }

    // Create modifier for drag handle
    val dragHandleModifier = Modifier.dragHandle(state, item, index)

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                itemBounds = coordinates.boundsInParent()
            }
            .applyDragVisualEffects(state, index),
    ) {
        content(dragHandleModifier)
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
            // Auto-scroll logic
            val scrollThreshold = with(density) { 100.dp.toPx() }
            // Note: size.height is not accessible here, you'll need to pass it as parameter
            // or calculate it differently
            when {
                state.dragOffset.y < scrollThreshold -> {
                    scope.launch {
                        lazyListState.animateScrollBy(-50f)
                    }
                }
                state.dragOffset.y > scrollThreshold -> { // Simplified condition
                    scope.launch {
                        lazyListState.animateScrollBy(50f)
                    }
                }
            }
        }
    }

    this
}

// 8. Example usage
@Composable
fun <T> DraggableLazyColumn(
    items: List<T>,
    onItemMoved: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable LazyItemScope.(item: T, index: Int, isDragging: Boolean) -> Unit,
) {
    val lazyListState = rememberLazyListState()

    val dragDropHandler = remember {
        object : DragDropHandler<T> {
            override fun onDragStart(item: T, index: Int) {
                // Logic for drag start
            }

            override fun onDragEnd(fromIndex: Int, toIndex: Int): Boolean {
                onItemMoved(fromIndex, toIndex)
                return true
            }

            override fun onDragCancel() {
                // Cancel logic
            }
        }
    }

    val dragDropState = rememberDragDropState(dragDropHandler)

    LazyColumn(
        state = lazyListState,
        modifier = modifier.autoScrollDragDrop(dragDropState, lazyListState),
    ) {
        itemsIndexed(items) { index, item ->
            Box(
                modifier = Modifier.draggableItem(
                    state = dragDropState,
                    item = item,
                    index = index,
                ),
            ) {
                itemContent(item, index, dragDropState.isDragging && (dragDropState.draggedIndex == index))
            }
        }
    }
}

// 9. Examples of usage with custom handles

// Example 1: Using dragHandle modifier in your component
@Composable
fun YourCustomListItem(
    item: String,
    index: Int,
    dragDropState: DragDropState<String>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.applyDragVisualEffects(dragDropState, index),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Your content
            Text(
                text = item,
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
            )

            // Your custom drag handle
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag",
                modifier = Modifier
                    .dragHandle(dragDropState, item, index)  // Add this modifier
                    .padding(16.dp)
                    .size(24.dp),
            )
        }
    }
}

// Example 2: Using DraggableItemContainer
@Composable
fun ExampleWithContainer() {
    var items by remember { mutableStateOf(listOf("Item 1", "Item 2", "Item 3")) }

    val dragDropHandler = remember {
        object : DragDropHandler<String> {
            override fun onDragStart(item: String, index: Int) {}
            override fun onDragEnd(fromIndex: Int, toIndex: Int): Boolean {
                items = items.toMutableList().apply {
                    val item = removeAt(fromIndex)
                    add(toIndex, item)
                }
                return true
            }
            override fun onDragCancel() {}
        }
    }

    val dragDropState = rememberDragDropState(dragDropHandler)
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.autoScrollDragDrop(dragDropState, lazyListState),
    ) {
        itemsIndexed(items) { index, item ->
            DraggableItemContainer(
                state = dragDropState,
                item = item,
                index = index,
            ) { dragHandleModifier ->
                // Your custom component
                YourExistingComponent(
                    item = item,
                    modifier = dragHandleModifier, // Pass modifier to your handle
                )
            }
        }
    }
}

// Example 3: Direct usage with existing LazyColumn
@Composable
fun IntegrateWithExistingLazyColumn() {
    var items by remember { mutableStateOf(listOf("Item 1", "Item 2", "Item 3")) }

    val dragDropHandler = remember {
        object : DragDropHandler<String> {
            override fun onDragStart(item: String, index: Int) {}
            override fun onDragEnd(fromIndex: Int, toIndex: Int): Boolean {
                items = items.toMutableList().apply {
                    val item = removeAt(fromIndex)
                    add(toIndex, item)
                }
                return true
            }
            override fun onDragCancel() {}
        }
    }

    val dragDropState = rememberDragDropState(dragDropHandler)
    val lazyListState = rememberLazyListState()

    // Your existing LazyColumn
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.autoScrollDragDrop(dragDropState, lazyListState),
    ) {
        itemsIndexed(items) { index, item ->
            // Your existing item composable
            YourCustomListItem(
                item = item,
                index = index,
                dragDropState = dragDropState,
            )
        }
    }
}

// Example of your component that receives dragHandleModifier
@Composable
fun YourExistingComponent(
    item: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item,
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
            )

            // Your existing handle - just add dragHandleModifier
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Drag Handle",
                modifier = modifier
                    .padding(8.dp)
                    .size(32.dp),
            )
        }
    }
}