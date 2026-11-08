package com.romankozak.forwardappmobile.data.workspace

import androidx.room.Dao
import androidx.room.Query
import com.romankozak.forwardappmobile.data.workspace.capability.WorkspaceBacklogReadRow

/** One SQL statement is one coherent SQLite snapshot, without Room's writer transaction queue. */
@Dao
interface WorkspaceBacklogReadDao {
    @Query("""
        SELECT e.id AS id, e.workspaceId, e.targetKind, e.targetId, e.entryOrder AS itemOrder,
               e.updatedAt, e.syncedAt, e.version,
               m.sourceId AS mappedGoalId,
               w.isDeleted AS workspaceDeleted,
               NULL AS derivedGoalId, NULL AS associationOwnerContextId, NULL AS associationTag
        FROM workspace_backlog_entries e
        LEFT JOIN legacy_subject_mappings m
          ON e.targetKind = 'ORIENTATION' AND m.subjectId = e.targetId
         AND m.sourceType = 'GOAL' AND m.state = 'CUT_OVER' AND m.isDeleted = 0
        LEFT JOIN workspaces w ON e.targetKind = 'WORKSPACE' AND w.id = e.targetId
        WHERE e.workspaceId = :workspaceId AND e.isDeleted = 0
        UNION ALL
        SELECT bg.projection_id, bg.context_id, '', '', bg.item_order,
               bg.linked_at, NULL, 0, NULL, NULL,
               bg.goal_id, bg.owner_context_id, bg.association_tag
        FROM backlog_goal_association_links bg
        WHERE :includeDerived AND bg.context_id = :workspaceId
        ORDER BY itemOrder, id
    """)
    suspend fun getRows(workspaceId: String, includeDerived: Boolean): List<WorkspaceBacklogReadRow>
}
