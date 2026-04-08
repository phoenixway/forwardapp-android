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
