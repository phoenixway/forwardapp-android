package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex


@Composable
fun DraggableItem(
    isDragging: Boolean,
    yOffset: Float,
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit,
) {
    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 16f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "elevation",
    )
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isDragging) 0.8f else 1f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 500f),
        label = "alpha",
    )

    val itemModifier =
        modifier
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = yOffset
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                shadowElevation = elevation
                clip = false
            }

    Box(modifier = itemModifier) {
        content(isDragging)
    }
