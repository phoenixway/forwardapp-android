package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema-only BACKLOG foundation.
 *
 * Context-backed list_items and backlog_orders deliberately remain untouched
 * and authoritative until the later frozen hard-cutover migration.
 */
val MIGRATION_159_160 =
    object : Migration(159, 160) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS workspace_backlog_entries (
                    id TEXT NOT NULL,
                    workspaceId TEXT NOT NULL,
                    capabilityInstanceId TEXT NOT NULL,
                    targetKind TEXT NOT NULL,
                    targetId TEXT NOT NULL,
                    entryOrder INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncedAt INTEGER,
                    isDeleted INTEGER NOT NULL,
                    version INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(workspaceId) REFERENCES workspaces(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(capabilityInstanceId) REFERENCES workspace_capability_instances(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX index_workspace_backlog_entries_workspaceId " +
                    "ON workspace_backlog_entries(workspaceId)",
            )
            db.execSQL(
                "CREATE INDEX index_workspace_backlog_entries_capabilityInstanceId " +
                    "ON workspace_backlog_entries(capabilityInstanceId)",
            )
            db.execSQL(
                "CREATE INDEX index_workspace_backlog_entries_targetKind_targetId " +
                    "ON workspace_backlog_entries(targetKind, targetId)",
            )
            db.execSQL(
                "CREATE INDEX index_workspace_backlog_entries_updatedAt " +
                    "ON workspace_backlog_entries(updatedAt)",
            )
            db.execSQL(
                "CREATE INDEX index_workspace_backlog_entries_isDeleted " +
                    "ON workspace_backlog_entries(isDeleted)",
            )
            db.execSQL(
                "CREATE INDEX index_workspace_backlog_entries_capabilityInstanceId_entryOrder " +
                    "ON workspace_backlog_entries(capabilityInstanceId, entryOrder)",
            )
            db.execSQL(
                "CREATE INDEX index_workspace_backlog_entries_capabilityInstanceId_targetKind_targetId " +
                    "ON workspace_backlog_entries(capabilityInstanceId, targetKind, targetId)",
            )
        }
    }
