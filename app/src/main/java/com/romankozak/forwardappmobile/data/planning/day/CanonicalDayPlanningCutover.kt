package com.romankozak.forwardappmobile.data.planning.day

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.CanonicalDayThemeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenance
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenanceKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import java.util.UUID

internal data class CanonicalDayPlanningCutoverPlan(
    val scopes: List<PlanningScope>,
    val extensions: List<DayPlanningScopeExtension>,
    val commitments: List<PlanningCommitment>,
)

/** Pure deterministic migration planner. It never creates or repairs semantic Theme identity. */
internal fun planCanonicalDayPlanningCutover(
    dayPlans: List<DayPlan>,
    dayThemes: List<CanonicalDayThemeEntity>,
    mappings: List<LegacySubjectMappingEntity>,
    subjects: List<ManagedSubjectEntity>,
    orientations: List<OrientationEntity>,
): CanonicalDayPlanningCutoverPlan {
    require(dayPlans.map(DayPlan::id).distinct().size == dayPlans.size) {
        "Day planning cutover contains duplicate DayPlan ids"
    }
    val plansById = dayPlans.associateBy(DayPlan::id)
    val subjectsById = subjects.associateBy(ManagedSubjectEntity::id)
    val orientationsById = orientations.associateBy(OrientationEntity::subjectId)
    val themeMappings =
        mappings
            .filter { it.sourceType == LegacyOrientationSourceType.THEME_DEFINITION.name }
            .groupBy(LegacySubjectMappingEntity::sourceId)

    val commitments =
        dayThemes.sortedBy(CanonicalDayThemeEntity::id).map { dayTheme ->
            requireNotNull(plansById[dayTheme.dayPlanId]) {
                "DayTheme ${dayTheme.id} references missing DayPlan ${dayTheme.dayPlanId}"
            }
            val sourceMappings = themeMappings[dayTheme.themeId].orEmpty()
            require(sourceMappings.size == 1) {
                "DayTheme ${dayTheme.id} requires exactly one THEME_DEFINITION mapping"
            }
            val mapping = sourceMappings.single()
            require(mapping.state == LegacySubjectMappingState.CUT_OVER.name && !mapping.isDeleted) {
                "DayTheme ${dayTheme.id} mapping is not live CUT_OVER"
            }
            val subject = requireNotNull(subjectsById[mapping.subjectId]) {
                "DayTheme ${dayTheme.id} mapping target is missing"
            }
            val orientation = requireNotNull(orientationsById[mapping.subjectId]) {
                "DayTheme ${dayTheme.id} Orientation is missing"
            }
            require(subject.subjectType == ManagedSubjectType.ORIENTATION.name) {
                "DayTheme ${dayTheme.id} target is not an Orientation"
            }
            require(orientation.kind == OrientationKind.THEME.name) {
                "DayTheme ${dayTheme.id} target kind is not THEME"
            }
            if (!dayTheme.isDeleted) {
                require(!subject.isDeleted) {
                    "Live DayTheme ${dayTheme.id} targets a tombstoned Theme"
                }
            }
            dayTheme.toCanonicalDayThemeCommitment(mapping.subjectId)
        }

    val duplicateLiveParticipation =
        commitments
            .filterNot(PlanningCommitment::isDeleted)
            .groupBy { Triple(it.scopeId, it.orientationId, it.role) }
            .entries.firstOrNull { it.value.size > 1 }
    require(duplicateLiveParticipation == null) {
        "Duplicate live DAY.THEME participation ${duplicateLiveParticipation?.key}"
    }

    return CanonicalDayPlanningCutoverPlan(
        scopes = dayPlans.sortedBy(DayPlan::id).map(DayPlan::toCanonicalDayPlanningScope),
        extensions =
            dayPlans.sortedBy(DayPlan::id).map(DayPlan::toCanonicalDayPlanningScopeExtension),
        commitments = commitments,
    )
}

internal fun canonicalDayThemeCommitmentId(dayThemeId: String): PlanningCommitmentId {
    require(dayThemeId.isNotBlank())
    return PlanningCommitmentId(
        LegacySubjectUuid.uuidV5(
            DAY_THEME_COMMITMENT_NAMESPACE,
            "DAY_THEME_COMMITMENT:$dayThemeId",
        ).toString(),
    )
}

private fun CanonicalDayThemeEntity.toCanonicalDayThemeCommitment(
    orientationId: String,
): PlanningCommitment =
    PlanningCommitment(
        id = canonicalDayThemeCommitmentId(id),
        scopeId = com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId(dayPlanId),
        orientationId = orientationId,
        role = PlanningRoleCode.DAY_THEME,
        order = order,
        priorityLevel = null,
        status =
            when {
                isDeleted -> PlanningCommitmentStatus.DROPPED
                isActive -> PlanningCommitmentStatus.ACTIVE
                else -> PlanningCommitmentStatus.PAUSED
            },
        provenance =
            PlanningProvenance(
                kind = PlanningProvenanceKind.MIGRATED,
                sourceType = DAY_THEME_PROVENANCE_SOURCE,
                sourceId = id,
            ),
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

private const val DAY_THEME_PROVENANCE_SOURCE = "DAY_THEME"
private val DAY_THEME_COMMITMENT_NAMESPACE =
    UUID.fromString("f20bbf38-f08b-5ea3-b754-c123445f5a24")
