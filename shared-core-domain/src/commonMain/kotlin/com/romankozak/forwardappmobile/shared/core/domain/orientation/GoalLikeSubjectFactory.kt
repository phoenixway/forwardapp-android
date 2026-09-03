@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.AssessmentRevisionSource
import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.AttentionTierValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.BreadthValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.CommitmentValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ConfidenceValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.CurrentOrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.ExpectedSpanValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ImportanceValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ImpactValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMapping
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessmentRevision
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationLifecycle
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.TargetWindowValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import kotlin.js.JsExport

/** Pure canonical records for a new Goal-like subject. Persistence is owned by the caller. */
@JsExport
data class GoalLikeCanonicalSubject(
    val subject: ManagedSubject,
    val orientation: OrientationNode,
    val currentAssessment: CurrentOrientationAssessment,
    val revision: OrientationAssessmentRevision,
    val mapping: LegacySubjectMapping,
)

/** Primitive JS-friendly projection of the factory result for non-Kotlin clients. */
@JsExport
data class GoalLikeCanonicalSubjectWire(
    val goalId: String,
    val subjectId: String,
    val assessmentId: String,
    val mappingId: String,
    val title: String,
    val description: String?,
    val completed: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val migrationVersion: Int,
    val subjectVersion: Long,
    val mappingState: String,
    val orientationKind: String,
    val orientationLifecycle: String,
    val assessmentImportance: String,
    val assessmentImpact: String,
    val assessmentBreadth: String,
    val assessmentExpectedSpan: String,
    val assessmentTargetWindow: String,
    val assessmentAttentionTier: String,
    val assessmentCommitment: String,
    val assessmentConfidence: String,
)

/**
 * Builds the canonical target graph used by Android Goal creation and future
 * Desktop local-first creation. IDs and time are caller-owned for retry safety.
 */
@JsExport
fun createGoalLikeCanonicalSubject(
    goalId: String,
    subjectId: String,
    assessmentId: String,
    mappingId: String,
    title: String,
    description: String?,
    completed: Boolean,
    createdAt: Long,
    updatedAt: Long,
    migrationVersion: Int,
): GoalLikeCanonicalSubject {
    require(goalId.isNotBlank()) { "goalId must not be blank" }
    require(subjectId.isNotBlank()) { "subjectId must not be blank" }
    require(assessmentId.isNotBlank()) { "assessmentId must not be blank" }
    require(mappingId.isNotBlank()) { "mappingId must not be blank" }
    val assessment = goalLikeAssessment()
    val subject = ManagedSubject(
        id = subjectId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
        subjectType = ManagedSubjectType.ORIENTATION,
        title = title,
        description = description,
    )
    val orientation = OrientationNode(
        subjectId = subjectId,
        kind = OrientationKind.GOAL,
        lifecycle = if (completed) OrientationLifecycle.REALIZED else OrientationLifecycle.ACTIVE,
        lifecycleOrigin = ValueOrigin.DERIVED,
        assessment = assessment,
    )
    val revision = OrientationAssessmentRevision(
        id = assessmentId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
        orientationId = subjectId,
        effectiveFrom = updatedAt,
        recordedAt = updatedAt,
        source = AssessmentRevisionSource.USER,
        reason = "Created from Goal",
        assessment = assessment,
    )
    val mapping = LegacySubjectMapping(
        id = mappingId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
        source = LegacySubjectRef(LegacyOrientationSourceType.GOAL, goalId),
        subjectId = subjectId,
        migrationVersion = migrationVersion,
        state = LegacySubjectMappingState.CUT_OVER,
    )
    return GoalLikeCanonicalSubject(
        subject = subject,
        orientation = orientation,
        currentAssessment = CurrentOrientationAssessment(subjectId, assessmentId, assessment),
        revision = revision,
        mapping = mapping,
    )
}

@JsExport
fun createGoalLikeCanonicalSubjectWire(
    goalId: String,
    subjectId: String,
    assessmentId: String,
    mappingId: String,
    title: String,
    description: String?,
    completed: Boolean,
    createdAt: Long,
    updatedAt: Long,
    migrationVersion: Int,
): GoalLikeCanonicalSubjectWire {
    val graph = createGoalLikeCanonicalSubject(
        goalId, subjectId, assessmentId, mappingId, title, description,
        completed, createdAt, updatedAt, migrationVersion,
    )
    val assessment = graph.orientation.assessment
    return GoalLikeCanonicalSubjectWire(
        goalId = goalId,
        subjectId = subjectId,
        assessmentId = assessmentId,
        mappingId = mappingId,
        title = title,
        description = description,
        completed = completed,
        createdAt = createdAt,
        updatedAt = updatedAt,
        migrationVersion = migrationVersion,
        subjectVersion = graph.subject.version,
        mappingState = graph.mapping.state.name,
        orientationKind = graph.orientation.kind.name,
        orientationLifecycle = graph.orientation.lifecycle!!.name,
        assessmentImportance = assessment.importance.valueCode!!,
        assessmentImpact = assessment.impact.valueCode!!,
        assessmentBreadth = assessment.breadth.valueCode!!,
        assessmentExpectedSpan = assessment.expectedSpan.valueCode!!,
        assessmentTargetWindow = assessment.targetWindow.valueCode!!,
        assessmentAttentionTier = assessment.attentionTier.valueCode!!,
        assessmentCommitment = assessment.commitment.valueCode!!,
        assessmentConfidence = assessment.confidence.valueCode!!,
    )
}

private fun goalLikeAssessment() = OrientationAssessment(
    importance = AxisAssessment(ImportanceValue.MEDIUM.name, ValueOrigin.DERIVED),
    impact = AxisAssessment(ImpactValue.MEDIUM.name, ValueOrigin.DERIVED),
    breadth = AxisAssessment(BreadthValue.LOCAL.name, ValueOrigin.DERIVED),
    expectedSpan = AxisAssessment(ExpectedSpanValue.DAYS.name, ValueOrigin.DERIVED),
    targetWindow = AxisAssessment(TargetWindowValue.NOW.name, ValueOrigin.DERIVED),
    attentionTier = AxisAssessment(AttentionTierValue.P2_NEXT.name, ValueOrigin.DERIVED),
    commitment = AxisAssessment(CommitmentValue.INTENDED.name, ValueOrigin.DERIVED),
    confidence = AxisAssessment(ConfidenceValue.POSSIBLE.name, ValueOrigin.DERIVED),
)
