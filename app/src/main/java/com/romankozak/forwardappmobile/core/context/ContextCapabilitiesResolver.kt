package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry

class ContextCapabilitiesResolver {
    fun resolve(config: ContextConfiguration): Set<CapabilityId> {
        val roleCapabilities = ContextRoleRegistry.getCapabilitiesForRole(config.basePresetCode)
        return buildSet {
            // Legacy capabilities: explicit false in config overrides role defaults.
            if (isEnabled(roleCapabilities, "inbox", config.enableInbox, defaultEnabled = true)) add(CapabilityId("inbox"))
            if (isEnabled(roleCapabilities, "log", config.enableLog, defaultEnabled = true)) add(CapabilityId("log"))
            if (isEnabled(roleCapabilities, "dashboard", config.enableDashboard, defaultEnabled = true)) add(CapabilityId("dashboard"))
            if (isEnabled(roleCapabilities, "backlog", config.enableBacklog, defaultEnabled = true)) add(CapabilityId("backlog"))
            if (isEnabled(roleCapabilities, "attachments", config.enableAttachments, defaultEnabled = true)) add(CapabilityId("attachments"))
            if (isEnabled(roleCapabilities, "advanced", config.enableAdvanced, defaultEnabled = false)) add(CapabilityId("advanced"))

            // Non-legacy role capabilities are still provided by role.
            roleCapabilities.forEach { cap ->
                if (cap.raw !in LEGACY_CAPABILITY_IDS) {
                    add(cap)
                }
            }

            config.experimentalCapabilityIds.forEach { id ->
                val normalized = runCatching { id.raw.trim() }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return@forEach
                if (normalized == "artifact") {
                    add(CapabilityId("advanced"))
                } else {
                    add(CapabilityId(normalized))
                }
            }
        }
    }

    private fun isEnabled(
        roleCapabilities: Set<CapabilityId>,
        capabilityRaw: String,
        override: Boolean?,
        defaultEnabled: Boolean,
    ): Boolean {
        if (override != null) return override
        return roleCapabilities.contains(CapabilityId(capabilityRaw)) || defaultEnabled
    }

    private companion object {
        val LEGACY_CAPABILITY_IDS =
            setOf(
                "inbox",
                "log",
                "dashboard",
                "backlog",
                "attachments",
                "advanced",
            )
    }
}
