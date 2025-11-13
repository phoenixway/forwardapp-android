package com.romankozak.forwardappmobile.shared.features.aichat.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.aichat.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.ConversationFolder
import com.romankozak.forwardappmobile.shared.features.aichat.domain.repository.ConversationFolderRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ConversationFolderRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : ConversationFolderRepository {

    override fun observeFolders(): Flow<List<ConversationFolder>> =
        database.conversationFoldersQueries.getConversationFolders()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun insertFolder(name: String): Long = withContext(dispatcher) {
        database.conversationFoldersQueries.insertConversationFolder(name)
        database.conversationFoldersQueries.lastInsertRowId().executeAsOne()
    }

    override suspend fun renameFolder(id: Long, name: String) = withContext(dispatcher) {
        database.conversationFoldersQueries.updateConversationFolderName(name = name, id = id)
    }

    override suspend fun deleteFolder(id: Long) = withContext(dispatcher) {
        database.conversationFoldersQueries.deleteConversationFolder(id)
    }

    override suspend fun deleteAll() = withContext(dispatcher) {
        database.conversationFoldersQueries.deleteAllConversationFolders()
    }
}
