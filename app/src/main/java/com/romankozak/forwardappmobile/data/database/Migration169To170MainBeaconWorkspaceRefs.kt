package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Moves reserved-System Main Beacon operational-owner refs off Context FK
 * ownership and onto the already-canonical same-id Workspace.
 *
 * The exact reserved ids are intentionally frozen in this migration.
 * Historical migrations must not change meaning if SystemContexts evolves later.
 */
val MIGRATION_169_170 =
    object : Migration(169, 170) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Existing schema-169 seed markers may represent startup seeding,
            // direct canonical writes, or imported canonical transport. That
            // history cannot be reconstructed. Close legacy transport ingress
            // conservatively so an old backup can never overwrite established
            // canonical state after upgrade.
            db.execSQL(
                """
                ALTER TABLE `system_workspace_tag_seed_states`
                ADD COLUMN `legacyIngressClosedAt` INTEGER
                """.trimIndent(),
            )
            db.execSQL(
                """
                UPDATE `system_workspace_tag_seed_states`
                SET `legacyIngressClosedAt` = `seededAt`
                WHERE `legacyIngressClosedAt` IS NULL
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `main_beacon_workspace_cross_ref` (
                    `beacon_id` TEXT NOT NULL,
                    `workspace_id` TEXT NOT NULL,
                    `ref_order` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`beacon_id`, `workspace_id`),
                    FOREIGN KEY(`beacon_id`) REFERENCES `main_beacons`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`workspace_id`) REFERENCES `workspaces`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_main_beacon_workspace_cross_ref_workspace_id`
                ON `main_beacon_workspace_cross_ref` (`workspace_id`)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_main_beacon_workspace_cross_ref_beacon_id_ref_order`
                ON `main_beacon_workspace_cross_ref` (`beacon_id`, `ref_order`)
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

            val quotedIds = reservedIds.joinToString(",") { "'${it.replace("'", "''")}'" }

            val invalidOwnerCount =
                db.query(
                    """
                    SELECT COUNT(*)
                    FROM main_beacon_context_cross_ref AS ref
                    LEFT JOIN workspaces AS workspace
                        ON workspace.id = ref.context_id
                    WHERE ref.context_id IN ($quotedIds)
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
                "Reserved Main Beacon Context refs cannot be migrated: " +
                    "$invalidOwnerCount owner(s) lack a live same-id CANONICAL_ONLY Workspace"
            }

            db.execSQL(
                """
                INSERT OR REPLACE INTO main_beacon_workspace_cross_ref(
                    beacon_id,
                    workspace_id,
                    ref_order
                )
                SELECT
                    beacon_id,
                    context_id,
                    ref_order
                FROM main_beacon_context_cross_ref
                WHERE context_id IN ($quotedIds)
                """.trimIndent(),
            )

            db.execSQL(
                """
                DELETE FROM main_beacon_context_cross_ref
                WHERE context_id IN ($quotedIds)
                """.trimIndent(),
            )
        }
    }
