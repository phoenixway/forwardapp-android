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
import com.romankozak.forwardappmobile.ui.screens.backlog.components.EnhancedTagChip
import com.romankozak.forwardappmobile.ui.screens.backlog.components.TagType

@Composable
private fun EnhancedSublistIconBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .semantics { contentDescription = "Підсписок" }
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Gradient background for modern look
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SubdirectoryArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SublistItemRow(
    sublistContent: ListItemContent.SublistItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
    onTagClick: (String) -> Unit = {},
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

            // Badges row with enhanced sublist icon always visible
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (hasExtraContent) 6.dp else 4.dp),
            ) {
                // Always show the sublist icon
                EnhancedSublistIconBadge(
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                // Reminder badge
                sublist.reminderTime?.let { time ->
                    EnhancedReminderBadge(
                        reminderTime = time,
                        currentTimeMillis = currentTimeMillis,
                    )
                }

                // Score status badge
                EnhancedScoreStatusBadge(
                    scoringStatus = sublist.scoringStatus,
                    displayScore = sublist.displayScore
                )

                // Enhanced tags with staggered animation
                if (!sublist.tags.isNullOrEmpty()) {
                    sublist.tags.forEachIndexed { index, tag ->
                        key(tag) {
                            var delayedVisible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(index * 75L) // Staggered animation
                                delayedVisible = true
                            }

                            AnimatedVisibility(
                                visible = delayedVisible,
                                enter = slideInVertically(
                                    initialOffsetY = { height -> -height },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + fadeIn(),
                            ) {
                                EnhancedTagChip(
                                    text = "#$tag",
                                    onClick = { onTagClick("#$tag") },
                                    isDismissible = false,
                                    tagType = TagType.HASHTAG,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }
                }
            }

            // Description with better styling
            if (!sublist.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = sublist.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = if (isSelected) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}