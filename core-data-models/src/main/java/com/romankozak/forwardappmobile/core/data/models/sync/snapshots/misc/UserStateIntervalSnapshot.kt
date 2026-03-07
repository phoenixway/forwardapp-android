package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc

import com.google.gson.annotations.SerializedName

data class UserStateIntervalSnapshot(
    @SerializedName("id") val id: Long,
    @SerializedName("stateType") val stateType: String,
    @SerializedName("crisisLevel") val crisisLevel: Int? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("source") val source: String,
    @SerializedName("createdFromActivityId") val createdFromActivityId: String? = null,
    @SerializedName("startedAt") val startedAt: Long,
    @SerializedName("endedAt") val endedAt: Long? = null,
)
