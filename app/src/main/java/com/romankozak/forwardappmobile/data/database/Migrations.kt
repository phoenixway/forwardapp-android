package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink

val MIGRATION_8_9 =
    object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
        }
    }

val MIGRATION_10_11 =
    object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
        }
    }

val MIGRATION_11_12 =
    object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN tags TEXT")
        }
    }

val MIGRATION_12_13 =
    object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
            CREATE TABLE IF NOT EXISTS `activity_records` (
                `id` TEXT NOT NULL, 
                `text` TEXT NOT NULL, 
                `createdAt` INTEGER NOT NULL, 
                `startTime` INTEGER, 
                `endTime` INTEGER, 
                PRIMARY KEY(`id`)
            )
        """,
            )
        }
    }

val MIGRATION_13_14 =
    object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
            CREATE TABLE IF NOT EXISTS `recent_list_entries` (
                `list_id` TEXT NOT NULL, 
                `last_accessed` INTEGER NOT NULL, 
                PRIMARY KEY(`list_id`), 
                FOREIGN KEY(`list_id`) REFERENCES `goal_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """,
            )
        }
    }

val MIGRATION_14_15 =
    object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `notes` (`id` TEXT NOT NULL, `title` TEXT, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `list_items` (`id` TEXT NOT NULL, `listId` TEXT NOT NULL, `itemType` TEXT NOT NULL, `entityId` TEXT NOT NULL, `item_order` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`listId`) REFERENCES `goal_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_list_items_listId` ON `list_items` (`listId`)")

            db.execSQL(
                """
            INSERT INTO `list_items` (id, listId, itemType, entityId, item_order)
            SELECT instance_id, listId, 'GOAL', goalId, goal_order FROM goal_instances
        """,
            )

            db.execSQL("DROP TABLE `goal_instances`")

            db.execSQL(
                """
            CREATE TABLE `goals_new` (
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
                `scoring_status` TEXT NOT NULL DEFAULT 'NOT_ASSESSED',
                `parentValueImportance` REAL DEFAULT 0.0,
                `impactOnParentGoal` REAL DEFAULT 0.0,
                `timeCost` REAL DEFAULT 0.0,
                `financialCost` REAL DEFAULT 0.0,
                PRIMARY KEY(`id`)
            )
        """,
            )

            val cursor = db.query("SELECT id, associatedListIds FROM goals WHERE associatedListIds IS NOT NULL")
            val gson = Gson()
            val listStringType = object : TypeToken<List<String>>() {}.type
            val relatedLinkList = mutableListOf<Pair<String, String>>()

            if (cursor.moveToFirst()) {
                do {
                    val goalId = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                    val jsonOld = cursor.getString(cursor.getColumnIndexOrThrow("associatedListIds"))

                    if (jsonOld != null) {
                        val oldListIds: List<String> = gson.fromJson(jsonOld, listStringType)
                        val newRelatedLinks =
                            oldListIds.map { listId ->
                                RelatedLink(type = LinkType.GOAL_LIST, target = listId)
                            }
                        val jsonNew = gson.toJson(newRelatedLinks)
                        relatedLinkList.add(goalId to jsonNew)
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()

            db.execSQL(
                """
            INSERT INTO `goals_new` (id, text, description, completed, createdAt, updatedAt, tags, valueImportance, valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore, scoring_status, parentValueImportance, impactOnParentGoal, timeCost, financialCost)
            SELECT id, text, description, completed, createdAt, updatedAt, tags, valueImportance, valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore, scoring_status, parentValueImportance, impactOnParentGoal, timeCost, financialCost FROM `goals`
        """,
            )

            relatedLinkList.forEach { (goalId, jsonNew) ->
                db.execSQL("UPDATE goals_new SET relatedLinks = ? WHERE id = ?", arrayOf(jsonNew, goalId))
            }

            db.execSQL("DROP TABLE `goals`")
            db.execSQL("ALTER TABLE `goals_new` RENAME TO `goals`")
        }
    }

val MIGRATION_15_16 =
    object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
            CREATE TABLE IF NOT EXISTS `link_items` (
                `id` TEXT NOT NULL, 
                `link_data` TEXT NOT NULL, 
                PRIMARY KEY(`id`)
            )
        """,
            )
        }
    }

val MIGRATION_16_17 =
    object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN is_attachments_expanded INTEGER NOT NULL DEFAULT 0")
        }
    }

val MIGRATION_17_18 =
    object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
            CREATE TABLE IF NOT EXISTS `inbox_records` (
                `id` TEXT NOT NULL, 
                `projectId` TEXT NOT NULL, 
                `text` TEXT NOT NULL, 
                `createdAt` INTEGER NOT NULL, 
                `item_order` INTEGER NOT NULL, 
                PRIMARY KEY(`id`), 
                FOREIGN KEY(`projectId`) REFERENCES `goal_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """,
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_inbox_records_projectId` ON `inbox_records` (`projectId`)")
        }
    }

val MIGRATION_18_19 =
    object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN is_completed INTEGER NOT NULL DEFAULT 0")
        }
    }

val MIGRATION_19_20 =
    object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE goals ADD COLUMN reminder_time INTEGER")
        }
    }

val MIGRATION_20_21 =
    object : Migration(20, 21) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE activity_records ADD COLUMN reminderTime INTEGER")
        }
    }

val MIGRATION_21_22 =
    object : Migration(21, 22) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE link_items ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        }
    }

val MIGRATION_22_23 =
    object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE activity_records ADD COLUMN target_id TEXT")
            db.execSQL("ALTER TABLE activity_records ADD COLUMN target_type TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_records_target_id` ON `activity_records` (`target_id`)")
        }
    }

val MIGRATION_23_24 =
    object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
            CREATE TABLE IF NOT EXISTS `activity_records` (
                `id` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `description` TEXT NOT NULL,
                `target_id` TEXT,
                `target_type` TEXT,
                PRIMARY KEY(`id`)
            )
        """,
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_records_target_id` ON `activity_records` (`target_id`)")

            db.execSQL("ALTER TABLE `link_items` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")

            db.execSQL("ALTER TABLE `goal_lists` ADD COLUMN `default_view_mode` TEXT NOT NULL DEFAULT 'BACKLOG'")

            db.execSQL("ALTER TABLE `goal_lists` ADD COLUMN `is_completed` INTEGER NOT NULL DEFAULT 0")
        }
    }

val MIGRATION_24_25 =
    object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `activity_records` ADD COLUMN `goal_id` TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE `activity_records` ADD COLUMN `list_id` TEXT DEFAULT NULL")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_records_goal_id` ON `activity_records` (`goal_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_records_list_id` ON `activity_records` (`list_id`)")
        }
    }

val MIGRATION_25_26 =
    object : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN is_project_management_enabled INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN project_status TEXT NOT NULL DEFAULT 'NO_PLAN'")
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN project_status_text TEXT")
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN project_log_level TEXT NOT NULL DEFAULT 'NORMAL'")
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN total_time_spent_minutes INTEGER NOT NULL DEFAULT 0")

            db.execSQL(
                """
            CREATE TABLE IF NOT EXISTS `project_execution_logs` (
                `id` TEXT NOT NULL, 
                `projectId` TEXT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                `type` TEXT NOT NULL, 
                `description` TEXT NOT NULL, 
                PRIMARY KEY(`id`), 
                FOREIGN KEY(`projectId`) REFERENCES `goal_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """,
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_execution_logs_projectId` ON `project_execution_logs` (`projectId`)")
        }
    }
val MIGRATION_26_27 =
    object : Migration(26, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
        }
    }

val MIGRATION_27_28 =
    object : Migration(27, 28) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE activity_records ADD COLUMN startTime INTEGER")
            db.execSQL("ALTER TABLE activity_records ADD COLUMN endTime INTEGER")
            db.execSQL(
                """
            CREATE TABLE chat_messages_new (
                `id` TEXT NOT NULL, 
                `text` TEXT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                `isFromUser` INTEGER NOT NULL, 
                `image_uri` TEXT, 
                PRIMARY KEY(`id`)
            )
        """,
            )
            db.execSQL(
                """
            INSERT INTO chat_messages_new (id, text, timestamp, isFromUser, image_uri)
            SELECT id, text, timestamp, isUser, image_uri FROM chat_messages
        """,
            )
            db.execSQL("DROP TABLE chat_messages")
            db.execSQL("ALTER TABLE chat_messages_new RENAME TO chat_messages")
        }
    }

val MIGRATION_28_29 =
    object : Migration(28, 29) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN reminder_time INTEGER")
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN valueImportance REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN valueImpact REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN effort REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN cost REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN risk REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN weightEffort REAL NOT NULL DEFAULT 1.0")
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN weightCost REAL NOT NULL DEFAULT 1.0")
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN weightRisk REAL NOT NULL DEFAULT 1.0")
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN rawScore REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN displayScore INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE goal_lists ADD COLUMN scoring_status TEXT NOT NULL DEFAULT 'NOT_ASSESSED'")
        }
    }
