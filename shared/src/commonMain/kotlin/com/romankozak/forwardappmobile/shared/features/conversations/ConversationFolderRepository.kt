package com.romankozak.forwardappmobile.shared.features.conversations

import com.romankozak.forwardappmobile.shared.data.database.models.ConversationFolder
import kotlinx.coroutines.flow.Flow

interface ConversationFolderRepository {
    fun getFolders(): Flow<List<ConversationFolder>>
    suspend fun addFolder(name: String)
    suspend fun updateFolder(id: Long, name: String)
    suspend fun deleteFolder(id: Long)
}
