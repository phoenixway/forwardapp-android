package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context

import com.google.gson.annotations.SerializedName

/**
 * Снапшот глобального пресету (шаблону) структури контексту.
 */
data class ContextRoleProfileSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("code") val code: String,
    @SerializedName("label") val label: String,
    @SerializedName("description") val description: String?,
    @SerializedName("enableInbox") val enableInbox: Boolean,
    @SerializedName("enableLog") val enableLog: Boolean,
    @SerializedName("enableArtifact") val enableArtifact: Boolean,
    @SerializedName("enableAdvanced") val enableAdvanced: Boolean,
    @SerializedName("enableDashboard") val enableDashboard: Boolean,
    @SerializedName("enableBacklog") val enableBacklog: Boolean,
    @SerializedName("enableAttachments") val enableAttachments: Boolean,
    @SerializedName("enableAutoLinkSubprojects") val enableAutoLinkSubprojects: Boolean,
    @SerializedName("version") val version: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)