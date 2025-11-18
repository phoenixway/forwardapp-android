package com.romankozak.forwardappmobile.ui.holdmenu

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HoldMenuOverlay(
    state: HoldMenuState,
    onChangeState: (HoldMenuState) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isOpen || state.items.isEmpty()) {
        return
    }

    val density = LocalDensity.current
    val menuWidth = 220.dp
    val itemH = 48.dp

    // Позиції кожного елемента меню
    val itemPositions = remember { mutableStateMapOf<Int, Pair<Offset, IntSize>>() }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.items) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: continue
                        val position = change.position

                        Log.e("HOLDMENU", "👆 Touch at $position")

                        // Знаходимо елемент під пальцем
                        val hoveredIndex = itemPositions.entries.firstOrNull { (_, posSize) ->
                            val (topLeft, size) = posSize
                            position.x >= topLeft.x &&
                                    position.x <= topLeft.x + size.width &&
                                    position.y >= topLeft.y &&
                                    position.y <= topLeft.y + size.height
                        }?.key

                        if (hoveredIndex != selectedIndex) {
                            selectedIndex = hoveredIndex
                            Log.e("HOLDMENU", "🎯 Selected: $hoveredIndex")
                        }

                        // Відпустив палець - вибираємо елемент
                        if (!change.pressed) {
                            Log.e("HOLDMENU", "✅ Released on item: $selectedIndex")
                            selectedIndex?.let { index ->
                                state.onItemSelected?.invoke(index)
                            }
                            onChangeState(state.copy(isOpen = false))
                            break
                        }

                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.TopStart
    ) {
        val menuWidthPx = with(density) { menuWidth.toPx() }
        val menuHeightPx = with(density) { (itemH * state.items.size).toPx() }

        val desiredX = state.anchor.x - menuWidthPx / 2f
        val desiredY = state.anchor.y - menuHeightPx - 16f

        val offsetX = desiredX.toInt().coerceAtLeast(8)
        val offsetY = desiredY.toInt().coerceAtLeast(8)

        Column(
            modifier = Modifier
                .offset { IntOffset(offsetX, offsetY) }
                .width(menuWidth)
                .background(Color(0xFF2A2A2A), RoundedCornerShape(16.dp))
                .padding(vertical = 8.dp)
        ) {
            state.items.forEachIndexed { index, label ->
                val isSelected = selectedIndex == index
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1f,
                    label = "item_scale_$index"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemH)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInWindow()
                            itemPositions[index] = pos to coords.size
                        }
                        .scale(scale)
                        .background(
                            if (isSelected) Color(0xFF3A3A3A) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFFCCCCCC),
                        fontSize = if (isSelected) 16.sp else 15.sp,
                    )
                }
            }
        }
    }
}