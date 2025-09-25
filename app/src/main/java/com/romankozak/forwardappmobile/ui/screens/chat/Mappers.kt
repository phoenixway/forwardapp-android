package com.romankozak.forwardappmobile.ui.screens.chat

import com.romankozak.forwardappmobile.data.database.models.ChatMessageEntity

fun ChatMessageEntity.toChatMessage(conversationId: Long) =
    ChatMessage(
        id = this.id,
        conversationId = conversationId,
        text = this.text,
        isFromUser = this.isFromUser,
        isError = this.isError,
        timestamp = this.timestamp,
        isStreaming = this.isStreaming,
        translatedText = null,
        isTranslating = false,
    )

fun ChatMessage.toEntity() =
    ChatMessageEntity(
        id = this.id,
        conversationId = this.conversationId,
        text = this.text,
        isFromUser = this.isFromUser,
        isError = this.isError,
        timestamp = this.timestamp,
        isStreaming = this.isStreaming,
    )
