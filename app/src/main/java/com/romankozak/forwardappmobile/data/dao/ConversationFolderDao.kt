package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.data.database.models.ConversationFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationFolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: ConversationFolderEntity): Long

    @Query("SELECT * FROM conversation_folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<ConversationFolderEntity>>
}