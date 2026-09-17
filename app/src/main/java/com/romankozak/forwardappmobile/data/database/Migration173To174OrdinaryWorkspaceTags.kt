package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Backfills canonical Workspace-owned tag membership for ordinary Contexts
 * that were already semantically retired before ordinary tag ownership was
 * cut over.
 *
 * context_tag_refs is the normalized legacy membership mirror maintained by
 * TagAssociationHandler. Only deleted Contexts whose same-id live Workspace is
 * already CANONICAL_ONLY are eligible. Existing workspace_tag_refs rows are
 * preserved because canonical state wins over legacy compatibility evidence.
 * If any canonical row already exists for a Workspace, including a tombstone,
 * that whole Workspace collection is considered canonical and legacy rows are
 * not added.
 */
val MIGRATION_173_174 =
    object : Migration(173, 174) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                INSERT OR IGNORE INTO workspace_tag_refs (
                    workspaceId,
                    normalizedTag,
                    createdAt,
                    updatedAt,
                    syncedAt,
                    isDeleted,
                    version
                )
                SELECT
                    legacy.context_id,
                    legacy.normalized_tag,
                    context.createdAt,
                    COALESCE(context.updatedAt, context.createdAt),
                    NULL,
                    0,
                    1
                FROM context_tag_refs AS legacy
                JOIN contexts AS context
                  ON context.id = legacy.context_id
                JOIN workspaces AS workspace
                  ON workspace.id = legacy.context_id
                WHERE context.is_deleted = 1
                  AND workspace.isDeleted = 0
                  AND workspace.provenance = 'CANONICAL_ONLY'
                  AND workspace.sourceContextId IS NULL
                  AND NOT EXISTS (
                      SELECT 1
                      FROM workspace_tag_refs AS canonical
                      WHERE canonical.workspaceId = legacy.context_id
                  )
                """.trimIndent(),
            )
        }
    }
