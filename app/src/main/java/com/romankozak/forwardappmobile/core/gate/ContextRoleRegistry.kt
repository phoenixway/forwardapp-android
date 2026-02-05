package com.romankozak.forwardappmobile.core.gate

import com.romankozak.forwardappmobile.core.capability.CapabilityId

/**
 * Реєстр пресетів ролей.
 * Визначає, які можливості (CapabilityId) автоматично доступні для конкретної ролі.
 */
object ContextRoleRegistry {
    const val ROLE_VET_PATIENT = "vet_patient"
    const val ROLE_DEVELOPMENT = "development"
    const val ROLE_DEFAULT = "default"

    // Явно вказуємо типи Map<String, Set<CapabilityId>>, щоб допомогти компілятору
    private val roleCapabilities: Map<String, Set<CapabilityId>> =
        mapOf(
            ROLE_VET_PATIENT to
                setOf(
                    CapabilityId("notes"),
                    CapabilityId("treatment_plan"),
                    CapabilityId("attachments"),
                ),
            ROLE_DEVELOPMENT to
                setOf(
                    CapabilityId("code_index"),
                    CapabilityId("log"),
                    CapabilityId("backlog"),
                ),
            ROLE_DEFAULT to
                setOf(
                    CapabilityId("inbox"),
                    CapabilityId("log"),
                ),
        )

    /**
     * Повертає набір ідентифікаторів можливостей для вказаного коду ролі.
     */
    fun getCapabilitiesForRole(roleCode: String?): Set<CapabilityId> {
        if (roleCode == null) return emptySet()
        return roleCapabilities[roleCode] ?: emptySet()
    }
}
