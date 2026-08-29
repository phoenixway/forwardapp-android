package com.romankozak.forwardappmobile.data.orientation

import androidx.room.withTransaction
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateOrientationAssessment
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateSingleParentHierarchy
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessmentRevision
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Canonical write boundary. Legacy repositories remain unchanged until a later cutover phase. */
@Singleton
class CanonicalOrientationRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val dao: OrientationDao,
    ) {
        private val gson = Gson()

        fun observeSubjects(): Flow<List<ManagedSubjectEntity>> = dao.observeManagedSubjects()

        suspend fun saveOrientation(
            subject: ManagedSubject,
            orientation: OrientationNode,
            revision: OrientationAssessmentRevision,
        ) {
            require(subject.subjectType == ManagedSubjectType.ORIENTATION)
            require(subject.id == orientation.subjectId)
            require(revision.orientationId == orientation.subjectId)
            require(revision.assessment == orientation.assessment)
            require(validateOrientationAssessment(orientation.kind, revision.assessment).isEmpty()) {
                "Orientation assessment violates DOMAIN-CONTRACT v1"
            }
            database.withTransaction {
                dao.upsertManagedSubjects(listOf(subject.toEntity()))
                dao.upsertOrientations(listOf(orientation.toEntity()))
                dao.upsertAssessmentRevisions(listOf(revision.toEntity(gson)))
                dao.upsertAssessments(listOf(revision.toCurrentEntity(gson)))
            }
        }

        @Deprecated("Use CanonicalAspectRepository lifecycle commands")
        suspend fun saveAspect(subject: ManagedSubject, aspect: AspectNode) {
            require(subject.subjectType == ManagedSubjectType.ASPECT)
            require(subject.id == aspect.subjectId)
            database.withTransaction {
                val hierarchy =
                    dao.getAllAspects().associate { it.subjectId to it.parentAspectId }.toMutableMap()
                        .also { it[aspect.subjectId] = aspect.parentAspectId }
                require(validateSingleParentHierarchy(hierarchy).isEmpty()) {
                    "Aspect hierarchy violates DOMAIN-CONTRACT v1"
                }
                dao.upsertManagedSubjects(listOf(subject.toEntity()))
                dao.upsertAspects(listOf(aspect.toEntity()))
            }
        }
    }

private fun ManagedSubject.toEntity() =
    ManagedSubjectEntity(
        id = id,
        subjectType = subjectType.name,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

private fun OrientationNode.toEntity() =
    OrientationEntity(
        subjectId = subjectId,
        kind = kind.name,
        lifecycle = lifecycle?.name,
        lifecycleOrigin = lifecycleOrigin.name,
    )

private fun AspectNode.toEntity() =
    AspectEntity(
        subjectId = subjectId,
        parentAspectId = parentAspectId,
        aspectOrder = order,
        archived = archived,
    )

private fun OrientationAssessmentRevision.toEntity(gson: Gson) =
    OrientationAssessmentRevisionEntity(
        id = id,
        orientationId = orientationId,
        effectiveFrom = effectiveFrom,
        recordedAt = recordedAt,
        source = source.name,
        reason = reason,
        assessmentJson = gson.toJson(assessment),
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

private fun OrientationAssessmentRevision.toCurrentEntity(gson: Gson) =
    assessment.toCurrentEntity(
        orientationId = orientationId,
        revisionId = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        gson = gson,
    )

private fun OrientationAssessment.toCurrentEntity(
    orientationId: String,
    revisionId: String,
    createdAt: Long,
    updatedAt: Long,
    syncedAt: Long?,
    isDeleted: Boolean,
    version: Long,
    gson: Gson,
) = OrientationAssessmentEntity(
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
    provenanceJson = gson.toJson(
        listOf(importance, impact, breadth, expectedSpan, targetWindow, attentionTier, commitment, confidence)
            .mapNotNull { it.provenance },
    ),
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncedAt = syncedAt,
    isDeleted = isDeleted,
    version = version,
)
