package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.backlogitems

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.backlogitems.MarkdownText
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.backlogitems.MarkdownTextActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.backlogitems.MarkdownTextState
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.backlogitems.GoalItemViewModelFactory
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.backlogitems.GoalItemViewModel
import com.romankozak.forwardappmobile.ui.common.rememberParsedText
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurface
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurfaceLayout
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemTokens

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun GoalItem(
    goal: Goal,
    obsidianVaultName: String,
    onCheckedChange: (Boolean) -> Unit,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    onTagClick: (String) -> Unit,
    onRelatedLinkClick: (RelatedLink) -> Unit,
    modifier: Modifier = Modifier,
    showCheckbox: Boolean = false,
    emojiToHide: String? = null,
    contextMarkerToEmojiMap: Map<String, String>,
    isSelected: Boolean,
    reminders: List<Reminder> = emptyList(),
    endAction: @Composable () -> Unit = {},
) {
    val reminder = reminders.firstOrNull()
    val parsedData = rememberParsedText(goal.text, contextMarkerToEmojiMap)
    val viewModel: GoalItemViewModel =
        viewModel(key = goal.hashCode().toString(), factory = GoalItemViewModelFactory(goal, parsedData, reminder))
    val shouldShowStatusIcons by viewModel.shouldShowStatusIcons.collectAsState()

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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(end = 48.dp) // Reserve space for the handle
                            .pointerInput(onItemClick, onLongClick) {
                                detectTapGestures(
                                    onLongPress = { onLongClick() },
                                    onTap = { onItemClick() },
                                )
                            },
                ) {
                    MarkdownText(
                        state =
                            MarkdownTextState(
                                text = parsedData.mainText,
                                style =
                                    MaterialTheme.typography.bodySmall.copy(
                                        lineHeight = 16.sp,
                                        letterSpacing = 0.1.sp,
                                        fontSize = 12.sp,
                                        fontWeight = if (goal.completed) FontWeight.Normal else FontWeight.Medium,
                                    ),
                                isCompleted = goal.completed,
                                obsidianVaultName = obsidianVaultName,
                                maxLines = 4,
                            ),
                        actions =
                            MarkdownTextActions(
                                onTagClick = onTagClick,
                                onTextClick = onItemClick,
                                onLongClick = onLongClick,
                            ),
                    )

                    AnimatedVisibility(
                        visible = shouldShowStatusIcons,
                        enter =
                            slideInVertically(
                                initialOffsetY = { height -> -height },
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            ) + fadeIn(),
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            StatusIconsRow(
                                goal = goal,
                                parsedData = parsedData,
                                reminder = reminder,
                                emojiToHide = emojiToHide,
                                onRelatedLinkClick = onRelatedLinkClick,
                            )
                        }
                    }
                }
            }
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                endAction()
            }
        }
    }
}
