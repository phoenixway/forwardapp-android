package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: ChecklistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklists(checklists: List<ChecklistEntity>)

    @Update
    suspend fun updateChecklist(checklist: ChecklistEntity)

    @Query("SELECT * FROM checklists WHERE id = :checklistId")
    suspend fun getChecklistById(checklistId: String): ChecklistEntity?

    @Query("SELECT * FROM checklists WHERE id = :checklistId")
    fun observeChecklistById(checklistId: String): Flow<ChecklistEntity?>

    @Query(
        """
        SELECT c.*
        FROM checklists AS c
        INNER JOIN attachments AS a
            ON a.entity_id = c.id AND a.attachment_type = :attachmentType
        INNER JOIN workspace_connections AS link
            ON link.attachmentId = a.id AND link.isDeleted = 0
        INNER JOIN workspaces AS w
            ON w.id = link.workspaceId
        WHERE w.sourceContextId = :contextId
          AND a.isDeleted = 0
        ORDER BY c.name COLLATE NOCASE ASC
        """,
    )
    fun getChecklistsForContext(
        contextId: String,
        attachmentType: String,
    ): Flow<List<ChecklistEntity>>

    @Query("SELECT * FROM checklists")
    fun getAllChecklistsAsFlow(): Flow<List<ChecklistEntity>>

    @Query("SELECT * FROM checklists WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): ChecklistEntity?

    @Query("SELECT * FROM checklists")
    suspend fun getAllChecklists(): List<ChecklistEntity>

    @Query("DELETE FROM checklists WHERE id = :checklistId")
    suspend fun deleteChecklistById(checklistId: String)

    @Query("DELETE FROM checklists")
    suspend fun deleteAllChecklists()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ChecklistItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ChecklistItemEntity>)

    @Update
    suspend fun updateItem(item: ChecklistItemEntity)

    @Update
    suspend fun updateItems(items: List<ChecklistItemEntity>)

    @Query("SELECT * FROM checklist_items WHERE id = :itemId LIMIT 1")
    suspend fun getItemById(itemId: String): ChecklistItemEntity?

    @Query("SELECT * FROM checklist_items WHERE id IN (:itemIds)")
    suspend fun getItemsByIds(itemIds: List<String>): List<ChecklistItemEntity>

    @Query("SELECT * FROM checklist_items WHERE checklistId = :checklistId ORDER BY itemOrder ASC, id ASC")
    fun getItemsForChecklist(checklistId: String): Flow<List<ChecklistItemEntity>>

    @Query("DELETE FROM checklist_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: String)

    @Query("DELETE FROM checklist_items WHERE id IN (:itemIds)")
    suspend fun deleteItemsByIds(itemIds: List<String>)

    @Query("DELETE FROM checklist_items WHERE checklistId = :checklistId")
    suspend fun deleteItemsByChecklistId(checklistId: String)

    @Query("DELETE FROM checklist_items")
    suspend fun deleteAllChecklistItems()

    @Query("SELECT * FROM checklist_items WHERE checklistId = :checklistId ORDER BY itemOrder ASC, id ASC")
    suspend fun getItemsForChecklistSync(checklistId: String): List<ChecklistItemEntity>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllChecklistItems(): List<ChecklistItemEntity>

    @Query("SELECT * FROM checklists")
    suspend fun getAllChecklistsRaw(): List<ChecklistEntity>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllChecklistItemsRaw(): List<ChecklistItemEntity>
}
