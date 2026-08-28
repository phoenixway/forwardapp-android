package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.AttributionMode
import com.romankozak.forwardappmobile.shared.core.models.orientation.ContributionSet
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationContribution

fun validateContributionSet(set: ContributionSet): List<OrientationContractViolation> {
    val violations = mutableListOf<OrientationContractViolation>()
    val duplicates = set.contributions.groupBy { it.orientationId }.filterValues { it.size > 1 }
    if (duplicates.isNotEmpty()) {
        violations += OrientationContractViolation("contributions", "DUPLICATE_ORIENTATION", "Canonical Orientation IDs must be unique")
    }

    when (set.mode) {
        AttributionMode.INCLUSIVE -> Unit
        AttributionMode.PRIMARY_ONLY ->
            if (set.contributions.count { it.isPrimary } != 1) {
                violations += OrientationContractViolation("contributions", "PRIMARY_REQUIRED", "PRIMARY_ONLY requires exactly one primary")
            }

        AttributionMode.ALLOCATED -> {
            if (set.contributions.any { contribution ->
                    val weight = contribution.allocationWeight
                    weight == null || weight <= 0.0
                }
            ) {
                violations += OrientationContractViolation("contributions", "INVALID_WEIGHT", "ALLOCATED weights must be positive")
            }
        }
    }
    return violations
}

fun normalizedAllocation(set: ContributionSet): List<OrientationContribution> {
    require(validateContributionSet(set).isEmpty()) { "Invalid contribution set" }
    if (set.mode != AttributionMode.ALLOCATED) return set.contributions

    val total = set.contributions.sumOf { it.allocationWeight ?: 0.0 }
    return set.contributions.map { contribution ->
        contribution.copy(allocationWeight = (contribution.allocationWeight ?: 0.0) / total)
    }
}
