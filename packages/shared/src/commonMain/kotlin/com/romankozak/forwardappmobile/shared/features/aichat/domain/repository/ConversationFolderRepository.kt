package com.romankozak.forwardappmobile.shared.features.aichat.domain.repository

import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.ConversationFolder
import kotlinx.coroutines.flow.Flow

interface ConversationFolderRepository {
    fun observeFolders(): Flow<List<ConversationFolder>>
    suspend fun insertFolder(name: String): Long
    suspend fun renameFolder(id: Long, name: String)
    suspend fun deleteFolder(id: Long)
    suspend fun deleteAll()
}
