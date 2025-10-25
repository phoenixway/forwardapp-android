package com.romankozak.forwardappmobile.ui.screens.projectscreen.dnd

import androidx.compose.ui.geometry.Offset

data class DragState(
    val dragInProgress: Boolean = false,
    val dragAmount: Offset = Offset.Zero,
    val draggedItemIndex: Int? = null,
    val targetItemIndex: Int? = null,
    val draggedItemHeight: Float = 0f,
    val itemHeights: Map<Int, Float> = emptyMap() // Нове поле
)
