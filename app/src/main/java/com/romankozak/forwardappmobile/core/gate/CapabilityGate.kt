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
    /**
     * Визначає, чи є можливість активною в поточному контексті.
     * Об'єднує стару логіку прапорців та нову систему ID.
     */
    // У CapabilityGate.kt
fun isEnabled(id: CapabilityId): Boolean {
    val currentState = contextController.current()
    val config = (currentState as? ConfigurableState)?.config ?: return false

    // 1. Перевірка через роль (пресет)
    val enabledByRole = ContextRoleRegistry
        .getCapabilitiesForRole(config.basePresetCode)
        .contains(id)

    // 2. Перевірка через прямі ID або старі прапорці
    return enabledByRole || 
           config.experimentalCapabilityIds.contains(id) || 
           isLegacyEnabled(id, config)
}


    /**
     * Мапінг ідентифікаторів можливостей на старі boolean-поля конфігурації
     */
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

/**
 * Тимчасовий інтерфейс для мікрокроку, щоб CapabilityGate міг дістати конфігурацію.
 * Переконайтеся, що ваші класи станів (наприклад, ContextState) реалізують цей інтерфейс.
 */
interface ConfigurableState {
    val config: ContextConfiguration
}
