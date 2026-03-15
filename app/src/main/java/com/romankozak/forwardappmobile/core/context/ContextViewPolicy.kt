package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import java.util.Locale

private const val DASHBOARD_PRIORITY = 0
private const val BACKLOG_PRIORITY = 1
private const val INBOX_PRIORITY = 2
private const val ADVANCED_PRIORITY = 3
private const val CONNECTIONS_PRIORITY = 4
private const val DIRECTION_PRIORITY = 5
private const val NOTES_PRIORITY = 6
private const val LOG_PRIORITY = 7
private const val ARTIFACT_PRIORITY = 8
private const val KEY_PROBLEMS_PRIORITY = 9
private const val VET_CASE_PRIORITY = 10

object ContextViewPolicy {
    fun availableViews(enabled: Set<CapabilityId>): List<ContextViewMode> {
        return ContextViewMode.entries
            .filter { mode -> enabled.contains(mode.toCapabilityId()) }
            .sortedBy { it.orderPriority() }
            .reversed()
    }

    fun resolveView(
        available: List<ContextViewMode>,
        preferred: ContextViewMode?,
        current: ContextViewMode,
    ): ContextViewMode {
        return when {
            preferred != null && preferred in available -> preferred
            current in available -> current
            available.isNotEmpty() -> available.first()
            else -> current
        }
    }

    fun ContextViewMode.toCapabilityId(): CapabilityId {
        return when (this) {
            ContextViewMode.ADVANCED -> CapabilityId("advanced")
            ContextViewMode.ARTIFACT -> CapabilityId("advanced")
            else -> CapabilityId(this.name.lowercase(Locale.ROOT))
        }
    }

    private fun ContextViewMode.orderPriority() =
        when (this) {
            ContextViewMode.DASHBOARD -> DASHBOARD_PRIORITY
            ContextViewMode.BACKLOG -> BACKLOG_PRIORITY
            ContextViewMode.INBOX -> INBOX_PRIORITY
            ContextViewMode.ADVANCED -> ADVANCED_PRIORITY
            ContextViewMode.CONNECTIONS -> CONNECTIONS_PRIORITY
            ContextViewMode.DIRECTION -> DIRECTION_PRIORITY
            ContextViewMode.NOTES -> NOTES_PRIORITY
            ContextViewMode.LOG -> LOG_PRIORITY
            ContextViewMode.ARTIFACT -> ARTIFACT_PRIORITY
            ContextViewMode.KEY_PROBLEMS -> KEY_PROBLEMS_PRIORITY
            ContextViewMode.VET_CASE -> VET_CASE_PRIORITY
        }
}
