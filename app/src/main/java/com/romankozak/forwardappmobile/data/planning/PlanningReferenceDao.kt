package com.romankozak.forwardappmobile.data.planning

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanningReferenceDao {
    /** Lightweight inverse relation read. Explicit owner discriminators, never cross-table ID guessing. */
    @Query("""
        SELECT r.id AS relationId, r.ownerKind, r.ownerId, r.role,
            COALESCE(s.title, m.title, q.title, t.title, f.title, 'Planning scope') AS title
        FROM planning_references r
        JOIN workspaces w ON w.id = r.targetId AND w.isDeleted = 0
        LEFT JOIN planning_scopes s ON r.ownerKind='SCOPE' AND s.id=r.ownerId AND s.isDeleted=0
        LEFT JOIN tactical_missions m ON r.ownerKind='TACTICAL_MISSION' AND CAST(m.id AS TEXT)=r.ownerId AND m.is_deleted=0
        LEFT JOIN arc_quests q ON r.ownerKind='ARC_QUEST' AND q.id=r.ownerId AND q.is_deleted=0
        LEFT JOIN day_tasks t ON r.ownerKind='DAY_TASK' AND t.id=r.ownerId AND t.isDeleted=0
        LEFT JOIN day_focus_items f ON r.ownerKind='DAY_FOCUS' AND f.id=r.ownerId AND f.isDeleted=0
        WHERE r.targetKind='WORKSPACE' AND r.targetId=:workspaceId AND r.isDeleted=0
            AND r.role IN ('SOURCE', 'RELATED')
            AND (s.id IS NOT NULL OR m.id IS NOT NULL OR q.id IS NOT NULL OR t.id IS NOT NULL OR f.id IS NOT NULL)
        ORDER BY r.ownerKind, r.ownerId, r.role, r.id
    """)
    fun observeWorkspaceRelations(workspaceId: String): Flow<List<PlanningWorkspaceRelationRow>>

    /** Same explicit Planning owner read, with a live typed Aspect endpoint. */
    @Query("""
        SELECT r.id AS relationId, r.ownerKind, r.ownerId, r.role,
            COALESCE(s.title, m.title, q.title, t.title, f.title, 'Planning scope') AS title
        FROM planning_references r
        JOIN managed_subjects target ON target.id=r.targetId AND target.subjectType='ASPECT' AND target.isDeleted=0
        JOIN aspects a ON a.subjectId=target.id
        LEFT JOIN planning_scopes s ON r.ownerKind='SCOPE' AND s.id=r.ownerId AND s.isDeleted=0
        LEFT JOIN tactical_missions m ON r.ownerKind='TACTICAL_MISSION' AND CAST(m.id AS TEXT)=r.ownerId AND m.is_deleted=0
        LEFT JOIN arc_quests q ON r.ownerKind='ARC_QUEST' AND q.id=r.ownerId AND q.is_deleted=0
        LEFT JOIN day_tasks t ON r.ownerKind='DAY_TASK' AND t.id=r.ownerId AND t.isDeleted=0
        LEFT JOIN day_focus_items f ON r.ownerKind='DAY_FOCUS' AND f.id=r.ownerId AND f.isDeleted=0
        WHERE r.targetKind='ASPECT' AND r.targetId=:aspectId AND r.isDeleted=0
            AND r.role IN ('SOURCE', 'RELATED')
            AND (s.id IS NOT NULL OR m.id IS NOT NULL OR q.id IS NOT NULL OR t.id IS NOT NULL OR f.id IS NOT NULL)
        ORDER BY r.ownerKind, r.ownerId, r.role, r.id
    """)
    fun observeAspectRelations(aspectId: String): Flow<List<PlanningWorkspaceRelationRow>>

    @Query("SELECT * FROM planning_references ORDER BY id")
    fun observeAll(): Flow<List<PlanningReferenceEntity>>

    @Query("SELECT * FROM planning_references ORDER BY id")
    suspend fun getAll(): List<PlanningReferenceEntity>

    @Query("SELECT * FROM planning_references WHERE ownerKind=:kind AND ownerId=:id ORDER BY role, targetKind, targetId")
    suspend fun getForOwner(kind: String, id: String): List<PlanningReferenceEntity>

    @Query("SELECT * FROM planning_references WHERE id=:id")
    suspend fun get(id: String): PlanningReferenceEntity?

    @Upsert
    suspend fun upsert(row: PlanningReferenceEntity)

    @Query("DELETE FROM planning_references")
    suspend fun clear()

    @Query("SELECT * FROM planning_references WHERE syncedAt IS NULL ORDER BY id")
    suspend fun getUnsynced(): List<PlanningReferenceEntity>

    @Query("SELECT * FROM planning_references WHERE updatedAt > :timestamp ORDER BY id")
    suspend fun getChangedSince(timestamp: Long): List<PlanningReferenceEntity>

    @Query("UPDATE planning_references SET syncedAt=:at WHERE id=:id AND version=:version")
    suspend fun acknowledge(id: String, version: Long, at: Long)
}

data class PlanningWorkspaceRelationRow(
    val relationId: String,
    val ownerKind: String,
    val ownerId: String,
    val role: String,
    val title: String,
)
