package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.backlogitems.AnimatedContextEmoji
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayTaskWithReminder
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.ParentInfo
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.ParentType
import com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes.DayTheme
import com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes.EntityThemeSelector
import com.romankozak.forwardappmobile.features.daymanagement.utils.formatDayTime
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.planning.PlanningOrientationSourceBadge
import com.romankozak.forwardappmobile.ui.common.rememberParsedText
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedCheckboxStyle
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemCheckbox
import com.romankozak.forwardappmobile.ui.components.listitem.unifiedCheckboxColors
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedMetaChip
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusChipSpec
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusRow

private const val COMPLETED_TASK_CONTENT_ALPHA = 0.6f

data class DayTaskCardActions(
    val onClick: () -> Unit,
    val onToggle: () -> Unit,
    val onLongPress: () -> Unit,
    val onParentInfoClick: (ParentInfo) -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayTaskCard(
    taskWithReminder: DayTaskWithReminder,
    linkedProjectTitles: Map<String, String>,
    contextMarkerToEmojiMap: Map<String, String>,
    actions: DayTaskCardActions,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    themes: List<DayTheme>,
    selectedThemeIds: Set<String>,
    onToggleTheme: (String) -> Unit,
    orientationSource: CanonicalEntitySummary? = null,
    relatedOrientations: List<CanonicalEntitySummary> = emptyList(),
    onOpenOrientation: (String) -> Unit = {},
) {
    val task = taskWithReminder.dayTask
    val contentAlpha = if (task.completed) COMPLETED_TASK_CONTENT_ALPHA else 1f

    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        UnifiedItemCheckbox(
            checked = task.completed,
            onCheckedChange = { actions.onToggle() },
            style = UnifiedCheckboxStyle.Square,
            modifier = dragHandleModifier,
            colors =
                unifiedCheckboxColors(
                    checked = MaterialTheme.colorScheme.primary,
                    uncheckedBorder = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                ),
        )

        Spacer(modifier = Modifier.width(6.dp))

        DayTaskCardContent(
            taskWithReminder = taskWithReminder,
            linkedProjectTitles = linkedProjectTitles,
            contextMarkerToEmojiMap = contextMarkerToEmojiMap,
            contentAlpha = contentAlpha,
            onClick = actions.onClick,
            onParentInfoClick = actions.onParentInfoClick,
            themes = themes,
            selectedThemeIds = selectedThemeIds,
            onToggleTheme = onToggleTheme,
            orientationSource = orientationSource,
            relatedOrientations = relatedOrientations,
            onOpenOrientation = onOpenOrientation,
        )

        task.priority.dayTaskPriorityDescription()?.let { description ->
            Box(Modifier.width(24.dp).height(34.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(7.dp).background(task.priority.priorityIndicatorColor().copy(alpha = contentAlpha), CircleShape)
                    .semantics { contentDescription = description })
            }
        }

        IconButton(
            onClick = actions.onLongPress,
            modifier = dragHandleModifier.size(34.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Більше опцій",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RowScope.DayTaskCardContent(
    taskWithReminder: DayTaskWithReminder,
    linkedProjectTitles: Map<String, String>,
    contextMarkerToEmojiMap: Map<String, String>,
    contentAlpha: Float,
    onClick: () -> Unit,
    onParentInfoClick: (ParentInfo) -> Unit,
    themes: List<DayTheme>,
    selectedThemeIds: Set<String>,
    onToggleTheme: (String) -> Unit,
    orientationSource: CanonicalEntitySummary?,
    relatedOrientations: List<CanonicalEntitySummary>,
    onOpenOrientation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val task = taskWithReminder.dayTask
    val parsedTitle = rememberParsedText(task.title, contextMarkerToEmojiMap)
    val parsedDescription = rememberParsedText(task.description.orEmpty(), contextMarkerToEmojiMap)
    val parsedTaskText =
        rememberParsedText(
            buildString {
                append(task.title)
                task.description?.takeIf { it.isNotBlank() }?.let {
                    append('\n')
                    append(it)
                }
            },
            contextMarkerToEmojiMap,
        )
    Column(
        modifier =
            modifier
                .weight(1f)
                .padding(top = 5.dp)
                .clickable(onClick = onClick),
    ) {
        DayPlanMarkdownText(
            text = parsedTitle.mainText,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight(450),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                ),
            isCompleted = task.completed,
            maxLines = 4,
        )

        if (hasVisibleTaskThemes(themes, selectedThemeIds)) EntityThemeSelector(
            themes = themes,
            selectedThemeIds = selectedThemeIds,
            entityLabel = task.title,
            onToggle = onToggleTheme,
            modifier = Modifier.padding(top = 5.dp),
        )
        com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayTaskRealizedPriorityChips(task, onClick)

        parsedDescription.mainText.takeIf { it.isNotBlank() }?.let { description ->
            DayPlanMarkdownText(
                text = description,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    ),
                isCompleted = task.completed,
                maxLines = 2,
            )
        }

        DayTaskMetaRow(
            taskWithReminder = taskWithReminder,
            linkedProjectTitles = linkedProjectTitles,
            contextIcons = parsedTaskText.icons,
            modifier = Modifier.padding(top = 8.dp),
            onParentInfoClick = onParentInfoClick,
            orientationSource = orientationSource,
            relatedOrientations = relatedOrientations,
            onOpenOrientation = onOpenOrientation,
        )
    }
}

@Composable
private fun DayTaskMetaRow(
    taskWithReminder: DayTaskWithReminder,
    linkedProjectTitles: Map<String, String>,
    contextIcons: List<String>,
    modifier: Modifier = Modifier,
    onParentInfoClick: (ParentInfo) -> Unit,
    orientationSource: CanonicalEntitySummary?,
    relatedOrientations: List<CanonicalEntitySummary>,
    onOpenOrientation: (String) -> Unit,
) {
    val task = taskWithReminder.dayTask
    val metaItems =
        buildList<UnifiedStatusChipSpec> {
            if (task.recurrenceSeriesId != null) add(UnifiedStatusChipSpec(icon = Icons.Outlined.Repeat, text = ""))
            (task.scheduledTime ?: taskWithReminder.reminder?.reminderTime ?: task.dueTime)?.let {
                add(UnifiedStatusChipSpec(text = formatDayTime(it)))
            }
            if (task.title.contains("#focus", ignoreCase = true) || task.description?.contains("#focus", ignoreCase = true) == true) {
                add(
                    UnifiedStatusChipSpec(
                        icon = Icons.Outlined.Flag,
                        text = "Focus",
                        contentColor = MaterialTheme.colorScheme.tertiary,
                    ),
                )
            }
            task.points.takeIf { it > 0 }?.let { points ->
                add(
                    UnifiedStatusChipSpec(
                        icon = Icons.Filled.Star,
                        text = "$points балів",
                        contentColor = MaterialTheme.colorScheme.secondary,
                    ),
                )
            }
            taskWithReminder.parentInfo?.let { parentInfo ->
                add(
                    parentInfo.toStatusChipSpec(onParentInfoClick),
                )
            }
            task.linkedProjectIds.orEmpty()
                .map(String::trim)
                .filter { linkedProjectId -> linkedProjectId.isNotBlank() && linkedProjectId != "root" }
                .distinct()
                .forEach { linkedProjectId ->
                    val title = linkedProjectTitles[linkedProjectId] ?: linkedProjectId
                    add(
                        ParentInfo(
                            id = linkedProjectId,
                            title = title,
                            type = ParentType.PROJECT,
                            projectId = linkedProjectId,
                        ).toStatusChipSpec(onParentInfoClick),
                    )
                }
            task.estimatedDurationMinutes?.takeIf { it > 0 }?.let { minutes ->
                add(UnifiedStatusChipSpec(icon = Icons.Outlined.Timer, text = "$minutes хв"))
            }
            if (taskWithReminder.reminder != null) {
                add(UnifiedStatusChipSpec(icon = Icons.Outlined.Notifications, text = "Нагадування"))
            }
        }

    if (metaItems.isEmpty() && contextIcons.isEmpty() && orientationSource == null && relatedOrientations.isEmpty()) return
    UnifiedStatusRow(modifier = modifier) {
        orientationSource?.let { source ->
            PlanningOrientationSourceBadge(source) { onOpenOrientation(source.ref.id) }
        }
        relatedOrientations.forEach { related ->
            PlanningOrientationSourceBadge(related, related = true) { onOpenOrientation(related.ref.id) }
        }
        contextIcons.forEach { icon ->
            AnimatedContextEmoji(emoji = icon)
        }
        metaItems.forEach { item ->
            UnifiedMetaChip(
                text = item.text,
                icon = item.icon,
                contentColor = item.contentColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = item.onClick,
            )
        }
    }
}

@Composable
private fun ParentInfo.toStatusChipSpec(
    onParentInfoClick: (ParentInfo) -> Unit,
): UnifiedStatusChipSpec =
    UnifiedStatusChipSpec(
        icon =
            if (type == ParentType.GOAL) {
                Icons.Default.TrackChanges
            } else {
                Icons.Default.Topic
            },
        text = title,
        contentColor =
            if (type == ParentType.GOAL) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.tertiary
            },
        onClick = { onParentInfoClick(this) },
    )

@Composable
private fun TaskPriority.priorityIndicatorColor(): Color {
    val colorScheme = MaterialTheme.colorScheme
    return when (this) {
        TaskPriority.CRITICAL -> colorScheme.error
        TaskPriority.HIGH -> colorScheme.tertiary
        TaskPriority.MEDIUM -> colorScheme.primary
        TaskPriority.LOW -> colorScheme.secondary
        TaskPriority.NONE -> colorScheme.outline
    }
}
