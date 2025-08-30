// File: app/src/main/java/com/romankozak/forwardappmobile/ui/components/listItemsRenderers/SwipeableListItem.kt

package com.romankozak.forwardappmobile.ui.screens.backlog.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Moving
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Start
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class SwipeState { ActionsRevealedStart, Normal, ActionsRevealedEnd }

object SwipeConstants {
    val LEFT_ACTION_WIDTH = 60.dp
    val RIGHT_ACTION_WIDTH = 60.dp
    const val LEFT_ACTIONS_COUNT = 4
    const val RIGHT_ACTIONS_COUNT = 2
    const val SWIPE_THRESHOLD = 0.3f
    const val VELOCITY_THRESHOLD_DP = 250f
    const val ANIMATION_DURATION = 400
    val CORNER_RADIUS = 16.dp
    val SHADOW_ELEVATION = 2.dp
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableListItem(
    modifier: Modifier = Modifier,
    isDragging: Boolean,
    isAnyItemDragging: Boolean,
    resetTrigger: Int,
    backgroundColor: Color,
    onSwipeStart: () -> Unit,
    isAnotherItemSwiped: Boolean,
    onDelete: () -> Unit,
    onMoreActionsRequest: () -> Unit,
    onGoalTransportRequest: () -> Unit, // Нова функція для транспорту цілі
    onCopyContentRequest: () -> Unit, // Нова функція для копіювання контенту
    content: @Composable () -> Unit,
    swipeEnabled: Boolean = true,
) {
    key(resetTrigger) {
        val coroutineScope = rememberCoroutineScope()
        val density = LocalDensity.current

        val leftActionsWidth = SwipeConstants.LEFT_ACTION_WIDTH * SwipeConstants.LEFT_ACTIONS_COUNT
        val rightActionsWidth = SwipeConstants.RIGHT_ACTION_WIDTH * SwipeConstants.RIGHT_ACTIONS_COUNT
        val actionsRevealPx = with(density) { leftActionsWidth.toPx() }
        val actionsRevealPxNegative = with(density) { -(rightActionsWidth.toPx()) }

        val anchors = remember {
            DraggableAnchors {
                SwipeState.ActionsRevealedStart at actionsRevealPx
                SwipeState.Normal at 0f
                SwipeState.ActionsRevealedEnd at actionsRevealPxNegative
            }
        }

        var lastConfirmedState by remember { mutableStateOf(SwipeState.Normal) }
        var swipeDirection by remember { mutableStateOf<Int?>(null) }

        val swipeState = remember {
            AnchoredDraggableState(
                initialValue = SwipeState.Normal,
                anchors = anchors,
                positionalThreshold = { distance: Float -> distance * SwipeConstants.SWIPE_THRESHOLD },
                velocityThreshold = { with(density) { SwipeConstants.VELOCITY_THRESHOLD_DP.dp.toPx() } },
                snapAnimationSpec = tween(durationMillis = SwipeConstants.ANIMATION_DURATION, easing = FastOutSlowInEasing),
                decayAnimationSpec = exponentialDecay(),
                confirmValueChange = { newValue ->
                    // КЛЮЧОВЕ ВИПРАВЛЕННЯ: Не дозволяємо swipe під час dragging або якщо він вимкнений
                    if (!swipeEnabled || isDragging || isAnyItemDragging) return@AnchoredDraggableState false

                    when {
                        newValue == SwipeState.Normal -> {
                            swipeDirection = null
                            lastConfirmedState = newValue
                            true
                        }
                        lastConfirmedState == SwipeState.Normal -> {
                            swipeDirection = when (newValue) {
                                SwipeState.ActionsRevealedStart -> 1
                                SwipeState.ActionsRevealedEnd -> -1
                                else -> null
                            }
                            lastConfirmedState = newValue
                            true
                        }
                        else -> {
                            val newDirection = when (newValue) {
                                SwipeState.ActionsRevealedStart -> 1
                                SwipeState.ActionsRevealedEnd -> -1
                                else -> null
                            }
                            val canChange = swipeDirection == null || swipeDirection == newDirection
                            if (canChange) lastConfirmedState = newValue
                            canChange
                        }
                    }
                },
            )
        }

        // ВАЖЛИВО: Скидаємо swipe стан при початку dragging
        LaunchedEffect(isAnyItemDragging) {
            if (isAnyItemDragging && !isDragging && swipeState.currentValue != SwipeState.Normal) {
                coroutineScope.launch { swipeState.animateTo(SwipeState.Normal) }
            }
        }

        val resetSwipe = { coroutineScope.launch { swipeState.animateTo(SwipeState.Normal) } }

        LaunchedEffect(swipeState.settledValue) {
            lastConfirmedState = swipeState.settledValue
            if (swipeState.settledValue == SwipeState.Normal) {
                swipeDirection = null
            } else {
                onSwipeStart()
            }
        }

        LaunchedEffect(isAnotherItemSwiped) {
            if (isAnotherItemSwiped) resetSwipe()
        }

        val offset = swipeState.requireOffset()
        val actionsAlpha = (abs(offset) / (if (offset > 0) actionsRevealPx else abs(actionsRevealPxNegative))).coerceIn(0f, 1f)
        val leftActionsScale by animateFloatAsState(targetValue = if (offset > 0) (0.8f + 0.2f * actionsAlpha) else 0.8f, animationSpec = tween(200), label = "leftActionsScale")
        val rightActionsScale by animateFloatAsState(targetValue = if (offset < 0) (0.8f + 0.2f * actionsAlpha) else 0.8f, animationSpec = tween(200), label = "rightActionsScale")

        val dynamicShape = remember(offset > 0, offset < 0) {
            when {
                offset > 0 -> RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = SwipeConstants.CORNER_RADIUS, bottomEnd = SwipeConstants.CORNER_RADIUS)
                offset < 0 -> RoundedCornerShape(topStart = SwipeConstants.CORNER_RADIUS, bottomStart = SwipeConstants.CORNER_RADIUS, topEnd = 0.dp, bottomEnd = 0.dp)
                else -> RoundedCornerShape(SwipeConstants.CORNER_RADIUS)
            }
        }

        val shadowElevation by animateFloatAsState(targetValue = if (abs(offset) > 10f) SwipeConstants.SHADOW_ELEVATION.value else 0f, animationSpec = tween(200), label = "shadowElevation")

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            // Дії для swipe вправо (оновлені дії)
            if (swipeEnabled && !isDragging && !isAnyItemDragging && offset > 0) {
                Row(
                    modifier = Modifier.matchParentSize().alpha(actionsAlpha).padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SwipeActionButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "Більше дій",
                        color = MaterialTheme.colorScheme.secondary,
                        scale = leftActionsScale,
                        onClick = { onMoreActionsRequest(); resetSwipe() }
                    )
                    SwipeActionButton(
                        icon = Icons.Default.Moving ,
                        contentDescription = "Транспорт цілі",
                        color = MaterialTheme.colorScheme.primary,
                        scale = leftActionsScale,
                        onClick = { onGoalTransportRequest(); resetSwipe() }
                    )
                    SwipeActionButton(
                        icon = Icons.Default.ContentCopy,
                        contentDescription = "Копіювати контент",
                        color = MaterialTheme.colorScheme.tertiary,
                        scale = leftActionsScale,
                        onClick = { onCopyContentRequest(); resetSwipe() }
                    )
                    // Четверта дія залишається порожньою або можна додати ще одну
                    SwipeActionButton(
                        icon = Icons.Default.PlayArrow,
                        contentDescription = "Резерв",
                        color = MaterialTheme.colorScheme.inversePrimary,
                        scale = leftActionsScale,
                        onClick = { resetSwipe() }
                    )
                }
            }

            // Дії для swipe вліво (залишаються без змін)
            if (swipeEnabled && !isDragging && !isAnyItemDragging && offset < 0) {
                Row(
                    modifier = Modifier.matchParentSize().alpha(actionsAlpha).padding(end = 1.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SwipeActionButton(
                        icon = Icons.Default.Delete,
                        contentDescription = "Видалити",
                        color = MaterialTheme.colorScheme.error,
                        scale = rightActionsScale,
                        onClick = { onDelete(); resetSwipe() }
                    )
                    SwipeActionButton(
                        icon = Icons.Default.DeleteForever,
                        contentDescription = "Видалити назавжди",
                        color = MaterialTheme.colorScheme.errorContainer,
                        scale = rightActionsScale,
                        onClick = { resetSwipe() }
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offset.roundToInt(), 0) }
                    .shadow(elevation = shadowElevation.dp, shape = dynamicShape)
                    .graphicsLayer { rotationZ = (offset / 50f).coerceIn(-2f, 2f) }
                    .anchoredDraggable(
                        state = swipeState,
                        orientation = Orientation.Horizontal,
                        // КЛЮЧОВЕ ВИПРАВЛЕННЯ: Вимикаємо swipe під час dragging або якщо він вимкнений
                        enabled = swipeEnabled && !isDragging && !isAnyItemDragging
                    ),
                color = backgroundColor,
                shape = dynamicShape,
                tonalElevation = if (abs(offset) > 10f) 4.dp else 0.dp
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SwipeActionButton(
    icon: ImageVector,
    contentDescription: String,
    color: Color,
    scale: Float,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(48.dp).scale(scale),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.85f),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}