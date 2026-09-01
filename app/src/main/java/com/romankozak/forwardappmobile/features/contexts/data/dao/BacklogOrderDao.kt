package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogOrder

/** Legacy order evidence retained only for migration/fallback accounting. */
@Dao
interface BacklogOrderDao {
    @Query("SELECT * FROM backlog_orders")
    suspend fun getAllRaw(): List<BacklogOrder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<BacklogOrder>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<BacklogOrder>)
}
