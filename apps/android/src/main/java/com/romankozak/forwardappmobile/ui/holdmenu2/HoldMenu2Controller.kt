package com.romankozak.forwardappmobile.ui.holdmenu2

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset

@Stable
class HoldMenu2Controller {
    var state by mutableStateOf(HoldMenu2State())
        private set

    fun open(
        anchor: Offset,
        touch: Offset,
        items: List<String>,
        onSelect: (Int) -> Unit
    ) {
        Log.e("HOLDMENU2", "📝 Controller.open() called, items=$items")
        state = state.copy(
            isOpen = true,
            anchor = anchor,
            touch = touch,
            items = items,
            onItemSelected = onSelect,
            hoverIndex = -1
        )
        Log.e("HOLDMENU2", "📝 State updated: isOpen=${state.isOpen}, items=${state.items.size}")
    }

    fun setHover(index: Int) {
        if (state.hoverIndex != index) {
            state = state.copy(hoverIndex = index)
        }
    }

    fun close() {
        Log.e("HOLDMENU2", "📝 Controller.close() called")
        state = state.copy(isOpen = false, hoverIndex = -1)
    }
}

@Composable
fun rememberHoldMenu2(): HoldMenu2Controller {
    return remember { HoldMenu2Controller() }
}