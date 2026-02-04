package com.romankozak.forwardappmobile.core.data.models.entities

import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry
import com.romankozak.forwardappmobile.core.capability.CapabilityId
// Переконайтеся, що цей імпорт правильний і модуль має доступ до цього пакета
// import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry 

fun ContextConfiguration.has(id: CapabilityId): Boolean {
    // 1. Виправлено: activeCapabilities -> experimentalCapabilityIds
    // 2. Виправлено: baseRoleCode -> basePresetCode
    // 3. Додано перевірку на null для basePresetCode
    
    val isExperimental = experimentalCapabilityIds.contains(id)
    
    val isFromPreset = basePresetCode?.let { preset ->
        ContextRoleRegistry
            .getCapabilitiesForRole(preset)
            .contains(id)
    } ?: false

    return isExperimental || isFromPreset
}
