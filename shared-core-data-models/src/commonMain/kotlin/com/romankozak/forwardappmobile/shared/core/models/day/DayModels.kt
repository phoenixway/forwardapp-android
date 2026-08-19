package com.romankozak.forwardappmobile.shared.core.models.day

import com.romankozak.forwardappmobile.shared.core.models.recurrence.LocalDayKey
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceOrigin
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeries
import com.romankozak.forwardappmobile.shared.core.models.sync.SyncEntityMeta

enum class TaskPriority {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

enum class TaskStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
}

enum class DayStatus {
    PLANNED,
    ACTIVE,
    COMPLETED,
    SKIPPED,
}

enum class DayFocusType {
    FOCUS,
    RESPONSIBILITY,
}

enum class DayManagementPhase {
    PREPARATION,
    IMPLEMENTATION,
    FINALIZATION,
    CLOSED,
}

enum class DayRuntimeClosedBy {
    USER,
    AUTO,
}

/** Canonical persisted representation of one calendar day. */
data class DayPlan(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val dayKey: LocalDayKey,
    val name: String?,
    val linkedProjectIds: List<String>,
    val linkedAttachmentIds: List<String>,
    val status: DayStatus,
    val reflection: String?,
    val energyLevel: Int?,
    val mood: String?,
    val weatherConditions: String?,
    val predictedDurationMinutes: Long?,
    val totalPlannedMinutes: Long,
    val totalCompletedMinutes: Long,
    val completionPercentage: Float,
) : SyncEntityMeta

/**
 * Canonical concrete task belonging to one DayPlan.
 * A recurring occurrence is represented by recurrence provenance, not a separate
 * persistent occurrence entity.
 */
data class DayTask(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val dayPlanId: String,
    val recurrence: RecurrenceOrigin?,
    val title: String,
    val description: String?,
    val goalId: String?,
    val projectId: String?,
    val linkedProjectIds: List<String>,
    val linkedAttachmentIds: List<String>,
    val activityRecordId: String?,
    val taskType: String?,
    val entityId: String?,
    val order: Long,
    val priority: TaskPriority,
    val status: TaskStatus,
    val completed: Boolean,
    val scheduledTime: Long?,
    val estimatedDurationMinutes: Long?,
    val actualDurationMinutes: Long?,
    val dueTime: Long?,
    val executionStrictness: String?,
    val valueImportance: Float,
    val valueImpact: Float,
    val effort: Float,
    val cost: Float,
    val risk: Float,
    val location: String?,
    val tags: List<String>,
    val notes: String?,
    val completedAt: Long?,
    val points: Int,
) : SyncEntityMeta

/** Canonical concrete focus/responsibility item belonging to one DayPlan. */
data class DayFocusItem(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val dayPlanId: String,
    val recurrence: RecurrenceOrigin?,
    val title: String,
    val notes: String?,
    val relatedLinks: List<Any?>,
    val type: DayFocusType,
    val budgetPercent: Int?,
    val order: Long,
) : SyncEntityMeta

data class DayManagementRuntimeState(
    val sessionId: String?,
    val calendarAnchorDayKey: LocalDayKey?,
    val wokeAt: Long?,
    val sleepAt: Long?,
    val currentPhase: DayManagementPhase,
    val phaseStartedAt: Long?,
    val dayFocusFinalizedAt: Long?,
    val dayPlanFinalizedAt: Long?,
    val implementationStartedAt: Long?,
    val finalizationStartedAt: Long?,
    val activeAlarmIds: List<String>,
    val riskFlags: List<String>,
    val updatedAt: Long?,
    val workingDayKey: LocalDayKey?,
    val closedBy: DayRuntimeClosedBy?,
    val autoClosedAt: Long?,
    val autoCloseReason: String?,
)

/** Read/application projection. It is not a replicated database entity. */
data class DayBoard(
    val plan: DayPlan,
    val focusItems: List<DayFocusItem>,
    val tasks: List<DayTask>,
)

/** Persisted canonical Day + Recurrence state, including tombstones. */
data class CanonicalDayDatabase(
    val dayPlans: List<DayPlan>,
    val dayTasks: List<DayTask>,
    val dayFocusItems: List<DayFocusItem>,
    val recurringSeries: List<RecurringSeries>,
    val dayManagementRuntimeState: DayManagementRuntimeState?,
)

const val DAY_MODEL_VERSION: Int = 2
