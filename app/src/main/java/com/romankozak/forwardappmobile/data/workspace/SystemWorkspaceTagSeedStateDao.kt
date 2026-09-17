package com.romankozak.forwardappmobile.data.workspace

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.SystemWorkspaceTagSeedStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemWorkspaceTagSeedStateDao {
    @Query("SELECT * FROM system_workspace_tag_seed_states WHERE workspaceId = :workspaceId LIMIT 1")
    suspend fun getByWorkspaceId(workspaceId: String): SystemWorkspaceTagSeedStateEntity?

    @Query("SELECT * FROM system_workspace_tag_seed_states")
    suspend fun getAll(): List<SystemWorkspaceTagSeedStateEntity>

    @Query("SELECT * FROM system_workspace_tag_seed_states")
    fun observeAll(): Flow<List<SystemWorkspaceTagSeedStateEntity>>

    @Upsert
    suspend fun upsert(items: List<SystemWorkspaceTagSeedStateEntity>)
}
