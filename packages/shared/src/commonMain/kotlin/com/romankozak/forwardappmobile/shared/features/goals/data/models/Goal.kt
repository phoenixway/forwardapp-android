package com.romankozak.forwardappmobile.shared.features.goals.data.models

import com.romankozak.forwardappmobile.shared.data.database.models.ScoringStatusValues
import kotlinx.serialization.Serializable
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink

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
    val valueImportance: Double = 0.0,
    val valueImpact: Double = 0.0,
    val effort: Double = 0.0,
    val cost: Double = 0.0,
    val risk: Double = 0.0,
    val weightEffort: Double = 1.0,
    val weightCost: Double = 1.0,
    val weightRisk: Double = 1.0,
    val rawScore: Double = 0.0,
    val displayScore: Long = 0L,
    val scoringStatus: String = ScoringStatusValues.NOT_ASSESSED,
    val parentValueImportance: Double? = null,
    val impactOnParentGoal: Double? = null,
    val timeCost: Double? = null,
    val financialCost: Double? = null,
    val markdown: String? = null,
)
