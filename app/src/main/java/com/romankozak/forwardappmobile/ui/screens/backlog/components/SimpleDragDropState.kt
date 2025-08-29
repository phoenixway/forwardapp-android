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

        // Оновлюємо позицію перетягуваного елемента відносно viewport
        val currentItemOffset = layoutInfo.offset + draggedDistance
        val itemSize = layoutInfo.size
        val itemCenter = currentItemOffset + itemSize / 2f

        val items = itemsProvider()
        val viewportHeight = state.layoutInfo.viewportSize.height

        var newDropIndex = draggedIndex

        // Покращена логіка визначення позиції вставки
        val visibleItems = state.layoutInfo.visibleItemsInfo.filter {
            it.index > 0 // Виключаємо header
        }

        when {
            // Перетягування до початку списку
            itemCenter < 50 && state.firstVisibleItemIndex > 1 -> {
                newDropIndex = 0
            }

            // Перетягування до кінця списку
            itemCenter > viewportHeight - 50 -> {
                newDropIndex = items.size - 1
            }

            else -> {
                // Знаходимо найкращу позицію для вставки
                newDropIndex = findBestDropIndex(
                    draggedIndex = draggedIndex,
                    itemCenter = itemCenter,
                    itemSize = itemSize,
                    visibleItems = visibleItems,
                    totalItems = items.size
                )
            }
        }

        // Обмежуємо індекс
        newDropIndex = newDropIndex.coerceIn(0, items.size - 1)

        if (newDropIndex != currentDropIndex) {
            currentDropIndex = newDropIndex
            targetIndexOfDraggedItem = newDropIndex
            println("Drag: draggedIndex=$draggedIndex, newDropIndex=$newDropIndex, itemCenter=$itemCenter")
        }

        handleAutoScroll()
    }

    private fun findBestDropIndex(
        draggedIndex: Int,
        itemCenter: Float,
        itemSize: Int,
        visibleItems: List<LazyListItemInfo>,
        totalItems: Int, // Added missing comma here
    ): Int {
        var bestDropIndex = draggedIndex
        for (visibleItem in visibleItems) {
            val itemIndex = visibleItem.index - 1 // -1 for header
            if (itemIndex !in 0 until totalItems || itemIndex == draggedIndex) continue

            val visibleItemCenter = (visibleItem.offset + (visibleItem.size / 2f)) // Properly clarified
            val visibleItemTop = visibleItem.offset.toFloat()
            val visibleItemBottom = (visibleItem.offset + visibleItem.size).toFloat() // Ensure it's Float

            // Improved logic for position determination
            when {
                // If the center of the dragged item is in the upper half of the visible item
                itemCenter in visibleItemTop..(visibleItemCenter - (itemSize.toFloat() / 4f)) -> {
                    bestDropIndex = itemIndex
                    break
                }
                // If the center of the dragged item is in the lower half of the visible item
                itemCenter in (visibleItemCenter + (itemSize.toFloat() / 4f))..visibleItemBottom -> {
                    bestDropIndex = minOf(itemIndex + 1, totalItems - 1)
                    break
                }
                // Special logic for dragging down
                itemCenter > visibleItemBottom -> {
                    bestDropIndex = minOf(itemIndex + 1, totalItems - 1)
                }
            }
        }
        return bestDropIndex
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

                // Покращена логіка автопрокрутки
                val scrollThreshold = 120f
                val maxScrollSpeed = 20f

                val scrollSpeed = when {
                    itemTop < scrollThreshold -> {
                        val proximity = (scrollThreshold - itemTop) / scrollThreshold
                        -maxScrollSpeed * proximity
                    }
                    itemBottom > viewportHeight - scrollThreshold -> {
                        val proximity = (itemBottom - (viewportHeight - scrollThreshold)) / scrollThreshold
                        maxScrollSpeed * proximity
                    }
                    else -> 0f
                }

                if (scrollSpeed != 0f) {
                    try {
                        state.scrollBy(scrollSpeed)
                        delay(16) // 60fps

                        // Оновлюємо layout info після прокрутки
                        val items = itemsProvider()
                        val draggedIndex = draggedItemIndex
                        if (draggedIndex in 0 until items.size) {
                            draggedItemLayoutInfo = state.layoutInfo.visibleItemsInfo.firstOrNull {
                                it.index == draggedIndex + 1
                            }
                        }
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
            draggedIndex < dropIndex && itemIndex in (draggedIndex + 1)..dropIndex -> {
                -(draggedItemLayoutInfo?.size?.toFloat() ?: 0f)
            }

            // Елементи, які потрібно зсунути при русі вгору
            draggedIndex > dropIndex && itemIndex in dropIndex until draggedIndex -> {
                (draggedItemLayoutInfo?.size?.toFloat() ?: 0f)
            }

            // Всі інші елементи залишаються на місці
            else -> 0f
        }
    }
}