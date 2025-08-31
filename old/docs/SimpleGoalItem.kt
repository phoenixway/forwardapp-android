package com.romankozak.forwardappmobile.ui.components.listItemsRenderers

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ListItemContent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SimpleGoalItem(
    modifier: Modifier = Modifier,
    goalContent: ListItemContent.GoalItem,
    isDragging: Boolean,
    onToggle: (Boolean) -> Unit,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
) {
    val goal = goalContent.goal
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Чекбокс для відмітки виконання
        Checkbox(
            checked = goal.completed,
            onCheckedChange = onToggle,
        )

        Spacer(Modifier.width(16.dp))

        // Текст цілі
        Text(
            text = goal.text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (goal.completed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (goal.completed) TextDecoration.LineThrough else TextDecoration.None,
            overflow = TextOverflow.Ellipsis
        )

        // Ручка для перетягування
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Перетягнути",
            modifier = Modifier
                .padding(start = 16.dp)
                .then(dragHandleModifier), // Застосовуємо переданий модифікатор
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}