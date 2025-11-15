package com.romankozak.forwardappmobile.shared.features.aichat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ConversationWithLastMessage(
    val conversation: Conversation,
    val lastMessage: ChatMessage?,
)
