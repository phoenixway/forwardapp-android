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

    @Query("SELECT * FROM checklists WHERE projectId = :projectId ORDER BY name ASC")
    fun getChecklistsForProject(projectId: String): Flow<List<ChecklistEntity>>

    @Query("SELECT * FROM checklists")
    fun getAllChecklistsAsFlow(): Flow<List<ChecklistEntity>>

    @Query("DELETE FROM checklists WHERE id = :checklistId")
    suspend fun deleteChecklist(checklistId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: ChecklistItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(items: List<ChecklistItemEntity>)

    @Update
    suspend fun updateChecklistItem(item: ChecklistItemEntity)

    @Update
    suspend fun updateChecklistItems(items: List<ChecklistItemEntity>)

    @Query("SELECT * FROM checklist_items WHERE checklistId = :checklistId ORDER BY itemOrder ASC")
    fun getChecklistItems(checklistId: String): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE id = :itemId")
    suspend fun getChecklistItemById(itemId: String): ChecklistItemEntity?

    @Query("DELETE FROM checklist_items WHERE id = :itemId")
    suspend fun deleteChecklistItem(itemId: String)

    @Query("DELETE FROM checklists")
    suspend fun deleteAllChecklists()

    @Query("DELETE FROM checklist_items")
    suspend fun deleteAllChecklistItems()

    @Query("SELECT * FROM checklists")
    suspend fun getAllChecklists(): List<ChecklistEntity>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllChecklistItems(): List<ChecklistItemEntity>
}
