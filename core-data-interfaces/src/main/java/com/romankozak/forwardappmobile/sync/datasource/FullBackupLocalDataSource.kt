package com.romankozak.forwardappmobile.sync.datasource

import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.CanonicalRecurringSeriesSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot

data class CanonicalRecurringSeriesSyncVersion(
    val id: String,
    val version: Long,
)

data class CanonicalDayThemeSyncPayload(
    val themeDefinitions: List<ThemeDefinitionSnapshot> = emptyList(),
    val dayThemes: List<DayThemeSnapshot> = emptyList(),
    val assignmentDocuments: List<DayThemeAssignmentDocumentSnapshot> = emptyList(),
)

data class CanonicalDayThemeSyncVersion(
    val id: String,
    val version: Long,
)

data class CanonicalDayThemeSyncAck(
    val themeDefinitions: List<CanonicalDayThemeSyncVersion> = emptyList(),
    val dayThemes: List<CanonicalDayThemeSyncVersion> = emptyList(),
    val assignmentDocuments: List<CanonicalDayThemeSyncVersion> = emptyList(),
)

interface FullBackupLocalDataSource {
    // === Legacy Methods ===
    suspend fun loadFullDatabaseContent(): DatabaseContent

    suspend fun restoreDatabaseFromBackup(content: DatabaseContent)

    suspend fun clearAllTables()

    suspend fun getSettingsSnapshot(): Map<String, String>

    suspend fun loadUnsyncedCanonicalRecurringSeries(): List<CanonicalRecurringSeriesSnapshot>

    suspend fun loadCanonicalRecurringSeriesChangedSince(timestamp: Long): List<CanonicalRecurringSeriesSnapshot>

    suspend fun markCanonicalRecurringSeriesSynced(series: List<CanonicalRecurringSeriesSyncVersion>)

    suspend fun loadUnsyncedCanonicalDayThemes(): CanonicalDayThemeSyncPayload

    suspend fun loadCanonicalDayThemesChangedSince(timestamp: Long): CanonicalDayThemeSyncPayload

    suspend fun markCanonicalDayThemesSynced(ack: CanonicalDayThemeSyncAck)

    suspend fun restoreSettings(settings: Map<String, String>)

    // === New Snapshot-based Methods ===
    suspend fun loadFullSnapshotBundle(): SnapshotBundle

    suspend fun applySnapshotBundle(bundle: SnapshotBundle)
}
