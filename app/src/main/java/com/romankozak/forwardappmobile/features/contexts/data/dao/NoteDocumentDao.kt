package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: NoteDocumentEntity)

    @Update
    suspend fun updateDocument(document: NoteDocumentEntity)

    @Query("SELECT * FROM note_documents WHERE id = :documentId")
    suspend fun getDocumentById(documentId: String): NoteDocumentEntity?

    @Query("SELECT * FROM note_documents WHERE id = :documentId")
    fun getDocumentByIdFlow(documentId: String): Flow<NoteDocumentEntity?>

    @Query(
        """
        SELECT nd.*
        FROM note_documents AS nd
        INNER JOIN attachments AS a
            ON a.entity_id = nd.id AND a.attachment_type IN ('NOTE_DOCUMENT', 'JOURNAL_DOCUMENT')
        INNER JOIN context_attachment_cross_ref AS link
            ON link.attachment_id = a.id
        WHERE link.context_id = :contextId
        ORDER BY nd.updatedAt DESC
        """,
    )
    fun getDocumentsForContext(contextId: String): Flow<List<NoteDocumentEntity>>

    @Query("DELETE FROM note_documents WHERE id = :documentId")
    suspend fun deleteDocumentById(documentId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDocuments(items: List<NoteDocumentEntity>)

    @Query("DELETE FROM note_documents")
    suspend fun deleteAllDocuments()

    @Query("SELECT * FROM note_documents")
    suspend fun getAllDocuments(): List<NoteDocumentEntity>

    @Query("SELECT * FROM note_documents")
    fun getAllDocumentsAsFlow(): Flow<List<NoteDocumentEntity>>

    @Query("SELECT * FROM note_documents WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): NoteDocumentEntity?

    @Query("SELECT * FROM note_documents")
    suspend fun getAllDocumentsRaw(): List<NoteDocumentEntity>
}
