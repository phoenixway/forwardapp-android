package com.romankozak.forwardappmobile.shared.core.domain.planning

/** Tactical-cycle shape that is deliberately outside the shared scope core. */
enum class TacticalPlanningScopeType {
    TIMEBOXED,
    OPEN_ENDED,
}

/**
 * Typed one-to-one extension of a [PlanningScopeKind.TACTICAL_CYCLE] scope.
 *
 * [weekKey] is a compatibility/calendar locator for a timeboxed cycle, not an
 * alternative scope identity. [closedAt] is Tactical lifecycle audit state;
 * the shared scope lifecycle remains authoritative for whether the cycle is
 * DRAFT, ACTIVE, CLOSED, or ARCHIVED.
 */
data class TacticalPlanningScopeExtension(
    val scopeId: PlanningScopeId,
    val type: TacticalPlanningScopeType,
    val weekKey: String?,
    val closedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val version: Long,
    val isDeleted: Boolean,
)

fun validateTacticalPlanningScopeExtension(
    extension: TacticalPlanningScopeExtension,
): List<PlanningContractViolation> {
    val path = "tacticalScopeExtensions.${extension.scopeId.value}"
    val violations = mutableListOf<PlanningContractViolation>()

    when (extension.type) {
        TacticalPlanningScopeType.TIMEBOXED -> {
            if (extension.weekKey == null || !ISO_WEEK_KEY.matches(extension.weekKey)) {
                violations +=
                    PlanningContractViolation(
                        path = path,
                        code = "INVALID_TIMEBOXED_WEEK_KEY",
                        message = "Timeboxed Tactical scope requires an ISO week key",
                    )
            }
        }

        TacticalPlanningScopeType.OPEN_ENDED -> {
            if (extension.weekKey != null) {
                violations +=
                    PlanningContractViolation(
                        path = path,
                        code = "OPEN_ENDED_WEEK_KEY",
                        message = "Open-ended Tactical scope must not carry a week key",
                    )
            }
        }
    }

    if (extension.closedAt?.let { it < 0L } == true) {
        violations +=
            PlanningContractViolation(
                path = path,
                code = "INVALID_CLOSED_AT",
                message = "Tactical closedAt must be nonnegative",
            )
    }
    if (extension.version < 0L) {
        violations +=
            PlanningContractViolation(
                path = path,
                code = "NEGATIVE_VERSION",
                message = "Tactical scope extension version must be nonnegative",
            )
    }
    if (extension.createdAt < 0L || extension.updatedAt?.let { it < 0L } == true ||
        extension.syncedAt?.let { it < 0L } == true
    ) {
        violations +=
            PlanningContractViolation(
                path = path,
                code = "INVALID_TIMESTAMP",
                message = "Tactical scope extension timestamps must be nonnegative",
            )
    }
    if (extension.updatedAt != null && extension.updatedAt < extension.createdAt) {
        violations +=
            PlanningContractViolation(
                path = path,
                code = "UPDATED_BEFORE_CREATED",
                message = "Tactical scope extension updatedAt must not precede createdAt",
            )
    }

    return violations
}

/** Validates the extension-to-scope dependency without persistence access. */
fun validateTacticalPlanningScopeExtensionDependency(
    extension: TacticalPlanningScopeExtension,
    scope: PlanningScope?,
): List<PlanningContractViolation> {
    val path = "tacticalScopeExtensions.${extension.scopeId.value}"
    val violations = validateTacticalPlanningScopeExtension(extension).toMutableList()

    when {
        scope == null ->
            violations +=
                PlanningContractViolation(path, "MISSING_SCOPE", "Tactical extension scope does not exist")

        scope.id != extension.scopeId ->
            violations +=
                PlanningContractViolation(path, "SCOPE_ID_MISMATCH", "Tactical extension scope id does not match")

        scope.kind != PlanningScopeKind.TACTICAL_CYCLE ->
            violations +=
                PlanningContractViolation(
                    path,
                    "NON_TACTICAL_SCOPE",
                    "Tactical extension requires a TACTICAL_CYCLE scope",
                )

        !extension.isDeleted && scope.isDeleted ->
            violations +=
                PlanningContractViolation(
                    path,
                    "TOMBSTONED_SCOPE",
                    "Live Tactical extension requires a live scope",
                )
    }

    if (scope != null &&
        extension.type == TacticalPlanningScopeType.OPEN_ENDED &&
        scope.endsAt != null
    ) {
        violations +=
            PlanningContractViolation(
                path,
                "OPEN_ENDED_HORIZON_END",
                "Open-ended Tactical scope must not have an end bound",
            )
    }

    return violations
}

private val ISO_WEEK_KEY = Regex("""^\d{4}-W(?:0[1-9]|[1-4]\d|5[0-3])$""")
