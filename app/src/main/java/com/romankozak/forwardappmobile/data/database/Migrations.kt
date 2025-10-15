package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE custom_lists ADD COLUMN content TEXT")
    }
}

val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_36_37 = object : Migration(36, 37) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_37_38 = object : Migration(37, 38) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_38_39 = object : Migration(38, 39) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_39_40 = object : Migration(39, 40) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_40_41 = object : Migration(40, 41) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_41_42 = object : Migration(41, 42) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_42_43 = object : Migration(42, 43) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_43_44 = object : Migration(43, 44) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE day_tasks ADD COLUMN points INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE recurring_tasks ADD COLUMN points INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_44_45 = object : Migration(44, 45) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // goals table
        db.execSQL("ALTER TABLE goals RENAME TO goals_old")
        db.execSQL("""
            CREATE TABLE `goals` (
                `id` TEXT NOT NULL, 
                `text` TEXT NOT NULL, 
                `description` TEXT, 
                `completed` INTEGER NOT NULL, 
                `createdAt` INTEGER NOT NULL, 
                `updatedAt` INTEGER, 
                `tags` TEXT, 
                `relatedLinks` TEXT, 
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
                `parentValueImportance` REAL DEFAULT 0.0, 
                `impactOnParentGoal` REAL DEFAULT 0.0, 
                `timeCost` REAL DEFAULT 0.0, 
                `financialCost` REAL DEFAULT 0.0, 
                `reminder_time` INTEGER, 
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO goals 
            SELECT 
                id, text, description, completed, createdAt, updatedAt, 
                tags, relatedLinks, valueImportance, valueImpact, effort, 
                cost, risk, weightEffort, weightCost, weightRisk, rawScore, 
                displayScore, 
                CASE scoring_status 
                    WHEN 0 THEN 'NOT_ASSESSED' 
                    WHEN 1 THEN 'IMPOSSIBLE_TO_ASSESS' 
                    WHEN 2 THEN 'ASSESSED' 
                    ELSE 'NOT_ASSESSED' 
                END, 
                parentValueImportance, impactOnParentGoal, timeCost, 
                financialCost, reminder_time 
            FROM goals_old
        """.trimIndent())
        db.execSQL("DROP TABLE goals_old")

        // projects table
        db.execSQL("ALTER TABLE projects RENAME TO projects_old")
        db.execSQL("""
            CREATE TABLE `projects` (
                `id` TEXT NOT NULL, 
                `name` TEXT NOT NULL, 
                `description` TEXT, 
                `parentId` TEXT, 
                `createdAt` INTEGER NOT NULL, 
                `updatedAt` INTEGER, 
                `tags` TEXT, 
                `is_expanded` INTEGER NOT NULL DEFAULT 1, 
                `goal_order` INTEGER NOT NULL DEFAULT 0, 
                `is_attachments_expanded` INTEGER NOT NULL DEFAULT 0, 
                `default_view_mode` TEXT, 
                `is_completed` INTEGER NOT NULL DEFAULT 0, 
                `is_project_management_enabled` INTEGER, 
                `project_status` TEXT, 
                `project_status_text` TEXT, 
                `project_log_level` TEXT, 
                `total_time_spent_minutes` INTEGER, 
                `reminder_time` INTEGER, 
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
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO projects 
            SELECT 
                id, name, description, parentId, createdAt, updatedAt, tags, 
                is_expanded, goal_order, is_attachments_expanded, 
                default_view_mode, is_completed, is_project_management_enabled, 
                CASE project_status 
                    WHEN 0 THEN 'NO_PLAN' 
                    WHEN 1 THEN 'PLANNING' 
                    WHEN 2 THEN 'IN_PROGRESS' 
                    WHEN 3 THEN 'COMPLETED' 
                    WHEN 4 THEN 'ON_HOLD' 
                    WHEN 5 THEN 'PAUSED' 
                    ELSE 'NO_PLAN' 
                END, 
                project_status_text, 
                CASE project_log_level 
                    WHEN 0 THEN 'DETAILED' 
                    WHEN 1 THEN 'NORMAL' 
                    ELSE 'NORMAL' 
                END, 
                total_time_spent_minutes, reminder_time, valueImportance, 
                valueImpact, effort, cost, risk, weightEffort, weightCost, 
                weightRisk, rawScore, displayScore, 
                CASE scoring_status 
                    WHEN 0 THEN 'NOT_ASSESSED' 
                    WHEN 1 THEN 'IMPOSSIBLE_TO_ASSESS' 
                    WHEN 2 THEN 'ASSESSED' 
                    ELSE 'NOT_ASSESSED' 
                END 
            FROM projects_old
        """.trimIndent())
        db.execSQL("DROP TABLE projects_old")

        // project_execution_logs table
        db.execSQL("ALTER TABLE project_execution_logs RENAME TO project_execution_logs_old")
        db.execSQL("""
            CREATE TABLE `project_execution_logs` (
                `id` TEXT NOT NULL, 
                `projectId` TEXT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                `type` TEXT NOT NULL, 
                `description` TEXT NOT NULL, 
                `details` TEXT, 
                PRIMARY KEY(`id`), 
                FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_execution_logs_projectId` ON `project_execution_logs` (`projectId`)")
        db.execSQL("""
            INSERT INTO project_execution_logs 
            SELECT 
                id, projectId, timestamp, 
                CASE type 
                    WHEN 0 THEN 'STATUS_CHANGE' 
                    WHEN 1 THEN 'COMMENT' 
                    WHEN 2 THEN 'AUTOMATIC' 
                    WHEN 3 THEN 'INSIGHT' 
                    WHEN 4 THEN 'MILESTONE' 
                    ELSE 'COMMENT' 
                END, 
                description, details 
            FROM project_execution_logs_old
        """.trimIndent())
        db.execSQL("DROP TABLE project_execution_logs_old")

        // list_items table
        db.execSQL("ALTER TABLE list_items RENAME TO list_items_old")
        db.execSQL("""
            CREATE TABLE `list_items` (
                `id` TEXT NOT NULL, 
                `project_id` TEXT NOT NULL, 
                `itemType` TEXT NOT NULL, 
                `entityId` TEXT NOT NULL, 
                `item_order` INTEGER NOT NULL, 
                PRIMARY KEY(`id`), 
                FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_list_items_project_id` ON `list_items` (`project_id`)")
        db.execSQL("""
            INSERT INTO list_items 
            SELECT 
                id, project_id, 
                CASE itemType 
                    WHEN 0 THEN 'GOAL' 
                    WHEN 1 THEN 'SUBLIST' 
                    WHEN 2 THEN 'LINK_ITEM' 
                    WHEN 3 THEN 'NOTE' 
                    WHEN 4 THEN 'CUSTOM_LIST' 
                    ELSE 'UNKNOWN' 
                END, 
                entityId, item_order 
            FROM list_items_old
        """.trimIndent())
        db.execSQL("DROP TABLE list_items_old")
    }
}

val MIGRATION_45_46 = object : Migration(45, 46) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Перевіряємо, чи існує таблиця reminder_info
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='reminder_info'")
        val tableExists = cursor.count > 0
        cursor.close()
        
        if (!tableExists) {
            // Якщо таблиця не існує - створюємо
            db.execSQL("""
                CREATE TABLE `reminder_info` (
                    `goalId` TEXT NOT NULL,
                    `reminder_status` TEXT NOT NULL,
                    `snooze_time` INTEGER,
                    PRIMARY KEY(`goalId`)
                )
            """.trimIndent())
        } else {
            // Якщо таблиця існує але пошкоджена - пересоздаємо
            val columnsCursor = db.query("PRAGMA table_info(reminder_info)")
            val hasColumns = columnsCursor.count > 0
            columnsCursor.close()
            
            if (!hasColumns) {
                // Таблиця порожня - видаляємо та створюємо знову
                db.execSQL("DROP TABLE IF EXISTS reminder_info")
                db.execSQL("""
                    CREATE TABLE `reminder_info` (
                        `goalId` TEXT NOT NULL,
                        `reminder_status` TEXT NOT NULL,
                        `snooze_time` INTEGER,
                        PRIMARY KEY(`goalId`)
                    )
                """.trimIndent())
            }
        }
        
        // Додаємо індекси для list_items та project_execution_logs
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_list_items_project_id` ON `list_items` (`project_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_execution_logs_projectId` ON `project_execution_logs` (`projectId`)")
    }
}

val MIGRATION_46_47 = object : Migration(46, 47) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE custom_lists ADD COLUMN lastCursorPosition INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_47_48 = object : Migration(47, 48) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_48_49 = object : Migration(48, 49) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `project_reminder_info` (`projectId` TEXT NOT NULL, `reminder_status` TEXT NOT NULL, `snooze_time` INTEGER, PRIMARY KEY(`projectId`))"
        )
    }
}

val MIGRATION_49_50 = object : Migration(49, 50) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE `reminders` (`id` TEXT NOT NULL, `goalId` TEXT NOT NULL, `reminderTime` INTEGER NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`id`, `goalId`))")
    }
}

val MIGRATION_50_51 = object : Migration(50, 51) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Step 1: Check which columns exist in day_tasks table
        val dayTasksCursor = db.query("PRAGMA table_info(day_tasks)")
        val existingDayTasksColumns = mutableSetOf<String>()
        while (dayTasksCursor.moveToNext()) {
            val nameIndex = dayTasksCursor.getColumnIndex("name")
            if (nameIndex >= 0) {
                val columnName = dayTasksCursor.getString(nameIndex)
                existingDayTasksColumns.add(columnName)
            }
        }
        dayTasksCursor.close()
        
        // Step 2: Create the new unified reminders table
        db.execSQL("""
            CREATE TABLE `reminders_new` (
                `id` TEXT NOT NULL, 
                `entityId` TEXT NOT NULL, 
                `entityType` TEXT NOT NULL, 
                `reminderTime` INTEGER NOT NULL, 
                `status` TEXT NOT NULL, 
                `creationTime` INTEGER NOT NULL, 
                `snoozeUntil` INTEGER, 
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        // Step 3: Migrate data from goals and reminder_info
        db.execSQL("""
            INSERT INTO reminders_new (id, entityId, entityType, reminderTime, status, creationTime, snoozeUntil)
            SELECT
                goals.id,
                goals.id,
                'GOAL',
                goals.reminder_time,
                COALESCE(ri.reminder_status, 'SCHEDULED'),
                goals.createdAt,
                ri.snooze_time
            FROM goals
            LEFT JOIN reminder_info AS ri ON goals.id = ri.goalId
            WHERE goals.reminder_time IS NOT NULL
        """.trimIndent())

        // Step 4: Migrate data from projects and project_reminder_info
        db.execSQL("""
            INSERT INTO reminders_new (id, entityId, entityType, reminderTime, status, creationTime, snoozeUntil)
            SELECT
                projects.id,
                projects.id,
                'PROJECT',
                projects.reminder_time,
                COALESCE(pri.reminder_status, 'SCHEDULED'),
                projects.createdAt,
                pri.snooze_time
            FROM projects
            LEFT JOIN project_reminder_info AS pri ON projects.id = pri.projectId
            WHERE projects.reminder_time IS NOT NULL
        """.trimIndent())

        // Step 5: Migrate data from day_tasks (only if reminderTime column exists)
        if ("reminderTime" in existingDayTasksColumns) {
            db.execSQL("""
                INSERT INTO reminders_new (id, entityId, entityType, reminderTime, status, creationTime, snoozeUntil)
                SELECT
                    id,
                    id,
                    'TASK',
                    reminderTime,
                    'SCHEDULED',
                    createdAt,
                    NULL
                FROM day_tasks
                WHERE reminderTime IS NOT NULL
            """.trimIndent())
        }

        // Step 6: Drop the old tables
        db.execSQL("DROP TABLE reminders")
        db.execSQL("DROP TABLE reminder_info")
        db.execSQL("DROP TABLE project_reminder_info")

        // Step 7: Rename the new table
        db.execSQL("ALTER TABLE reminders_new RENAME TO reminders")

        // Step 8: Remove reminder_time column from goals
        db.execSQL("""
            CREATE TABLE `goals_temp` (
                `id` TEXT NOT NULL, 
                `text` TEXT NOT NULL, 
                `description` TEXT, 
                `completed` INTEGER NOT NULL, 
                `createdAt` INTEGER NOT NULL, 
                `updatedAt` INTEGER, 
                `tags` TEXT, 
                `relatedLinks` TEXT, 
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
                `parentValueImportance` REAL DEFAULT 0.0, 
                `impactOnParentGoal` REAL DEFAULT 0.0, 
                `timeCost` REAL DEFAULT 0.0, 
                `financialCost` REAL DEFAULT 0.0, 
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("INSERT INTO goals_temp SELECT id, text, description, completed, createdAt, updatedAt, tags, relatedLinks, valueImportance, valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore, scoring_status, parentValueImportance, impactOnParentGoal, timeCost, financialCost FROM goals")
        db.execSQL("DROP TABLE goals")
        db.execSQL("ALTER TABLE goals_temp RENAME TO goals")

        // Step 9: Remove reminder_time column from projects
        db.execSQL("""
            CREATE TABLE `projects_temp` (
                `id` TEXT NOT NULL, 
                `name` TEXT NOT NULL, 
                `description` TEXT, 
                `parentId` TEXT, 
                `createdAt` INTEGER NOT NULL, 
                `updatedAt` INTEGER, 
                `tags` TEXT, 
                `is_expanded` INTEGER NOT NULL DEFAULT 1, 
                `goal_order` INTEGER NOT NULL DEFAULT 0, 
                `is_attachments_expanded` INTEGER NOT NULL DEFAULT 0, 
                `default_view_mode` TEXT, 
                `is_completed` INTEGER NOT NULL DEFAULT 0, 
                `is_project_management_enabled` INTEGER, 
                `project_status` TEXT, 
                `project_status_text` TEXT, 
                `project_log_level` TEXT, 
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
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("INSERT INTO projects_temp SELECT id, name, description, parentId, createdAt, updatedAt, tags, is_expanded, goal_order, is_attachments_expanded, default_view_mode, is_completed, is_project_management_enabled, project_status, project_status_text, project_log_level, total_time_spent_minutes, valueImportance, valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore, scoring_status FROM projects")
        db.execSQL("DROP TABLE projects")
        db.execSQL("ALTER TABLE projects_temp RENAME TO projects")

        // Step 10: Recreate day_tasks with proper schema (NO DEFAULT VALUES except those with explicit defaults in Entity)
        db.execSQL("""
            CREATE TABLE `day_tasks_temp` (
                `id` TEXT NOT NULL, 
                `dayPlanId` TEXT NOT NULL, 
                `title` TEXT NOT NULL, 
                `description` TEXT, 
                `goalId` TEXT, 
                `projectId` TEXT, 
                `activityRecordId` TEXT, 
                `recurringTaskId` TEXT, 
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
                `completedAt` INTEGER, 
                `nextOccurrenceTime` INTEGER, 
                `points` INTEGER NOT NULL DEFAULT 0, 
                PRIMARY KEY(`id`),
                FOREIGN KEY(`dayPlanId`) REFERENCES `day_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`activityRecordId`) REFERENCES `activity_records`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`recurringTaskId`) REFERENCES `recurring_tasks`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """.trimIndent())
        
        // Build dynamic SELECT based on existing columns
        fun getColumnValue(columnName: String, defaultValue: String): String {
            return if (columnName in existingDayTasksColumns) columnName else defaultValue
        }
        
        val selectStatement = """
            INSERT INTO day_tasks_temp (
                id, dayPlanId, title, description, goalId, projectId,
                activityRecordId, recurringTaskId, taskType, entityId, `order`,
                priority, status, completed, scheduledTime, estimatedDurationMinutes,
                actualDurationMinutes, dueTime, valueImportance, valueImpact, effort,
                cost, risk, location, tags, notes, createdAt, updatedAt,
                completedAt, nextOccurrenceTime, points
            ) SELECT 
                id,
                dayPlanId,
                ${getColumnValue("title", "''")},
                ${getColumnValue("description", "NULL")},
                ${getColumnValue("goalId", "NULL")},
                ${getColumnValue("projectId", "NULL")},
                ${getColumnValue("activityRecordId", "NULL")},
                ${getColumnValue("recurringTaskId", "NULL")},
                ${getColumnValue("taskType", "NULL")},
                ${getColumnValue("entityId", "NULL")},
                ${getColumnValue("`order`", "0")},
                ${getColumnValue("priority", "'MEDIUM'")},
                ${getColumnValue("status", "'NOT_STARTED'")},
                completed,
                ${getColumnValue("scheduledTime", "NULL")},
                ${getColumnValue("estimatedDurationMinutes", "NULL")},
                ${getColumnValue("actualDurationMinutes", "NULL")},
                ${getColumnValue("dueTime", "NULL")},
                ${getColumnValue("valueImportance", "0.0")},
                ${getColumnValue("valueImpact", "0.0")},
                ${getColumnValue("effort", "0.0")},
                ${getColumnValue("cost", "0.0")},
                ${getColumnValue("risk", "0.0")},
                ${getColumnValue("location", "NULL")},
                ${getColumnValue("tags", "NULL")},
                ${getColumnValue("notes", "NULL")},
                createdAt,
                ${getColumnValue("updatedAt", "NULL")},
                ${getColumnValue("completedAt", "NULL")},
                ${getColumnValue("nextOccurrenceTime", "NULL")},
                ${getColumnValue("points", "0")}
            FROM day_tasks
        """.trimIndent()
        
        db.execSQL(selectStatement)
        db.execSQL("DROP TABLE day_tasks")
        db.execSQL("ALTER TABLE day_tasks_temp RENAME TO day_tasks")
        
        // Step 11: Create indices AFTER renaming the table
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_dayPlanId` ON `day_tasks` (`dayPlanId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_goalId` ON `day_tasks` (`goalId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_projectId` ON `day_tasks` (`projectId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_activityRecordId` ON `day_tasks` (`activityRecordId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_scheduledTime` ON `day_tasks` (`scheduledTime`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_recurringTaskId` ON `day_tasks` (`recurringTaskId`)")
    }
}

val MIGRATION_51_52 = object : Migration(51, 52) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `project_artifacts` (`id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_artifacts_projectId` ON `project_artifacts` (`projectId`)")
    }
}
