package com.romankozak.forwardappmobile.core.gate

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilityRegistry
import com.romankozak.forwardappmobile.core.context.ContextController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CapabilityGate @Inject constructor(
    private val registry: CapabilityRegistry,
    private val contextController: ContextController
) {
    /**
     * Визначає, чи є можливість активною в поточному контексті.
     */
    fun isEnabled(id: CapabilityId): Boolean {
        // 1. Перевіряємо, чи така можливість взагалі зареєстрована в системі
        if (registry.get(id) == null) return false

        // 2. Отримуємо поточний стан контексту
        val currentState = contextController.current()

        // 3. Перевіряємо, чи ID можливості є в наборі активних фіч [cite: 2, 5]
        return currentState.features.active.contains(id)
    }
}