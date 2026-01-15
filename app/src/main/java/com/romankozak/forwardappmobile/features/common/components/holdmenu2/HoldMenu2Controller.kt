package com.romankozak.forwardappmobile.features.common.components.holdmenu2

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density

@Stable
class HoldMenu2Controller {
    var state by mutableStateOf(HoldMenu2State())
    private set

    private var screenWidth: Float = 0f
        private var screenHeight: Float = 0f
            private var density: Density? = null

                fun setScreenDimensions(width: Float, height: Float, density: Density) {
                    this.screenWidth = width
                    this.screenHeight = height
                    this.density = density
                }

                fun open(
                    anchor: Offset,
                    touch: Offset,
                    items: List<HoldMenuItem>,
                    onSelect: (Int) -> Unit,
                         iconPosition: IconPosition = IconPosition.START,
                         menuAlignment: MenuAlignment = MenuAlignment.START,
                         isDragMode: Boolean = true,
                ) {
                    Log.e("HOLDMENU2", "📂 Controller.open() called, items=${items.size}, isDragMode=$isDragMode")

                    val currentDensity = density
                    if (currentDensity == null || screenWidth == 0f || screenHeight == 0f) {
                        Log.e("HOLDMENU2", "⚠️ Screen dimensions not set!")
                        return
                    }

                    // Розраховуємо layout меню
                    val layout = HoldMenu2Geometry.calculateMenuLayout(
                        anchor = anchor,
                        itemCount = items.size,
                        density = currentDensity,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                    )

                    // Розраховуємо початковий hover (тільки для drag mode)
                    val initialHover = if (isDragMode) {
                        HoldMenu2Geometry.calculateHoverIndex(
                            fingerPosition = touch,
                            layout = layout,
                            itemCount = items.size,
                        )
                    } else {
                        -1  // В tap mode немає початкового hover
                    }

                    state = state.copy(
                        isOpen = true,
                        anchor = anchor,
                        touch = touch,
                        items = items,
                        onItemSelected = onSelect,
                        hoverIndex = initialHover,
                        layout = layout,
                        iconPosition = iconPosition,
                        menuAlignment = menuAlignment,
                        isDragMode = isDragMode,
                    )

                    Log.e("HOLDMENU2", "📂 State updated: isOpen=${state.isOpen}, items=${state.items.size}, hover=$initialHover, isDragMode=$isDragMode")
                }

                fun updateHover(fingerPosition: Offset) {
                    val layout = state.layout ?: return

                    val newHover = HoldMenu2Geometry.calculateHoverIndex(
                        fingerPosition = fingerPosition,
                        layout = layout,
                        itemCount = state.items.size,
                    )

                    if (state.hoverIndex != newHover) {
                        Log.e("HOLDMENU2", "🎯 Hover: $newHover (pos=$fingerPosition)")
                        state = state.copy(hoverIndex = newHover)
                    }
                }

                fun close() {
                    Log.e("HOLDMENU2", "🔒 Controller.close() called")
                    state = state.copy(isOpen = false, hoverIndex = -1)
                }
}

@Composable
fun rememberHoldMenu2(): HoldMenu2Controller {
    return remember { HoldMenu2Controller() }
}
