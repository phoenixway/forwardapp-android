package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management

import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.RelatedLinkSnapshot

data class DayFocusItemSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("dayPlanId") val dayPlanId: String,
    @SerializedName("title") val title: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("relatedLinks") val relatedLinks: List<RelatedLinkSnapshot> = emptyList(),
    @SerializedName("type") val type: String,
    @SerializedName("isEveryday") val isEveryday: Boolean,
    @SerializedName("recurringKey") val recurringKey: String?,
    @SerializedName("budgetPercent") val budgetPercent: Int? = null,
    @SerializedName("order") val order: Long,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("syncedAt") val syncedAt: Long?,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long,
)
