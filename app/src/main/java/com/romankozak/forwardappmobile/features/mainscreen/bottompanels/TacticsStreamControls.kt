package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.bottomPanelColors

@Composable
internal fun MissionStreamChipRow(
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
internal fun MissionStreamFittingRow(content: @Composable () -> Unit) {
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

internal inline fun <T> List<T>.takeWhileIndexed(predicate: (Int, T) -> Boolean): List<T> {
    val result = ArrayList<T>()
    forEachIndexed { index, item ->
        if (!predicate(index, item)) return result
        result.add(item)
    }
    return result
}

internal data class MissionStreamChipUi(
    val id: String,
    val title: String,
    val count: Int,
    val budgetPercent: Int?,
)

@Composable
internal fun MissionStreamChip(
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
internal fun MissionStreamMoreButton(
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
internal fun MissionStreamCountBadge(
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
internal fun MissionStreamBudgetBadge(
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
