package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.data.database.models.ChecklistEntity
import com.romankozak.forwardappmobile.data.database.models.ChecklistItemEntity
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
        INNER JOIN project_attachment_cross_ref AS link
            ON link.attachment_id = a.id
        WHERE link.project_id = :projectId
        ORDER BY c.name COLLATE NOCASE ASC
        """,
    )
    fun getChecklistsForProject(
        projectId: String,
        attachmentType: String,
    ): Flow<List<ChecklistEntity>>

    @Query("SELECT * FROM checklists")
    fun getAllChecklistsAsFlow(): Flow<List<ChecklistEntity>>

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

    @Query("SELECT * FROM checklist_items WHERE checklistId = :checklistId ORDER BY itemOrder ASC, id ASC")
    fun getItemsForChecklist(checklistId: String): Flow<List<ChecklistItemEntity>>

    @Query("DELETE FROM checklist_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: String)

    @Query("DELETE FROM checklist_items WHERE checklistId = :checklistId")
    suspend fun deleteItemsByChecklistId(checklistId: String)

    @Query("DELETE FROM checklist_items")
    suspend fun deleteAllChecklistItems()

    @Query("SELECT * FROM checklist_items WHERE checklistId = :checklistId ORDER BY itemOrder ASC, id ASC")
    suspend fun getItemsForChecklistSync(checklistId: String): List<ChecklistItemEntity>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllChecklistItems(): List<ChecklistItemEntity>
}
