package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context

import com.google.gson.annotations.SerializedName

/**
 * Снапшот артефакту контексту (текстовий опис/підсумок).
 */
data class ContextArtifactSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("contextId") val contextId: String,
    @SerializedName("content") val content: String,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long
)