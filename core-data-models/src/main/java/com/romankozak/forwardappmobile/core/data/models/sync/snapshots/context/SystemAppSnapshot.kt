package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context

import com.google.gson.annotations.SerializedName

/**
 * Снапшот системного застосунку, прив'язаного до ключа.
 */
data class SystemAppSnapshot(
    val id: String,
    val systemKey: String,
    val appType: String,
    @SerializedName(value = "contextId", alternate = ["projectId"]) val contextId: String = "",
    val noteDocumentId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean
)