package com.romankozak.forwardappmobile.data.planning.tactical

import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionPriority
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningPriorityLevel
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenance
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenanceKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.TacticalPlanningScopeExtension
import java.util.UUID

internal enum class TacticalMissionDeferralReason {
    NO_CANONICAL_SCOPE,
    DELETED_UNADOPTED,
    NON_ORIENTATION_BACKLOG_TARGET,
    SOURCE_MAPPING_REQUIRED,
}

internal data class DeferredTacticalMission(
    val missionId: Long,
    val reason: TacticalMissionDeferralReason,
    val sourceId: String?,
)

internal data class TacticalPlanningMaterializationPlan(
    val scopes: List<PlanningScope>,
    val extensions: List<TacticalPlanningScopeExtension>,
    val commitments: List<PlanningCommitment>,
    val deferredMissions: List<DeferredTacticalMission>,
)

/** Pure deterministic V3.5f planner. It performs no adoption or persistence. */
internal fun planCanonicalTacticalPlanningMaterialization(
    iterations: List<TacticalIteration>,
    resolutionSnapshot: TacticalMissionOrientationResolutionSnapshot,
): TacticalPlanningMaterializationPlan {
    require(iterations.map(TacticalIteration::id).distinct().size == iterations.size) {
        "Tactical planning materialization contains duplicate iteration ids"
    }
    require(resolutionSnapshot.missions.map(TacticalMission::id).distinct().size == resolutionSnapshot.missions.size) {
        "Tactical planning materialization contains duplicate mission ids"
    }
    val iterationsById = iterations.associateBy(TacticalIteration::id)
    val scopes = iterations.sortedBy(TacticalIteration::id).map(TacticalIteration::toCanonicalTacticalPlanningScope)
    val extensions =
        iterations.sortedBy(TacticalIteration::id)
            .map(TacticalIteration::toCanonicalTacticalPlanningScopeExtension)
    val commitments = mutableListOf<PlanningCommitment>()
    val deferred = mutableListOf<DeferredTacticalMission>()

    resolutionSnapshot.missions.sortedBy(TacticalMission::id).forEach { mission ->
        val scopeId = mission.iterationId
        val iteration = scopeId?.let(iterationsById::get)
        if (iteration == null) {
            if (mission.isDeleted) {
                deferred +=
                    DeferredTacticalMission(
                        missionId = mission.id,
                        reason = TacticalMissionDeferralReason.NO_CANONICAL_SCOPE,
                        sourceId = scopeId,
                    )
                return@forEach
            }
            // A pre-cycle legacy mission may be standalone. This is not a
            // license to detach an arbitrary canonical Tactical mission.
            val legacyUnscopedReference =
                scopeId == null ||
                    (mission.weekKey.isNotBlank() && scopeId == mission.weekKey)

            if (legacyUnscopedReference) {
                val resolution =
                    resolveTacticalMissionOrientation(mission, resolutionSnapshot)

                val admitted =
                    when (mission.sourceType) {
                        MissionSourceType.MANUAL ->
                            resolution is TacticalMissionOrientationResolution.Resolved &&
                                resolution.path ==
                                    TacticalMissionOrientationResolutionPath.LEGACY_MANUAL_MAPPING

                        MissionSourceType.CONTEXT_BACKLOG_ITEM ->
                            (
                                resolution is TacticalMissionOrientationResolution.NeedsAdoption &&
                                    resolution.code ==
                                        TacticalMissionOrientationResolutionCode.NON_ORIENTATION_BACKLOG_TARGET
                            ) ||
                                (
                                    resolution is TacticalMissionOrientationResolution.Resolved &&
                                        resolution.path ==
                                            TacticalMissionOrientationResolutionPath.BACKLOG_ORIENTATION
                                )

                        else -> false
                    }

                if (admitted) {
                    deferred +=
                        DeferredTacticalMission(
                            missionId = mission.id,
                            reason = TacticalMissionDeferralReason.NO_CANONICAL_SCOPE,
                            sourceId = scopeId,
                        )
                    return@forEach
                }
            }

            // Diagnostic classification only. Do not relax admission.
            // Never include Orientation titles or source-record details.
            val resolutionCategory =
                if (legacyUnscopedReference) {
                    when (val resolution =
                        resolveTacticalMissionOrientation(mission, resolutionSnapshot)
                    ) {
                        is TacticalMissionOrientationResolution.Resolved ->
                            "RESOLVED_${resolution.path}"

                        is TacticalMissionOrientationResolution.NeedsAdoption ->
                            "NEEDS_ADOPTION_${resolution.code}"

                        is TacticalMissionOrientationResolution.SemanticPrerequisite ->
                            "SEMANTIC_PREREQUISITE_${resolution.code}"

                        is TacticalMissionOrientationResolution.IntegrityFailure ->
                            "INTEGRITY_FAILURE_${resolution.code}"
                    }
                } else {
                    "NOT_LEGACY_UNSCOPED"
                }

            error(
                "Live TacticalMission ${mission.id} references missing canonical Tactical iteration " +
                    "${scopeId ?: "<null>"} " +
                    "[sourceType=${mission.sourceType.name}, resolution=$resolutionCategory]",
            )
        }
        require(!iteration.isDeleted || mission.isDeleted) {
            "Live TacticalMission ${mission.id} belongs to tombstoned iteration ${iteration.id}"
        }
        when (val resolution = resolveTacticalMissionOrientation(mission, resolutionSnapshot)) {
            is TacticalMissionOrientationResolution.Resolved ->
                commitments += mission.toCanonicalMissionCommitment(resolution.orientationId)

            is TacticalMissionOrientationResolution.NeedsAdoption -> {
                if (mission.isDeleted && resolution.code in DELETED_DEFERRABLE_CODES) {
                    deferred +=
                        DeferredTacticalMission(
                            missionId = mission.id,
                            reason = TacticalMissionDeferralReason.DELETED_UNADOPTED,
                            sourceId = resolution.sourceId,
                        )
                    return@forEach
                }
                val reason =
                    when (resolution.code) {
                        TacticalMissionOrientationResolutionCode.NON_ORIENTATION_BACKLOG_TARGET ->
                            TacticalMissionDeferralReason.NON_ORIENTATION_BACKLOG_TARGET
                        TacticalMissionOrientationResolutionCode.SOURCE_MAPPING_REQUIRED ->
                            TacticalMissionDeferralReason.SOURCE_MAPPING_REQUIRED
                        else -> error(
                            "TacticalMission ${mission.id} requires unresolved semantic prerequisite " +
                                "${resolution.code}",
                        )
                    }
                deferred += DeferredTacticalMission(mission.id, reason, resolution.sourceId)
            }

            is TacticalMissionOrientationResolution.SemanticPrerequisite ->
                error("TacticalMission ${mission.id} semantic prerequisite failed: ${resolution.code}")

            is TacticalMissionOrientationResolution.IntegrityFailure ->
                error("TacticalMission ${mission.id} integrity failure: ${resolution.code}: ${resolution.detail}")
        }
    }

    val duplicateParticipation =
        commitments
            .filterNot(PlanningCommitment::isDeleted)
            .groupBy { Triple(it.scopeId, it.orientationId, it.role) }
            .entries.firstOrNull { it.value.size > 1 }
    require(duplicateParticipation == null) {
        "Duplicate live Tactical MISSION participation ${duplicateParticipation?.key}"
    }
    require(commitments.map { it.id }.distinct().size == commitments.size) {
        "Deterministic Tactical commitment id collision"
    }
    check(commitments.size + deferred.size == resolutionSnapshot.missions.size) {
        "Tactical mission eligibility accounting is incomplete"
    }

    return TacticalPlanningMaterializationPlan(scopes, extensions, commitments, deferred)
}

internal fun canonicalTacticalMissionCommitmentId(
    scopeId: String,
    missionId: Long,
): PlanningCommitmentId {
    require(scopeId.isNotBlank())
    require(missionId > 0L)
    return PlanningCommitmentId(
        LegacySubjectUuid.uuidV5(
            TACTICAL_MISSION_COMMITMENT_NAMESPACE,
            "TACTICAL_MISSION_COMMITMENT:$scopeId:$missionId:${PlanningRoleCode.MISSION.value}",
        ).toString(),
    )
}

internal fun TacticalMission.toCanonicalMissionCommitment(
    orientationId: String,
    provenanceKind: PlanningProvenanceKind = PlanningProvenanceKind.MIGRATED,
): PlanningCommitment {
    val canonicalScopeId = requireNotNull(iterationId) { "TacticalMission $id has no iteration" }
    return PlanningCommitment(
        id = canonicalTacticalMissionCommitmentId(canonicalScopeId, id),
        scopeId = PlanningScopeId(canonicalScopeId),
        orientationId = orientationId,
        role = PlanningRoleCode.MISSION,
        order = orderInWeek,
        priorityLevel = priority.toPlanningPriorityLevel(),
        status = if (isDeleted) PlanningCommitmentStatus.DROPPED else status.toPlanningCommitmentStatus(),
        provenance =
            PlanningProvenance(
                kind = provenanceKind,
                sourceType = TACTICAL_MISSION_PROVENANCE_SOURCE,
                sourceId = id.toString(),
            ),
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )
}

internal fun MissionStatus.toPlanningCommitmentStatus(): PlanningCommitmentStatus =
    when (this) {
        MissionStatus.ACTIVE -> PlanningCommitmentStatus.ACTIVE
        MissionStatus.INACTIVE -> PlanningCommitmentStatus.PLANNED
        MissionStatus.PAUSED -> PlanningCommitmentStatus.PAUSED
        MissionStatus.COMPLETED -> PlanningCommitmentStatus.COMPLETED
    }

internal fun MissionPriority.toPlanningPriorityLevel(): PlanningPriorityLevel =
    when (this) {
        MissionPriority.LOW -> PlanningPriorityLevel.LOW
        MissionPriority.MEDIUM -> PlanningPriorityLevel.MEDIUM
        MissionPriority.HIGH -> PlanningPriorityLevel.HIGH
        MissionPriority.CRITICAL -> PlanningPriorityLevel.CRITICAL
    }

internal const val TACTICAL_MISSION_PROVENANCE_SOURCE = "TACTICAL_MISSION"
private val TACTICAL_MISSION_COMMITMENT_NAMESPACE =
    UUID.fromString("07b811da-f183-5fb4-b79f-5c78bfe4931c")
private val DELETED_DEFERRABLE_CODES =
    setOf(
        TacticalMissionOrientationResolutionCode.MANUAL_ADOPTION_REQUIRED,
        TacticalMissionOrientationResolutionCode.PREVIOUS_MISSION_UNRESOLVED,
        TacticalMissionOrientationResolutionCode.NON_ORIENTATION_BACKLOG_TARGET,
        TacticalMissionOrientationResolutionCode.SOURCE_MAPPING_REQUIRED,
    )
