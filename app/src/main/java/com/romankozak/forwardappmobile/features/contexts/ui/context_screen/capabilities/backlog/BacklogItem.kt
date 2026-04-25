package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.GoalStatusValues
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.backlogitems.MarkdownText
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.backlogitems.MarkdownTextActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.backlogitems.MarkdownTextState
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.backlogitems.EnhancedReminderBadge
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.backlogitems.StatusIconsRow
import com.romankozak.forwardappmobile.ui.common.rememberParsedText
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurface
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurfaceLayout
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemTokens
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableCollectionItemScope

private const val BACKLOG_SWIPE_DEBUG_TAG = "BACKLOG_SWIPE_DEBUG"

@Composable
fun BacklogItem(
    item: BacklogItemContent,
    reorderableScope: ReorderableCollectionItemScope,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit,
    onTagClick: (String) -> Unit,
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
                onTagClick = onTagClick,
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
    onTagClick: (String) -> Unit,
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
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var textValue by remember(goal.id, isInlineEditing, goal.text) {
        mutableStateOf(TextFieldValue(goal.text, TextRange(goal.text.length)))
    }

    LaunchedEffect(isInlineEditing) {
        if (isInlineEditing) {
            focusRequester.requestFocus()
            delay(250)
            bringIntoViewRequester.bringIntoView()
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
    val inWorkColors =
        BacklogCompletedColors(
            containerStart = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.74f),
            containerEnd = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
            border = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.96f),
            iconTint = MaterialTheme.colorScheme.tertiary,
            badgeBackground = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f),
            badgeText = MaterialTheme.colorScheme.tertiary,
        )
    val isInWork = goal.goalStatus == GoalStatusValues.IN_WORK

    UnifiedListItemSurface(
        isSelected = isSelected,
        layout =
            UnifiedListItemSurfaceLayout(
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
            ),
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
                } else if (isInWork) {
                    Modifier
                        .background(
                            brush =
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            inWorkColors.containerStart,
                                            inWorkColors.containerEnd,
                                            inWorkColors.containerStart.copy(alpha = 0.34f),
                                        ),
                                ),
                        )
                        .background(
                            color = inWorkColors.border.copy(alpha = 0.08f),
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
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    text = "Редагування",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    onInlineEditCancel()
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Скасувати",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    onInlineEditSave(textValue.text)
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Зберегти",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewRequester(bringIntoViewRequester)
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
                    } else {
                        Box(
                            modifier =
                                when {
                                    goal.completed -> Modifier.alpha(0.65f)
                                    else -> Modifier
                                },
                        ) {
                            MarkdownText(
                                state =
                                    MarkdownTextState(
                                        text = parsedData.mainText,
                                        style =
                                            if (isInWork) {
                                                MaterialTheme.typography.bodyLarge.copy(
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            } else {
                                                MaterialTheme.typography.bodyLarge
                                            },
                                        isCompleted = goal.completed,
                                        maxLines = 4,
                                    ),
                                actions =
                                    MarkdownTextActions(
                                        onTagClick = onTagClick,
                                        onTextClick = onItemClick,
                                        onLongClick = onLongClick,
                                    ),
                            )
                        }
                    }

                    val reminder = reminders.firstOrNull()
                    val shouldShowStatusIcons =
                        !isInlineEditing && ((goal.scoringStatus != ScoringStatusValues.NOT_ASSESSED) ||
                            (goal.relativeSize > 0) ||
                            (parsedData.icons.isNotEmpty()) ||
                            (!goal.description.isNullOrBlank()) ||
                            (!goal.relatedLinks.isNullOrEmpty()))

                    if (!isInlineEditing) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            reminder?.let {
                                Box(
                                    modifier =
                                        when {
                                            goal.completed -> Modifier.alpha(0.6f)
                                            else -> Modifier
                                        },
                                ) {
                                    EnhancedReminderBadge(reminder = it)
                                }
                                if (shouldShowStatusIcons) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                            if (shouldShowStatusIcons) {
                                Box(
                                    modifier =
                                        when {
                                            goal.completed -> Modifier.alpha(0.6f)
                                            else -> Modifier
                                        },
                                ) {
                                    StatusIconsRow(
                                        goal = goal,
                                        currentContextId = currentContextId,
                                        parsedData = parsedData,
                                        reminder = null,
                                        emojiToHide = null,
                                        onRelatedLinkClick = onRelatedLinkClick,
                                    )
                                }
                            }
                        }
                    }
                }
                if (!isInlineEditing) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        modifier =
                            with(reorderableScope) {
                                Modifier.draggableHandle(
                                    onDragStarted = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = { onDragStopped() },
                                )
                            }
                                .size(18.dp)
                                .alpha(
                                    when {
                                        goal.completed -> 0.42f
                                        isInWork -> 0.96f
                                        else -> 0.78f
                                    },
                                )
                                .clickable(onClick = onMoreClick),
                    )
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
        layout =
            UnifiedListItemSurfaceLayout(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = UnifiedListItemTokens.OuterVerticalSpacing,
                            horizontal = UnifiedListItemTokens.OuterHorizontalSpacing,
                        ),
                contentPadding = PaddingValues(0.dp),
            ),
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
                        onCheckedChange = {
                            Log.d(
                                BACKLOG_SWIPE_DEBUG_TAG,
                                "subcontextCheckbox itemId=${subproject.id} checked=$it",
                            )
                            onCheckedChange(it)
                        },
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .combinedClickable(
                                onClick = {
                                    Log.d(
                                        BACKLOG_SWIPE_DEBUG_TAG,
                                        "subcontextTap itemId=${subproject.id} name=${subproject.name}",
                                    )
                                    onItemClick()
                                },
                                onLongClick = {
                                    Log.d(
                                        BACKLOG_SWIPE_DEBUG_TAG,
                                        "subcontextLongTap itemId=${subproject.id} name=${subproject.name}",
                                    )
                                    onLongClick()
                                },
                            ),
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
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic,
                            ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (subproject.isCompleted) TextDecoration.LineThrough else null,
                        modifier = if (subproject.isCompleted) Modifier.alpha(0.65f) else Modifier,
                    )

                    val reminder = reminders.firstOrNull()
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
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More actions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    modifier =
                        with(reorderableScope) {
                            Modifier.draggableHandle(
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = { onDragStopped() },
                            )
                        }
                            .size(18.dp)
                            .alpha(if (subproject.isCompleted) 0.42f else 0.78f)
                            .clickable(
                                onClick = {
                                    Log.d(
                                        BACKLOG_SWIPE_DEBUG_TAG,
                                        "subcontextMoreTap itemId=${subproject.id}",
                                    )
                                    onMoreClick()
                                },
                            ),
                )
            }
        }
    }
}
