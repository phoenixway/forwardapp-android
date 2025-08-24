// File: app/src/main/java/com/romankozak/forwardappmobile/ui/components/listItemsRenderers/NoteItemRow.kt
package com.romankozak.forwardappmobile.ui.components.listItemsRenderers

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.ListItemContent

@Composable
fun NoteItemRow(
    noteContent: ListItemContent.NoteItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    maxLines: Int = 3,
) {
    val background by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = spring(),
        label = "note_background_color"
    )

    var isPressed by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 1.dp,
        label = "elevation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .semantics {
                val title = noteContent.note.title
                val content = noteContent.note.content
                contentDescription = if (!title.isNullOrBlank()) "Нотатка: $title" else "Нотатка: $content"
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.elevatedCardElevation(elevation)
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
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon styled to match the checkbox size
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Transparent)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.StickyNote2,
                        contentDescription = "Нотатка",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(10.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val note = noteContent.note
                    if (!note.title.isNullOrBlank()) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    Text(
                        text = note.content.trim(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 16.sp,
                            letterSpacing = 0.1.sp,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = if (isSelected) Int.MAX_VALUE else maxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Drag handle identical to GoalItem
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 4.dp),
                color = Color.Transparent
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.semantics { contentDescription = "Перетягнути нотатку" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = dragHandleModifier
                                .size(24.dp)
                                .padding(4.dp)
                                .pointerInput(Unit) { detectTapGestures { } }
                        )
                    }
                }
            }
        }
    }
}