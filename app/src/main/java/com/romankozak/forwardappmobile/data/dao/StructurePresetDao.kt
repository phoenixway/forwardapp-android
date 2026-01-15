package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.data.database.models.StructurePreset
import kotlinx.coroutines.flow.Flow

@Dao
interface StructurePresetDao {
    @Query("SELECT * FROM structure_presets")
    fun getAll(): Flow<List<StructurePreset>>

    @Query("SELECT * FROM structure_presets WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): StructurePreset?

    @Query("SELECT * FROM structure_presets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StructurePreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: StructurePreset)

    // --- Backup Methods ---
    @Query("SELECT * FROM structure_presets")
    suspend fun getAllSync(): List<StructurePreset>

    @Query("DELETE FROM structure_presets")
    suspend fun deleteAll()
}
