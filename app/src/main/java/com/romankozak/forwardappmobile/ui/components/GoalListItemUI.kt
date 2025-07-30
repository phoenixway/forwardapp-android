// GoalListItemUI.kt
package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.GoalList
import androidx.compose.foundation.clickable // Додайте цей імпорт
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState // ✅ 1. Додайте цей імпорт
import androidx.compose.runtime.getValue // ✅ 2. Додайте цей імпорт

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
    isPressed: Boolean,
    modifier: Modifier = Modifier
) {
    // ✅ 3. Анімуємо зміну кольору
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed && !isCurrentlyDragging) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        } else {
            Color.Transparent
        },
        label = "background_animation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = (level * 24).dp)
    ) {
        if (isHovered && !isDraggingDown && !isCurrentlyDragging) {
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor) // ✅ 4. Застосовуємо анімований колір
                .clickable { onListClick(list.id) }
                .alpha(if (isCurrentlyDragging) 0.6f else 1f)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ... решта коду Row без змін
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

        if (isHovered && isDraggingDown && !isCurrentlyDragging) {
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
        }
    }
}