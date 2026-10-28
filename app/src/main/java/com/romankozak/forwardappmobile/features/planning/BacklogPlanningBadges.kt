package com.romankozak.forwardappmobile.features.planning

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.data.planning.PlanningParticipantSummary
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedMetaChip
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusRow

@Composable
internal fun BacklogPlanningBadges(badges: List<PlanningContextBadge>, open: (PlanningParticipantSummary) -> Unit) {
    if (badges.isNotEmpty()) UnifiedStatusRow {
        badges.forEach { badge -> UnifiedMetaChip(badge.label, Icons.Outlined.EventNote, onClick = { open(badge.participant) }) }
    }
}
