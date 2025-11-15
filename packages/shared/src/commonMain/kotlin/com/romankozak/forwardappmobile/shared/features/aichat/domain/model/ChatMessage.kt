package com.romankozak.forwardappmobile.shared.features.aichat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: Long,
    val conversationId: Long,
    val text: String,
    val isFromUser: Boolean,
    val isError: Boolean,
    val timestamp: Long,
    val isStreaming: Boolean,
)
