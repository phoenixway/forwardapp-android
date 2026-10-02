package com.romankozak.forwardappmobile.shared.core.domain.planning

data class PlanningContractViolation(
    val path: String,
    val code: String,
    val message: String,
)

fun validatePlanningScope(scope: PlanningScope): List<PlanningContractViolation> {
    val path = "scopes.${scope.id.value}"
    val violations = mutableListOf<PlanningContractViolation>()

    if (scope.version < 0L) {
        violations += violation(path, "NEGATIVE_VERSION", "Scope version must be nonnegative")
    }
    if (scope.title != null && scope.title.isBlank()) {
        violations += violation(path, "BLANK_TITLE", "Optional scope title must not be blank")
    }
    if (scope.startsAt != null && scope.endsAt != null && scope.endsAt < scope.startsAt) {
        violations += violation(path, "INVALID_HORIZON", "Scope end must not precede its start")
    }

    return violations
}

fun validatePlanningCommitment(
    commitment: PlanningCommitment,
    roleRegistry: PlanningRoleRegistry = PlanningRoleRegistry.INITIAL,
): List<PlanningContractViolation> {
    val path = "commitments.${commitment.id.value}"
    val violations = mutableListOf<PlanningContractViolation>()

    if (commitment.orientationId.isBlank()) {
        violations += violation(path, "BLANK_ORIENTATION_ID", "Orientation id must not be blank")
    }
    if (!roleRegistry.contains(commitment.role)) {
        violations += violation(path, "UNREGISTERED_ROLE", "Planning role ${commitment.role.value} is not registered")
    }
    if (commitment.version < 0L) {
        violations += violation(path, "NEGATIVE_VERSION", "Commitment version must be nonnegative")
    }
    if (commitment.provenance.sourceType != null && commitment.provenance.sourceType.isBlank()) {
        violations += violation(path, "BLANK_SOURCE_TYPE", "Optional provenance source type must not be blank")
    }
    if (commitment.provenance.sourceId != null && commitment.provenance.sourceId.isBlank()) {
        violations += violation(path, "BLANK_SOURCE_ID", "Optional provenance source id must not be blank")
    }

    return violations
}

/**
 * Validates one complete prospective planning state without persistence access.
 *
 * Tombstoned records remain subject to identity and field validation but do not
 * participate in live uniqueness or target-liveness checks.
 */
fun validateProspectivePlanningState(
    scopes: Collection<PlanningScope>,
    commitments: Collection<PlanningCommitment>,
    isOrientationLive: (String) -> Boolean,
    roleRegistry: PlanningRoleRegistry = PlanningRoleRegistry.INITIAL,
): List<PlanningContractViolation> {
    val violations = mutableListOf<PlanningContractViolation>()
    val scopesById = scopes.groupBy { it.id }
    val commitmentsById = commitments.groupBy { it.id }

    scopes.forEach { scope -> violations += validatePlanningScope(scope) }
    commitments.forEach { commitment ->
        violations += validatePlanningCommitment(commitment, roleRegistry)
    }

    scopesById
        .filterValues { it.size > 1 }
        .forEach { (id, duplicates) ->
            violations +=
                violation(
                    path = "scopes.${id.value}",
                    code = "DUPLICATE_SCOPE_ID",
                    message = "Planning scope identity ${id.value} occurs ${duplicates.size} times",
                )
        }

    commitmentsById
        .filterValues { it.size > 1 }
        .forEach { (id, duplicates) ->
            violations +=
                violation(
                    path = "commitments.${id.value}",
                    code = "DUPLICATE_COMMITMENT_ID",
                    message = "Planning commitment identity ${id.value} occurs ${duplicates.size} times",
                )
        }

    commitments
        .filterNot { it.isDeleted }
        .groupBy { Triple(it.scopeId, it.orientationId, it.role) }
        .filterValues { it.size > 1 }
        .forEach { (key, duplicates) ->
            violations +=
                violation(
                    path = "commitments",
                    code = "DUPLICATE_LIVE_PARTICIPATION",
                    message =
                        "Scope ${key.first.value} has ${duplicates.size} live commitments for " +
                            "Orientation ${key.second} in role ${key.third.value}",
                )
        }

    commitments.filterNot { it.isDeleted }.forEach { commitment ->
        val path = "commitments.${commitment.id.value}"
        val scopeCandidates = scopesById[commitment.scopeId]

        when {
            scopeCandidates == null ->
                violations += violation(path, "MISSING_SCOPE", "Live commitment scope does not exist")

            scopeCandidates.size != 1 ->
                violations += violation(path, "SCOPE_ID_NOT_UNIQUE", "Live commitment scope is not uniquely identified")

            scopeCandidates.single().isDeleted ->
                violations += violation(path, "TOMBSTONED_SCOPE", "Live commitment scope is tombstoned")
        }

        if (!isOrientationLive(commitment.orientationId)) {
            violations += violation(path, "ORIENTATION_NOT_LIVE", "Live commitment Orientation must exist and be live")
        }
    }

    return violations.distinctBy { it.path to it.code }
}

private fun violation(
    path: String,
    code: String,
    message: String,
): PlanningContractViolation = PlanningContractViolation(path, code, message)
