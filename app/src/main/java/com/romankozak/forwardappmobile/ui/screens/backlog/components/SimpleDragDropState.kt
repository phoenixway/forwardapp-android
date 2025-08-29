package com.romankozak.forwardappmobile.ui.screens.backlog.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SimpleDragDropState(
    private val state: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit
) {
    lateinit var itemsProvider: () -> List<ListItemContent>

    private var draggedDistance by mutableFloatStateOf(0f)

    var draggedItemIndex by mutableIntStateOf(-1)
        private set

    // Додаємо необхідні властивості для сумісності з InteractiveListItem
    var currentDropIndex by mutableIntStateOf(-1)
        private set

    var initialIndexOfDraggedItem by mutableIntStateOf(-1)
        private set

    var targetIndexOfDraggedItem by mutableIntStateOf(-1)
        private set

    private var draggedItemLayoutInfo by mutableStateOf<LazyListItemInfo?>(null)
    private var autoScrollJob: Job? = null

    val isDragging: Boolean get() = draggedItemIndex != -1

    fun onDragStart(item: ListItemContent) {
        if (isDragging) return

        val items = itemsProvider()
        val itemIndex = items.indexOfFirst { it.item.id == item.item.id }
        if (itemIndex == -1) return

        draggedItemIndex = itemIndex
        initialIndexOfDraggedItem = itemIndex
        currentDropIndex = itemIndex
        targetIndexOfDraggedItem = itemIndex
        draggedDistance = 0f

        // Знаходимо layout info для поточного елемента
        draggedItemLayoutInfo = state.layoutInfo.visibleItemsInfo.firstOrNull {
            it.index == itemIndex + 1 // +1 бо є header
        }

        println("DragStart: itemIndex=$itemIndex, layoutInfo=${draggedItemLayoutInfo != null}")
    }

    fun onDrag(dragAmount: Float) {
        if (!isDragging) return

        draggedDistance += dragAmount
        val draggedIndex = draggedItemIndex
        val layoutInfo = draggedItemLayoutInfo ?: return

        val draggedCenter = layoutInfo.offset + layoutInfo.size / 2 + draggedDistance
        val items = itemsProvider()
        val viewportHeight = state.layoutInfo.viewportSize.height

        var newDropIndex = draggedIndex

        // Покращена логіка визначення позиції вставки
        val visibleItems = state.layoutInfo.visibleItemsInfo.filter {
            it.index > 0 // Виключаємо header
        }

        when {
            // Перетягування до початку списку
            draggedCenter < 50 && state.firstVisibleItemIndex > 1 -> {
                newDropIndex = 0
            }

            // Перетягування до кінця списку
            draggedCenter > viewportHeight - 50 -> {
                newDropIndex = items.size
            }

            else -> {
                // Знаходимо найближчий елемент
                var closestDistance = Float.MAX_VALUE
                var closestIndex = draggedIndex

                visibleItems.forEach { visibleItem ->
                    val itemIndex = visibleItem.index - 1 // -1 для header
                    if (itemIndex in 0 until items.size && itemIndex != draggedIndex) {
                        val itemCenter = visibleItem.offset + visibleItem.size / 2f
                        val distance = kotlin.math.abs(draggedCenter - itemCenter)

                        if (distance < closestDistance) {
                            closestDistance = distance
                            closestIndex = itemIndex
                        }
                    }
                }

                // Визначаємо, куди вставляти відносно найближчого елемента
                val closestItem = visibleItems.find { it.index - 1 == closestIndex }
                if (closestItem != null) {
                    val itemCenter = closestItem.offset + closestItem.size / 2f
                    newDropIndex = if (draggedCenter > itemCenter) {
                        // Вставляти після елемента
                        kotlin.math.min(closestIndex + 1, items.size)
                    } else {
                        // Вставляти перед елементом
                        kotlin.math.max(closestIndex, 0)
                    }
                }
            }
        }

        // Обмежуємо індекс
        newDropIndex = newDropIndex.coerceIn(0, items.size)

        if (newDropIndex != currentDropIndex) {
            currentDropIndex = newDropIndex
            targetIndexOfDraggedItem = newDropIndex
            println("Drag: draggedIndex=$draggedIndex, newDropIndex=$newDropIndex")
        }

        handleAutoScroll()
    }

    fun onDragEnd() {
        val fromIndex = draggedItemIndex
        val toIndex = currentDropIndex

        if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
            println("DragEnd: moving from $fromIndex to $toIndex")
            onMove(fromIndex, toIndex)
        }

        reset()
    }

    fun reset() {
        draggedItemIndex = -1
        initialIndexOfDraggedItem = -1
        currentDropIndex = -1
        targetIndexOfDraggedItem = -1
        draggedDistance = 0f
        draggedItemLayoutInfo = null
        autoScrollJob?.cancel()
    }

    private fun handleAutoScroll() {
        if (autoScrollJob?.isActive == true) return

        autoScrollJob = scope.launch {
            while (isDragging) {
                val layoutInfo = draggedItemLayoutInfo ?: break
                val viewportHeight = state.layoutInfo.viewportSize.height
                val itemTop = layoutInfo.offset + draggedDistance
                val itemBottom = itemTop + layoutInfo.size

                val scrollSpeed = when {
                    itemTop < 100 -> -15f
                    itemBottom > viewportHeight - 100 -> 15f
                    else -> 0f
                }

                if (scrollSpeed != 0f) {
                    try {
                        state.scrollBy(scrollSpeed)
                        delay(16) // 60fps
                    } catch (e: Exception) {
                        break
                    }
                } else {
                    delay(50)
                }
            }
        }
    }

    fun getItemOffset(item: ListItemContent): Float {
        if (!isDragging) return 0f

        val items = itemsProvider()
        val itemIndex = items.indexOfFirst { it.item.id == item.item.id }
        val draggedIndex = draggedItemIndex
        val dropIndex = currentDropIndex

        return when {
            // Перетягуваний елемент
            itemIndex == draggedIndex -> draggedDistance

            // Елементи, які потрібно зсунути при русі вниз
            itemIndex > draggedIndex && itemIndex < dropIndex -> {
                -(draggedItemLayoutInfo?.size?.toFloat() ?: 0f)
            }

            // Елементи, які потрібно зсунути при русі вгору
            itemIndex < draggedIndex && itemIndex >= dropIndex -> {
                (draggedItemLayoutInfo?.size?.toFloat() ?: 0f)
            }

            // Всі інші елементи залишаються на місці
            else -> 0f
        }
    }
}