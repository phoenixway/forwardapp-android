package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.logic.GoalScoringManager

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
 * ✨ ДОДАНО: Міграція бази даних з версії 11 на 12.
 * Додає підтримку тегів для списків цілей (GoalList).
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Просто додаємо нову колонку 'tags' до таблиці 'goal_lists'.
        // Оскільки поле в моделі є nullable, SQLite за замовчуванням заповнить
        // існуючі рядки значенням NULL, що нам і потрібно.
        db.execSQL("ALTER TABLE goal_lists ADD COLUMN tags TEXT")
    }
}