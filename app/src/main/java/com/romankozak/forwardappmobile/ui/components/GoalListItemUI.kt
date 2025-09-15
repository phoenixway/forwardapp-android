// File: com/romankozak/forwardappmobile/ui/components/GoalListItemUI.kt
// ПОВНА ОНОВЛЕНА ВЕРСІЯ

package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterCenterFocus
import androidx.compose.material.icons.outlined.ZoomInMap
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
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
    // --- ЗМІНИ ---
    showFocusButton: Boolean, // Новий параметр для контролю видимості кнопки
    onFocusRequested: (list: GoalList) -> Unit, // Нова дія для кнопки
    // ---
    modifier: Modifier = Modifier,
    displayName: AnnotatedString? = null,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent,
        animationSpec = tween(durationMillis = 500),
        label = "Highlight Animation",
    )

    // Відступ тепер застосовується до самого Row, а не до Column
    val indentation = (level * 24).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundColor)
    ) {
        if (isHovered && !isDraggingDown && !isCurrentlyDragging) {
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = indentation))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onListClick(list.id) }
                .alpha(if (isCurrentlyDragging) 0.6f else 1f)
                .padding(start = indentation) // Відступ тут
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Іконка розгортання/згортання
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (hasChildren) {
                    IconButton(onClick = { onToggleExpanded(list) }) {
                        Icon(
                            imageVector = if (list.isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = "Згорнути/Розгорнути",
                        )
                    }
                }
            }

            Text(
                text = displayName ?: AnnotatedString(list.name),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )

            // --- ЗМІНА: Нова іконка для фокусування ---
            AnimatedVisibility(visible = showFocusButton, enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = { onFocusRequested(list) }) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Сфокусуватися",
                        tint = MaterialTheme.colorScheme.secondary // <-- ЗАМІНІТЬ ЦЕ ЗНАЧЕННЯ
                 )
                }
            }
            // ---

            // Іконка "Більше"
            IconButton(onClick = { onMenuRequested(list) }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Дії зі списком")
            }
        }

        if (isHovered && isDraggingDown && !isCurrentlyDragging) {
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = indentation))
        }
    }
}