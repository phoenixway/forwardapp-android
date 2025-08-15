// File: app/src/main/java/com/romankozak/forwardappmobile/ui/components/SwipeableGoalItem.kt

package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var swipeResetKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(isDragging) { if (!isDragging) swipeResetKey++ }

    key(swipeResetKey, resetTrigger) {
        val actionsRevealPx = with(density) { 240.dp.toPx() } // Зменшено відстань для активації
        val actionsRevealPxNegative = with(density) { -160.dp.toPx() } // Зменшено відстань для активації

        // Додаткові пороги для більш контрольованого свайпу
        val minSwipeDistance = with(density) { 32.dp.toPx() }
        val maxSwipeDistance = with(density) { 320.dp.toPx() }

        val anchors = DraggableAnchors {
            SwipeState.ActionsRevealedStart at actionsRevealPx
            SwipeState.Normal at 0f
            SwipeState.ActionsRevealedEnd at actionsRevealPxNegative
        }

        // Простіший підхід без використання swipeState в confirmValueChange
        var lastConfirmedState by remember { mutableStateOf(SwipeState.Normal) }

        val swipeState = remember {
            AnchoredDraggableState(
                initialValue = SwipeState.Normal,
                anchors = anchors,
                positionalThreshold = { distance: Float -> distance * 0.8f },
                velocityThreshold = { with(density) { 200.dp.toPx() } },
                snapAnimationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                decayAnimationSpec = androidx.compose.animation.core.exponentialDecay(),
                confirmValueChange = { newValue ->
                    val canChange = when {
                        // Блокуємо прямий перехід між протилежними станами
                        lastConfirmedState == SwipeState.ActionsRevealedStart && newValue == SwipeState.ActionsRevealedEnd -> false
                        lastConfirmedState == SwipeState.ActionsRevealedEnd && newValue == SwipeState.ActionsRevealedStart -> false
                        else -> true
                    }
                    if (canChange) {
                        lastConfirmedState = newValue
                    }
                    canChange
                },
            )
        }

        // Оновлюємо lastConfirmedState при зміні стану
        LaunchedEffect(swipeState.settledValue) {
            lastConfirmedState = swipeState.settledValue
        }

        val resetSwipe = { coroutineScope.launch { swipeState.animateTo(SwipeState.Normal) } }

        LaunchedEffect(swipeState.settledValue) {
            if (swipeState.settledValue != SwipeState.Normal) onSwipeStart()
        }

        LaunchedEffect(isAnotherItemSwiped) {
            if (isAnotherItemSwiped) resetSwipe()
        }

        val offset = swipeState.requireOffset()
            .coerceIn(-maxSwipeDistance, maxSwipeDistance) // Обмежуємо максимальний offset
        val actionsAlpha = (abs(offset) /
                if (offset > 0) actionsRevealPx else abs(actionsRevealPxNegative)
                ).coerceIn(0f, 1f)

        Box(modifier = Modifier.fillMaxWidth()) {
            // --- Кнопки дій справа (offset > 0) ---
            if (offset > 0) {
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(actionsAlpha),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val buttonWidth = 72.dp

                    Surface(
                        onClick = { onMoreActionsRequest(); resetSwipe() },
                        modifier = Modifier.fillMaxHeight().width(buttonWidth),
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MoreVert, "Більше дій", tint = MaterialTheme.colorScheme.onSecondary)
                        }
                    }

                    Surface(
                        onClick = { onCreateInstanceRequest(); resetSwipe() },
                        modifier = Modifier.fillMaxHeight().width(buttonWidth),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RectangleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AddLink, "Створити зв'язок", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }

                    Surface(
                        onClick = { onMoveInstanceRequest(); resetSwipe() },
                        modifier = Modifier.fillMaxHeight().width(buttonWidth),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RectangleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Перемістити", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }

                    Surface(
                        onClick = { onCopyGoalRequest(); resetSwipe() },
                        modifier = Modifier.fillMaxHeight().width(buttonWidth),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RectangleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ContentCopy, "Клонувати ціль", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            // --- Кнопки дій зліва (offset < 0) ---
            if (offset < 0) {
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(actionsAlpha),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Surface(
                        onClick = { onDelete(); resetSwipe() },
                        modifier = Modifier.fillMaxHeight().width(88.dp),
                        color = MaterialTheme.colorScheme.error,
                        shape = RectangleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Delete, "Видалити", tint = MaterialTheme.colorScheme.onError)
                        }
                    }

                    Surface(
                        onClick = { resetSwipe() },
                        modifier = Modifier.fillMaxHeight().width(88.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.DeleteForever, "Видалити звідусіль", tint = MaterialTheme.colorScheme.onTertiary)
                        }
                    }
                }
            }

            // --- Основний контент ---
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offset.roundToInt(), 0) }
                    .anchoredDraggable(
                        state = swipeState,
                        orientation = Orientation.Horizontal,
                    ),
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp),
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
                )
            }
        }
    }
}