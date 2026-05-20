package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc

import com.google.gson.annotations.SerializedName

data class LifeManagementLevelStatusSnapshot(
    @SerializedName("levelId") val levelId: String,
    @SerializedName("generalStatus") val generalStatus: String,
    @SerializedName("transferStatus") val transferStatus: String,
    @SerializedName("freshnessStatus") val freshnessStatus: String,
    @SerializedName("blockerText") val blockerText: String?,
    @SerializedName("nextActionText") val nextActionText: String?,
    @SerializedName("updatedAt") val updatedAt: Long,
)
