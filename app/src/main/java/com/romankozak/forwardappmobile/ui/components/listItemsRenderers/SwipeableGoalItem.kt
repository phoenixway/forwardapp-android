// File: app/src/main/java/com/romankozak/forwardappmobile/ui/components/SwipeableGoalItem.kt
package com.romankozak.forwardappmobile.ui.components.listItemsRenderers

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class SwipeState {
    ActionsRevealedStart,
    Normal,
    ActionsRevealedEnd
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableGoalItem(
    modifier: Modifier = Modifier,
    goalContent: ListItemContent.GoalItem,
    isDragging: Boolean,
    isAnyItemDragging: Boolean, // Глобальний прапор: "чи перетягують будь-який елемент на екрані?"

    obsidianVaultName: String,
    resetTrigger: Int, // Параметр залишається
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onTagClick: (String) -> Unit,
    onRelatedLinkClick: (RelatedLink) -> Unit,
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
    contextMarkerToEmojiMap: Map<String, String>,
) {
    // --- ПОЧАТОК ІНТЕГРАЦІЇ resetTrigger ---
    // Огортаємо всю логіку стану в `key`. Коли `resetTrigger` зміниться,
    // всі `remember` всередині будуть скинуті до початкових значень.
    key(resetTrigger) {
        val coroutineScope = rememberCoroutineScope()
        val density = LocalDensity.current

        val leftActionsWidth = 60.dp * 4
        val rightActionsWidth = 60.dp * 2

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
                positionalThreshold = { distance: Float -> distance * 0.85f },
                velocityThreshold = { with(density) { 250.dp.toPx() } },
                snapAnimationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                decayAnimationSpec = exponentialDecay(),
                confirmValueChange = { newValue ->
                    if (isDragging) return@AnchoredDraggableState false
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

        /*LaunchedEffect(isDragging) {
            if (isDragging && swipeState.currentValue != SwipeState.Normal) {
                swipeState.snapTo(SwipeState.Normal)
            }
        }*/

        LaunchedEffect(isAnyItemDragging) {
            // Якщо будь-який елемент почали тягнути, І ЦЕ НЕ Я, і я відкритий...
            if (isAnyItemDragging && !isDragging && swipeState.currentValue != SwipeState.Normal) {
                // ... миттєво закриваємося.
                //swipeState.snapTo(SwipeState.Normal)
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

        val dynamicShape = remember(offset > 0, offset < 0) {
            val cornerRadius = 12.dp
            when {
                offset > 0 -> RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = cornerRadius, bottomEnd = cornerRadius)
                offset < 0 -> RoundedCornerShape(topStart = cornerRadius, bottomStart = cornerRadius, topEnd = 0.dp, bottomEnd = 0.dp)
                else -> RoundedCornerShape(cornerRadius)
            }
        }

        Box(modifier = modifier.fillMaxWidth()) {
            if (!isDragging) {
                // ... Swipe Actions UI ...
                Row(
                    modifier = Modifier.matchParentSize().alpha(if (offset > 0) actionsAlpha else 0f),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SwipeActionIcon(Icons.Default.MoreVert, "Більше дій", MaterialTheme.colorScheme.secondary) { onMoreActionsRequest(); resetSwipe() }
                    SwipeActionIcon(Icons.Default.AddLink, "Створити зв'язок", MaterialTheme.colorScheme.primary) { onCreateInstanceRequest(); resetSwipe() }
                    SwipeActionIcon(Icons.AutoMirrored.Filled.Send, "Перемістити", MaterialTheme.colorScheme.tertiary) { onMoveInstanceRequest(); resetSwipe() }
                    SwipeActionIcon(Icons.Default.ContentCopy, "Клонувати ціль", MaterialTheme.colorScheme.inversePrimary) { onCopyGoalRequest(); resetSwipe() }
                }

                if (offset < 0) {
                    Row(
                        modifier = Modifier.matchParentSize().alpha(if (offset < 0) actionsAlpha else 0f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SwipeActionIcon(Icons.Default.Delete, "Видалити", MaterialTheme.colorScheme.error) { onDelete(); resetSwipe() }
                        SwipeActionIcon(Icons.Default.DeleteForever, "Видалити звідусіль", MaterialTheme.colorScheme.tertiary) { resetSwipe() }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offset.roundToInt(), 0) }
                    .anchoredDraggable(state = swipeState, orientation = Orientation.Horizontal, enabled = !isDragging)
                    .combinedClickable(
                        onClick = {
                            // Якщо елемент свайпнутий - клік його закриває.
                            // Якщо ні - виконується основна дія (редагування).
                            if (swipeState.settledValue != SwipeState.Normal) {
                                resetSwipe()
                            } else {
                                onItemClick()
                            }
                        },
                        onLongClick = {
                            // Довгий клік спрацьовує тільки якщо елемент не свайпнутий.
                            if (swipeState.settledValue == SwipeState.Normal) {
                                onLongClick()
                            }
                        }
                    ),

                color = backgroundColor,
                shape = dynamicShape
            ) {
                GoalItem(
                    goal = goalContent.goal,
                    obsidianVaultName = obsidianVaultName,
                    onToggle = onToggle,
                    onItemClick = { if (swipeState.settledValue == SwipeState.Normal) onItemClick() else resetSwipe() },
                    onLongClick = { if (swipeState.settledValue == SwipeState.Normal) onLongClick() },
                    onTagClick = onTagClick,
                    onRelatedLinkClick = onRelatedLinkClick,
                    dragHandleModifier = dragHandleModifier,
                    contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                )
            }
        }
    }
    // --- КІНЕЦЬ ІНТЕГРАЦІЇ resetTrigger ---
}
