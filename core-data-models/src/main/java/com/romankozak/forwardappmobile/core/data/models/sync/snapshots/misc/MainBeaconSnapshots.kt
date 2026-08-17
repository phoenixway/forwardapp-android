package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc

import com.google.gson.annotations.SerializedName

data class MainBeaconSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("whyItMatters") val whyItMatters: String?,
    @SerializedName("successShape") val successShape: String?,
    @SerializedName("failureShape") val failureShape: String?,
    @SerializedName("antiGoal") val antiGoal: String?,
    @SerializedName("decisionImpact") val decisionImpact: String?,
    @SerializedName("readinessStatus") val readinessStatus: String,
    @SerializedName("blockerText") val blockerText: String?,
    @SerializedName("nextActionText") val nextActionText: String?,
    @SerializedName("parentBeaconId") val parentBeaconId: String?,
    @SerializedName("order") val order: Long,
    @SerializedName("isExpanded") val isExpanded: Boolean = true,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("createdAt") val createdAt: Long,
)

data class MainBeaconGroupSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("order") val order: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("createdAt") val createdAt: Long,
)

data class MainBeaconGroupMemberSnapshot(
    @SerializedName("groupId") val groupId: String,
    @SerializedName("beaconId") val beaconId: String,
    @SerializedName("order") val order: Long,
)

data class MainBeaconParentLinkSnapshot(
    @SerializedName("parentBeaconId") val parentBeaconId: String,
    @SerializedName("childBeaconId") val childBeaconId: String,
    @SerializedName("order") val order: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("createdAt") val createdAt: Long,
)

data class MainBeaconContextCrossRefSnapshot(
    @SerializedName("beaconId") val beaconId: String,
    @SerializedName("contextId") val contextId: String,
    @SerializedName("order") val order: Long = 0L,
)

data class MainBeaconAttachmentCrossRefSnapshot(
    @SerializedName("beaconId") val beaconId: String,
    @SerializedName("attachmentId") val attachmentId: String,
)

data class MainBeaconLevelStatusSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("mainBeaconId") val mainBeaconId: String,
    @SerializedName("levelType") val levelType: String,
    @SerializedName("generalStatus") val generalStatus: String,
    @SerializedName("syncStatus") val syncStatus: String,
    @SerializedName("blockerText") val blockerText: String?,
    @SerializedName("nextActionText") val nextActionText: String?,
    @SerializedName("updatedAt") val updatedAt: Long,
)
