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
    fun isEnabled(id: CapabilityId): Boolean {
        // 1. Перевіряємо, чи можливість зареєстрована в реєстрі
        if (registry.get(id) == null) return false

        // 2. Отримуємо поточний стан
        val currentState = contextController.current()

        // 3. Пріоритет 1: Перевірка в динамічному наборі (якщо він уже сформований)
        if (currentState.features.active.contains(id)) return true

        // 4. Пріоритет 2: Перевірка через стабільну конфігурацію
        // Примітка: використовуємо ConfigurableState для доступу до даних БД у стані
        val config = (currentState as? ConfigurableState)?.config ?: return false

        return isLegacyEnabled(id, config) || config.experimentalCapabilityIds.contains(id)
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
