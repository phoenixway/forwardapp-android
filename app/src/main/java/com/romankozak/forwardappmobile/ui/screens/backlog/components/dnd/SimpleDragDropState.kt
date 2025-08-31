package com.romankozak.forwardappmobile.ui.screens.backlog.components.dnd

import android.os.SystemClock
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

const val TAG = "DND_DEBUG"
private const val ENABLE_AUTOSCROLL_LOGS = true
private const val TAG_AUTOSCROLL = "DragAutoScroll"

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
        if (autoScrollJob?.isActive == true) {
            if (ENABLE_AUTOSCROLL_LOGS) {
                Log.d(TAG_AUTOSCROLL, "handleAutoScroll(): already running -> return")
            }
            return
        }

        autoScrollJob = scope.launch {
            if (ENABLE_AUTOSCROLL_LOGS) {
                Log.d(TAG_AUTOSCROLL, "autoScrollJob START: isDragging=$isDragging")
            }

            var iteration = 0

            try {
                while (isDragging) {
                    iteration++

                    val dragged = draggedItemLayoutInfo
                    if (dragged == null) {
                        if (ENABLE_AUTOSCROLL_LOGS) {
                            Log.d(TAG_AUTOSCROLL, "[$iteration] draggedItemLayoutInfo == null -> break")
                        }
                        break
                    }

                    val layoutInfo = state.layoutInfo
                    val viewportHeight = layoutInfo.viewportSize.height.toFloat()
                    val scrollThreshold = 120f
                    val itemCenter = dragged.offset + draggedDistance + (dragged.size / 2f)

                    val firstIdx = state.firstVisibleItemIndex
                    val firstOffset = state.firstVisibleItemScrollOffset
                    val firstVisibleIdx = layoutInfo.visibleItemsInfo.firstOrNull()?.index
                    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
                    val lastIdx = lastVisible?.index
                    val total = layoutInfo.totalItemsCount
                    val canBack = state.canScrollBackward
                    val canFwd = state.canScrollForward

                    if (ENABLE_AUTOSCROLL_LOGS) {
                        Log.d(
                            TAG_AUTOSCROLL,
                            "[$iteration] snapshot: " +
                                    "center=$itemCenter, thr=$scrollThreshold, viewportH=$viewportHeight, draggedDist=$draggedDistance | " +
                                    "firstIdx=$firstIdx off=$firstOffset vis=[$firstVisibleIdx..$lastIdx] total=$total | " +
                                    "canBack=$canBack canFwd=$canFwd"
                        )
                    }

                    var needsToScroll = false
                    var decision = "NONE"

                    // --- ВГОРУ ---
                    if (itemCenter < scrollThreshold) {
                        if (firstIdx > 0) {
                            needsToScroll = true
                            decision = "UP"
                            val target = firstIdx - 1
                            if (ENABLE_AUTOSCROLL_LOGS) {
                                Log.d(TAG_AUTOSCROLL, "[$iteration] ↑ request animateScrollToItem($target) from firstIdx=$firstIdx")
                            }
                            // Виконуємо без додаткового launch, щоб можна було заміряти час тут же
                            val t0 = SystemClock.uptimeMillis()
                            runCatching { state.animateScrollToItem(target) }
                                .onSuccess {
                                    if (ENABLE_AUTOSCROLL_LOGS) {
                                        Log.d(TAG_AUTOSCROLL, "[$iteration] ↑ animateScrollToItem($target) OK in ${SystemClock.uptimeMillis() - t0} ms")
                                    }
                                }
                                .onFailure { e ->
                                    Log.e(TAG_AUTOSCROLL, "[$iteration] ↑ animateScrollToItem($target) FAILED", e)
                                }
                        } else {
                            if (ENABLE_AUTOSCROLL_LOGS) {
                                Log.d(TAG_AUTOSCROLL, "[$iteration] ↑ blocked: firstIdx=$firstIdx (top as per state)")
                            }
                        }
                    }
                    // --- ВНИЗ ---
                    else if (itemCenter > viewportHeight - scrollThreshold) {
                        if (lastIdx != null && lastIdx < total - 1) {
                            needsToScroll = true
                            decision = "DOWN"
                            val target = lastIdx + 1
                            if (ENABLE_AUTOSCROLL_LOGS) {
                                Log.d(TAG_AUTOSCROLL, "[$iteration] ↓ request animateScrollToItem($target) from lastIdx=$lastIdx")
                            }
                            val t0 = SystemClock.uptimeMillis()
                            runCatching { state.animateScrollToItem(target) }
                                .onSuccess {
                                    if (ENABLE_AUTOSCROLL_LOGS) {
                                        Log.d(TAG_AUTOSCROLL, "[$iteration] ↓ animateScrollToItem($target) OK in ${SystemClock.uptimeMillis() - t0} ms")
                                    }
                                }
                                .onFailure { e ->
                                    Log.e(TAG_AUTOSCROLL, "[$iteration] ↓ animateScrollToItem($target) FAILED", e)
                                }
                        } else {
                            if (ENABLE_AUTOSCROLL_LOGS) {
                                Log.d(TAG_AUTOSCROLL, "[$iteration] ↓ blocked: lastIdx=$lastIdx total=$total")
                            }
                        }
                    } else {
                        if (ENABLE_AUTOSCROLL_LOGS) {
                            Log.d(TAG_AUTOSCROLL, "[$iteration] no-scroll: center within safe zone")
                        }
                    }

                    // Контроль швидкості автоскролу
                    if (needsToScroll) {
                        if (ENABLE_AUTOSCROLL_LOGS) {
                            Log.d(TAG_AUTOSCROLL, "[$iteration] decision=$decision -> delay(400)")
                        }
                        delay(400)
                    } else {
                        delay(50)
                    }
                }
            } finally {
                if (ENABLE_AUTOSCROLL_LOGS) {
                    Log.d(TAG_AUTOSCROLL, "autoScrollJob END: isDragging=$isDragging")
                }
            }
        }.also { job ->
            if (ENABLE_AUTOSCROLL_LOGS) {
                job.invokeOnCompletion { e ->
                    if (e == null) {
                        Log.d(TAG_AUTOSCROLL, "autoScrollJob completed normally")
                    } else {
                        Log.e(TAG_AUTOSCROLL, "autoScrollJob completed with error", e)
                    }
                }
            }
        }
    }


    // Допоміжні функції для надійної перевірки можливості скролу
    private fun canScrollUpward(): Boolean {
        return state.firstVisibleItemIndex > 0 ||
                state.firstVisibleItemScrollOffset > 0
    }

    private fun canScrollDownward(): Boolean {
        val layoutInfo = state.layoutInfo
        val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
        return lastVisibleItem != null &&
                lastVisibleItem.index < layoutInfo.totalItemsCount - 1
    }

    private suspend fun performUpwardScroll() {
        try {
            // Спочатку пробуємо точний скрол
            val currentOffset = state.firstVisibleItemScrollOffset
            if (currentOffset > 0) {
                // Якщо є offset, скролимо в межах поточного елемента
                val newOffset = maxOf(0, currentOffset - 50)
                state.scrollToItem(state.firstVisibleItemIndex, newOffset)
            } else {
                // Інакше переходимо до попереднього елемента
                val newIndex = maxOf(0, state.firstVisibleItemIndex - 1)
                state.animateScrollToItem(newIndex)
            }
        } catch (e: Exception) {
            // Fallback на випадок помилки
            val newIndex = maxOf(0, state.firstVisibleItemIndex - 1)
            state.scrollToItem(newIndex)
        }
    }

    private suspend fun performDownwardScroll() {
        try {
            val layoutInfo = state.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()

            if (lastVisibleItem != null) {
                val newIndex = minOf(
                    layoutInfo.totalItemsCount - 1,
                    lastVisibleItem.index + 1
                )
                state.animateScrollToItem(newIndex)
            }
        } catch (e: Exception) {
            // Fallback
            val currentIndex = state.firstVisibleItemIndex
            state.scrollToItem(currentIndex + 1)
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