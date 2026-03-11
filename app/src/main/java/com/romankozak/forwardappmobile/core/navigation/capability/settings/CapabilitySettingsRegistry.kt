package com.romankozak.forwardappmobile.core.navigation.capability.settings

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import javax.inject.Inject

interface CapabilitySettingsRegistry {
    fun all(): List<CapabilitySettingsEntry>

    fun forCapabilities(enabledCapabilities: Set<CapabilityId>): List<CapabilitySettingsEntry>
}

class InMemoryCapabilitySettingsRegistry
    @Inject
    constructor(
        entries: Set<@JvmSuppressWildcards CapabilitySettingsEntry>,
    ) : CapabilitySettingsRegistry {
        private val ordered =
            entries
                .toList()
                .sortedWith(
                    compareBy<CapabilitySettingsEntry> { it.descriptor.order }
                        .thenBy { it.descriptor.tabTitle },
                )

        override fun all(): List<CapabilitySettingsEntry> = ordered

        override fun forCapabilities(enabledCapabilities: Set<CapabilityId>): List<CapabilitySettingsEntry> =
            ordered.filter { enabledCapabilities.contains(it.descriptor.ownerCapability) }
    }
