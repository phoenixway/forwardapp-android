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
    companion object { private const val TAG = "DND_DEBUG" }

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
        if (itemIndex == -1) {
            Log.e(TAG, "[onDragStart] FAILED: Item with id=${item.item.id} not found in the list.")
            return
        }

        Log.d(TAG, "▶️ [onDragStart] START. ItemId: ${item.item.id}, Index: $itemIndex")

        draggedItemIndex = itemIndex
        initialIndexOfDraggedItem = itemIndex
        currentDropIndex = itemIndex
        targetIndexOfDraggedItem = itemIndex
        draggedDistance = 0f

        draggedItemLayoutInfo = state.layoutInfo.visibleItemsInfo.firstOrNull {
            it.index == itemIndex + 1
        }

        if (draggedItemLayoutInfo == null) {
            Log.w(TAG, "[onDragStart] WARNING: Could not find layout info for dragged item at index $itemIndex.")
        } else {
            Log.d(TAG, "[onDragStart] Layout info FOUND. Offset=${draggedItemLayoutInfo?.offset}, Size=${draggedItemLayoutInfo?.size}")
        }
    }

    fun onDrag(dragAmount: Float) {
        if (!isDragging) return

        draggedDistance += dragAmount
        val draggedIndex = draggedItemIndex
        val layoutInfo = draggedItemLayoutInfo ?: run {
            Log.w(TAG, "[onDrag] SKIPPING: draggedItemLayoutInfo is null.")
            return
        }

        val currentItemOffset = layoutInfo.offset + draggedDistance
        val itemSize = layoutInfo.size
        val itemCenter = currentItemOffset + itemSize / 2f

        val items = itemsProvider()
        val viewportHeight = state.layoutInfo.viewportSize.height

        val newDropIndex = findBestDropIndex(
            draggedIndex = draggedIndex,
            itemCenter = itemCenter,
            visibleItems = state.layoutInfo.visibleItemsInfo.filter { it.index > 0 } // виключаємо header
        ).coerceIn(0, items.size)

        if (newDropIndex != currentDropIndex) {
            Log.i(TAG, "[onDrag] DROP TARGET CHANGED: from $currentDropIndex to $newDropIndex. (Item Center: $itemCenter)")
            currentDropIndex = newDropIndex
            targetIndexOfDraggedItem = newDropIndex
        }

        handleAutoScroll()
    }

    private fun findBestDropIndex(
        draggedIndex: Int,
        itemCenter: Float,
        visibleItems: List<LazyListItemInfo>
    ): Int {
        val items = itemsProvider()
        val totalItems = items.size
        Log.d(TAG, "  [findBestDropIndex] Searching... DraggedIndex: $draggedIndex, ItemCenter: $itemCenter, TotalItems: $totalItems")

        // Знаходимо найближчий видимий елемент, центр якого знаходиться після центру перетягуваного елемента
        val firstItemAfter = visibleItems
            .filter { (it.index - 1) != draggedIndex } // Виключаємо сам перетягуваний елемент
            .minByOrNull {
                val visibleCenter = it.offset + it.size / 2f
                // Шукаємо мінімальну позитивну різницю
                val diff = visibleCenter - itemCenter
                if (diff > 0) diff else Float.MAX_VALUE
            }

        if (firstItemAfter == null) {
            // Якщо таких елементів немає, значить ми в кінці списку
            Log.d(TAG, "  [findBestDropIndex] No item found after. Assuming end of list. Returning $totalItems")
            return totalItems
        }

        val targetIndex = firstItemAfter.index - 1 // Конвертуємо в індекс списку
        val targetCenter = firstItemAfter.offset + firstItemAfter.size / 2f

        // Якщо центр перетягуваного елемента знаходиться до центру цільового - вставляємо перед ним
        val bestDropIndex = if (itemCenter < targetCenter) {
            targetIndex
        } else {
            // Інакше - вставляємо після нього
            targetIndex + 1
        }

        Log.d(TAG, "  [findBestDropIndex] Found best drop index: $bestDropIndex. Target item index was $targetIndex.")
        return bestDropIndex
    }


// SimpleDragDropState.kt

    fun onDragEnd() {
        Log.d(TAG, "⏹️ [onDragEnd] END. ===========================")
        val fromIndex = draggedItemIndex
        val toIndexRaw = currentDropIndex
        val items = itemsProvider()

        Log.d(TAG, "[onDragEnd] State before move: fromIndex=$fromIndex, toIndex(raw)=$toIndexRaw, initialIndex=$initialIndexOfDraggedItem, totalItems=${items.size}")

        if (fromIndex != -1 && toIndexRaw != -1 && fromIndex != toIndexRaw) {
            // Передаємо "сирий" індекс без жодних корекцій. ViewModel розбереться.
            Log.i(TAG, "[onDragEnd]EXECUTING onMove(from=$fromIndex, to=$toIndexRaw)")
            onMove(fromIndex, toIndexRaw)
        } else {
            Log.d(TAG, "[onDragEnd] SKIPPED: No change in position or invalid state.")
        }
        Log.d(TAG, "[onDragEnd] ===================================")
        reset()
    }


    fun reset() {
        Log.d(TAG, "[reset] Resetting drag state.")
        draggedItemIndex = -1
        initialIndexOfDraggedItem = -1
        currentDropIndex = -1
        targetIndexOfDraggedItem = -1
        draggedDistance = 0f
        draggedItemLayoutInfo = null
        autoScrollJob?.cancel()
        autoScrollJob = null
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
                    Log.d(TAG, "  [AutoScroll] Scrolling by ${"%.2f".format(scrollSpeed)}")
                    try {
                        state.scrollBy(scrollSpeed)
                        delay(16) // ~60fps

                        // Оновлюємо layoutInfo ПІСЛЯ прокрутки, це важливо
                        draggedItemLayoutInfo = state.layoutInfo.visibleItemsInfo.firstOrNull {
                            it.index == (draggedItemIndex + 1)
                        }
                        if (draggedItemLayoutInfo == null) {
                            Log.w(TAG, "  [AutoScroll] Lost layout info for dragged item after scroll.")
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "  [AutoScroll] Exception during scroll: $e")
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

        // Використовуємо скоригований toIndex для візуалізації
        val toIndexRaw = currentDropIndex
        val dropIndex = if (toIndexRaw > draggedIndex) toIndexRaw -1 else toIndexRaw

        val itemSizeF = (draggedItemLayoutInfo?.size ?: 0).toFloat()

        val offset = when {
            // Сам елемент, що перетягується
            itemIndex == draggedIndex -> draggedDistance

            // Елемент рухається вниз: елементи між старою та новою позицією зсуваються ВГОРУ
            draggedIndex < dropIndex && itemIndex in (draggedIndex + 1)..dropIndex -> -itemSizeF

            // Елемент рухається вгору: елементи між новою та старою позицією зсуваються ВНИЗ
            draggedIndex > dropIndex && itemIndex in dropIndex until draggedIndex -> itemSizeF

            else -> 0f
        }

        if (offset != 0f) {
            Log.v(TAG, "  [getItemOffset] ItemIndex: $itemIndex, DraggedIndex: $draggedIndex, DropIndex: $dropIndex -> Applying offset: $offset")
        }

        return offset
    }
}