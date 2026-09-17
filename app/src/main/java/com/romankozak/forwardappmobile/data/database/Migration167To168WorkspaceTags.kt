package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Introduces canonical Workspace-owned tag membership storage.
 *
 * Legacy Context.tags and context_tag_refs remain untouched. Runtime ownership
 * cutover and compatibility projection are separate step-8 slices.
 */
val MIGRATION_167_168 =
    object : Migration(167, 168) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workspace_tag_refs` (
                    `workspaceId` TEXT NOT NULL,
                    `normalizedTag` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `isDeleted` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    PRIMARY KEY(`workspaceId`, `normalizedTag`),
                    FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workspace_tag_refs_normalizedTag` " +
                    "ON `workspace_tag_refs` (`normalizedTag`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workspace_tag_refs_updatedAt` " +
                    "ON `workspace_tag_refs` (`updatedAt`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workspace_tag_refs_isDeleted` " +
                    "ON `workspace_tag_refs` (`isDeleted`)",
            )
        }
    }
