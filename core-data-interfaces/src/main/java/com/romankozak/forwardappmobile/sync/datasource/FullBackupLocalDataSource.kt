package com.romankozak.forwardappmobile.sync.datasource

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.CanonicalExecutionLogSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceDirectionEntrySnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceDirectionEntrySyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.CanonicalRecurringSeriesSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectOrientationRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.SavedOrientationViewEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity

data class CanonicalExecutionLogSyncVersion(
    val id: String,
    val version: Long,
)

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

data class CanonicalOrientationSyncPayload(
    val managedSubjects: List<ManagedSubjectEntity> = emptyList(),
    val orientations: List<OrientationEntity> = emptyList(),
    val aspects: List<AspectEntity> = emptyList(),
    val assessments: List<OrientationAssessmentEntity> = emptyList(),
    val assessmentRevisions: List<OrientationAssessmentRevisionEntity> = emptyList(),
    val legacyMappings: List<LegacySubjectMappingEntity> = emptyList(),
    val relations: List<OrientationRelationEntity> = emptyList(),
    val aspectRefs: List<AspectOrientationRefEntity> = emptyList(),
    val workspaces: List<WorkspaceEntity> = emptyList(),
    val workspaceBindings: List<WorkspaceBindingEntity> = emptyList(),
    val workspaceCapabilities: List<WorkspaceCapabilityInstanceEntity> = emptyList(),
    val savedViews: List<SavedOrientationViewEntity> = emptyList(),
)

data class CanonicalOrientationSyncVersion(val id: String, val version: Long)

data class CanonicalOrientationSyncAck(
    val managedSubjects: List<CanonicalOrientationSyncVersion> = emptyList(),
    val assessments: List<CanonicalOrientationSyncVersion> = emptyList(),
    val assessmentRevisions: List<CanonicalOrientationSyncVersion> = emptyList(),
    val legacyMappings: List<CanonicalOrientationSyncVersion> = emptyList(),
    val relations: List<CanonicalOrientationSyncVersion> = emptyList(),
    val aspectRefs: List<CanonicalOrientationSyncVersion> = emptyList(),
    val workspaces: List<CanonicalOrientationSyncVersion> = emptyList(),
    val workspaceBindings: List<CanonicalOrientationSyncVersion> = emptyList(),
    val workspaceCapabilities: List<CanonicalOrientationSyncVersion> = emptyList(),
    val savedViews: List<CanonicalOrientationSyncVersion> = emptyList(),
)

interface FullBackupLocalDataSource {
    // === Legacy Methods ===
    suspend fun clearAllTables()

    suspend fun getSettingsSnapshot(): Map<String, String>

    suspend fun loadUnsyncedCanonicalRecurringSeries(): List<CanonicalRecurringSeriesSnapshot>

    suspend fun loadCanonicalRecurringSeriesChangedSince(timestamp: Long): List<CanonicalRecurringSeriesSnapshot>

    suspend fun markCanonicalRecurringSeriesSynced(series: List<CanonicalRecurringSeriesSyncVersion>)

    suspend fun loadUnsyncedCanonicalDayThemes(): CanonicalDayThemeSyncPayload

    suspend fun loadCanonicalDayThemesChangedSince(timestamp: Long): CanonicalDayThemeSyncPayload

    suspend fun markCanonicalDayThemesSynced(ack: CanonicalDayThemeSyncAck)

    suspend fun loadUnsyncedCanonicalOrientations(): CanonicalOrientationSyncPayload

    suspend fun markCanonicalOrientationsSynced(ack: CanonicalOrientationSyncAck)

    suspend fun loadUnsyncedCanonicalExecutionLogs(): List<CanonicalExecutionLogSnapshot>

    suspend fun loadCanonicalExecutionLogsChangedSince(timestamp: Long): List<CanonicalExecutionLogSnapshot>

    suspend fun markCanonicalExecutionLogsSynced(logs: List<CanonicalExecutionLogSyncVersion>)

    suspend fun loadUnsyncedCanonicalWorkspaceDirectionEntries(): List<WorkspaceDirectionEntrySnapshot>

    suspend fun loadCanonicalWorkspaceDirectionEntriesChangedSince(timestamp: Long): List<WorkspaceDirectionEntrySnapshot>

    suspend fun markCanonicalWorkspaceDirectionEntriesSynced(entries: List<WorkspaceDirectionEntrySyncVersion>)

    suspend fun restoreSettings(settings: Map<String, String>)

    // === New Snapshot-based Methods ===
    suspend fun loadFullSnapshotBundle(): SnapshotBundle

    suspend fun applySnapshotBundle(bundle: SnapshotBundle)
}
