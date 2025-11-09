package com.romankozak.forwardappmobile.shared.data.database.models

data class Goal(
    val id: String,
    val text: String,
    val description: String? = null,
    val completed: Boolean,
    val createdAt: Long,
    val updatedAt: Long?,
    val tags: List<String>? = null,
    val relatedLinks: RelatedLinkList? = null,
    val valueImportance: Float = 0.0f,
    val valueImpact: Float = 0.0f,
    val effort: Float = 0.0f,
    val cost: Float = 0.0f,
    val risk: Float = 0.0f,
    val weightEffort: Float = 1.0f,
    val weightCost: Float = 1.0f,
    val weightRisk: Float = 1.0f,
    val rawScore: Float = 0.0f,
    val displayScore: Int = 0,
    val scoringStatus: String,
    val parentValueImportance: Float? = null,
    val impactOnParentGoal: Float? = null,
    val timeCost: Float? = null,
    val financialCost: Float? = null,
    val markdown: String? = null
)