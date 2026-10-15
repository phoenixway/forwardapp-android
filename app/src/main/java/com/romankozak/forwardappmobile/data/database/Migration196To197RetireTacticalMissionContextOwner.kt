package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Context-FK.6: physical TacticalMission Context-owner retirement.
 *
 * Schema 196 -> 197.
 *
 * Canonical owner: project_workspace_id.
 * Legacy projectId remains a logical transport compatibility field only.
 *
 * Room owns the migration transaction. Invalid ownership or preservation
 * evidence fails closed and rolls back the entire migration.
 *
 * The attachment cross-ref is deliberately backed up and recreated before
 * dropping the original mission parent, avoiding ON DELETE CASCADE loss.
 */
val MIGRATION_196_197 =
    object : Migration(196, 197) {
        override fun migrate(db: SupportSQLiteDatabase) {
            requireTacticalSchema196197(db)
            requireTacticalDependents196197(db)
            requireForeignKeysClean196197(db)
            validateTacticalOwners196197(db)
            validateTacticalSources196197(db)

            check(formalContextFkCount196197(db) == 3) {
                "TacticalMission 196->197 unexpected initial Context FK count"
            }

            val originalMissionCount =
                scalar196197(db, "SELECT COUNT(*) FROM `tactical_missions`")
            val originalAttachmentCount =
                scalar196197(
                    db,
                    "SELECT COUNT(*) FROM `tactical_mission_attachment_cross_ref`",
                )
            val previousSequence = sequence196197(db)

            // The temporary table has no inbound/outbound FK constraints.
            db.execSQL(
                """
                CREATE TEMP TABLE `fk6_tactical_attachment_backup` AS
                SELECT `missionId`, `attachmentId`
                FROM `tactical_mission_attachment_cross_ref`
                """.trimIndent(),
            )

            check(
                scalar196197(
                    db,
                    "SELECT COUNT(*) FROM `fk6_tactical_attachment_backup`",
                ) == originalAttachmentCount,
            )

            // Remove the sole inbound FK child before dropping the parent.
            db.execSQL("DROP TABLE `tactical_mission_attachment_cross_ref`")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tactical_missions_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT,
                    `startTime` INTEGER,
                    `deadline` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `priority` TEXT NOT NULL,
                    `linkedProjectIds` TEXT,
                    `linkedAttachmentIds` TEXT,
                    `mission_order` INTEGER NOT NULL DEFAULT 0,
                    `week_key` TEXT NOT NULL DEFAULT '',
                    `iteration_id` TEXT,
                    `carried_from_mission_id` INTEGER,
                    `order_in_week` INTEGER NOT NULL DEFAULT 0,
                    `order_in_slot` INTEGER,
                    `mission_stream_id` TEXT,
                    `activity_slot_context_id` TEXT,
                    `source_type` TEXT NOT NULL DEFAULT 'MANUAL',
                    `source_context_id` TEXT,
                    `source_backlog_item_id` TEXT,
                    `source_arc_quest_id` TEXT,
                    `created_at` INTEGER NOT NULL DEFAULT 0,
                    `updated_at` INTEGER,
                    `synced_at` INTEGER,
                    `is_deleted` INTEGER NOT NULL DEFAULT 0,
                    `version` INTEGER NOT NULL DEFAULT 0,
                    `project_workspace_id` TEXT,
                    FOREIGN KEY(`project_workspace_id`)
                        REFERENCES `workspaces`(`id`)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent(),
            )

            val survivingColumns =
                TACTICAL_SURVIVING_COLUMNS_196197.joinToString(", ") { "`$it`" }

            val selectedColumns =
                TACTICAL_SURVIVING_COLUMNS_196197.joinToString(", ") { column ->
                    if (column == "project_workspace_id") {
                        "COALESCE(`project_workspace_id`, `projectId`)"
                    } else {
                        "`$column`"
                    }
                }

            db.execSQL(
                """
                INSERT INTO `tactical_missions_new` ($survivingColumns)
                SELECT $selectedColumns
                FROM `tactical_missions`
                """.trimIndent(),
            )

            check(
                scalar196197(
                    db,
                    "SELECT COUNT(*) FROM `tactical_missions_new`",
                ) == originalMissionCount,
            ) {
                "TacticalMission 196->197 changed mission count"
            }

            val nonOwnerColumns =
                TACTICAL_SURVIVING_COLUMNS_196197
                    .filterNot { it == "project_workspace_id" }
                    .joinToString(", ") { "`$it`" }

            requireSameRows196197(
                db,
                "SELECT $nonOwnerColumns FROM `tactical_missions`",
                "SELECT $nonOwnerColumns FROM `tactical_missions_new`",
                "TacticalMission specialized payload",
            )

            check(
                scalar196197(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM `tactical_missions` AS old
                    JOIN `tactical_missions_new` AS replacement
                        ON replacement.id = old.id
                    WHERE replacement.project_workspace_id
                        IS NOT COALESCE(
                            old.project_workspace_id,
                            old.projectId
                        )
                    """.trimIndent(),
                ) == 0L,
            ) {
                "TacticalMission 196->197 changed canonical ownership"
            }

            db.execSQL("DROP TABLE `tactical_missions`")
            db.execSQL(
                "ALTER TABLE `tactical_missions_new` RENAME TO `tactical_missions`",
            )

            // Preserve AUTOINCREMENT high-water mark even when high-id
            // historical missions were deleted before this migration.
            val currentMaxId =
                scalar196197(
                    db,
                    "SELECT COALESCE(MAX(id), 0) FROM `tactical_missions`",
                )
            val preservedSequence =
                maxOf(previousSequence ?: 0L, currentMaxId)

            db.execSQL(
                "DELETE FROM sqlite_sequence WHERE name = 'tactical_missions'",
            )
            db.execSQL(
                "INSERT INTO sqlite_sequence(name, seq) VALUES (?, ?)",
                arrayOf<Any?>("tactical_missions", preservedSequence),
            )

            val missionIndexes =
                listOf(
                    "index_tactical_missions_project_workspace_id" to
                        "`project_workspace_id`",
                    "index_tactical_missions_week_key" to "`week_key`",
                    "index_tactical_missions_iteration_id" to "`iteration_id`",
                    "index_tactical_missions_mission_stream_id" to
                        "`mission_stream_id`",
                    "index_tactical_missions_activity_slot_context_id" to
                        "`activity_slot_context_id`",
                    "index_tactical_missions_source_backlog_item_id_week_key" to
                        "`source_backlog_item_id`, `week_key`",
                )

            missionIndexes.forEach { (name, columns) ->
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `$name` " +
                        "ON `tactical_missions` ($columns)",
                )
            }

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS
                    `tactical_mission_attachment_cross_ref` (
                        `missionId` INTEGER NOT NULL,
                        `attachmentId` TEXT NOT NULL,
                        PRIMARY KEY(`missionId`, `attachmentId`),
                        FOREIGN KEY(`missionId`)
                            REFERENCES `tactical_missions`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`attachmentId`)
                            REFERENCES `attachments`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO `tactical_mission_attachment_cross_ref`
                    (`missionId`, `attachmentId`)
                SELECT `missionId`, `attachmentId`
                FROM `fk6_tactical_attachment_backup`
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                    `index_tactical_mission_attachment_cross_ref_missionId`
                ON `tactical_mission_attachment_cross_ref` (`missionId`)
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                    `index_tactical_mission_attachment_cross_ref_attachmentId`
                ON `tactical_mission_attachment_cross_ref` (`attachmentId`)
                """.trimIndent(),
            )

            requireSameRows196197(
                db,
                """
                SELECT `missionId`, `attachmentId`
                FROM `fk6_tactical_attachment_backup`
                """.trimIndent(),
                """
                SELECT `missionId`, `attachmentId`
                FROM `tactical_mission_attachment_cross_ref`
                """.trimIndent(),
                "TacticalMission attachment references",
            )

            check(
                scalar196197(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM `tactical_mission_attachment_cross_ref`
                    """.trimIndent(),
                ) == originalAttachmentCount,
            )

            db.execSQL("DROP TABLE `fk6_tactical_attachment_backup`")

            check(
                scalar196197(
                    db,
                    "SELECT COUNT(*) FROM `tactical_missions`",
                ) == originalMissionCount,
            )

            val columns = columns196197(db, "tactical_missions")
            check("projectId" !in columns)
            check("project_workspace_id" in columns)

            check(formalContextFkCount196197(db) == 2) {
                "TacticalMission 196->197 Context FK count is not 2"
            }

            check(sequence196197(db) == preservedSequence)

            requireForeignKeysClean196197(db)
            db.query("PRAGMA integrity_check").use { cursor ->
                check(cursor.moveToFirst() && cursor.getString(0) == "ok") {
                    "TacticalMission 196->197 integrity_check failed"
                }
            }
        }
    }

private val TACTICAL_SURVIVING_COLUMNS_196197 =
    listOf(
        "id", "title", "description", "startTime", "deadline",
        "status", "priority", "linkedProjectIds", "linkedAttachmentIds",
        "mission_order", "week_key", "iteration_id",
        "carried_from_mission_id", "order_in_week", "order_in_slot",
        "mission_stream_id", "activity_slot_context_id",
        "source_type", "source_context_id",
        "source_backlog_item_id", "source_arc_quest_id",
        "created_at", "updated_at", "synced_at", "is_deleted",
        "version", "project_workspace_id",
    )

private fun scalar196197(
    db: SupportSQLiteDatabase,
    sql: String,
): Long =
    db.query(sql).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getLong(0)
    }

private fun sequence196197(db: SupportSQLiteDatabase): Long? =
    db.query(
        """
        SELECT seq FROM sqlite_sequence
        WHERE name = 'tactical_missions'
        ORDER BY seq DESC LIMIT 1
        """.trimIndent(),
    ).use { cursor ->
        if (cursor.moveToFirst()) cursor.getLong(0) else null
    }

private fun columns196197(
    db: SupportSQLiteDatabase,
    table: String,
): Set<String> =
    db.query("PRAGMA table_info(`$table`)").use { cursor ->
        buildSet {
            val index = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                add(cursor.getString(index))
            }
        }
    }

private fun requireTacticalSchema196197(db: SupportSQLiteDatabase) {
    val expected =
        TACTICAL_SURVIVING_COLUMNS_196197.toSet() + "projectId"

    check(columns196197(db, "tactical_missions") == expected) {
        "TacticalMission 196->197 source schema differs from expected 196"
    }
}

private fun requireTacticalDependents196197(
    db: SupportSQLiteDatabase,
) {
    db.query(
        """
        SELECT name FROM sqlite_master
        WHERE type = 'trigger'
          AND tbl_name IN (
              'tactical_missions',
              'tactical_mission_attachment_cross_ref'
          )
        """.trimIndent(),
    ).use { cursor ->
        check(!cursor.moveToFirst()) {
            "TacticalMission 196->197 has persistent triggers"
        }
    }

    db.query(
        """
        SELECT name FROM sqlite_master
        WHERE type = 'view'
          AND lower(sql) LIKE '%tactical_missions%'
        """.trimIndent(),
    ).use { cursor ->
        check(!cursor.moveToFirst()) {
            "TacticalMission 196->197 has dependent SQL views"
        }
    }

    val inbound = mutableListOf<String>()

    db.query(
        """
        SELECT name FROM sqlite_master
        WHERE type = 'table' AND name NOT LIKE 'sqlite_%'
        """.trimIndent(),
    ).use { tables ->
        while (tables.moveToNext()) {
            val table = tables.getString(0)
            val escaped = table.replace("`", "``")

            db.query("PRAGMA foreign_key_list(`$escaped`)").use { fk ->
                val targetIndex = fk.getColumnIndexOrThrow("table")
                val fromIndex = fk.getColumnIndexOrThrow("from")
                val toIndex = fk.getColumnIndexOrThrow("to")
                val deleteIndex = fk.getColumnIndexOrThrow("on_delete")

                while (fk.moveToNext()) {
                    if (fk.getString(targetIndex) == "tactical_missions") {
                        inbound +=
                            "$table|${fk.getString(fromIndex)}|" +
                                "${fk.getString(toIndex)}|" +
                                fk.getString(deleteIndex)
                    }
                }
            }
        }
    }

    check(
        inbound == listOf(
            "tactical_mission_attachment_cross_ref|missionId|id|CASCADE",
        ),
    ) {
        "TacticalMission 196->197 unexpected inbound FKs: $inbound"
    }
}

private fun validateTacticalSources196197(
    db: SupportSQLiteDatabase,
) {
    val accepted =
        setOf(
            "MANUAL",
            "CONTEXT_BACKLOG_ITEM",
            "SLOT_BACKLOG_ITEM",
            "PREVIOUS_WEEK",
            "ARC_QUEST",
        )

    db.query(
        "SELECT DISTINCT source_type FROM tactical_missions",
    ).use { cursor ->
        while (cursor.moveToNext()) {
            check(!cursor.isNull(0) && cursor.getString(0) in accepted) {
                "TacticalMission 196->197 unsupported mission source type"
            }
        }
    }
}

private fun validateTacticalOwners196197(
    db: SupportSQLiteDatabase,
) {
    db.query(
        """
        SELECT mission.id,
               mission.projectId,
               mission.project_workspace_id,
               context.id,
               context.is_deleted,
               workspace.id,
               workspace.isDeleted,
               workspace.provenance,
               workspace.sourceContextId
        FROM tactical_missions AS mission
        LEFT JOIN contexts AS context
            ON context.id =
                COALESCE(
                    mission.project_workspace_id,
                    mission.projectId
                )
        LEFT JOIN workspaces AS workspace
            ON workspace.id =
                COALESCE(
                    mission.project_workspace_id,
                    mission.projectId
                )
        ORDER BY mission.id
        """.trimIndent(),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val legacy =
                if (cursor.isNull(1)) null else cursor.getString(1)
            val canonical =
                if (cursor.isNull(2)) null else cursor.getString(2)

            if (legacy == null && canonical == null) continue

            check(legacy == null || canonical == null || legacy == canonical) {
                "TacticalMission $id divergent project owners"
            }

            val owner = canonical ?: legacy
                ?: error("TacticalMission $id missing owner")

            check(owner.isNotBlank()) {
                "TacticalMission $id blank project owner"
            }

            check(!cursor.isNull(5) && cursor.getString(5) == owner) {
                "TacticalMission $id missing canonical Workspace"
            }

            check(cursor.getInt(6) == 0) {
                "TacticalMission $id deleted Workspace"
            }

            val provenance = cursor.getString(7)
            val source =
                if (cursor.isNull(8)) null else cursor.getString(8)

            val canonicalOnly =
                provenance == "CANONICAL_ONLY" && source == null
            val standalone =
                provenance == "STANDALONE" && source == null
            val contextBacked =
                provenance == "CONTEXT_BACKED" && source == owner

            val allowed =
                when {
                    owner in FROZEN_SYSTEM_TACTICAL_196 ->
                        canonicalOnly

                    !cursor.isNull(3) && cursor.getInt(4) == 0 ->
                        contextBacked

                    !cursor.isNull(3) ->
                        canonicalOnly

                    else ->
                        canonicalOnly || standalone || contextBacked
                }

            check(allowed) {
                "TacticalMission $id has unproven Workspace provenance"
            }
        }
    }
}

private val FROZEN_SYSTEM_TACTICAL_196 =
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

private fun requireSameRows196197(
    db: SupportSQLiteDatabase,
    left: String,
    right: String,
    label: String,
) {
    val lost =
        scalar196197(
            db,
            "SELECT COUNT(*) FROM ($left EXCEPT $right)",
        )
    val invented =
        scalar196197(
            db,
            "SELECT COUNT(*) FROM ($right EXCEPT $left)",
        )

    check(lost == 0L && invented == 0L) {
        "$label changed: lost=$lost invented=$invented"
    }
}

private fun requireForeignKeysClean196197(
    db: SupportSQLiteDatabase,
) {
    db.query("PRAGMA foreign_key_check").use { cursor ->
        check(!cursor.moveToFirst()) {
            "TacticalMission 196->197 foreign_key_check failed"
        }
    }
}

private fun formalContextFkCount196197(
    db: SupportSQLiteDatabase,
): Int {
    var count = 0

    db.query(
        """
        SELECT name FROM sqlite_master
        WHERE type = 'table' AND name NOT LIKE 'sqlite_%'
        """.trimIndent(),
    ).use { tables ->
        while (tables.moveToNext()) {
            val table = tables.getString(0).replace("`", "``")

            db.query("PRAGMA foreign_key_list(`$table`)").use { fk ->
                val targetIndex = fk.getColumnIndexOrThrow("table")

                while (fk.moveToNext()) {
                    if (fk.getString(targetIndex) == "contexts") {
                        count++
                    }
                }
            }
        }
    }

    return count
}
