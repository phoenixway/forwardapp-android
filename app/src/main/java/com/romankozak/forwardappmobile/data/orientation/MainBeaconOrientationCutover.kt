package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroupMember
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.data.database.OrientationBootstrapIssueEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.EffectiveOrientation
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState

internal data class MainBeaconCutoverPlan(
    val mappings: List<LegacySubjectMappingEntity>,
    val relationChanges: List<OrientationRelationEntity>,
    val issues: List<OrientationBootstrapIssueEntity>,
)

internal fun planMainBeaconCutover(
    projections: List<EffectiveOrientation>,
    mappings: List<LegacySubjectMappingEntity>,
    subjects: List<ManagedSubjectEntity>,
    orientations: List<OrientationEntity>,
    legacyMembers: List<MainBeaconGroupMember>,
    existingRelations: List<OrientationRelationEntity>,
    now: Long,
    migrationVersion: Int,
): MainBeaconCutoverPlan {
    val eligible = projections.filter { it.source.sourceType in MAIN_BEACON_SOURCE_TYPES }
    val mappingBySource = mappings.associateBy { it.sourceType to it.sourceId }
    val subjectById = subjects.associateBy { it.id }
    val orientationById = orientations.associateBy { it.subjectId }
    val issues = mutableListOf<OrientationBootstrapIssueEntity>()

    eligible.forEach { projection ->
        val mapping = mappingBySource[projection.source.sourceType.name to projection.source.sourceId]
        val subject = mapping?.let { subjectById[it.subjectId] }
        val orientation = mapping?.let { orientationById[it.subjectId] }
        when {
            mapping == null ->
                issues += projection.issue("CUTOVER_MAPPING_MISSING", "Durable legacy mapping is missing")
            mapping.subjectId != projection.subject.id ->
                issues += projection.issue("CUTOVER_IDENTITY_MISMATCH", "Legacy mapping points to ${mapping.subjectId}")
            mapping.state == LegacySubjectMappingState.QUARANTINED.name ->
                issues += projection.issue("CUTOVER_MAPPING_QUARANTINED", "Legacy mapping is quarantined")
            subject == null -> issues += projection.issue("CUTOVER_SUBJECT_MISSING", "Canonical subject is missing")
            orientation?.kind != projection.orientation.kind.name ->
                issues += projection.issue("CUTOVER_KIND_MISMATCH", "Canonical Orientation kind does not match source")
            mapping.state != LegacySubjectMappingState.CUT_OVER.name &&
                (subject.title != projection.subject.title || subject.description != projection.subject.description) ->
                issues +=
                    projection.issue("CUTOVER_SHADOW_DIVERGENCE", "Common fields diverged before ownership cutover")
        }
    }
    if (issues.isNotEmpty()) return MainBeaconCutoverPlan(emptyList(), emptyList(), issues)

    val changedMappings =
        eligible.mapNotNull { projection ->
            val mapping = mappingBySource.getValue(projection.source.sourceType.name to projection.source.sourceId)
            mapping.takeIf {
                it.state != LegacySubjectMappingState.CUT_OVER.name || it.migrationVersion != migrationVersion
            }?.copy(
                migrationVersion = migrationVersion,
                state = LegacySubjectMappingState.CUT_OVER.name,
                updatedAt = now,
                syncedAt = null,
                version = mapping.version + 1L,
            )
        }
    val finalMappings =
        (mappings.associateBy { it.id } + changedMappings.associateBy { it.id }).values.toList()
    val newlyCutOverSubjectIds = changedMappings.mapTo(hashSetOf()) { it.subjectId }
    return MainBeaconCutoverPlan(
        mappings = changedMappings,
        relationChanges =
            planCanonicalMainBeaconMembershipChanges(
                legacyMembers = legacyMembers,
                mappings = finalMappings,
                existingRelations = existingRelations,
                now = now,
            ).filter {
                it.fromOrientationId in newlyCutOverSubjectIds || it.toOrientationId in newlyCutOverSubjectIds
            },
        issues = emptyList(),
    )
}

internal val MAIN_BEACON_SOURCE_TYPES =
    setOf(
        LegacyOrientationSourceType.MAIN_BEACON,
        LegacyOrientationSourceType.MAIN_BEACON_GROUP,
    )
