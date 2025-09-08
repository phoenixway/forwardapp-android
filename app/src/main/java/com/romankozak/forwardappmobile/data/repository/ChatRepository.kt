package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ChatDao
import com.romankozak.forwardappmobile.data.database.models.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(private val chatDao: ChatDao) {

    fun getChatHistory(): Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()

    suspend fun addMessage(message: ChatMessageEntity): Long {
        return chatDao.insertMessage(message)
    }

    suspend fun updateMessage(message: ChatMessageEntity) {
        // У вашому файлі тут був insertMessage, що є правильним завдяки
        // OnConflictStrategy.REPLACE, але для ясності можна створити окремий @Update метод
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

    // --- ПОЧАТОК ЗМІНИ: Додано новий метод ---
    suspend fun getMessageById(messageId: Long): ChatMessageEntity? {
        return chatDao.getMessageById(messageId)
    }
    // --- КІНЕЦЬ ЗМІНИ ---
}