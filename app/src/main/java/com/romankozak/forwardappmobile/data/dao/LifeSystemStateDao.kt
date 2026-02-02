package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.LifeSystemStateEntity

@Dao
interface LifeSystemStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: LifeSystemStateEntity)

    @Query("SELECT * FROM life_system_state LIMIT 1")
    suspend fun getState(): LifeSystemStateEntity?

    // --- Backup Methods ---
    @Query("SELECT * FROM life_system_state")
    suspend fun getAllSync(): List<LifeSystemStateEntity>

    @Query("DELETE FROM life_system_state")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(states: List<LifeSystemStateEntity>)
}
