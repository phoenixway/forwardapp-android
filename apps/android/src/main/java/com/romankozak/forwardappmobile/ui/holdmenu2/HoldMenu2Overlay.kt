package com.romankozak.forwardappmobile.ui.holdmenu2

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun HoldMenu2Overlay(
    controller: HoldMenu2Controller,
    modifier: Modifier = Modifier
) {
    val state = controller.state

    if (!state.isOpen || state.items.isEmpty()) return

    val density = LocalDensity.current
    val itemH = 44.dp
    val itemHPx = with(density) { itemH.toPx() }

    Box(
        modifier = modifier.pointerInput(state.isOpen) {
            awaitPointerEventScope{
            while (controller.state.isOpen) {
                val event = awaitPointerEvent()

                val press = event.changes.firstOrNull() ?: break

                val menuHeight = itemHPx * state.items.size
                val menuTop = state.anchor.y - menuHeight - 8f
                val relativeY = press.position.y - menuTop

                val hover = (relativeY / itemHPx)
                    .toInt()
                    .coerceIn(0, state.items.lastIndex)

                if (hover != state.hoverIndex) {
                    controller.setHover(hover)
                }

                if (press.changedToUp()) {
                    controller.state.onItemSelected?.invoke(hover)
                    controller.close()
                }
            }
            }
        }
    ) {
        HoldMenu2Popup(state)
    }
}

