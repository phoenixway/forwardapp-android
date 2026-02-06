package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context

import com.google.gson.annotations.SerializedName

/**
 * Снапшот системного застосунку, прив'язаного до ключа.
 */
data class SystemAppSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("systemKey") val systemKey: String,
    @SerializedName("appType") val appType: String,
    @SerializedName(value = "contextId", alternate = ["projectId"]) val contextId: String = "",
    @SerializedName("noteDocumentId") val noteDocumentId: String?,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean
)