package com.romankozak.forwardappmobile.ui.dnd

import androidx.compose.ui.geometry.Offset

data class DnDVisualState(
    val itemOffsets: Map<Int, Float> = emptyMap(),
    val draggedItemHeight: Float = 0f,
    val isDragging: Boolean = false
)