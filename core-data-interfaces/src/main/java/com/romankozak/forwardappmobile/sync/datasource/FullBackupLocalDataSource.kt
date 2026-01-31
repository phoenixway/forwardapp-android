package com.romankozak.forwardappmobile.sync.datasource

import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.SnapshotBundle

interface FullBackupLocalDataSource {
    // === Legacy Methods ===
    suspend fun loadFullDatabaseContent(): DatabaseContent

    // Очищення та відновлення даних
    suspend fun restoreDatabaseFromBackup(content: DatabaseContent)

    // Очищення всіх таблиць
    suspend fun clearAllTables()

    // Отримання налаштувань (як Map)
    suspend fun getSettingsSnapshot(): Map<String, String>

    // Відновлення налаштувань
    suspend fun restoreSettings(settings: Map<String, String>)

    // === New Snapshot-based Methods ===
    suspend fun loadFullSnapshotBundle(): SnapshotBundle
    suspend fun applySnapshotBundle(bundle: SnapshotBundle)
}