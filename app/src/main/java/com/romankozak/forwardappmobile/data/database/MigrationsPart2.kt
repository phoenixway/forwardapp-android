@file:Suppress("LongMethod", "NestedBlockDepth")

package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_79_80 =
    object : Migration(79, 80) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE activity_records ADD COLUMN xp_gained INTEGER")
        }
    }

val MIGRATION_80_81 =
    object : Migration(80, 81) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE activity_records ADD COLUMN anty_xp INTEGER")
        }
    }

val MIGRATION_81_82 =
    object : Migration(81, 82) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `structure_presets` (
                    `id` TEXT NOT NULL,
                    `code` TEXT NOT NULL,
                    `label` TEXT NOT NULL,
                    `description` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_structure_presets_code` ON `structure_presets` (`code`)")
        }
    }

val MIGRATION_82_83 =
    object : Migration(82, 83) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `structure_preset_items` (
                    `id` TEXT NOT NULL,
                    `presetId` TEXT NOT NULL,
                    `entityType` TEXT NOT NULL,
                    `roleCode` TEXT NOT NULL,
                    `containerType` TEXT,
                    `title` TEXT NOT NULL,
                    `mandatory` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`presetId`) REFERENCES `structure_presets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_structure_preset_items_presetId` ON `structure_preset_items` (`presetId`)")
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `index_structure_preset_items_role_per_preset`
                ON `structure_preset_items` (`presetId`, `roleCode`)
                """.trimIndent(),
            )
        }
    }

val MIGRATION_83_84 =
    object : Migration(83, 84) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE attachments ADD COLUMN role_code TEXT")
            db.execSQL("ALTER TABLE attachments ADD COLUMN is_system INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE projects ADD COLUMN role_code TEXT")
        }
    }

val MIGRATION_84_85 =
    object : Migration(84, 85) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `project_structures` (
                    `id` TEXT NOT NULL,
                    `projectId` TEXT NOT NULL,
                    `base_preset_code` TEXT,
                    `apply_mode` TEXT NOT NULL DEFAULT 'ADDITIVE',
                    PRIMARY KEY(`id`),
                    UNIQUE(`projectId`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `project_structure_items` (
                    `id` TEXT NOT NULL,
                    `projectStructureId` TEXT NOT NULL,
                    `entityType` TEXT NOT NULL,
                    `roleCode` TEXT NOT NULL,
                    `containerType` TEXT,
                    `title` TEXT NOT NULL,
                    `mandatory` INTEGER NOT NULL DEFAULT 0,
                    `is_enabled` INTEGER NOT NULL DEFAULT 1,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`projectStructureId`) REFERENCES `project_structures`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_structures_projectId` ON `project_structures` (`projectId`)")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_project_structure_items_projectStructureId` ON `project_structure_items` (`projectStructureId`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_project_structure_items_role_per_structure` ON `project_structure_items` (`projectStructureId`, `roleCode`)",
            )
        }
    }

val MIGRATION_85_86 =
    object : Migration(85, 86) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS `index_project_structures_projectId`")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_project_structures_projectId` ON `project_structures` (`projectId`)")
        }
    }

val MIGRATION_86_87 =
    object : Migration(86, 87) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE structure_presets ADD COLUMN enable_inbox INTEGER")
            db.execSQL("ALTER TABLE structure_presets ADD COLUMN enable_log INTEGER")
            db.execSQL("ALTER TABLE structure_presets ADD COLUMN enable_artifact INTEGER")
            db.execSQL("ALTER TABLE structure_presets ADD COLUMN enable_advanced INTEGER")

            db.execSQL("ALTER TABLE project_structures ADD COLUMN enable_inbox INTEGER")
            db.execSQL("ALTER TABLE project_structures ADD COLUMN enable_log INTEGER")
            db.execSQL("ALTER TABLE project_structures ADD COLUMN enable_artifact INTEGER")
            db.execSQL("ALTER TABLE project_structures ADD COLUMN enable_advanced INTEGER")
        }
    }

val MIGRATION_87_88 =
    object : Migration(87, 88) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE structure_presets ADD COLUMN enable_dashboard INTEGER")
            db.execSQL("ALTER TABLE structure_presets ADD COLUMN enable_backlog INTEGER")
            db.execSQL("ALTER TABLE project_structures ADD COLUMN enable_dashboard INTEGER")
            db.execSQL("ALTER TABLE project_structures ADD COLUMN enable_backlog INTEGER")
        }
    }

val MIGRATION_88_89 =
    object : Migration(88, 89) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE structure_presets ADD COLUMN enable_attachments INTEGER")
            db.execSQL("ALTER TABLE project_structures ADD COLUMN enable_attachments INTEGER")
        }
    }

val MIGRATION_89_90 =
    object : Migration(89, 90) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `ai_events` (
                    `id` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `payload` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `life_system_state` (
                    `id` TEXT NOT NULL,
                    `loadLevel` TEXT NOT NULL,
                    `executionMode` TEXT NOT NULL,
                    `stability` TEXT NOT NULL,
                    `entropy` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }
    }

val MIGRATION_90_91 =
    object : Migration(90, 91) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `ai_insights` (
                    `id` TEXT NOT NULL,
                    `text` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `isRead` INTEGER NOT NULL,
                    `isFavorite` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }
    }

val MIGRATION_91_92 =
    object : Migration(91, 92) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE structure_presets ADD COLUMN enable_auto_link_subprojects INTEGER")
            db.execSQL(
                "ALTER TABLE project_structures ADD COLUMN enable_auto_link_subprojects INTEGER",
            )
            db.execSQL(
                "UPDATE project_structures SET enable_auto_link_subprojects = 1 WHERE enable_auto_link_subprojects IS NULL",
            )
        }
    }

val MIGRATION_92_93 =
    object : Migration(92, 93) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN linkedProjectIds TEXT")
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN linkedAttachmentIds TEXT")
        }
    }

val MIGRATION_93_94 =
    object : Migration(93, 94) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE projects RENAME TO contexts")
            db.execSQL("ALTER TABLE project_execution_logs RENAME TO context_execution_logs")
            db.execSQL("ALTER TABLE project_structures RENAME TO context_structures")
            db.execSQL("ALTER TABLE project_structure_items RENAME TO context_structure_items")
            db.execSQL("ALTER TABLE project_artifacts RENAME TO context_artifacts")

            db.execSQL("ALTER TABLE contexts RENAME COLUMN is_project_management_enabled TO is_context_management_enabled")
            db.execSQL("ALTER TABLE contexts RENAME COLUMN project_status TO context_status")
            db.execSQL("ALTER TABLE contexts RENAME COLUMN project_status_text TO context_status_text")
            db.execSQL("ALTER TABLE contexts RENAME COLUMN project_log_level TO context_log_level")
            db.execSQL("ALTER TABLE contexts RENAME COLUMN project_type TO context_type")

            db.execSQL("ALTER TABLE system_apps RENAME COLUMN project_id TO context_id")
            db.execSQL("ALTER TABLE list_items RENAME COLUMN project_id TO context_id")
        }
    }

val MIGRATION_94_95 =
    object : Migration(94, 95) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys=off;")
            db.beginTransaction()
            try {
                // 1. Define mappings from old string keys to new sys-prefixed IDs
                val keyToNewId =
                    mapOf(
                        "personal-management" to "sys_personal-management",
                        "strategic" to "sys_strategic",
                        "mission" to "sys_mission",
                        "long-term-strategy" to "sys_long-term-strategy",
                        "strategic-programs" to "sys_strategic-programs",
                        "medium-term-strategy" to "sys_medium-term-strategy",
                        "active-quests" to "sys_active-quests",
                        "week" to "sys_week",
                        "inbox" to "sys_inbox",
                        "strategic-inbox" to "sys_strategic-inbox",
                        "strategic-review" to "sys_strategic-review",
                        "main-beacons" to "sys_main-beacons",
                        "today" to "sys_today",
                    )

                // 2. Collect a map of old_id -> new_id for system contexts
                val oldIdToNewId = mutableMapOf<String, String>()
                val keysInClause = keyToNewId.keys.joinToString(",") { "'$it'" }
                db.query("SELECT id, system_key FROM contexts WHERE system_key IN ($keysInClause)").use { cursor ->
                    val idIndex = cursor.getColumnIndex("id")
                    val keyIndex = cursor.getColumnIndex("system_key")
                    while (cursor.moveToNext()) {
                        val oldId = cursor.getString(idIndex)
                        val systemKey = cursor.getString(keyIndex)
                        keyToNewId[systemKey]?.let { newId ->
                            oldIdToNewId[oldId] = newId
                        }
                    }
                }

                // 3. Define all tables and columns that reference contexts.id
                val tablesToUpdate =
                    mapOf(
                        "contexts" to "parentId", // Self-reference
                        "backlog_orders" to "list_id",
                        "context_execution_logs" to "contextId",
                        "inbox_records" to "projectId", // Legacy name from before contexts rename
                        "list_items" to "context_id",
                        "note_documents" to "projectId", // Legacy name
                        "checklists" to "projectId", // Legacy name
                        "scripts" to "projectId", // Legacy name
                        "context_artifacts" to "contextId",
                        "context_attachment_cross_ref" to "context_id",
                        "tactical_missions" to "projectId",
                        "system_apps" to "context_id",
                        "attachments" to "owner_context_id",
                    )

                // 4. Update all tables with the new IDs
                oldIdToNewId.forEach { (oldId, newId) ->
                    // First update all foreign key columns in child tables
                    tablesToUpdate.forEach { (table, column) ->
                        if (db.hasColumn(table, column)) {
                            db.execSQL("UPDATE $table SET $column = ? WHERE $column = ?", arrayOf(newId, oldId))
                        }
                    }

                    // Then update the primary key in the parent table
                    db.execSQL("UPDATE contexts SET id = ?, system_key = ? WHERE id = ?", arrayOf(newId, newId, oldId))

                    // Special handling for tactical_missions.linkedProjectIds (TEXT column with JSON array of strings)
                    if (db.hasColumn("tactical_missions", "linkedProjectIds")) {
                        db.query(
                            "SELECT id, linkedProjectIds FROM tactical_missions WHERE linkedProjectIds LIKE ?",
                            arrayOf("%$oldId%"),
                        ).use {
                                cursor ->
                            val idIndex = cursor.getColumnIndex("id")
                            val jsonIndex = cursor.getColumnIndex("linkedProjectIds")
                            while (cursor.moveToNext()) {
                                val missionId = cursor.getString(idIndex)
                                val jsonString = cursor.getString(jsonIndex)
                                if (!jsonString.isNullOrEmpty()) {
                                    val updatedJson = jsonString.replace(oldId, newId)
                                    db.execSQL(
                                        "UPDATE tactical_missions SET linkedProjectIds = ? WHERE id = ?",
                                        arrayOf(updatedJson, missionId),
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Drop old columns if they exist
                if (db.hasColumn("contexts", "project_type")) {
                    db.execSQL("ALTER TABLE contexts DROP COLUMN project_type")
                }
                if (db.hasColumn("contexts", "context_type")) {
                    db.execSQL("ALTER TABLE contexts DROP COLUMN context_type")
                }
                if (db.hasColumn("contexts", "reserved_group")) {
                    db.execSQL("ALTER TABLE contexts DROP COLUMN reserved_group")
                }

                // 6. Drop old indexes if they exist
                db.execSQL("DROP INDEX IF EXISTS index_contexts_project_type")
                db.execSQL("DROP INDEX IF EXISTS index_contexts_reserved_group")
                db.execSQL("DROP INDEX IF EXISTS idx_projects_systemkey_unique") // old index on system_key

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
                db.execSQL("PRAGMA foreign_keys=on;")
            }
        }
    }
val MIGRATION_95_96 =
    object : Migration(95, 96) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Створюємо нову таблицю (Точно за схемою з логу)
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `contexts_new` (
                    `id` TEXT NOT NULL, 
                    `name` TEXT NOT NULL, 
                    `description` TEXT, 
                    `parentId` TEXT, 
                    `createdAt` INTEGER NOT NULL, 
                    `updatedAt` INTEGER, 
                    `synced_at` INTEGER, 
                    `is_deleted` INTEGER NOT NULL DEFAULT 0, 
                    `version` INTEGER NOT NULL DEFAULT 0, 
                    `tags` TEXT, 
                    `relatedLinks` TEXT, 
                    `is_expanded` INTEGER NOT NULL DEFAULT 1, 
                    `goal_order` INTEGER NOT NULL DEFAULT 0, 
                    `is_attachments_expanded` INTEGER NOT NULL DEFAULT 0, 
                    `default_view_mode` TEXT, 
                    `is_completed` INTEGER NOT NULL DEFAULT 0, 
                    `is_context_management_enabled` INTEGER, 
                    `context_status` TEXT, 
                    `context_status_text` TEXT, 
                    `context_log_level` TEXT, 
                    `total_time_spent_minutes` INTEGER, 
                    `valueImportance` REAL NOT NULL DEFAULT 0.0, 
                    `valueImpact` REAL NOT NULL DEFAULT 0.0, 
                    `effort` REAL NOT NULL DEFAULT 0.0, 
                    `cost` REAL NOT NULL DEFAULT 0.0, 
                    `risk` REAL NOT NULL DEFAULT 0.0, 
                    `weightEffort` REAL NOT NULL DEFAULT 1.0, 
                    `weightCost` REAL NOT NULL DEFAULT 1.0, 
                    `weightRisk` REAL NOT NULL DEFAULT 1.0, 
                    `rawScore` REAL NOT NULL DEFAULT 0.0, 
                    `displayScore` INTEGER NOT NULL DEFAULT 0, 
                    `scoring_status` TEXT NOT NULL, 
                    `show_checkboxes` INTEGER NOT NULL DEFAULT 0, 
                    `role_code` TEXT, 
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )

            // 2. Копіюємо дані
            db.execSQL(
                """
                INSERT INTO `contexts_new` (
                    id, name, description, parentId, createdAt, updatedAt, synced_at, is_deleted, version, 
                    tags, relatedLinks, is_expanded, goal_order, is_attachments_expanded, default_view_mode, 
                    is_completed, is_context_management_enabled, context_status, context_status_text, 
                    context_log_level, total_time_spent_minutes, valueImportance, valueImpact, effort, 
                    cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore, 
                    scoring_status, show_checkboxes, role_code
                )
                SELECT 
                    id, name, description, parentId, createdAt, updatedAt, synced_at, is_deleted, version, 
                    tags, relatedLinks, is_expanded, goal_order, is_attachments_expanded, default_view_mode, 
                    is_completed, is_context_management_enabled, context_status, context_status_text, 
                    context_log_level, total_time_spent_minutes, 
                    COALESCE(valueImportance, 0.0), COALESCE(valueImpact, 0.0), COALESCE(effort, 0.0), 
                    COALESCE(cost, 0.0), COALESCE(risk, 0.0), COALESCE(weightEffort, 1.0), 
                    COALESCE(weightCost, 1.0), COALESCE(weightRisk, 1.0), COALESCE(rawScore, 0.0), 
                    COALESCE(displayScore, 0), 
                    COALESCE(scoring_status, 'NOT_ASSESSED'), 
                    show_checkboxes, role_code
                FROM `contexts`
                """.trimIndent(),
            )

            // 3. Перейменування
            db.execSQL("DROP TABLE `contexts`")
            db.execSQL("ALTER TABLE `contexts_new` RENAME TO `contexts`")
        }
    }

val MIGRATION_100_101 =
    object : Migration(100, 101) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `direction_items` (
                    `id` TEXT NOT NULL,
                    `contextId` TEXT NOT NULL,
                    `text` TEXT NOT NULL,
                    `itemOrder` INTEGER NOT NULL,
                    `updatedAt` INTEGER,
                    `synced_at` INTEGER,
                    `is_deleted` INTEGER NOT NULL DEFAULT 0,
                    `version` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`contextId`) REFERENCES `contexts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_direction_items_contextId ON direction_items(contextId)",
            )
        }
    }

val MIGRATION_101_102 =
    object : Migration(101, 102) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `direction_items` ADD COLUMN `linked_context_id` TEXT",
            )
        }
    }

val MIGRATION_102_103 =
    object : Migration(102, 103) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `day_tasks` ADD COLUMN `linkedProjectIds` TEXT")
            db.execSQL("ALTER TABLE `day_tasks` ADD COLUMN `linkedAttachmentIds` TEXT")
        }
    }

val MIGRATION_103_104 =
    object : Migration(103, 104) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `day_plans` ADD COLUMN `linkedProjectIds` TEXT")
            db.execSQL("ALTER TABLE `day_plans` ADD COLUMN `linkedAttachmentIds` TEXT")
        }
    }

val MIGRATION_104_105 =
    object : Migration(104, 105) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `tactical_missions` ADD COLUMN `mission_order` INTEGER NOT NULL DEFAULT 0")
        }
    }

val MIGRATION_105_106 =
    object : Migration(105, 106) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `user_state_intervals` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `stateType` TEXT NOT NULL,
                    `crisisLevel` INTEGER,
                    `label` TEXT,
                    `source` TEXT NOT NULL,
                    `createdFromActivityId` TEXT,
                    `startedAt` INTEGER NOT NULL,
                    `endedAt` INTEGER
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_state_intervals_endedAt` ON `user_state_intervals` (`endedAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_state_intervals_startedAt` ON `user_state_intervals` (`startedAt`)")

            db.execSQL("ALTER TABLE `activity_records` ADD COLUMN `rawNoteText` TEXT")
            db.execSQL("ALTER TABLE `activity_records` ADD COLUMN `noteText` TEXT")
            db.execSQL("ALTER TABLE `activity_records` ADD COLUMN `stateEventType` TEXT")
            db.execSQL("ALTER TABLE `activity_records` ADD COLUMN `stateEventCrisisLevel` INTEGER")
            db.execSQL("ALTER TABLE `activity_records` ADD COLUMN `stateEventLabel` TEXT")
            db.execSQL("ALTER TABLE `activity_records` ADD COLUMN `stateEventApplied` INTEGER NOT NULL DEFAULT 0")

            db.execSQL("UPDATE `activity_records` SET `rawNoteText` = `text` WHERE `rawNoteText` IS NULL")
            db.execSQL("UPDATE `activity_records` SET `noteText` = `text` WHERE `noteText` IS NULL")
        }
    }

val MIGRATION_106_107 =
    object : Migration(106, 107) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `context_key_problems` (
                    `context_id` TEXT NOT NULL,
                    `payload_json` TEXT NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    PRIMARY KEY(`context_id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_context_key_problems_updated_at` ON `context_key_problems` (`updated_at`)")
        }
    }

val MIGRATION_107_108 =
    object : Migration(107, 108) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `focus_context_intervals` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `contextId` TEXT NOT NULL,
                    `scope` TEXT NOT NULL,
                    `priority` INTEGER,
                    `source` TEXT NOT NULL,
                    `createdFromActivityId` TEXT,
                    `startedAt` INTEGER NOT NULL,
                    `endedAt` INTEGER
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_context_intervals_contextId` ON `focus_context_intervals` (`contextId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_context_intervals_scope` ON `focus_context_intervals` (`scope`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_context_intervals_endedAt` ON `focus_context_intervals` (`endedAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_context_intervals_startedAt` ON `focus_context_intervals` (`startedAt`)")
        }
    }

val MIGRATION_108_109 =
    object : Migration(108, 109) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `context_inbox_sorting` (
                    `context_id` TEXT NOT NULL,
                    `rules_text` TEXT NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    PRIMARY KEY(`context_id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_context_inbox_sorting_updated_at` ON `context_inbox_sorting` (`updated_at`)")
        }
    }

val MIGRATION_109_110 =
    object : Migration(109, 110) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `note_documents` RENAME TO `note_documents_old`")
            db.execSQL("DROP INDEX IF EXISTS `index_note_documents_contextId`")
            db.execSQL("DROP INDEX IF EXISTS `index_note_documents_projectId`")
            db.execSQL(
                """
                CREATE TABLE `note_documents` (
                    `id` TEXT NOT NULL,
                    `contextId` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `content` TEXT,
                    `lastCursorPosition` INTEGER NOT NULL DEFAULT 0,
                    `syncedAt` INTEGER,
                    `isDeleted` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX `index_note_documents_contextId` ON `note_documents` (`contextId`)")
            val oldNoteContextColumn =
                db.query("PRAGMA table_info(`note_documents_old`)").use { cursor ->
                    var hasContextId = false
                    var hasProjectId = false
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        when (cursor.getString(nameIndex)) {
                            "contextId" -> hasContextId = true
                            "projectId" -> hasProjectId = true
                        }
                    }
                    when {
                        hasContextId -> "contextId"
                        hasProjectId -> "projectId"
                        else -> "contextId"
                    }
                }
            db.execSQL(
                """
                INSERT INTO `note_documents`
                (`id`, `contextId`, `name`, `createdAt`, `updatedAt`, `content`, `lastCursorPosition`, `syncedAt`, `isDeleted`, `version`)
                SELECT
                `id`, `$oldNoteContextColumn`, `name`, `createdAt`, `updatedAt`, `content`, `lastCursorPosition`, `syncedAt`, `isDeleted`, `version`
                FROM `note_documents_old`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `note_documents_old`")

            db.execSQL("ALTER TABLE `checklists` RENAME TO `checklists_old`")
            db.execSQL("DROP INDEX IF EXISTS `index_checklists_contextId`")
            db.execSQL("DROP INDEX IF EXISTS `index_checklists_projectId`")
            db.execSQL(
                """
                CREATE TABLE `checklists` (
                    `id` TEXT NOT NULL,
                    `contextId` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `isDeleted` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX `index_checklists_contextId` ON `checklists` (`contextId`)")
            val oldChecklistContextColumn =
                db.query("PRAGMA table_info(`checklists_old`)").use { cursor ->
                    var hasContextId = false
                    var hasProjectId = false
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        when (cursor.getString(nameIndex)) {
                            "contextId" -> hasContextId = true
                            "projectId" -> hasProjectId = true
                        }
                    }
                    when {
                        hasContextId -> "contextId"
                        hasProjectId -> "projectId"
                        else -> "contextId"
                    }
                }
            db.execSQL(
                """
                INSERT INTO `checklists`
                (`id`, `contextId`, `name`, `createdAt`, `updatedAt`, `syncedAt`, `isDeleted`, `version`)
                SELECT
                `id`, `$oldChecklistContextColumn`, `name`, `createdAt`, `updatedAt`, `syncedAt`, `isDeleted`, `version`
                FROM `checklists_old`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `checklists_old`")
        }
    }

val MIGRATION_110_111 =
    object : Migration(110, 111) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `music_notes` (
                    `id` TEXT NOT NULL,
                    `contextId` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `content` TEXT NOT NULL,
                    `syncedAt` INTEGER,
                    `isDeleted` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_music_notes_contextId` ON `music_notes` (`contextId`)")
        }
    }

val MIGRATION_111_112 =
    object : Migration(111, 112) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                UPDATE `context_structures`
                SET
                    `enable_inbox` = 0,
                    `enable_log` = 0,
                    `enable_backlog` = 0,
                    `enable_attachments` = 0,
                    `enable_advanced` = 0,
                    `enable_artifact` = 0,
                    `enable_dashboard` = 1,
                    `updatedAt` = CAST(strftime('%s','now') AS INTEGER) * 1000
                WHERE lower(trim(`base_preset_code`)) = 'default'
                  AND `contextId` IN (
                        SELECT `id`
                        FROM `contexts`
                        WHERE `role_code` IS NULL OR trim(`role_code`) = ''
                  )
                """.trimIndent(),
            )
        }
    }

val MIGRATION_112_113 =
    object : Migration(112, 113) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `goals` ADD COLUMN `relativeSize` INTEGER NOT NULL DEFAULT 0")
        }
    }

val MIGRATION_113_114 =
    object : Migration(113, 114) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `goals` ADD COLUMN `goal_status` TEXT NOT NULL DEFAULT 'ACTIVE'")
            db.execSQL("UPDATE `goals` SET `goal_status` = 'DONE' WHERE `completed` = 1")
        }
    }

val MIGRATION_114_115 =
    object : Migration(114, 115) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val idMappings =
                listOf(
                    "sys_session-improve" to "sys_mode-improve",
                    "sys_session-execution" to "sys_mode-execution",
                    "sys_session-control" to "sys_mode-control",
                    "sys_session-recovery" to "sys_mode-recovery",
                    "sys_session-emergency" to "sys_mode-emergency",
                )

            db.execSQL("PRAGMA foreign_keys=OFF")
            db.beginTransaction()
            try {
                idMappings.forEach { (oldId, newId) ->
                    db.execSQL("UPDATE `contexts` SET `parentId` = '$newId' WHERE `parentId` = '$oldId'")
                    db.execSQL("UPDATE `context_execution_logs` SET `contextId` = '$newId' WHERE `contextId` = '$oldId'")
                    db.execSQL("UPDATE `inbox_records` SET `contextId` = '$newId' WHERE `contextId` = '$oldId'")
                    db.execSQL("UPDATE `list_items` SET `context_id` = '$newId' WHERE `context_id` = '$oldId'")
                    db.execSQL("UPDATE `activity_records` SET `context_id` = '$newId' WHERE `context_id` = '$oldId'")
                    db.execSQL("UPDATE `notes` SET `contextId` = '$newId' WHERE `contextId` = '$oldId'")
                    db.execSQL("UPDATE `note_documents` SET `contextId` = '$newId' WHERE `contextId` = '$oldId'")
                    db.execSQL("UPDATE `music_notes` SET `contextId` = '$newId' WHERE `contextId` = '$oldId'")
                    db.execSQL("UPDATE `checklists` SET `contextId` = '$newId' WHERE `contextId` = '$oldId'")
                    db.execSQL("UPDATE `context_inbox_sorting` SET `context_id` = '$newId' WHERE `context_id` = '$oldId'")
                    db.execSQL("UPDATE `context_key_problems` SET `context_id` = '$newId' WHERE `context_id` = '$oldId'")
                    db.execSQL("UPDATE `context_structures` SET `contextId` = '$newId' WHERE `contextId` = '$oldId'")
                    db.execSQL("UPDATE `context_artifacts` SET `contextId` = '$newId' WHERE `contextId` = '$oldId'")
                    db.execSQL("UPDATE `focus_context_intervals` SET `contextId` = '$newId' WHERE `contextId` = '$oldId'")
                    db.execSQL("UPDATE `direction_items` SET `contextId` = '$newId' WHERE `contextId` = '$oldId'")
                    db.execSQL("UPDATE `system_apps` SET `context_id` = '$newId' WHERE `context_id` = '$oldId'")
                    db.execSQL("UPDATE `scripts` SET `contextId` = '$newId' WHERE `contextId` = '$oldId'")
                    db.execSQL("UPDATE `attachments` SET `owner_context_id` = '$newId' WHERE `owner_context_id` = '$oldId'")
                    db.execSQL("UPDATE `context_attachment_cross_ref` SET `context_id` = '$newId' WHERE `context_id` = '$oldId'")
                    db.execSQL("UPDATE `recent_items` SET `target` = '$newId' WHERE `target` = '$oldId'")
                    db.execSQL("UPDATE `context_structures` SET `id` = 'default_config_$newId' WHERE `id` = 'default_config_$oldId'")
                    db.execSQL(
                        "UPDATE `context_structure_items` SET `contextStructureId` = 'default_config_$newId' WHERE `contextStructureId` = 'default_config_$oldId'",
                    )
                    db.execSQL("UPDATE `contexts` SET `id` = '$newId' WHERE `id` = '$oldId'")
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
                db.execSQL("PRAGMA foreign_keys=ON")
            }
        }
    }

val MIGRATION_115_116 =
    object : Migration(115, 116) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `life_management_level_statuses` (
                    `levelId` TEXT NOT NULL,
                    `generalStatus` TEXT NOT NULL,
                    `transferStatus` TEXT NOT NULL,
                    `freshnessStatus` TEXT NOT NULL,
                    `blockerText` TEXT,
                    `nextActionText` TEXT,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`levelId`)
                )
                """.trimIndent(),
            )
        }
    }

val MIGRATION_116_117 =
    object : Migration(116, 117) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `main_beacons` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT,
                    `why_it_matters` TEXT,
                    `success_shape` TEXT,
                    `failure_shape` TEXT,
                    `anti_goal` TEXT,
                    `decision_impact` TEXT,
                    `readiness_status` TEXT NOT NULL,
                    `blocker_text` TEXT,
                    `next_action_text` TEXT,
                    `updatedAt` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_main_beacons_readiness_status` ON `main_beacons` (`readiness_status`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `main_beacon_context_cross_ref` (
                    `beacon_id` TEXT NOT NULL,
                    `context_id` TEXT NOT NULL,
                    PRIMARY KEY(`beacon_id`, `context_id`),
                    FOREIGN KEY(`beacon_id`) REFERENCES `main_beacons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`context_id`) REFERENCES `contexts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_main_beacon_context_cross_ref_context_id` ON `main_beacon_context_cross_ref` (`context_id`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `main_beacon_attachment_cross_ref` (
                    `beacon_id` TEXT NOT NULL,
                    `attachment_id` TEXT NOT NULL,
                    PRIMARY KEY(`beacon_id`, `attachment_id`),
                    FOREIGN KEY(`beacon_id`) REFERENCES `main_beacons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`attachment_id`) REFERENCES `attachments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_main_beacon_attachment_cross_ref_attachment_id` ON `main_beacon_attachment_cross_ref` (`attachment_id`)",
            )
        }
    }

val MIGRATION_117_118 =
    object : Migration(117, 118) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `main_beacon_level_statuses` (
                    `id` TEXT NOT NULL,
                    `main_beacon_id` TEXT NOT NULL,
                    `level_type` TEXT NOT NULL,
                    `general_status` TEXT NOT NULL,
                    `sync_status` TEXT NOT NULL,
                    `blocker_text` TEXT,
                    `next_action_text` TEXT,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`main_beacon_id`) REFERENCES `main_beacons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_main_beacon_level_statuses_main_beacon_id` ON `main_beacon_level_statuses` (`main_beacon_id`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_main_beacon_level_statuses_main_beacon_id_level_type` ON `main_beacon_level_statuses` (`main_beacon_id`, `level_type`)",
            )

            val now = System.currentTimeMillis()
            val levels =
                listOf(
                    "MAIN_BEACON",
                    "REALIZATION_MODEL_OF_MAIN_BEACON",
                    "MANDATORY_CORE_OF_MAIN_BEACON",
                    "STRATEGIC_PROJECTING_OF_MAIN_BEACON",
                    "LONG_TERM_STRATEGY",
                    "MEDIUM_TERM_PROGRAM",
                    "WEEK",
                    "DAY",
                )
            val cursor = db.query("SELECT id FROM main_beacons")
            if (cursor.moveToFirst()) {
                do {
                    val beaconId = cursor.getString(0)
                    levels.forEachIndexed { index, level ->
                        db.execSQL(
                            """
                            INSERT INTO `main_beacon_level_statuses`
                            (`id`, `main_beacon_id`, `level_type`, `general_status`, `sync_status`, `blocker_text`, `next_action_text`, `updatedAt`)
                            VALUES (?, ?, ?, 'CONDITIONAL', 'IN_SYNC', NULL, NULL, ?)
                            """.trimIndent(),
                            arrayOf("${beaconId}_$index", beaconId, level, now),
                        )
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
    }

val MIGRATION_118_119 =
    object : Migration(118, 119) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE main_beacons ADD COLUMN beacon_order INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                """
                UPDATE main_beacons
                SET beacon_order = (
                    SELECT COUNT(*)
                    FROM main_beacons AS ordered
                    WHERE ordered.createdAt < main_beacons.createdAt
                        OR (ordered.createdAt = main_beacons.createdAt AND ordered.id <= main_beacons.id)
                ) - 1
                """.trimIndent(),
            )
        }
    }

val MIGRATION_119_120 =
    object : Migration(119, 120) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE list_items ADD COLUMN association_owner_context_id TEXT")
            db.execSQL("ALTER TABLE list_items ADD COLUMN association_tag TEXT")
            db.execSQL(
                """
                DELETE FROM list_items
                WHERE rowid NOT IN (
                    SELECT MIN(rowid)
                    FROM list_items
                    GROUP BY context_id, itemType, entityId, is_deleted
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `index_list_items_context_id_itemType_entityId_is_deleted`
                ON `list_items` (`context_id`, `itemType`, `entityId`, `is_deleted`)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_list_items_entityId_itemType_association_owner_context_id`
                ON `list_items` (`entityId`, `itemType`, `association_owner_context_id`)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `context_tag_refs` (
                    `context_id` TEXT NOT NULL,
                    `normalized_tag` TEXT NOT NULL,
                    PRIMARY KEY(`context_id`, `normalized_tag`),
                    FOREIGN KEY(`context_id`) REFERENCES `contexts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_context_tag_refs_normalized_tag` ON `context_tag_refs` (`normalized_tag`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `inbox_record_links` (
                    `record_id` TEXT NOT NULL,
                    `context_id` TEXT NOT NULL,
                    `owner_context_id` TEXT NOT NULL,
                    `association_tag` TEXT,
                    `linked_at` INTEGER NOT NULL,
                    PRIMARY KEY(`record_id`, `context_id`),
                    FOREIGN KEY(`record_id`) REFERENCES `inbox_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`context_id`) REFERENCES `contexts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_inbox_record_links_context_id` ON `inbox_record_links` (`context_id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_inbox_record_links_record_id` ON `inbox_record_links` (`record_id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_inbox_record_links_owner_context_id_record_id` ON `inbox_record_links` (`owner_context_id`, `record_id`)",
            )
        }
    }

val MIGRATION_120_121 =
    object : Migration(120, 121) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE context_structures ADD COLUMN remove_inbox_entry_after_tag_autocopy INTEGER",
            )
            db.execSQL(
                "ALTER TABLE inbox_records ADD COLUMN hide_in_owner_inbox INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

val MIGRATION_121_122 =
    object : Migration(121, 122) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE day_tasks
                ADD COLUMN executionStrictness TEXT NOT NULL DEFAULT 'NORMAL'
                """.trimIndent(),
            )
        }
    }

val MIGRATION_122_123 =
    object : Migration(122, 123) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `day_focus_items` (
                    `id` TEXT NOT NULL,
                    `dayPlanId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `notes` TEXT,
                    `type` TEXT NOT NULL,
                    `order` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER,
                    `syncedAt` INTEGER,
                    `isDeleted` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`dayPlanId`) REFERENCES `day_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_day_focus_items_dayPlanId` ON `day_focus_items` (`dayPlanId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_day_focus_items_dayPlanId_order` ON `day_focus_items` (`dayPlanId`, `order`)",
            )
        }
    }

val MIGRATION_123_124 =
    object : Migration(123, 124) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE day_focus_items
                ADD COLUMN isEveryday INTEGER NOT NULL DEFAULT 0
                """.trimIndent(),
            )
            db.execSQL(
                """
                ALTER TABLE day_focus_items
                ADD COLUMN recurringKey TEXT
                """.trimIndent(),
            )
        }
    }

val MIGRATION_124_125 =
    object : Migration(124, 125) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE day_focus_items
                ADD COLUMN relatedLinks TEXT
                """.trimIndent(),
            )
        }
    }

val MIGRATION_125_126 =
    object : Migration(125, 126) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE context_structures ADD COLUMN remove_backlog_entry_after_tag_autocopy INTEGER",
            )
        }
    }

val MIGRATION_126_127 =
    object : Migration(126, 127) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `context_parent_links` (
                    `parent_context_id` TEXT NOT NULL,
                    `child_context_id` TEXT NOT NULL,
                    `link_order` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER,
                    `synced_at` INTEGER,
                    `is_deleted` INTEGER NOT NULL DEFAULT 0,
                    `version` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`parent_context_id`, `child_context_id`),
                    FOREIGN KEY(`parent_context_id`) REFERENCES `contexts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`child_context_id`) REFERENCES `contexts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_context_parent_links_parent_context_id_link_order`
                ON `context_parent_links` (`parent_context_id`, `link_order`)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_context_parent_links_child_context_id`
                ON `context_parent_links` (`child_context_id`)
                """.trimIndent(),
            )
        }
    }

val MIGRATION_127_128 =
    object : Migration(127, 128) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `main_beacon_groups` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT,
                    `group_order` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_main_beacon_groups_group_order`
                ON `main_beacon_groups` (`group_order`)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `main_beacon_group_members` (
                    `group_id` TEXT NOT NULL,
                    `beacon_id` TEXT NOT NULL,
                    `member_order` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`group_id`, `beacon_id`),
                    FOREIGN KEY(`group_id`) REFERENCES `main_beacon_groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`beacon_id`) REFERENCES `main_beacons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_main_beacon_group_members_beacon_id`
                ON `main_beacon_group_members` (`beacon_id`)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_main_beacon_group_members_group_id_member_order`
                ON `main_beacon_group_members` (`group_id`, `member_order`)
                """.trimIndent(),
            )
        }
    }

val MIGRATION_128_129 =
    object : Migration(128, 129) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE main_beacons ADD COLUMN parent_beacon_id TEXT")
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_main_beacons_parent_beacon_id`
                ON `main_beacons` (`parent_beacon_id`)
                """.trimIndent(),
            )
        }
    }

val MIGRATION_129_130 =
    object : Migration(129, 130) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE recurring_tasks ADD COLUMN linkedProjectIds TEXT")
            db.execSQL("ALTER TABLE recurring_tasks ADD COLUMN linkedAttachmentIds TEXT")
        }
    }

val MIGRATION_130_131 =
    object : Migration(130, 131) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `main_beacon_parent_links` (
                    `parent_beacon_id` TEXT NOT NULL,
                    `child_beacon_id` TEXT NOT NULL,
                    `link_order` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`parent_beacon_id`, `child_beacon_id`),
                    FOREIGN KEY(`parent_beacon_id`) REFERENCES `main_beacons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`child_beacon_id`) REFERENCES `main_beacons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_main_beacon_parent_links_child_beacon_id`
                ON `main_beacon_parent_links` (`child_beacon_id`)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_main_beacon_parent_links_parent_beacon_id_link_order`
                ON `main_beacon_parent_links` (`parent_beacon_id`, `link_order`)
                """.trimIndent(),
            )
        }
    }

val MIGRATION_131_132 =
    object : Migration(131, 132) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE main_beacon_context_cross_ref
                ADD COLUMN ref_order INTEGER NOT NULL DEFAULT 0
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_main_beacon_context_cross_ref_beacon_id_ref_order`
                ON `main_beacon_context_cross_ref` (`beacon_id`, `ref_order`)
                """.trimIndent(),
            )
        }
    }

val MIGRATION_132_133 =
    object : Migration(132, 133) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE activity_records
                ADD COLUMN record_kind TEXT NOT NULL DEFAULT 'TIMED_ACTIVITY'
                """.trimIndent(),
            )
            db.execSQL(
                """
                UPDATE activity_records
                SET record_kind = CASE
                    WHEN startTime IS NULL AND endTime IS NULL THEN 'COMMENT'
                    WHEN startTime IS NOT NULL AND endTime IS NOT NULL AND startTime = endTime THEN 'EVENT'
                    ELSE 'TIMED_ACTIVITY'
                END
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_activity_records_record_kind`
                ON `activity_records` (`record_kind`)
                """.trimIndent(),
            )
        }
    }

val MIGRATION_133_134 =
    object : Migration(133, 134) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `arc_quests` (
                    `id` TEXT NOT NULL,
                    `arc_key` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT,
                    `linked_context_id` TEXT,
                    `linked_mission_id` INTEGER,
                    `source_type` TEXT NOT NULL DEFAULT 'MANUAL',
                    `source_id` TEXT,
                    `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                    `quest_order` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER,
                    `synced_at` INTEGER,
                    `is_deleted` INTEGER NOT NULL DEFAULT 0,
                    `version` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_arc_quests_arc_key_quest_order`
                ON `arc_quests` (`arc_key`, `quest_order`)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_arc_quests_linked_context_id`
                ON `arc_quests` (`linked_context_id`)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_arc_quests_linked_mission_id`
                ON `arc_quests` (`linked_mission_id`)
                """.trimIndent(),
            )
        }
    }

val MIGRATION_134_135 =
    object : Migration(134, 135) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN week_key TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN order_in_week INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN order_in_slot INTEGER")
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN activity_slot_context_id TEXT")
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN source_type TEXT NOT NULL DEFAULT 'MANUAL'")
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN source_context_id TEXT")
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN source_backlog_item_id TEXT")
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN source_arc_quest_id TEXT")
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN updated_at INTEGER")
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN synced_at INTEGER")
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN version INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE tactical_missions SET order_in_week = mission_order WHERE order_in_week = 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tactical_missions_week_key ON tactical_missions(week_key)")
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_tactical_missions_activity_slot_context_id
                ON tactical_missions(activity_slot_context_id)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_tactical_missions_source_backlog_item_id_week_key
                ON tactical_missions(source_backlog_item_id, week_key)
                """.trimIndent(),
            )
        }
    }

val MIGRATION_135_136 =
    object : Migration(135, 136) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS tactical_activity_slots (
                    id TEXT NOT NULL,
                    context_id TEXT NOT NULL,
                    slot_order INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER,
                    synced_at INTEGER,
                    is_deleted INTEGER NOT NULL DEFAULT 0,
                    version INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(id),
                    FOREIGN KEY(context_id) REFERENCES contexts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_tactical_activity_slots_context_id
                ON tactical_activity_slots(context_id)
                """.trimIndent(),
            )
        }
    }

val MIGRATION_136_137 =
    object : Migration(136, 137) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN mission_stream_id TEXT")
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_tactical_missions_mission_stream_id
                ON tactical_missions(mission_stream_id)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS mission_streams (
                    id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT,
                    color_key TEXT,
                    icon_key TEXT,
                    stream_order INTEGER NOT NULL DEFAULT 0,
                    is_default INTEGER NOT NULL DEFAULT 0,
                    is_archived INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER,
                    synced_at INTEGER,
                    is_deleted INTEGER NOT NULL DEFAULT 0,
                    version INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_mission_streams_stream_order
                ON mission_streams(stream_order)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_mission_streams_is_default
                ON mission_streams(is_default)
                """.trimIndent(),
            )
            val now = System.currentTimeMillis()
            db.execSQL(
                """
                INSERT OR IGNORE INTO mission_streams (
                    id, title, description, color_key, icon_key, stream_order,
                    is_default, is_archived, created_at, updated_at, synced_at,
                    is_deleted, version
                ) VALUES (
                    'general', 'General', NULL, NULL, NULL, -9223372036854775808,
                    1, 0, $now, $now, NULL, 0, 1
                )
                """.trimIndent(),
            )
        }
    }

val MIGRATION_137_138 =
    object : Migration(137, 138) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE day_focus_items ADD COLUMN budgetPercent INTEGER")
        }
    }

val MIGRATION_138_139 =
    object : Migration(138, 139) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE day_plans ADD COLUMN predictedDurationMinutes INTEGER")
        }
    }

val MIGRATION_139_140 =
    object : Migration(139, 140) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE mission_streams ADD COLUMN budget_percent INTEGER")
        }
    }

val MIGRATION_140_141 =
    object : Migration(140, 141) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS tactical_iterations (
                    id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    started_at INTEGER NOT NULL,
                    planned_end_at INTEGER,
                    closed_at INTEGER,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    iteration_type TEXT NOT NULL DEFAULT 'TIMEBOXED',
                    week_key TEXT,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER,
                    synced_at INTEGER,
                    is_deleted INTEGER NOT NULL DEFAULT 0,
                    version INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tactical_iterations_status ON tactical_iterations(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tactical_iterations_week_key ON tactical_iterations(week_key)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tactical_iterations_started_at ON tactical_iterations(started_at)")
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN iteration_id TEXT")
            db.execSQL("ALTER TABLE tactical_missions ADD COLUMN carried_from_mission_id INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tactical_missions_iteration_id ON tactical_missions(iteration_id)")
            val now = System.currentTimeMillis()
            db.execSQL(
                """
                INSERT OR IGNORE INTO tactical_iterations (
                    id, title, started_at, planned_end_at, closed_at, status,
                    iteration_type, week_key, created_at, updated_at, synced_at,
                    is_deleted, version
                )
                SELECT
                    week_key,
                    week_key,
                    0,
                    NULL,
                    $now,
                    'CLOSED',
                    'TIMEBOXED',
                    week_key,
                    $now,
                    $now,
                    NULL,
                    0,
                    1
                FROM tactical_missions
                WHERE week_key IS NOT NULL AND week_key != ''
                GROUP BY week_key
                """.trimIndent(),
            )
            db.execSQL(
                """
                UPDATE tactical_missions
                SET iteration_id = week_key
                WHERE week_key IS NOT NULL AND week_key != ''
                """.trimIndent(),
            )
        }
    }

val MIGRATION_141_142 =
    object : Migration(141, 142) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE main_beacons ADD COLUMN is_expanded INTEGER NOT NULL DEFAULT 1")
        }
    }

val MIGRATION_142_143 =
    object : Migration(142, 143) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE day_tasks ADD COLUMN recurrenceSeriesId TEXT")
            db.execSQL("ALTER TABLE day_tasks ADD COLUMN recurrenceOccurrenceDayKey TEXT")
            db.execSQL("ALTER TABLE day_tasks ADD COLUMN recurrenceSourceSeriesVersion INTEGER")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_day_tasks_recurrenceSeriesId_recurrenceOccurrenceDayKey " +
                    "ON day_tasks(recurrenceSeriesId, recurrenceOccurrenceDayKey)",
            )

            db.execSQL("ALTER TABLE day_focus_items ADD COLUMN recurrenceSeriesId TEXT")
            db.execSQL("ALTER TABLE day_focus_items ADD COLUMN recurrenceOccurrenceDayKey TEXT")
            db.execSQL("ALTER TABLE day_focus_items ADD COLUMN recurrenceSourceSeriesVersion INTEGER")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_day_focus_items_recurrenceSeriesId_recurrenceOccurrenceDayKey " +
                    "ON day_focus_items(recurrenceSeriesId, recurrenceOccurrenceDayKey)",
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS canonical_recurring_series (
                    id TEXT NOT NULL,
                    kind TEXT NOT NULL,
                    ruleFrequency TEXT NOT NULL,
                    ruleInterval INTEGER NOT NULL,
                    ruleDaysOfWeekCsv TEXT,
                    startDayKey TEXT NOT NULL,
                    endDayKey TEXT,
                    templateJson TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncedAt INTEGER,
                    isDeleted INTEGER NOT NULL,
                    version INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_canonical_recurring_series_kind " +
                    "ON canonical_recurring_series(kind)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_canonical_recurring_series_startDayKey " +
                    "ON canonical_recurring_series(startDayKey)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_canonical_recurring_series_endDayKey " +
                    "ON canonical_recurring_series(endDayKey)",
            )
        }
    }

val MIGRATION_143_144 =
    object : Migration(143, 144) {
        override fun migrate(db: SupportSQLiteDatabase) {
            check(countRows(db, "SELECT COUNT(*) FROM recurring_tasks WHERE frequency = 'HOURLY'") == 0L) {
                "Cannot retire recurrence-v1: legacy HOURLY recurring tasks still exist"
            }
            check(countRows(db, "SELECT COUNT(*) FROM day_tasks WHERE recurringTaskId IS NOT NULL") == 0L) {
                "Cannot retire recurrence-v1: DayTask rows still reference recurring_tasks"
            }
            check(countRows(db, "SELECT COUNT(*) FROM day_tasks WHERE nextOccurrenceTime IS NOT NULL") == 0L) {
                "Cannot retire recurrence-v1: nextOccurrenceTime markers still exist"
            }
            check(
                countRows(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM recurring_tasks r
                    LEFT JOIN canonical_recurring_series c
                      ON c.id = r.id AND c.kind = 'TASK'
                    WHERE c.id IS NULL
                    """.trimIndent(),
                ) == 0L,
            ) {
                "Cannot retire recurrence-v1: a legacy RecurringTask has no canonical TASK counterpart"
            }
            check(
                countRows(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM canonical_recurring_series
                    WHERE ruleInterval < 1
                       OR ruleFrequency NOT IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')
                    """.trimIndent(),
                ) == 0L,
            ) {
                "Cannot retire recurrence-v1: invalid canonical recurrence rule"
            }
            check(
                countRows(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM day_tasks t
                    LEFT JOIN canonical_recurring_series c
                      ON c.id = t.recurrenceSeriesId
                    WHERE t.recurrenceSeriesId IS NOT NULL
                      AND (c.id IS NULL OR c.kind != 'TASK')
                    """.trimIndent(),
                ) == 0L,
            ) {
                "Cannot retire recurrence-v1: canonical DayTask provenance references a missing or non-TASK series"
            }
            check(
                countRows(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM day_tasks
                    WHERE
                        (recurrenceSeriesId IS NULL AND
                            (recurrenceOccurrenceDayKey IS NOT NULL OR recurrenceSourceSeriesVersion IS NOT NULL))
                        OR
                        (recurrenceSeriesId IS NOT NULL AND
                            (recurrenceOccurrenceDayKey IS NULL OR recurrenceSourceSeriesVersion IS NULL))
                    """.trimIndent(),
                ) == 0L,
            ) {
                "Cannot retire recurrence-v1: partial canonical DayTask provenance exists"
            }

            sanitizeCanonicalTaskTemplateLinks(db)
        }

        private fun countRows(
            db: SupportSQLiteDatabase,
            sql: String,
        ): Long =
            db.query(sql).use { cursor ->
                check(cursor.moveToFirst()) { "Migration invariant query returned no row: $sql" }
                cursor.getLong(0)
            }

        private fun sanitizeCanonicalTaskTemplateLinks(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            val rows = mutableListOf<Pair<String, String>>()

            db.query(
                "SELECT id, templateJson FROM canonical_recurring_series WHERE kind = 'TASK'",
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    rows += cursor.getString(0) to cursor.getString(1)
                }
            }

            rows.forEach { (seriesId, templateJson) ->
                val template =
                    runCatching {
                        com.google.gson.JsonParser.parseString(templateJson).asJsonObject
                    }.getOrElse { cause ->
                        error("Invalid canonical TASK template JSON for series $seriesId: ${cause.message}")
                    }

                var changed = false
                listOf("linkedProjectIds", "linkedAttachmentIds").forEach { fieldName ->
                    val field = template.get(fieldName)
                    check(field != null && field.isJsonArray) {
                        "Canonical TASK template $seriesId has invalid $fieldName"
                    }

                    val normalizedValues =
                        field.asJsonArray
                            .mapNotNull { element ->
                                if (element.isJsonNull || !element.isJsonPrimitive) {
                                    null
                                } else {
                                    element.asString.trim().takeIf { value -> value.isNotEmpty() }
                                }
                            }.distinct()

                    val currentValues =
                        field.asJsonArray.mapNotNull { element ->
                            if (element.isJsonNull || !element.isJsonPrimitive) null else element.asString
                        }

                    if (currentValues != normalizedValues) {
                        val normalizedArray = com.google.gson.JsonArray()
                        normalizedValues.forEach(normalizedArray::add)
                        template.add(fieldName, normalizedArray)
                        changed = true
                    }
                }

                if (changed) {
                    db.execSQL(
                        """
                        UPDATE canonical_recurring_series
                        SET templateJson = ?,
                            updatedAt = ?,
                            syncedAt = NULL,
                            version = version + 1
                        WHERE id = ?
                        """.trimIndent(),
                        arrayOf<Any?>(template.toString(), now, seriesId),
                    )
                }
            }
        }
    }

val MIGRATION_144_145 =
    object : Migration(144, 145) {
        override fun migrate(db: SupportSQLiteDatabase) {
            check(tableExists(db, "recurring_tasks")) {
                "Cannot retire recurrence-v1: recurring_tasks table is missing from schema 144"
            }
            check(tableExists(db, "recurring_tasks_fts")) {
                "Cannot retire recurrence-v1: recurring_tasks_fts table is missing from schema 144"
            }
            check(!tableExists(db, "day_tasks_new")) {
                "Cannot retire recurrence-v1: unexpected day_tasks_new table already exists"
            }

            check(countRows(db, "SELECT COUNT(*) FROM recurring_tasks WHERE frequency = 'HOURLY'") == 0L) {
                "Cannot retire recurrence-v1: legacy HOURLY recurring tasks still exist"
            }
            check(countRows(db, "SELECT COUNT(*) FROM day_tasks WHERE recurringTaskId IS NOT NULL") == 0L) {
                "Cannot retire recurrence-v1: DayTask rows still reference recurring_tasks"
            }
            check(countRows(db, "SELECT COUNT(*) FROM day_tasks WHERE nextOccurrenceTime IS NOT NULL") == 0L) {
                "Cannot retire recurrence-v1: nextOccurrenceTime markers still exist"
            }
            check(
                countRows(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM recurring_tasks r
                    LEFT JOIN canonical_recurring_series c
                      ON c.id = r.id AND c.kind = 'TASK'
                    WHERE c.id IS NULL
                    """.trimIndent(),
                ) == 0L,
            ) {
                "Cannot retire recurrence-v1: a legacy RecurringTask has no canonical TASK counterpart"
            }
            check(
                countRows(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM day_tasks t
                    LEFT JOIN canonical_recurring_series c
                      ON c.id = t.recurrenceSeriesId
                    WHERE t.recurrenceSeriesId IS NOT NULL
                      AND (c.id IS NULL OR c.kind != 'TASK')
                    """.trimIndent(),
                ) == 0L,
            ) {
                "Cannot retire recurrence-v1: canonical DayTask provenance references a missing or non-TASK series"
            }
            check(
                countRows(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM day_tasks
                    WHERE
                        (recurrenceSeriesId IS NULL AND
                            (recurrenceOccurrenceDayKey IS NOT NULL OR recurrenceSourceSeriesVersion IS NOT NULL))
                        OR
                        (recurrenceSeriesId IS NOT NULL AND
                            (recurrenceOccurrenceDayKey IS NULL OR recurrenceSourceSeriesVersion IS NULL))
                    """.trimIndent(),
                ) == 0L,
            ) {
                "Cannot retire recurrence-v1: partial canonical DayTask provenance exists"
            }

            val inboundDayTaskForeignKeys = foreignKeyReferencesTo(db, "day_tasks")
            check(inboundDayTaskForeignKeys.isEmpty()) {
                "Cannot rebuild day_tasks safely: other tables reference it: ${inboundDayTaskForeignKeys.joinToString()}"
            }

            val dayTaskCountBefore = countRows(db, "SELECT COUNT(*) FROM day_tasks")

            db.execSQL(
                """
                CREATE TABLE `day_tasks_new` (
                    `id` TEXT NOT NULL,
                    `dayPlanId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT,
                    `goalId` TEXT,
                    `projectId` TEXT,
                    `linkedProjectIds` TEXT,
                    `linkedAttachmentIds` TEXT,
                    `activityRecordId` TEXT,
                    `recurrenceSeriesId` TEXT,
                    `recurrenceOccurrenceDayKey` TEXT,
                    `recurrenceSourceSeriesVersion` INTEGER,
                    `taskType` TEXT,
                    `entityId` TEXT,
                    `order` INTEGER NOT NULL,
                    `priority` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `completed` INTEGER NOT NULL,
                    `scheduledTime` INTEGER,
                    `estimatedDurationMinutes` INTEGER,
                    `actualDurationMinutes` INTEGER,
                    `dueTime` INTEGER,
                    `executionStrictness` TEXT NOT NULL DEFAULT 'NORMAL',
                    `valueImportance` REAL NOT NULL DEFAULT 0.0,
                    `valueImpact` REAL NOT NULL DEFAULT 0.0,
                    `effort` REAL NOT NULL DEFAULT 0.0,
                    `cost` REAL NOT NULL DEFAULT 0.0,
                    `risk` REAL NOT NULL DEFAULT 0.0,
                    `location` TEXT,
                    `tags` TEXT,
                    `notes` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER,
                    `syncedAt` INTEGER,
                    `isDeleted` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    `completedAt` INTEGER,
                    `points` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`dayPlanId`) REFERENCES `day_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                    FOREIGN KEY(`projectId`) REFERENCES `contexts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                    FOREIGN KEY(`activityRecordId`) REFERENCES `activity_records`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO `day_tasks_new` (
                    `id`, `dayPlanId`, `title`, `description`, `goalId`, `projectId`,
                    `linkedProjectIds`, `linkedAttachmentIds`, `activityRecordId`,
                    `recurrenceSeriesId`, `recurrenceOccurrenceDayKey`, `recurrenceSourceSeriesVersion`,
                    `taskType`, `entityId`, `order`, `priority`, `status`, `completed`,
                    `scheduledTime`, `estimatedDurationMinutes`, `actualDurationMinutes`, `dueTime`,
                    `executionStrictness`, `valueImportance`, `valueImpact`, `effort`, `cost`, `risk`,
                    `location`, `tags`, `notes`, `createdAt`, `updatedAt`, `syncedAt`, `isDeleted`,
                    `version`, `completedAt`, `points`
                )
                SELECT
                    `id`, `dayPlanId`, `title`, `description`, `goalId`, `projectId`,
                    `linkedProjectIds`, `linkedAttachmentIds`, `activityRecordId`,
                    `recurrenceSeriesId`, `recurrenceOccurrenceDayKey`, `recurrenceSourceSeriesVersion`,
                    `taskType`, `entityId`, `order`, `priority`, `status`, `completed`,
                    `scheduledTime`, `estimatedDurationMinutes`, `actualDurationMinutes`, `dueTime`,
                    `executionStrictness`, `valueImportance`, `valueImpact`, `effort`, `cost`, `risk`,
                    `location`, `tags`, `notes`, `createdAt`, `updatedAt`, `syncedAt`, `isDeleted`,
                    `version`, `completedAt`, `points`
                FROM `day_tasks`
                """.trimIndent(),
            )

            check(countRows(db, "SELECT COUNT(*) FROM day_tasks_new") == dayTaskCountBefore) {
                "Cannot retire recurrence-v1: day_tasks row count changed while copying"
            }
            check(
                countRows(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM day_tasks old
                    LEFT JOIN day_tasks_new replacement ON replacement.id = old.id
                    WHERE replacement.id IS NULL
                    """.trimIndent(),
                ) == 0L,
            ) {
                "Cannot retire recurrence-v1: at least one DayTask id was not copied"
            }

            db.execSQL("DROP TABLE `day_tasks`")
            db.execSQL("ALTER TABLE `day_tasks_new` RENAME TO `day_tasks`")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_dayPlanId` ON `day_tasks` (`dayPlanId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_goalId` ON `day_tasks` (`goalId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_projectId` ON `day_tasks` (`projectId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_activityRecordId` ON `day_tasks` (`activityRecordId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_scheduledTime` ON `day_tasks` (`scheduledTime`)")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_day_tasks_recurrenceSeriesId_recurrenceOccurrenceDayKey` " +
                    "ON `day_tasks` (`recurrenceSeriesId`, `recurrenceOccurrenceDayKey`)",
            )

            check(countRows(db, "SELECT COUNT(*) FROM day_tasks") == dayTaskCountBefore) {
                "Cannot retire recurrence-v1: day_tasks row count changed after table swap"
            }
            check(!columnExists(db, "day_tasks", "recurringTaskId")) {
                "Cannot retire recurrence-v1: recurringTaskId survived day_tasks rebuild"
            }
            check(!columnExists(db, "day_tasks", "nextOccurrenceTime")) {
                "Cannot retire recurrence-v1: nextOccurrenceTime survived day_tasks rebuild"
            }

            val recurringTaskForeignKeys = foreignKeyReferencesTo(db, "recurring_tasks")
            check(recurringTaskForeignKeys.isEmpty()) {
                "Cannot drop recurring_tasks safely: foreign keys still reference it: ${recurringTaskForeignKeys.joinToString()}"
            }

            val recurringTaskTriggers = triggerNames(db, "recurring_tasks")
            val unexpectedRecurringTaskTriggers =
                recurringTaskTriggers.filterNot { triggerName ->
                    triggerName.startsWith("room_fts_content_sync_recurring_tasks_fts_")
                }
            check(unexpectedRecurringTaskTriggers.isEmpty()) {
                "Cannot drop recurring_tasks safely: unexpected triggers exist: ${unexpectedRecurringTaskTriggers.joinToString()}"
            }
            recurringTaskTriggers.forEach { triggerName ->
                db.execSQL("DROP TRIGGER IF EXISTS ${quoteIdentifier(triggerName)}")
            }

            db.execSQL("DROP TABLE `recurring_tasks_fts`")
            db.execSQL("DROP TABLE `recurring_tasks`")

            check(!tableExists(db, "recurring_tasks")) {
                "Cannot retire recurrence-v1: recurring_tasks still exists after DROP"
            }
            check(!tableExists(db, "recurring_tasks_fts")) {
                "Cannot retire recurrence-v1: recurring_tasks_fts still exists after DROP"
            }

            db.query("PRAGMA foreign_key_check").use { cursor ->
                check(!cursor.moveToFirst()) {
                    "Cannot retire recurrence-v1: foreign_key_check failed after migration"
                }
            }
            db.query("PRAGMA integrity_check").use { cursor ->
                check(cursor.moveToFirst() && cursor.getString(0) == "ok" && !cursor.moveToNext()) {
                    "Cannot retire recurrence-v1: integrity_check failed after migration"
                }
            }
        }

        private fun countRows(
            db: SupportSQLiteDatabase,
            sql: String,
        ): Long =
            db.query(sql).use { cursor ->
                check(cursor.moveToFirst()) { "Migration invariant query returned no row: $sql" }
                cursor.getLong(0)
            }

        private fun tableExists(
            db: SupportSQLiteDatabase,
            tableName: String,
        ): Boolean =
            countRows(
                db,
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = '${sqlLiteral(tableName)}'",
            ) == 1L

        private fun columnExists(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
        ): Boolean {
            val escapedTableName = sqlLiteral(tableName)
            return db.query("PRAGMA table_info('$escapedTableName')").use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == columnName) return@use true
                }
                false
            }
        }

        private fun foreignKeyReferencesTo(
            db: SupportSQLiteDatabase,
            targetTableName: String,
        ): List<String> {
            val tableNames = mutableListOf<String>()
            db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'").use { cursor ->
                while (cursor.moveToNext()) tableNames += cursor.getString(0)
            }

            val references = mutableListOf<String>()
            tableNames.forEach { tableName ->
                db.query("PRAGMA foreign_key_list('${sqlLiteral(tableName)}')").use { cursor ->
                    while (cursor.moveToNext()) {
                        if (cursor.getString(2) == targetTableName) {
                            references += "$tableName.${cursor.getString(3)} -> $targetTableName.${cursor.getString(4)}"
                        }
                    }
                }
            }
            return references
        }

        private fun triggerNames(
            db: SupportSQLiteDatabase,
            tableName: String,
        ): List<String> {
            val result = mutableListOf<String>()
            db.query(
                "SELECT name FROM sqlite_master WHERE type = 'trigger' AND tbl_name = '${sqlLiteral(tableName)}'",
            ).use { cursor ->
                while (cursor.moveToNext()) result += cursor.getString(0)
            }
            return result
        }

        private fun sqlLiteral(value: String): String = value.replace("'", "''")

        private fun quoteIdentifier(value: String): String = "`" + value.replace("`", "``") + "`"
    }

val MIGRATION_145_146 =
    object : Migration(145, 146) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `day_theme_documents` (
                    `dayPlanId` TEXT NOT NULL,
                    `contentJson` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER,
                    `syncedAt` INTEGER,
                    `isDeleted` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    PRIMARY KEY(`dayPlanId`),
                    FOREIGN KEY(`dayPlanId`) REFERENCES `day_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
        }
    }
