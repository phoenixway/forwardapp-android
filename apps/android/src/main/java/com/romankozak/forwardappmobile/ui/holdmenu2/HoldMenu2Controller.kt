package com.romankozak.forwardappmobile.ui.holdmenu2

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset

@Stable
class HoldMenu2Controller internal constructor(
    initial: HoldMenu2State,
    private val onStateChange: (HoldMenu2State) -> Unit
) {
    var state: HoldMenu2State = initial
        private set

    private fun update(block: (HoldMenu2State) -> HoldMenu2State) {
        val new = block(state)
        state = new
        onStateChange(new)
    }

    fun open(
        anchor: Offset,
        touch: Offset,
        items: List<String>,
        onSelect: (Int) -> Unit
    ) {
        update {
            it.copy(
                isOpen = true,
                anchor = anchor,
                touch = touch,
                items = items,
                onItemSelected = onSelect
            )
        }
    }

    fun setHover(index: Int) {
        update { it.copy(hoverIndex = index) }
    }

    fun close() {
        update { it.copy(isOpen = false, hoverIndex = -1) }
    }
}

@Composable
fun rememberHoldMenu2(): HoldMenu2Controller {
    val state = remember { mutableStateOf(HoldMenu2State()) }
    return remember { HoldMenu2Controller(state.value) { state.value = it } }
}

