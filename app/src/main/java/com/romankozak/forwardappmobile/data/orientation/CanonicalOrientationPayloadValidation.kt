@file:Suppress("WildcardImport")

package com.romankozak.forwardappmobile.data.orientation

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.shared.core.domain.orientation.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.*

internal fun validateCanonicalPayloadReferences(bundle: SnapshotBundle) {
    val violations = validateCanonicalOrientationReferences(bundle.toCanonicalOrientationValidationGraph())
    require(violations.isEmpty()) { violations.first().message }
}

private fun SnapshotBundle.toCanonicalOrientationValidationGraph(): CanonicalOrientationValidationGraph {
    val gson = Gson()
    return CanonicalOrientationValidationGraph(
        subjects = requireNotNull(managedSubjects).map {
            CanonicalSubjectReference(it.id, ManagedSubjectType.valueOf(it.subjectType))
        },
        orientations = requireNotNull(orientations).map {
            CanonicalOrientationReference(it.subjectId, OrientationKind.valueOf(it.kind))
        },
        aspects = requireNotNull(aspects).map { CanonicalAspectReference(it.subjectId, it.parentAspectId) },
        assessments = requireNotNull(orientationAssessments).map {
            CanonicalCurrentAssessmentReference(it.orientationId, it.revisionId, it.toModel())
        },
        revisions = requireNotNull(orientationAssessmentRevisions).map {
            CanonicalAssessmentRevisionReference(
                id = it.id,
                orientationId = it.orientationId,
                assessment = gson.fromJson(it.assessmentJson, OrientationAssessment::class.java),
            )
        },
        mappings = requireNotNull(legacySubjectMappings).map {
            CanonicalLegacyMappingReference(it.id, it.subjectId)
        },
        relations = requireNotNull(orientationRelations).map { it.toModel() },
        aspectRefs = requireNotNull(aspectOrientationRefs).map { it.toModel() },
        workspaces = workspaces?.map { CanonicalWorkspaceReference(it.id, it.parentWorkspaceId) },
        bindings = requireNotNull(workspaceBindings).map { it.toModel() },
        capabilities = requireNotNull(workspaceCapabilityInstances).map { it.toModel() },
        savedViews = requireNotNull(savedOrientationViews).map {
            CanonicalSavedViewReference(it.id, it.filterAstVersion)
        },
    )
}

private fun com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity.toModel() =
    OrientationAssessment(
        importance = AxisAssessment(importanceValue, ValueOrigin.valueOf(importanceOrigin)),
        impact = AxisAssessment(impactValue, ValueOrigin.valueOf(impactOrigin)),
        breadth = AxisAssessment(breadthValue, ValueOrigin.valueOf(breadthOrigin)),
        expectedSpan = AxisAssessment(expectedSpanValue, ValueOrigin.valueOf(expectedSpanOrigin)),
        targetWindow = AxisAssessment(targetWindowValue, ValueOrigin.valueOf(targetWindowOrigin)),
        attentionTier = AxisAssessment(attentionTierValue, ValueOrigin.valueOf(attentionTierOrigin)),
        commitment = AxisAssessment(commitmentValue, ValueOrigin.valueOf(commitmentOrigin)),
        confidence = AxisAssessment(confidenceValue, ValueOrigin.valueOf(confidenceOrigin)),
    )

private fun com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity.toModel() =
    OrientationRelation(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        fromOrientationId = fromOrientationId,
        toOrientationId = toOrientationId,
        relationType = OrientationRelationType.valueOf(relationType),
        order = relationOrder,
    )

private fun com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectOrientationRefEntity.toModel() =
    AspectOrientationRef(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        aspectId = aspectId,
        orientationId = orientationId,
        relationType = AspectOrientationRelationType.valueOf(relationType),
        isPrimary = isPrimary,
        order = refOrder,
    )

private fun com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity.toModel() =
    WorkspaceBinding(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        workspaceId = workspaceId,
        subjectId = subjectId,
        bindingType = WorkspaceBindingType.valueOf(bindingType),
        isPrimary = isPrimary,
        order = bindingOrder,
    )

private fun com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity.toModel() =
    WorkspaceCapabilityInstance(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        workspaceId = workspaceId,
        capabilityType = WorkspaceCapabilityType.valueOf(capabilityType),
        instanceKey = instanceKey,
        order = capabilityOrder,
        state = WorkspaceCapabilityState.valueOf(state),
        configurationVersion = configurationVersion,
        configuration = configuration,
    )
