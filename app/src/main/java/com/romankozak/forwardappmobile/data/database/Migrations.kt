package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
 * ✨ ДОДАЙТЕ ЦЕЙ КОД: Міграція бази даних з версії 13 на 14.
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