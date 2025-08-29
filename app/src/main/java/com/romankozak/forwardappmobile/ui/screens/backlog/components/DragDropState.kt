package com.romankozak.forwardappmobile.ui.screens.backlog.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// КЛЮЧОВА ЗМІНА: ПОВНІСТЮ ЗАМІНІТЬ ЦЕЙ КЛАС
class DragDropState(
    private val state: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (ListItemContent, ListItemContent) -> Unit,
    private val itemsProvider: () -> List<ListItemContent>
) {
    private var draggedDistance by mutableStateOf(0f)
    var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)
        private set
    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set
    private var overscrollJob by mutableStateOf<Job?>(null)

    val isDragging: Boolean get() = initiallyDraggedElement != null
    private val currentItems: List<ListItemContent> get() = itemsProvider()

    // Властивості, потрібні для InteractiveListItem
    val initialDraggedItemIndex: Int? get() = initiallyDraggedElement?.index
    val draggedItem: ListItemContent? get() = currentIndexOfDraggedItem?.let { currentItems.getOrNull(it) }
    fun getItemIndex(item: ListItemContent): Int = currentItems.indexOf(item)

    fun onDragStart(item: ListItemContent) {
        if (isDragging) return

        val index = currentItems.indexOf(item)
        if (index == -1) return

        currentIndexOfDraggedItem = index
        initiallyDraggedElement = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    }

    fun onDrag(offset: Float) {
        if (!isDragging) return
        draggedDistance += offset

        val initialElement = initiallyDraggedElement ?: return
        val currentDraggedIndex = currentIndexOfDraggedItem ?: return

        val draggedItemCenter = initialElement.offset + draggedDistance + (initialElement.size / 2)

        val targetItemInfo = state.layoutInfo.visibleItemsInfo.find {
            it.index != currentDraggedIndex &&
                    draggedItemCenter in (it.offset.toFloat()..(it.offset + it.size).toFloat())
        }

        if (targetItemInfo != null) {
            val fromIndex = currentIndexOfDraggedItem ?: return
            val toIndex = targetItemInfo.index

            if (fromIndex != toIndex) {
                val fromItem = currentItems.getOrNull(fromIndex)
                val toItem = currentItems.getOrNull(toIndex)
                if (fromItem != null && toItem != null) {
                    onMove(fromItem, toItem)
                    // Оновлюємо лише індекс, не чіпаючи інші змінні стану
                    currentIndexOfDraggedItem = toIndex
                }
            }
        }

        if (overscrollJob?.isActive != true) {
            checkForOverscroll()
        }
    }

    fun onDragEnd() {
        // Повне скидання всього стану після завершення перетягування
        draggedDistance = 0f
        initiallyDraggedElement = null
        currentIndexOfDraggedItem = null
        overscrollJob?.cancel()
    }

    fun getOffset(item: ListItemContent): Float {
        if (!isDragging) return 0f

        val initialIndex = initialDraggedItemIndex ?: return 0f
        val currentIndex = currentIndexOfDraggedItem ?: return 0f
        val itemIndex = getItemIndex(item)

        val draggedItemSize = initiallyDraggedElement?.size?.toFloat() ?: 0f

        return when {
            // Це елемент, який ми тягнемо. Його зміщення залежить від початкової позиції та накопиченої дистанції.
            itemIndex == initialIndex -> draggedDistance

            // Елемент, який був між старою та новою позицією, "виштовхується"
            (itemIndex > initialIndex && itemIndex <= currentIndex) -> -draggedItemSize // Зсув вгору
            (itemIndex < initialIndex && itemIndex >= currentIndex) -> draggedItemSize // Зсув вниз

            // Інші елементи не зачіпаються
            else -> 0f
        }
    }

    fun canDrag(item: ListItemContent): Boolean {
        return !isDragging
    }

    private fun checkForOverscroll() {
        if (overscrollJob?.isActive == true) return
        overscrollJob = scope.launch {
            while (isDragging) {
                val initialElement = initiallyDraggedElement ?: break
                val viewportHeight = state.layoutInfo.viewportSize.height
                val draggedItemTop = initialElement.offset + draggedDistance
                val draggedItemBottom = draggedItemTop + initialElement.size

                val scrollAmount = when {
                    draggedItemBottom > viewportHeight - 200 -> 20f
                    draggedItemTop < 200 -> -20f
                    else -> 0f
                }

                if (scrollAmount != 0f) {
                    state.scrollBy(scrollAmount)
                }
                delay(16)
            }
        }
    }
}