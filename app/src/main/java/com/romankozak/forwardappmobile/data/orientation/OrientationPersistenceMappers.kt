package com.romankozak.forwardappmobile.data.orientation

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.AssessmentRevisionSource
import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.EffectiveOrientation
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment

internal data class CanonicalOrientationRows(
    val subject: ManagedSubjectEntity,
    val orientation: OrientationEntity,
    val assessment: OrientationAssessmentEntity,
    val revision: OrientationAssessmentRevisionEntity,
    val mapping: LegacySubjectMappingEntity,
)

internal fun EffectiveOrientation.toCanonicalRows(
    gson: Gson,
    migrationVersion: Int,
): CanonicalOrientationRows {
    val revisionId =
        LegacySubjectUuid.uuidV5(
            namespace = java.util.UUID.fromString(LegacySubjectUuid.NAMESPACE_UUID),
            name = "${source.sourceType.name}:${source.sourceId}:assessment:$migrationVersion",
        ).toString()
    val mappingId = subject.id
    return CanonicalOrientationRows(
        subject =
            ManagedSubjectEntity(
                id = subject.id,
                subjectType = subject.subjectType.name,
                title = subject.title,
                description = subject.description,
                createdAt = subject.createdAt,
                updatedAt = subject.updatedAt,
                syncedAt = null,
                isDeleted = subject.isDeleted,
                version = subject.version,
            ),
        orientation =
            OrientationEntity(
                subjectId = orientation.subjectId,
                kind = orientation.kind.name,
                lifecycle = orientation.lifecycle?.name,
                lifecycleOrigin = orientation.lifecycleOrigin.name,
            ),
        assessment = orientation.assessment.toEntity(subject.id, revisionId, subject),
        revision =
            OrientationAssessmentRevisionEntity(
                id = revisionId,
                orientationId = subject.id,
                effectiveFrom = subject.updatedAt,
                recordedAt = subject.updatedAt,
                source = AssessmentRevisionSource.MIGRATION.name,
                reason = "Legacy shadow bootstrap v$migrationVersion",
                assessmentJson = gson.toJson(orientation.assessment),
                createdAt = subject.createdAt,
                updatedAt = subject.updatedAt,
                syncedAt = null,
                isDeleted = subject.isDeleted,
                version = subject.version,
            ),
        mapping =
            LegacySubjectMappingEntity(
                id = mappingId,
                sourceType = source.sourceType.name,
                sourceId = source.sourceId,
                subjectId = subject.id,
                migrationVersion = migrationVersion,
                state = LegacySubjectMappingState.MATERIALIZED.name,
                createdAt = subject.createdAt,
                updatedAt = subject.updatedAt,
                syncedAt = null,
                isDeleted = subject.isDeleted,
                version = subject.version,
            ),
    )
}

private fun OrientationAssessment.toEntity(
    orientationId: String,
    revisionId: String,
    subject: com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject,
): OrientationAssessmentEntity =
    OrientationAssessmentEntity(
        orientationId = orientationId,
        revisionId = revisionId,
        importanceValue = importance.valueCode,
        importanceOrigin = importance.origin.name,
        impactValue = impact.valueCode,
        impactOrigin = impact.origin.name,
        breadthValue = breadth.valueCode,
        breadthOrigin = breadth.origin.name,
        expectedSpanValue = expectedSpan.valueCode,
        expectedSpanOrigin = expectedSpan.origin.name,
        targetWindowValue = targetWindow.valueCode,
        targetWindowOrigin = targetWindow.origin.name,
        attentionTierValue = attentionTier.valueCode,
        attentionTierOrigin = attentionTier.origin.name,
        commitmentValue = commitment.valueCode,
        commitmentOrigin = commitment.origin.name,
        confidenceValue = confidence.valueCode,
        confidenceOrigin = confidence.origin.name,
        provenanceJson = Gson().toJson(provenances()),
        createdAt = subject.createdAt,
        updatedAt = subject.updatedAt,
        syncedAt = null,
        isDeleted = subject.isDeleted,
        version = subject.version,
    )

private fun OrientationAssessment.provenances() =
    listOf(importance, impact, breadth, expectedSpan, targetWindow, attentionTier, commitment, confidence)
        .mapNotNull(AxisAssessment::provenance)
