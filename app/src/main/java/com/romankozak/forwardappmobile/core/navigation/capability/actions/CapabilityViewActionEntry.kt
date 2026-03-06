package com.romankozak.forwardappmobile.core.navigation.capability.actions

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode

data class CapabilityViewActionDescriptor(
    val id: String,
    val ownerCapability: CapabilityId,
    val viewMode: ContextViewMode,
    val title: String,
    val description: String? = null,
    val order: Int = 0,
)

interface CapabilityViewActionEntry {
    val descriptor: CapabilityViewActionDescriptor
}

object CapabilityViewActionIds {
    const val BACKLOG_IMPORT_MARKDOWN = "backlog.import_markdown"
    const val BACKLOG_EXPORT_MARKDOWN = "backlog.export_markdown"
    const val DIRECTION_COPY_LINKED_BACKLOGS_AS_LINKS = "direction.copy_linked_backlogs_as_links"
}
