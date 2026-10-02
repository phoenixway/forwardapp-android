package com.romankozak.forwardappmobile.shared.core.domain.planning

data class PlanningScopeId(val value: String) {
    init {
        require(value.isNotBlank()) { "PlanningScopeId must not be blank" }
    }
}

data class PlanningCommitmentId(val value: String) {
    init {
        require(value.isNotBlank()) { "PlanningCommitmentId must not be blank" }
    }
}

enum class PlanningScopeKind {
    DAY,
    TACTICAL_CYCLE,
    STRATEGIC_ARC,
}

enum class PlanningScopeLifecycle {
    DRAFT,
    ACTIVE,
    CLOSED,
    ARCHIVED,
}

enum class PlanningCommitmentStatus {
    PLANNED,
    ACTIVE,
    PAUSED,
    COMPLETED,
    DROPPED,
}

enum class PlanningPriorityLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

/**
 * Stable planning-role wire code.
 *
 * The three common roles are closed constants. Domain roles remain extensible
 * through uppercase dot-namespaced codes and must additionally be admitted by
 * the validator's [PlanningRoleRegistry].
 */
class PlanningRoleCode private constructor(val value: String) {
    val isNamespaced: Boolean
        get() = '.' in value

    override fun equals(other: Any?): Boolean =
        this === other || (other is PlanningRoleCode && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    companion object {
        val MISSION = PlanningRoleCode("MISSION")
        val PRIORITY = PlanningRoleCode("PRIORITY")
        val FOCUS = PlanningRoleCode("FOCUS")

        val DAY_THEME = PlanningRoleCode("DAY.THEME")
        val DAY_RESPONSIBILITY = PlanningRoleCode("DAY.RESPONSIBILITY")
        val DAY_TASK = PlanningRoleCode("DAY.TASK")

        val common: Set<PlanningRoleCode> = setOf(MISSION, PRIORITY, FOCUS)
        val initialDomain: Set<PlanningRoleCode> =
            setOf(DAY_THEME, DAY_RESPONSIBILITY, DAY_TASK)

        fun fromStableCode(value: String): PlanningRoleCode =
            common.firstOrNull { it.value == value }
                ?: namespaced(value)

        fun namespaced(value: String): PlanningRoleCode {
            require(isValidNamespacedRoleCode(value)) {
                "Planning role must be an uppercase dot-namespaced stable code"
            }
            return PlanningRoleCode(value)
        }

        private fun isValidNamespacedRoleCode(value: String): Boolean {
            val segments = value.split('.')
            return segments.size >= 2 && segments.all(::isValidSegment)
        }

        private fun isValidSegment(segment: String): Boolean {
            if (segment.isEmpty() || segment.first() !in 'A'..'Z') return false
            return segment.all { character ->
                character in 'A'..'Z' || character in '0'..'9' || character == '_'
            }
        }
    }
}

data class PlanningRoleRegistry(
    val domainRoles: Set<PlanningRoleCode>,
) {
    init {
        require(domainRoles.all(PlanningRoleCode::isNamespaced)) {
            "Domain planning roles must use namespaced codes"
        }
    }

    fun contains(role: PlanningRoleCode): Boolean =
        role in PlanningRoleCode.common || role in domainRoles

    companion object {
        val INITIAL = PlanningRoleRegistry(PlanningRoleCode.initialDomain)
    }
}

enum class PlanningProvenanceKind {
    MANUAL,
    MIGRATED,
    DERIVED,
    CARRIED_FORWARD,
    IMPORTED,
}

data class PlanningProvenance(
    val kind: PlanningProvenanceKind,
    val sourceType: String? = null,
    val sourceId: String? = null,
)

data class PlanningScope(
    val id: PlanningScopeId,
    val kind: PlanningScopeKind,
    val lifecycle: PlanningScopeLifecycle,
    val title: String?,
    val startsAt: Long?,
    val endsAt: Long?,
    val createdAt: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val version: Long,
    val isDeleted: Boolean,
)

data class PlanningCommitment(
    val id: PlanningCommitmentId,
    val scopeId: PlanningScopeId,
    val orientationId: String,
    val role: PlanningRoleCode,
    val order: Long,
    val priorityLevel: PlanningPriorityLevel?,
    val status: PlanningCommitmentStatus,
    val provenance: PlanningProvenance,
    val createdAt: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val version: Long,
    val isDeleted: Boolean,
)

val planningCommitmentOrderComparator: Comparator<PlanningCommitment> =
    compareBy<PlanningCommitment> { it.order }
        .thenBy { it.id.value }
