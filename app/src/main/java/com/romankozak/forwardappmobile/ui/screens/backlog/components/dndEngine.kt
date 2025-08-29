// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/backlog/components/dndEngine.kt

package com.romankozak.forwardappmobile.ui.screens.backlog.components

import android.R.attr.translationZ
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

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
class DragDropState<T> constructor(
    private val handler: DragDropHandler<T>,
) {
    var isDragging by mutableStateOf(false); private set
    var draggedIndex by mutableIntStateOf(-1); private set
    var targetIndex by mutableIntStateOf(-1); private set
    var draggedItem by mutableStateOf<T?>(null); private set
    var dragOffset by mutableStateOf(Offset.Zero); private set

    private var accumulatedDy by mutableFloatStateOf(0f)
    private var committedDy by mutableFloatStateOf(0f)
    var lastMoveDir by mutableIntStateOf(0); private set

    private val itemHeights = mutableStateMapOf<Int, Float>()
    var totalItems by mutableIntStateOf(0); private set

    fun reportItemMeasured(index: Int, heightPx: Float) {
        if (heightPx > 0f) {
            itemHeights[index] = heightPx
        }
        if (index + 1 > totalItems) {
            totalItems = index + 1
        }
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
        startIndexFallback: Int = draggedIndex,
    ) {
        dragOffset += delta
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

        if (newTarget != targetIndex && handler.canDrop(draggedIndex, newTarget)) {
            targetIndex = newTarget
        }
    }

    fun endDrag(): Boolean {
        val success = if (draggedIndex != -1 && targetIndex != -1 && draggedIndex != targetIndex) {
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
        accumulatedDy = 0f
        committedDy = 0f
        lastMoveDir = 0
        itemHeights.clear()
        totalItems = 0
    }
}

// 3. Composable to create state
@Composable
fun <T> rememberDragDropState(handler: DragDropHandler<T>): DragDropState<T> {
    return remember(handler) { DragDropState(handler) }
}

private fun Modifier.draggedItemVisuals() = this.then(
    Modifier.graphicsLayer {
        alpha = 0.8f
        scaleX = 1.05f
        scaleY = 1.05f
        shadowElevation = 16f
        // translationZ замінено на підхід, що працює з Compose
    },
)

// 4. Visual effects for dragging
private fun <T> Modifier.applyDragVisualEffects(
    state: DragDropState<T>,
    index: Int,
): Modifier = this.then(
    if (state.isDragging && state.draggedIndex == index) {
        Modifier.graphicsLayer {
            alpha = 0.8f
            scaleX = 1.05f
            scaleY = 1.05f
            shadowElevation = 16f
        }
    } else {
        Modifier
    }
)

// 5. Drag handle modifier
fun <T> Modifier.dragHandle(
    state: DragDropState<T>,
    item: T,
    index: Int,
): Modifier = this.pointerInput(item, index) {
    detectDragGestures(
        onDragStart = {
            state.startDrag(item, index)
            // Вібрація прибрана, бо не можна викликати @Composable тут
        },
        onDragCancel = { state.cancelDrag() },
        onDragEnd = { state.endDrag() },
        onDrag = { change, dragAmount ->
            change.consume()
            if (state.isDragging && state.draggedIndex == index) {
                state.applyDragDelta(dragAmount, 0.35f)
            }
        },
    )
}

// 6. Drop indicator
// Покращений DropIndicator з анімацією
@Composable
private fun DropIndicator(isValidDrop: Boolean) {
    val color = if (isValidDrop) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val infiniteTransition = rememberInfiniteTransition(label = "dropIndicatorPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dropIndicatorAlpha"
    )
    val height by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dropIndicatorHeight"
    )
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(isValidDrop) {
        haptic.performHapticFeedback(
            if (isValidDrop) HapticFeedbackType.LongPress
            else HapticFeedbackType.TextHandleMove
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .padding(horizontal = 12.dp)
            .shadow(4.dp, shape = MaterialTheme.shapes.medium)
            .border(0.5.dp, color.copy(alpha = alpha * 0.5f), MaterialTheme.shapes.medium)
            .semantics {
                contentDescription = if (isValidDrop) "Дозволена зона для скидання"
                else "Недозволена зона для скидання"
            }
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        color.copy(alpha = alpha),
                        Color.Transparent,
                    ),
                ),
            ),
    ) {
        if (!isValidDrop) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Недозволена зона",
                tint = color.copy(alpha = alpha),
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.Center)
            )
        }
    }
}


// 7. Container with drag handle support
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
                state.reportItemMeasured(index, coordinates.size.height.toFloat())
            }
            .applyDragVisualEffects(state, index),
    ) {
        content(dragHandleModifier)

        if (state.isDragging && index == state.targetIndex && index != state.draggedIndex) {
            val isDraggingDown = state.draggedIndex < state.targetIndex
            val align = if (isDraggingDown) Alignment.BottomCenter else Alignment.TopCenter

            Box(modifier = Modifier.align(align)) {
                DropIndicator(true)
            }
        }
    }
}

// 8. Auto-scroll modifier
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

            val viewportStart = 0
            val viewportEnd = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
            val scrollThreshold = with(density) { 65.dp.toPx() }

            val draggedItemInfo = layoutInfo.visibleItemsInfo.find { it.index == state.draggedIndex }
            if (draggedItemInfo != null) {
                val itemTop = draggedItemInfo.offset
                val fingerPosition = itemTop + state.dragOffset.y

                when {
                    fingerPosition < viewportStart + scrollThreshold -> {
                        scope.launch { lazyListState.animateScrollBy(-50f) }
                    }
                    fingerPosition > viewportEnd - scrollThreshold -> {
                        scope.launch { lazyListState.animateScrollBy(50f) }
                    }
                }
            }
        }
    }

    this
}

// 9. DraggableLazyColumn
@Composable
fun <T> DraggableLazyColumn(
    items: List<T>,
    onItemMoved: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    showHint: Boolean = false,
    itemContent: @Composable LazyItemScope.(item: T, index: Int, isDragging: Boolean) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val dragDropHandler = remember {
        object : DragDropHandler<T> {
            override fun onDragStart(item: T, index: Int) {}
            override fun onDragEnd(fromIndex: Int, toIndex: Int): Boolean {
                onItemMoved(fromIndex, toIndex)
                return true
            }
            override fun onDragCancel() {}
        }
    }

    val dragDropState = rememberDragDropState(dragDropHandler)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.autoScrollDragDrop(dragDropState, lazyListState),
        ) {
            items(
                items = items,
                key = { item -> items.indexOf(item) }, // або краще: { item.id } якщо є
                contentType = { "item" }
            ) { item ->
                val index = items.indexOf(item)
                itemContent(
                    item,
                    index,
                    dragDropState.isDragging && dragDropState.draggedIndex == index
                )
            }
        }

        // Optional hint
        if (showHint && !dragDropState.isDragging) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {},
            ) {
                Text(
                    text = "Перетягніть хендл, щоб змінити порядок",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(0.9f),
                            shape = MaterialTheme.shapes.medium,
                        )
                        .padding(12.dp)
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}

// 10. Example usage with handle
@Composable
fun YourCustomListItem(
    item: String,
    index: Int,
    dragDropState: DragDropState<String>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Text(
                text = item,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )

            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                modifier = Modifier
                    .dragHandle(dragDropState, item, index)
                    .padding(12.dp)
                    .size(20.dp),
            )
        }
    }
}

// 11. Example screen
@Composable
fun ExampleWithContainer() {
    var items by rememberSaveable { mutableStateOf(listOf("Завдання 1", "Завдання 2", "Завдання 3")) }

    val dragDropHandler = remember {
        object : DragDropHandler<String> {
            override fun onDragStart(item: String, index: Int) {}
            override fun onDragEnd(fromIndex: Int, toIndex: Int): Boolean {
                items = items.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }
                return true
            }
            override fun onDragCancel() {}
        }
    }

    val dragDropState = rememberDragDropState(dragDropHandler)

    DraggableLazyColumn(
        items = items,
        onItemMoved = { from, to ->
            items = items.toMutableList().apply {
                add(to, removeAt(from))
            }
        },
        showHint = true,
    ) { item, index, isDragging ->
        DraggableItemContainer(
            state = dragDropState,
            item = item,
            index = index,
        ) { dragHandleModifier ->
            YourCustomListItem(
                item = item,
                index = index,
                dragDropState = dragDropState,
            )
        }
    }
}