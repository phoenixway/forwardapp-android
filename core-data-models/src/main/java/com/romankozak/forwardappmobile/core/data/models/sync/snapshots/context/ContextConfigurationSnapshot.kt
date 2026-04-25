package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context

import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.core.capability.CapabilityId

/**
 * Снапшот конфігурації функцій конкретного контексту.
 */
data class ContextConfigurationSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("contextId") val contextId: String,
    @SerializedName("basePresetCode") val basePresetCode: String,
    @SerializedName("experimentalCapabilityIds") val experimentalCapabilityIds: List<CapabilityId>? = emptyList(),
    @SerializedName("applyMode") val applyMode: String,
    @SerializedName("enableInbox") val enableInbox: Boolean,
    @SerializedName("enableLog") val enableLog: Boolean,
    @SerializedName("enableArtifact") val enableArtifact: Boolean,
    @SerializedName("enableAdvanced") val enableAdvanced: Boolean,
    @SerializedName("enableDashboard") val enableDashboard: Boolean,
    @SerializedName("enableBacklog") val enableBacklog: Boolean,
    @SerializedName("enableAttachments") val enableAttachments: Boolean,
    @SerializedName("enableAutoLinkSubprojects") val enableAutoLinkSubprojects: Boolean,
    @SerializedName("removeInboxEntryAfterTagAutocopy") val removeInboxEntryAfterTagAutocopy: Boolean? = false,
    @SerializedName("version") val version: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
)
