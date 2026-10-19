package com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalSnapshot
import com.romankozak.forwardappmobile.data.dao.CanonicalDayThemeDao
import com.romankozak.forwardappmobile.data.orientation.ThemeOrientationBridge
import com.romankozak.forwardappmobile.data.planning.day.CanonicalDayThemePlanningAdapter
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
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
        private val themeOrientationBridge: ThemeOrientationBridge,
        private val planningAdapter: CanonicalDayThemePlanningAdapter,
    ) {
        private val mutex = Mutex()

        fun observe(dayPlanId: String): Flow<DayThemeDocument> =
            flow {
                themeOrientationBridge.ensureCutOver()
                emitAll(
                    database.invalidationTracker
                        .createFlow(
                            "theme_definitions",
                            "day_themes",
                            "day_theme_assignment_documents",
                            "planning_commitments",
                            "managed_subjects",
                            "legacy_subject_mappings",
                            "orientations",
                            emitInitialState = true,
                        ).map {
                            database.withTransaction {
                                projectCanonicalDayThemeDocument(
                                    dayPlanId = dayPlanId,
                                    themeDefinitions =
                                        themeOrientationBridge
                                            .projectCanonicalDefinitions(dao.getAllThemeDefinitionsSync())
                                            .map { it.toCanonicalSnapshot() },
                                    dayThemes =
                                        planningAdapter.applyCanonicalProjection(
                                            dao.getDayThemesForDay(dayPlanId)
                                                .map { it.toCanonicalSnapshot() },
                                            planningAdapter.getDayThemeCommitments(dayPlanId),
                                        ),
                                    assignmentDocument =
                                        dao.getAssignmentDocumentByDayPlanId(dayPlanId)
                                            ?.toCanonicalSnapshot(),
                                )
                            }
                        }.distinctUntilChanged(),
                )
            }

        /**
         * Compatibility entry point retained for the existing ViewModel.
         * The one-time legacy migration is now global and canonical, not per-day.
         */
        suspend fun migrateLegacyDayIfNeeded(dayPlanId: String) {
            require(dayPlanId.isNotBlank())
            themeOrientationBridge.ensureCutOver()
        }

        /** Explicit Day-entry command. Merely observing existing themes never materializes participation. */
        suspend fun prepareDay(dayPlanId: String, now: Long = System.currentTimeMillis()) {
            themeOrientationBridge.ensureCutOver()
            carryForwardDayThemes(database, dao, themeOrientationBridge, planningAdapter, dayPlanId, now)
        }

        suspend fun update(
            dayPlanId: String,
            transform: (DayThemeDocument) -> DayThemeDocument,
        ) {
            themeOrientationBridge.ensureCutOver()

            mutex.withLock {
                database.withTransaction {
                    val rawDefinitions = dao.getAllThemeDefinitionsSync()
                    val definitions =
                        themeOrientationBridge.projectCanonicalDefinitions(rawDefinitions)
                            .map { it.toCanonicalSnapshot() }
                    val dayThemes =
                        planningAdapter.applyCanonicalProjection(
                            dao.getDayThemesForDay(dayPlanId).map { it.toCanonicalSnapshot() },
                            planningAdapter.getDayThemeCommitments(dayPlanId),
                        )
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
                    val now = System.currentTimeMillis()
                    val plan =
                        planCanonicalDayThemeRuntimeUpdate(
                            dayPlanId = dayPlanId,
                            now = now,
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
                    val definitionUpsertsById =
                        plan.themeDefinitions.associateBy { it.id }
                    val existingDefinitionsById = rawDefinitions.associateBy { it.id }
                    val existingDayThemesById = dayThemes.associateBy { it.id }
                    transformed.themes.forEach { theme ->
                        val definitionId = existingDayThemesById[theme.id]?.themeId ?: theme.id
                        val definition =
                            definitionUpsertsById[definitionId]?.toCanonicalEntity()
                                ?: requireNotNull(existingDefinitionsById[definitionId]) {
                                    "Day Theme $definitionId has no Theme extension row"
                                }
                        themeOrientationBridge.writeCommon(
                            definition = definition,
                            title = theme.title.trim(),
                            description = theme.comment.trim(),
                            now = now,
                        )
                    }
                    if (plan.dayThemes.isNotEmpty()) {
                        planningAdapter.applyProjectionChanges(plan.dayThemes)
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
