package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.data.database.OrientationBootstrapIssueEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.EffectiveOrientation
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import java.util.UUID

internal data class QuestOrientationCutoverPlan(
    val subjects: List<ManagedSubjectEntity>,
    val orientations: List<OrientationEntity>,
    val mappings: List<LegacySubjectMappingEntity>,
    val issues: List<OrientationBootstrapIssueEntity>,
)

/**
 * Promotes deterministic MANUAL ArcQuest compatibility shadows in place.
 *
 * ARC_QUEST remains compatibility/provenance vocabulary; QUEST is the
 * reusable semantic kind. Legacy ArcQuest status belongs to Strategic
 * planning state, so its compatibility-derived Orientation lifecycle is not
 * promoted into QUEST semantic authority.
 *
 * Once CUT_OVER, canonical common fields, lifecycle, and assessment are
 * authoritative and are not compared back to the legacy projection.
 */
internal fun planQuestOrientationCutover(
    projections: List<EffectiveOrientation>,
    mappings: List<LegacySubjectMappingEntity>,
    subjects: List<ManagedSubjectEntity>,
    orientations: List<OrientationEntity>,
    assessments: List<OrientationAssessmentEntity>,
    revisions: List<OrientationAssessmentRevisionEntity>,
    now: Long,
    migrationVersion: Int,
): QuestOrientationCutoverPlan {
    val quests = projections.filter { it.source.sourceType == LegacyOrientationSourceType.ARC_QUEST }
    val mappingsBySource = mappings.groupBy { it.sourceType to it.sourceId }
    val mappingsBySubject = mappings.groupBy { it.subjectId }
    val subjectsById = subjects.associateBy { it.id }
    val orientationsById = orientations.associateBy { it.subjectId }
    val assessmentsById = assessments.associateBy { it.orientationId }
    val revisionsById = revisions.associateBy { it.id }
    val issues = mutableListOf<OrientationBootstrapIssueEntity>()
    val projectedSourceIds = quests.mapTo(hashSetOf()) { it.source.sourceId }

    mappings.asSequence()
        .filter {
            it.sourceType == LegacyOrientationSourceType.ARC_QUEST.name &&
                it.sourceId !in projectedSourceIds
        }.forEach { mapping ->
            issues += mapping.questIssue("QUEST_CUTOVER_EXTENSION_MISSING", "ArcQuest extension row is missing")
        }

    quests.forEach { projection ->
        val key = projection.source.sourceType.name to projection.source.sourceId
        val sourceMappings = mappingsBySource[key].orEmpty()
        val mapping = sourceMappings.singleOrNull()
        val subject = mapping?.let { subjectsById[it.subjectId] }
        val orientation = mapping?.let { orientationsById[it.subjectId] }
        val assessment = mapping?.let { assessmentsById[it.subjectId] }
        val revision = assessment?.let { revisionsById[it.revisionId] }
        when {
            sourceMappings.size != 1 ->
                issues += projection.issue("QUEST_CUTOVER_MAPPING_INVALID", "Expected exactly one durable Quest mapping")
            mapping == null -> Unit
            mapping.sourceType != LegacyOrientationSourceType.ARC_QUEST.name ->
                issues += projection.issue("QUEST_CUTOVER_SOURCE_INVALID", "Mapping source type is not ARC_QUEST")
            mapping.subjectId != projection.subject.id ->
                issues += projection.issue("QUEST_CUTOVER_IDENTITY_MISMATCH", "Mapping points to ${mapping.subjectId}")
            mappingsBySubject[mapping.subjectId].orEmpty().size != 1 ->
                issues += projection.issue("QUEST_CUTOVER_SUBJECT_COLLISION", "Canonical subject has competing source mappings")
            mapping.state !in QUEST_CUTOVER_ALLOWED_MAPPING_STATES ->
                issues += projection.issue("QUEST_CUTOVER_STATE_INVALID", "Mapping state ${mapping.state} cannot be promoted")
            subject == null ->
                issues += projection.issue("QUEST_CUTOVER_SUBJECT_MISSING", "Canonical subject is missing")
            subject.subjectType != ManagedSubjectType.ORIENTATION.name ->
                issues += projection.issue("QUEST_CUTOVER_SUBJECT_TYPE_INVALID", "Mapped subject is not an Orientation")
            orientation == null ->
                issues += projection.issue("QUEST_CUTOVER_ORIENTATION_MISSING", "Canonical Orientation is missing")
            orientation.kind !in QUEST_CUTOVER_ALLOWED_KINDS ->
                issues += projection.issue("QUEST_CUTOVER_KIND_INVALID", "Unexpected canonical kind ${orientation.kind}")
            assessment == null ->
                issues += projection.issue("QUEST_CUTOVER_ASSESSMENT_MISSING", "Current assessment is missing")
            revision == null || revision.orientationId != mapping.subjectId ->
                issues += projection.issue("QUEST_CUTOVER_REVISION_INVALID", "Current assessment revision is missing or invalid")
            mapping.state == LegacySubjectMappingState.CUT_OVER.name &&
                (subject.isDeleted != mapping.isDeleted || subject.isDeleted != assessment.isDeleted) ->
                issues += projection.issue("QUEST_CUTOVER_DELETION_DIVERGENCE", "Canonical Quest deletion state is incoherent")
            mapping.state != LegacySubjectMappingState.CUT_OVER.name &&
                (
                    subject.isDeleted != projection.subject.isDeleted ||
                        mapping.isDeleted != projection.subject.isDeleted ||
                        assessment.isDeleted != projection.subject.isDeleted
                ) ->
                issues += projection.issue("QUEST_CUTOVER_DELETION_DIVERGENCE", "Quest deletion state diverged before cutover")
            mapping.state != LegacySubjectMappingState.CUT_OVER.name &&
                (subject.title != projection.subject.title || subject.description != projection.subject.description) ->
                issues += projection.issue("QUEST_CUTOVER_SHADOW_DIVERGENCE", "Common fields diverged before ownership cutover")
            mapping.state != LegacySubjectMappingState.CUT_OVER.name &&
                !assessment.hasSameAxisValues(projection.orientation.assessment) ->
                issues += projection.issue(
                    "QUEST_CUTOVER_ASSESSMENT_DIVERGENCE",
                    "Quest assessment differs from compatibility projection before ownership cutover",
                )
        }
    }

    if (issues.isNotEmpty()) {
        return QuestOrientationCutoverPlan(emptyList(), emptyList(), emptyList(), issues)
    }

    val orientationChanges = mutableListOf<OrientationEntity>()
    val subjectChanges = mutableListOf<ManagedSubjectEntity>()
    val mappingChanges = mutableListOf<LegacySubjectMappingEntity>()
    quests.forEach { projection ->
        val mapping = mappingsBySource.getValue(projection.source.sourceType.name to projection.source.sourceId).single()
        val orientation = orientationsById.getValue(mapping.subjectId)
        if (orientation.kind != OrientationKind.QUEST.name) {
            orientationChanges +=
                orientation.copy(
                    kind = OrientationKind.QUEST.name,
                    lifecycle = null,
                    lifecycleOrigin = ValueOrigin.UNSET.name,
                )
            val subject = subjectsById.getValue(mapping.subjectId)
            subjectChanges +=
                subject.copy(
                    updatedAt = now,
                    syncedAt = null,
                    version = subject.version + 1L,
                )
        }
        if (
            mapping.state != LegacySubjectMappingState.CUT_OVER.name ||
            mapping.migrationVersion != migrationVersion
        ) {
            mappingChanges +=
                mapping.copy(
                    migrationVersion = migrationVersion,
                    state = LegacySubjectMappingState.CUT_OVER.name,
                    updatedAt = now,
                    syncedAt = null,
                    version = mapping.version + 1L,
                )
        }
    }
    return QuestOrientationCutoverPlan(subjectChanges, orientationChanges, mappingChanges, emptyList())
}

private val QUEST_CUTOVER_ALLOWED_MAPPING_STATES =
    setOf(
        LegacySubjectMappingState.MATERIALIZED.name,
        LegacySubjectMappingState.CUT_OVER.name,
    )

private val QUEST_CUTOVER_ALLOWED_KINDS =
    setOf(
        OrientationKind.ARC_QUEST.name,
        OrientationKind.QUEST.name,
    )

private fun LegacySubjectMappingEntity.questIssue(
    code: String,
    detail: String,
): OrientationBootstrapIssueEntity =
    OrientationBootstrapIssueEntity(
        id =
            LegacySubjectUuid.uuidV5(
                UUID.fromString(LegacySubjectUuid.NAMESPACE_UUID),
                "bootstrap-issue:$sourceType:$sourceId:$code",
            ).toString(),
        sourceType = sourceType,
        sourceId = sourceId,
        code = code,
        detail = detail,
        createdAt = System.currentTimeMillis(),
        resolvedAt = null,
    )
