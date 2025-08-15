package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.GoalWithInstanceInfo
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class SwipeState {
    ActionsRevealedStart,
    Normal,
    ActionsRevealedEnd
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
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    onTagClick: (String) -> Unit,
    onAssociatedListClick: (String) -> Unit,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    backgroundColor: Color,
    onSwipeStart: () -> Unit,
    isAnotherItemSwiped: Boolean,
    dragHandleModifier: Modifier = Modifier,
    // ✨ ДОДАНО: Нові колбеки для правого свайпу
    onMoreActionsRequest: () -> Unit,
    onCreateInstanceRequest: () -> Unit,
    onMoveInstanceRequest: () -> Unit,
    onCopyGoalRequest: () -> Unit
)

{
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

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
                velocityThreshold = { with(density) { 100.dp.toPx() } },
                snapAnimationSpec = tween(300, easing = FastOutSlowInEasing),
                decayAnimationSpec = splineBasedDecay(density)
            )
        }

        // ✨ ЗМІНЕНО: Збільшено відстань для правого свайпу, щоб розмістити 4 кнопки
        val actionsRevealPx = with(density) { 288.dp.toPx() }
        val actionsRevealPxNegative = with(density) { -180.dp.toPx() }

        LaunchedEffect(Unit) {
            swipeState.updateAnchors(
                DraggableAnchors {
                    SwipeState.Normal at 0f
                    SwipeState.ActionsRevealedStart at actionsRevealPx
                    SwipeState.ActionsRevealedEnd at actionsRevealPxNegative
                }
            )
        }

        val resetSwipe: () -> Unit = {
            coroutineScope.launch {
                swipeState.animateTo(SwipeState.Normal)
            }
        }

        LaunchedEffect(swipeState.currentValue) {
            if (swipeState.currentValue != SwipeState.Normal) {
                onSwipeStart()
            }
        }

        LaunchedEffect(isAnotherItemSwiped) {
            if (isAnotherItemSwiped) {
                resetSwipe()
            }
        }

        val actionsAlpha = (abs(swipeState.offset) / actionsRevealPx).coerceIn(0f, 1f)

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ✨ ПОВНІСТЮ ОНОВЛЕНО: Блок для правого свайпу тепер містить 4 кнопки
            if (swipeState.offset > 0) {
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(actionsAlpha),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val buttonWidth = 72.dp
                    // 1. More Actions (заглушка)
                    Surface(
                        onClick = { onMoreActionsRequest(); resetSwipe() },
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(buttonWidth),
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.MoreVert, "Більше дій", tint = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                    // 2. Create Instance (Clone)
                    Surface(
                        onClick = { onCreateInstanceRequest(); resetSwipe() },
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(buttonWidth),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RectangleShape
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.AddLink, "Створити зв'язок", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    // 3. Move Instance
                    Surface(
                        onClick = { onMoveInstanceRequest(); resetSwipe() },
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(buttonWidth),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RectangleShape
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Перемістити", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    // 4. Copy Goal (Clone goal)
                    Surface(
                        onClick = { onCopyGoalRequest(); resetSwipe() },
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(buttonWidth),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RectangleShape
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.ContentCopy, "Клонувати ціль", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
            else if (swipeState.offset < 0) {
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(actionsAlpha),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        onClick = {
                            onDelete()
                            resetSwipe()
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(88.dp),
                        color = MaterialTheme.colorScheme.error,
                        shape = RectangleShape
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.Delete, "Видалити", tint = MaterialTheme.colorScheme.onError)
                        }
                    }
                    Surface(
                        onClick = {
                            // заглушка для нової дії
                            resetSwipe()
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(88.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.DeleteForever, "Видалити звідусіль", tint = MaterialTheme.colorScheme.onTertiary)
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
                color = backgroundColor,
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
                    dragHandleModifier = dragHandleModifier
                )
            }
        }
    }
}