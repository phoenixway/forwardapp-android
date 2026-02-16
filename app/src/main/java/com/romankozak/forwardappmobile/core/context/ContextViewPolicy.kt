package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import java.util.Locale

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
            else -> CapabilityId(this.name.lowercase(Locale.ROOT))
        }
    }

    private fun ContextViewMode.orderPriority() =
        when (this) {
            ContextViewMode.DASHBOARD -> 0
            ContextViewMode.BACKLOG -> 1
            ContextViewMode.INBOX -> 2
            ContextViewMode.ADVANCED -> 3
            ContextViewMode.ATTACHMENTS -> 4
            ContextViewMode.DIRECTION -> 5
            ContextViewMode.NOTES -> 6
            ContextViewMode.LOG -> 7
            ContextViewMode.ARTIFACT -> 8
            ContextViewMode.KEY_PROBLEMS -> 9
            ContextViewMode.VET_CASE -> 10
        }
}
