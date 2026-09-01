package com.romankozak.forwardappmobile.core.gate

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilityRegistry
import com.romankozak.forwardappmobile.core.context.ConfigurableState
import com.romankozak.forwardappmobile.core.context.ContextController
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Центральний шлюз для перевірки доступності функцій (capabilities).
 * Визначає, чи має користувач доступ до певного модуля в поточному контексті.
 */
@Singleton
class CapabilityGate
    @Inject
    constructor(
        private val registry: CapabilityRegistry,
        private val contextController: ContextController,
    ) {
        /**
         * Головна функція перевірки.
         * Логіка пріоритетів:
         * 1. Реєстр (чи взагалі існує така фіча?)
         * 2. Динамічний стан (що активовано в пам'яті прямо зараз)
         * 3. Роль/Пресет (наприклад, "vet_patient")
         * 4. Експериментальні ID (конкретно додані в БД для цього проекту)
         * 5. Legacy-прапорці (старі колонки enable_inbox тощо)
         */
        fun isEnabled(id: CapabilityId): Boolean {
            val currentState = contextController.current()
            if (id.raw == "dashboard") {
                return isRegisteredOrLegacyAlias(id) && currentState.features.active.contains(id)
            }
            val config = (currentState as? ConfigurableState)?.config
            val enabledByRole = config?.let { currentConfig ->
                val useRoleDefaults =
                    !currentConfig.applyMode.equals(APPLY_MODE_OVERRIDE, ignoreCase = true)
                useRoleDefaults &&
                    ContextRoleRegistry.getCapabilitiesForRole(currentConfig.basePresetCode)
                        .contains(id)
            } == true

            if (!isRegisteredOrLegacyAlias(id)) return false

            if (id.raw == "log") {
                return currentState.features.active.contains(id)
            }

            return currentState.features.active.contains(id) ||
                (config != null && (
                    enabledByRole ||
                        config.experimentalCapabilityIds.contains(id) ||
                        isLegacyEnabled(id, config)
                ))
        }

        private fun isRegisteredOrLegacyAlias(id: CapabilityId): Boolean {
            if (registry.get(id) != null) {
                return true
            }

            return id.raw == "connections" && registry.get(CapabilityId("attachments")) != null
        }

        /**
         * Забезпечує зворотну сумісність зі старими boolean-полями в таблиці БД.
         */
        private fun isLegacyEnabled(
            id: CapabilityId,
            config: ContextConfiguration,
        ): Boolean {
            return when (id.raw) {
                "inbox" -> config.enableInbox == true
                "log" -> config.enableLog == true
                "artifact" -> config.enableArtifact == true
                "dashboard" -> config.enableDashboard == true
                "backlog" -> config.enableBacklog == true
                "attachments" -> config.enableAttachments == true
                "connections" -> config.enableAttachments == true
                else -> false
            }
        }

        private companion object {
            private const val APPLY_MODE_OVERRIDE = "OVERRIDE"
        }
    }
