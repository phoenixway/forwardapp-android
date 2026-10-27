package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import com.romankozak.forwardappmobile.core.capability.CapabilityId

/** Presentation names may evolve; canonical capability/view IDs must not. */
internal fun featureLabelToCapabilityId(label: String): CapabilityId = when (label) {
    "Inbox" -> CapabilityId("inbox")
    "Log" -> CapabilityId("log")
    "Dashboard" -> CapabilityId("dashboard")
    "Semantics", "Related entities", "Зв’язки" -> CapabilityId("relations")
    "Backlog" -> CapabilityId("backlog")
    "Attachments", "Connections" -> CapabilityId("connections")
    "Issues" -> CapabilityId("key_problems")
    "Directions" -> CapabilityId("direction")
    else -> CapabilityId(label.lowercase().replace(" ", "_"))
}
