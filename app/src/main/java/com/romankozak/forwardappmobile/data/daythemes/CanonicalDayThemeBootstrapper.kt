package com.romankozak.forwardappmobile.data.daythemes

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot
import com.romankozak.forwardappmobile.data.dao.CanonicalDayThemeDao
import com.romankozak.forwardappmobile.data.dao.DayThemeDocumentDao
import com.romankozak.forwardappmobile.data.database.DayThemeCanonicalBootstrapStateEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanonicalDayThemeBootstrapper
    @Inject
    constructor(
        private val database: AppDatabase,
        private val legacyDao: DayThemeDocumentDao,
        private val canonicalDao: CanonicalDayThemeDao,
    ) {
        private val mutex = Mutex()

        suspend fun ensureBootstrapped(): CanonicalDayThemeBootstrapReport =
            mutex.withLock {
                database.withTransaction {
                    val completedVersion = canonicalDao.getBootstrapVersion()
                    if (completedVersion != null && completedVersion >= CURRENT_BOOTSTRAP_VERSION) {
                        return@withTransaction CanonicalDayThemeBootstrapReport.alreadyComplete()
                    }

                    val legacyDocuments = legacyDao.getAllSync().map { it.toSnapshot() }
                    val existingDefinitions = canonicalDao.getAllThemeDefinitionsSync().map { it.toCanonicalSnapshot() }
                    val existingDayThemes = canonicalDao.getAllDayThemesSync().map { it.toCanonicalSnapshot() }
                    val existingAssignmentDocuments = canonicalDao.getAllAssignmentDocumentsSync().map { it.toCanonicalSnapshot() }

                    val migrated =
                        LegacyDayThemeCanonicalMigrationMapper.migrate(
                            legacyDocuments = legacyDocuments,
                            existingThemeDefinitions = existingDefinitions,
                        )

                    val rows =
                        planCanonicalDayThemeBootstrapRows(
                            migrated = migrated,
                            existingThemeDefinitions = existingDefinitions,
                            existingDayThemes = existingDayThemes,
                            existingAssignmentDocuments = existingAssignmentDocuments,
                        )

                    if (rows.themeDefinitions.isNotEmpty()) {
                        canonicalDao.upsertThemeDefinitions(rows.themeDefinitions.map { it.toCanonicalEntity() })
                    }
                    if (rows.dayThemes.isNotEmpty()) {
                        canonicalDao.upsertDayThemes(rows.dayThemes.map { it.toCanonicalEntity() })
                    }
                    if (rows.assignmentDocuments.isNotEmpty()) {
                        canonicalDao.upsertAssignmentDocuments(rows.assignmentDocuments.map { it.toCanonicalEntity() })
                    }

                    canonicalDao.upsertBootstrapState(
                        DayThemeCanonicalBootstrapStateEntity(
                            version = CURRENT_BOOTSTRAP_VERSION,
                            completedAt = System.currentTimeMillis(),
                        ),
                    )

                    CanonicalDayThemeBootstrapReport(
                        performed = true,
                        insertedThemeDefinitions = rows.themeDefinitions.size,
                        insertedDayThemes = rows.dayThemes.size,
                        insertedAssignmentDocuments = rows.assignmentDocuments.size,
                        diagnostics = migrated.diagnostics,
                    )
                }
            }

        companion object {
            internal const val CURRENT_BOOTSTRAP_VERSION: Int = 1
        }
    }

data class CanonicalDayThemeBootstrapReport(
    val performed: Boolean,
    val insertedThemeDefinitions: Int,
    val insertedDayThemes: Int,
    val insertedAssignmentDocuments: Int,
    val diagnostics: List<LegacyDayThemeMigrationDiagnostic>,
) {
    companion object {
        fun alreadyComplete(): CanonicalDayThemeBootstrapReport =
            CanonicalDayThemeBootstrapReport(
                performed = false,
                insertedThemeDefinitions = 0,
                insertedDayThemes = 0,
                insertedAssignmentDocuments = 0,
                diagnostics = emptyList(),
            )
    }
}

internal data class CanonicalDayThemeBootstrapRows(
    val themeDefinitions: List<ThemeDefinitionSnapshot>,
    val dayThemes: List<DayThemeSnapshot>,
    val assignmentDocuments: List<DayThemeAssignmentDocumentSnapshot>,
)

internal fun planCanonicalDayThemeBootstrapRows(
    migrated: LegacyDayThemeCanonicalMigrationResult,
    existingThemeDefinitions: List<ThemeDefinitionSnapshot>,
    existingDayThemes: List<DayThemeSnapshot>,
    existingAssignmentDocuments: List<DayThemeAssignmentDocumentSnapshot>,
): CanonicalDayThemeBootstrapRows {
    val existingDefinitionIds = existingThemeDefinitions.mapTo(hashSetOf()) { it.id }
    val existingDayThemeIds = existingDayThemes.mapTo(hashSetOf()) { it.id }
    val existingDayThemePairs = existingDayThemes.mapTo(hashSetOf()) { it.dayPlanId to it.themeId }
    val existingAssignmentDayPlanIds = existingAssignmentDocuments.mapTo(hashSetOf()) { it.dayPlanId }

    return CanonicalDayThemeBootstrapRows(
        themeDefinitions = migrated.themeDefinitions.filter { it.id !in existingDefinitionIds },
        dayThemes =
            migrated.dayThemes.filter { candidate ->
                candidate.id !in existingDayThemeIds &&
                    (candidate.dayPlanId to candidate.themeId) !in existingDayThemePairs
            },
        assignmentDocuments =
            migrated.dayThemeAssignmentDocuments.filter { it.dayPlanId !in existingAssignmentDayPlanIds },
    )
}
