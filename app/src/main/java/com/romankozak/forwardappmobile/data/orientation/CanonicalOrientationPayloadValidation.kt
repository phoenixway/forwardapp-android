@file:Suppress("WildcardImport")

package com.romankozak.forwardappmobile.data.orientation

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateAspectOrientationRefs
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateCapabilityInstances
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateOrientationAssessment
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateOrientationRelations
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateSingleParentHierarchy
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateWorkspaceBindings
import com.romankozak.forwardappmobile.shared.core.models.orientation.*

internal fun validateCanonicalPayloadReferences(bundle: SnapshotBundle) {
    val subjects = requireNotNull(bundle.managedSubjects).associateBy { it.id }
    val orientationNodes = requireNotNull(bundle.orientations)
    val aspectNodes = requireNotNull(bundle.aspects)
    val orientationIds = orientationNodes.mapTo(hashSetOf()) { it.subjectId }
    val aspectIds = aspectNodes.mapTo(hashSetOf()) { it.subjectId }

    require(orientationIds.all { subjects[it]?.subjectType == ManagedSubjectType.ORIENTATION.name }) {
        "Every Orientation must reference an ORIENTATION ManagedSubject."
    }
    require(aspectIds.all { subjects[it]?.subjectType == ManagedSubjectType.ASPECT.name }) {
        "Every Aspect must reference an ASPECT ManagedSubject."
    }
    require(validateSingleParentHierarchy(aspectNodes.associate { it.subjectId to it.parentAspectId }).isEmpty()) {
        "Aspect hierarchy violates DOMAIN-CONTRACT v1."
    }

    val assessments = requireNotNull(bundle.orientationAssessments)
    require(assessments.all { it.orientationId in orientationIds }) {
        "Every current assessment must reference an Orientation in the payload."
    }
    val kindById = orientationNodes.associate { it.subjectId to OrientationKind.valueOf(it.kind) }
    assessments.forEach { assessment ->
        require(validateOrientationAssessment(kindById.getValue(assessment.orientationId), assessment.toModel()).isEmpty()) {
            "Assessment ${assessment.orientationId} violates DOMAIN-CONTRACT v1."
        }
    }
    val revisions = requireNotNull(bundle.orientationAssessmentRevisions)
    require(revisions.all { it.orientationId in orientationIds }) {
        "Every assessment revision must reference an Orientation in the payload."
    }
    val revisionById = revisions.associateBy { it.id }
    require(assessments.all { revisionById[it.revisionId]?.orientationId == it.orientationId }) {
        "Every current assessment must reference its matching immutable revision."
    }
    val gson = Gson()
    val revisionAssessmentById = mutableMapOf<String, OrientationAssessment>()
    revisions.forEach { revision ->
        val assessment = gson.fromJson(revision.assessmentJson, OrientationAssessment::class.java)
        revisionAssessmentById[revision.id] = assessment
        require(validateOrientationAssessment(kindById.getValue(revision.orientationId), assessment).isEmpty()) {
            "Assessment revision ${revision.id} violates DOMAIN-CONTRACT v1."
        }
    }
    require(
        assessments.all { current ->
            current.toModel().hasSameAxisValues(revisionAssessmentById.getValue(current.revisionId))
        },
    ) {
        "Every current assessment must match the immutable revision it names."
    }
    require(requireNotNull(bundle.legacySubjectMappings).all { it.subjectId in subjects }) {
        "Every legacy mapping must reference a ManagedSubject in the payload."
    }

    val relations = requireNotNull(bundle.orientationRelations).map { it.toModel() }
    require(validateOrientationRelations(orientationIds, relations).isEmpty()) {
        "Orientation relations violate DOMAIN-CONTRACT v1."
    }
    val aspectRefs = requireNotNull(bundle.aspectOrientationRefs).map { it.toModel() }
    require(aspectRefs.all { it.aspectId in aspectIds && it.orientationId in orientationIds }) {
        "Every Aspect reference must use known Aspect and Orientation endpoints."
    }
    require(validateAspectOrientationRefs(aspectRefs).isEmpty()) {
        "Aspect references violate DOMAIN-CONTRACT v1."
    }

    val bindings = requireNotNull(bundle.workspaceBindings).map { it.toModel() }
    require(bindings.all { it.subjectId in subjects }) {
        "Every Workspace binding must reference a ManagedSubject."
    }
    require(validateWorkspaceBindings(bindings).isEmpty()) {
        "Workspace bindings violate DOMAIN-CONTRACT v1."
    }
    val capabilities = requireNotNull(bundle.workspaceCapabilityInstances).map { it.toModel() }
    require(validateCapabilityInstances(capabilities).isEmpty()) {
        "Workspace capabilities violate DOMAIN-CONTRACT v1."
    }
    require(requireNotNull(bundle.savedOrientationViews).all { it.filterAstVersion > 0 }) {
        "Saved Orientation views require a positive filter AST version."
    }
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

private fun OrientationAssessment.hasSameAxisValues(other: OrientationAssessment): Boolean =
    OrientationAxis.entries.all { axis ->
        val left = valueFor(axis)
        val right = other.valueFor(axis)
        left.valueCode == right.valueCode && left.origin == right.origin
    }

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
