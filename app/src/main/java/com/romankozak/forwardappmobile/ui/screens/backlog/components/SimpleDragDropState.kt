package com.romankozak.forwardappmobile.ui.screens.backlog.components

import android.util.Log
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "DND_DEBUG"

// --- Утиліти для індикатора скидання (перенесено зі старого файлу) ---

enum class DropPosition { Top, Bottom }
data class DropIndicatorState(val show: Boolean, val position: DropPosition = DropPosition.Bottom)

fun shouldShowDropIndicator(
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


@Stable
class SimpleDragDropState(
    private val state: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit,
) {
    private var draggedDistance by mutableFloatStateOf(0f)
    private var draggedItemLayoutInfo by mutableStateOf<LazyListItemInfo?>(null)
    private var autoScrollJob: Job? = null // Додано для автопрокрутки

    var initialIndexOfDraggedItem by mutableIntStateOf(-1)
        private set

    var draggedItemIndex by mutableIntStateOf(-1)
        private set

    var targetIndexOfDraggedItem by mutableIntStateOf(-1)
        private set

    val isDragging: Boolean
        get() = draggedItemIndex != -1

    fun onDragStart(item: ListItemContent) {
        if (isDragging) return // Запобігаємо початку нового перетягування, якщо воно вже триває

        val itemInfo = state.layoutInfo.visibleItemsInfo.find { it.key == item.item.id }
        if (itemInfo != null) {
            // Перевіряємо, чи є вже активне перетягування, і скидаємо його, якщо потрібно
            if (isDragging) {
                reset()
            }
            draggedItemLayoutInfo = itemInfo
            initialIndexOfDraggedItem = itemInfo.index
            draggedItemIndex = itemInfo.index
            targetIndexOfDraggedItem = itemInfo.index
            Log.d(TAG, "▶️ [onDragStart] START. ItemId: ${item.item.id}, Index: ${itemInfo.index}")
        } else {
            Log.w(TAG, "[onDragStart] WARNING: Could not find layout info for dragged item: ${item.item.id}")
        }
    }

    fun onDrag(dragAmount: Float) {
        if (!isDragging) return

        draggedDistance += dragAmount

        val draggedItem = draggedItemLayoutInfo
        if (draggedItem == null) {
            Log.w(TAG, "[onDrag] SKIPPING: draggedItemLayoutInfo is null.")
            return
        }

        val startOffset = draggedItem.offset + draggedDistance
        val endOffset = startOffset + draggedItem.size

        val hoveredItem = state.layoutInfo.visibleItemsInfo.find { item ->
            // Не порівнюємо елемент сам із собою
            if (item.index == initialIndexOfDraggedItem) return@find false

            val delta = (startOffset + endOffset) / 2f
            val itemStart = item.offset
            val itemEnd = item.offset + item.size

            delta.toInt() in itemStart..itemEnd
        }

        if (hoveredItem != null && hoveredItem.index != targetIndexOfDraggedItem) {
            targetIndexOfDraggedItem = hoveredItem.index
            Log.d(TAG, "[onDrag] New target index: ${hoveredItem.index}")
        }

        // Запускаємо логіку автопрокрутки
        handleAutoScroll()
    }

    fun onDragEnd() {
        Log.d(TAG, "⏹️ [onDragEnd] END. ===========================")
        Log.d(TAG, "[onDragEnd] State before move: fromIndex=$initialIndexOfDraggedItem, toIndex(raw)=$targetIndexOfDraggedItem, initialIndex=$initialIndexOfDraggedItem, totalItems=${state.layoutInfo.totalItemsCount}")

        val fromIndex = initialIndexOfDraggedItem
        val toIndex = targetIndexOfDraggedItem

        if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
            Log.i(TAG, "[onDragEnd] Executing move: $fromIndex -> $toIndex")
            onMove(fromIndex, toIndex)
        } else {
            Log.d(TAG, "[onDragEnd] SKIPPED: No change in position or invalid state.")
        }

        reset()
        Log.d(TAG, "[onDragEnd] ===================================")
    }

    fun reset() {
        Log.d(TAG, "[reset] Resetting drag state.")
        draggedDistance = 0f
        draggedItemLayoutInfo = null
        initialIndexOfDraggedItem = -1
        draggedItemIndex = -1
        targetIndexOfDraggedItem = -1
        autoScrollJob?.cancel() // Зупиняємо автопрокрутку
        autoScrollJob = null
    }

    // --- Логіка автопрокрутки (перенесено зі старого файлу) ---
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
                    state.scrollBy(scrollSpeed)
                    delay(16) // ~60fps
                } else {
                    // Якщо не скролимо, можна збільшити затримку
                    delay(50)
                }
            }
        }
    }

    // --- Покращена логіка зміщення ---
    fun getItemOffset(item: ListItemContent): Float {
        if (!isDragging) return 0f

        val itemInfo = state.layoutInfo.visibleItemsInfo.find { it.key == item.item.id }
        val itemIndex = itemInfo?.index ?: return 0f

        // Використовуємо initialIndexOfDraggedItem для стабільного "початкового" індексу.
        val draggedIndex = initialIndexOfDraggedItem
        val targetIndex = targetIndexOfDraggedItem

        if (draggedIndex == -1 || targetIndex == -1) return 0f

        // Розмір елемента, що перетягується.
        val draggedItemSize = (draggedItemLayoutInfo?.size ?: 0).toFloat()

        val offset = when {
            // Елемент, що перетягується, слідує за пальцем.
            itemIndex == draggedIndex -> draggedDistance

            // --- Перетягування ВНИЗ ---
            // Елементи між початковою та новою позиціями повинні зсунутися ВГОРУ.
            // Ми включаємо `targetIndex` в діапазон, щоб елемент, над яким ми знаходимось,
            // також зсунувся, звільняючи місце.
            draggedIndex < targetIndex && itemIndex in (draggedIndex + 1)..targetIndex -> -draggedItemSize

            // --- Перетягування ВГОРУ ---
            // Елементи між новою та початковою позиціями повинні зсунутися ВНИЗ.
            draggedIndex > targetIndex && itemIndex in targetIndex until draggedIndex -> draggedItemSize

            // Інші елементи не зсуваються.
            else -> 0f
        }

        if (offset != 0f) {
            Log.v(TAG, "  [getItemOffset] ItemIndex: $itemIndex, DraggedIndex: $draggedIndex, TargetIndex: $targetIndex -> Applying offset: $offset")
        }

        return offset
    }
}