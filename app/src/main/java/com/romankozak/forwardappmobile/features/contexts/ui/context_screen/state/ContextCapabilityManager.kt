package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.gate.CapabilityGate
import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.ContextCapabilityHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Управляє можливостями (capabilities) контексту
 */
class ContextCapabilityManager(
    private val capabilityGate: CapabilityGate,
    private val capabilityHandler: ContextCapabilityHandler,
) {
    private val _enabledCapabilities = MutableStateFlow<Set<CapabilityId>>(emptySet())
    val enabledCapabilities: StateFlow<Set<CapabilityId>> = _enabledCapabilities.asStateFlow()

    /**
     * Оновлює набір активних можливостей на основі конфігурації
     */
    fun updateCapabilities(config: ContextConfiguration) {
        _enabledCapabilities.value = calculateEffectiveCapabilities(config)
    }

    /**
     * Перевіряє, чи доступна певна можливість
     */
    fun hasCapability(capabilityId: CapabilityId): Boolean {
        return capabilityGate.isEnabled(capabilityId) &&
            _enabledCapabilities.value.contains(capabilityId)
    }

    /**
     * Повертає всі активні можливості
     */
    fun getEnabledCapabilities(): Set<CapabilityId> {
        return _enabledCapabilities.value
    }

    /**
     * Розраховує фінальний набір можливостей для контексту.
     * Об'єднує дані з трьох джерел:
     * 1. Пресет (Роль)
     * 2. Експериментальні фічі
     * 3. Legacy-прапорці
     */
    private fun calculateEffectiveCapabilities(config: ContextConfiguration): Set<CapabilityId> {
        return buildSet {
            // 1. Можливості з пресету ролі
            val preset = config.basePresetCode ?: "default"
            addAll(ContextRoleRegistry.getCapabilitiesForRole(preset))

            // 2. Кастомні експериментальні можливості
            addAll(config.experimentalCapabilityIds)

            // 3. Legacy-прапорці (null трактуємо як true для базових фіч)
            if (config.enableInbox != false) add(CapabilityId("inbox"))
            if (config.enableLog != false) add(CapabilityId("log"))
            if (config.enableArtifact != false) add(CapabilityId("artifact"))
            if (config.enableDashboard != false) add(CapabilityId("dashboard"))
            if (config.enableBacklog != false) add(CapabilityId("backlog"))
            if (config.enableAttachments != false) add(CapabilityId("attachments"))

            // Додаткові опціональні можливості
            if (config.enableAdvanced == true) add(CapabilityId("advanced"))
            if (config.enableAutoLinkSubprojects == true) add(CapabilityId("auto_link_subprojects"))
        }
    }
}
