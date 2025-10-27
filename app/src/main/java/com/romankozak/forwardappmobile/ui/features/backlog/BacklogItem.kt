package com.romankozak.forwardappmobile.ui.features.backlog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableCollectionItemScope
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.data.database.models.ScoringStatusValues
import com.romankozak.forwardappmobile.ui.common.rememberParsedText
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.MarkdownText
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.StatusIconsRow
import com.romankozak.forwardappmobile.data.database.models.RelatedLink

@Composable
fun BacklogItem(
    item: ListItemContent,
    reorderableScope: ReorderableCollectionItemScope,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    showCheckbox: Boolean,
    isSelected: Boolean,
    onRelatedLinkClick: (RelatedLink) -> Unit,
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
                showCheckbox = showCheckbox,
                isSelected = isSelected,
                onRelatedLinkClick = onRelatedLinkClick
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
                showCheckbox = showCheckbox,
                isSelected = isSelected
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
    showCheckbox: Boolean,
    isSelected: Boolean,
    onRelatedLinkClick: (RelatedLink) -> Unit
) {
    val parsedData = rememberParsedText(goal.text, emptyMap()) // Simplified
    val hapticFeedback = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        tonalElevation = if (isSelected) 3.dp else 1.dp,
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        },
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
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
                    MarkdownText(
                        text = parsedData.mainText,
                        isCompleted = goal.completed,
                        obsidianVaultName = "", // Simplified
                        onTagClick = {}, // Simplified
                        onTextClick = onItemClick,
                        onLongClick = onLongClick,
                        maxLines = 4
                    )

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
                            StatusIconsRow(
                                goal = goal,
                                parsedData = parsedData,
                                reminder = reminder,
                                emojiToHide = null, // Simplified
                                onRelatedLinkClick = onRelatedLinkClick
                            )
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
                    },
                    onClick = onMoreClick
                ) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More actions")
                }
            }
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
    showCheckbox: Boolean,
    isSelected: Boolean
) {
    val parsedData = rememberParsedText(subproject.name, emptyMap()) // Simplified
    val hapticFeedback = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        tonalElevation = if (isSelected) 3.dp else 1.dp,
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        },
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
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
                    Text(
                        text = parsedData.mainText,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (subproject.isCompleted) TextDecoration.LineThrough else null
                    )
                }

                IconButton(
                    modifier = with(reorderableScope) {
                        Modifier.longPressDraggableHandle(
                            onDragStarted = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    },
                    onClick = onMoreClick
                ) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More actions")
                }
            }
        }
    }
}