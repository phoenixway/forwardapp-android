package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.data.database.models.LifeSystemStateEntity

@Dao
interface LifeSystemStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: LifeSystemStateEntity)

    @Query("SELECT * FROM life_system_state LIMIT 1")
    suspend fun getState(): LifeSystemStateEntity?
}
