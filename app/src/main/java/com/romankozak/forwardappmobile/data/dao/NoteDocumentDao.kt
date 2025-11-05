package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDocumentDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: NoteDocumentEntity)

    @Update
    suspend fun updateDocument(document: NoteDocumentEntity)

    @Query("SELECT * FROM note_documents WHERE id = :documentId")
    suspend fun getDocumentById(documentId: String): NoteDocumentEntity?

    @Query(
        """
        SELECT nd.*
        FROM note_documents AS nd
        INNER JOIN attachments AS a
            ON a.entity_id = nd.id AND a.attachment_type = :attachmentType
        INNER JOIN project_attachment_cross_ref AS link
            ON link.attachment_id = a.id
        WHERE link.project_id = :projectId
        ORDER BY nd.updatedAt DESC
        """,
    )
    fun getDocumentsForProject(
        projectId: String,
        attachmentType: String,
    ): Flow<List<NoteDocumentEntity>>

    @Query("DELETE FROM note_documents WHERE id = :documentId")
    suspend fun deleteDocumentById(documentId: String)

    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListItem(item: NoteDocumentItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListItems(items: List<NoteDocumentItemEntity>)

    @Update
    suspend fun updateListItem(item: NoteDocumentItemEntity)

    @Update
    suspend fun updateListItems(items: List<NoteDocumentItemEntity>)

    @Query("SELECT * FROM note_document_items WHERE id = :itemId")
    suspend fun getListItemById(itemId: String): NoteDocumentItemEntity?

    @Query("SELECT * FROM note_document_items WHERE listId = :documentId ORDER BY itemOrder ASC")
    fun getItemsForDocument(documentId: String): Flow<List<NoteDocumentItemEntity>>

    @Query("DELETE FROM note_document_items WHERE id = :itemId")
    suspend fun deleteListItemById(itemId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDocuments(items: List<NoteDocumentEntity>)

    @Query("DELETE FROM note_documents")
    suspend fun deleteAllDocuments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDocumentItems(items: List<NoteDocumentItemEntity>)

    @Query("DELETE FROM note_document_items")
    suspend fun deleteAllDocumentItems()

    @Query("SELECT * FROM note_documents")
    suspend fun getAllDocuments(): List<NoteDocumentEntity>

    @Query("SELECT * FROM note_documents")
    fun getAllDocumentsAsFlow(): Flow<List<NoteDocumentEntity>>

    @Query("SELECT * FROM note_document_items")
    suspend fun getAllDocumentItems(): List<NoteDocumentItemEntity>

    @Query("DELETE FROM note_document_items WHERE id IN (:itemIds)")
    suspend fun deleteListItemsByIds(itemIds: List<String>)
}
