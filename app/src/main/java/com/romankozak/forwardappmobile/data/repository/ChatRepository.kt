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

    suspend fun getMessageById(messageId: Long): ChatMessageEntity? {
        return chatDao.getMessageById(messageId)
    }

    // --- ПОЧАТОК ЗМІНИ: Реалізація відсутніх методів ---

    suspend fun updateMessageText(id: Long, newText: String) {
        // Отримуємо повідомлення за ID
        val message = getMessageById(id)
        message?.let {
            // Створюємо копію об'єкта з новим текстом
            val updatedMessage = it.copy(text = newText)
            // Зберігаємо оновлений об'єкт. Room замінить старий завдяки OnConflictStrategy.REPLACE
            updateMessage(updatedMessage)
        }
    }

    suspend fun updateMessageStreamingStatus(id: Long, isStreaming: Boolean) {
        // Та сама логіка для оновлення статусу стрімінгу
        val message = getMessageById(id)
        message?.let {
            val updatedMessage = it.copy(isStreaming = isStreaming)
            updateMessage(updatedMessage)
        }
    }
    // --- КІНЕЦЬ ЗМІНИ ---
}