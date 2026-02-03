package com.romankozak.forwardappmobile.core.gate

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilityRegistry
import com.romankozak.forwardappmobile.core.context.ContextController
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CapabilityGate @Inject constructor(
    private val registry: CapabilityRegistry,
    private val contextController: ContextController
) {
    fun isEnabled(id: CapabilityId): Boolean {
        if (registry.get(id) == null) return false

        val currentState = contextController.current()
        if (currentState.features.active.contains(id)) return true

        val config = (currentState as? ConfigurableState)?.config ?: return false

        // Тепер ContextRoleRegistry має бути видимим
        val enabledByRole = ContextRoleRegistry.getCapabilitiesForRole(config.basePresetCode).contains(id)

        return enabledByRole || 
               config.experimentalCapabilityIds.contains(id) || 
               isLegacyEnabled(id, config)
    }

    private fun isLegacyEnabled(id: CapabilityId, config: ContextConfiguration): Boolean {
        return when (id.raw) {
            "inbox" -> config.enableInbox == true
            "log" -> config.enableLog == true
            "artifact" -> config.enableArtifact == true
            "advanced" -> config.enableAdvanced == true
            "dashboard" -> config.enableDashboard == true
            "backlog" -> config.enableBacklog == true
            "attachments" -> config.enableAttachments == true
            "auto_link_subprojects" -> config.enableAutoLinkSubprojects == true
            else -> false
        }
    }
}
