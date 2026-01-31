package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context

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
    @SerializedName("order") val order: Long,
    @SerializedName("isAttachmentsExpanded") val isAttachmentsExpanded: Boolean,
    @SerializedName("defaultViewModeName") val defaultViewModeName: String?,
    @SerializedName("isCompleted") val isCompleted: Boolean,
    @SerializedName("isContextManagementEnabled") val isContextManagementEnabled: Boolean?,
    @SerializedName("contextStatus") val contextStatus: String?,
    @SerializedName("contextStatusText") val contextStatusText: String?,
    @SerializedName("contextLogLevel") val contextLogLevel: String?,
    @SerializedName("totalTimeSpentMinutes") val totalTimeSpentMinutes: Long?,
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
    @SerializedName("scoringStatus") val scoringStatus: String,
    @SerializedName("showCheckboxes") val showCheckboxes: Boolean,
    @SerializedName("roleCode") val roleCode: String?
)