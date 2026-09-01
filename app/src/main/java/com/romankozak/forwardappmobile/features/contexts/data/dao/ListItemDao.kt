package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem

/**
 * Retained legacy BACKLOG evidence boundary.
 *
 * Runtime reads and mutations belong to canonical workspace_backlog_entries.
 * These operations exist only for the guarded old-full-backup planner fallback,
 * migration verification fixtures, and complete-database clearing.
 */
@Dao
interface ListItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: BacklogItem)

    @Query("SELECT * FROM list_items")
    suspend fun getAllRaw(): List<BacklogItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BacklogItem>)

    @Query("DELETE FROM list_items")
    suspend fun deleteAll()
}
