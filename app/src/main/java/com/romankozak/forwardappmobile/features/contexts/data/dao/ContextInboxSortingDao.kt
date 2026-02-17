package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.ContextInboxSortingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextInboxSortingDao {
    @Query("SELECT * FROM context_inbox_sorting WHERE context_id = :contextId LIMIT 1")
    fun observeForContext(contextId: String): Flow<ContextInboxSortingEntity?>

    @Query("SELECT * FROM context_inbox_sorting WHERE context_id = :contextId LIMIT 1")
    suspend fun getForContext(contextId: String): ContextInboxSortingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ContextInboxSortingEntity)
}

