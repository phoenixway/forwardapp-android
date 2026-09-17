package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Moves exact reserved-System TacticalMission primary-project refs off the
 * Context FK branch and onto the already-canonical same-id Workspace branch.
 *
 * The reserved ids are frozen in this historical migration. Runtime changes to
 * SystemContexts must not retroactively alter migration meaning.
 */
val MIGRATION_170_171 =
    object : Migration(170, 171) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE `tactical_missions`
                ADD COLUMN `project_workspace_id` TEXT
                    REFERENCES `workspaces`(`id`)
                    ON UPDATE NO ACTION
                    ON DELETE SET NULL
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_tactical_missions_project_workspace_id`
                ON `tactical_missions` (`project_workspace_id`)
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

            val invalidOwnerCount =
                db.query(
                    """
                    SELECT COUNT(*)
                    FROM tactical_missions AS mission
                    LEFT JOIN workspaces AS workspace
                        ON workspace.id = mission.projectId
                    WHERE mission.projectId IN ($quotedIds)
                      AND (
                          workspace.id IS NULL
                          OR workspace.isDeleted != 0
                          OR workspace.provenance != 'CANONICAL_ONLY'
                          OR workspace.sourceContextId IS NOT NULL
                      )
                    """.trimIndent(),
                ).use { cursor ->
                    check(cursor.moveToFirst())
                    cursor.getLong(0)
                }

            check(invalidOwnerCount == 0L) {
                "Reserved TacticalMission project refs cannot be migrated: " +
                    "$invalidOwnerCount owner(s) lack a live same-id " +
                    "CANONICAL_ONLY Workspace"
            }

            db.execSQL(
                """
                UPDATE tactical_missions
                SET project_workspace_id = projectId,
                    projectId = NULL
                WHERE projectId IN ($quotedIds)
                """.trimIndent(),
            )
        }
    }
