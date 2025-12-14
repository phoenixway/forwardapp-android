package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ... (код цієї міграції залишається без змін)
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ... (код цієї міграції залишається без змін)
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goal_lists ADD COLUMN tags TEXT")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
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
        """
        )
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recent_list_entries` (
                `list_id` TEXT NOT NULL,
                `last_accessed` INTEGER NOT NULL,
                PRIMARY KEY(`list_id`),
                FOREIGN KEY(`list_id`) REFERENCES `goal_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """
        )
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- PART 1: Create new tables ---
        db.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`id` TEXT NOT NULL, `title` TEXT, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `list_items` (`id` TEXT NOT NULL, `listId` TEXT NOT NULL, `itemType` TEXT NOT NULL, `entityId` TEXT NOT NULL, `item_order` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`listId`) REFERENCES `goal_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_list_items_listId` ON `list_items` (`listId`)")

        // --- PART 2: Transfer data from GoalInstance to ListItem ---
        db.execSQL(
            """
            INSERT INTO `list_items` (id, listId, itemType, entityId, item_order)
            SELECT instance_id, listId, 'GOAL', goalId, goal_order FROM goal_instances
        """
        )

        // --- PART 3: Delete the old GoalInstance table ---
        db.execSQL("DROP TABLE `goal_instances`")

        // --- PART 4: Migration of associatedListIds -> relatedLinks in the Goal table ---
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
        """
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
                    val newRelatedLinks = oldListIds.map { listId ->
                        RelatedLink(type = LinkType.PROJECT, target = listId)
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
        """
        )

        relatedLinkList.forEach { (goalId, jsonNew) ->
            db.execSQL("UPDATE goals_new SET relatedLinks = ? WHERE id = ?", arrayOf(jsonNew, goalId))
        }

        db.execSQL("DROP TABLE `goals`")
        db.execSQL("ALTER TABLE `goals_new` RENAME TO `goals`")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `link_items` (
                `id` TEXT NOT NULL,
                `link_data` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """
        )
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goal_lists ADD COLUMN is_attachments_expanded INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
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
        """
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inbox_records_projectId` ON `inbox_records` (`projectId`)")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goal_lists ADD COLUMN is_completed INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goals ADD COLUMN reminder_time INTEGER")
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE activity_records ADD COLUMN reminderTime INTEGER")
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE link_items ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activity_records ADD COLUMN target_id TEXT")
        db.execSQL("ALTER TABLE activity_records ADD COLUMN target_type TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_records_target_id` ON `activity_records` (`target_id`)")
    }
}

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `goal_lists` ADD COLUMN `is_completed` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `activity_records` ADD COLUMN `goal_id` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `activity_records` ADD COLUMN `list_id` TEXT DEFAULT NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_records_goal_id` ON `activity_records` (`goal_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_records_list_id` ON `activity_records` (`list_id`)")
    }
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `is_favorite` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `parent_project_id` TEXT")
    }
}

val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `description` TEXT")
    }
}

val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `icon_name` TEXT")
    }
}

val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `color` INTEGER")
    }
}

val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `is_archived` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `view_type` TEXT NOT NULL DEFAULT 'LIST'")
    }
}

val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `sort_type` TEXT NOT NULL DEFAULT 'DEFAULT'")
    }
}

val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `group_by` TEXT NOT NULL DEFAULT 'NONE'")
    }
}

val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `show_completed_tasks` INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `notifications_enabled` INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_36_37 = object : Migration(36, 37) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `last_opened_at` INTEGER")
    }
}

val MIGRATION_37_38 = object : Migration(37, 38) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_38_39 = object : Migration(38, 39) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `updated_at` INTEGER")
    }
}

val MIGRATION_39_40 = object : Migration(39, 40) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `custom_fields` TEXT")
    }
}

val MIGRATION_40_41 = object : Migration(40, 41) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'IN_PROGRESS'")
    }
}

val MIGRATION_41_42 = object : Migration(41, 42) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `deadline` INTEGER")
    }
}

val MIGRATION_42_43 = object : Migration(42, 43) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `tags` TEXT")
    }
}

val MIGRATION_44_45 = object : Migration(44, 45) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `owner_id` TEXT")
    }
}

val MIGRATION_45_46 = object : Migration(45, 46) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `members` TEXT")
    }
}

val MIGRATION_46_47 = object : Migration(46, 47) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `permissions` TEXT")
    }
}

val MIGRATION_47_48 = object : Migration(47, 48) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `default_task_list_id` TEXT")
    }
}

val MIGRATION_48_49 = object : Migration(48, 49) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `completed_at` INTEGER")
    }
}

val MIGRATION_49_50 = object : Migration(49, 50) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `progress` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_50_51 = object : Migration(50, 51) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `budget` REAL")
    }
}

val MIGRATION_51_52 = object : Migration(51, 52) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `spent` REAL")
    }
}

val MIGRATION_52_53 = object : Migration(52, 53) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `is_private` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_53_54 = object : Migration(53, 54) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `is_template` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_54_55 = object : Migration(54, 55) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `template_id` TEXT")
    }
}

val MIGRATION_55_56 = object : Migration(55, 56) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE projects ADD COLUMN relatedLinks TEXT")
    }
}

val MIGRATION_57_58 = object : Migration(57, 58) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Empty migration for schema changes
    }
}

val MIGRATION_58_59 = object : Migration(58, 59) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Empty migration for schema changes
    }
}

val MIGRATION_59_60 = object : Migration(59, 60) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateSpecialProjects(db)
    }
}

val MIGRATION_60_61 = object : Migration(60, 61) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `note_documents` (
                `id` TEXT NOT NULL,
                `projectId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `content` TEXT,
                `lastCursorPosition` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `note_documents` (id, projectId, name, createdAt, updatedAt, content, lastCursorPosition)
            SELECT id, projectId, name, createdAt, updatedAt, content, lastCursorPosition
            FROM `custom_lists`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `custom_lists`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_documents_projectId` ON `note_documents` (`projectId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `note_document_items` (
                `id` TEXT NOT NULL,
                `listId` TEXT NOT NULL,
                `parentId` TEXT,
                `content` TEXT NOT NULL,
                `isCompleted` INTEGER NOT NULL,
                `itemOrder` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`listId`) REFERENCES `note_documents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`parentId`) REFERENCES `note_document_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `note_document_items` (id, listId, parentId, content, isCompleted, itemOrder, createdAt, updatedAt)
            SELECT id, listId, parentId, content, isCompleted, itemOrder, createdAt, updatedAt
            FROM `custom_list_items`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `custom_list_items`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_document_items_listId` ON `note_document_items` (`listId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_document_items_parentId` ON `note_document_items` (`parentId`)")

        db.execSQL("UPDATE `list_items` SET `itemType` = 'NOTE_DOCUMENT' WHERE `itemType` = 'CUSTOM_LIST'")
    }
}

val MIGRATION_61_62 = object : Migration(61, 62) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `checklists` (
                `id` TEXT NOT NULL,
                `projectId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_checklists_projectId` ON `checklists` (`projectId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `checklist_items` (
                `id` TEXT NOT NULL,
                `checklistId` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `isChecked` INTEGER NOT NULL DEFAULT 0,
                `itemOrder` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`checklistId`) REFERENCES `checklists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_checklist_items_checklistId` ON `checklist_items` (`checklistId`)")
    }
}

val MIGRATION_62_63 = object : Migration(62, 63) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE `daily_metrics`
            ADD COLUMN `completedPoints` INTEGER NOT NULL DEFAULT 0
            """.trimIndent(),
        )
    }
}

val MIGRATION_63_64 = object : Migration(63, 64) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `attachments` (
                `id` TEXT NOT NULL,
                `attachment_type` TEXT NOT NULL,
                `entity_id` TEXT NOT NULL,
                `owner_project_id` TEXT,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_attachments_attachment_type`
            ON `attachments`(`attachment_type`)
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_attachments_entity_id`
            ON `attachments`(`entity_id`)
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `project_attachment_cross_ref` (
                `project_id` TEXT NOT NULL,
                `attachment_id` TEXT NOT NULL,
                `attachment_order` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`project_id`, `attachment_id`),
                FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`attachment_id`) REFERENCES `attachments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_project_attachment_attachment_id`
            ON `project_attachment_cross_ref`(`attachment_id`)
            """.trimIndent(),
        )

        db.execSQL(
            """
            INSERT INTO `attachments` (id, attachment_type, entity_id, owner_project_id, createdAt, updatedAt)
            SELECT
                id,
                itemType,
                entityId,
                project_id,
                CASE WHEN item_order < 0 THEN -item_order ELSE item_order END,
                CASE WHEN item_order < 0 THEN -item_order ELSE item_order END
            FROM `list_items`
            WHERE itemType IN ('LINK_ITEM', 'NOTE_DOCUMENT', 'CHECKLIST')
            """.trimIndent(),
        )

        db.execSQL(
            """
            INSERT INTO `project_attachment_cross_ref` (project_id, attachment_id, attachment_order)
            SELECT
                project_id,
                id,
                item_order
            FROM `list_items`
            WHERE itemType IN ('LINK_ITEM', 'NOTE_DOCUMENT', 'CHECKLIST')
            """.trimIndent(),
        )

        db.execSQL(
            """
            DELETE FROM `list_items`
            WHERE itemType IN ('LINK_ITEM', 'NOTE_DOCUMENT', 'CHECKLIST')
            """.trimIndent(),
        )
    }
}

val MIGRATION_64_65 = object : Migration(64, 65) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateSpecialProjects(db)
    }
}

val MIGRATION_65_66 = object : Migration(65, 66) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `system_key` TEXT")
        migrateSpecialProjects(db)
    }
}

val MIGRATION_66_67 = object : Migration(66, 67) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `system_apps` (
                `id` TEXT NOT NULL,
                `system_key` TEXT NOT NULL,
                `app_type` TEXT NOT NULL,
                `project_id` TEXT NOT NULL,
                `note_document_id` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`note_document_id`) REFERENCES `note_documents`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_system_apps_system_key` ON `system_apps` (`system_key`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_system_apps_project_id` ON `system_apps` (`project_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_system_apps_note_document_id` ON `system_apps` (`note_document_id`)")
    }
}

val MIGRATION_67_68 = object : Migration(67, 68) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `scripts` (
                `id` TEXT NOT NULL,
                `projectId` TEXT,
                `name` TEXT NOT NULL,
                `description` TEXT,
                `content` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_scripts_projectId` ON `scripts` (`projectId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_scripts_name` ON `scripts` (`name`)")
    }
}

val MIGRATION_68_69 = object : Migration(68, 69) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `synced_at` INTEGER")
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `goals` ADD COLUMN `synced_at` INTEGER")
        db.execSQL("ALTER TABLE `goals` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `goals` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `list_items` ADD COLUMN `synced_at` INTEGER")
        db.execSQL("ALTER TABLE `list_items` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `list_items` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_69_70 = object : Migration(69, 70) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `notes` ADD COLUMN `syncedAt` INTEGER")
        db.execSQL("ALTER TABLE `notes` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `notes` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `note_documents` ADD COLUMN `syncedAt` INTEGER")
        db.execSQL("ALTER TABLE `note_documents` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `note_documents` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `note_document_items` ADD COLUMN `syncedAt` INTEGER")
        db.execSQL("ALTER TABLE `note_document_items` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `note_document_items` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `checklists` ADD COLUMN `syncedAt` INTEGER")
        db.execSQL("ALTER TABLE `checklists` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `checklists` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `checklist_items` ADD COLUMN `syncedAt` INTEGER")
        db.execSQL("ALTER TABLE `checklist_items` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `checklist_items` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `activity_records` ADD COLUMN `syncedAt` INTEGER")
        db.execSQL("ALTER TABLE `activity_records` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `activity_records` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `inbox_records` ADD COLUMN `synced_at` INTEGER")
        db.execSQL("ALTER TABLE `inbox_records` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `inbox_records` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `link_items` ADD COLUMN `synced_at` INTEGER")
        db.execSQL("ALTER TABLE `link_items` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `link_items` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `project_execution_logs` ADD COLUMN `synced_at` INTEGER")
        db.execSQL("ALTER TABLE `project_execution_logs` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `project_execution_logs` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `attachments` ADD COLUMN `syncedAt` INTEGER")
        db.execSQL("ALTER TABLE `attachments` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `attachments` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `project_attachment_cross_ref` ADD COLUMN `syncedAt` INTEGER")
        db.execSQL("ALTER TABLE `project_attachment_cross_ref` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `project_attachment_cross_ref` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `scripts` ADD COLUMN `syncedAt` INTEGER")
        db.execSQL("ALTER TABLE `scripts` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `scripts` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_70_71 = object : Migration(70, 71) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `list_items` ADD COLUMN `updatedAt` INTEGER")
        db.execSQL("UPDATE `list_items` SET `updatedAt` = (strftime('%s','now') * 1000)")

        db.execSQL("ALTER TABLE `checklists` ADD COLUMN `updatedAt` INTEGER")
        db.execSQL("UPDATE `checklists` SET `updatedAt` = (strftime('%s','now') * 1000)")

        db.execSQL("ALTER TABLE `checklist_items` ADD COLUMN `updatedAt` INTEGER")
        db.execSQL("UPDATE `checklist_items` SET `updatedAt` = (strftime('%s','now') * 1000)")

        db.execSQL("ALTER TABLE `inbox_records` ADD COLUMN `updatedAt` INTEGER")
        db.execSQL("UPDATE `inbox_records` SET `updatedAt` = `createdAt`")

        db.execSQL("ALTER TABLE `activity_records` ADD COLUMN `updatedAt` INTEGER")
        db.execSQL("UPDATE `activity_records` SET `updatedAt` = COALESCE(`endTime`, `startTime`, `createdAt`)")

        db.execSQL("ALTER TABLE `link_items` ADD COLUMN `updatedAt` INTEGER")
        db.execSQL("UPDATE `link_items` SET `updatedAt` = `createdAt`")

        db.execSQL("ALTER TABLE `project_execution_logs` ADD COLUMN `updatedAt` INTEGER")
        db.execSQL("UPDATE `project_execution_logs` SET `updatedAt` = `timestamp`")

        db.execSQL("ALTER TABLE `project_attachment_cross_ref` ADD COLUMN `updatedAt` INTEGER")
        db.execSQL("UPDATE `project_attachment_cross_ref` SET `updatedAt` = ABS(`attachment_order`)")
    }
}

val MIGRATION_71_72 = object : Migration(71, 72) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        // Day plans
        db.execSQL("ALTER TABLE `day_plans` ADD COLUMN `syncedAt` INTEGER")
        db.execSQL("ALTER TABLE `day_plans` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `day_plans` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `day_plans` SET `version` = 1, `updatedAt` = COALESCE(`updatedAt`, `createdAt`, $now)")

        // Day tasks
        db.execSQL("ALTER TABLE `day_tasks` ADD COLUMN `syncedAt` INTEGER")
        db.execSQL("ALTER TABLE `day_tasks` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `day_tasks` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `day_tasks` SET `version` = 1 WHERE `version` = 0")
        db.execSQL("UPDATE `day_tasks` SET `updatedAt` = COALESCE(`updatedAt`, `createdAt`, $now)")

        // Daily metrics
        db.execSQL("ALTER TABLE `daily_metrics` ADD COLUMN `syncedAt` INTEGER")
        db.execSQL("ALTER TABLE `daily_metrics` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `daily_metrics` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `daily_metrics` SET `version` = 1 WHERE `version` = 0")
        db.execSQL("UPDATE `daily_metrics` SET `updatedAt` = COALESCE(`updatedAt`, `createdAt`, $now)")

        // Reminders
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `updatedAt` INTEGER")
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `syncedAt` INTEGER")
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `reminders` SET `updatedAt` = COALESCE(`updatedAt`, `creationTime`, $now)")
        db.execSQL("UPDATE `reminders` SET `version` = 1 WHERE `version` = 0")
    }
}

val MIGRATION_72_73 = object : Migration(72, 73) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Step 1: Delete duplicate system_key entries (keep the first one)
        db.execSQL(
            """
            DELETE FROM `projects`
            WHERE `id` NOT IN (
                SELECT MIN(`id`) FROM `projects`
                WHERE `system_key` IS NOT NULL
                GROUP BY `system_key`
            )
            AND `system_key` IS NOT NULL
            """.trimIndent()
        )

        // Step 2: Create the unique index on system_key
        // This enforces uniqueness at the database level
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `idx_projects_systemkey_unique` ON `projects` (`system_key`)
            """.trimIndent()
        )
    }
}

val MIGRATION_73_74 = object : Migration(73, 74) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Re-run special projects reconciliation to ensure system_key and reserved_group are present.
        migrateSpecialProjects(db)
    }
}

val MIGRATION_74_75 = object : Migration(74, 75) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateSpecialProjects(db)
        // Hard-set system_key/reserved_group/parentId for all reserved projects to ensure consistency.
        val updates = listOf(
            Triple("personal-management", null, "personal-management"),
            Triple("strategic", "personal-management", "strategic_group"),
            Triple("strategic-beacons", "strategic", "main_beacons_group"),
            Triple("week", "personal-management", "strategic"),
            Triple("today", "personal-management", "inbox"),
            Triple("main-beacons", "personal-management", "main_beacons"),
            Triple("mission", "strategic-beacons", "main_beacons"),
            Triple("long-term-strategy", "strategic-beacons", "strategic"),
            Triple("strategic-programs", "strategic-beacons", "strategic"),
            Triple("medium-term-strategy", "personal-management", "strategic"),
            Triple("active-quests", "week", "strategic"),
            Triple("strategic-inbox", "strategic", "strategic"),
            Triple("strategic-review", "strategic", "strategic"),
            Triple("inbox", "today", "inbox"),
        )
        updates.forEach { (key, parentName, reservedGroup) ->
            val parentIdClause = if (parentName != null) {
                "parentId = (SELECT id FROM projects WHERE name = '$parentName' LIMIT 1)"
            } else {
                "parentId IS NULL"
            }
            db.execSQL(
                """
                UPDATE projects
                   SET system_key = '$key',
                       reserved_group = '$reservedGroup'
                 WHERE name = '$key'
                   AND $parentIdClause
                """.trimIndent()
            )
        }
    }
}

val MIGRATION_75_76 = object : Migration(75, 76) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateSpecialProjects(db)
        // Force set system_key/reserved_group even if names differ: use ReservedProjectKeys mapping.
        val mapping = mapOf(
            "personal-management" to Triple(null, "personal-management", "SYSTEM"),
            "strategic" to Triple("personal-management", "strategic_group", "RESERVED"),
            "strategic-beacons" to Triple("strategic", "main_beacons_group", "RESERVED"),
            "week" to Triple("personal-management", "strategic", "RESERVED"),
            "today" to Triple("personal-management", "inbox", "RESERVED"),
            "main-beacons" to Triple("personal-management", "main_beacons", "RESERVED"),
            "mission" to Triple("strategic-beacons", "main_beacons", "RESERVED"),
            "long-term-strategy" to Triple("strategic-beacons", "strategic", "RESERVED"),
            "strategic-programs" to Triple("strategic-beacons", "strategic", "RESERVED"),
            "medium-term-strategy" to Triple("personal-management", "strategic", "RESERVED"),
            "active-quests" to Triple("week", "strategic", "RESERVED"),
            "strategic-inbox" to Triple("strategic", "strategic", "RESERVED"),
            "strategic-review" to Triple("strategic", "strategic", "RESERVED"),
            "inbox" to Triple("today", "inbox", "RESERVED"),
        )
        mapping.forEach { (key, triple) ->
            val (parentName, reservedGroup, projectType) = triple
            val parentClause = if (parentName == null) "parentId IS NULL" else "parentId = (SELECT id FROM projects WHERE system_key = '$parentName' OR name = '$parentName' LIMIT 1)"
            db.execSQL(
                """
                UPDATE projects
                   SET system_key = '$key',
                       reserved_group = '$reservedGroup',
                       project_type = '$projectType'
                 WHERE (system_key IS NULL OR system_key != '$key')
                   AND name = '$key'
                   AND $parentClause
                """.trimIndent()
            )
        }
    }
}

val MIGRATION_76_77 = object : Migration(76, 77) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `backlog_orders` (
                `id` TEXT NOT NULL,
                `list_id` TEXT NOT NULL,
                `item_id` TEXT NOT NULL,
                `item_order` INTEGER NOT NULL,
                `order_version` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER,
                `synced_at` INTEGER,
                `is_deleted` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`list_id`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_backlog_orders_list_id` ON `backlog_orders` (`list_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_backlog_orders_list_item` ON `backlog_orders` (`list_id`, `item_id`)")
        // Seed from existing list_items to preserve current order
        db.execSQL(
            """
            INSERT OR REPLACE INTO backlog_orders (id, list_id, item_id, item_order, order_version, updatedAt, synced_at, is_deleted)
            SELECT li.id,
                   li.project_id,
                   li.entityId,
                   li.item_order,
                   COALESCE(li.version, li.updatedAt, 0),
                   li.updatedAt,
                   li.synced_at,
                   li.is_deleted
              FROM list_items li
            """.trimIndent()
        )
    }
}

val MIGRATION_79_80 = object : Migration(79, 80) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activity_records ADD COLUMN xp_gained INTEGER")
    }
}

val MIGRATION_80_81 = object : Migration(80, 81) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activity_records ADD COLUMN anty_xp INTEGER")
    }
}

val MIGRATION_81_82 = object : Migration(81, 82) {
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
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_structure_presets_code` ON `structure_presets` (`code`)")
    }
}

val MIGRATION_82_83 = object : Migration(82, 83) {
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
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_structure_preset_items_presetId` ON `structure_preset_items` (`presetId`)")
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_structure_preset_items_role_per_preset`
            ON `structure_preset_items` (`presetId`, `roleCode`)
            """.trimIndent()
        )
    }
}

val MIGRATION_83_84 = object : Migration(83, 84) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE attachments ADD COLUMN role_code TEXT")
        db.execSQL("ALTER TABLE attachments ADD COLUMN is_system INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE projects ADD COLUMN role_code TEXT")
    }
}

val MIGRATION_84_85 = object : Migration(84, 85) {
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
            """.trimIndent()
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
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_structures_projectId` ON `project_structures` (`projectId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_structure_items_projectStructureId` ON `project_structure_items` (`projectStructureId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_project_structure_items_role_per_structure` ON `project_structure_items` (`projectStructureId`, `roleCode`)")
    }
}

val MIGRATION_85_86 = object : Migration(85, 86) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS `index_project_structures_projectId`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_project_structures_projectId` ON `project_structures` (`projectId`)")
    }
}

val MIGRATION_86_87 = object : Migration(86, 87) {
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

val MIGRATION_87_88 = object : Migration(87, 88) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE structure_presets ADD COLUMN enable_dashboard INTEGER")
        db.execSQL("ALTER TABLE structure_presets ADD COLUMN enable_backlog INTEGER")
        db.execSQL("ALTER TABLE project_structures ADD COLUMN enable_dashboard INTEGER")
        db.execSQL("ALTER TABLE project_structures ADD COLUMN enable_backlog INTEGER")
    }
}

val MIGRATION_88_89 = object : Migration(88, 89) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE structure_presets ADD COLUMN enable_attachments INTEGER")
        db.execSQL("ALTER TABLE project_structures ADD COLUMN enable_attachments INTEGER")
    }
}
