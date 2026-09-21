package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HierarchyPlacementGroupScopeDao {
    @Query(
        """
        SELECT * FROM hierarchy_placement_group_scopes
        WHERE hierarchyId = :hierarchyId
          AND isDeleted = 0
        ORDER BY placementId
        """,
    )
    suspend fun getLiveForHierarchy(
        hierarchyId: String,
    ): List<HierarchyPlacementGroupScopeEntity>

    @Query(
        """
        SELECT * FROM hierarchy_placement_group_scopes
        WHERE hierarchyId = :hierarchyId
          AND isDeleted = 0
        ORDER BY placementId
        """,
    )
    fun observeLiveForHierarchy(
        hierarchyId: String,
    ): Flow<List<HierarchyPlacementGroupScopeEntity>>

    @Query(
        """
        SELECT * FROM hierarchy_placement_group_scopes
        WHERE placementId = :placementId
        LIMIT 1
        """,
    )
    suspend fun getByPlacementId(
        placementId: String,
    ): HierarchyPlacementGroupScopeEntity?

    @Query(
        """
        SELECT * FROM hierarchy_placement_group_scopes
        ORDER BY hierarchyId, placementId
        """,
    )
    suspend fun getAll(): List<HierarchyPlacementGroupScopeEntity>

    @Query(
        """
        SELECT * FROM hierarchy_placement_group_scopes
        WHERE syncedAt IS NULL
        ORDER BY updatedAt, placementId
        """,
    )
    suspend fun getUnsynced(): List<HierarchyPlacementGroupScopeEntity>

    @Query(
        """
        SELECT * FROM hierarchy_placement_group_scopes
        WHERE updatedAt > :timestamp
        ORDER BY updatedAt, placementId
        """,
    )
    suspend fun getChangedSince(
        timestamp: Long,
    ): List<HierarchyPlacementGroupScopeEntity>

    @Query(
        """
        UPDATE hierarchy_placement_group_scopes
        SET syncedAt = :syncedAt
        WHERE placementId = :placementId
          AND version = :version
          AND syncedAt IS NULL
        """,
    )
    suspend fun markSyncedIfVersionMatches(
        placementId: String,
        version: Long,
        syncedAt: Long,
    ): Int

    @Upsert
    suspend fun upsert(
        item: HierarchyPlacementGroupScopeEntity,
    )

    @Upsert
    suspend fun upsertAll(
        items: List<HierarchyPlacementGroupScopeEntity>,
    )
}
