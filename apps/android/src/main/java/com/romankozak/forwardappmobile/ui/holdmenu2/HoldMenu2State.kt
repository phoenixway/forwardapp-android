package com.romankozak.forwardappmobile.ui.holdmenu2

import androidx.compose.ui.geometry.Offset

data class HoldMenu2State(
    val isOpen: Boolean = false,
    val anchor: Offset = Offset.Zero,
    val touch: Offset = Offset.Zero,
    val items: List<String> = emptyList(),
    val hoverIndex: Int = -1,
    val onItemSelected: ((Int) -> Unit)? = null,
)

