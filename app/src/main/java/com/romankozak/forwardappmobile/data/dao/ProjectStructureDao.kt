package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.romankozak.forwardappmobile.data.database.models.ProjectStructure
import com.romankozak.forwardappmobile.data.database.models.ProjectStructureItem
import kotlinx.coroutines.flow.Flow

data class ProjectStructureWithItems(
    val structure: ProjectStructure,
    val items: List<ProjectStructureItem>
)

@Dao
interface ProjectStructureDao {

    @Query("SELECT * FROM project_structures WHERE projectId = :projectId LIMIT 1")
    suspend fun getStructureByProject(projectId: String): ProjectStructure?

    @Query("SELECT * FROM project_structures WHERE projectId = :projectId LIMIT 1")
    fun observeStructureByProject(projectId: String): Flow<ProjectStructure?>

    @Query(
        """
        SELECT psi.*
          FROM project_structure_items psi
          INNER JOIN project_structures ps ON ps.id = psi.projectStructureId
         WHERE ps.projectId = :projectId
        """
    )
    fun observeItemsForProject(projectId: String): Flow<List<ProjectStructureItem>>

    @Query("SELECT * FROM project_structure_items WHERE projectStructureId = :structureId")
    fun observeItems(structureId: String): Flow<List<ProjectStructureItem>>

    @Query("SELECT * FROM project_structure_items WHERE projectStructureId = :structureId")
    suspend fun getItems(structureId: String): List<ProjectStructureItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStructure(structure: ProjectStructure)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ProjectStructureItem>)

    @Update
    suspend fun updateStructure(structure: ProjectStructure)

    @Update
    suspend fun updateItem(item: ProjectStructureItem)

    @Query("DELETE FROM project_structure_items WHERE projectStructureId = :structureId")
    suspend fun deleteItemsForStructure(structureId: String)

    @Transaction
    suspend fun replaceItems(structureId: String, newItems: List<ProjectStructureItem>) {
        deleteItemsForStructure(structureId)
        insertItems(newItems)
    }

    // --- Backup Methods ---
    @Query("SELECT * FROM project_structures")
    suspend fun getAllStructures(): List<ProjectStructure>

    @Query("SELECT * FROM project_structure_items")
    suspend fun getAllItems(): List<ProjectStructureItem>

    @Query("DELETE FROM project_structures")
    suspend fun deleteAllStructures()

    @Query("DELETE FROM project_structure_items")
    suspend fun deleteAllItems()
}
