package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Replaces SystemApp's legacy Context foreign key with its canonical
 * operational Workspace owner. Existing stable system ids are preserved.
 */
val MIGRATION_166_167 =
    object : Migration(166, 167) {
        override fun migrate(db: SupportSQLiteDatabase) {
            requireNoSystemAppsWithoutLiveSameIdWorkspace(db)
            val sourceRowCount = rowCount(db, "system_apps")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `system_apps_new` (
                    `id` TEXT NOT NULL,
                    `system_key` TEXT NOT NULL,
                    `app_type` TEXT NOT NULL,
                    `workspace_id` TEXT NOT NULL,
                    `note_document_id` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    `isDeleted` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`workspace_id`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`note_document_id`) REFERENCES `note_documents`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `system_apps_new` (
                    `id`, `system_key`, `app_type`, `workspace_id`, `note_document_id`,
                    `createdAt`, `updatedAt`, `version`, `isDeleted`
                )
                SELECT
                    `id`, `system_key`, `app_type`, `context_id`, `note_document_id`,
                    `createdAt`, `updatedAt`, `version`, `isDeleted`
                FROM `system_apps`
                """.trimIndent(),
            )
            check(rowCount(db, "system_apps_new") == sourceRowCount) {
                "SystemApp Workspace ownership cutover lost rows"
            }
            db.execSQL("DROP TABLE `system_apps`")
            db.execSQL("ALTER TABLE `system_apps_new` RENAME TO `system_apps`")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_system_apps_system_key` ON `system_apps` (`system_key`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_system_apps_workspace_id` ON `system_apps` (`workspace_id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_system_apps_note_document_id` ON `system_apps` (`note_document_id`)",
            )
        }
}

private fun rowCount(
    db: SupportSQLiteDatabase,
    table: String,
): Long =
    db.query("SELECT COUNT(*) FROM `$table`").use { cursor ->
        check(cursor.moveToFirst()) { "Unable to count $table" }
        cursor.getLong(0)
    }

private fun requireNoSystemAppsWithoutLiveSameIdWorkspace(db: SupportSQLiteDatabase) {
    db.query(
        """
        SELECT app.`id`, app.`context_id`
        FROM `system_apps` AS app
        LEFT JOIN `workspaces` AS workspace
            ON workspace.`id` = app.`context_id`
        WHERE workspace.`id` IS NULL OR workspace.`isDeleted` != 0
        LIMIT 1
        """.trimIndent(),
    ).use { cursor ->
        check(!cursor.moveToFirst()) {
            val appId = cursor.getString(0)
            val ownerId = cursor.getString(1)
            "SystemApp Workspace ownership cutover blocked: app=$appId owner=$ownerId has no live same-id Workspace"
        }
    }
}
