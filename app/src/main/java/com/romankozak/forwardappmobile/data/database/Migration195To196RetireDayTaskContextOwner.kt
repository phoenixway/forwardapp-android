package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Context-FK.4: physical DayTask Context owner retirement, schema 195 -> 196.
 *
 * Every non-null owner must be proven against the frozen 195 Workspace
 * provenance contract before the physical column is removed.
 *
 * Room owns the migration transaction. Any rejection rolls back all changes.
 * Historical wire projectId remains a logical transport field elsewhere.
 */
val MIGRATION_195_196 =
    object : Migration(195, 196) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ensureNoDayTaskSchemaDependents(db)
            validateDayTaskOwners195(db)
            ensureForeignKeys195196(db)

            val previousCount = dayTaskRowCount195196(db, "day_tasks")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `day_tasks_new` (`id` TEXT NOT NULL, `dayPlanId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `goalId` TEXT, `linkedProjectIds` TEXT, `linkedAttachmentIds` TEXT, `activityRecordId` TEXT, `recurrenceSeriesId` TEXT, `recurrenceOccurrenceDayKey` TEXT, `recurrenceSourceSeriesVersion` INTEGER, `taskType` TEXT, `entityId` TEXT, `order` INTEGER NOT NULL, `priority` TEXT NOT NULL, `status` TEXT NOT NULL, `completed` INTEGER NOT NULL, `scheduledTime` INTEGER, `estimatedDurationMinutes` INTEGER, `actualDurationMinutes` INTEGER, `dueTime` INTEGER, `executionStrictness` TEXT NOT NULL DEFAULT 'NORMAL', `valueImportance` REAL NOT NULL DEFAULT 0.0, `valueImpact` REAL NOT NULL DEFAULT 0.0, `effort` REAL NOT NULL DEFAULT 0.0, `cost` REAL NOT NULL DEFAULT 0.0, `risk` REAL NOT NULL DEFAULT 0.0, `location` TEXT, `tags` TEXT, `notes` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER, `syncedAt` INTEGER, `isDeleted` INTEGER NOT NULL, `version` INTEGER NOT NULL, `completedAt` INTEGER, `points` INTEGER NOT NULL DEFAULT 0, `project_workspace_id` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`dayPlanId`) REFERENCES `day_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL  , FOREIGN KEY(`project_workspace_id`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`activityRecordId`) REFERENCES `activity_records`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO `day_tasks_new` (`id`, `dayPlanId`, `title`, `description`, `goalId`, `linkedProjectIds`, `linkedAttachmentIds`, `activityRecordId`, `recurrenceSeriesId`, `recurrenceOccurrenceDayKey`, `recurrenceSourceSeriesVersion`, `taskType`, `entityId`, `order`, `priority`, `status`, `completed`, `scheduledTime`, `estimatedDurationMinutes`, `actualDurationMinutes`, `dueTime`, `executionStrictness`, `valueImportance`, `valueImpact`, `effort`, `cost`, `risk`, `location`, `tags`, `notes`, `createdAt`, `updatedAt`, `syncedAt`, `isDeleted`, `version`, `completedAt`, `points`, `project_workspace_id`)
                SELECT `id`, `dayPlanId`, `title`, `description`, `goalId`, `linkedProjectIds`, `linkedAttachmentIds`, `activityRecordId`, `recurrenceSeriesId`, `recurrenceOccurrenceDayKey`, `recurrenceSourceSeriesVersion`, `taskType`, `entityId`, `order`, `priority`, `status`, `completed`, `scheduledTime`, `estimatedDurationMinutes`, `actualDurationMinutes`, `dueTime`, `executionStrictness`, `valueImportance`, `valueImpact`, `effort`, `cost`, `risk`, `location`, `tags`, `notes`, `createdAt`, `updatedAt`, `syncedAt`, `isDeleted`, `version`, `completedAt`, `points`, COALESCE(`project_workspace_id`, `projectId`)
                FROM `day_tasks`
                """.trimIndent(),
            )

            check(
                dayTaskRowCount195196(db, "day_tasks_new") == previousCount,
            ) {
                "DayTask 195->196 migration changed row count"
            }

            check(
                dayTaskScalar195196(
                    db,
                    """
                    SELECT COUNT(*) FROM (
                        SELECT `id`, `dayPlanId`, `title`, `description`, `goalId`, `linkedProjectIds`, `linkedAttachmentIds`, `activityRecordId`, `recurrenceSeriesId`, `recurrenceOccurrenceDayKey`, `recurrenceSourceSeriesVersion`, `taskType`, `entityId`, `order`, `priority`, `status`, `completed`, `scheduledTime`, `estimatedDurationMinutes`, `actualDurationMinutes`, `dueTime`, `executionStrictness`, `valueImportance`, `valueImpact`, `effort`, `cost`, `risk`, `location`, `tags`, `notes`, `createdAt`, `updatedAt`, `syncedAt`, `isDeleted`, `version`, `completedAt`, `points` FROM `day_tasks`
                        EXCEPT
                        SELECT `id`, `dayPlanId`, `title`, `description`, `goalId`, `linkedProjectIds`, `linkedAttachmentIds`, `activityRecordId`, `recurrenceSeriesId`, `recurrenceOccurrenceDayKey`, `recurrenceSourceSeriesVersion`, `taskType`, `entityId`, `order`, `priority`, `status`, `completed`, `scheduledTime`, `estimatedDurationMinutes`, `actualDurationMinutes`, `dueTime`, `executionStrictness`, `valueImportance`, `valueImpact`, `effort`, `cost`, `risk`, `location`, `tags`, `notes`, `createdAt`, `updatedAt`, `syncedAt`, `isDeleted`, `version`, `completedAt`, `points` FROM `day_tasks_new`
                    )
                    """.trimIndent(),
                ) == 0L,
            ) {
                "DayTask 195->196 migration changed non-owner fields"
            }

            check(
                dayTaskScalar195196(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM `day_tasks` AS old
                    JOIN `day_tasks_new` AS replacement
                        ON replacement.id = old.id
                    WHERE replacement.project_workspace_id
                        IS NOT COALESCE(old.project_workspace_id, old.projectId)
                    """.trimIndent(),
                ) == 0L,
            ) {
                "DayTask 195->196 migration changed canonical owner identity"
            }

            db.execSQL("DROP TABLE `day_tasks`")
            db.execSQL("ALTER TABLE `day_tasks_new` RENAME TO `day_tasks`")

            db.execSQL("""CREATE INDEX IF NOT EXISTS `index_day_tasks_dayPlanId` ON `day_tasks` (`dayPlanId`)""")
            db.execSQL("""CREATE INDEX IF NOT EXISTS `index_day_tasks_goalId` ON `day_tasks` (`goalId`)""")
            db.execSQL("""CREATE INDEX IF NOT EXISTS `index_day_tasks_project_workspace_id` ON `day_tasks` (`project_workspace_id`)""")
            db.execSQL("""CREATE INDEX IF NOT EXISTS `index_day_tasks_activityRecordId` ON `day_tasks` (`activityRecordId`)""")
            db.execSQL("""CREATE INDEX IF NOT EXISTS `index_day_tasks_scheduledTime` ON `day_tasks` (`scheduledTime`)""")
            db.execSQL("""CREATE INDEX IF NOT EXISTS `index_day_tasks_recurrenceSeriesId_recurrenceOccurrenceDayKey` ON `day_tasks` (`recurrenceSeriesId`, `recurrenceOccurrenceDayKey`)""")

            ensureForeignKeys195196(db)

            db.query("PRAGMA integrity_check").use { cursor ->
                check(cursor.moveToFirst() && cursor.getString(0) == "ok") {
                    "DayTask 195->196 integrity_check failed"
                }
            }

            val columns =
                db.query("PRAGMA table_info(`day_tasks`)").use { cursor ->
                    buildSet {
                        val index = cursor.getColumnIndexOrThrow("name")
                        while (cursor.moveToNext()) {
                            add(cursor.getString(index))
                        }
                    }
                }

            check("projectId" !in columns)
            check("project_workspace_id" in columns)
            check(dayTaskRowCount195196(db, "day_tasks") == previousCount)
        }
    }

private fun validateDayTaskOwners195(db: SupportSQLiteDatabase) {
    db.query(
        """
        SELECT
            task.id,
            task.projectId,
            task.project_workspace_id,
            context.id,
            context.is_deleted,
            workspace.id,
            workspace.isDeleted,
            workspace.provenance,
            workspace.sourceContextId
        FROM day_tasks AS task
        LEFT JOIN contexts AS context
            ON context.id = COALESCE(task.project_workspace_id, task.projectId)
        LEFT JOIN workspaces AS workspace
            ON workspace.id = COALESCE(task.project_workspace_id, task.projectId)
        WHERE task.projectId IS NOT NULL
           OR task.project_workspace_id IS NOT NULL
        ORDER BY task.id
        """.trimIndent(),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            val taskId = cursor.getString(0)
            val legacy = if (cursor.isNull(1)) null else cursor.getString(1)
            val canonical = if (cursor.isNull(2)) null else cursor.getString(2)
            val ownerId = canonical ?: legacy
                ?: error("DayTask $taskId has no project owner")

            check(ownerId.isNotBlank()) {
                "DayTask $taskId has a blank project owner"
            }

            check(legacy == null || canonical == null || legacy == canonical) {
                "DayTask $taskId has divergent legacy and Workspace owners"
            }

            check(!cursor.isNull(5) && cursor.getString(5) == ownerId) {
                "DayTask $taskId owner $ownerId has no canonical Workspace"
            }

            check(cursor.getInt(6) == 0) {
                "DayTask $taskId owner $ownerId has a deleted Workspace"
            }

            val provenance = cursor.getString(7)
            val source = if (cursor.isNull(8)) null else cursor.getString(8)
            val canonicalOnly = provenance == "CANONICAL_ONLY" && source == null
            val standalone = provenance == "STANDALONE" && source == null
            val contextBacked =
                provenance == "CONTEXT_BACKED" && source == ownerId

            val allowed =
                when {
                    ownerId in FROZEN_SYSTEM_DAYTASK_195 -> canonicalOnly
                    !cursor.isNull(3) && cursor.getInt(4) == 0 -> contextBacked
                    !cursor.isNull(3) -> canonicalOnly
                    else -> canonicalOnly || standalone || contextBacked
                }

            check(allowed) {
                "DayTask $taskId owner $ownerId has unproven Workspace lineage"
            }
        }
    }
}

private fun ensureNoDayTaskSchemaDependents(db: SupportSQLiteDatabase) {
    db.query(
        """
        SELECT name FROM sqlite_master
        WHERE type = 'trigger' AND tbl_name = 'day_tasks'
        """.trimIndent(),
    ).use { cursor ->
        check(!cursor.moveToFirst()) {
            "DayTask 195->196 has a persistent table trigger"
        }
    }

    db.query(
        """
        SELECT name FROM sqlite_master
        WHERE type = 'view' AND lower(sql) LIKE '%day_tasks%'
        """.trimIndent(),
    ).use { cursor ->
        check(!cursor.moveToFirst()) {
            "DayTask 195->196 has a dependent SQL view"
        }
    }

    val inbound =
        db.query(
            """
            SELECT name FROM sqlite_master
            WHERE type = 'table'
              AND name NOT LIKE 'sqlite_%'
            """.trimIndent(),
        ).use { tables ->
            buildList {
                while (tables.moveToNext()) {
                    val table = tables.getString(0)
                    db.query("PRAGMA foreign_key_list(`$table`)").use { foreignKeys ->
                        val targetColumn =
                            foreignKeys.getColumnIndexOrThrow("table")
                        while (foreignKeys.moveToNext()) {
                            if (foreignKeys.getString(targetColumn) == "day_tasks") {
                                add(table)
                            }
                        }
                    }
                }
            }
        }

    check(inbound.isEmpty()) {
        "DayTask 195->196 has inbound foreign keys: $inbound"
    }
}

private fun ensureForeignKeys195196(db: SupportSQLiteDatabase) {
    db.query("PRAGMA foreign_key_check").use { cursor ->
        check(!cursor.moveToFirst()) {
            "DayTask 195->196 foreign_key_check failed"
        }
    }
}

private fun dayTaskRowCount195196(
    db: SupportSQLiteDatabase,
    table: String,
): Long = dayTaskScalar195196(db, "SELECT COUNT(*) FROM `$table`")

private fun dayTaskScalar195196(
    db: SupportSQLiteDatabase,
    sql: String,
): Long =
    db.query(sql).use { cursor ->
        check(cursor.moveToFirst()) {
            "DayTask 195->196 scalar query produced no row"
        }
        cursor.getLong(0)
    }

private val FROZEN_SYSTEM_DAYTASK_195 =
    setOf(
        "sys_personal-management",
        "sys_mode-about",
        "sys_mode-improve",
        "sys_mode-execution",
        "sys_mode-control",
        "sys_mode-recovery",
        "sys_mode-emergency",
        "sys_strategic",
        "sys_mission",
        "sys_long-term-strategy",
        "sys_strategic-programs",
        "sys_medium-term-strategy",
        "sys_active-quests",
        "sys_levels",
        "sys_week",
        "sys_inbox",
        "sys_strategic-inbox",
        "sys_strategic-review",
        "sys_main-beacons",
        "sys_today",
    )
