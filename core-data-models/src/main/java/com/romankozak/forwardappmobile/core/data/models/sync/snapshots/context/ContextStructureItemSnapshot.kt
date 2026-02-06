package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context

import com.google.gson.annotations.SerializedName

/**
 * Снапшот елемента структури (ролі) в конкретній конфігурації.
 */
data class ContextStructureItemSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("contextStructureId") val contextStructureId: String,
    @SerializedName("entityType") val entityType: String,
    @SerializedName("roleCode") val roleCode: String,
    @SerializedName("containerType") val containerType: String?,
    @SerializedName("title") val title: String,
    @SerializedName("mandatory") val mandatory: Boolean,
    @SerializedName("isEnabled") val isEnabled: Boolean,
    @SerializedName("version") val version: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean
)