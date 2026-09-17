package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context

import com.google.gson.annotations.SerializedName

/**
 * Снапшот системного застосунку, прив'язаного до ключа.
 */
data class SystemAppSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("systemKey") val systemKey: String,
    @SerializedName("appType") val appType: String,
    /** Canonical SystemApp operational owner. */
    @SerializedName("workspaceId") val workspaceId: String? = null,
    /**
     * Historical full-backup input only. Current exports never populate this
     * legacy Context owner field.
     */
    @SerializedName(value = "contextId", alternate = ["projectId"])
    val legacyContextId: String? = null,
    @SerializedName("noteDocumentId") val noteDocumentId: String?,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean
)
