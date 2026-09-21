package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementLinkedAppearanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HierarchyPlacementLinkedAppearanceDao {
    @Query(
        """
        SELECT * FROM hierarchy_placement_linked_appearances
        WHERE hierarchyId = :hierarchyId
          AND isDeleted = 0
        ORDER BY placementId
        """,
    )
    suspend fun getLiveForHierarchy(
        hierarchyId: String,
    ): List<HierarchyPlacementLinkedAppearanceEntity>

    @Query(
        """
        SELECT * FROM hierarchy_placement_linked_appearances
        WHERE hierarchyId = :hierarchyId
          AND isDeleted = 0
        ORDER BY placementId
        """,
    )
    fun observeLiveForHierarchy(
        hierarchyId: String,
    ): Flow<List<HierarchyPlacementLinkedAppearanceEntity>>

    @Query(
        """
        SELECT * FROM hierarchy_placement_linked_appearances
        WHERE placementId = :placementId
        LIMIT 1
        """,
    )
    suspend fun getByPlacementId(
        placementId: String,
    ): HierarchyPlacementLinkedAppearanceEntity?

    @Query(
        """
        SELECT * FROM hierarchy_placement_linked_appearances
        ORDER BY hierarchyId, placementId
        """,
    )
    suspend fun getAll(): List<HierarchyPlacementLinkedAppearanceEntity>

    @Query(
        """
        SELECT * FROM hierarchy_placement_linked_appearances
        WHERE syncedAt IS NULL
        ORDER BY updatedAt, placementId
        """,
    )
    suspend fun getUnsynced(): List<HierarchyPlacementLinkedAppearanceEntity>

    @Query(
        """
        SELECT * FROM hierarchy_placement_linked_appearances
        WHERE updatedAt > :timestamp
        ORDER BY updatedAt, placementId
        """,
    )
    suspend fun getChangedSince(
        timestamp: Long,
    ): List<HierarchyPlacementLinkedAppearanceEntity>

    @Query(
        """
        UPDATE hierarchy_placement_linked_appearances
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
        item: HierarchyPlacementLinkedAppearanceEntity,
    )

    @Upsert
    suspend fun upsertAll(
        items: List<HierarchyPlacementLinkedAppearanceEntity>,
    )
}
