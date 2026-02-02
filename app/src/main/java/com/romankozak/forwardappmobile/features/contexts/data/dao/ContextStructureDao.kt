package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStructureItem
import kotlinx.coroutines.flow.Flow

data class ContextStructureWithItems(
    val structure: ContextConfiguration,
    val items: List<ContextStructureItem>,
)

@Dao
interface ContextStructureDao {
    @Query("SELECT * FROM context_structures WHERE contextId = :contextId LIMIT 1")
    suspend fun getStructureByContext(contextId: String): ContextConfiguration?

    @Query("SELECT * FROM context_structures WHERE contextId = :contextId LIMIT 1")
    fun observeStructureByContext(contextId: String): Flow<ContextConfiguration?>

    @Query(
        """
        SELECT psi.*
          FROM context_structure_items psi
          INNER JOIN context_structures ps ON ps.id = psi.contextStructureId
         WHERE ps.contextId = :contextId
        """,
    )
    fun observeItemsForContext(contextId: String): Flow<List<ContextStructureItem>>

    @Query("SELECT * FROM context_structure_items WHERE contextStructureId = :structureId")
    fun observeItems(structureId: String): Flow<List<ContextStructureItem>>

    @Query("SELECT * FROM context_structure_items WHERE contextStructureId = :structureId")
    suspend fun getItems(structureId: String): List<ContextStructureItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStructure(structure: ContextConfiguration)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ContextStructureItem>)

    @Update
    suspend fun updateStructure(structure: ContextConfiguration)

    @Update
    suspend fun updateItem(item: ContextStructureItem)

    @Query("DELETE FROM context_structure_items WHERE contextStructureId = :structureId")
    suspend fun deleteItemsForStructure(structureId: String)

    @Transaction
    suspend fun replaceItems(
        structureId: String,
        newItems: List<ContextStructureItem>,
    ) {
        deleteItemsForStructure(structureId)
        insertItems(newItems)
    }

    // --- Backup Methods ---
    @Query("SELECT * FROM context_structures")
    suspend fun getAllSync(): List<ContextConfiguration>

    @Query("SELECT * FROM context_structure_items")
    suspend fun getAllItemsSync(): List<ContextStructureItem>

    @Query("DELETE FROM context_structures")
    suspend fun deleteAllStructures()

    @Query("DELETE FROM context_structure_items")
    suspend fun deleteAllItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(structures: List<ContextConfiguration>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<ContextStructureItem>)
}
