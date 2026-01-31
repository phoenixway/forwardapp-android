package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context

import com.google.gson.annotations.SerializedName

data class BacklogItemSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("contextId") val contextId: String,
    @SerializedName("itemType") val itemType: String,
    @SerializedName("entityId") val entityId: String,
    @SerializedName("order") val order: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean
)