// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/backlog/components/NoteItemRow.kt
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
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ListItemContent

/**
 * Об'єднаний компонент для відображення нотатки,
 * що поєднує деталізований UI, анімації та свайп для видалення.
 */
@Composable
fun NoteItemRow(
    noteContent: ListItemContent.NoteItem,
    isSelected: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit, // API для видалення
    modifier: Modifier = Modifier,
    endAction: @Composable () -> Unit = {},
    maxLines: Int = 3,
) {
    // Анімації фону, рамки та тіні взяті з вашої детальної версії
    val background by animateColorAsState(
        targetValue = when {
            isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(),
        label = "note_background_color"
    )

    var isPressed by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 1.dp,
        animationSpec = spring(stiffness = 400f),
        label = "elevation"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "border_color_anim"
    )

    // Компонент обгорнутий в SwipeableListItem для підтримки свайпу
    SwipeableListItem(
        isDragging = false,
        isAnyItemDragging = false,
        swipeEnabled = true, // Свайп увімкнено
        isAnotherItemSwiped = false,
        resetTrigger = 0,
        onSwipeStart = { },
        onDelete = onDelete, // Передаємо дію видалення
        onMoreActionsRequest = { },
        onGoalTransportRequest = { },
        onCopyContentRequest = { },
        backgroundColor = background, // Використовуємо анімований фон
        content = {
            // Вся візуальна частина (Card) вкладена сюди
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .semantics {
                        val title = noteContent.note.title
                        val content = noteContent.note.content
                        contentDescription = if (!title.isNullOrBlank()) "Нотатка: $title" else "Нотатка: $content"
                    },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = background),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
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
                            imageVector = Icons.Outlined.StickyNote2,
                            contentDescription = "Нотатка",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            val note = noteContent.note
                            val hasTitle = !note.title.isNullOrBlank()
                            val hasContent = note.content.isNotBlank()

                            if (hasTitle) {
                                Text(
                                    text = note.title!!,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (hasTitle && hasContent) {
                                Spacer(modifier = Modifier.height(2.dp))
                            }

                            if (hasContent) {
                                Text(
                                    text = note.content.trim(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (isSelected) Int.MAX_VALUE else maxLines,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    endAction()
                }
            }
        }
    )
}