package com.romankozak.forwardappmobile.features.planning

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*

@Composable
fun MissionPlanningTab(missionId: Long) {
    Column { PlanningCompositionPanel(PlanningReferenceOwner(PlanningReferenceOwnerKind.TACTICAL_MISSION, missionId.toString())) }
}
