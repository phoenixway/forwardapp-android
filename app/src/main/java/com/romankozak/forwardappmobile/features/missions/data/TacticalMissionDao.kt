package com.romankozak.forwardappmobile.features.missions.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMissionAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.logicalProjectId
import com.romankozak.forwardappmobile.data.dao.requireDayTaskWorkspaceOwner
import kotlinx.coroutines.flow.Flow

@Dao
interface TacticalMissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissionRaw(mission: TacticalMission): Long

    @Update
    suspend fun updateMissionRaw(mission: TacticalMission)

    @Query("SELECT * FROM workspaces WHERE id = :workspaceId LIMIT 1")
    suspend fun getOperationalProjectWorkspace(workspaceId: String): WorkspaceEntity?

    @Query("SELECT * FROM contexts WHERE id = :contextId LIMIT 1")
    suspend fun getOperationalProjectContext(contextId: String): Context?

    @Transaction
    suspend fun insertMission(mission: TacticalMission): Long =
        insertMissionRaw(routeProjectForPersistence(mission))

    @Transaction
    suspend fun updateMission(mission: TacticalMission) {
        updateMissionRaw(routeProjectForPersistence(mission))
    }

    @Query("DELETE FROM tactical_missions WHERE id = :missionId")
    suspend fun deleteMissionById(missionId: Long)

    @Query(
        """
        SELECT * FROM tactical_missions
        WHERE project_workspace_id = :projectId
            AND is_deleted = 0
        ORDER BY mission_order ASC, deadline DESC
        """,
    )
    fun getMissionsForProject(projectId: String): Flow<List<TacticalMission>>

    @Query("SELECT * FROM tactical_missions WHERE is_deleted = 0 ORDER BY mission_order ASC, deadline DESC")
    fun getAllMissions(): Flow<List<TacticalMission>>

    @Query("SELECT * FROM tactical_missions ORDER BY id ASC")
    fun observeAllMissionRows(): Flow<List<TacticalMission>>

    @Query("SELECT * FROM tactical_missions WHERE id = :missionId")
    suspend fun getMissionById(missionId: Long): TacticalMission?

    @Query(
        """
        SELECT source_backlog_item_id FROM tactical_missions
        WHERE week_key = :weekKey
            AND source_backlog_item_id IS NOT NULL
            AND is_deleted = 0
        """,
    )
    fun observeBacklogMissionIdsForWeek(weekKey: String): Flow<List<String>>

    @Query(
        """
        SELECT * FROM tactical_missions
        WHERE week_key = :weekKey
            AND source_backlog_item_id IS NOT NULL
            AND is_deleted = 0
        """,
    )
    fun observeBacklogMissionsForWeek(weekKey: String): Flow<List<TacticalMission>>

    @Query(
        """
        SELECT * FROM tactical_missions
        WHERE week_key = :weekKey
            AND source_backlog_item_id = :backlogItemId
            AND is_deleted = 0
        LIMIT 1
        """,
    )
    suspend fun getMissionForBacklogItemInWeek(
        backlogItemId: String,
        weekKey: String,
    ): TacticalMission?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissionAttachmentCrossRef(crossRef: TacticalMissionAttachmentCrossRef)

    @Query("DELETE FROM tactical_mission_attachment_cross_ref WHERE missionId = :missionId AND attachmentId = :attachmentId")
    suspend fun deleteMissionAttachmentCrossRef(
        missionId: Long,
        attachmentId: String,
    )

    @Query(
        """
        SELECT attachmentId FROM tactical_mission_attachment_cross_ref
        WHERE missionId = :missionId
        """,
    )
    suspend fun getAttachmentIdsForMission(missionId: Long): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM attachments WHERE id = :attachmentId)")
    suspend fun attachmentExists(attachmentId: String): Boolean

    @Query(
        "UPDATE tactical_missions SET synced_at = :syncedAt " +
            "WHERE id = :missionId AND version = :expectedVersion",
    )
    suspend fun markMissionSyncedIfVersionMatches(
        missionId: Long,
        expectedVersion: Long,
        syncedAt: Long,
    ): Int

    // --- Backup Methods ---
    @Query("SELECT * FROM tactical_missions")
    suspend fun getAllMissionsSync(): List<TacticalMission>

    @Query("""
        SELECT MIN(COALESCE(c.commitmentOrder, m.order_in_week)) FROM tactical_missions m
        LEFT JOIN planning_commitments c ON c.role='MISSION' AND c.provenanceSourceType='TACTICAL_MISSION'
            AND c.provenanceSourceId=CAST(m.id AS TEXT) AND c.isDeleted=0
        WHERE c.scopeId=:scopeId OR (c.id IS NULL AND m.is_deleted=0 AND m.iteration_id=:scopeId)
    """)
    suspend fun minimumCanonicalOrder(scopeId: String): Long?

    @Query("SELECT * FROM tactical_mission_attachment_cross_ref")
    suspend fun getAllMissionAttachmentCrossRefs(): List<TacticalMissionAttachmentCrossRef>

    @Query("DELETE FROM tactical_missions")
    suspend fun deleteAllMissions()

    @Query("DELETE FROM tactical_mission_attachment_cross_ref")
    suspend fun deleteAllMissionAttachmentCrossRefs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissionsRaw(missions: List<TacticalMission>)

    @Transaction
    suspend fun insertMissions(missions: List<TacticalMission>) {
        if (missions.isEmpty()) return
        insertMissionsRaw(missions.map { routeProjectForPersistence(it) })
    }

    @Query("SELECT * FROM tactical_mission_attachment_cross_ref")
    suspend fun getAllMissionAttachmentsSync(): List<TacticalMissionAttachmentCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissionAttachments(attachments: List<TacticalMissionAttachmentCrossRef>)

    @Query("SELECT COALESCE(MAX(mission_order), -1) FROM tactical_missions")
    suspend fun getMaxMissionOrder(): Long

    @Query("SELECT COALESCE(MIN(mission_order), 0) FROM tactical_missions")
    suspend fun getMinMissionOrder(): Long

    @Query("UPDATE tactical_missions SET mission_order = :order, order_in_week = :order WHERE id = :missionId")
    suspend fun updateMissionOrder(
        missionId: Long,
        order: Long,
    )
}

private suspend fun TacticalMissionDao.routeProjectForPersistence(
    mission: TacticalMission,
): TacticalMission {
    val contextProjectId = mission.projectId
    val workspaceProjectId = mission.projectWorkspaceId

    require(
        contextProjectId == null ||
            workspaceProjectId == null ||
            contextProjectId == workspaceProjectId,
    ) {
        "TacticalMission ${mission.id} has conflicting project ids: " +
            "context=$contextProjectId workspace=$workspaceProjectId"
    }

    val logicalProjectId =
        mission.logicalProjectId
            ?: return mission.copy(
                projectId = null,
                projectWorkspaceId = null,
            )

    requireDayTaskWorkspaceOwner(
        ownerId = logicalProjectId,
        context = getOperationalProjectContext(logicalProjectId),
        workspace = getOperationalProjectWorkspace(logicalProjectId),
    )

    return mission.copy(
        projectId = null,
        projectWorkspaceId = logicalProjectId,
    )
}
