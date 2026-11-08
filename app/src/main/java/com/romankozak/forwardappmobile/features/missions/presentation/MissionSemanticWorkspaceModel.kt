package com.romankozak.forwardappmobile.features.missions.presentation

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.data.planning.PlanningCommitmentCompositionProjection
import com.romankozak.forwardappmobile.data.workspace.EmbodyingWorkspaceRequest
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType

internal sealed interface MissionSemanticWorkspaceAction {
    data class Open(val workspaceId: String, val title: String) : MissionSemanticWorkspaceAction
    data class Create(val request: EmbodyingWorkspaceRequest) : MissionSemanticWorkspaceAction
}

internal data class MissionPrimaryTarget(
    val orientationId: String,
    val workspace: MissionSemanticWorkspaceAction.Open?,
) {
    val destination: NavTarget
        get() = workspace?.let { NavTarget.Workspace(it.workspaceId) } ?: NavTarget.SemanticEntityEditor(orientationId)
}

internal data class MissionSemanticWorkspaceSnapshot(
    val actions: Map<Long, MissionSemanticWorkspaceAction> = emptyMap(),
    val targets: Map<Long, MissionPrimaryTarget> = emptyMap(),
)

/** Navigation uses the Mission's own exact participation, never an arbitrary RELATED entity. */
internal fun projectMissionPrimaryTargets(
    frame: PlanningCommitmentCompositionProjection,
    catalog: List<CanonicalEntitySummary>,
    actions: Map<Long, MissionSemanticWorkspaceAction>,
): Map<Long, MissionPrimaryTarget> {
    val entities = catalog.associateBy { it.ref }
    return frame.participants.mapNotNull { it.commitment.provenance.takeIf { it.sourceType == "TACTICAL_MISSION" }?.sourceId?.toLongOrNull() }
        .filter { it > 0 }.distinct().mapNotNull { missionId ->
            val participant = frame.forOwner(PlanningReferenceOwner(PlanningReferenceOwnerKind.TACTICAL_MISSION, missionId.toString()))
                ?: return@mapNotNull null
            if (participant.commitment.role != PlanningRoleCode.MISSION || participant.scope.kind != PlanningScopeKind.TACTICAL_CYCLE) return@mapNotNull null
            val id = participant.commitment.orientationId
            if (entities[CanonicalEntityRef.Orientation(id)] == null) return@mapNotNull null
            missionId to MissionPrimaryTarget(id, actions[missionId] as? MissionSemanticWorkspaceAction.Open)
        }.toMap()
}

/** Own Planning participation, not an arbitrary external SOURCE/RELATED reference. Bulk reads only. */
internal fun projectMissionSemanticWorkspaces(
    frame: PlanningCommitmentCompositionProjection,
    subjects: List<ManagedSubjectEntity>,
    bindings: List<WorkspaceBindingEntity>,
    catalog: List<CanonicalEntitySummary>,
): Map<Long, MissionSemanticWorkspaceAction> {
    val liveSubjects = subjects.filter { !it.isDeleted && it.subjectType == ManagedSubjectType.ORIENTATION.name }.associateBy { it.id }
    val entities = catalog.associateBy { it.ref }
    val embodiments = resolveOperationalEmbodiments(bindings, catalog)
    val liveEmbodiments = bindings.filter { !it.isDeleted && it.bindingType == WorkspaceBindingType.EMBODIES.name }.groupBy { it.subjectId }
    return frame.participants.mapNotNull { it.commitment.provenance.takeIf { it.sourceType == "TACTICAL_MISSION" }?.sourceId?.toLongOrNull() }
        .filter { it > 0 }.distinct().mapNotNull { missionId ->
            val participant = frame.forOwner(PlanningReferenceOwner(PlanningReferenceOwnerKind.TACTICAL_MISSION, missionId.toString()))
                ?: return@mapNotNull null
            if (participant.commitment.role != PlanningRoleCode.MISSION || participant.scope.kind != PlanningScopeKind.TACTICAL_CYCLE) return@mapNotNull null
            val id = participant.commitment.orientationId
            val subject = liveSubjects[id] ?: return@mapNotNull null
            val ref = CanonicalEntityRef.Orientation(id)
            if (entities[ref] == null) return@mapNotNull null
            val workspace = embodiments[ref]?.let(entities::get)
            val action = when {
                workspace != null -> MissionSemanticWorkspaceAction.Open(workspace.ref.id, workspace.title)
                liveEmbodiments[id].isNullOrEmpty() -> MissionSemanticWorkspaceAction.Create(
                    EmbodyingWorkspaceRequest(id, ManagedSubjectType.ORIENTATION, subject.version, subject.title, subject.description))
                else -> null // Ambiguous/unavailable EMBODIES must not advertise unsafe creation.
            }
            action?.let { missionId to it }
        }.toMap()
}
