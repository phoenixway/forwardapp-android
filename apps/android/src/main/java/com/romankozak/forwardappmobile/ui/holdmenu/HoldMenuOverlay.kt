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
    onChangeState: (HoldMenuState) -> Unit
) {
    // ❌ Нема чого малювати
    if (!state.isOpen || state.items.isEmpty()) {
        Log.e("HOLDMENU", "❌ Nothing to draw, closed or empty")
        return
    }

    val density = LocalDensity.current
    val itemHeightDp = 44.dp
    val itemHeightPx = with(density) { itemHeightDp.toPx() }

    Log.e("HOLDMENU", "📡 Overlay ACTIVE, anchor=${state.anchor}, items=${state.items.size}")

    Box(
        modifier = Modifier
            .fillMaxSize()
            // тач поза меню — закриває меню
            .pointerInput(Unit) {
                if (!state.isOpen) return@pointerInput
                awaitPointerEventScope {
                    val event = awaitPointerEvent()
                    val anyPressed = event.changes.any { it.pressed }
                    if (!anyPressed) onChangeState(state.copy(isOpen = false, hoverIndex = -1))
                }
            }
    ) {

        MenuPopup(
            state = state,
            itemHeightPx = itemHeightPx,
            onChangeState = onChangeState
        )
    }
}
