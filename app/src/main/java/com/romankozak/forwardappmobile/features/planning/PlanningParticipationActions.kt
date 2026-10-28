package com.romankozak.forwardappmobile.features.planning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.data.planning.PlanningParticipantSummary
import com.romankozak.forwardappmobile.shared.core.domain.planning.*

internal fun PlanningParticipantSummary.canRemoveParticipation() = dayTaskId != null ||
    commitment.provenance.sourceType in setOf(null, "ARC_QUEST", "TACTICAL_MISSION", "DAY_TASK")

@Composable
internal fun PlanningParticipationActions(item: PlanningParticipantSummary, model: PlanningCompositionViewModel) {
    val busy by model.busy.collectAsStateWithLifecycle()
    var removing by rememberSaveable(item.commitment.id.value) { mutableStateOf(false) }
    var statusMenu by rememberSaveable(item.commitment.id.value) { mutableStateOf(false) }
    Column {
        TextButton(onClick = { model.orientation(item.commitment.orientationId) }) { Text("Planning for this Orientation") }
        if (item.scope.kind == PlanningScopeKind.STRATEGIC_ARC) {
            TextButton(enabled = !busy, onClick = { model.promote(item, PlanningScopeKind.TACTICAL_CYCLE) }) { Text("Add to Tactical") }
        }
        if (item.scope.kind == PlanningScopeKind.TACTICAL_CYCLE) {
            TextButton(enabled = !busy, onClick = { model.promote(item, PlanningScopeKind.DAY) }) { Text("Add to Today") }
        }
        if (item.commitment.provenance.sourceType in setOf("ARC_QUEST", "TACTICAL_MISSION")) Box {
            TextButton(enabled = !busy, onClick = { statusMenu = true }) { Text("Status · ${item.statusLabel.ifBlank { item.commitment.status.name }}") }
            DropdownMenu(statusMenu, { statusMenu = false }) {
                listOf(PlanningCommitmentStatus.ACTIVE, PlanningCommitmentStatus.PAUSED, PlanningCommitmentStatus.COMPLETED).forEach { status ->
                    DropdownMenuItem(text = { Text(when (status) {
                        PlanningCommitmentStatus.ACTIVE -> "Active"
                        PlanningCommitmentStatus.PAUSED -> "Paused"
                        else -> "Completed"
                    }) }, onClick = { statusMenu = false; model.setStatus(item, status) }, enabled = !busy && item.commitment.status != status)
                }
            }
        }
        if (item.canRemoveParticipation()) TextButton(enabled = !busy, onClick = { removing = true }) { Text("Remove from planning…") }
    }
    if (removing) AlertDialog(onDismissRequest = { removing = false }, title = { Text("Remove from planning?") },
        text = { Text("${item.entity.title}\n${item.contextLabel}\nЦя участь та її planning-зв’язки зникнуть. Orientation і її інші участі залишаться.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { removing = false; model.removeParticipation(item) }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { removing = false }) { Text("Скасувати") } })
}
