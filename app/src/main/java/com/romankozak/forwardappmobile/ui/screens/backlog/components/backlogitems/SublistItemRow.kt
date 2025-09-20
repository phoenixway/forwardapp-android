package com.romankozak.forwardappmobile.ui.screens.backlog.components.backlogitems

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
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
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
import com.romankozak.forwardappmobile.ui.screens.backlog.components.TagChip

@Composable
private fun SublistIconBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .semantics { contentDescription = "Підсписок" }
            .padding(2.dp), // Додаємо невеликий внутрішній відступ
        contentAlignment = Alignment.Center,
    ) {
        // Круглий фон для виділення
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SubdirectoryArrowRight, // Більш інтуїтивна іконка!
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer, // Контрастний колір
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

// File: SublistItemRow.kt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SublistItemRow(
    sublistContent: ListItemContent.SublistItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
    currentTimeMillis: Long,
) {
    val sublist = sublistContent.project

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EnhancedCustomCheckbox(
            checked = sublist.isCompleted,
            onCheckedChange = onCheckedChange,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .pointerInput(onClick, onLongClick) {
                        detectTapGestures(
                            onLongPress = { onLongClick() },
                            onTap = { onClick() },
                        )
                    },
        ) {
            val textColor =
                if (sublist.isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            val textDecoration = if (sublist.isCompleted) TextDecoration.LineThrough else null

            Text(
                text = sublist.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = textColor,
                textDecoration = textDecoration,
            )

            val hasExtraContent = !sublist.tags.isNullOrEmpty() ||
                    (sublist.scoringStatus != ScoringStatus.NOT_ASSESSED) ||
                    (sublist.reminderTime != null)

            // ВИПРАВЛЕНО: Ми переносимо бейдж підсписку за межі AnimatedVisibility.
            // Тепер він завжди буде відображатися.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SublistIconBadge(modifier = Modifier.align(Alignment.CenterVertically))

                AnimatedVisibility(
                    visible = hasExtraContent,
                    enter =
                        slideInVertically(
                            initialOffsetY = { height -> -height },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        ) + fadeIn(),
                ) {
                    // Тепер тут тільки ті бейджи, які залежать від наявності додаткового контенту
                    sublist.reminderTime?.let { time ->
                        EnhancedReminderBadge(
                            reminderTime = time,
                            currentTimeMillis = currentTimeMillis,
                        )
                    }

                    EnhancedScoreStatusBadge(
                        scoringStatus = sublist.scoringStatus,
                        displayScore = sublist.displayScore
                    )

                    if (!sublist.tags.isNullOrEmpty()) {
                        sublist.tags.forEach { tag ->
                            TagChip(
                                text = "#$tag",
                                onDismiss = {},
                                isDismissible = false,
                            )
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