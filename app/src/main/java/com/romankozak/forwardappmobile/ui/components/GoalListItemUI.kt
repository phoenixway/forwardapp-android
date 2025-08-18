package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.GoalList

@Composable
fun GoalListRow(
    list: GoalList,
    level: Int,
    hasChildren: Boolean,
    onListClick: (String) -> Unit,
    onToggleExpanded: (list: GoalList) -> Unit,
    onMenuRequested: (list: GoalList) -> Unit,
    isCurrentlyDragging: Boolean,
    isHovered: Boolean,
    isDraggingDown: Boolean,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    // ✨ ПОВЕРНУЛИ АНІМАЦІЮ: Плавна зміна фону для підсвітки
    val backgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 500),
        label = "Highlight Animation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundColor)
            .padding(start = (level * 24).dp)
    ) {
        // Індикатор зверху
        if (isHovered && !isDraggingDown && !isCurrentlyDragging) {
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onListClick(list.id) }
                .alpha(if (isCurrentlyDragging) 0.6f else 1f)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasChildren) {
                IconButton(onClick = { onToggleExpanded(list) }) {
                    Icon(
                        imageVector = if (list.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                        contentDescription = "Згорнути/Розгорнути"
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            Text(
                text = list.name,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )

            IconButton(onClick = { onMenuRequested(list) }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Дії зі списком")
            }
        }

        // Індикатор знизу
        if (isHovered && isDraggingDown && !isCurrentlyDragging) {
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
        }
    }
}