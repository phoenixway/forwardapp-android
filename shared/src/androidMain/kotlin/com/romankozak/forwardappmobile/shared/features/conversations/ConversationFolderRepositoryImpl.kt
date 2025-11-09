package com.romankozak.forwardappmobile.shared.features.conversations

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.data.database.models.ConversationFolder
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.romankozak.forwardappmobile.shared.features.conversations.toDomain

class ConversationFolderRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : ConversationFolderRepository {

    private val queries = db.conversationFoldersQueries

    override fun getFolders(): Flow<List<ConversationFolder>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { folders -> folders.map { it.toDomain() } }
    }

    override suspend fun addFolder(name: String) {
        withContext(ioDispatcher) {
            queries.insert(name)
        }
    }

    override suspend fun updateFolder(id: Long, name: String) {
        withContext(ioDispatcher) {
            queries.updateName(name = name, id = id)
        }
    }

    override suspend fun deleteFolder(id: Long) {
        withContext(ioDispatcher) {
            queries.deleteById(id)
        }
    }
}