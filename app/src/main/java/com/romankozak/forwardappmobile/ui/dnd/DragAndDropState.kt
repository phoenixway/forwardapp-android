package com.romankozak.forwardappmobile.ui.dnd

import androidx.compose.ui.geometry.Offset

data class DragAndDropState(
    val dragInProgress: Boolean = false,
    val draggedItemIndex: Int? = null,
    val targetItemIndex: Int? = null,
    val dragAmount: Offset = Offset.Zero,
    val initialItemOffset: Int = 0,
    val dragOffsetInItem: Float = 0f,
    val totalScrollAmount: Float = 0f,
    // ✅ НОВЕ: Дані для рендерингу ghost елемента
    val draggedItemHeight: Float = 0f,
    val ghostScreenY: Float = 0f  // Абсолютна Y позиція на екрані
)