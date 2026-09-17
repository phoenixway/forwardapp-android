package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import com.romankozak.forwardappmobile.core.capability.CapabilityId

/**
 * Resolves the input panel's effective capability set.
 *
 * Canonical overrides are replacement authority and therefore run last:
 * true adds the capability and false removes it from every legacy/session
 * source. Unrelated legacy capabilities remain untouched.
 */
internal fun resolveInputPanelCapabilities(
    enableInbox: Boolean,
    enableLog: Boolean,
    enableBacklog: Boolean,
    enableDashboard: Boolean,
    enableAttachments: Boolean,
    experimentalCapabilityIds: List<CapabilityId>,
    enabledCapabilitiesOverride: Set<CapabilityId>?,
    canonicalCapabilityOverrides: Map<CapabilityId, Boolean>,
): Set<CapabilityId> =
    buildSet {
        if (enableInbox) add(CapabilityId("inbox"))
        if (enableLog) add(CapabilityId("log"))
        if (enableBacklog) add(CapabilityId("backlog"))
        if (enableDashboard) add(CapabilityId("dashboard"))
        if (enableAttachments) add(CapabilityId("connections"))

        experimentalCapabilityIds.forEach { id ->
            val normalized = id.raw.trim().takeIf { it.isNotEmpty() } ?: return@forEach
            add(CapabilityId(normalized))
        }
        enabledCapabilitiesOverride?.forEach { add(it) }
    }.let { legacyAndSession ->
        canonicalCapabilityOverrides.entries.fold(legacyAndSession) { capabilities, (id, enabled) ->
            if (enabled) capabilities + id else capabilities - id
        }
    }
