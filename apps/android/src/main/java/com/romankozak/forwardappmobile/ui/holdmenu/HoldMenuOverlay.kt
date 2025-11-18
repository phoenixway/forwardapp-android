package com.romankozak.forwardappmobile.ui.holdmenu

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.isActive

@Composable
fun HoldMenuOverlay(
    state: HoldMenuState,
    onChangeState: (HoldMenuState) -> Unit,
    modifier: Modifier = Modifier

) {
    if (!state.isOpen || state.items.isEmpty()) {
        Log.e("HOLDMENU", "❌ Nothing to draw, closed or empty")
        return
    }

    Log.e("HOLDMENU", "📡 Overlay ACTIVE, anchor=${state.anchor}, items=${state.items.size}")

    val density = LocalDensity.current
    val itemHeightPx = with(density) { 44.dp.toPx() }

    // FULLSCREEN overlay capturing all input
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.isOpen) {
                if (!state.isOpen) return@pointerInput

                val menuHeight = itemHeightPx * state.items.size
                val menuTop = state.anchor.y - menuHeight - 8f
                awaitPointerEventScope{
                while (true) {
                    val event = awaitPointerEvent()   // <— тепер працює 100%

                    val press = event.changes.firstOrNull { it.pressed }
                        ?: break

                    val relativeY = press.position.y - menuTop
                    val hoverIndex = (relativeY / itemHeightPx)
                        .toInt()
                        .coerceIn(0, state.items.lastIndex)

                    if (hoverIndex != state.hoverIndex) {
                        onChangeState(state.copy(hoverIndex = hoverIndex))
                    }

                    if (press.changedToUp()) {
                        state.onItemSelected?.invoke(hoverIndex)
                        onChangeState(state.copy(isOpen = false, hoverIndex = -1))
                        break
                    }

                }
                }
            }
    ) {
        MenuPopup(state)
    }
}
