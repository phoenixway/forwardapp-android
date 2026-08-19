package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DayFocusItemDao {
    @Query("SELECT * FROM day_focus_items ORDER BY dayPlanId ASC, `order` ASC, createdAt ASC")
    suspend fun getAllSync(): List<DayFocusItem>

    @Query(
        """
        SELECT * FROM day_focus_items
        WHERE dayPlanId = :dayPlanId
        ORDER BY `order` ASC, createdAt ASC
        """,
    )
    suspend fun getItemsForDayPlanSync(dayPlanId: String): List<DayFocusItem>

    @Query(
        """
        SELECT * FROM day_focus_items
        WHERE dayPlanId = :dayPlanId AND isDeleted = 0
        ORDER BY `order` ASC, createdAt ASC
        """,
    )
    fun getItemsForDayPlan(dayPlanId: String): Flow<List<DayFocusItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DayFocusItem)

    @Update
    suspend fun update(item: DayFocusItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DayFocusItem>)

    @Query("SELECT * FROM day_focus_items WHERE id = :itemId LIMIT 1")
    suspend fun getByIdForCanonicalRecurrenceSync(itemId: String): DayFocusItem?

    @Query(
        """
        UPDATE day_focus_items
        SET isDeleted = 1,
            updatedAt = :updatedAt,
            syncedAt = NULL,
            version = version + 1
        WHERE id = :itemId
        """,
    )
    suspend fun softDelete(itemId: String, updatedAt: Long)

    @Query(
        """
        UPDATE day_focus_items
        SET isDeleted = 1,
            updatedAt = :updatedAt,
            syncedAt = NULL,
            version = version + 1
        WHERE recurringKey = :recurringKey
        """,
    )
    suspend fun softDeleteByRecurringKey(recurringKey: String, updatedAt: Long)

    @Query(
        """
        UPDATE day_focus_items
        SET `order` = :order,
            updatedAt = :updatedAt,
            syncedAt = NULL,
            version = version + 1
        WHERE id = :itemId
        """,
    )
    suspend fun updateOrder(itemId: String, order: Long, updatedAt: Long)
}
