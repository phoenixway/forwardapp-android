package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Establishes canonical per-Workspace presentation preference authority.
 *
 * Existing same-id Context rows are migration evidence only. The migration
 * copies their three presentation scalars into live Workspace-owned rows and
 * never creates, revives, or mutates a Context or Workspace.
 */
val MIGRATION_184_185 =
    object : Migration(184, 185) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workspace_presentation_preferences` (
                    `workspaceId` TEXT NOT NULL,
                    `defaultViewModeName` TEXT,
                    `isAttachmentsExpanded` INTEGER NOT NULL DEFAULT 0,
                    `showCheckboxes` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `version` INTEGER NOT NULL,
                    PRIMARY KEY(`workspaceId`),
                    FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_workspace_presentation_preferences_updatedAt`
                ON `workspace_presentation_preferences` (`updatedAt`)
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT OR IGNORE INTO `workspace_presentation_preferences` (
                    `workspaceId`,
                    `defaultViewModeName`,
                    `isAttachmentsExpanded`,
                    `showCheckboxes`,
                    `createdAt`,
                    `updatedAt`,
                    `syncedAt`,
                    `version`
                )
                SELECT
                    workspace.`id`,
                    context.`default_view_mode`,
                    context.`is_attachments_expanded`,
                    context.`show_checkboxes`,
                    workspace.`createdAt`,
                    CASE
                        WHEN context.`updatedAt` IS NOT NULL
                            AND context.`updatedAt` > workspace.`updatedAt`
                            THEN context.`updatedAt`
                        ELSE workspace.`updatedAt`
                    END,
                    NULL,
                    1
                FROM `workspaces` AS workspace
                INNER JOIN `contexts` AS context
                    ON context.`id` = workspace.`id`
                WHERE workspace.`isDeleted` = 0
                """.trimIndent(),
            )
        }
    }
