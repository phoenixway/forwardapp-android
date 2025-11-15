package com.romankozak.forwardappmobile.shared.features.aichat.domain.repository

import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.ChatMessage
import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.Conversation
import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.ConversationWithLastMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeConversations(): Flow<List<Conversation>>
    fun observeConversationsByFolder(folderId: Long?): Flow<List<Conversation>>
    fun observeAllConversationsWithLastMessage(): Flow<List<ConversationWithLastMessage>>
    fun observeConversationsWithLastMessageByFolder(folderId: Long): Flow<List<ConversationWithLastMessage>>
    fun observeConversationsWithLastMessageWithoutFolder(): Flow<List<ConversationWithLastMessage>>
    fun observeMessages(conversationId: Long): Flow<List<ChatMessage>>
    fun observeMessageCount(conversationId: Long): Flow<Long>

    suspend fun getConversationById(id: Long): Conversation?
    suspend fun createConversation(title: String, folderId: Long?): Long
    suspend fun updateConversation(conversationId: Long, title: String, folderId: Long?)
    suspend fun deleteConversation(conversationId: Long)

    suspend fun insertMessage(
        conversationId: Long,
        text: String,
        isFromUser: Boolean,
        isError: Boolean,
        timestamp: Long,
        isStreaming: Boolean,
    ): Long

    suspend fun getMessageById(id: Long): ChatMessage?
    suspend fun getLastAssistantMessage(conversationId: Long): ChatMessage?
    suspend fun deleteMessage(messageId: Long)
    suspend fun deleteMessagesForConversation(conversationId: Long)
    suspend fun deleteAllConversations()
}
