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
            // 1. Якщо фіча не зареєстрована в системі — вона вимкнена за замовчуванням
            if (registry.get(id) == null) {
                if (id.raw == "connections" && registry.get(CapabilityId("attachments")) != null) {
                    // Legacy alias support.
                } else {
                    return false
                }
            }

            val currentState = contextController.current()

            // 2. Перевірка в динамічному наборі фіч поточного стану
            if (currentState.features.active.contains(id)) return true

            // Отримуємо конфігурацію через інтерфейс-міст ConfigurableState
            val config = (currentState as? ConfigurableState)?.config ?: return false

            // 3. Перевірка через пресет ролі (ContextRoleRegistry)
            val useRoleDefaults = !config.applyMode.equals(APPLY_MODE_OVERRIDE, ignoreCase = true)
            val enabledByRole = useRoleDefaults && ContextRoleRegistry.getCapabilitiesForRole(config.basePresetCode).contains(id)

            // 4 & 5. Комбінована перевірка: Роль АБО Експериментальний список АБО Старий прапорець
            return enabledByRole ||
                config.experimentalCapabilityIds.contains(id) ||
                isLegacyEnabled(id, config)
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
                "artifact" -> config.enableAdvanced == true
                "advanced" -> config.enableAdvanced == true
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
