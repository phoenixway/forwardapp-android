package com.romankozak.forwardappmobile.shared.features.goals.data.models

import kotlinx.serialization.Serializable
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.models.ScoringStatusValues

@Serializable
data class Goal(
    val id: String,
    val text: String,
    val description: String? = null,
    val completed: Boolean,
    val createdAt: Long,
    val updatedAt: Long?,
    val tags: List<String>? = null,
    val relatedLinks: List<RelatedLink>? = null,
    val valueImportance: Float = 0f,
    val valueImpact: Float = 0f,
    val effort: Float = 0f,
    val cost: Float = 0f,
    val risk: Float = 0f,
    val weightEffort: Float = 1f,
    val weightCost: Float = 1f,
    val weightRisk: Float = 1f,
    val rawScore: Float = 0f,
    val displayScore: Int = 0,
    val scoringStatus: String = ScoringStatusValues.NOT_ASSESSED,
    val parentValueImportance: Float? = null,
    val impactOnParentGoal: Float? = null,
    val timeCost: Float? = null,
    val financialCost: Float? = null,
)
