package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context

import com.google.gson.annotations.SerializedName

data class DirectionItemSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("contextId") val contextId: String,
    @SerializedName("text") val text: String,
    @SerializedName("linkedContextId") val linkedContextId: String? = null,
    @SerializedName("itemOrder") val itemOrder: Int,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("syncedAt") val syncedAt: Long? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("version") val version: Long = 0
)
