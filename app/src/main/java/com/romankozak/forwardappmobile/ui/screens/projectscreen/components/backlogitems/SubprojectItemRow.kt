package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems

import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.database.models.ScoringStatusValues
import com.romankozak.forwardappmobile.ui.common.rememberParsedText
import kotlinx.coroutines.delay
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.ui.common.components.EnhancedReminderBadge

@Composable
private fun EnhancedSublistIconBadge(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .semantics { contentDescription = "Підсписок" }
                .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(22.dp)
                    .background(
                        brush =
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = 
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    ),
                            ),
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
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

 

private sealed class FlowItem {
    data class SublistIcon(val item: @Composable () -> Unit) : FlowItem()
    data class ChildProject(val project: Project) : FlowItem()
    data class ReminderItem(val reminder: Reminder) : FlowItem()
    data class ScoreStatus(val scoringStatus: String, val displayScore: Int) : FlowItem()
    data class IconEmoji(val icon: String, val index: Int) : FlowItem()
    data class Tag(val tag: String) : FlowItem()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubprojectItemRow(
    subprojectContent: ListItemContent.SublistItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
    onTagClick: (String) -> Unit = {},
    childProjects: List<Project> = emptyList(),
    onChildProjectClick: (Project) -> Unit = {},
    currentTimeMillis: Long,
    contextMarkerToEmojiMap: Map<String, String>,
    emojiToHide: String?,
    reminders: List<Reminder> = emptyList(),
    endAction: @Composable () -> Unit = {},
) {
    val subproject = subprojectContent.project
    val futureReminders = reminders.filter { it.reminderTime >= currentTimeMillis }
    val reminder = if (futureReminders.isNotEmpty()) {
        futureReminders.minByOrNull { it.reminderTime }
    } else {
        reminders.maxByOrNull { it.reminderTime }
    }
    val parsedData = rememberParsedText(subproject.name, contextMarkerToEmojiMap)

    Surface(
        modifier =
            modifier
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                .pointerInput(onClick, onLongClick) {
                    detectTapGestures(
                        onLongPress = { onLongClick() },
                        onTap = { onClick() },
                    )
                },
        ) {
            val textColor = if (subproject.isCompleted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val textDecoration = if (subproject.isCompleted) TextDecoration.LineThrough else null

            Text(
                text = parsedData.mainText,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = textColor,
                textDecoration = textDecoration,
            )

            val hasExtraContent = !subproject.tags.isNullOrEmpty() ||
                (subproject.scoringStatus != ScoringStatusValues.NOT_ASSESSED) ||
                (reminder != null) ||
                (parsedData.icons.isNotEmpty()) ||
                childProjects.isNotEmpty()

            val items = remember(childProjects, subproject, parsedData, emojiToHide, currentTimeMillis, reminder) {
                buildList<FlowItem> {
                    add(FlowItem.SublistIcon { EnhancedSublistIconBadge() })
                    reminder?.let { add(FlowItem.ReminderItem(it)) }
                    childProjects.forEach { add(FlowItem.ChildProject(it)) }
                    add(FlowItem.ScoreStatus(subproject.scoringStatus, subproject.displayScore))
                    parsedData.icons
                        .filterNot { icon -> icon == emojiToHide }
                        .forEachIndexed { index, icon -> add(FlowItem.IconEmoji(icon, index)) }
                    subproject.tags?.filter { it.isNotBlank() }?.forEach { add(FlowItem.Tag(it)) }
                }
            }

            if (hasExtraContent) {
                ExpandableFlowRow(
                    items = items,
                    maxLinesWhenCollapsed = 2,
                    horizontalSpacing = 6.dp,
                    verticalSpacing = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                ) { item ->
                    when (item) {
                        is FlowItem.SublistIcon -> item.item()
                        is FlowItem.ChildProject -> {
                            RelatedLinkChip(
                                link = RelatedLink(
                                    type = LinkType.PROJECT,
                                    target = item.project.id,
                                    displayName = item.project.name,
                                ),
                                onClick = { onChildProjectClick(item.project) },
                            )
                        }
                        is FlowItem.ReminderItem -> {
                            EnhancedReminderBadge(
                                reminder = item.reminder,
                                currentTimeMillis = currentTimeMillis
                            )
                        }
                        is FlowItem.ScoreStatus -> {
                            EnhancedScoreStatusBadge(
                                scoringStatus = item.scoringStatus,
                                displayScore = item.displayScore,
                            )
                        }
                        is FlowItem.IconEmoji -> {
                            key(item.icon) {
                                var delayedVisible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    delay(item.index * 50L)
                                    delayedVisible = true
                                }
                                AnimatedVisibility(
                                    visible = delayedVisible,
                                    enter = scaleIn(
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    ) + fadeIn(),
                                ) {
                                    AnimatedContextEmoji(
                                        emoji = item.icon,
                                    )
                                }
                            }
                        }
                        is FlowItem.Tag -> {
                            val formattedTag = "#${item.tag.trim().trimStart('#')}"
                            ModernTagChip(
                                text = formattedTag,
                                onClick = { onTagClick(formattedTag) },
                                tagType = TagType.PROJECT,
                            )
                        }
                    }
                }
            }

            if (!subproject.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Text(
                        text = subproject.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = if (isSelected) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
        endAction()
    }
}
}
