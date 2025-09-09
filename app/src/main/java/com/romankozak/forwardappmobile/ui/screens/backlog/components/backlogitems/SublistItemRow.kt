package com.romankozak.forwardappmobile.ui.screens.backlog.components.backlogitems

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.ui.screens.backlog.components.TagChip

/**
 * List icon badge component (status indicator for sublists)
 */
@Composable
private fun SublistIconBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .semantics { contentDescription = "Підсписок" },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.List,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
    }
}

/**
 * A unified, reusable Composable for displaying a sublist item.
 * Matches GoalItem structure, but shows a special status badge.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SublistItemRow(
    sublistContent: ListItemContent.SublistItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
) {
    val sublist = sublistContent.sublist

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EnhancedCustomCheckbox(
            checked = sublist.isCompleted,
            onCheckedChange = onCheckedChange
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .pointerInput(onClick, onLongClick) {
                    detectTapGestures(
                        onLongPress = { onLongClick() },
                        onTap = { onClick() }
                    )
                }
        ) {
            val textColor = if (sublist.isCompleted)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.onSurface
            val textDecoration = if (sublist.isCompleted) TextDecoration.LineThrough else null

            Text(
                text = sublist.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = textColor,
                textDecoration = textDecoration
            )

            val hasExtraContent = true // завжди показуємо рядок статусу для бейджа

            AnimatedVisibility(
                visible = hasExtraContent,
                enter = slideInVertically(
                    initialOffsetY = { height -> -height },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeIn(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // завжди показуємо бейдж підсписку
                        SublistIconBadge(modifier = Modifier.align(Alignment.CenterVertically))

                        if (!sublist.tags.isNullOrEmpty()) {
                            sublist.tags.forEach { tag ->
                                TagChip(
                                    text = "#$tag",
                                    onDismiss = {},
                                    isDismissible = false
                                )
                            }
                        }
                    }
                }
            }

            if (!sublist.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sublist.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = if (isSelected) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
