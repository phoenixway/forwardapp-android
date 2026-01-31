package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.ai

import com.google.gson.annotations.SerializedName

data class ConversationSnapshot(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("creationTimestamp") val creationTimestamp: Long,
    @SerializedName("folderId") val folderId: Long?
)
