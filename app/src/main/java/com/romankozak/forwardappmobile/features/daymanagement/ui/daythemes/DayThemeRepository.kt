package com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalSnapshot
import com.romankozak.forwardappmobile.data.dao.CanonicalDayThemeDao
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DayThemeRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val dao: CanonicalDayThemeDao,
        private val bootstrapper: CanonicalDayThemeBootstrapper,
    ) {
        private val mutex = Mutex()

        fun observe(dayPlanId: String): Flow<DayThemeDocument> =
            flow {
                bootstrapper.ensureBootstrapped()
                emitAll(
                    combine(
                        dao.observeAllThemeDefinitions(),
                        dao.observeDayThemesForDay(dayPlanId),
                        dao.observeAssignmentDocumentByDayPlanId(dayPlanId),
                    ) { definitions, dayThemes, assignmentDocument ->
                        projectCanonicalDayThemeDocument(
                            dayPlanId = dayPlanId,
                            themeDefinitions = definitions.map { it.toCanonicalSnapshot() },
                            dayThemes = dayThemes.map { it.toCanonicalSnapshot() },
                            assignmentDocument = assignmentDocument?.toCanonicalSnapshot(),
                        )
                    },
                )
            }.catch { emit(DayThemeDocument()) }

        /**
         * Compatibility entry point retained for the existing ViewModel.
         * The one-time legacy migration is now global and canonical, not per-day.
         */
        suspend fun migrateLegacyDayIfNeeded(dayPlanId: String) {
            require(dayPlanId.isNotBlank())
            bootstrapper.ensureBootstrapped()
        }

        suspend fun update(
            dayPlanId: String,
            transform: (DayThemeDocument) -> DayThemeDocument,
        ) {
            bootstrapper.ensureBootstrapped()

            mutex.withLock {
                database.withTransaction {
                    val definitions =
                        dao.getAllThemeDefinitionsSync().map { it.toCanonicalSnapshot() }
                    val dayThemes =
                        dao.getDayThemesForDay(dayPlanId).map { it.toCanonicalSnapshot() }
                    val assignmentDocument =
                        dao.getAssignmentDocumentByDayPlanId(dayPlanId)?.toCanonicalSnapshot()

                    val current =
                        projectCanonicalDayThemeDocument(
                            dayPlanId = dayPlanId,
                            themeDefinitions = definitions,
                            dayThemes = dayThemes,
                            assignmentDocument = assignmentDocument,
                        )

                    val transformed = transform(current).normalizedForDay(dayPlanId)
                    val plan =
                        planCanonicalDayThemeRuntimeUpdate(
                            dayPlanId = dayPlanId,
                            now = System.currentTimeMillis(),
                            localThemeDefinitions = definitions,
                            localDayThemes = dayThemes,
                            localAssignmentDocument = assignmentDocument,
                            transformedDocument = transformed,
                        )

                    if (plan.themeDefinitions.isNotEmpty()) {
                        dao.upsertThemeDefinitions(
                            plan.themeDefinitions.map { it.toCanonicalEntity() },
                        )
                    }
                    if (plan.dayThemes.isNotEmpty()) {
                        dao.upsertDayThemes(
                            plan.dayThemes.map { it.toCanonicalEntity() },
                        )
                    }
                    plan.assignmentDocument?.let { document ->
                        dao.upsertAssignmentDocuments(
                            listOf(document.toCanonicalEntity()),
                        )
                    }
                }
            }
        }
    }
