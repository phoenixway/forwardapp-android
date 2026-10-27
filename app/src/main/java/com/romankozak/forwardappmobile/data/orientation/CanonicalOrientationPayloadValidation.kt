@file:Suppress("WildcardImport")

package com.romankozak.forwardappmobile.data.orientation

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.toConversionEvidence
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.decodeOrientationAssessmentJson
import com.romankozak.forwardappmobile.shared.core.domain.orientation.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.*

internal fun validateCanonicalPayloadReferences(
    bundle: SnapshotBundle,
    validateEmbeddedWorkspaceTopology: Boolean,
) {
    requireLiveSemanticEndpoints(
        requireNotNull(bundle.managedSubjects), requireNotNull(bundle.orientationRelations),
        requireNotNull(bundle.aspectOrientationRefs), requireNotNull(bundle.workspaceBindings), requireNotNull(bundle.aspects),
    )
    bundle.orientationScoringOverrides?.let { overrides ->
        val orientationIds = requireNotNull(bundle.orientations).mapTo(hashSetOf()) { it.subjectId }
        require(overrides.map { it.orientationId }.distinct().size == overrides.size) {
            "Canonical Orientation scoring payload contains duplicate override ids"
        }
        val policy = DEFAULT_ORIENTATION_SCORING_POLICY_V1
        overrides.forEach { override ->
            require(override.orientationId in orientationIds) {
                "Orientation scoring override ${override.orientationId} has no Orientation target"
            }
            require(
                override.policyId == policy.id &&
                    override.policyVersion == policy.version &&
                    override.formulaVersion == policy.formulaVersion,
            ) { "Unsupported Orientation scoring policy binding" }
            listOf(
                override.importanceCoordinate,
                override.impactCoordinate,
                override.effortWeight,
                override.costWeight,
                override.riskWeight,
            ).filterNotNull().forEach { value ->
                require(value.isFinite() && value >= 0f) {
                    "Orientation scoring override contains an invalid numeric value"
                }
            }
        }
    }
    val violations =
        validateCanonicalOrientationReferences(
            bundle.toCanonicalOrientationValidationGraph(validateEmbeddedWorkspaceTopology),
        )
    require(violations.isEmpty()) { violations.first().message }
}

private fun SnapshotBundle.toCanonicalOrientationValidationGraph(
    validateEmbeddedWorkspaceTopology: Boolean,
): CanonicalOrientationValidationGraph {
    val gson = Gson()
    return CanonicalOrientationValidationGraph(
        subjects = requireNotNull(managedSubjects).map {
            CanonicalSubjectReference(it.id, ManagedSubjectType.valueOf(it.subjectType), it.version, it.isDeleted)
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
                assessment = decodeOrientationAssessmentJson(gson, it.assessmentJson),
            )
        },
        mappings = requireNotNull(legacySubjectMappings).map {
            CanonicalLegacyMappingReference(it.id, it.subjectId)
        },
        relations = requireNotNull(orientationRelations).map { it.toModel() },
        aspectRefs = requireNotNull(aspectOrientationRefs).map { it.toModel() },
        workspaces =
            workspaces?.map {
                CanonicalWorkspaceReference(
                    id = it.id,
                    parentWorkspaceId =
                        it.parentWorkspaceId.takeIf {
                            validateEmbeddedWorkspaceTopology
                        },
                )
            },
        bindings = requireNotNull(workspaceBindings).map { it.toModel() },
        capabilities = requireNotNull(workspaceCapabilityInstances).map { it.toModel() },
        savedViews = requireNotNull(savedOrientationViews).map {
            CanonicalSavedViewReference(it.id, it.filterAstVersion)
        },
        subjectConversions = managedSubjectConversions.orEmpty().map { it.toConversionEvidence() },
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
        effort = AxisAssessment(effortValue, ValueOrigin.valueOf(effortOrigin)),
        cost = AxisAssessment(costValue, ValueOrigin.valueOf(costOrigin)),
        risk = AxisAssessment(riskValue, ValueOrigin.valueOf(riskOrigin)),
        evaluationState = OrientationEvaluationState.valueOf(evaluationState),
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
