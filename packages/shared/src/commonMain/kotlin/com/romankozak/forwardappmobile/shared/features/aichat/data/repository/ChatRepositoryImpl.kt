package com.romankozak.forwardappmobile.shared.features.aichat.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.aichat.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.Conversation
import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.ChatMessage
import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.ConversationWithLastMessage
import com.romankozak.forwardappmobile.shared.features.aichat.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class ChatRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher
) : ChatRepository {

    // Conversation methods
    override fun getAllConversations(): Flow<List<Conversation>> {
        return db.conversationsQueries.getAllConversations()
            .asFlow()
            .mapToList(dispatcher)
            .map { conversations -> conversations.map { it.toDomain() } }
    }

    override fun getConversationsByFolder(folderId: Long): Flow<List<Conversation>> {
        return db.conversationsQueries.getConversationsByFolder(folderId)
            .asFlow()
            .mapToList(dispatcher)
            .map { conversations -> conversations.map { it.toDomain() } }
    }

    override fun getConversationsWithoutFolder(): Flow<List<Conversation>> {
        return db.conversationsQueries.getConversationsWithoutFolder()
            .asFlow()
            .mapToList(dispatcher)
            .map { conversations -> conversations.map { it.toDomain() } }
    }

    override fun getConversationById(id: Long): Flow<Conversation?> {
        return db.conversationsQueries.getConversationById(id)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { it?.toDomain() }
    }

    override fun observeAllConversationsWithLastMessage(): Flow<List<ConversationWithLastMessage>> {
        return db.conversationsQueries.getConversationsWithLastMessage()
            .asFlow()
            .mapToList(dispatcher)
            .map { conversations -> conversations.map { it.toDomain() } }
    }

    override suspend fun insertConversation(title: String, folderId: Long?): Long {
        return withContext(dispatcher) {
            db.transactionWithResult {
                db.conversationsQueries.insertConversation(
                    title = title,
                    creationTimestamp = Clock.System.now().toEpochMilliseconds(),
                    folderId = folderId
                )
                db.conversationsQueries.getLastInsertedConversationId().executeAsOne()
            }
        }
    }

    override suspend fun updateConversation(id: Long, title: String, folderId: Long?) {
        withContext(dispatcher) {
            db.conversationsQueries.updateConversation(
                id = id,
                title = title,
                folderId = folderId
            )
        }
    }

    override suspend fun deleteConversationById(id: Long) {
        withContext(dispatcher) {
            db.conversationsQueries.deleteConversationById(id)
        }
    }

    override suspend fun deleteAllConversations() {
        withContext(dispatcher) {
            db.conversationsQueries.deleteAllConversations()
        }
    }

    // ChatMessage methods
    override fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessage>> {
        return db.chatMessagesQueries.getMessagesForConversation(conversationId)
            .asFlow()
            .mapToList(dispatcher)
            .map { messages -> messages.map { it.toDomain() } }
    }

    override fun getLastAssistantMessage(conversationId: Long): Flow<ChatMessage?> {
        return db.chatMessagesQueries.getLastAssistantMessage(conversationId)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { it?.toDomain() }
    }

    override fun getMessageById(messageId: Long): Flow<ChatMessage?> {
        return db.chatMessagesQueries.getMessageById(messageId)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { it?.toDomain() }
    }

    override suspend fun insertChatMessage(
        conversationId: Long,
        text: String,
        isFromUser: Boolean,
        isError: Boolean,
        isStreaming: Boolean
    ): Long {
        return withContext(dispatcher) {
            db.transactionWithResult {
                db.chatMessagesQueries.insertChatMessage(
                    conversationId = conversationId,
                    text = text,
                    isFromUser = isFromUser,
                    isError = isError,
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    isStreaming = isStreaming
                )
                db.chatMessagesQueries.getLastInsertedChatMessageId().executeAsOne()
            }
        }
    }

    override suspend fun deleteMessageById(messageId: Long) {
        withContext(dispatcher) {
            db.chatMessagesQueries.deleteMessageById(messageId)
        }
    }

    override suspend fun deleteMessagesForConversation(conversationId: Long) {
        withContext(dispatcher) {
            db.chatMessagesQueries.deleteMessagesForConversation(conversationId)
        }
    }

    override suspend fun deleteAllMessages() {
        withContext(dispatcher) {
            db.chatMessagesQueries.deleteAllMessages()
        }
    }

    override fun countMessagesForConversation(conversationId: Long): Flow<Long> {
        return db.chatMessagesQueries.countMessagesForConversation(conversationId)
            .asFlow()
            .mapToOne(dispatcher)
    }
}
