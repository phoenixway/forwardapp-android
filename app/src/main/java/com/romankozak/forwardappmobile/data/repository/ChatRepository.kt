package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ChatDao
import com.romankozak.forwardappmobile.data.database.models.ChatMessageEntity
import com.romankozak.forwardappmobile.data.database.models.ConversationEntity
import com.romankozak.forwardappmobile.data.database.models.ConversationWithLastMessage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

import com.romankozak.forwardappmobile.data.dao.ConversationFolderDao
import com.romankozak.forwardappmobile.data.database.models.ConversationFolderEntity

@Singleton
class ChatRepository
@Inject
constructor(
    private val chatDao: ChatDao,
    private val conversationFolderDao: ConversationFolderDao
) {
    // ChatMessageEntity related operations
    fun getChatHistory(conversationId: Long): Flow<List<ChatMessageEntity>> = chatDao.getMessagesForConversation(conversationId)

    suspend fun addMessage(message: ChatMessageEntity): Long = chatDao.insertMessage(message)

    suspend fun clearChat(conversationId: Long) {
        chatDao.deleteMessagesForConversation(conversationId)
    }

    // ConversationEntity related operations
    suspend fun createConversation(title: String): Long {
        val conversation = ConversationEntity(title = title)
        return chatDao.insertConversation(conversation)
    }

    suspend fun getConversationById(id: Long): ConversationEntity? = chatDao.getConversationById(id)

    fun getAllConversations(): Flow<List<ConversationEntity>> = chatDao.getAllConversations()

    fun getConversationsWithLastMessage(): Flow<List<ConversationWithLastMessage>> = chatDao.getConversationsWithLastMessage()

    suspend fun updateConversation(conversation: ConversationEntity) {
        chatDao.updateConversation(conversation)
    }

    suspend fun deleteConversation(id: Long) {
        chatDao.deleteConversationAndMessages(id)
    }

    // File: data/repository/ChatRepository.kt

    suspend fun deleteLastAssistantMessage(conversationId: Long) {
        chatDao.getLastAssistantMessageForConversation(conversationId)?.let { message ->
            chatDao.deleteMessageById(message.id)
        }
    }

    // ConversationFolderEntity related operations
    suspend fun createFolder(folderName: String): Long {
        val folder = ConversationFolderEntity(name = folderName)
        return conversationFolderDao.insertFolder(folder)
    }

    fun getAllFolders(): Flow<List<ConversationFolderEntity>> = conversationFolderDao.getAllFolders()

    suspend fun addConversationToFolder(conversationId: Long, folderId: Long) {
        val conversation = chatDao.getConversationById(conversationId)
        if (conversation != null) {
            chatDao.updateConversation(conversation.copy(folderId = folderId))
        }
    }
}
