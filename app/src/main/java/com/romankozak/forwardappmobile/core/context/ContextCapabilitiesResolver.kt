package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry

class ContextCapabilitiesResolver {
    fun resolve(config: ContextConfiguration): Set<CapabilityId> {
        val useRoleDefaults = !config.applyMode.equals(APPLY_MODE_OVERRIDE, ignoreCase = true)
        val roleCapabilities =
            if (useRoleDefaults) {
                ContextRoleRegistry.getCapabilitiesForRole(config.basePresetCode)
            } else {
                emptySet()
            }
        val useLegacyDefaults = shouldUseLegacyDefaults(config)
        return buildSet {
            addLegacyCapabilities(roleCapabilities, config, useLegacyDefaults)

            // Non-legacy role capabilities are provided only in ADDITIVE mode.
            if (useRoleDefaults) {
                roleCapabilities.forEach { cap ->
                    if (cap.raw !in LEGACY_CAPABILITY_IDS) {
                        add(cap)
                    }
                }
            }

            config.experimentalCapabilityIds.forEach { id ->
                val normalized =
                    runCatching { id.raw.trim() }
                        .getOrNull()
                        ?.takeIf { it.isNotEmpty() }
                        ?: return@forEach
                add(CapabilityId(normalized))
            }
        }
    }

    private fun MutableSet<CapabilityId>.addLegacyCapabilities(
        roleCapabilities: Set<CapabilityId>,
        config: ContextConfiguration,
        useLegacyDefaults: Boolean,
    ) {
        // Legacy capabilities:
        // - for contexts without role/preset we keep only dashboard as the safe default;
        // - explicit overrides in config still win.
        addIfEnabled(roleCapabilities, "inbox", config.enableInbox)
        addIfEnabled(roleCapabilities, "log", config.enableLog)
        addIfEnabled(
            roleCapabilities = roleCapabilities,
            capabilityRaw = "dashboard",
            override = config.enableDashboard,
            defaultEnabled = useLegacyDefaults,
        )
        addIfEnabled(roleCapabilities, "backlog", config.enableBacklog)
        if (
            isEnabledAny(
                roleCapabilities,
                setOf("attachments", "connections"),
                config.enableAttachments,
                defaultEnabled = false,
            )
        ) {
            add(CapabilityId("connections"))
        }
    }

    private fun MutableSet<CapabilityId>.addIfEnabled(
        roleCapabilities: Set<CapabilityId>,
        capabilityRaw: String,
        override: Boolean?,
        defaultEnabled: Boolean = false,
    ) {
        if (isEnabled(roleCapabilities, capabilityRaw, override, defaultEnabled)) {
            add(CapabilityId(capabilityRaw))
        }
    }

    private fun shouldUseLegacyDefaults(config: ContextConfiguration): Boolean {
        val hasPreset = !config.basePresetCode.isNullOrBlank()
        val hasLegacyOverrides =
            config.enableInbox != null ||
                config.enableLog != null ||
                config.enableDashboard != null ||
                config.enableBacklog != null ||
                config.enableAttachments != null ||
                config.enableAdvanced != null
        val hasExperimentalOverrides = config.experimentalCapabilityIds.isNotEmpty()
        return !hasPreset && !hasLegacyOverrides && !hasExperimentalOverrides
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

    private fun isEnabledAny(
        roleCapabilities: Set<CapabilityId>,
        capabilityRaws: Set<String>,
        override: Boolean?,
        defaultEnabled: Boolean,
    ): Boolean {
        if (override != null) return override
        return capabilityRaws.any { raw -> roleCapabilities.contains(CapabilityId(raw)) } || defaultEnabled
    }

    private companion object {
        private const val APPLY_MODE_OVERRIDE = "OVERRIDE"
        val LEGACY_CAPABILITY_IDS =
            setOf(
                "inbox",
                "log",
                "dashboard",
                "backlog",
                "attachments",
                "connections",
            )
    }
}
