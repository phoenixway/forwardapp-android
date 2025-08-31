// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/backlog/components/LinkItemRow.kt
package com.romankozak.forwardappmobile.ui.screens.backlog.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink

@Composable
fun LinkItemRow(
    link: RelatedLink,
    isSelected: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit, // API для видалення
    modifier: Modifier = Modifier,
    endAction: @Composable () -> Unit = {},
) {
    // Анімації взяті з вашої детальної версії
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(),
        label = "link_item_background",
    )

    var isPressed by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 1.dp,
        label = "elevation"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "border_color_anim"
    )

    // Обгортка в SwipeableListItem для підтримки свайпу
    SwipeableListItem(
        isDragging = false,
        isAnyItemDragging = false,
        swipeEnabled = true,
        isAnotherItemSwiped = false,
        resetTrigger = 0,
        onSwipeStart = { },
        onDelete = onDelete,
        onMoreActionsRequest = { },
        onGoalTransportRequest = { },
        onCopyContentRequest = { },
        backgroundColor = backgroundColor,
        content = {
            // Вся візуальна частина (Card) вкладена сюди
            Card(
                modifier = modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                elevation = CardDefaults.elevatedCardElevation(elevation),
                border = BorderStroke(2.dp, animatedBorderColor)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(onClick, onLongClick) {
                                detectTapGestures(
                                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                                    onLongPress = { onLongClick() },
                                    onTap = { onClick() }
                                )
                            }
                            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (link.type) {
                                LinkType.GOAL_LIST -> Icons.AutoMirrored.Filled.ListAlt
                                LinkType.URL -> Icons.Default.Language
                                LinkType.OBSIDIAN -> Icons.AutoMirrored.Filled.Note
                                else -> Icons.Default.Link
                            },
                            contentDescription = "Link",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = link.displayName ?: link.target,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = when (link.type) {
                                    LinkType.GOAL_LIST -> "Посилання на список"
                                    LinkType.URL -> link.target
                                    LinkType.OBSIDIAN -> "Нотатка Obsidian"
                                    else -> "Посилання"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    endAction()
                }
            }
        }
    )
}