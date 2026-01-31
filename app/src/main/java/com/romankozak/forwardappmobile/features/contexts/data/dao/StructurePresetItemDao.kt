package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.data.models.ContextRoleProfileItem
import kotlinx.coroutines.flow.Flow

@Dao
interface StructurePresetItemDao {
    @Query("SELECT * FROM structure_preset_items WHERE presetId = :presetId")
    fun getItemsByPreset(presetId: String): Flow<List<ContextRoleProfileItem>>

    @Query("SELECT * FROM structure_preset_items WHERE presetId = :presetId")
    suspend fun getItemsByPresetOnce(presetId: String): List<ContextRoleProfileItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ContextRoleProfileItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ContextRoleProfileItem>)

    @Query("DELETE FROM structure_preset_items WHERE presetId = :presetId")
    suspend fun deleteItemsByPreset(presetId: String)

    @Transaction
    suspend fun replaceItems(
        presetId: String,
        items: List<ContextRoleProfileItem>,
    ) {
        deleteItemsByPreset(presetId)
        insertItems(items)
    }

    // --- Backup Methods ---
    @Query("SELECT * FROM structure_preset_items")
    suspend fun getAllSync(): List<ContextRoleProfileItem>

    @Query("DELETE FROM structure_preset_items")
    suspend fun deleteAllItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ContextRoleProfileItem>)
}
