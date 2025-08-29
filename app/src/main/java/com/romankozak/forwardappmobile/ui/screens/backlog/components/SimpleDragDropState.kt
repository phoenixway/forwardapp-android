package com.romankozak.forwardappmobile.ui.screens.backlog.components

import android.util.Log
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


enum class DropPosition { Top, Bottom }
data class DropIndicatorState(val show: Boolean, val position: DropPosition = DropPosition.Bottom)

private fun shouldShowDropIndicator(
    currentIndex: Int,
    draggedIndex: Int,
    targetIndex: Int
): DropIndicatorState {


    if (draggedIndex == -1 || targetIndex == -1) {
        return DropIndicatorState(show = false)
    }

    // Не показуємо індикатор, якщо елемент залишається на тому самому місці
    if (draggedIndex == targetIndex) {
        return DropIndicatorState(show = false)
    }

    return when {
        // Рух вниз: показуємо індикатор там, де буде вставлений елемент
        draggedIndex < targetIndex -> {
            when (currentIndex) {
                targetIndex -> DropIndicatorState(show = true, position = DropPosition.Top)
                else -> DropIndicatorState(show = false)
            }
        }
        // Рух вгору: показуємо індикатор там, де буде вставлений елемент
        draggedIndex > targetIndex -> {
            when (currentIndex) {
                targetIndex -> DropIndicatorState(show = true, position = DropPosition.Top)
                else -> DropIndicatorState(show = false)
            }
        }
        else -> DropIndicatorState(show = false)
    }
}
class SimpleDragDropState(
    private val state: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit
) {
    companion object { private const val TAG = "SimpleDragDropState" }

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

        // Доступ до LazyListItemInfo (з урахуванням header => +1)
        draggedItemLayoutInfo = state.layoutInfo.visibleItemsInfo.firstOrNull {
            it.index == itemIndex + 1
        }

        Log.d(TAG, "DragStart: itemIndex=$itemIndex, layoutInfo=${draggedItemLayoutInfo != null}")
    }

    fun onDrag(dragAmount: Float) {
        if (!isDragging) return

        draggedDistance += dragAmount
        val draggedIndex = draggedItemIndex
        val layoutInfo = draggedItemLayoutInfo ?: return

        val currentItemOffset = layoutInfo.offset + draggedDistance
        val itemSize = layoutInfo.size
        val itemCenter = currentItemOffset + itemSize / 2f

        val items = itemsProvider()
        val viewportHeight = state.layoutInfo.viewportSize.height

        var newDropIndex = draggedIndex

        val visibleItems = state.layoutInfo.visibleItemsInfo.filter { it.index > 0 } // виключаємо header

        when {
            // до початку
            itemCenter < 50 && state.firstVisibleItemIndex > 1 -> {
                newDropIndex = 0
            }

            // до кінця — даємо значення items.size (вставити в кінець)
            itemCenter > viewportHeight - 50 -> {
                newDropIndex = items.size
            }

            else -> {
                newDropIndex = findBestDropIndex(
                    draggedIndex = draggedIndex,
                    itemCenter = itemCenter,
                    itemSize = itemSize,
                    visibleItems = visibleItems,
                    totalItems = items.size
                )
            }
        }

        // Тепер дозволяємо значення в діапазоні 0..items.size (items.size = "в кінець")
        newDropIndex = newDropIndex.coerceIn(0, items.size)

        if (newDropIndex != currentDropIndex) {
            currentDropIndex = newDropIndex
            targetIndexOfDraggedItem = newDropIndex
            Log.d(TAG, "Drag: draggedIndex=$draggedIndex, newDropIndex=$newDropIndex, currentDropIndex=$currentDropIndex, itemCenter=$itemCenter")
        }

        handleAutoScroll()
    }

    private fun findBestDropIndex(
        draggedIndex: Int,
        itemCenter: Float,
        itemSize: Int,
        visibleItems: List<LazyListItemInfo>,
        totalItems: Int
    ): Int {
        Log.d(TAG, "findBestDropIndex: draggedIndex=$draggedIndex, itemCenter=$itemCenter, totalItems=$totalItems")

        var bestDropIndex = draggedIndex
        var lastCheckedIndex = -1

        for (visibleItem in visibleItems.sortedBy { it.offset }) {
            val itemIndex = visibleItem.index - 1 // перетворюємо LazyList-індекс → індекс у списку
            if (itemIndex !in 0 until totalItems || itemIndex == draggedIndex) continue

            val visibleTop = visibleItem.offset.toFloat()
            val visibleBottom = (visibleItem.offset + visibleItem.size).toFloat()
            val visibleCenter = (visibleTop + visibleBottom) / 2f

            lastCheckedIndex = itemIndex

            when {
                itemCenter < visibleTop -> {
                    bestDropIndex = itemIndex
                    break
                }
                itemCenter >= visibleTop && itemCenter < visibleCenter -> {
                    bestDropIndex = itemIndex
                    break
                }
                itemCenter >= visibleCenter && itemCenter <= visibleBottom -> {
                    bestDropIndex = itemIndex + 1
                }
                itemCenter > visibleBottom -> {
                    bestDropIndex = itemIndex + 1
                }
            }
        }

        // Якщо перетягуємо нижче останнього видимого — можемо повернути totalItems (вставка в кінець)
        bestDropIndex = bestDropIndex.coerceIn(0, totalItems)
        Log.d(TAG, "Final bestDropIndex: $bestDropIndex for draggedIndex: $draggedIndex")
        return bestDropIndex
    }

    fun onDragEnd() {
        val fromIndex = draggedItemIndex
        val rawToIndex = currentDropIndex
        val items = itemsProvider()

        Log.d(TAG, "DragEnd: fromIndex=$fromIndex, toIndex=$rawToIndex, totalItems=${items.size}")

        if (fromIndex != -1 && rawToIndex != -1 && fromIndex != rawToIndex) {
            // Не обмежуємо зверху – дозволяємо toIndex == items.size (вставка в кінець)
            val safeToIndex = rawToIndex.coerceIn(0, items.size)

            if (fromIndex != safeToIndex) {
                onMove(fromIndex, safeToIndex)
                Log.d(TAG, "DragEnd: moving from $fromIndex to $safeToIndex (raw toIndex=$rawToIndex)")
            } else {
                Log.d(TAG, "DragEnd: fromIndex == toIndex → нічого не робимо")
            }
        } else {
            Log.d(TAG, "DragEnd: no move needed. fromIndex=$fromIndex, toIndex=$rawToIndex")
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
                        delay(16)

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

        val itemSizeF = (draggedItemLayoutInfo?.size ?: 0).toFloat()

        return when {
            itemIndex == draggedIndex -> draggedDistance

            // рух вниз: зсуваємо елементи між draggedIndex+1 .. (dropIndex-1) вгору
            draggedIndex < dropIndex && itemIndex in (draggedIndex + 1) until dropIndex -> -itemSizeF

            // рух вгору: зсуваємо елементи між dropIndex .. draggedIndex-1 вниз
            draggedIndex > dropIndex && itemIndex in dropIndex until draggedIndex -> itemSizeF

            else -> 0f
        }
    }
}
