package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Retires three false Context-FK ownership constraints before exact reserved
 * System Context shells can disappear.
 *
 * These columns are stable logical association/target ids. Their domains are
 * wider than persisted Context rows:
 *
 * - inbox_record_links.context_id may target an ordinary Context or a
 *   canonical exact-System Workspace;
 * - backlog_goal_association_links.context_id has the same projection
 *   semantics;
 * - scripts.contextId is optional logical grouping/provenance, matching the
 *   non-FK association semantics already used by note documents/checklists.
 *
 * No ids are rewritten or detached. Only the invalid FK ownership contract is
 * removed. All authoritative/non-Context foreign keys remain intact.
 */
val MIGRATION_172_173 =
    object : Migration(172, 173) {
        override fun migrate(db: SupportSQLiteDatabase) {
            rebuildInboxRecordLinks(db)
            rebuildBacklogGoalAssociationLinks(db)
            rebuildScripts(db)
        }
    }

private fun rebuildInboxRecordLinks(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE `inbox_record_links_new` (
            `record_id` TEXT NOT NULL,
            `context_id` TEXT NOT NULL,
            `owner_context_id` TEXT NOT NULL,
            `association_tag` TEXT,
            `linked_at` INTEGER NOT NULL,
            PRIMARY KEY(`record_id`, `context_id`),
            FOREIGN KEY(`record_id`) REFERENCES `workspace_inbox_records`(`id`)
                ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )

    db.execSQL(
        """
        INSERT INTO `inbox_record_links_new` (
            `record_id`,
            `context_id`,
            `owner_context_id`,
            `association_tag`,
            `linked_at`
        )
        SELECT
            `record_id`,
            `context_id`,
            `owner_context_id`,
            `association_tag`,
            `linked_at`
        FROM `inbox_record_links`
        """.trimIndent(),
    )

    db.execSQL("DROP TABLE `inbox_record_links`")
    db.execSQL("ALTER TABLE `inbox_record_links_new` RENAME TO `inbox_record_links`")

    db.execSQL(
        "CREATE INDEX `index_inbox_record_links_context_id` " +
            "ON `inbox_record_links` (`context_id`)",
    )
    db.execSQL(
        "CREATE INDEX `index_inbox_record_links_record_id` " +
            "ON `inbox_record_links` (`record_id`)",
    )
    db.execSQL(
        "CREATE INDEX `index_inbox_record_links_owner_context_id_record_id` " +
            "ON `inbox_record_links` (`owner_context_id`, `record_id`)",
    )
}

private fun rebuildBacklogGoalAssociationLinks(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE `backlog_goal_association_links_new` (
            `projection_id` TEXT NOT NULL,
            `goal_id` TEXT NOT NULL,
            `context_id` TEXT NOT NULL,
            `owner_context_id` TEXT NOT NULL,
            `association_tag` TEXT,
            `item_order` INTEGER NOT NULL,
            `linked_at` INTEGER NOT NULL,
            PRIMARY KEY(`goal_id`, `context_id`),
            FOREIGN KEY(`goal_id`) REFERENCES `goals`(`id`)
                ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )

    db.execSQL(
        """
        INSERT INTO `backlog_goal_association_links_new` (
            `projection_id`,
            `goal_id`,
            `context_id`,
            `owner_context_id`,
            `association_tag`,
            `item_order`,
            `linked_at`
        )
        SELECT
            `projection_id`,
            `goal_id`,
            `context_id`,
            `owner_context_id`,
            `association_tag`,
            `item_order`,
            `linked_at`
        FROM `backlog_goal_association_links`
        """.trimIndent(),
    )

    db.execSQL("DROP TABLE `backlog_goal_association_links`")
    db.execSQL(
        "ALTER TABLE `backlog_goal_association_links_new` " +
            "RENAME TO `backlog_goal_association_links`",
    )

    db.execSQL(
        "CREATE INDEX `index_backlog_goal_association_links_context_id` " +
            "ON `backlog_goal_association_links` (`context_id`)",
    )
    db.execSQL(
        "CREATE INDEX `index_backlog_goal_association_links_goal_id` " +
            "ON `backlog_goal_association_links` (`goal_id`)",
    )
    db.execSQL(
        "CREATE INDEX `index_backlog_goal_association_links_owner_context_id_goal_id` " +
            "ON `backlog_goal_association_links` (`owner_context_id`, `goal_id`)",
    )
    db.execSQL(
        "CREATE UNIQUE INDEX `index_backlog_goal_association_links_projection_id` " +
            "ON `backlog_goal_association_links` (`projection_id`)",
    )
}

private fun rebuildScripts(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE `scripts_new` (
            `id` TEXT NOT NULL,
            `contextId` TEXT,
            `name` TEXT NOT NULL,
            `description` TEXT,
            `content` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL,
            `syncedAt` INTEGER,
            `isDeleted` INTEGER NOT NULL,
            `version` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
        """.trimIndent(),
    )

    db.execSQL(
        """
        INSERT INTO `scripts_new` (
            `id`,
            `contextId`,
            `name`,
            `description`,
            `content`,
            `createdAt`,
            `updatedAt`,
            `syncedAt`,
            `isDeleted`,
            `version`
        )
        SELECT
            `id`,
            `contextId`,
            `name`,
            `description`,
            `content`,
            `createdAt`,
            `updatedAt`,
            `syncedAt`,
            `isDeleted`,
            `version`
        FROM `scripts`
        """.trimIndent(),
    )

    db.execSQL("DROP TABLE `scripts`")
    db.execSQL("ALTER TABLE `scripts_new` RENAME TO `scripts`")

    db.execSQL(
        "CREATE INDEX `index_scripts_contextId` ON `scripts` (`contextId`)",
    )
    db.execSQL(
        "CREATE INDEX `index_scripts_name` ON `scripts` (`name`)",
    )
}
