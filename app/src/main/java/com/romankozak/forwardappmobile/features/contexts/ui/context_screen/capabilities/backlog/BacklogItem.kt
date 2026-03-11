package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.backlogitems.MarkdownText
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.backlogitems.StatusIconsRow
import com.romankozak.forwardappmobile.ui.common.rememberParsedText
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurface
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedMetaChip
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemTokens
import sh.calvin.reorderable.ReorderableCollectionItemScope

@Composable
fun BacklogItem(
    item: BacklogItemContent,
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
    isInlineEditing: Boolean,
    onInlineEditSave: (String) -> Unit,
    onInlineEditCancel: () -> Unit,
    onDragStopped: () -> Unit,
) {
    when (item) {
        is BacklogItemContent.GoalItem -> {
            InternalGoalItem(
                goal = item.goal,
                currentContextId = item.backlogItem.contextId,
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
                contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                isInlineEditing = isInlineEditing,
                onInlineEditSave = onInlineEditSave,
                onInlineEditCancel = onInlineEditCancel,
                onDragStopped = onDragStopped,
            )
        }
        is BacklogItemContent.ContextLinkItem -> {
            InternalSubprojectItem(
                subproject = item.project,
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
                contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                onDragStopped = onDragStopped,
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
    currentContextId: String,
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
    contextMarkerToEmojiMap: Map<String, String>,
    isInlineEditing: Boolean,
    onInlineEditSave: (String) -> Unit,
    onInlineEditCancel: () -> Unit,
    onDragStopped: () -> Unit,
) {
    val parsedData = rememberParsedText(goal.text, contextMarkerToEmojiMap)
    val hapticFeedback = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var textValue by remember(goal.id, isInlineEditing, goal.text) {
        mutableStateOf(TextFieldValue(goal.text, TextRange(goal.text.length)))
    }

    LaunchedEffect(isInlineEditing) {
        if (isInlineEditing) {
            focusRequester.requestFocus()
        }
    }

    val completedColors =
        BacklogCompletedColors(
            containerStart = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            containerEnd = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.12f),
            border = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
            iconTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            badgeBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            badgeText = MaterialTheme.colorScheme.primary,
        )

    UnifiedListItemSurface(
        isSelected = isSelected,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    vertical = UnifiedListItemTokens.OuterVerticalSpacing,
                    horizontal = UnifiedListItemTokens.OuterHorizontalSpacing,
                )
                .combinedClickable(
                    onClick = {
                        if (!isInlineEditing) {
                            onItemClick()
                        }
                    },
                    onLongClick = {
                        if (!isInlineEditing) {
                            onLongClick()
                        }
                    },
                ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier =
                if (goal.completed) {
                    Modifier
                        .background(
                            brush =
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            completedColors.containerStart,
                                            completedColors.containerEnd,
                                        ),
                                ),
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                } else {
                    Modifier
                        .background(Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showCheckbox) {
                    Checkbox(
                        checked = goal.completed,
                        onCheckedChange = onCheckedChange,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (isInlineEditing) {
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                            minLines = 1,
                            maxLines = 4,
                            singleLine = false,
                            keyboardOptions =
                                KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Done,
                                ),
                            keyboardActions =
                                androidx.compose.foundation.text.KeyboardActions(
                                    onDone = {
                                        keyboardController?.hide()
                                        onInlineEditSave(textValue.text)
                                    },
                                ),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    onInlineEditCancel()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Скасувати",
                                )
                            }
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    onInlineEditSave(textValue.text)
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Зберегти",
                                )
                            }
                        }
                    } else {
                        Box(modifier = if (goal.completed) Modifier.alpha(0.65f) else Modifier) {
                            MarkdownText(
                                text = parsedData.mainText,
                                isCompleted = goal.completed,
                                obsidianVaultName = "",
                                onTagClick = {},
                                onTextClick = onItemClick,
                                onLongClick = onLongClick,
                                maxLines = 4,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    BacklogTypeBadge(
                        icon = Icons.Default.Flag,
                        label = "Ціль",
                        tint = if (goal.completed) completedColors.iconTint else MaterialTheme.colorScheme.primary,
                        modifier = if (goal.completed) Modifier.alpha(0.7f) else Modifier,
                    )

                    val reminder = reminders.firstOrNull()
                    val shouldShowStatusIcons =
                        !isInlineEditing && ((goal.scoringStatus != ScoringStatusValues.NOT_ASSESSED) ||
                            (reminder != null) ||
                            (parsedData.icons.isNotEmpty()) ||
                            (!goal.description.isNullOrBlank()) ||
                            (!goal.relatedLinks.isNullOrEmpty()))

                    AnimatedVisibility(
                        visible = shouldShowStatusIcons,
                        enter = slideInVertically(animationSpec = spring()) { -it } + fadeIn(),
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = if (goal.completed) Modifier.alpha(0.6f) else Modifier) {
                                StatusIconsRow(
                                    goal = goal,
                                    currentContextId = currentContextId,
                                    parsedData = parsedData,
                                    reminder = reminder,
                                    emojiToHide = null,
                                    onRelatedLinkClick = onRelatedLinkClick,
                                )
                            }
                        }
                    }
                }

                if (!isInlineEditing) {
                    IconButton(
                        modifier =
                            with(reorderableScope) {
                                Modifier.draggableHandle(
                                    onDragStarted = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = { onDragStopped() },
                                )
                            }.alpha(if (goal.completed) 0.5f else 1f),
                        onClick = onMoreClick,
                    ) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More actions")
                    }
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
private fun BacklogTypeBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        UnifiedMetaChip(
            text = label,
            icon = icon,
            contentColor = tint,
        )
    }
}

@Composable
private fun CompletedBadge(
    backgroundColor: Color,
    textColor: Color,
) {
    // Deprecated badge removed intentionally
}

@Composable
private fun InternalSubprojectItem(
    subproject: Context,
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
    contextMarkerToEmojiMap: Map<String, String>,
    onDragStopped: () -> Unit,
) {
    val parsedData = rememberParsedText(subproject.name, contextMarkerToEmojiMap)
    val tagContextIcons =
        remember(subproject.tags, contextMarkerToEmojiMap) {
            subproject.tags.orEmpty().mapNotNull { rawTag ->
                val normalized = rawTag.trim().removePrefix("#").removePrefix("@").lowercase()
                listOf("@$normalized", "#$normalized", normalized).firstNotNullOfOrNull { candidate ->
                    contextMarkerToEmojiMap[candidate]
                }
            }
        }
    val enrichedParsedData =
        remember(parsedData, tagContextIcons) {
            if (tagContextIcons.isEmpty()) {
                parsedData
            } else {
                parsedData.copy(
                    icons = (parsedData.icons + tagContextIcons).distinct(),
                )
            }
        }
    val hapticFeedback = LocalHapticFeedback.current

    val completedColors =
        BacklogCompletedColors(
            containerStart = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f),
            containerEnd = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.12f),
            border = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
            iconTint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
            badgeBackground = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
            badgeText = MaterialTheme.colorScheme.secondary,
        )

    UnifiedListItemSurface(
        isSelected = isSelected,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    vertical = UnifiedListItemTokens.OuterVerticalSpacing,
                    horizontal = UnifiedListItemTokens.OuterHorizontalSpacing,
                )
                .combinedClickable(
                    onClick = onItemClick,
                    onLongClick = onLongClick,
                ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier =
                if (subproject.isCompleted) {
                    Modifier
                        .background(
                            brush =
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            completedColors.containerStart,
                                            completedColors.containerEnd,
                                        ),
                                ),
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                } else {
                    Modifier
                        .background(Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showCheckbox) {
                    Checkbox(
                        checked = subproject.isCompleted,
                        onCheckedChange = onCheckedChange,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(
                    modifier =
                        Modifier
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
                            textColor = completedColors.badgeText,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = enrichedParsedData.mainText,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (subproject.isCompleted) TextDecoration.LineThrough else null,
                        modifier = if (subproject.isCompleted) Modifier.alpha(0.65f) else Modifier,
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    BacklogTypeBadge(
                        icon = Icons.Default.AccountTree,
                        label = "Підконтекст",
                        tint = if (subproject.isCompleted) completedColors.iconTint else MaterialTheme.colorScheme.secondary,
                        modifier = if (subproject.isCompleted) Modifier.alpha(0.7f) else Modifier,
                    )

                    val reminder = reminders.firstOrNull()
                    val shouldShowStatusIcons =
                        (subproject.scoringStatus != ScoringStatusValues.NOT_ASSESSED) ||
                            (reminder != null) ||
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
                                    reminder = reminder,
                                    emojiToHide = null,
                                    onRelatedLinkClick = onRelatedLinkClick,
                                )
                            }
                        }
                    }
                }

                IconButton(
                    modifier =
                        with(reorderableScope) {
                            Modifier.draggableHandle(
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = { onDragStopped() },
                            )
                        }.alpha(if (subproject.isCompleted) 0.5f else 1f),
                    onClick = onMoreClick,
                ) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More actions")
                }
            }
        }
    }
}
