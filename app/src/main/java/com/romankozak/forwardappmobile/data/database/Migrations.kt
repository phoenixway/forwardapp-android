package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink

/**
 * Міграція бази даних з версії 8 на 9.
 * Впроваджує нову систему оцінки цілей (Система Б).
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ... (код цієї міграції залишається без змін)
    }
}

/**
 * Міграція бази даних з версії 10 на 11.
 * Впроваджує поле статусу оцінки цілі (неоціненно, неможливо-оцінити, оцінено).
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ... (код цієї міграції залишається без змін)
    }
}

/**
 * Міграція бази даних з версії 11 на 12.
 * Додає підтримку тегів для списків цілей (GoalList).
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goal_lists ADD COLUMN tags TEXT")
    }
}

/**
 * Міграція бази даних з версії 12 на 13.
 * Додає таблицю для відстеження активності.
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `activity_records` (
                `id` TEXT NOT NULL, 
                `text` TEXT NOT NULL, 
                `createdAt` INTEGER NOT NULL, 
                `startTime` INTEGER, 
                `endTime` INTEGER, 
                PRIMARY KEY(`id`)
            )
        """)
    }
}

/**
 * Міграція бази даних з версії 13 на 14.
 * Додає таблицю для зберігання історії нещодавно відкритих списків.
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `recent_list_entries` (
                `list_id` TEXT NOT NULL, 
                `last_accessed` INTEGER NOT NULL, 
                PRIMARY KEY(`list_id`), 
                FOREIGN KEY(`list_id`) REFERENCES `goal_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- ЧАСТИНА 1: Створення нових таблиць ---
        db.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`id` TEXT NOT NULL, `title` TEXT, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `list_items` (`id` TEXT NOT NULL, `listId` TEXT NOT NULL, `itemType` TEXT NOT NULL, `entityId` TEXT NOT NULL, `item_order` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`listId`) REFERENCES `goal_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_list_items_listId` ON `list_items` (`listId`)")

        // --- ЧАСТИНА 2: Перенесення даних з GoalInstance до ListItem ---
        db.execSQL("""
            INSERT INTO `list_items` (id, listId, itemType, entityId, item_order)
            SELECT instance_id, listId, 'GOAL', goalId, goal_order FROM goal_instances
        """)

        // --- ЧАСТИНА 3: Видалення старої таблиці GoalInstance ---
        db.execSQL("DROP TABLE `goal_instances`")

        // --- ЧАСТИНА 4: Міграція associatedListIds -> relatedLinks у таблиці Goal ---
        db.execSQL("""
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
        """)

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
                        RelatedLink(type = LinkType.GOAL_LIST, target = listId)
                    }
                    val jsonNew = gson.toJson(newRelatedLinks)
                    relatedLinkList.add(goalId to jsonNew)
                }
            } while (cursor.moveToNext())
        }
        cursor.close()

        db.execSQL("""
            INSERT INTO `goals_new` (id, text, description, completed, createdAt, updatedAt, tags, valueImportance, valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore, scoring_status, parentValueImportance, impactOnParentGoal, timeCost, financialCost)
            SELECT id, text, description, completed, createdAt, updatedAt, tags, valueImportance, valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore, scoring_status, parentValueImportance, impactOnParentGoal, timeCost, financialCost FROM `goals`
        """)

        relatedLinkList.forEach { (goalId, jsonNew) ->
            db.execSQL("UPDATE goals_new SET relatedLinks = ? WHERE id = ?", arrayOf(jsonNew, goalId))
        }

        db.execSQL("DROP TABLE `goals`")
        db.execSQL("ALTER TABLE `goals_new` RENAME TO `goals`")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `link_items` (
                `id` TEXT NOT NULL, 
                `link_data` TEXT NOT NULL, 
                PRIMARY KEY(`id`)
            )
        """)
    }
}

// ПЕРЕВІРТЕ ЦЮ МІГРАЦІЮ
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // SQL-запит для додавання нової колонки.
        // INTEGER NOT NULL DEFAULT 0 - це точна відповідність для поля Boolean = false в Kotlin
        db.execSQL("ALTER TABLE goal_lists ADD COLUMN is_attachments_expanded INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `inbox_records` (
                `id` TEXT NOT NULL, 
                `projectId` TEXT NOT NULL, 
                `text` TEXT NOT NULL, 
                `createdAt` INTEGER NOT NULL, 
                `item_order` INTEGER NOT NULL, 
                PRIMARY KEY(`id`), 
                FOREIGN KEY(`projectId`) REFERENCES `goal_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inbox_records_projectId` ON `inbox_records` (`projectId`)")
    }

}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goal_lists ADD COLUMN is_completed INTEGER NOT NULL DEFAULT 0")
    }
}
