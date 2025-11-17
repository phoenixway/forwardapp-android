package com.romankozak.forwardappmobile.ui.holdmenu

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerId

data class HoldMenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit
)

data class HoldMenuState(
    val isOpen: Boolean = false,
    val anchor: Offset = Offset.Zero,
    val selectedIndex: Int? = null,
    val items: List<HoldMenuItem> = emptyList(),
    val pointerId: PointerId? = null
)
