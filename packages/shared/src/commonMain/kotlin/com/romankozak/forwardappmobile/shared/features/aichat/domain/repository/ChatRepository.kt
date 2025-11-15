package com.romankozak.forwardappmobile.shared.features.aichat.domain.repository

import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.Conversation
import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.ChatMessage
import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.ConversationWithLastMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    // Conversation methods
    fun getAllConversations(): Flow<List<Conversation>>
    fun getConversationsByFolder(folderId: Long): Flow<List<Conversation>>
    fun getConversationsWithoutFolder(): Flow<List<Conversation>>
    fun getConversationById(id: Long): Flow<Conversation?>
    fun observeAllConversationsWithLastMessage(): Flow<List<ConversationWithLastMessage>>
    suspend fun insertConversation(title: String, folderId: Long?): Long
    suspend fun updateConversation(id: Long, title: String, folderId: Long?)
    suspend fun deleteConversationById(id: Long)
    suspend fun deleteAllConversations()

    // ChatMessage methods
    fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessage>>
    fun getLastAssistantMessage(conversationId: Long): Flow<ChatMessage?>
    fun getMessageById(messageId: Long): Flow<ChatMessage?>
    suspend fun insertChatMessage(
        conversationId: Long,
        text: String,
        isFromUser: Boolean,
        isError: Boolean,
        isStreaming: Boolean
    ): Long
    suspend fun deleteMessageById(messageId: Long)
    suspend fun deleteMessagesForConversation(conversationId: Long)
    suspend fun deleteAllMessages()
    fun countMessagesForConversation(conversationId: Long): Flow<Long>
}
