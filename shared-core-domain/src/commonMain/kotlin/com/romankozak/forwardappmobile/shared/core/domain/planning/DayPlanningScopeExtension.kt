package com.romankozak.forwardappmobile.shared.core.domain.planning

/** Stable Day-only result classification that is intentionally not scope lifecycle. */
enum class DayPlanningOutcome {
    COMPLETED,
    MISSED,
}

/**
 * Typed one-to-one extension of a [PlanningScopeKind.DAY] scope.
 *
 * [calendarDayStartMillis] preserves the current Day calendar key exactly: the
 * epoch-millisecond value normalized to local start-of-day by the Day domain.
 * It is not an alternative scope identity and does not assert global date
 * uniqueness.
 */
data class DayPlanningScopeExtension(
    val scopeId: PlanningScopeId,
    val calendarDayStartMillis: Long,
    val outcome: DayPlanningOutcome?,
    val plannedBudgetMinutes: Long?,
    val createdAt: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val version: Long,
    val isDeleted: Boolean,
)

fun validateDayPlanningScopeExtension(
    extension: DayPlanningScopeExtension,
): List<PlanningContractViolation> {
    val path = "dayScopeExtensions.${extension.scopeId.value}"
    val violations = mutableListOf<PlanningContractViolation>()

    if (extension.calendarDayStartMillis < 0L) {
        violations +=
            PlanningContractViolation(
                path = path,
                code = "INVALID_CALENDAR_DAY_KEY",
                message = "Day calendar start millis must be nonnegative",
            )
    }
    if (extension.plannedBudgetMinutes != null && extension.plannedBudgetMinutes <= 0L) {
        violations +=
            PlanningContractViolation(
                path = path,
                code = "INVALID_PLANNED_BUDGET",
                message = "Optional planned budget must be positive minutes",
            )
    }
    if (extension.version < 0L) {
        violations +=
            PlanningContractViolation(
                path = path,
                code = "NEGATIVE_VERSION",
                message = "Day scope extension version must be nonnegative",
            )
    }
    if (extension.createdAt < 0L || extension.updatedAt?.let { it < 0L } == true ||
        extension.syncedAt?.let { it < 0L } == true
    ) {
        violations +=
            PlanningContractViolation(
                path = path,
                code = "INVALID_TIMESTAMP",
                message = "Day scope extension timestamps must be nonnegative",
            )
    }
    if (extension.updatedAt != null && extension.updatedAt < extension.createdAt) {
        violations +=
            PlanningContractViolation(
                path = path,
                code = "UPDATED_BEFORE_CREATED",
                message = "Day scope extension updatedAt must not precede createdAt",
            )
    }

    return violations
}

/** Validates the extension-to-scope dependency without persistence access. */
fun validateDayPlanningScopeExtensionDependency(
    extension: DayPlanningScopeExtension,
    scope: PlanningScope?,
): List<PlanningContractViolation> {
    val path = "dayScopeExtensions.${extension.scopeId.value}"
    val violations = validateDayPlanningScopeExtension(extension).toMutableList()

    when {
        scope == null ->
            violations +=
                PlanningContractViolation(path, "MISSING_SCOPE", "Day extension scope does not exist")

        scope.id != extension.scopeId ->
            violations +=
                PlanningContractViolation(path, "SCOPE_ID_MISMATCH", "Day extension scope id does not match")

        scope.kind != PlanningScopeKind.DAY ->
            violations +=
                PlanningContractViolation(path, "NON_DAY_SCOPE", "Day extension requires a DAY scope")

        !extension.isDeleted && scope.isDeleted ->
            violations +=
                PlanningContractViolation(path, "TOMBSTONED_SCOPE", "Live Day extension requires a live scope")
    }

    return violations
}
