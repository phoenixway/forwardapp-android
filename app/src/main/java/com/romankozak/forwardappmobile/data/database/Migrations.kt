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
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_projectId` ON `notes` (`projectId`)")
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` USING FTS4(`title` TEXT NOT NULL, `content` TEXT NOT NULL, content=`notes`)")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_notes_fts_BEFORE_UPDATE BEFORE UPDATE ON `notes` BEGIN DELETE FROM `notes_fts` WHERE `docid`=OLD.`rowid`; END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_notes_fts_BEFORE_DELETE BEFORE DELETE ON `notes` BEGIN DELETE FROM `notes_fts` WHERE `docid`=OLD.`rowid`; END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_notes_fts_AFTER_UPDATE AFTER UPDATE ON `notes` BEGIN INSERT INTO `notes_fts`(`docid`, `title`, `content`) VALUES (NEW.`rowid`, NEW.`title`, NEW.`content`); END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_notes_fts_AFTER_INSERT AFTER INSERT ON `notes` BEGIN INSERT INTO `notes_fts`(`docid`, `title`, `content`) VALUES (NEW.`rowid`, NEW.`title`, NEW.`content`); END")
        db.execSQL("CREATE TABLE IF NOT EXISTS `custom_lists` (`id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_lists_projectId` ON `custom_lists` (`projectId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `custom_list_items` (`id` TEXT NOT NULL, `listId` TEXT NOT NULL, `parentId` TEXT, `content` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `itemOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`listId`) REFERENCES `custom_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`parentId`) REFERENCES `custom_list_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_list_items_listId` ON `custom_list_items` (`listId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_list_items_parentId` ON `custom_list_items` (`parentId`)")
    }
}

val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE custom_lists ADD COLUMN content TEXT")
    }
}

val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // No schema changes from 34 to 35.
    }
}

val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS recent_project_entries")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `recent_items` (
                `id` TEXT NOT NULL, 
                `type` TEXT NOT NULL, 
                `lastAccessed` INTEGER NOT NULL, 
                `displayName` TEXT NOT NULL, 
                `target` TEXT NOT NULL, 
                PRIMARY KEY(`id`)
            )
        """)
    }
}

val MIGRATION_37_38 = object : Migration(37, 38) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `conversation_folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
        db.execSQL("ALTER TABLE `conversations` ADD COLUMN `folderId` INTEGER")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversations_folderId` ON `conversations` (`folderId`)")
    }
}

val MIGRATION_36_37 = object : Migration(36, 37) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `conversations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `creationTimestamp` INTEGER NOT NULL)")

        db.execSQL("""
            CREATE TABLE `chat_messages_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `conversationId` INTEGER NOT NULL, 
                `text` TEXT NOT NULL, 
                `isFromUser` INTEGER NOT NULL, 
                `isError` INTEGER NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                `isStreaming` INTEGER NOT NULL, 
                FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)

        try {
            db.execSQL("""
                INSERT INTO chat_messages_new (id, conversationId, text, isFromUser, isError, timestamp, isStreaming)
                SELECT id, conversationId, text, isFromUser, isError, timestamp, isStreaming FROM chat_messages
            """)
        } catch (e: Exception) {
            // Old table might not exist or have a different schema, ignore copy errors
        }

        db.execSQL("DROP TABLE IF EXISTS chat_messages")
        db.execSQL("ALTER TABLE chat_messages_new RENAME TO chat_messages")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_conversationId` ON `chat_messages` (`conversationId`)")
    }
}

// Migrations.kt

val MIGRATION_38_39 = object : Migration(38, 39) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- ВИПРАВЛЕННЯ ДЛЯ ТАБЛИЦІ 'chat_messages' (з попереднього кроку) ---
        try {
            db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `conversationId` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_conversationId` ON `chat_messages` (`conversationId`)")
        } catch (e: Exception) {
            println("Migration 38-39: Failed to add 'conversationId' column, likely already exists. ${e.message}")
        }

        // --- НОВЕ ВИПРАВЛЕННЯ ДЛЯ ТАБЛИЦІ 'conversations' ---
        try {
            db.execSQL("ALTER TABLE `conversations` ADD COLUMN `folderId` INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversations_folderId` ON `conversations` (`folderId`)")
        } catch (e: Exception) {
            println("Migration 38-39: Failed to add 'folderId' column, likely already exists. ${e.message}")
        }
    }
}

val MIGRATION_39_40 = object : Migration(39, 40) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create the new recurring_tasks table
        db.execSQL("CREATE TABLE IF NOT EXISTS `recurring_tasks` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `duration` INTEGER, `priority` TEXT NOT NULL, `frequency` TEXT NOT NULL, `interval` INTEGER NOT NULL, `daysOfWeek` TEXT, `startDate` INTEGER NOT NULL, `endDate` INTEGER, PRIMARY KEY(`id`))")
        
        // 2. Create the FTS table for it
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `recurring_tasks_fts` USING FTS4(`title`, `description`, content=`recurring_tasks`)")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_recurring_tasks_fts_BEFORE_UPDATE BEFORE UPDATE ON `recurring_tasks` BEGIN DELETE FROM `recurring_tasks_fts` WHERE `docid`=OLD.`rowid`; END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_recurring_tasks_fts_BEFORE_DELETE BEFORE DELETE ON `recurring_tasks` BEGIN DELETE FROM `recurring_tasks_fts` WHERE `docid`=OLD.`rowid`; END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_recurring_tasks_fts_AFTER_UPDATE AFTER UPDATE ON `recurring_tasks` BEGIN INSERT INTO `recurring_tasks_fts`(`docid`, `title`, `description`) VALUES (NEW.`rowid`, NEW.`title`, NEW.`description`); END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_recurring_tasks_fts_AFTER_INSERT AFTER INSERT ON `recurring_tasks` BEGIN INSERT INTO `recurring_tasks_fts`(`docid`, `title`, `description`) VALUES (NEW.`rowid`, NEW.`title`, NEW.`description`); END")

        // 3. Recreate day_tasks table
        db.execSQL("""
            CREATE TABLE `day_tasks_new` (
                `id` TEXT NOT NULL, `dayPlanId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, 
                `goalId` TEXT, `projectId` TEXT, `activityRecordId` TEXT, `recurringTaskId` TEXT, `taskType` TEXT, 
                `entityId` TEXT, `order` INTEGER NOT NULL, `priority` TEXT NOT NULL, `status` TEXT NOT NULL, 
                `completed` INTEGER NOT NULL, `scheduledTime` INTEGER, `estimatedDurationMinutes` INTEGER, 
                `actualDurationMinutes` INTEGER, `reminderTime` INTEGER, `dueTime` INTEGER, 
                `valueImportance` REAL NOT NULL DEFAULT 0.0, `valueImpact` REAL NOT NULL DEFAULT 0.0, 
                `effort` REAL NOT NULL DEFAULT 0.0, `cost` REAL NOT NULL DEFAULT 0.0, `risk` REAL NOT NULL DEFAULT 0.0, 
                `location` TEXT, `tags` TEXT, `notes` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER, 
                `completedAt` INTEGER, 
                PRIMARY KEY(`id`), 
                FOREIGN KEY(`dayPlanId`) REFERENCES `day_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, 
                FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, 
                FOREIGN KEY(`activityRecordId`) REFERENCES `activity_records`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, 
                FOREIGN KEY(`recurringTaskId`) REFERENCES `recurring_tasks`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """)

        db.execSQL("""
            INSERT INTO day_tasks_new (id, dayPlanId, title, description, goalId, projectId, activityRecordId, taskType, entityId, `order`, priority, status, completed, scheduledTime, estimatedDurationMinutes, actualDurationMinutes, reminderTime, dueTime, valueImportance, valueImpact, effort, cost, risk, location, tags, notes, createdAt, updatedAt, completedAt)
            SELECT id, dayPlanId, title, description, goalId, projectId, activityRecordId, taskType, entityId, `order`, priority, status, completed, scheduledTime, estimatedDurationMinutes, actualDurationMinutes, reminderTime, dueTime, valueImportance, valueImpact, effort, cost, risk, location, tags, notes, createdAt, updatedAt, completedAt FROM day_tasks
        """)

        db.execSQL("DROP TABLE day_tasks")

        db.execSQL("ALTER TABLE day_tasks_new RENAME TO day_tasks")

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_dayPlanId` ON `day_tasks` (`dayPlanId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_goalId` ON `day_tasks` (`goalId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_projectId` ON `day_tasks` (`projectId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_activityRecordId` ON `day_tasks` (`activityRecordId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_scheduledTime` ON `day_tasks` (`scheduledTime`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tasks_recurringTaskId` ON `day_tasks` (`recurringTaskId`)")
    }
}