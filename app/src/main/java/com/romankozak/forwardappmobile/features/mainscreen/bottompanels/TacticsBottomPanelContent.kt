package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStream
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.AutocompleteSuggestions
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelActionRow
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelComposer
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelGlobalActions
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelGlobalRail
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelIconButton
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.MoreSheetAction
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelTokens
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.bottomPanelColors
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.features.missions.presentation.TacticsWorkspaceMode
import com.romankozak.forwardappmobile.ui.components.CommonBottomPanelLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
internal fun TacticsBottomPanelContent(
    inputValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    placeholder: String,
    autocompleteSuggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    onAddMissionFromContext: () -> Unit,
    onToggleScopeLinksSheet: () -> Unit,
    selectedMode: TacticsWorkspaceMode,
    missionStreams: List<MissionStream>,
    allMissionStreams: List<MissionStream>,
    selectedMissionStreamId: String,
    missionStreamCounts: Map<String, Int>,
    iterationDurationDays: Int?,
    iterationDurationHours: Int?,
    activitySlotContexts: List<Context>,
    selectedPlanningContextId: String?,
    projectOptions: List<ProjectOption>,
    canPasteAsMissions: Boolean,
    onModeSelected: (TacticsWorkspaceMode) -> Unit,
    onMissionStreamSelected: (String) -> Unit,
    onPlanningContextSelected: (String?) -> Unit,
    onOpenMissionStreamsSheet: () -> Unit,
    onPasteMissions: () -> Unit,
    onSetIterationDuration: () -> Unit,
    onOpenIterationArchive: () -> Unit,
    onStartTimeboxedIteration: () -> Unit,
    onStartOpenEndedIteration: () -> Unit,
    globalActions: BottomPanelGlobalActions,
) {
    CommonBottomPanelLayout {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = BottomPanelTokens.OuterVerticalPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TacticsControlPanel(
                selectedMode = selectedMode,
                missionStreams = missionStreams,
                allMissionStreams = allMissionStreams,
                selectedMissionStreamId = selectedMissionStreamId,
                missionStreamCounts = missionStreamCounts,
                iterationDurationDays = iterationDurationDays,
                iterationDurationHours = iterationDurationHours,
                activitySlotContexts = activitySlotContexts,
                selectedPlanningContextId = selectedPlanningContextId,
                projectOptions = projectOptions,
                canPasteAsMissions = canPasteAsMissions,
                onModeSelected = onModeSelected,
                onMissionStreamSelected = onMissionStreamSelected,
                onPlanningContextSelected = onPlanningContextSelected,
                onOpenMissionStreamsSheet = onOpenMissionStreamsSheet,
                onPasteMissions = onPasteMissions,
            )

            TacticsComposerPanel {
                TacticalIterationDeadlineLine(iterationDurationDays = iterationDurationDays)
                BottomPanelActionRow(
                    leadingContent = {
                        BottomPanelIconButton(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Додати місію",
                            onClick = onAddMissionFromContext,
                        )
                        BottomPanelIconButton(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = "Показати зв'язки",
                            onClick = onToggleScopeLinksSheet,
                        )
                    },
                    trailingContent = {
                        BottomPanelGlobalRail(
                            actions = globalActions,
                            additionalActions =
                                listOf(
                                    MoreSheetAction(
                                        label = "Минулі ітерації",
                                        onClick = onOpenIterationArchive,
                                    ),
                                    MoreSheetAction(
                                        label = "Нова тижнева ітерація",
                                        onClick = onStartTimeboxedIteration,
                                    ),
                                    MoreSheetAction(
                                        label = "Нова відкрита ітерація",
                                        onClick = onStartOpenEndedIteration,
                                    ),
                                    MoreSheetAction(
                                        label = "Вказати тривалість тактичної ітерації",
                                        onClick = onSetIterationDuration,
                                    ),
                                ),
                        )
                    },
                )

                AutocompleteSuggestions(
                    suggestions = autocompleteSuggestions,
                    onSuggestionClick = onSuggestionClick,
                    modifier = Modifier.fillMaxWidth(),
                )

                BottomPanelComposer(
                    inputValue = inputValue,
                    onValueChange = onValueChange,
                    onSubmit = onSubmit,
                    placeholderText = placeholder,
                    sendContentDescription = "Створити місію",
                )
            }
        }
    }
}

@Composable
private fun TacticsControlPanel(
    selectedMode: TacticsWorkspaceMode,
    missionStreams: List<MissionStream>,
    allMissionStreams: List<MissionStream>,
    selectedMissionStreamId: String,
    missionStreamCounts: Map<String, Int>,
    iterationDurationDays: Int?,
    iterationDurationHours: Int?,
    activitySlotContexts: List<Context>,
    selectedPlanningContextId: String?,
    projectOptions: List<ProjectOption>,
    canPasteAsMissions: Boolean,
    onModeSelected: (TacticsWorkspaceMode) -> Unit,
    onMissionStreamSelected: (String) -> Unit,
    onPlanningContextSelected: (String?) -> Unit,
    onOpenMissionStreamsSheet: () -> Unit,
    onPasteMissions: () -> Unit,
) {
    val colors = bottomPanelColors()
    val streamItems =
        missionStreams.map { stream ->
            MissionStreamChipUi(
                id = stream.id,
                title = stream.title,
                count = missionStreamCounts[stream.id] ?: 0,
                budgetPercent = stream.budgetPercent,
            )
        }
    val allStreamItems =
        allMissionStreams.map { stream ->
            MissionStreamChipUi(
                id = stream.id,
                title = stream.title,
                count = missionStreamCounts[stream.id] ?: 0,
                budgetPercent = stream.budgetPercent,
            )
        }
    val isBudgetOverLimit = allMissionStreams.sumOf { it.budgetPercent ?: 0 } > 100
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = BottomPanelTokens.OuterHorizontalPadding),
        shape = RoundedCornerShape(18.dp),
        color = colors.inputContainer.copy(alpha = 0.55f),
        contentColor = colors.content,
        border =
            BorderStroke(
                BottomPanelTokens.BorderWidth,
                colors.border.copy(alpha = 0.32f),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TacticsModeSegmentedSwitch(
                    streamCount = missionStreams.size,
                    selectedMode = selectedMode,
                    onModeSelected = onModeSelected,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (canPasteAsMissions) {
                    TacticsRoundActionButton(
                        icon = Icons.Outlined.ContentPaste,
                        contentDescription = "Вставити в потік",
                        onClick = onPasteMissions,
                    )
                }
                TacticsRoundActionButton(
                    icon = Icons.Outlined.Checklist,
                    contentDescription = "План тижня",
                    selected = selectedMode == TacticsWorkspaceMode.PLAN,
                    onClick = { onModeSelected(TacticsWorkspaceMode.PLAN) },
                )
                TacticsRoundActionButton(
                    icon = Icons.Outlined.Settings,
                    contentDescription = "Керувати потоками",
                    filled = false,
                    onClick = onOpenMissionStreamsSheet,
                )
            }

            when (selectedMode) {
                TacticsWorkspaceMode.STREAMS ->
                    MissionStreamChipRow(
                        streams = streamItems,
                        allStreams = allStreamItems,
                        selectedMissionStreamId = selectedMissionStreamId,
                        iterationDurationDays = iterationDurationDays,
                        iterationDurationHours = iterationDurationHours,
                        isBudgetOverLimit = isBudgetOverLimit,
                        onMissionStreamSelected = onMissionStreamSelected,
                    )
                TacticsWorkspaceMode.PLAN ->
                    TacticsPlanControlRows(
                        missionStreams = missionStreams,
                        selectedMissionStreamId = selectedMissionStreamId,
                        activitySlotContexts = activitySlotContexts,
                        selectedPlanningContextId = selectedPlanningContextId,
                        projectOptions = projectOptions,
                        onMissionStreamSelected = onMissionStreamSelected,
                        onPlanningContextSelected = onPlanningContextSelected,
                    )
                TacticsWorkspaceMode.ALL -> Unit
            }
        }
    }
}

@Composable
private fun TacticsComposerPanel(content: @Composable ColumnScope.() -> Unit) {
    val colors = bottomPanelColors()
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = BottomPanelTokens.OuterHorizontalPadding),
        shape = RoundedCornerShape(BottomPanelTokens.ContainerCornerRadius),
        color = colors.container,
        contentColor = colors.content,
        border =
            BorderStroke(
                width = BottomPanelTokens.BorderWidth,
                color = colors.border,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = BottomPanelTokens.ContainerHorizontalPadding,
                        vertical = BottomPanelTokens.ContainerVerticalPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(BottomPanelTokens.RowSpacing),
            content = content,
        )
    }
}

@Composable
private fun MissionStreamChipRow(
    streams: List<MissionStreamChipUi>,
    allStreams: List<MissionStreamChipUi>,
    selectedMissionStreamId: String,
    iterationDurationDays: Int?,
    iterationDurationHours: Int?,
    isBudgetOverLimit: Boolean,
    onMissionStreamSelected: (String) -> Unit,
) {
    MissionStreamFittingRow {
        streams.forEach { stream ->
            MissionStreamChip(
                stream = stream,
                selected = selectedMissionStreamId == stream.id,
                iterationDurationHours = iterationDurationHours,
                isBudgetOverLimit = isBudgetOverLimit,
                onClick = { onMissionStreamSelected(stream.id) },
            )
        }
        MissionStreamMoreButton(
            streams = allStreams,
            selectedMissionStreamId = selectedMissionStreamId,
            iterationDurationHours = iterationDurationHours,
            isBudgetOverLimit = isBudgetOverLimit,
            onMissionStreamSelected = onMissionStreamSelected,
        )
    }
}

@Composable
private fun MissionStreamFittingRow(content: @Composable () -> Unit) {
    val spacing = 6.dp
    Layout(content = content) { measurables, constraints ->
        val spacingPx = spacing.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        if (placeables.isEmpty()) {
            layout(width = constraints.minWidth, height = 0) {}
        } else {
            val morePlaceable = placeables.last()
            val chipPlaceables = placeables.dropLast(1)
            val fullWidth =
                chipPlaceables.sumOf { it.width } +
                    spacingPx * (chipPlaceables.size - 1).coerceAtLeast(0)
            val shouldShowMore = fullWidth > constraints.maxWidth
            val reservedMoreWidth = if (shouldShowMore) morePlaceable.width + spacingPx else 0
            var usedWidth = 0
            val visibleChips =
                chipPlaceables.takeWhileIndexed { index, placeable ->
                    val nextWidth =
                        usedWidth +
                            if (index == 0) placeable.width else spacingPx + placeable.width
                    val fits = nextWidth + reservedMoreWidth <= constraints.maxWidth
                    if (fits) {
                        usedWidth = nextWidth
                    }
                    fits
                }
            val visiblePlaceables = if (shouldShowMore) visibleChips + morePlaceable else chipPlaceables
            val contentWidth =
                visiblePlaceables.sumOf { it.width } +
                    spacingPx * (visiblePlaceables.size - 1).coerceAtLeast(0)
            val rowWidth =
                if (shouldShowMore) {
                    constraints.maxWidth
                } else {
                    contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
                }
            val rowHeight = visiblePlaceables.maxOfOrNull { it.height } ?: 0
            layout(width = rowWidth, height = rowHeight) {
                var x = 0
                visibleChips.forEach { placeable ->
                    placeable.placeRelative(x = x, y = (rowHeight - placeable.height) / 2)
                    x += placeable.width + spacingPx
                }
                if (shouldShowMore) {
                    morePlaceable.placeRelative(
                        x = rowWidth - morePlaceable.width,
                        y = (rowHeight - morePlaceable.height) / 2,
                    )
                }
            }
        }
    }
}

private inline fun <T> List<T>.takeWhileIndexed(predicate: (Int, T) -> Boolean): List<T> {
    val result = ArrayList<T>()
    forEachIndexed { index, item ->
        if (!predicate(index, item)) return result
        result.add(item)
    }
    return result
}

private data class MissionStreamChipUi(
    val id: String,
    val title: String,
    val count: Int,
    val budgetPercent: Int?,
)

@Composable
private fun MissionStreamChip(
    stream: MissionStreamChipUi,
    selected: Boolean,
    iterationDurationHours: Int?,
    isBudgetOverLimit: Boolean,
    onClick: () -> Unit,
) {
    val colors = bottomPanelColors()
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color =
            if (selected) {
                colors.selectedActionContainer.copy(alpha = 0.64f)
            } else {
                colors.inputContainer.copy(alpha = 0.75f)
            },
        contentColor =
            if (selected) {
                colors.selectedActionContent
            } else {
                colors.content
            },
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    if (selected) {
                        colors.selectedActionContent.copy(alpha = 0.45f)
                    } else {
                        colors.border.copy(alpha = 0.35f)
                    },
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MissionStreamCountBadge(count = stream.count, selected = selected)
            Text(
                text = stream.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
            )
            MissionStreamBudgetBadge(
                budgetPercent = stream.budgetPercent,
                iterationDurationHours = iterationDurationHours,
                isOverLimit = isBudgetOverLimit,
                selected = selected,
            )
        }
    }
}

@Composable
private fun MissionStreamMoreButton(
    streams: List<MissionStreamChipUi>,
    selectedMissionStreamId: String,
    iterationDurationHours: Int?,
    isBudgetOverLimit: Boolean,
    onMissionStreamSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = bottomPanelColors()
    Box {
        Surface(
            modifier = Modifier.clickable { expanded = true },
            shape = RoundedCornerShape(999.dp),
            color = colors.inputContainer.copy(alpha = 0.75f),
            contentColor = colors.mutedContent,
            border = BorderStroke(1.dp, colors.border.copy(alpha = 0.35f)),
        ) {
            Text(
                text = "..",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            streams.forEach { stream ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MissionStreamCountBadge(
                                count = stream.count,
                                selected = selectedMissionStreamId == stream.id,
                            )
                            Text(
                                text = stream.title,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            MissionStreamBudgetBadge(
                                budgetPercent = stream.budgetPercent,
                                iterationDurationHours = iterationDurationHours,
                                isOverLimit = isBudgetOverLimit,
                                selected = selectedMissionStreamId == stream.id,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onMissionStreamSelected(stream.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun MissionStreamCountBadge(
    count: Int,
    selected: Boolean,
) {
    val colors = bottomPanelColors()
    val background =
        if (selected) {
            colors.selectedActionContent.copy(alpha = 0.18f)
        } else {
            colors.inputContainer
        }
    val contentColor =
        if (selected) {
            colors.selectedActionContent
        } else {
            colors.mutedContent
        }
    Box(
        modifier =
            Modifier
                .background(background, RoundedCornerShape(999.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

@Composable
private fun MissionStreamBudgetBadge(
    budgetPercent: Int?,
    iterationDurationHours: Int?,
    isOverLimit: Boolean,
    selected: Boolean,
) {
    val percent = budgetPercent ?: return
    val colors = bottomPanelColors()
    val accent =
        when {
            isOverLimit -> MaterialTheme.colorScheme.error
            selected -> colors.selectedActionContent
            else -> colors.mutedContent
        }
    Box(
        modifier =
            Modifier
                .background(accent.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = buildStreamBudgetLabel(percent, iterationDurationHours),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            maxLines = 1,
        )
    }
}

@Composable
private fun TacticalIterationDeadlineLine(iterationDurationDays: Int?) {
    val deadline =
        iterationDurationDays
            ?.takeIf { it > 0 }
            ?.let(::calculateTacticalIterationDeadlineMillis)
            ?: return
    val colors = bottomPanelColors()
    val now = System.currentTimeMillis()
    val isUrgent = deadline - now <= 24L * 60L * 60L * 1000L
    val textColor =
        if (isUrgent) {
            MaterialTheme.colorScheme.error
        } else {
            colors.mutedContent
        }
    Text(
        text = "Дедлайн: ${formatTacticalDeadline(deadline)}",
        modifier = Modifier.padding(horizontal = BottomPanelTokens.ContentHorizontalPadding),
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun buildStreamBudgetLabel(
    budgetPercent: Int,
    iterationDurationHours: Int?,
): String {
    val totalHours = iterationDurationHours?.takeIf { it > 0 } ?: return "$budgetPercent%"
    val streamHours = totalHours * budgetPercent / 100.0
    return "${formatBudgetHours(streamHours)} год"
}

private fun formatBudgetHours(hours: Double): String =
    if (hours % 1.0 == 0.0) {
        hours.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", hours)
    }

private fun calculateTacticalIterationDeadlineMillis(durationDays: Int): Long =
    Calendar.getInstance()
        .apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
            add(Calendar.DAY_OF_YEAR, durationDays - 1)
        }.timeInMillis

private fun formatTacticalDeadline(timestamp: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(timestamp)

@Composable
private fun TacticsPlanControlRows(
    missionStreams: List<MissionStream>,
    selectedMissionStreamId: String,
    activitySlotContexts: List<Context>,
    selectedPlanningContextId: String?,
    projectOptions: List<ProjectOption>,
    onMissionStreamSelected: (String) -> Unit,
    onPlanningContextSelected: (String?) -> Unit,
) {
    val streamOptions = missionStreams.map { PlanSelectorOption(it.id, it.title) }
    val sourceOptions =
        buildList {
            activitySlotContexts.forEach { context -> add(PlanSelectorOption(context.id, context.name)) }
            projectOptions.forEach { option -> add(PlanSelectorOption(option.id, option.name)) }
        }.distinctBy { it.id }
    val selectedStreamTitle =
        streamOptions.firstOrNull { it.id == selectedMissionStreamId }?.title ?: "Не вибрано"
    val selectedSourceTitle =
        sourceOptions.firstOrNull { it.id == selectedPlanningContextId }?.title ?: "Вибрати джерело"
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TacticsPlanSelectorRow(
            label = "У потік",
            value = selectedStreamTitle,
            options = streamOptions,
            onSelected = { option -> onMissionStreamSelected(option.id) },
        )
        TacticsPlanSelectorRow(
            label = "З беклогу",
            value = selectedSourceTitle,
            options = sourceOptions,
            onSelected = { option -> onPlanningContextSelected(option.id) },
        )
    }
}

private data class PlanSelectorOption(
    val id: String,
    val title: String,
)

@Composable
private fun TacticsPlanSelectorRow(
    label: String,
    value: String,
    options: List<PlanSelectorOption>,
    onSelected: (PlanSelectorOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = bottomPanelColors()
    Box {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = options.isNotEmpty()) { expanded = true },
            shape = RoundedCornerShape(14.dp),
            color = colors.inputContainer.copy(alpha = 0.68f),
            contentColor = colors.content,
            border = BorderStroke(1.dp, colors.border.copy(alpha = 0.24f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.mutedContent,
                )
                Text(
                    text = value,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.content,
                )
                Text(
                    text = ">",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.mutedContent,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun TacticsModeSegmentedSwitch(
    modifier: Modifier = Modifier,
    streamCount: Int,
    selectedMode: TacticsWorkspaceMode,
    onModeSelected: (TacticsWorkspaceMode) -> Unit,
) {
    val colors = bottomPanelColors()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = colors.inputContainer.copy(alpha = 0.62f),
        contentColor = colors.content,
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TacticsModeSegment(
                modifier = Modifier.widthIn(min = 76.dp),
                label = "Потоки $streamCount",
                selected = selectedMode == TacticsWorkspaceMode.STREAMS,
                onClick = { onModeSelected(TacticsWorkspaceMode.STREAMS) },
            )
            TacticsModeSegment(
                modifier = Modifier.widthIn(min = 52.dp),
                label = "Усі",
                selected = selectedMode == TacticsWorkspaceMode.ALL,
                onClick = { onModeSelected(TacticsWorkspaceMode.ALL) },
            )
        }
    }
}

@Composable
private fun TacticsModeSegment(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = bottomPanelColors()
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color =
            if (selected) {
                colors.selectedActionContainer.copy(alpha = 0.72f)
            } else {
                colors.inputContainer.copy(alpha = 0f)
            },
        contentColor =
            if (selected) {
                colors.selectedActionContent
            } else {
                colors.mutedContent
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun TacticsRoundActionButton(
    icon: ImageVector,
    contentDescription: String,
    filled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = bottomPanelColors()
    val backgroundModifier =
        when {
            selected -> Modifier.background(colors.selectedActionContainer.copy(alpha = 0.72f), CircleShape)
            filled -> Modifier.background(colors.inputContainer, CircleShape)
            else -> Modifier
        }
    val iconTint = if (selected) colors.selectedActionContent else colors.mutedContent
    IconButton(
        onClick = onClick,
        modifier =
            Modifier
                .size(40.dp)
                .then(backgroundModifier),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
    }
}
