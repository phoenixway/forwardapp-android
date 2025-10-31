package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentItemEntity
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteDocumentRepository @Inject constructor(
    private val noteDocumentDao: NoteDocumentDao,
    private val listItemDao: ListItemDao,
    private val recentItemsRepository: RecentItemsRepository,
) {
    private val TAG = "NoteDocumentRepository"

    fun getDocumentsForProject(projectId: String): Flow<List<NoteDocumentEntity>> =
        noteDocumentDao.getDocumentsForProject(projectId)

    fun getAllDocumentsAsFlow(): Flow<List<NoteDocumentEntity>> =
        noteDocumentDao.getAllDocumentsAsFlow()

    @Transaction
    suspend fun createDocument(
        name: String,
        projectId: String,
        content: String? = null,
    ): String {
        Log.d(TAG, "createDocument called with name: $name, projectId: $projectId, content: $content")
        val document = NoteDocumentEntity(name = name, projectId = projectId, content = content)
        Log.d(TAG, "Inserting new note document: $document")
        noteDocumentDao.insertDocument(document)

        val newListItem =
            ListItem(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                itemType = ListItemTypeValues.CUSTOM_LIST,
                entityId = document.id,
                order = -System.currentTimeMillis(),
            )
        Log.d(TAG, "Inserting new list item: $newListItem")
        listItemDao.insertItem(newListItem)
        Log.d(TAG, "createDocument finished")
        return document.id
    }

    @Transaction
    suspend fun deleteDocument(documentId: String) {
        noteDocumentDao.deleteDocumentById(documentId)
        listItemDao.deleteItemByEntityId(documentId)
    }

    fun getDocumentItems(documentId: String): Flow<List<NoteDocumentItemEntity>> =
        noteDocumentDao.getItemsForDocument(documentId)

    suspend fun saveDocumentItem(item: NoteDocumentItemEntity) {
        val existingItem = noteDocumentDao.getListItemById(item.id)
        if (existingItem == null) {
            noteDocumentDao.insertListItem(item)
        } else {
            noteDocumentDao.updateListItem(item.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteDocumentItem(itemId: String) {
        noteDocumentDao.deleteListItemById(itemId)
    }

    suspend fun updateDocumentItems(items: List<NoteDocumentItemEntity>) {
        noteDocumentDao.updateListItems(items)
    }

    suspend fun getDocumentById(id: String): NoteDocumentEntity? {
        Log.d(TAG, "getDocumentById called with id: $id")
        val document = noteDocumentDao.getDocumentById(id)
        Log.d(TAG, "getDocumentById returned: $document")
        return document
    }

    suspend fun updateDocument(document: NoteDocumentEntity) {
        android.util.Log.d(
            "CursorDebug",
            "Repository updating note document. lastCursorPosition: ${document.lastCursorPosition}",
        )
        Log.d(TAG, "updateDocument called with document: $document")
        noteDocumentDao.updateDocument(document)
        recentItemsRepository.updateRecentItemDisplayName(document.id, document.name)
        Log.d(TAG, "updateDocument finished")
    }
}
