package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry

class ContextCapabilitiesResolver {
    fun resolve(config: ContextConfiguration): Set<CapabilityId> {
        return buildSet {
            addAll(ContextRoleRegistry.getCapabilitiesForRole(config.basePresetCode))

            if (config.enableInbox != false) add(CapabilityId("inbox"))
            if (config.enableLog != false) add(CapabilityId("log"))
            if (config.enableArtifact != false) add(CapabilityId("artifact"))
            if (config.enableDashboard != false) add(CapabilityId("dashboard"))
            if (config.enableBacklog != false) add(CapabilityId("backlog"))
            if (config.enableAttachments != false) add(CapabilityId("attachments"))

            if (config.enableAdvanced == true) add(CapabilityId("advanced"))
            if (config.enableAutoLinkSubprojects == true) add(CapabilityId("auto_link_subprojects"))
            config.experimentalCapabilityIds.forEach { id ->
                val normalized = runCatching { id.raw.trim() }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return@forEach
                add(CapabilityId(normalized))
            }
        }
    }
}
