package com.romankozak.forwardappmobile.data.planning.day

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.DayStatus
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.planning.PlanningDao
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeEntity
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningOutcome
import com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Canonical authority facade for Day scope-owned planning facts. */
@Singleton
class CanonicalDayPlanningAdapter
    @Inject
    constructor(
        private val database: AppDatabase,
        private val dayPlanDao: DayPlanDao,
        private val planningDao: PlanningDao,
        private val extensionDao: DayPlanningScopeExtensionDao,
        private val activator: CanonicalDayPlanningAuthorityActivator,
    ) {
        fun observe(planId: String): Flow<DayPlan?> =
            combine(
                dayPlanDao.getPlanByIdStream(planId),
                planningDao.observeScope(planId),
                extensionDao.observe(planId),
            ) { legacy, scope, extension ->
                if (legacy == null && scope == null && extension == null) return@combine null
                compose(
                    requireNotNull(legacy) { "Canonical Day $planId is missing its domain row" },
                    requireNotNull(scope) { "Canonical Day $planId is missing its scope" }
                        .toPlanningScopeStrict(),
                    requireNotNull(extension) { "Canonical Day $planId is missing its extension" }
                        .toDomainStrict(),
                )
            }

        fun observeForCalendarDay(calendarDayStartMillis: Long): Flow<DayPlan?> =
            combine(
                dayPlanDao.getPlanForDate(calendarDayStartMillis),
                planningDao.observeAllScopes(),
                extensionDao.observeAll(),
            ) { legacy, scopes, extensions ->
                legacy?.let { row ->
                    val scope = requireNotNull(scopes.singleOrNull { it.id == row.id }) {
                        "Canonical Day ${row.id} is missing its scope"
                    }.toPlanningScopeStrict()
                    val extension = requireNotNull(extensions.singleOrNull { it.scopeId == row.id }) {
                        "Canonical Day ${row.id} is missing its extension"
                    }.toDomainStrict()
                    compose(row, scope, extension)
                }
            }

        fun observeRange(startDate: Long, endDate: Long): Flow<List<DayPlan>> =
            combine(
                dayPlanDao.getPlansForDateRange(startDate, endDate),
                planningDao.observeAllScopes(),
                extensionDao.observeAll(),
            ) { legacyPlans, scopes, extensions ->
                val scopesById = scopes.associateBy { it.id }
                val extensionsById = extensions.associateBy { it.scopeId }
                legacyPlans.map { legacy ->
                    compose(
                        legacy,
                        requireNotNull(scopesById[legacy.id]) {
                            "Canonical Day ${legacy.id} is missing its scope"
                        }.toPlanningScopeStrict(),
                        requireNotNull(extensionsById[legacy.id]) {
                            "Canonical Day ${legacy.id} is missing its extension"
                        }.toDomainStrict(),
                    )
                }
            }

        suspend fun get(planId: String): DayPlan? {
            activator.requireActivated()
            val legacy = dayPlanDao.getPlanById(planId) ?: return null
            return compose(legacy, requireScope(planId), requireExtension(planId))
        }

        suspend fun createOrUpdate(
            calendarDayStartMillis: Long,
            title: String?,
            now: Long,
        ): DayPlan {
            activator.requireActivated()
            return database.withTransaction {
                val existing = dayPlanDao.getPlanForDateSync(calendarDayStartMillis)
                if (existing == null) {
                    val legacy =
                        DayPlan(
                            id = UUID.randomUUID().toString(),
                            date = calendarDayStartMillis,
                            name = title,
                            status = DayStatus.PLANNED,
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            version = 1,
                        )
                    val scope = legacy.toCanonicalDayPlanningScope()
                    val extension = legacy.toCanonicalDayPlanningScopeExtension()
                    dayPlanDao.insert(legacy)
                    planningDao.upsertScope(scope.toPlanningScopeEntity())
                    extensionDao.upsert(extension.toEntity())
                    compose(legacy, scope, extension)
                } else {
                    val scope = requireScope(existing.id)
                    val extension = requireExtension(existing.id)
                    val updatedScope =
                        scope.copy(
                            title = title?.trim()?.takeIf(String::isNotEmpty) ?: scope.title,
                            updatedAt = now,
                            syncedAt = null,
                            version = nextVersion(scope.version),
                        )
                    planningDao.upsertScope(updatedScope.toPlanningScopeEntity())
                    projectLegacy(existing, updatedScope, extension)
                    compose(existing, updatedScope, extension)
                }
            }
        }

        suspend fun updateStatus(planId: String, status: DayStatus, now: Long) {
            activator.requireActivated()
            database.withTransaction {
                val legacy = requireNotNull(dayPlanDao.getPlanById(planId)) { "Day $planId does not exist" }
                val scope = requireScope(planId)
                val extension = requireExtension(planId)
                val updatedScope =
                    scope.copy(
                        lifecycle = status.toCanonicalDayScopeLifecycle(),
                        updatedAt = now,
                        syncedAt = null,
                        version = nextVersion(scope.version),
                    )
                val updatedExtension =
                    extension.copy(
                        outcome = status.toDayOutcome(),
                        updatedAt = now,
                        syncedAt = null,
                        version = nextVersion(extension.version),
                    )
                planningDao.upsertScope(updatedScope.toPlanningScopeEntity())
                extensionDao.upsert(updatedExtension.toEntity())
                projectLegacy(legacy, updatedScope, updatedExtension)
            }
        }

        suspend fun updatePlannedBudget(planId: String, minutes: Long?, now: Long) {
            activator.requireActivated()
            database.withTransaction {
                val legacy = requireNotNull(dayPlanDao.getPlanById(planId)) { "Day $planId does not exist" }
                val scope = requireScope(planId)
                val extension = requireExtension(planId)
                val updated =
                    extension.copy(
                        plannedBudgetMinutes = minutes?.takeIf { it > 0L },
                        updatedAt = now,
                        syncedAt = null,
                        version = nextVersion(extension.version),
                    )
                extensionDao.upsert(updated.toEntity())
                projectLegacy(legacy, scope, updated)
            }
        }

        private suspend fun requireScope(id: String): PlanningScope =
            requireNotNull(planningDao.getScope(id)) { "Canonical Day $id is missing its scope" }
                .toPlanningScopeStrict()

        private suspend fun requireExtension(id: String): DayPlanningScopeExtension =
            requireNotNull(extensionDao.get(id)) { "Canonical Day $id is missing its extension" }
                .toDomainStrict()

        private suspend fun projectLegacy(
            legacy: DayPlan,
            scope: PlanningScope,
            extension: DayPlanningScopeExtension,
        ) {
            dayPlanDao.projectCanonicalPlanningFields(
                planId = legacy.id,
                date = extension.calendarDayStartMillis,
                name = scope.title,
                status = statusOf(scope.lifecycle, extension.outcome),
                predictedDurationMinutes = extension.plannedBudgetMinutes,
            )
        }
    }

internal fun compose(
    legacy: DayPlan,
    scope: PlanningScope,
    extension: DayPlanningScopeExtension,
): DayPlan {
    require(scope.id == extension.scopeId && scope.id.value == legacy.id)
    return legacy.copy(
        date = extension.calendarDayStartMillis,
        name = scope.title,
        status = statusOf(scope.lifecycle, extension.outcome),
        predictedDurationMinutes = extension.plannedBudgetMinutes,
        isDeleted = scope.isDeleted,
    )
}

private fun DayStatus.toDayOutcome(): DayPlanningOutcome? =
    when (this) {
        DayStatus.COMPLETED -> DayPlanningOutcome.COMPLETED
        DayStatus.MISSED -> DayPlanningOutcome.MISSED
        else -> null
    }

private fun statusOf(
    lifecycle: PlanningScopeLifecycle,
    outcome: DayPlanningOutcome?,
): DayStatus =
    when (lifecycle) {
        PlanningScopeLifecycle.DRAFT -> DayStatus.PLANNED
        PlanningScopeLifecycle.ACTIVE -> DayStatus.IN_PROGRESS
        PlanningScopeLifecycle.CLOSED ->
            when (outcome) {
                DayPlanningOutcome.COMPLETED -> DayStatus.COMPLETED
                DayPlanningOutcome.MISSED -> DayStatus.MISSED
                null -> error("Closed DAY scope requires a Day outcome")
            }
        PlanningScopeLifecycle.ARCHIVED -> DayStatus.ARCHIVED
    }

private fun nextVersion(current: Long): Long {
    require(current < Long.MAX_VALUE) { "Day planning version is exhausted" }
    return current + 1L
}
