package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context

import com.google.gson.annotations.SerializedName

data class ContextInboxSortingSnapshot(
    @SerializedName("contextId") val contextId: String,
    @SerializedName("rulesText") val rulesText: String,
    @SerializedName("updatedAt") val updatedAt: Long,
)
