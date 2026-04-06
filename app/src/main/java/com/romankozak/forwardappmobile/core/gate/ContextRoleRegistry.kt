package com.romankozak.forwardappmobile.core.gate

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import java.util.Locale

/**
 * Реєстр пресетів ролей.
 * Визначає, які можливості (CapabilityId) автоматично доступні для конкретної ролі.
 */
object ContextRoleRegistry {
    data class ReservedBaseRoleDefinition(
        val code: String,
        val label: String,
        val description: String,
        val capabilities: Set<CapabilityId>,
    )

    const val ROLE_PROJECT = "project"
    const val ROLE_DIRECTION = "direction"
    const val ROLE_ASPECT = "aspect"
    const val ROLE_MAIN_BEACON = "main-beacon"
    const val ROLE_MANAGEMENT = "management"
    const val ROLE_CRISIS_CASE = "crisis_case"

    const val ROLE_VET_PATIENT = "vet_patient"
    const val ROLE_DEVELOPMENT = "development"
    const val ROLE_DEFAULT = "default"

    private val reservedRoleDefinitions: List<ReservedBaseRoleDefinition> =
        listOf(
            ReservedBaseRoleDefinition(
                code = ROLE_PROJECT,
                label = "Проєкт",
                description = "Базова роль проєкту",
                capabilities =
                    setOf(
                        CapabilityId("backlog"),
                        CapabilityId("artifact"),
                    ),
            ),
            ReservedBaseRoleDefinition(
                code = ROLE_DIRECTION,
                label = "Напрямок",
                description = "Базова роль напрямку",
                capabilities =
                    setOf(
                        CapabilityId("direction"),
                        CapabilityId("artifact"),
                    ),
            ),
            ReservedBaseRoleDefinition(
                code = ROLE_ASPECT,
                label = "Аспект",
                description = "Базова роль аспекту",
                capabilities =
                    setOf(
                        CapabilityId("dashboard"),
                    ),
            ),
            ReservedBaseRoleDefinition(
                code = ROLE_MAIN_BEACON,
                label = "Main Beacon",
                description = "Базова роль головного маяка",
                capabilities =
                    setOf(
                        CapabilityId("direction"),
                    ),
            ),
            ReservedBaseRoleDefinition(
                code = ROLE_MANAGEMENT,
                label = "Management",
                description = "Базова роль менеджменту",
                capabilities =
                    setOf(
                        CapabilityId("backlog"),
                        CapabilityId("inbox"),
                    ),
            ),
            ReservedBaseRoleDefinition(
                code = ROLE_CRISIS_CASE,
                label = "Crisis Case",
                description = "Роль кризового кейсу",
                capabilities =
                    setOf(
                        CapabilityId("direction"),
                        CapabilityId("backlog"),
                        CapabilityId("inbox"),
                        CapabilityId("artifact"),
                        CapabilityId("log"),
                        CapabilityId("key_problems"),
                    ),
            ),
        )

    private val reservedRoleCapabilities: Map<String, Set<CapabilityId>> =
        reservedRoleDefinitions.associate { it.code to it.capabilities }

    // Явно вказуємо типи Map<String, Set<CapabilityId>>, щоб допомогти компілятору
    private val customRoleCapabilities: Map<String, Set<CapabilityId>> =
        mapOf(
            ROLE_VET_PATIENT to setOf(CapabilityId("connections")),
            ROLE_DEVELOPMENT to
                setOf(
                    CapabilityId("log"),
                    CapabilityId("backlog"),
                ),
            ROLE_DEFAULT to
                setOf(
                    CapabilityId("dashboard"),
                ),
        )

    /**
     * Повертає набір ідентифікаторів можливостей для вказаного коду ролі.
     */
    fun getCapabilitiesForRole(roleCode: String?): Set<CapabilityId> {
        if (roleCode == null) return emptySet()
        val normalizedCode = roleCode.trim().lowercase(Locale.ROOT)
        return reservedRoleCapabilities[normalizedCode]
            ?: customRoleCapabilities[normalizedCode]
            ?: emptySet()
    }

    fun isReservedBaseRole(roleCode: String?): Boolean {
        if (roleCode.isNullOrBlank()) return false
        return reservedRoleCapabilities.containsKey(roleCode.trim().lowercase(Locale.ROOT))
    }

    fun getReservedBaseRoleDefinitions(): List<ReservedBaseRoleDefinition> = reservedRoleDefinitions

    fun getReservedBaseRoleDefinition(roleCode: String?): ReservedBaseRoleDefinition? {
        if (roleCode.isNullOrBlank()) return null
        val normalizedCode = roleCode.trim().lowercase(Locale.ROOT)
        return reservedRoleDefinitions.firstOrNull { it.code == normalizedCode }
    }

    /**
     * Повертає повний набір всіх відомих ідентифікаторів можливостей.
     */
    fun getAllKnownCapabilities(): Set<CapabilityId> {
        return buildSet {
            // Збираємо можливості, визначені в ролях
            reservedRoleCapabilities.values.forEach { addAll(it) }
            customRoleCapabilities.values.forEach { addAll(it) }

            // Додаємо можливості, які можуть не входити в жодну роль за замовчуванням,
            // але є частиною загальної системи або legacy-прапорців.
            // inbox, log, backlog, connections вже покриті ролями або іншими джерелами.
            add(CapabilityId("artifact"))
            add(CapabilityId("dashboard"))
            add(CapabilityId("direction"))
            add(CapabilityId("key_problems"))
            add(CapabilityId("inbox_sorting"))
            add(CapabilityId("connections"))
        }
    }
}
