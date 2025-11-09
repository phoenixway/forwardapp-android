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

class ConversationFolderRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : ConversationFolderRepository {

    override fun getFolders(): Flow<List<ConversationFolder>> {
        return db.conversationFolderQueries.selectAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { folders -> folders.map { it.toDomain() } }
    }

    override suspend fun addFolder(name: String) {
        withContext(ioDispatcher) {
            db.conversationFolderQueries.insert(name)
        }
    }

    override suspend fun updateFolder(id: Long, name: String) {
        withContext(ioDispatcher) {
            db.conversationFolderQueries.updateName(name = name, id = id)
        }
    }

    override suspend fun deleteFolder(id: Long) {
        withContext(ioDispatcher) {
            db.conversationFolderQueries.deleteById(id)
        }
    }
}
