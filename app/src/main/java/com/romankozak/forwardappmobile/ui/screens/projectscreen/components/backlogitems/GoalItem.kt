package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems

import androidx.compose.material.icons.filled.Flag
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.ui.common.rememberParsedText
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.StatusIconsRow

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
    val viewModel: GoalItemViewModel = viewModel(key = goal.hashCode().toString(), factory = GoalItemViewModelFactory(goal, parsedData, reminder))
    val shouldShowStatusIcons by viewModel.shouldShowStatusIcons.collectAsState()

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
                    modifier = 
                        Modifier
                            .padding(end = 48.dp) // Reserve space for the handle
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
                        obsidianVaultName = obsidianVaultName,
                        onTagClick = onTagClick,
                        onTextClick = onItemClick,
                        onLongClick = onLongClick,
                        maxLines = 4,
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                lineHeight = 16.sp,
                                letterSpacing = 0.1.sp,
                                fontSize = 12.sp,
                                fontWeight = if (goal.completed) FontWeight.Normal else FontWeight.Medium,
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
                                onRelatedLinkClick = onRelatedLinkClick
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
