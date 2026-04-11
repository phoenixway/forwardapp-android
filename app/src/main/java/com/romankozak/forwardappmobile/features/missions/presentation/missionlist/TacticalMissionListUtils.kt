@file:Suppress("MagicNumber")

package com.romankozak.forwardappmobile.features.missions.presentation.missionlist

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemState

private val MissionMonthLabels =
    listOf(
        "січ",
        "лют",
        "бер",
        "квіт",
        "трав",
        "черв",
        "лип",
        "серп",
        "вер",
        "жовт",
        "лист",
        "груд",
    )

internal fun List<String>.normalizedLinkedIds(): List<String> =
    asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .toList()

internal fun missionItemState(status: MissionStatus, overdue: Boolean): UnifiedItemState =
    when {
        status == MissionStatus.COMPLETED -> UnifiedItemState.COMPLETED
        overdue -> UnifiedItemState.OVERDUE
        else -> UnifiedItemState.DEFAULT
    }

@Composable
internal fun missionOverdueChipColor(): Color = MaterialTheme.colorScheme.error

@Composable
internal fun missionPausedColor(): Color = MaterialTheme.colorScheme.tertiary

@Composable
internal fun missionContainerColor(
    itemState: UnifiedItemState,
    isInactive: Boolean,
    isPaused: Boolean,
): Color {
    val colorScheme = MaterialTheme.colorScheme
    return when (itemState) {
        UnifiedItemState.COMPLETED -> colorScheme.secondaryContainer.copy(alpha = 0.36f)
        UnifiedItemState.OVERDUE -> colorScheme.errorContainer.copy(alpha = 0.34f)
        UnifiedItemState.DEFAULT ->
            when {
                isInactive -> colorScheme.surfaceVariant.copy(alpha = 0.70f)
                isPaused -> colorScheme.surfaceVariant.copy(alpha = 0.56f)
                else -> colorScheme.tertiaryContainer.copy(alpha = 0.40f)
            }
        UnifiedItemState.SELECTED -> colorScheme.surfaceContainerHighest
        UnifiedItemState.DISABLED -> colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
}

@Composable
internal fun missionBorderColor(
    itemState: UnifiedItemState,
    isInactive: Boolean,
    isPaused: Boolean,
): Color {
    val colorScheme = MaterialTheme.colorScheme
    return when (itemState) {
        UnifiedItemState.COMPLETED -> colorScheme.secondary.copy(alpha = 0.35f)
        UnifiedItemState.OVERDUE -> colorScheme.error.copy(alpha = 0.28f)
        UnifiedItemState.DEFAULT ->
            when {
                isInactive -> colorScheme.outline.copy(alpha = 0.50f)
                isPaused -> colorScheme.outlineVariant.copy(alpha = 0.44f)
                else -> colorScheme.tertiary.copy(alpha = 0.38f)
            }
        UnifiedItemState.SELECTED -> colorScheme.primary.copy(alpha = 0.4f)
        UnifiedItemState.DISABLED -> colorScheme.outlineVariant.copy(alpha = 0.35f)
    }
}

internal fun formatMissionDate(ts: Long): String {
    if (ts == NO_DEADLINE) return "Без дедлайну"
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = ts }
    val monthLabel = MissionMonthLabels[calendar.get(java.util.Calendar.MONTH)]
    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val year = calendar.get(java.util.Calendar.YEAR)
    return "$day $monthLabel $year"
}
