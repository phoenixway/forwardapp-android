package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.compositeOver
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
    modifier: Modifier = Modifier,
    resetTrigger: Int,
    goalWithInstance: GoalWithInstanceInfo,
    isHighlighted: Boolean,
    isDragging: Boolean,
    associatedLists: List<GoalList>,
    obsidianVaultName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
    onToggle: () -> Unit,
    onTagClick: (String) -> Unit,
    onAssociatedListClick: (String) -> Unit,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    backgroundColor: Color
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

        val finalBackgroundColor by animateColorAsState(
            targetValue = if (swipeState.offset < 0) {
                val fraction = (abs(swipeState.offset) / abs(deleteThresholdPx)).coerceIn(0f, 1f)
                backgroundColor.copy(alpha = 1f - fraction * 0.6f)
                    .compositeOver(MaterialTheme.colorScheme.errorContainer.copy(alpha = fraction * 0.4f))
            } else {
                backgroundColor
            },
            animationSpec = tween(150),
            label = "FinalBackgroundColor"
        )

        val resetSwipe: () -> Unit = {
            coroutineScope.launch {
                swipeState.animateTo(SwipeState.Normal)
            }
        }

        val actionsAlpha = (abs(swipeState.offset) / actionsRevealPx).coerceIn(0f, 1f)

        Box(
            modifier = Modifier.fillMaxWidth()
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
                        onClick = { onMore(); resetSwipe() },
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

            Surface(
                modifier = modifier
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
                color = finalBackgroundColor,
                shape = RoundedCornerShape(8.dp)
            ) {
                GoalItem(
                    goal = goalWithInstance.goal,
                    associatedLists = associatedLists,
                    obsidianVaultName = obsidianVaultName,
                    onToggle = onToggle,
                    onItemClick = {
                        if (swipeState.currentValue == SwipeState.Normal) {
                            onItemClick()
                        } else {
                            resetSwipe()
                        }
                    },
                    onLongClick = {
                        if (swipeState.currentValue == SwipeState.Normal) {
                            onLongClick()
                        }
                    },
                    onTagClick = onTagClick,
                    onAssociatedListClick = onAssociatedListClick,
                    backgroundColor = Color.Transparent,
                )
            }
        }
    }
}