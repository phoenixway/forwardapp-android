package com.romankozak.forwardappmobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.animation.splineBasedDecay // ✨ ДОДАЙТЕ ЦЕЙ ІМПОРТ

// Спрощені стани для свайпу
enum class SwipeState {
    Normal,      // Початкове положення
    ActionsRevealed,  // Лівий свайп - показані дії
    DeleteTriggered   // Правий свайп - запущено видалення
}


@Composable
fun SwipeableGoalItem(
    resetTrigger: Int,
    goalWithInstance: GoalWithInstanceInfo,
    isHighlighted: Boolean,
    isDragging: Boolean, // Додано
    associatedLists: List<GoalList>,
    obsidianVaultName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
    onToggle: () -> Unit,
    onTagClick: (String) -> Unit,
    onAssociatedListClick: (String) -> Unit,
    dragHandle: @Composable () -> Unit, // Додано
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current

    val actionsRevealPx = with(density) { 180.dp.toPx() }
    val deleteThresholdPx = with(density) { (-120.dp).toPx() }

    val anchors = DraggableAnchors {
        SwipeState.ActionsRevealed at actionsRevealPx
        SwipeState.Normal at 0f
        SwipeState.DeleteTriggered at deleteThresholdPx
    }

    val swipeState: AnchoredDraggableState<SwipeState> = remember(resetTrigger) {
        AnchoredDraggableState(
            initialValue = SwipeState.Normal,
            anchors = anchors,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = tween(300, easing = FastOutSlowInEasing),
            // ✨ ВИПРАВЛЕНО: Замість null використовується реалізація за замовчуванням
            decayAnimationSpec = splineBasedDecay(density)
        )
    }

    LaunchedEffect(swipeState.targetValue) {
        if (swipeState.targetValue == SwipeState.DeleteTriggered) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onDelete()
            swipeState.snapTo(SwipeState.Normal)
        }
    }

    // Оновлена анімація кольору фону з урахуванням перетягування
    val itemBackgroundColor by animateColorAsState(
        targetValue = when {
            isDragging -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
            swipeState.offset < 0 -> MaterialTheme.colorScheme.errorContainer.copy(
                alpha = (abs(swipeState.offset) / abs(deleteThresholdPx)).coerceIn(0f, 0.3f)
            )
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "ItemBackgroundColor"
    )

    val resetSwipe: () -> Unit = {
        coroutineScope.launch {
            swipeState.animateTo(SwipeState.Normal)
        }
    }

    val actionsAlpha = (abs(swipeState.offset) / actionsRevealPx).coerceIn(0f, 1f)
    val deleteAlpha = if (swipeState.offset < 0) (abs(swipeState.offset) / abs(deleteThresholdPx)).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(itemBackgroundColor, RoundedCornerShape(8.dp))
    ) {
        // Фонові елементи для свайпу
        Box(
            modifier = Modifier.matchParentSize()
        ) {
            // Фон для видалення (свайп вправо)
            if (swipeState.offset < 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(abs(swipeState.offset).let { with(density) { it.toDp() } })
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = deleteAlpha * 0.5f),
                            RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                        ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White.copy(alpha = deleteAlpha),
                        modifier = Modifier
                            .padding(end = 24.dp)
                            .size(24.dp)
                            .scale(if (deleteAlpha > 0.8f) 1.2f else 1f)
                    )
                }
            }

            // Фон для дій (свайп вліво)
            if (swipeState.offset > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .alpha(actionsAlpha),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Surface(
                        onClick = { onEdit(); resetSwipe() },
                        modifier = Modifier.size(width = 88.dp, height = 56.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Surface(
                        onClick = { onMore(); resetSwipe() },
                        modifier = Modifier.size(width = 88.dp, height = 56.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RectangleShape
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                }
            }
        }

        // Основний контент
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(swipeState.offset.roundToInt(), 0) }
                .anchoredDraggable(state = swipeState, orientation = Orientation.Horizontal),
            onClick = {
                if (swipeState.currentValue == SwipeState.Normal) onMore() else resetSwipe()
            },
            color = MaterialTheme.colorScheme.surface,
            // Оновлена тінь з урахуванням перетягування
            shadowElevation = if (swipeState.offset != 0f || isDragging) 8.dp else 0.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            GoalItem(
                goal = goalWithInstance.goal,
                associatedLists = associatedLists,
                obsidianVaultName = obsidianVaultName,
                onToggle = onToggle,
                onItemClick = { onMore() },
                onTagClick = onTagClick,
                onAssociatedListClick = onAssociatedListClick,
                backgroundColor = Color.Transparent,
                // Передаємо ручку для перетягування
                dragHandle = dragHandle
            )
        }
    }
}