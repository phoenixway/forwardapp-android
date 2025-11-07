package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.romankozak.forwardappmobile.core.database.models.ChatMessageEntity
import com.romankozak.forwardappmobile.core.database.models.ConversationEntity
import com.romankozak.forwardappmobile.core.database.models.ConversationWithLastMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // ChatMessageEntity DAOs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId AND isFromUser = 0 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastAssistantMessageForConversation(conversationId: Long): ChatMessageEntity?

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: Long)

    @Query("SELECT * FROM chat_messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Long): ChatMessageEntity?

    // ConversationEntity DAOs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversations ORDER BY creationTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Transaction
    @Query("SELECT * FROM conversations ORDER BY creationTimestamp DESC")
    fun getConversationsWithLastMessage(): Flow<List<ConversationWithLastMessage>>

    @Transaction
    @Query("SELECT * FROM conversations WHERE folderId = :folderId ORDER BY creationTimestamp DESC")
    fun getConversationsWithLastMessageByFolder(folderId: Long): Flow<List<ConversationWithLastMessage>>

    @Transaction
    @Query("SELECT * FROM conversations WHERE folderId IS NULL ORDER BY creationTimestamp DESC")
    fun getConversationsWithLastMessageWithoutFolder(): Flow<List<ConversationWithLastMessage>>

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Transaction
    suspend fun deleteConversationAndMessages(conversationId: Long) {
        deleteMessagesForConversation(conversationId)
        deleteConversation(conversationId)
    }

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: Long)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: Long)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :conversationId")
    fun getMessageCountForConversation(conversationId: Long): Flow<Int>
}

