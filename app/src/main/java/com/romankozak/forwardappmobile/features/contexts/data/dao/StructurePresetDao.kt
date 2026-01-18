package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextRoleProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface StructurePresetDao {
    @Query("SELECT * FROM structure_presets")
    fun getAll(): Flow<List<ContextRoleProfile>>

    @Query("SELECT * FROM structure_presets WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): ContextRoleProfile?

    @Query("SELECT * FROM structure_presets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ContextRoleProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: ContextRoleProfile)

    // --- Backup Methods ---
    @Query("SELECT * FROM structure_presets")
    suspend fun getAllSync(): List<ContextRoleProfile>

    @Query("DELETE FROM structure_presets")
    suspend fun deleteAll()
}
