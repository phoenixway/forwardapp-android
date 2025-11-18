package com.romankozak.forwardappmobile.features.common.components.holdmenu2

import androidx.compose.ui.geometry.Offset

data class HoldMenu2State(
    val isOpen: Boolean = false,
    val anchor: Offset = Offset.Zero,
    val touch: Offset = Offset.Zero,
    val items: List<HoldMenuItem> = emptyList(),
    val hoverIndex: Int = -1,
    val onItemSelected: ((Int) -> Unit)? = null,
    val layout: HoldMenu2Geometry.MenuLayout? = null,
    val iconPosition: IconPosition = IconPosition.START,
    val menuAlignment: MenuAlignment = MenuAlignment.START,
)