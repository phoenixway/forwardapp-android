package com.romankozak.forwardappmobile.ui.holdmenu

import androidx.compose.ui.geometry.Offset

data class HoldMenuState(
    val isOpen: Boolean = false,
    val anchor: Offset = Offset.Zero,
    val items: List<String> = emptyList(),
    val onItemSelected: ((Int) -> Unit)? = null,
)




