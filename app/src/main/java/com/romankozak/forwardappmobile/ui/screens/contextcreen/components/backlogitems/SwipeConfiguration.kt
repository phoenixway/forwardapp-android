package com.romankozak.forwardappmobile.ui.screens.contextcreen.components.backlogitems

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class SwipeAction(
    val icon: ImageVector,
    val color: Color,
    val contentDescription: String,
    val action: () -> Unit,
)

data class SwipeConfiguration(
    val startToEnd: SwipeAction?,
    val endToStart: SwipeAction?,
)
