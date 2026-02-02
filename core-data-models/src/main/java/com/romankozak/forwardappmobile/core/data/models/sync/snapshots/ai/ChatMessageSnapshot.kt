package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.ai

import com.google.gson.annotations.SerializedName

data class ChatMessageSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("conversationId") val conversationId: String,
    @SerializedName("text") val text: String,
    @SerializedName("isFromUser") val isFromUser: Boolean,
    @SerializedName("isError") val isError: Boolean,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean
)