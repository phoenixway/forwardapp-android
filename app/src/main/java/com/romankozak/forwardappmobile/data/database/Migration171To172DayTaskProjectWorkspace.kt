package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Moves exact reserved-System DayTask primary-project refs off the Context FK
 * branch and onto the already-canonical same-id Workspace branch.
 *
 * Legacy System-owned DayTask project data is non-authoritative here. If an
 * exact reserved ref has no valid canonical Workspace, its owner is detached.
 *
 * Reserved ids are frozen historical migration input. Runtime changes to
 * SystemContexts must not retroactively alter migration meaning.
 */
val MIGRATION_171_172 =
    object : Migration(171, 172) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE `day_tasks`
                ADD COLUMN `project_workspace_id` TEXT
                    REFERENCES `workspaces`(`id`)
                    ON UPDATE NO ACTION
                    ON DELETE SET NULL
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_day_tasks_project_workspace_id`
                ON `day_tasks` (`project_workspace_id`)
                """.trimIndent(),
            )

            val reservedIds =
                listOf(
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

            val quotedIds =
                reservedIds.joinToString(",") { "'${it.replace("'", "''")}'" }

            db.execSQL(
                """
                UPDATE day_tasks
                SET project_workspace_id = projectId,
                    projectId = NULL
                WHERE projectId IN ($quotedIds)
                  AND EXISTS (
                      SELECT 1
                      FROM workspaces AS workspace
                      WHERE workspace.id = day_tasks.projectId
                        AND workspace.isDeleted = 0
                        AND workspace.provenance = 'CANONICAL_ONLY'
                        AND workspace.sourceContextId IS NULL
                  )
                """.trimIndent(),
            )

            db.execSQL(
                """
                UPDATE day_tasks
                SET projectId = NULL
                WHERE projectId IN ($quotedIds)
                """.trimIndent(),
            )
        }
    }
