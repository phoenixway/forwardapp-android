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
    var isDragging by mutableStateOf(false); private set
    var draggedIndex by mutableIntStateOf(-1); private set
    var targetIndex by mutableIntStateOf(-1); private set
    var draggedItem by mutableStateOf<T?>(null); private set

    // Акумульований офсет для плавного перетягу
    var dragOffset by mutableStateOf(Offset.Zero); private set

    // --------- Гістерезис/геометрія ----------
    // Скільки всього проїхали по Y від старту жесту
    private var accumulatedDy by mutableStateOf(0f)
    // На якій «фіксованій» позначці по Y востаннє перейшли інший індекс
    private var committedDy by mutableStateOf(0f)
    // Куди рухаємося останнім часом: -1 вгору, +1 вниз, 0 стоїмо
    var lastMoveDir by mutableIntStateOf(0); private set
    // Для лінії вставки: true → зверху, false → знизу
    var insertOnTop by mutableStateOf(true); private set

    // Пер-елементні висоти, щоб рахувати пороги
    private val itemHeights = mutableStateMapOf<Int, Float>()
    // Приблизна кількість елементів (оновлюємо під час вимірювання)
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
            insertOnTop = true
            handler.onDragStart(item, index)
        }
    }

    // Головне: додаємо дельту, і тільки при перетині порога рухаємо targetIndex
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

        // Можемо «переступати» одразу кілька комірок, якщо тягнемо швидко
        while ((accumulatedDy - committedDy) > threshold && newTarget < (totalItems - 1)) {
            newTarget += 1
            committedDy += itemHeight
            insertOnTop = false // рух вниз → вставка під елементом
        }
        while ((committedDy - accumulatedDy) > threshold && newTarget > 0) {
            newTarget -= 1
            committedDy -= itemHeight
            insertOnTop = true // рух вгору → вставка над елементом
        }

        if (newTarget != targetIndex && handler.canDrop(draggedIndex, newTarget)) {
            targetIndex = newTarget
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

    private fun resetState() {
        isDragging = false
        draggedIndex = -1
        targetIndex = -1
        draggedItem = null
        dragOffset = Offset.Zero
        accumulatedDy = 0f
        committedDy = 0f
        lastMoveDir = 0
        insertOnTop = true
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
            // повідомляємо про висоту
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
                        // тепер користуємось новим методом з гістерезисом
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
        onDragStart = {
            state.startDrag(item, index)
        },
        onDragCancel = { state.cancelDrag() },
        onDragEnd = { state.endDrag() },
        onDrag = { change: PointerInputChange, dragAmount: Offset ->
            change.consume() // щоб нічого не з’їло жест
            if (state.isDragging && state.draggedIndex == index) {
                // Гістерезис всередині стану; фракцію можна підкрутити
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
    // handle створюємо як і раніше
    val dragHandleModifier = Modifier.dragHandle(state, item, index)

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val r = coordinates.boundsInParent()
                state.reportItemMeasured(index, r.height) // <- важливо!
            }
            .applyDragVisualEffects(state, index),
    ) {
        // 1) контент завжди малюємо (щоб жест не «випилювався»)
        content(dragHandleModifier)

        // 2) тонка риска-вставка поверх цілі, але НЕ на перетягуваному елементі
        if (state.isDragging && index == state.targetIndex && index != state.draggedIndex) {
            val align = if (state.insertOnTop) Alignment.TopCenter else Alignment.BottomCenter
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .align(align)
                        .fillMaxWidth()
                        .height(2.dp)
                        .graphicsLayer {
                            // невелика «тінь», щоб було видно на будь-якому фоні
                            shadowElevation = 8f
                        }
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}




// 7. LazyColumn modifier for auto-scrolling
// Виправлений autoScrollDragDrop модифікатор у dndEngine.kt

// 7. LazyColumn modifier for auto-scrolling
fun <T> Modifier.autoScrollDragDrop(
    state: DragDropState<T>,
    lazyListState: LazyListState,
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    LaunchedEffect(state.isDragging, state.dragOffset) {
        if (state.isDragging) {
            // Отримуємо інформацію про видимий контент
            val layoutInfo = lazyListState.layoutInfo
            val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
            val scrollThreshold = with(density) { 100.dp.toPx() }

            // Перевіряємо положення перетягуваного елемента відносно viewport
            val draggedItemInfo = layoutInfo.visibleItemsInfo.find {
                it.key == "draggable_${state.draggedItem?.let { (it as? ListItemContent)?.item?.id }}"
            }

            draggedItemInfo?.let { itemInfo ->
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                val draggedPosition = itemCenter + state.dragOffset.y

                when {
                    draggedPosition < scrollThreshold -> {
                        // Прокручуємо вгору
                        scope.launch {
                            lazyListState.animateScrollBy(-100f)
                        }
                    }
                    draggedPosition > (viewportHeight - scrollThreshold) -> {
                        // Прокручуємо вниз
                        scope.launch {
                            lazyListState.animateScrollBy(100f)
                        }
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