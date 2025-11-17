package com.romankozak.forwardappmobile.ui.holdmenu

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val TAG = "HOLDMENU"

@Composable
fun HoldMenuOverlay(
    state: HoldMenuState,
    onStateChange: (HoldMenuState) -> Unit,
    onDismiss: () -> Unit,
    itemHeight: Dp = 40.dp,
) {
    if (!state.isOpen || state.items.isEmpty()) return

    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }

    // Позиція меню на екрані (мінімальний варіант без жорсткого clamping до меж)
    val menuOffset = remember(state.anchor, state.items.size, itemHeightPx) {
        val x = state.anchor.x - with(density) { 120.dp.toPx() }
        val y = state.anchor.y - (state.items.size * itemHeightPx) / 2f
        IntOffset(x.roundToInt(), y.roundToInt())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // overlay "шар"
            .pointerInput(state.isOpen, state.items, state.anchor) {
                awaitEachGesture {
                    // Чекаємо перший дотик у межах overlay (тобто вже по меню)
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var currentIndex: Int? = null

                    fun updateSelection(pos: Offset) {
                        val relY = pos.y - menuOffset.y
                        val index = (relY / itemHeightPx).toInt()
                        if (index in state.items.indices && index != currentIndex) {
                            currentIndex = index
                            Log.e(TAG, "🔄 Highlight index=$index")
                            onStateChange(state.copy(selectedIndex = index))
                        }
                    }

                    // Одразу оновлюємо виділення під першим дотиком
                    updateSelection(down.position)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue

                        if (change.changedToUpIgnoreConsumed()) {
                            // Відпускання пальця → виконуємо вибраний пункт
                            val idx = currentIndex
                            if (idx != null && idx in state.items.indices) {
                                Log.e(TAG, "✔ SELECT: ${state.items[idx].label}")
                                state.items[idx].onClick()
                            }
                            onDismiss()
                            break
                        } else {
                            if (change.pressed) {
                                updateSelection(change.position)
                            } else {
                                // Якийсь інший стан (gesture скасували) → просто закриваємо меню
                                onDismiss()
                                break
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.TopStart,
    ) {
        Column(
            modifier = Modifier
                .offset { menuOffset }
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                )
        ) {
            state.items.forEachIndexed { index, item ->
                val selected = state.selectedIndex == index

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .background(
                            if (selected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else
                                Color.Transparent
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = item.label,
                        color = if (selected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}