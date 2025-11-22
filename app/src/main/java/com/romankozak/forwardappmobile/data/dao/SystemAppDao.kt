package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.data.database.models.SystemAppEntity

@Dao
interface SystemAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: SystemAppEntity)

    @Query("SELECT * FROM system_apps WHERE system_key = :systemKey LIMIT 1")
    suspend fun getBySystemKey(systemKey: String): SystemAppEntity?

    @Query("SELECT * FROM system_apps")
    suspend fun getAll(): List<SystemAppEntity>
}
