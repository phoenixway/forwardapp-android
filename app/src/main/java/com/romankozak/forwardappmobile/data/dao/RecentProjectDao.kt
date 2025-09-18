package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.RecentProjectEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentProjectDao {
    @Upsert suspend fun logAccess(entry: RecentProjectEntry)

    @Query(
        """
        SELECT p.* FROM projects AS p
        INNER JOIN recent_project_entries AS rpe ON p.id = rpe.project_id
        ORDER BY rpe.last_accessed DESC
        LIMIT :limit
    """,
    )
    fun getRecentProjects(limit: Int): Flow<List<Project>>

    @Query("SELECT * FROM recent_project_entries")
    suspend fun getAllEntries(): List<RecentProjectEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<RecentProjectEntry>)

    @Query("DELETE FROM recent_project_entries")
    suspend fun deleteAll()
}