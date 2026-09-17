package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Persists the one-time System Context.tags -> Workspace tag collection seed.
 *
 * The marker is required because an empty canonical Workspace tag collection
 * has no membership row. Without it, every future startup could mistake that
 * empty canonical state for an unseeded collection and re-import stale legacy
 * Context.tags.
 */
val MIGRATION_168_169 =
    object : Migration(168, 169) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `system_workspace_tag_seed_states` (
                    `workspaceId` TEXT NOT NULL,
                    `seededAt` INTEGER NOT NULL,
                    PRIMARY KEY(`workspaceId`),
                    FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
        }
    }
