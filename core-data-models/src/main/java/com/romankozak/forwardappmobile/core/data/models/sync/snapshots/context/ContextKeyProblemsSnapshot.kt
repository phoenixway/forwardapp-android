package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context

import com.google.gson.annotations.SerializedName

data class ContextKeyProblemsSnapshot(
    @SerializedName("contextId") val contextId: String,
    @SerializedName("payloadJson") val payloadJson: String,
    @SerializedName("updatedAt") val updatedAt: Long,
)
