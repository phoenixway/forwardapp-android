package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextArtifact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextArtifactDao {
    @Query("SELECT * FROM context_artifacts WHERE contextId = :contextId")
    fun getArtifactForContextStream(contextId: String): Flow<ContextArtifact?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(artifact: ContextArtifact)

    @Update
    suspend fun update(artifact: ContextArtifact)

    // --- Backup Methods ---
    @Query("SELECT * FROM context_artifacts")
    suspend fun getAll(): List<ContextArtifact>

    @Query("DELETE FROM context_artifacts")
    suspend fun deleteAll()
}
