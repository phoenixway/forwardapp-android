package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.GoalWithInstanceInfo
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class SwipeState {
    Normal,
    ActionsRevealed,
    DeleteTriggered
}

@Composable
fun SwipeableGoalItem(
    resetTrigger: Int,
    goalWithInstance: GoalWithInstanceInfo,
    isHighlighted: Boolean,
    isDragging: Boolean,
    associatedLists: List<GoalList>,
    obsidianVaultName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
    onItemClick: () -> Unit, // ✨ ЗМІНА №1: Додано новий параметр для обробки кліку
    onToggle: () -> Unit,
    onTagClick: (String) -> Unit,
    onAssociatedListClick: (String) -> Unit,
    dragHandle: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current

    var swipeResetKey by remember { mutableStateOf(0) }

    LaunchedEffect(isDragging) {
        if (!isDragging) {
            swipeResetKey++
        }
    }

    key(swipeResetKey, resetTrigger) {
        val swipeState = remember {
            AnchoredDraggableState(
                initialValue = SwipeState.Normal,
                anchors = DraggableAnchors { SwipeState.Normal at 0f },
                positionalThreshold = { distance: Float -> distance * 0.6f },
                velocityThreshold = { with(density) { 1800.dp.toPx() } },
                snapAnimationSpec = tween(900, easing = FastOutSlowInEasing),
                decayAnimationSpec = splineBasedDecay(density)
            )
        }

        val actionsRevealPx = with(density) { 180.dp.toPx() }
        val deleteThresholdPx = with(density) { (-160.dp).toPx() }

        LaunchedEffect(Unit) {
            swipeState.updateAnchors(
                DraggableAnchors {
                    SwipeState.Normal at 0f
                    SwipeState.ActionsRevealed at actionsRevealPx
                    SwipeState.DeleteTriggered at deleteThresholdPx
                }
            )
        }

        LaunchedEffect(swipeState.targetValue) {
            if (swipeState.targetValue == SwipeState.DeleteTriggered) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
                swipeState.snapTo(SwipeState.Normal)
            }
        }

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
            Box(
                modifier = Modifier.matchParentSize()
            ) {
                if (swipeState.offset > 0) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .alpha(actionsAlpha),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Surface(
                            onClick = { onEdit(); resetSwipe() },
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(88.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        Surface(
                            onClick = { onMore(); resetSwipe() }, // ✨ ЗМІНА №2: Додано resetSwipe() для узгодженості
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(88.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RectangleShape
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.SwapHoriz, "More", tint = MaterialTheme.colorScheme.onSecondary)
                            }
                        }
                    }
                }
                if (swipeState.offset < 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .alpha(deleteAlpha)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(88.dp),
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Видалити",
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        val offsetValue = swipeState.offset
                        if (offsetValue.isFinite()) {
                            IntOffset(offsetValue.roundToInt(), 0)
                        } else {
                            IntOffset.Zero
                        }
                    }
                    .anchoredDraggable(state = swipeState, orientation = Orientation.Horizontal),
                // ✨ ЗМІНА №3: Використовуємо новий параметр onItemClick
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = if (swipeState.offset != 0f || isDragging) 8.dp else 0.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                GoalItem(
                    goal = goalWithInstance.goal,
                    associatedLists = associatedLists,
                    obsidianVaultName = obsidianVaultName,
                    onToggle = onToggle,
                    // ✨ ЗМІНА №4: Передаємо onItemClick до GoalItem

                    onItemClick = {
                        if (swipeState.currentValue == SwipeState.Normal) {
                            onItemClick()
                        } else {
                            resetSwipe()
                        }
                    },
                    onTagClick = onTagClick,
                    onAssociatedListClick = onAssociatedListClick,
                    backgroundColor = Color.Transparent,
                    dragHandle = dragHandle
                )
            }
        }
    }
}