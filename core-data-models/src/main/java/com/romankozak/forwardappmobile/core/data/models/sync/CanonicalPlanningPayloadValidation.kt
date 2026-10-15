package com.romankozak.forwardappmobile.core.data.models.sync

const val LEGACY_COMMON_PLANNING_FORMAT_VERSION: Int = 1
const val DAY_EXTENSION_PLANNING_FORMAT_VERSION: Int = 2
const val TACTICAL_EXTENSION_PLANNING_FORMAT_VERSION: Int = 3
const val CURRENT_PLANNING_FORMAT_VERSION: Int = 4

/**
 * True only when the sender explicitly carries the canonical Planning V3
 * contract. Empty collections still mean authoritative presence.
 */
fun SnapshotBundle.hasCanonicalPlanningPayload(): Boolean =
    planningFormatVersion in
        setOf(
            LEGACY_COMMON_PLANNING_FORMAT_VERSION,
            DAY_EXTENSION_PLANNING_FORMAT_VERSION,
            TACTICAL_EXTENSION_PLANNING_FORMAT_VERSION,
            CURRENT_PLANNING_FORMAT_VERSION,
        ) &&
        planningScopes != null &&
        planningCommitments != null &&
        (
            planningFormatVersion == LEGACY_COMMON_PLANNING_FORMAT_VERSION ||
                dayPlanningScopeExtensions != null
        ) &&
        (
            planningFormatVersion in
                setOf(LEGACY_COMMON_PLANNING_FORMAT_VERSION, DAY_EXTENSION_PLANNING_FORMAT_VERSION) ||
                tacticalPlanningScopeExtensions != null
        ) &&
        (planningFormatVersion != CURRENT_PLANNING_FORMAT_VERSION || strategicPlanningScopeExtensions != null)

/**
 * Planning V3 transport is atomic at the common-contract boundary:
 *
 * marker=null + null/null/null = sender does not carry V3 planning
 * marker=v1 + list/list/null = legacy common planning transport
 * marker=current + list/list/list = planning plus typed Day extensions,
 * including explicit empty collections
 *
 * Partial presence, an unsupported marker, or collections without a marker
 * are malformed and must fail closed before persistence.
 */
fun requireValidCanonicalPlanningPayload(bundle: SnapshotBundle) {
    val formatVersion = bundle.planningFormatVersion
    val scopesPresent = bundle.planningScopes != null
    val commitmentsPresent = bundle.planningCommitments != null
    val dayExtensionsPresent = bundle.dayPlanningScopeExtensions != null
    val tacticalExtensionsPresent = bundle.tacticalPlanningScopeExtensions != null
    val strategicExtensionsPresent = bundle.strategicPlanningScopeExtensions != null

    if (formatVersion == null) {
        require(
            !scopesPresent && !commitmentsPresent && !dayExtensionsPresent && !tacticalExtensionsPresent &&
                !strategicExtensionsPresent,
        ) {
            "Canonical Planning collections require planningFormatVersion."
        }
        return
    }

    require(
            formatVersion == LEGACY_COMMON_PLANNING_FORMAT_VERSION ||
            formatVersion == DAY_EXTENSION_PLANNING_FORMAT_VERSION ||
            formatVersion == TACTICAL_EXTENSION_PLANNING_FORMAT_VERSION ||
            formatVersion == CURRENT_PLANNING_FORMAT_VERSION,
    ) {
        "Unsupported canonical Planning format version: $formatVersion"
    }
    require(scopesPresent && commitmentsPresent) {
        "Canonical Planning format requires both planningScopes and planningCommitments."
    }
    if (formatVersion == LEGACY_COMMON_PLANNING_FORMAT_VERSION) {
        require(!dayExtensionsPresent) {
            "Planning format v1 must not claim Day scope-extension authority."
        }
        require(!tacticalExtensionsPresent) {
            "Planning format v1 must not claim Tactical scope-extension authority."
        }
        require(!strategicExtensionsPresent) {
            "Planning format v1 must not claim Strategic scope-extension authority."
        }
    } else {
        require(dayExtensionsPresent) {
            "Planning format v2+ requires dayPlanningScopeExtensions."
        }
        if (formatVersion == DAY_EXTENSION_PLANNING_FORMAT_VERSION) {
            require(!tacticalExtensionsPresent) {
                "Planning format v2 must not claim Tactical scope-extension authority."
            }
            require(!strategicExtensionsPresent) {
                "Planning format v2 must not claim Strategic scope-extension authority."
            }
        } else {
            require(tacticalExtensionsPresent) {
                "Planning format v3+ requires tacticalPlanningScopeExtensions."
            }
            if (formatVersion == TACTICAL_EXTENSION_PLANNING_FORMAT_VERSION) {
                require(!strategicExtensionsPresent) {
                    "Planning format v3 must not claim Strategic scope-extension authority."
                }
            } else {
                require(strategicExtensionsPresent) {
                    "Planning format v4 requires strategicPlanningScopeExtensions."
                }
            }
        }
    }
}
