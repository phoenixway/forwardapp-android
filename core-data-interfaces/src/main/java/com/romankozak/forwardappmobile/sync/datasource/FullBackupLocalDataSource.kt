package com.romankozak.forwardappmobile.sync.datasource

import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent

interface FullBackupLocalDataSource {
    // Завантаження всіх даних для експорту
    suspend fun loadFullDatabaseContent(): DatabaseContent

    // Очищення та відновлення даних
    suspend fun restoreDatabaseFromBackup(content: DatabaseContent)

    // Очищення всіх таблиць
    suspend fun clearAllTables()

    // Отримання налаштувань (як Map)
    suspend fun getSettingsSnapshot(): Map<String, String>

    // Відновлення налаштувань
    suspend fun restoreSettings(settings: Map<String, String>)
}