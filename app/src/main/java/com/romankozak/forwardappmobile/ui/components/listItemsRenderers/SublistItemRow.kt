// --- File: app/src/main/java/com/romankozak/forwardappmobile/ui/components/SublistItemRow.kt ---
package com.romankozak.forwardappmobile.ui.components.listItemsRenderers

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ListItemContent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SublistItemRow(
    sublistContent: ListItemContent.SublistItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        label = "sublist_color"
    )
    Card(
        //modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.List, "Під-список", tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(16.dp))
            Text(
                text = sublistContent.sublist.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}