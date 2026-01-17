package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ChatDao
import com.romankozak.forwardappmobile.data.dao.ConversationFolderDao
import com.romankozak.forwardappmobile.data.database.models.ChatMessageEntity
import com.romankozak.forwardappmobile.data.database.models.ConversationEntity
import com.romankozak.forwardappmobile.data.database.models.ConversationFolderEntity
import com.romankozak.forwardappmobile.data.database.models.ConversationWithLastMessage
import com.romankozak.forwardappmobile.features.ai.chat.DrawerItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

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

    suspend fun getConversationByTitle(title: String): ConversationEntity? = chatDao.getConversationByTitle(title)

    fun getConversationsWithLastMessage(): Flow<List<ConversationWithLastMessage>> = chatDao.getConversationsWithLastMessage()

    fun getDrawerItems(): Flow<List<DrawerItem>> {
        return conversationFolderDao.getAllFolders().flatMapLatest { folders ->
            val folderFlows: List<Flow<DrawerItem.Folder>> = folders.map { folder ->
                chatDao.getConversationsWithLastMessageByFolder(folder.id).map { conversations ->
                    DrawerItem.Folder(folder, conversations)
                }
            }
            val conversationsFlow: Flow<List<DrawerItem.Conversation>> = chatDao.getConversationsWithLastMessageWithoutFolder().map { conversations ->
                conversations.map { DrawerItem.Conversation(it) }
            }

            if (folderFlows.isNotEmpty()) {
                combine(folderFlows) { folderItemsArray: Array<DrawerItem.Folder> ->
                    folderItemsArray.toList()
                }.flatMapLatest { combinedFolderItems: List<DrawerItem.Folder> ->
                    conversationsFlow.map { conversationItems: List<DrawerItem.Conversation> ->
                        combinedFolderItems + conversationItems
                    }
                }
            } else {
                conversationsFlow.map { it } // Ensure it's a Flow<List<DrawerItem>>
            }
        }
    }

    suspend fun updateConversation(conversation: ConversationEntity) {
        chatDao.updateConversation(conversation)
    }

    suspend fun deleteConversation(id: Long) {
        chatDao.deleteConversationAndMessages(id)
    }

    suspend fun deleteLastAssistantMessage(conversationId: Long) {
        chatDao.getLastAssistantMessageForConversation(conversationId)?.let { message ->
            chatDao.deleteMessageById(message.id)
        }
    }

    suspend fun updateMessageContent(
        messageId: Long,
        text: String,
        isStreaming: Boolean,
        isError: Boolean = false,
    ) {
        chatDao.updateMessageContent(
            messageId = messageId,
            text = text,
            isStreaming = isStreaming,
            isError = isError,
        )
    }

    suspend fun getMessageById(messageId: Long) = chatDao.getMessageById(messageId)

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
