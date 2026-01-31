package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.attachments

import com.google.gson.annotations.SerializedName

data class ChecklistItemSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("checklistId") val checklistId: String,
    @SerializedName("text") val text: String,
    @SerializedName("isChecked") val isChecked: Boolean,
    @SerializedName("order") val order: Long,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long
)
