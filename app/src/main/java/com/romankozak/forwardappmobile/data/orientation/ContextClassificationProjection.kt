package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef

enum class ContextClassificationOutcome {
    WORKSPACE_ONLY,
    ASPECT_AND_WORKSPACE,
    ORIENTATION_AND_WORKSPACE,
    WORKSPACE_WITH_RELATIONS,
    SYSTEM_OR_COMPATIBILITY_WORKSPACE,
    REVIEW_REQUIRED,
}

data class ContextClassificationSuggestion(
    val contextId: String,
    val suggestedOutcome: ContextClassificationOutcome,
    val reasons: List<String>,
    val requiresReview: Boolean = true,
)

enum class ContextClassificationConfidence {
    HIGH,
    MEDIUM,
    LOW,
}

/** Read-only result: proposed ids are stable, but no canonical or legacy row has been created. */
data class ContextClassificationPreview(
    val contextId: String,
    val compatibilityWorkspaceId: String,
    val suggestedOutcome: ContextClassificationOutcome,
    val confidence: ContextClassificationConfidence,
    val reasons: List<String>,
    val proposedAspectId: String? = null,
    val proposedOrientationId: String? = null,
    val requiresReview: Boolean = true,
)

/** Previews are diagnostic only and never authorize migration writes. */
fun Context.classificationPreview(): ContextClassificationPreview {
    val normalizedRole = roleCode?.trim()?.lowercase()
    val assessed = scoringStatus == ScoringStatusValues.ASSESSED
    val base =
        when {
            SystemContexts.isSystem(ContextId(id)) ->
                ClassificationBase(
                    ContextClassificationOutcome.SYSTEM_OR_COMPATIBILITY_WORKSPACE,
                    ContextClassificationConfidence.HIGH,
                    listOf("Reserved system Context identity"),
                )

            normalizedRole == ContextRoleRegistry.ROLE_ASPECT ->
                ClassificationBase(
                    ContextClassificationOutcome.ASPECT_AND_WORKSPACE,
                    ContextClassificationConfidence.HIGH,
                    listOf("Reserved aspect role"),
                )

            normalizedRole == ContextRoleRegistry.ROLE_MAIN_BEACON ->
                ClassificationBase(
                    ContextClassificationOutcome.WORKSPACE_WITH_RELATIONS,
                    ContextClassificationConfidence.HIGH,
                    listOf("Main-beacon role requires an explicit Beacon link before embodiment"),
                )

            normalizedRole == ContextRoleRegistry.ROLE_PROJECT ||
                normalizedRole == ContextRoleRegistry.ROLE_DIRECTION ||
                assessed ->
                ClassificationBase(
                    ContextClassificationOutcome.ORIENTATION_AND_WORKSPACE,
                    if (normalizedRole != null) {
                        ContextClassificationConfidence.HIGH
                    } else {
                        ContextClassificationConfidence.MEDIUM
                    },
                    buildList {
                        normalizedRole?.let { add("Semantic role: $it") }
                        if (assessed) add("Context carries an assessed legacy score")
                    },
                )

            normalizedRole in operationalWorkspaceRoles ->
                ClassificationBase(
                    ContextClassificationOutcome.WORKSPACE_ONLY,
                    ContextClassificationConfidence.MEDIUM,
                    listOf(
                        "Operational role '$normalizedRole' describes Workspace capabilities, not semantic identity",
                    ),
                )

            else ->
                ClassificationBase(
                    ContextClassificationOutcome.REVIEW_REQUIRED,
                    ContextClassificationConfidence.LOW,
                    listOf(
                        "No deterministic semantic classification signal; " +
                            "keep the Context as a compatibility Workspace",
                    ),
                )
        }
    return ContextClassificationPreview(
        contextId = id,
        compatibilityWorkspaceId = id,
        suggestedOutcome = base.outcome,
        confidence = base.confidence,
        reasons = base.reasons,
        proposedAspectId =
            if (base.outcome == ContextClassificationOutcome.ASPECT_AND_WORKSPACE) stableContextSubjectId(id) else null,
        proposedOrientationId =
            if (base.outcome == ContextClassificationOutcome.ORIENTATION_AND_WORKSPACE) {
                stableContextSubjectId(id)
            } else {
                null
            },
    )
}

fun Iterable<Context>.classificationPreviews(): List<ContextClassificationPreview> =
    map { it.classificationPreview() }

/** Compatibility adapter retained for callers that only consume the original summary. */
fun Context.classificationSuggestion(): ContextClassificationSuggestion {
    val preview = classificationPreview()
    return ContextClassificationSuggestion(
        contextId = preview.contextId,
        suggestedOutcome = preview.suggestedOutcome,
        reasons = preview.reasons,
        requiresReview = preview.requiresReview,
    )
}

private data class ClassificationBase(
    val outcome: ContextClassificationOutcome,
    val confidence: ContextClassificationConfidence,
    val reasons: List<String>,
)

private val operationalWorkspaceRoles =
    setOf(
        ContextRoleRegistry.ROLE_MANAGEMENT,
        ContextRoleRegistry.ROLE_CRISIS_CASE,
        ContextRoleRegistry.ROLE_VET_PATIENT,
        ContextRoleRegistry.ROLE_DEVELOPMENT,
        ContextRoleRegistry.ROLE_DEFAULT,
    )

private fun stableContextSubjectId(contextId: String): String =
    LegacySubjectUuid.resolve(LegacySubjectRef(LegacyOrientationSourceType.CONTEXT, contextId))
