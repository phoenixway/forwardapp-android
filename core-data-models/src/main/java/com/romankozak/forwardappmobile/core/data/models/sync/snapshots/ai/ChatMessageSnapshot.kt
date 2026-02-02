package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai

import com.google.gson.annotations.SerializedName

data class ChatMessageSnapshot(
    @SerializedName("id") val id: Long,
    @SerializedName("conversationId") val conversationId: Long,
    @SerializedName("text") val text: String,
    @SerializedName("isFromUser") val isFromUser: Boolean,
    @SerializedName("isError") val isError: Boolean,
    @SerializedName("timestamp") val timestamp: Long
)
