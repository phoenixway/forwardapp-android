package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context

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

    // Scoring (Цілі числа для Snapshot, як у Context)
    @SerializedName("valueImportance") val valueImportance: Int,
    @SerializedName("valueImpact") val valueImpact: Int,
    @SerializedName("effort") val effort: Int,
    @SerializedName("cost") val cost: Int,
    @SerializedName("risk") val risk: Int,

    // Weights & Calculated Scores
    @SerializedName("weightEffort") val weightEffort: Float,
    @SerializedName("weightCost") val weightCost: Float,
    @SerializedName("weightRisk") val weightRisk: Float,
    @SerializedName("rawScore") val rawScore: Double,      // Double для точності
    @SerializedName("displayScore") val displayScore: Double, // Double для точності
    @SerializedName("relativeSize") val relativeSize: Int = 0,

    // Parent Context
    @SerializedName("parentValueImportance") val parentValueImportance: Float?,
    @SerializedName("impactOnParentGoal") val impactOnParentGoal: Float?,
    @SerializedName("timeCost") val timeCost: Float?,
    @SerializedName("financialCost") val financialCost: Float?,
)
