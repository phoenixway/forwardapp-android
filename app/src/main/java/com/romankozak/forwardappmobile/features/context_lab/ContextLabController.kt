package com.romankozak.forwardappmobile.features.context_lab

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.Context
import com.romankozak.forwardappmobile.core.context.ContextConfiguration
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.ContextRole
import com.romankozak.forwardappmobile.core.navigation.capability.ViewRegistry

class ContextLabController(
    private val roles: Map<String, ContextRole>,
    private val viewRegistry: ViewRegistry
) {
    // Список контекстів у пам'яті
    private var contexts = mutableMapOf<ContextId, Context>()

    // Поточний активний контекст
    private var activeContextId: ContextId? = null

    // Отримати список доступних ролей
    fun getAvailableRoles(): List<ContextRole> = roles.values.toList()

    // Отримати роль за кодом
    fun getRole(roleCode: String): ContextRole? = roles[roleCode]

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

    // Створити контекст без активації
    fun createContext(roleCode: String, id: String): Context {
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
        return newContext
    }

    // Змінити роль контексту
    fun changeRole(contextId: ContextId, newRoleCode: String) {
        val context = contexts[contextId] ?: return
        val newRole = roles[newRoleCode] ?: error("Role not found")

        contexts[contextId] = context.copy(
            role = newRole,
            config = context.config.copy(
                baseRoleCode = newRole.code,
                activeCapabilities = newRole.defaultCapabilities,
                activeViews = newRole.availableViews,
                currentView = newRole.startView
            )
        )
    }

    fun toggleCapability(contextId: ContextId, capId: CapabilityId) {
        val context = contexts[contextId] ?: return
        val currentCaps = context.config.activeCapabilities

        val newCaps = if (currentCaps.contains(capId)) {
            currentCaps - capId
        } else {
            currentCaps + capId
        }

        // --- Start of preventative fix ---

        // 1. Get all views for the new set of active capabilities.
        val newAvailableViews = newCaps
            .flatMap { capabilityId -> viewRegistry.getForCapability(capabilityId) }
            .map { descriptor -> descriptor.id }
            .toSet()

        // 2. Check if the current start view is still valid
        val currentViewIsValid = newAvailableViews.contains(context.config.currentView)

        val newStartView = if (currentViewIsValid) {
            context.config.currentView
        } else {
            // If not, pick the first available view as the new start view.
            // If the set is empty, this will fallback to the old (invalid) one.
            // The safeguard in SwitchContextUseCase will prevent a crash on activation.
            newAvailableViews.firstOrNull() ?: context.config.currentView
        }

        // --- End of preventative fix ---

        contexts[contextId] = context.copy(
            config = context.config.copy(
                activeCapabilities = newCaps,
                activeViews = newAvailableViews, // Also update activeViews
                currentView = newStartView // Update the currentView
            )
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
