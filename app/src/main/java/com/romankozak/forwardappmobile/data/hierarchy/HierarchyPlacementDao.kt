package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity
import kotlinx.coroutines.flow.Flow

data class HierarchyPlacedTargetRow(
    val targetType: String,
    val targetId: String,
)

private const val LIVE_STRUCTURAL_READ_FRAME_QUERY = """
        SELECT
            p.*,
            gs.placementId AS groupScope_placementId,
            gs.hierarchyId AS groupScope_hierarchyId,
            gs.groupSubjectId AS groupScope_groupSubjectId,
            gs.createdAt AS groupScope_createdAt,
            gs.updatedAt AS groupScope_updatedAt,
            gs.syncedAt AS groupScope_syncedAt,
            gs.isDeleted AS groupScope_isDeleted,
            gs.version AS groupScope_version,
            la.placementId AS linkedAppearance_placementId,
            la.hierarchyId AS linkedAppearance_hierarchyId,
            la.createdAt AS linkedAppearance_createdAt,
            la.updatedAt AS linkedAppearance_updatedAt,
            la.syncedAt AS linkedAppearance_syncedAt,
            la.isDeleted AS linkedAppearance_isDeleted,
            la.version AS linkedAppearance_version,
            ga.placementId AS applicability_placementId,
            ga.hierarchyId AS applicability_hierarchyId,
            ga.mode AS applicability_mode,
            ga.createdAt AS applicability_createdAt,
            ga.updatedAt AS applicability_updatedAt,
            ga.syncedAt AS applicability_syncedAt,
            ga.version AS applicability_version,
            CASE WHEN hc.id IS NOT NULL THEN gs.groupSubjectId ELSE NULL END AS materializedHistoricalGroupSubjectId
        FROM hierarchy_placements AS p
        LEFT JOIN hierarchy_placement_group_scopes AS gs
          ON gs.placementId = p.id
         AND gs.hierarchyId = p.hierarchyId
        LEFT JOIN hierarchy_group_scope_applicability AS ga
          ON ga.placementId = p.id
         AND ga.hierarchyId = p.hierarchyId
        LEFT JOIN big_picture_containers AS hc
          ON hc.id = 'historical-group:v1:' || gs.groupSubjectId
         AND hc.hierarchyId = p.hierarchyId
        LEFT JOIN hierarchy_placement_linked_appearances AS la
          ON la.placementId = p.id
         AND la.hierarchyId = p.hierarchyId
         AND la.isDeleted = 0
        WHERE p.hierarchyId = :hierarchyId
          AND p.isDeleted = 0
        ORDER BY p.parentPlacementId ASC, p.siblingOrder ASC, p.id ASC
        """

@Dao
interface HierarchyPlacementDao {
    @Query(LIVE_STRUCTURAL_READ_FRAME_QUERY)
    fun observeLiveStructuralReadFrame(
        hierarchyId: String,
    ): Flow<List<CanonicalV2HierarchyStructuralReadRow>>

    @Query(LIVE_STRUCTURAL_READ_FRAME_QUERY)
    suspend fun getLiveStructuralReadFrame(hierarchyId: String): List<CanonicalV2HierarchyStructuralReadRow>

    @Query(
        """
        SELECT * FROM hierarchy_placements
        WHERE hierarchyId = :hierarchyId
          AND isDeleted = 0
        ORDER BY parentPlacementId ASC, siblingOrder ASC, id ASC
        """,
    )
    fun observeLiveHierarchy(hierarchyId: String): Flow<List<HierarchyPlacementEntity>>

    /**
     * Narrow typed-target presence read.
     * Both PRIMARY and LINK count as occurrences.
     * Live ancestor closure is guaranteed by canonical validation.
     */
    @Query(
        """
        SELECT DISTINCT targetType, targetId
        FROM hierarchy_placements
        WHERE hierarchyId = :hierarchyId
          AND isDeleted = 0
        """,
    )
    fun observeVisiblePlacedTargets(
        hierarchyId: String,
    ): Flow<List<HierarchyPlacedTargetRow>>

    @Query(
        """
        SELECT * FROM hierarchy_placements
        WHERE hierarchyId = :hierarchyId
          AND isDeleted = 0
        ORDER BY parentPlacementId ASC, siblingOrder ASC, id ASC
        """,
    )
    suspend fun getLiveHierarchy(hierarchyId: String): List<HierarchyPlacementEntity>

    @Query("SELECT * FROM hierarchy_placements WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HierarchyPlacementEntity?

    @Query(
        """
        SELECT * FROM hierarchy_placements
        WHERE hierarchyId = :hierarchyId
          AND parentPlacementId IS :parentPlacementId
          AND isDeleted = 0
        ORDER BY siblingOrder ASC, id ASC
        """,
    )
    suspend fun getLiveChildren(
        hierarchyId: String,
        parentPlacementId: String?,
    ): List<HierarchyPlacementEntity>

    @Query(
        """
        SELECT * FROM hierarchy_placements
        WHERE hierarchyId = :hierarchyId
          AND targetType = :targetType
          AND targetId = :targetId
          AND isDeleted = 0
        ORDER BY siblingOrder ASC, id ASC
        """,
    )
    suspend fun getLiveAppearances(
        hierarchyId: String,
        targetType: String,
        targetId: String,
    ): List<HierarchyPlacementEntity>

    @Query(
        """
        SELECT * FROM hierarchy_placements
        WHERE targetType = :targetType
          AND targetId IN (:targetIds)
          AND isDeleted = 0
        ORDER BY hierarchyId ASC, parentPlacementId ASC, siblingOrder ASC, id ASC
        """,
    )
    suspend fun getLiveByTargets(
        targetType: String,
        targetIds: List<String>,
    ): List<HierarchyPlacementEntity>

    @Query(
        """
        SELECT * FROM hierarchy_placements
        WHERE hierarchyId = :hierarchyId
        ORDER BY parentPlacementId ASC, siblingOrder ASC, id ASC
        """,
    )
    suspend fun getAllForHierarchy(hierarchyId: String): List<HierarchyPlacementEntity>

    @Query(
        """
        SELECT * FROM hierarchy_placements
        ORDER BY hierarchyId ASC, parentPlacementId ASC, siblingOrder ASC, id ASC
        """,
    )
    suspend fun getAll(): List<HierarchyPlacementEntity>

    @Query("SELECT * FROM hierarchy_placements WHERE updatedAt > :timestamp ORDER BY updatedAt ASC, id ASC")
    suspend fun getChangedSince(timestamp: Long): List<HierarchyPlacementEntity>

    @Query("SELECT * FROM hierarchy_placements WHERE syncedAt IS NULL ORDER BY updatedAt ASC, id ASC")
    suspend fun getUnsynced(): List<HierarchyPlacementEntity>

    @Query(
        """
        UPDATE hierarchy_placements
        SET syncedAt = :syncedAt
        WHERE id = :id
          AND version = :version
          AND syncedAt IS NULL
        """,
    )
    suspend fun markSyncedIfVersionMatches(
        id: String,
        version: Long,
        syncedAt: Long,
    ): Int

    @Upsert
    suspend fun upsert(item: HierarchyPlacementEntity)

    @Upsert
    suspend fun upsertAll(items: List<HierarchyPlacementEntity>)
}
