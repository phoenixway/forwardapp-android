package com.romankozak.forwardappmobile.ui.screens.chat

data class ChatMessage(
    val id: Long = 0,
    val conversationId: Long,
    val text: String,
    val isFromUser: Boolean,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val translatedText: String? = null,
    val isTranslating: Boolean = false,
)
