package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.SystemAppEntity

@Dao
interface SystemAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: SystemAppEntity)

    @Query("SELECT * FROM system_apps WHERE system_key = :systemKey LIMIT 1")
    suspend fun getBySystemKey(systemKey: String): SystemAppEntity?

    @Query("SELECT * FROM system_apps")
    suspend fun getAll(): List<SystemAppEntity>

    // --- Backup Methods ---
    @Query("DELETE FROM system_apps")
    suspend fun deleteAll()

    @Query("SELECT * FROM system_apps")
    suspend fun getAllRaw(): List<SystemAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<SystemAppEntity>)
}
