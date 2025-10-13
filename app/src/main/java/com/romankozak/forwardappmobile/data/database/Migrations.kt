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
