package com.romankozak.forwardappmobile.features.missions.presentation.missionlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionPriority
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.hasDeadline

internal fun missionDeadlineLabel(mission: TacticalMission): String? =
    if (mission.hasDeadline()) formatMissionDate(mission.deadline) else null

internal fun MissionPriority.missionPriorityDescription(): String =
    when (this) {
        MissionPriority.LOW -> "Низький пріоритет"
        MissionPriority.MEDIUM -> "Середній пріоритет"
        MissionPriority.HIGH -> "Високий пріоритет"
        MissionPriority.CRITICAL -> "Критичний пріоритет"
    }

@Composable
internal fun MissionPriority.missionPriorityColor(): Color =
    when (this) {
        MissionPriority.LOW -> MaterialTheme.colorScheme.secondary
        MissionPriority.MEDIUM -> MaterialTheme.colorScheme.primary
        MissionPriority.HIGH -> MaterialTheme.colorScheme.tertiary
        MissionPriority.CRITICAL -> MaterialTheme.colorScheme.error
    }

@Composable
internal fun TacticalMissionPriorityIndicator(priority: MissionPriority, completed: Boolean) {
    val description = priority.missionPriorityDescription()
    Box(Modifier.width(24.dp).height(34.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(7.dp)
                .background(priority.missionPriorityColor().copy(alpha = if (completed) 0.4f else 1f), CircleShape)
                .semantics { contentDescription = description },
        )
    }
}
