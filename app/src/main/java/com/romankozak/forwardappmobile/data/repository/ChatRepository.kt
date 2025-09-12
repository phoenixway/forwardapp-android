package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ChatDao
import com.romankozak.forwardappmobile.data.database.models.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository
    @Inject
    constructor(
        private val chatDao: ChatDao,
    ) {
        fun getChatHistory(): Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()

        suspend fun addMessage(message: ChatMessageEntity): Long = chatDao.insertMessage(message)

        suspend fun updateMessage(message: ChatMessageEntity) {
            chatDao.insertMessage(message)
        }

        suspend fun clearChat() {
            chatDao.clearAllMessages()
        }

        suspend fun deleteLastAssistantMessage() {
            chatDao.getLastAssistantMessage()?.let {
                chatDao.deleteMessageById(it.id)
            }
        }

        suspend fun getMessageById(messageId: Long): ChatMessageEntity? = chatDao.getMessageById(messageId)

        suspend fun updateMessageText(
            id: Long,
            newText: String,
        ) {
            val message = getMessageById(id)
            message?.let {
                val updatedMessage = it.copy(text = newText)
                updateMessage(updatedMessage)
            }
        }

        suspend fun updateMessageStreamingStatus(
            id: Long,
            isStreaming: Boolean,
        ) {
            val message = getMessageById(id)
            message?.let {
                val updatedMessage = it.copy(isStreaming = isStreaming)
                updateMessage(updatedMessage)
            }
        }
    }
