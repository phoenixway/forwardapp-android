package com.romankozak.forwardappmobile.features.context_lab

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.Context
import com.romankozak.forwardappmobile.core.context.ContextConfiguration
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.ContextRole

// Файл: features/context_lab/ContextLabController.kt

class ContextLabController(
    private val roles: Map<String, ContextRole>
) {
    // Список контекстів у пам'яті
    private var contexts = mutableMapOf<ContextId, Context>()

    // Поточний активний контекст
    private var activeContextId: ContextId? = null

    fun createAndActivate(roleCode: String, id: String) {
        val role = roles[roleCode] ?: error("Role not found")
        val contextId = ContextId(id)

        val newContext = Context(
            id = contextId,
            role = role,
            config = ContextConfiguration(
                activeCapabilities = role.defaultCapabilities,
                activeViews = role.availableViews,
                currentView = role.startView,
                contextId = ContextId(id),
                baseRoleCode = role.code
            )
        )

        contexts[contextId] = newContext
        activeContextId = contextId
    }

    fun toggleCapability(contextId: ContextId, capId: CapabilityId) {
        val context = contexts[contextId] ?: return
        val currentCaps = context.config.activeCapabilities

        val newCaps = if (currentCaps.contains(capId)) {
            currentCaps - capId
        } else {
            currentCaps + capId
        }

        contexts[contextId] = context.copy(
            config = context.config.copy(activeCapabilities = newCaps)
        )
    }

    fun activate(id: ContextId) {
        if (contexts.containsKey(id)) {
            activeContextId = id
        }
    }

    fun getActiveContext(): Context? = activeContextId?.let { contexts[it] }

    fun getAllContexts(): List<Context> = contexts.values.toList()
}