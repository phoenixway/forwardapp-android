package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai

import com.google.gson.annotations.SerializedName

data class ConversationFolderSnapshot(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String
)
