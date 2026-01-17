package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.data.database.models.ProjectArtifact
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectArtifactDao {
    @Query("SELECT * FROM project_artifacts WHERE projectId = :projectId")
    fun getArtifactForProjectStream(projectId: String): Flow<ProjectArtifact?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(artifact: ProjectArtifact)

    @Update
    suspend fun update(artifact: ProjectArtifact)

    // --- Backup Methods ---
    @Query("SELECT * FROM project_artifacts")
    suspend fun getAll(): List<ProjectArtifact>

    @Query("DELETE FROM project_artifacts")
    suspend fun deleteAll()
}