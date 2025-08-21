// --- File: app/src/main/java/com/romankozak/forwardappmobile/ui/components/NoteItemRow.kt ---
package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ListItemContent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItemRow(
    noteContent: ListItemContent.NoteItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        label = "note_color"
    )
    Card(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Text(
            text = noteContent.note.content,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}