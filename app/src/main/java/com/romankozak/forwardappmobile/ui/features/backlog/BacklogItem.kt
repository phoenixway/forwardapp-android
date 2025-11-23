package com.romankozak.forwardappmobile.ui.features.backlog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.data.database.models.ScoringStatusValues
import com.romankozak.forwardappmobile.ui.common.rememberParsedText
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.MarkdownText
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.StatusIconsRow
import sh.calvin.reorderable.ReorderableCollectionItemScope

@Composable
fun BacklogItem(
    item: ListItemContent,
    reorderableScope: ReorderableCollectionItemScope,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit,
                onLongClick: () -> Unit,
                onMoreClick: () -> Unit,
                onCheckedChange: (Boolean) -> Unit,
                onRelatedLinkClick: (RelatedLink) -> Unit,
                showCheckbox: Boolean,
                isSelected: Boolean,
                contextMarkerToEmojiMap: Map<String, String>,
) {
    when (item) {
        is ListItemContent.GoalItem -> {
            InternalGoalItem(
                goal = item.goal,
                reminders = item.reminders,
                reorderableScope = reorderableScope,
                modifier = modifier,
                onItemClick = onItemClick,
                onLongClick = onLongClick,
                onMoreClick = onMoreClick,
                onCheckedChange = onCheckedChange,
                onRelatedLinkClick = onRelatedLinkClick,
                showCheckbox = showCheckbox,
                isSelected = isSelected,
                contextMarkerToEmojiMap = contextMarkerToEmojiMap
            )
        }
        is ListItemContent.SublistItem -> {
            InternalSubprojectItem(
                subproject = item.project,
                reorderableScope = reorderableScope,
                modifier = modifier,
                onItemClick = onItemClick,
                onLongClick = onLongClick,
                onMoreClick = onMoreClick,
                onCheckedChange = onCheckedChange,
                onRelatedLinkClick = onRelatedLinkClick,
                showCheckbox = showCheckbox,
                isSelected = isSelected,
                contextMarkerToEmojiMap = contextMarkerToEmojiMap
            )
        }
        else -> {
            // Do nothing for other types for now
        }
    }
}

@Composable
private fun InternalGoalItem(
    goal: Goal,
    reminders: List<Reminder>,
    reorderableScope: ReorderableCollectionItemScope,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit,
                             onLongClick: () -> Unit,
                             onMoreClick: () -> Unit,
                             onCheckedChange: (Boolean) -> Unit,
                             onRelatedLinkClick: (RelatedLink) -> Unit,
                             showCheckbox: Boolean,
                             isSelected: Boolean,
                             contextMarkerToEmojiMap: Map<String, String>
) {
    val parsedData = rememberParsedText(goal.text, contextMarkerToEmojiMap)
    val hapticFeedback = LocalHapticFeedback.current

    val completedColors = BacklogCompletedColors(
        containerStart = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                                 containerEnd = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.12f),
                                                 border = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                                 iconTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                 badgeBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                 badgeText = MaterialTheme.colorScheme.primary,
    )

    Surface(
        modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp, horizontal = 8.dp)
        .combinedClickable(
            onClick = onItemClick,
            onLongClick = onLongClick,
        ),
        shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            shadowElevation = if (isSelected) 4.dp else if (goal.completed) 0.dp else 1.dp,
            tonalElevation = if (isSelected) 3.dp else 0.dp,
            border = if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            } else if (goal.completed) {
                BorderStroke(1.5.dp, completedColors.border)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            },
    ) {
        Box(
            modifier = if (goal.completed) {
                Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            completedColors.containerStart,
                            completedColors.containerEnd
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
            } else {
                Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp)
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showCheckbox) {
                    Checkbox(
                        checked = goal.completed,
                        onCheckedChange = onCheckedChange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Icon(
                    imageVector = Icons.Default.Flag,
                     contentDescription = "Goal",
                     tint = if (goal.completed) completedColors.iconTint else MaterialTheme.colorScheme.primary,
                     modifier = Modifier
                     .size(24.dp)
                     .alpha(if (goal.completed) 0.6f else 1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (goal.completed) {
                        CompletedBadge(
                            backgroundColor = completedColors.badgeBackground,
                            textColor = completedColors.badgeText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Box(modifier = if (goal.completed) Modifier.alpha(0.65f) else Modifier) {
                        MarkdownText(
                            text = parsedData.mainText,
                            isCompleted = goal.completed,
                            obsidianVaultName = "",
                            onTagClick = {},
                            onTextClick = onItemClick,
                            onLongClick = onLongClick,
                            maxLines = 4
                        )
                    }

                    val reminder = reminders.firstOrNull()
                    val shouldShowStatusIcons =
                    (goal.scoringStatus != ScoringStatusValues.NOT_ASSESSED) ||
                    (reminder != null) ||
                    (parsedData.icons.isNotEmpty()) ||
                    (!goal.description.isNullOrBlank()) ||
                    (!goal.relatedLinks.isNullOrEmpty())

                    AnimatedVisibility(
                        visible = shouldShowStatusIcons,
                        enter = slideInVertically(animationSpec = spring()) { -it } + fadeIn(),
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = if (goal.completed) Modifier.alpha(0.6f) else Modifier) {
                                StatusIconsRow(
                                    goal = goal,
                                    parsedData = parsedData,
                                    reminder = reminder,
                                    emojiToHide = null,
                                    onRelatedLinkClick = onRelatedLinkClick
                                )
                            }
                        }
                    }
                }

                IconButton(
                    modifier = with(reorderableScope) {
                        Modifier.longPressDraggableHandle(
                            onDragStarted = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }.alpha(if (goal.completed) 0.5f else 1f),
                           onClick = onMoreClick
                ) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More actions")
                }
            }
        }
    }
}

private data class BacklogCompletedColors(
    val containerStart: Color,
    val containerEnd: Color,
    val border: Color,
    val iconTint: Color,
    val badgeBackground: Color,
    val badgeText: Color,
)

@Composable
private fun CompletedBadge(
    backgroundColor: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
            color = backgroundColor,
            modifier = Modifier.padding(bottom = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                 contentDescription = null,
                 tint = textColor,
                 modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Виконано",
                 style = MaterialTheme.typography.labelSmall.copy(
                     fontSize = 11.sp,
                     fontWeight = FontWeight.Medium
                 ),
                 color = textColor
            )
        }
    }
}

@Composable
private fun InternalSubprojectItem(
    subproject: Project,
    reorderableScope: ReorderableCollectionItemScope,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit,
                                   onLongClick: () -> Unit,
                                   onMoreClick: () -> Unit,
                                   onCheckedChange: (Boolean) -> Unit,
                                   onRelatedLinkClick: (RelatedLink) -> Unit,
                                   showCheckbox: Boolean,
                                   isSelected: Boolean,
                                   contextMarkerToEmojiMap: Map<String, String>
) {
    val parsedData = rememberParsedText(subproject.name, contextMarkerToEmojiMap)
    val tagContextIcons = remember(subproject.tags, contextMarkerToEmojiMap) {
        subproject.tags.orEmpty().mapNotNull { rawTag ->
            val normalized = rawTag.trim().removePrefix("#").removePrefix("@").lowercase()
            listOf("@$normalized", "#$normalized", normalized).firstNotNullOfOrNull { candidate ->
                contextMarkerToEmojiMap[candidate]
            }
        }
    }
    val enrichedParsedData = remember(parsedData, tagContextIcons) {
        if (tagContextIcons.isEmpty()) parsedData else parsedData.copy(
            icons = (parsedData.icons + tagContextIcons).distinct()
        )
    }
    val hapticFeedback = LocalHapticFeedback.current

    val completedColors = BacklogCompletedColors(
        containerStart = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f),
                                                 containerEnd = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.12f),
                                                 border = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                                                 iconTint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                                 badgeBackground = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                                 badgeText = MaterialTheme.colorScheme.secondary,
    )

    Surface(
        modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp, horizontal = 8.dp)
        .combinedClickable(
            onClick = onItemClick,
            onLongClick = onLongClick,
        ),
        shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            shadowElevation = if (isSelected) 4.dp else if (subproject.isCompleted) 0.dp else 1.dp,
            tonalElevation = if (isSelected) 3.dp else 0.dp,
            border = if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            } else if (subproject.isCompleted) {
                BorderStroke(1.5.dp, completedColors.border)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            },
    ) {
        Box(
            modifier = if (subproject.isCompleted) {
                Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            completedColors.containerStart,
                            completedColors.containerEnd
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
            } else {
                Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp)
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showCheckbox) {
                    Checkbox(
                        checked = subproject.isCompleted,
                        onCheckedChange = onCheckedChange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Icon(
                    imageVector = Icons.Default.AccountTree,
                     contentDescription = "Subproject",
                     tint = if (subproject.isCompleted) completedColors.iconTint else MaterialTheme.colorScheme.secondary,
                     modifier = Modifier
                     .size(24.dp)
                     .alpha(if (subproject.isCompleted) 0.6f else 1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier
                    .weight(1f)
                    .pointerInput(onItemClick, onLongClick) {
                        detectTapGestures(
                            onLongPress = { onLongClick() },
                                          onTap = { onItemClick() },
                        )
                    },
                ) {
                    if (subproject.isCompleted) {
                        CompletedBadge(
                            backgroundColor = completedColors.badgeBackground,
                            textColor = completedColors.badgeText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = enrichedParsedData.mainText,
                         style = MaterialTheme.typography.bodyLarge,
                         maxLines = 1,
                         overflow = TextOverflow.Ellipsis,
                         textDecoration = if (subproject.isCompleted) TextDecoration.LineThrough else null,
                         modifier = if (subproject.isCompleted) Modifier.alpha(0.65f) else Modifier
                    )

                    val shouldShowStatusIcons =
                    (subproject.scoringStatus != ScoringStatusValues.NOT_ASSESSED) ||
                    (enrichedParsedData.icons.isNotEmpty()) ||
                    (!subproject.description.isNullOrBlank()) ||
                    (!subproject.relatedLinks.isNullOrEmpty())

                    AnimatedVisibility(
                        visible = shouldShowStatusIcons,
                        enter = slideInVertically(animationSpec = spring()) { -it } + fadeIn(),
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = if (subproject.isCompleted) Modifier.alpha(0.6f) else Modifier) {
                                StatusIconsRow(
                                    project = subproject,
                                    parsedData = enrichedParsedData,
                                    reminder = null,
                                    emojiToHide = null,
                                    onRelatedLinkClick = onRelatedLinkClick
                                )
                            }
                        }
                    }
                }

                IconButton(
                    modifier = with(reorderableScope) {
                        Modifier.longPressDraggableHandle(
                            onDragStarted = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }.alpha(if (subproject.isCompleted) 0.5f else 1f),
                           onClick = onMoreClick
                ) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More actions")
                }
            }
        }
    }
}
