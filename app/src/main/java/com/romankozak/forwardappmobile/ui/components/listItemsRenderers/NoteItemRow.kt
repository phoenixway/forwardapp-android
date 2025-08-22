// File: app/src/main/java/com/romankozak/forwardappmobile/ui/components/listItemsRenderers/NoteItemRow.kt
package com.romankozak.forwardappmobile.ui.components.listItemsRenderers

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItemRow(
    noteContent: ListItemContent.NoteItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    showTimestamp: Boolean = true,
    maxLines: Int = 3,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = spring(),
        label = "note_background_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.elevatedCardElevation(1.dp)
    ) {
        // --- ПОЧАТОК ЗМІН: Ідентична структура Row, як у GoalItem ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ЗОНА 1: Клікабельний контент
            Row(
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(onClick, onLongClick) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onLongPress = { onLongClick() }
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.StickyNote2,
                    contentDescription = "Note",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val note = noteContent.note
                    note.title?.let { title ->
                        if (title.isNotBlank()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }

                    Text(
                        text = note.content.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = if (isSelected) Int.MAX_VALUE else maxLines,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (showTimestamp && note.updatedAt != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatTimestamp(note.updatedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // ЗОНА 2: Ручка для перетягування
            Box(
                modifier = Modifier.padding(end = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Перетягнути нотатку",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = dragHandleModifier
                        .size(36.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = { /* поглинаємо жест */ })
                        }
                )
            }
        }
        // --- КІНЕЦЬ ЗМІН ---
    }
}

/**
 * Format timestamp for display
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now" // Less than 1 minute
        diff < 3600_000 -> "${diff / 60_000}m ago" // Less than 1 hour
        diff < 86400_000 -> "${diff / 3600_000}h ago" // Less than 1 day
        diff < 2592000_000 -> "${diff / 86400_000}d ago" // Less than 30 days
        else -> {
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}

/**
 * Preview-friendly version of NoteItemRow for testing
 */
@Composable
fun NoteItemRowPreview(
    content: String,
    title: String? = null,
    isSelected: Boolean = false,
    isDragging: Boolean = false,
    showTimestamp: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val mockNote = com.romankozak.forwardappmobile.data.database.models.Note(
        id = "preview-note",
        title = title,
        content = content,
        createdAt = System.currentTimeMillis() - 3600_000, // 1 hour ago
        updatedAt = System.currentTimeMillis() - 1800_000  // 30 minutes ago
    )

    val mockListItem = com.romankozak.forwardappmobile.data.database.models.ListItem(
        id = "preview-item",
        listId = "preview-list",
        itemType = com.romankozak.forwardappmobile.data.database.models.ListItemType.NOTE,
        entityId = "preview-note",
        order = 0
    )

    val noteContent = ListItemContent.NoteItem(
        note = mockNote,
        item = mockListItem
    )

    NoteItemRow(
        noteContent = noteContent,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        //isDragging = isDragging,
        showTimestamp = showTimestamp
    )
}