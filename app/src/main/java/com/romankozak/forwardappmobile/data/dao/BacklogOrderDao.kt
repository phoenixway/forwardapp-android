package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.data.database.models.BacklogOrder

@Dao
interface BacklogOrderDao {

    @Query("SELECT * FROM backlog_orders WHERE list_id = :listId AND is_deleted = 0 ORDER BY item_order")
    suspend fun getOrdersForList(listId: String): List<BacklogOrder>

    @Query("SELECT * FROM backlog_orders")
    suspend fun getAll(): List<BacklogOrder>

    @Query("SELECT * FROM backlog_orders WHERE is_deleted = 0")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<BacklogOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<BacklogOrder>)

    @Update
    suspend fun updateOrders(orders: List<BacklogOrder>)

    @Query("DELETE FROM backlog_orders WHERE id IN (:ids)")
    suspend fun deleteOrders(ids: List<String>)
}
