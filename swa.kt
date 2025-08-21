// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/components/SwipeableGoalItem.kt

package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.GoalWithInstanceInfo
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
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
    onMoreActionsRequest: () -> Unit,
    onCreateInstanceRequest: () -> Unit,
    onMoveInstanceRequest: () -> Unit,
    onCopyGoalRequest: () -> Unit,
    contextMarkerToHide: String? = null,
    emojiToHide: String? = null,

    contextMarkerToEmojiMap: Map<String, String>
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var swipeResetKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(isDragging) { if (!isDragging) swipeResetKey++ }

    key(swipeResetKey, resetTrigger) {
        val leftActionsWidth = 60.dp * 4
        val rightActionsWidth = 60.dp * 2

        val actionsRevealPx = with(density) { leftActionsWidth.toPx() }
        val actionsRevealPxNegative = with(density) { -(rightActionsWidth.toPx()) }

        val maxSwipeDistance = max(actionsRevealPx, abs(actionsRevealPxNegative))

        val anchors = DraggableAnchors {
            SwipeState.ActionsRevealedStart at actionsRevealPx
            SwipeState.Normal at 0f
            SwipeState.ActionsRevealedEnd at actionsRevealPxNegative
        }

        var lastConfirmedState by remember { mutableStateOf(SwipeState.Normal) }
        var swipeDirection by remember { mutableStateOf<Int?>(null) }

        val swipeState = remember {
            AnchoredDraggableState(
                initialValue = SwipeState.Normal,
                anchors = anchors,
                positionalThreshold = { distance: Float -> distance * 0.85f },
                velocityThreshold = { with(density) { 250.dp.toPx() } },
                snapAnimationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                decayAnimationSpec = exponentialDecay(),
                confirmValueChange = { newValue ->
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
                            if (canChange) {
                                lastConfirmedState = newValue
                            }
                            canChange
                        }
                    }
                },
            )
        }

        LaunchedEffect(swipeState.settledValue) {
            lastConfirmedState = swipeState.settledValue
            if (swipeState.settledValue == SwipeState.Normal) {
                swipeDirection = null
            }
        }

        val resetSwipe = { coroutineScope.launch { swipeState.animateTo(SwipeState.Normal) } }

        LaunchedEffect(swipeState.settledValue) {
            if (swipeState.settledValue != SwipeState.Normal) onSwipeStart()
        }

        LaunchedEffect(isAnotherItemSwiped) {
            if (isAnotherItemSwiped) {
                swipeDirection = null
                resetSwipe()
            }
        }

        val offset = swipeState.requireOffset().coerceIn(-maxSwipeDistance, maxSwipeDistance)
        val actionsAlpha = (abs(offset) /
                if (offset > 0) actionsRevealPx else abs(actionsRevealPxNegative)
                ).coerceIn(0f, 1f)

        val dynamicShape = remember(offset) {
            val cornerRadius = 8.dp
            when {
                offset > 0 -> RoundedCornerShape(
                    topStart = 0.dp, bottomStart = 0.dp,
                    topEnd = cornerRadius, bottomEnd = cornerRadius
                )
                offset < 0 -> RoundedCornerShape(
                    topStart = cornerRadius, bottomStart = cornerRadius,
                    topEnd = 0.dp, bottomEnd = 0.dp
                )
                else -> RoundedCornerShape(cornerRadius)
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(actionsAlpha),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val buttonSize = 60.dp
                Surface(
                    onClick = { onMoreActionsRequest(); resetSwipe() },
                    modifier = Modifier.size(buttonSize),
                    color = MaterialTheme.colorScheme.secondary,
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.MoreVert, "Більше дій", tint = MaterialTheme.colorScheme.onSecondary)
                    }
                }
                Surface(
                    onClick = { onCreateInstanceRequest(); resetSwipe() },
                    modifier = Modifier.size(buttonSize),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.AddLink, "Створити зв'язок", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Surface(
                    onClick = { onMoveInstanceRequest(); resetSwipe() },
                    modifier = Modifier.size(buttonSize),
                    color = MaterialTheme.colorScheme.tertiary
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Перемістити", tint = MaterialTheme.colorScheme.onTertiary)
                    }
                }
                Surface(
                    onClick = { onCopyGoalRequest(); resetSwipe() },
                    modifier = Modifier.size(buttonSize),
                    color = MaterialTheme.colorScheme.inversePrimary
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.ContentCopy, "Клонувати ціль", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (offset < 0) {
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(actionsAlpha),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val buttonSize = 60.dp
                    Surface(
                        onClick = { onDelete(); resetSwipe() },
                        modifier = Modifier.size(buttonSize),
                        color = MaterialTheme.colorScheme.error,
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.Delete, "Видалити", tint = MaterialTheme.colorScheme.onError)
                        }
                    }
                    Surface(
                        onClick = { resetSwipe() }, // Placeholder for future action
                        modifier = Modifier.size(buttonSize),
                        color = MaterialTheme.colorScheme.tertiary,
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
                    .offset { IntOffset(offset.roundToInt(), 0) }
                    .anchoredDraggable(
                        state = swipeState,
                        orientation = Orientation.Horizontal,
                    ),
                color = backgroundColor,
                shape = dynamicShape,
            ) {
                GoalItem(
                    goal = goalWithInstance.goal,
                    associatedLists = associatedLists,
                    obsidianVaultName = obsidianVaultName,
                    onToggle = onToggle,
                    onItemClick = { if (swipeState.settledValue == SwipeState.Normal) onItemClick() else resetSwipe() },
                    onLongClick = { if (swipeState.settledValue == SwipeState.Normal) onLongClick() },
                    onTagClick = onTagClick,
                    onAssociatedListClick = onAssociatedListClick,
                    backgroundColor = Color.Transparent,
                    dragHandleModifier = dragHandleModifier,
                    //contextMarkerToHide = contextMarkerToHide,
                    emojiToHide = emojiToHide,

                    contextMarkerToEmojiMap = contextMarkerToEmojiMap
                )
            }
        }
    }
}
