package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.misc

import com.google.gson.annotations.SerializedName

data class RecentProjectEntrySnapshot(
    @SerializedName("contextId") val contextId: String,
    @SerializedName("timestamp") val timestamp: Long
)
