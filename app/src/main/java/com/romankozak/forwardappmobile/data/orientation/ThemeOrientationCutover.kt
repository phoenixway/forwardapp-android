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
import java.util.UUID

internal data class ThemeOrientationCutoverPlan(
    val subjects: List<ManagedSubjectEntity>,
    val orientations: List<OrientationEntity>,
    val mappings: List<LegacySubjectMappingEntity>,
    val issues: List<OrientationBootstrapIssueEntity>,
)

/**
 * Promotes existing deterministic ThemeDefinition shadows without replacing
 * identity or assessment history. DAY_THEME remains input compatibility
 * vocabulary; THEME is the only post-cutover kind.
 */
internal fun planThemeOrientationCutover(
    projections: List<EffectiveOrientation>,
    mappings: List<LegacySubjectMappingEntity>,
    subjects: List<ManagedSubjectEntity>,
    orientations: List<OrientationEntity>,
    assessments: List<OrientationAssessmentEntity>,
    revisions: List<OrientationAssessmentRevisionEntity>,
    now: Long,
    migrationVersion: Int,
): ThemeOrientationCutoverPlan {
    val themes = projections.filter { it.source.sourceType == LegacyOrientationSourceType.THEME_DEFINITION }
    val mappingsBySource = mappings.groupBy { it.sourceType to it.sourceId }
    val mappingsBySubject = mappings.groupBy { it.subjectId }
    val subjectsById = subjects.associateBy { it.id }
    val orientationsById = orientations.associateBy { it.subjectId }
    val assessmentsById = assessments.associateBy { it.orientationId }
    val revisionsById = revisions.associateBy { it.id }
    val issues = mutableListOf<OrientationBootstrapIssueEntity>()
    val projectedSourceIds = themes.mapTo(hashSetOf()) { it.source.sourceId }

    mappings.asSequence()
        .filter {
            it.sourceType == LegacyOrientationSourceType.THEME_DEFINITION.name &&
                it.sourceId !in projectedSourceIds
        }.forEach { mapping ->
            issues += mapping.themeIssue("THEME_CUTOVER_EXTENSION_MISSING", "Theme extension row is missing")
        }

    themes.forEach { projection ->
        val key = projection.source.sourceType.name to projection.source.sourceId
        val sourceMappings = mappingsBySource[key].orEmpty()
        val mapping = sourceMappings.singleOrNull()
        val subject = mapping?.let { subjectsById[it.subjectId] }
        val orientation = mapping?.let { orientationsById[it.subjectId] }
        val assessment = mapping?.let { assessmentsById[it.subjectId] }
        val revision = assessment?.let { revisionsById[it.revisionId] }
        when {
            sourceMappings.size != 1 ->
                issues += projection.issue("THEME_CUTOVER_MAPPING_INVALID", "Expected exactly one durable Theme mapping")
            mapping == null -> Unit
            mapping.sourceType != LegacyOrientationSourceType.THEME_DEFINITION.name ->
                issues += projection.issue("THEME_CUTOVER_SOURCE_INVALID", "Mapping source type is not THEME_DEFINITION")
            mapping.subjectId != projection.subject.id ->
                issues += projection.issue("THEME_CUTOVER_IDENTITY_MISMATCH", "Mapping points to ${mapping.subjectId}")
            mappingsBySubject[mapping.subjectId].orEmpty().size != 1 ->
                issues += projection.issue("THEME_CUTOVER_SUBJECT_COLLISION", "Canonical subject has competing source mappings")
            mapping.state !in THEME_CUTOVER_ALLOWED_MAPPING_STATES ->
                issues += projection.issue("THEME_CUTOVER_STATE_INVALID", "Mapping state ${mapping.state} cannot be promoted")
            subject == null ->
                issues += projection.issue("THEME_CUTOVER_SUBJECT_MISSING", "Canonical subject is missing")
            subject.subjectType != ManagedSubjectType.ORIENTATION.name ->
                issues += projection.issue("THEME_CUTOVER_SUBJECT_TYPE_INVALID", "Mapped subject is not an Orientation")
            orientation == null ->
                issues += projection.issue("THEME_CUTOVER_ORIENTATION_MISSING", "Canonical Orientation is missing")
            orientation.kind !in THEME_CUTOVER_ALLOWED_KINDS ->
                issues += projection.issue("THEME_CUTOVER_KIND_INVALID", "Unexpected canonical kind ${orientation.kind}")
            assessment == null ->
                issues += projection.issue("THEME_CUTOVER_ASSESSMENT_MISSING", "Current assessment is missing")
            revision == null || revision.orientationId != mapping.subjectId ->
                issues += projection.issue("THEME_CUTOVER_REVISION_INVALID", "Current assessment revision is missing or invalid")
            mapping.state == LegacySubjectMappingState.CUT_OVER.name &&
                (subject.isDeleted != mapping.isDeleted || subject.isDeleted != assessment.isDeleted) ->
                issues += projection.issue("THEME_CUTOVER_DELETION_DIVERGENCE", "Canonical Theme deletion state is incoherent")
            mapping.state != LegacySubjectMappingState.CUT_OVER.name &&
                (
                    subject.isDeleted != projection.subject.isDeleted ||
                        mapping.isDeleted != projection.subject.isDeleted ||
                        assessment.isDeleted != projection.subject.isDeleted
                ) ->
                issues += projection.issue("THEME_CUTOVER_DELETION_DIVERGENCE", "Theme deletion state diverged before cutover")
            mapping.state != LegacySubjectMappingState.CUT_OVER.name &&
                (subject.title != projection.subject.title || subject.description != projection.subject.description) ->
                issues += projection.issue("THEME_CUTOVER_SHADOW_DIVERGENCE", "Common fields diverged before ownership cutover")
            !assessment.hasSameAxisValues(projection.orientation.assessment) ->
                issues += projection.issue("THEME_CUTOVER_ASSESSMENT_DIVERGENCE", "Theme assessment differs from compatibility projection")
        }
    }

    if (issues.isNotEmpty()) {
        return ThemeOrientationCutoverPlan(emptyList(), emptyList(), emptyList(), issues)
    }

    val orientationChanges = mutableListOf<OrientationEntity>()
    val subjectChanges = mutableListOf<ManagedSubjectEntity>()
    val mappingChanges = mutableListOf<LegacySubjectMappingEntity>()
    themes.forEach { projection ->
        val mapping = mappingsBySource.getValue(projection.source.sourceType.name to projection.source.sourceId).single()
        val orientation = orientationsById.getValue(mapping.subjectId)
        if (orientation.kind != OrientationKind.THEME.name) {
            orientationChanges += orientation.copy(kind = OrientationKind.THEME.name)
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
    return ThemeOrientationCutoverPlan(subjectChanges, orientationChanges, mappingChanges, emptyList())
}

private val THEME_CUTOVER_ALLOWED_MAPPING_STATES =
    setOf(
        LegacySubjectMappingState.MATERIALIZED.name,
        LegacySubjectMappingState.CUT_OVER.name,
    )

private val THEME_CUTOVER_ALLOWED_KINDS =
    setOf(
        OrientationKind.DAY_THEME.name,
        OrientationKind.THEME.name,
    )

private fun LegacySubjectMappingEntity.themeIssue(
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
