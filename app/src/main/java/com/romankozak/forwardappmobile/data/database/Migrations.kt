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
        // Крок 1: Додаємо всі нові колонки, яких ще не існує в схемі версії 8.
        // Автоматична міграція вже могла додати деякі з них, але для надійності
        // ми можемо додати їх з умовою `IF NOT EXISTS` або просто переконатись, що вони є.
        // Оскільки Room керує схемою, ми довіряємо, що поля з моделі Goal будуть додані.
        // Наша головна задача - правильно заповнити їх.

        // Крок 2: Отримуємо всі існуючі цілі з бази даних
        val cursor = db.query("SELECT * FROM goals", emptyArray())
        if (cursor.moveToFirst()) {
            do {
                // Читаємо ID та значення зі СТАРИХ колонок (назви з версії 8)
                val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                val oldImportance = cursor.getFloat(cursor.getColumnIndexOrThrow("parentValueImportance"))
                val oldImpact = cursor.getFloat(cursor.getColumnIndexOrThrow("impactOnParentGoal"))
                val oldTimeCost = cursor.getFloat(cursor.getColumnIndexOrThrow("timeCost"))
                val oldFinancialCost = cursor.getFloat(cursor.getColumnIndexOrThrow("financialCost"))
                val oldRisk = cursor.getFloat(cursor.getColumnIndexOrThrow("risk"))

                // Крок 3: Створюємо тимчасовий об'єкт Goal для розрахунку
                val tempGoal = Goal(
                    id = id,
                    text = "", description = null, completed = false, createdAt = 0, updatedAt = 0, // Неважливі для розрахунку
                    valueImportance = oldImportance, // Переносимо старе значення
                    valueImpact = oldImpact,         // Переносимо старе значення
                    effort = oldTimeCost,            // Зусилля беремо зі старого часу
                    cost = oldFinancialCost,         // Вартість беремо зі старих фінансів
                    risk = oldRisk                   // Ризик залишається
                )

                // Крок 4: Використовуємо нашу логіку для отримання нових оцінок
                val updatedGoal = GoalScoringManager.calculateScores(tempGoal)

                // Крок 5: Оновлюємо поточний рядок, записуючи дані в НОВІ колонки
                db.execSQL("""
                    UPDATE goals SET
                    valueImportance = ${updatedGoal.valueImportance},
                    valueImpact = ${updatedGoal.valueImpact},
                    effort = ${updatedGoal.effort},
                    cost = ${updatedGoal.cost},
                    rawScore = ${updatedGoal.rawScore},
                    displayScore = ${updatedGoal.displayScore}
                    WHERE id = "$id"
                """.trimIndent())

            } while (cursor.moveToNext())
        }
        cursor.close()
    }
}

/**
 * ✨ ДОДАНО: Міграція бази даних з версії 10 на 11.
 * Впроваджує поле статусу оцінки цілі (неоціненно, неможливо-оцінити, оцінено).
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Крок 1: Додати нову колонку зі значенням за замовчуванням 'NOT_ASSESSED'.
        db.execSQL("""
            ALTER TABLE goals
            ADD COLUMN scoring_status TEXT NOT NULL DEFAULT 'NOT_ASSESSED'
        """.trimIndent())

        // Крок 2: Оновити статус на 'ASSESSED' для існуючих цілей,
        // які вже були оцінені (де важливість та вплив задані).
        db.execSQL("""
            UPDATE goals
            SET scoring_status = 'ASSESSED'
            WHERE valueImportance > 0 AND valueImpact > 0
        """.trimIndent())
    }
}