package com.romankozak.forwardappmobile.shared.features.aichat.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.aichat.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.ChatMessage
import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.Conversation
import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.ConversationWithLastMessage
import com.romankozak.forwardappmobile.shared.features.aichat.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : ChatRepository {

    override fun observeConversations(): Flow<List<Conversation>> =
        database.conversationsQueries.getAllConversations()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeConversationsByFolder(folderId: Long?): Flow<List<Conversation>> {
        val query = if (folderId == null) {
            database.conversationsQueries.getConversationsWithoutFolder()
        } else {
            database.conversationsQueries.getConversationsByFolder(folderId)
        }
        return query
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeAllConversationsWithLastMessage(): Flow<List<ConversationWithLastMessage>> =
        database.conversationsQueries.getConversationsWithLastMessage()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeConversationsWithLastMessageByFolder(folderId: Long): Flow<List<ConversationWithLastMessage>> =
        database.conversationsQueries.getConversationsWithLastMessageByFolder(folderId)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeConversationsWithLastMessageWithoutFolder(): Flow<List<ConversationWithLastMessage>> =
        database.conversationsQueries.getConversationsWithLastMessageWithoutFolder()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeMessages(conversationId: Long): Flow<List<ChatMessage>> =
        database.chatMessagesQueries.getMessagesForConversation(conversationId)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeMessageCount(conversationId: Long): Flow<Long> =
        database.chatMessagesQueries.countMessagesForConversation(conversationId)
            .asFlow()
            .mapToOne(dispatcher)

    override suspend fun getConversationById(id: Long): Conversation? = withContext(dispatcher) {
        database.conversationsQueries.getConversationById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun createConversation(title: String, folderId: Long?): Long = withContext(dispatcher) {
        database.conversationsQueries.insertConversation(
            title = title,
            creationTimestamp = System.currentTimeMillis(),
            folderId = folderId,
        )
        database.conversationsQueries.getLastInsertedConversationId().executeAsOne()
    }

    override suspend fun updateConversation(conversationId: Long, title: String, folderId: Long?) = withContext(dispatcher) {
        database.conversationsQueries.updateConversation(
            id = conversationId,
            title = title,
            folderId = folderId,
        )
    }

    override suspend fun deleteConversation(conversationId: Long) = withContext(dispatcher) {
        database.transaction {
            database.chatMessagesQueries.deleteMessagesForConversation(conversationId)
            database.conversationsQueries.deleteConversationById(conversationId)
        }
    }

    override suspend fun insertMessage(
        conversationId: Long,
        text: String,
        isFromUser: Boolean,
        isError: Boolean,
        timestamp: Long,
        isStreaming: Boolean,
    ): Long = withContext(dispatcher) {
        database.chatMessagesQueries.insertChatMessage(
            conversationId = conversationId,
            text = text,
            isFromUser = isFromUser,
            isError = isError,
            timestamp = timestamp,
            isStreaming = isStreaming,
        )
        database.chatMessagesQueries.getLastInsertedChatMessageId().executeAsOne()
    }

    override suspend fun getMessageById(id: Long): ChatMessage? = withContext(dispatcher) {
        database.chatMessagesQueries.getMessageById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getLastAssistantMessage(conversationId: Long): ChatMessage? = withContext(dispatcher) {
        database.chatMessagesQueries.getLastAssistantMessage(conversationId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun deleteMessage(messageId: Long) = withContext(dispatcher) {
        database.chatMessagesQueries.deleteMessageById(messageId)
    }

    override suspend fun deleteMessagesForConversation(conversationId: Long) = withContext(dispatcher) {
        database.chatMessagesQueries.deleteMessagesForConversation(conversationId)
    }

    override suspend fun deleteAllConversations() = withContext(dispatcher) {
        database.transaction {
            database.chatMessagesQueries.deleteAllMessages()
            database.conversationsQueries.deleteAllConversations()
        }
    }
}
