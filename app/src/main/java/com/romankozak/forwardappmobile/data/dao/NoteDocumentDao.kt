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

    @Query("SELECT * FROM custom_lists WHERE id = :documentId")
    suspend fun getDocumentById(documentId: String): NoteDocumentEntity?

    @Query("SELECT * FROM custom_lists WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun getDocumentsForProject(projectId: String): Flow<List<NoteDocumentEntity>>

    @Query("DELETE FROM custom_lists WHERE id = :documentId")
    suspend fun deleteDocumentById(documentId: String)

    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListItem(item: NoteDocumentItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListItems(items: List<NoteDocumentItemEntity>)

    @Update
    suspend fun updateListItem(item: NoteDocumentItemEntity)

    @Update
    suspend fun updateListItems(items: List<NoteDocumentItemEntity>)

    @Query("SELECT * FROM custom_list_items WHERE id = :itemId")
    suspend fun getListItemById(itemId: String): NoteDocumentItemEntity?

    @Query("SELECT * FROM custom_list_items WHERE listId = :documentId ORDER BY itemOrder ASC")
    fun getItemsForDocument(documentId: String): Flow<List<NoteDocumentItemEntity>>

    @Query("DELETE FROM custom_list_items WHERE id = :itemId")
    suspend fun deleteListItemById(itemId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDocuments(items: List<NoteDocumentEntity>)

    @Query("DELETE FROM custom_lists")
    suspend fun deleteAllDocuments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDocumentItems(items: List<NoteDocumentItemEntity>)

    @Query("DELETE FROM custom_list_items")
    suspend fun deleteAllDocumentItems()

    @Query("SELECT * FROM custom_lists")
    suspend fun getAllDocuments(): List<NoteDocumentEntity>

    @Query("SELECT * FROM custom_lists")
    fun getAllDocumentsAsFlow(): Flow<List<NoteDocumentEntity>>

    @Query("SELECT * FROM custom_list_items")
    suspend fun getAllDocumentItems(): List<NoteDocumentItemEntity>

    @Query("DELETE FROM custom_list_items WHERE id IN (:itemIds)")
    suspend fun deleteListItemsByIds(itemIds: List<String>)
}
