package com.romankozak.forwardappmobile.shared.features.aichat.domain.model

data class Conversation(
    val id: Long,
    val title: String,
    val creationTimestamp: Long,
    val folderId: Long?,
)

data class ChatMessage(
    val id: Long,
    val conversationId: Long,
    val text: String,
    val isFromUser: Boolean,
    val isError: Boolean,
    val timestamp: Long,
    val isStreaming: Boolean,
)

data class ConversationWithLastMessage(
    val conversation: Conversation,
    val lastMessage: ChatMessage?,
)
