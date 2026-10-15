package com.romankozak.forwardappmobile.data.planning.day

import com.romankozak.forwardappmobile.core.data.models.entities.planning.DayPlanningScopeExtensionEntity
import com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningOutcome
import com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateDayPlanningScopeExtension

internal class CorruptDayPlanningScopeExtensionException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

internal fun DayPlanningScopeExtensionEntity.toDomainStrict(): DayPlanningScopeExtension =
    try {
        DayPlanningScopeExtension(
            scopeId = PlanningScopeId(scopeId),
            calendarDayStartMillis = calendarDayStartMillis,
            outcome = outcome?.toDayPlanningOutcomeStable(),
            plannedBudgetMinutes = plannedBudgetMinutes,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt,
            version = version,
            isDeleted = isDeleted,
        ).also { extension ->
            require(validateDayPlanningScopeExtension(extension).isEmpty()) {
                "Persisted Day planning extension violates the V3 contract"
            }
        }
    } catch (failure: RuntimeException) {
        throw CorruptDayPlanningScopeExtensionException(
            message = "Malformed day_planning_scope_extensions row scopeId=$scopeId",
            cause = failure,
        )
    }

internal fun DayPlanningScopeExtension.toEntity(): DayPlanningScopeExtensionEntity =
    DayPlanningScopeExtensionEntity(
        scopeId = scopeId.value,
        calendarDayStartMillis = calendarDayStartMillis,
        outcome = outcome?.toStableCode(),
        plannedBudgetMinutes = plannedBudgetMinutes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

internal fun DayPlanningOutcome.toStableCode(): String =
    when (this) {
        DayPlanningOutcome.COMPLETED -> "COMPLETED"
        DayPlanningOutcome.MISSED -> "MISSED"
    }

internal fun String.toDayPlanningOutcomeStable(): DayPlanningOutcome =
    when (this) {
        "COMPLETED" -> DayPlanningOutcome.COMPLETED
        "MISSED" -> DayPlanningOutcome.MISSED
        else -> throw IllegalArgumentException("Unknown Day planning outcome code: $this")
    }
