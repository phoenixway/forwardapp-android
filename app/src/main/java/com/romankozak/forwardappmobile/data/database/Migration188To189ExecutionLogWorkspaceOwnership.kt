package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Physically retires the legacy Context locator from canonical EXECUTION_LOG storage. */
val MIGRATION_188_189 =
    object : Migration(188, 189) {
        override fun migrate(db: SupportSQLiteDatabase) {
            requireCount(
                db = db,
                query =
                    """
                    SELECT COUNT(*)
                    FROM context_execution_logs AS log
                    LEFT JOIN workspaces AS workspace
                      ON workspace.id = log.contextId
                     AND workspace.provenance = 'CONTEXT_BACKED'
                     AND workspace.sourceContextId = log.contextId
                    WHERE log.contextId IS NOT NULL
                      AND (
                        (log.workspaceId IS NULL AND workspace.id IS NULL)
                        OR (log.workspaceId IS NOT NULL AND log.workspaceId != log.contextId)
                      )
                    """.trimIndent(),
                expected = 0L,
                message = "Legacy EXECUTION_LOG rows lack a unique owner or contain conflicting locators",
            )

            db.execSQL(
                """
                UPDATE context_execution_logs
                SET workspaceId = contextId
                WHERE contextId IS NOT NULL
                  AND workspaceId IS NULL
                """.trimIndent(),
            )

            requireCount(
                db = db,
                query =
                    """
                    SELECT COUNT(*)
                    FROM context_execution_logs AS log
                    LEFT JOIN workspaces AS workspace ON workspace.id = log.workspaceId
                    WHERE log.workspaceId IS NULL
                       OR workspace.id IS NULL
                       OR workspace.provenance NOT IN ('CONTEXT_BACKED', 'CANONICAL_ONLY', 'STANDALONE')
                       OR (workspace.isDeleted != 0 AND log.is_deleted = 0)
                    """.trimIndent(),
                expected = 0L,
                message = "EXECUTION_LOG rows contain invalid canonical Workspace ownership",
            )

            db.execSQL(
                """
                CREATE TABLE `context_execution_logs_189` (
                    `id` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `type` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `details` TEXT,
                    `updatedAt` INTEGER,
                    `synced_at` INTEGER,
                    `is_deleted` INTEGER NOT NULL DEFAULT 0,
                    `version` INTEGER NOT NULL DEFAULT 0,
                    `workspaceId` TEXT NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO context_execution_logs_189 (
                    id, timestamp, type, description, details, updatedAt,
                    synced_at, is_deleted, version, workspaceId
                )
                SELECT
                    id, timestamp, type, description, details, updatedAt,
                    synced_at, is_deleted, version, workspaceId
                FROM context_execution_logs
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE context_execution_logs")
            db.execSQL("ALTER TABLE context_execution_logs_189 RENAME TO context_execution_logs")
            db.execSQL(
                "CREATE INDEX `index_context_execution_logs_workspaceId` " +
                    "ON `context_execution_logs` (`workspaceId`)",
            )

            requireCount(
                db = db,
                query = "PRAGMA foreign_key_check",
                expected = 0L,
                message = "EXECUTION_LOG Workspace foreign-key validation failed",
                countRows = true,
            )
        }
    }

private fun requireCount(
    db: SupportSQLiteDatabase,
    query: String,
    expected: Long,
    message: String,
    countRows: Boolean = false,
) {
    val actual =
        db.query(query).use { cursor ->
            if (countRows) {
                cursor.count.toLong()
            } else {
                check(cursor.moveToFirst())
                cursor.getLong(0)
            }
        }
    check(actual == expected) { "$message: $actual row(s)" }
}
