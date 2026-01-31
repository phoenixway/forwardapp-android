package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context

import com.google.gson.annotations.SerializedName

data class GoalSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("text") val text: String,
    @SerializedName("description") val description: String?,
    @SerializedName("isCompleted") val isCompleted: Boolean,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("tags") val tags: List<String>,
    @SerializedName("scoringStatus") val scoringStatus: String,
    @SerializedName("valueImportance") val valueImportance: Float,
    @SerializedName("valueImpact") val valueImpact: Float,
    @SerializedName("effort") val effort: Float,
    @SerializedName("cost") val cost: Float,
    @SerializedName("risk") val risk: Float,
    @SerializedName("weightEffort") val weightEffort: Float,
    @SerializedName("weightCost") val weightCost: Float,
    @SerializedName("weightRisk") val weightRisk: Float,
    @SerializedName("rawScore") val rawScore: Float,
    @SerializedName("displayScore") val displayScore: Int,
    @SerializedName("parentValueImportance") val parentValueImportance: Float?,
    @SerializedName("impactOnParentGoal") val impactOnParentGoal: Float?,
    @SerializedName("timeCost") val timeCost: Float?,
    @SerializedName("financialCost") val financialCost: Float?
)