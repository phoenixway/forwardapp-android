package com.romankozak.forwardappmobile.shared.contracts.contexts

import kotlinx.serialization.Serializable

const val LEGACY_SHARED_PLANNING_FORMAT_VERSION: Int = 1
const val DAY_EXTENSION_SHARED_PLANNING_FORMAT_VERSION: Int = 2
const val TACTICAL_EXTENSION_SHARED_PLANNING_FORMAT_VERSION: Int = 3
const val CURRENT_SHARED_PLANNING_FORMAT_VERSION: Int = 4

@Serializable
data class SharedPlanningScopeSnapshot(
    val id: String,
    val kind: String,
    val lifecycle: String,
    val title: String?,
    val startsAt: Long?,
    val endsAt: Long?,
    val createdAt: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val version: Long,
    val isDeleted: Boolean,
)

@Serializable
data class SharedPlanningCommitmentSnapshot(
    val id: String,
    val scopeId: String,
    val orientationId: String,
    val role: String,
    val order: Long,
    val priorityLevel: String?,
    val status: String,
    val provenanceKind: String,
    val provenanceSourceType: String?,
    val provenanceSourceId: String?,
    val createdAt: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val version: Long,
    val isDeleted: Boolean,
)

@Serializable
data class SharedDayPlanningScopeExtensionSnapshot(
    val scopeId: String,
    val calendarDayStartMillis: Long,
    val outcome: String?,
    val plannedBudgetMinutes: Long?,
    val createdAt: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val version: Long,
    val isDeleted: Boolean,
)

@Serializable
data class SharedTacticalPlanningScopeExtensionSnapshot(
    val scopeId: String,
    val iterationType: String,
    val weekKey: String?,
    val closedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val version: Long,
    val isDeleted: Boolean,
)

@Serializable
data class SharedStrategicPlanningScopeExtensionSnapshot(
    val scopeId: String,
    val legacyArcKey: String,
    val createdAt: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val version: Long,
    val isDeleted: Boolean,
)

fun DesktopWorkspaceSnapshot.hasCanonicalPlanningTransport(): Boolean =
    planningFormatVersion in
        setOf(
            LEGACY_SHARED_PLANNING_FORMAT_VERSION,
            DAY_EXTENSION_SHARED_PLANNING_FORMAT_VERSION,
            TACTICAL_EXTENSION_SHARED_PLANNING_FORMAT_VERSION,
            CURRENT_SHARED_PLANNING_FORMAT_VERSION,
            COMPOSITION_SHARED_PLANNING_FORMAT_VERSION,
        ) &&
        planningScopes != null &&
        planningCommitments != null &&
        (
            planningFormatVersion == LEGACY_SHARED_PLANNING_FORMAT_VERSION ||
                dayPlanningScopeExtensions != null
        ) &&
        (
            planningFormatVersion in
                setOf(LEGACY_SHARED_PLANNING_FORMAT_VERSION, DAY_EXTENSION_SHARED_PLANNING_FORMAT_VERSION) ||
                tacticalPlanningScopeExtensions != null
        ) &&
        (planningFormatVersion !in setOf(CURRENT_SHARED_PLANNING_FORMAT_VERSION, COMPOSITION_SHARED_PLANNING_FORMAT_VERSION) || strategicPlanningScopeExtensions != null) &&
        (planningFormatVersion != COMPOSITION_SHARED_PLANNING_FORMAT_VERSION || planningCommitmentRelations != null)

fun DesktopWorkspaceSnapshot.hasCanonicalDayPlanningExtensionTransport(): Boolean =
    planningFormatVersion in
        setOf(
            DAY_EXTENSION_SHARED_PLANNING_FORMAT_VERSION,
            TACTICAL_EXTENSION_SHARED_PLANNING_FORMAT_VERSION,
            CURRENT_SHARED_PLANNING_FORMAT_VERSION,
            COMPOSITION_SHARED_PLANNING_FORMAT_VERSION,
        ) &&
        dayPlanningScopeExtensions != null

fun DesktopWorkspaceSnapshot.hasCanonicalTacticalPlanningExtensionTransport(): Boolean =
    planningFormatVersion in
        setOf(TACTICAL_EXTENSION_SHARED_PLANNING_FORMAT_VERSION, CURRENT_SHARED_PLANNING_FORMAT_VERSION, COMPOSITION_SHARED_PLANNING_FORMAT_VERSION) &&
        tacticalPlanningScopeExtensions != null

fun DesktopWorkspaceSnapshot.hasCanonicalStrategicPlanningExtensionTransport(): Boolean =
    planningFormatVersion in setOf(CURRENT_SHARED_PLANNING_FORMAT_VERSION, COMPOSITION_SHARED_PLANNING_FORMAT_VERSION) &&
        strategicPlanningScopeExtensions != null

fun DesktopWorkspaceSnapshot.requireValidCanonicalPlanningTransport() {
    requireValidPlanningComposition()
    val formatVersion = planningFormatVersion
    val scopesPresent = planningScopes != null
    val commitmentsPresent = planningCommitments != null
    val dayExtensionsPresent = dayPlanningScopeExtensions != null
    val tacticalExtensionsPresent = tacticalPlanningScopeExtensions != null
    val strategicExtensionsPresent = strategicPlanningScopeExtensions != null

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
            formatVersion == LEGACY_SHARED_PLANNING_FORMAT_VERSION ||
            formatVersion == DAY_EXTENSION_SHARED_PLANNING_FORMAT_VERSION ||
            formatVersion == TACTICAL_EXTENSION_SHARED_PLANNING_FORMAT_VERSION ||
            formatVersion == CURRENT_SHARED_PLANNING_FORMAT_VERSION || formatVersion == COMPOSITION_SHARED_PLANNING_FORMAT_VERSION,
    ) {
        "Unsupported canonical Planning format version: $formatVersion"
    }
    require(scopesPresent && commitmentsPresent) {
        "Canonical Planning format requires both planningScopes and planningCommitments."
    }
    if (formatVersion == LEGACY_SHARED_PLANNING_FORMAT_VERSION) {
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
            "Planning format v2 requires dayPlanningScopeExtensions."
        }
        val scopesById = planningScopes.orEmpty().associateBy { it.id }
        val extensions = dayPlanningScopeExtensions.orEmpty()
        require(extensions.map { it.scopeId }.toSet().size == extensions.size) {
            "Day planning extension payload contains duplicate scope ids."
        }
        extensions.forEach { extension ->
            require(extension.scopeId.isNotBlank()) { "Day planning extension scope id is blank." }
            require(extension.calendarDayStartMillis >= 0L) { "Invalid Day calendar key." }
            require(extension.plannedBudgetMinutes == null || extension.plannedBudgetMinutes > 0L) {
                "Invalid Day planned budget."
            }
            require(extension.outcome == null || extension.outcome in setOf("COMPLETED", "MISSED")) {
                "Unknown Day planning outcome: ${extension.outcome}"
            }
            require(extension.version >= 0L) { "Day planning extension version is negative." }
            require(
                extension.createdAt >= 0L &&
                    extension.updatedAt?.let { it >= 0L } != false &&
                    extension.syncedAt?.let { it >= 0L } != false,
            ) {
                "Day planning extension timestamps must be nonnegative."
            }
            require(extension.updatedAt == null || extension.updatedAt >= extension.createdAt) {
                "Day planning extension updatedAt must not precede createdAt."
            }
            val scope = requireNotNull(scopesById[extension.scopeId]) {
                "Day planning extension has a missing scope: ${extension.scopeId}"
            }
            require(scope.kind == "DAY") { "Day planning extension requires a DAY scope." }
            require(extension.isDeleted || !scope.isDeleted) {
                "Live Day planning extension requires a live scope."
            }
        }
        if (formatVersion == DAY_EXTENSION_SHARED_PLANNING_FORMAT_VERSION) {
            require(!tacticalExtensionsPresent) {
                "Planning format v2 must not claim Tactical scope-extension authority."
            }
            require(!strategicExtensionsPresent) {
                "Planning format v2 must not claim Strategic scope-extension authority."
            }
        } else {
            require(tacticalExtensionsPresent) {
                "Planning format v3 requires tacticalPlanningScopeExtensions."
            }
            val tacticalExtensions = tacticalPlanningScopeExtensions.orEmpty()
            require(tacticalExtensions.map { it.scopeId }.toSet().size == tacticalExtensions.size) {
                "Tactical planning extension payload contains duplicate scope ids."
            }
            tacticalExtensions.forEach { extension ->
                require(extension.scopeId.isNotBlank()) { "Tactical planning extension scope id is blank." }
                require(extension.iterationType in setOf("TIMEBOXED", "OPEN_ENDED")) {
                    "Unknown Tactical planning scope type: ${extension.iterationType}"
                }
                require(
                    (extension.iterationType == "TIMEBOXED" && extension.weekKey?.matches(ISO_WEEK_KEY) == true) ||
                        (extension.iterationType == "OPEN_ENDED" && extension.weekKey == null),
                ) { "Invalid Tactical planning weekKey/type combination." }
                require(extension.closedAt?.let { it >= 0L } != false) { "Invalid Tactical closedAt." }
                require(extension.version >= 0L) { "Tactical planning extension version is negative." }
                require(
                    extension.createdAt >= 0L &&
                        extension.updatedAt?.let { it >= extension.createdAt } != false &&
                        extension.syncedAt?.let { it >= 0L } != false,
                ) { "Invalid Tactical planning extension timestamps." }
                val scope = requireNotNull(scopesById[extension.scopeId]) {
                    "Tactical planning extension has a missing scope: ${extension.scopeId}"
                }
                require(scope.kind == "TACTICAL_CYCLE") {
                    "Tactical planning extension requires a TACTICAL_CYCLE scope."
                }
                require(extension.isDeleted || !scope.isDeleted) {
                    "Live Tactical planning extension requires a live scope."
                }
                require(extension.iterationType != "OPEN_ENDED" || scope.endsAt == null) {
                    "Open-ended Tactical scope must not have an end bound."
                }
            }
            if (formatVersion == TACTICAL_EXTENSION_SHARED_PLANNING_FORMAT_VERSION) {
                require(!strategicExtensionsPresent) {
                    "Planning format v3 must not claim Strategic scope-extension authority."
                }
            } else {
                require(strategicExtensionsPresent) {
                    "Planning format v4 requires strategicPlanningScopeExtensions."
                }
                val strategicExtensions = strategicPlanningScopeExtensions.orEmpty()
                require(strategicExtensions.map { it.scopeId }.toSet().size == strategicExtensions.size) {
                    "Strategic planning extension payload contains duplicate scope ids."
                }
                strategicExtensions.forEach { extension ->
                    require(extension.scopeId.isNotBlank()) { "Strategic planning extension scope id is blank." }
                    require(extension.legacyArcKey.isNotBlank()) {
                        "Strategic planning extension legacyArcKey is blank."
                    }
                    require(extension.version >= 0L) { "Strategic planning extension version is negative." }
                    require(
                        extension.createdAt >= 0L &&
                            extension.updatedAt?.let { it >= extension.createdAt } != false &&
                            extension.syncedAt?.let { it >= 0L } != false,
                    ) { "Invalid Strategic planning extension timestamps." }
                    val scope = requireNotNull(scopesById[extension.scopeId]) {
                        "Strategic planning extension has a missing scope: ${extension.scopeId}"
                    }
                    require(scope.kind == "STRATEGIC_ARC") {
                        "Strategic planning extension requires a STRATEGIC_ARC scope."
                    }
                    require(extension.isDeleted || !scope.isDeleted) {
                        "Live Strategic planning extension requires a live scope."
                    }
                }
            }
        }
    }
}

private val ISO_WEEK_KEY = Regex("""^\d{4}-W(?:0[1-9]|[1-4]\d|5[0-3])$""")
