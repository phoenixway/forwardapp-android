package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments

import com.google.gson.annotations.SerializedName

data class ChecklistSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("contextId") val contextId: String?,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)

data class ChecklistItemSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("checklistId") val checklistId: String,
    @SerializedName("text") val text: String, // Мапиться з 'content'
    @SerializedName("isChecked") val isChecked: Boolean,
    @SerializedName("order") val order: Int, // Мапиться з 'itemOrder'
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)