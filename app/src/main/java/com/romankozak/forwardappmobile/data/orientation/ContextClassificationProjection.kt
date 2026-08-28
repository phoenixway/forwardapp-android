package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues

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

/** Suggestions are diagnostic only and never authorize migration writes. */
fun Context.classificationSuggestion(): ContextClassificationSuggestion {
    val normalizedRole = roleCode?.trim()?.lowercase()
    val assessed = scoringStatus == ScoringStatusValues.ASSESSED
    return when {
        normalizedRole == "aspect" ->
            ContextClassificationSuggestion(
                contextId = id,
                suggestedOutcome = ContextClassificationOutcome.ASPECT_AND_WORKSPACE,
                reasons = listOf("Reserved aspect role"),
            )

        normalizedRole == "main-beacon" ->
            ContextClassificationSuggestion(
                contextId = id,
                suggestedOutcome = ContextClassificationOutcome.WORKSPACE_WITH_RELATIONS,
                reasons = listOf("Main-beacon role requires an explicit Beacon link before embodiment"),
            )

        normalizedRole == "project" || normalizedRole == "direction" || assessed ->
            ContextClassificationSuggestion(
                contextId = id,
                suggestedOutcome = ContextClassificationOutcome.ORIENTATION_AND_WORKSPACE,
                reasons =
                    buildList {
                        normalizedRole?.let { add("Semantic role: $it") }
                        if (assessed) add("Context carries an assessed legacy score")
                    },
            )

        else ->
            ContextClassificationSuggestion(
                contextId = id,
                suggestedOutcome = ContextClassificationOutcome.REVIEW_REQUIRED,
                reasons = listOf("No deterministic semantic classification signal"),
            )
    }
}
