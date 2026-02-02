package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context

import com.google.gson.annotations.SerializedName

data class ContextSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("parentId") val parentId: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("isExpanded") val isExpanded: Boolean,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long,
    @SerializedName("tags") val tags: List<String>?,
    @SerializedName("relatedLinks") val relatedLinks: List<RelatedLinkSnapshot>?,
    @SerializedName("order") val order: Int, // Виправлено: Long -> Int
    @SerializedName("isAttachmentsExpanded") val isAttachmentsExpanded: Boolean,
    @SerializedName("defaultViewModeName") val defaultViewModeName: String?,
    @SerializedName("isCompleted") val isCompleted: Boolean,
    @SerializedName("isContextManagementEnabled") val isContextManagementEnabled: Boolean?,
    @SerializedName("contextStatus") val contextStatus: String?,
    @SerializedName("contextStatusText") val contextStatusText: String?,
    @SerializedName("contextLogLevel") val contextLogLevel: String?,
    @SerializedName("totalTimeSpentMinutes") val totalTimeSpentMinutes: Long, // Виправлено: Long? -> Long

    // Scoring (Цілі числа в базі)
    @SerializedName("valueImportance") val valueImportance: Int, // Виправлено: Float -> Int
    @SerializedName("valueImpact") val valueImpact: Int,        // Виправлено: Float -> Int
    @SerializedName("effort") val effort: Int,                  // Виправлено: Float -> Int
    @SerializedName("cost") val cost: Int,                      // Виправлено: Float -> Int
    @SerializedName("risk") val risk: Int,                      // Виправлено: Float -> Int

    // Weights (Дійсні числа)
    @SerializedName("weightEffort") val weightEffort: Float,
    @SerializedName("weightCost") val weightCost: Float,
    @SerializedName("weightRisk") val weightRisk: Float,
    @SerializedName("rawScore") val rawScore: Double,          // Виправлено: Float -> Double
    @SerializedName("displayScore") val displayScore: Double,  // Виправлено: Int -> Double

    @SerializedName("scoringStatus") val scoringStatus: String,
    @SerializedName("showCheckboxes") val showCheckboxes: Boolean,
    @SerializedName("roleCode") val roleCode: String?,
)