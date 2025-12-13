package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.database.models.StructurePresetItem
import kotlinx.coroutines.flow.Flow

@Dao
interface StructurePresetItemDao {
    @Query("SELECT * FROM structure_preset_items WHERE presetId = :presetId")
    fun getItemsByPreset(presetId: String): Flow<List<StructurePresetItem>>

    @Query("SELECT * FROM structure_preset_items WHERE presetId = :presetId")
    suspend fun getItemsByPresetOnce(presetId: String): List<StructurePresetItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: StructurePresetItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<StructurePresetItem>)

    @Query("DELETE FROM structure_preset_items WHERE presetId = :presetId")
    suspend fun deleteItemsByPreset(presetId: String)

    @Transaction
    suspend fun replaceItems(presetId: String, items: List<StructurePresetItem>) {
        deleteItemsByPreset(presetId)
        insertItems(items)
    }
}
