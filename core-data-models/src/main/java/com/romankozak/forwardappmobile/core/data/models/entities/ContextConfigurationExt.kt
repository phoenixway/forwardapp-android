
package com.romankozak.forwardappmobile.core.data.models.entities

import com.romankozak.forwardappmobile.core.capability.CapabilityId

// Тепер у будь-якому місці коду ти можеш написати: config.has(CapabilityId("notes"))
fun ContextConfiguration.has(id: CapabilityId): Boolean {
    return activeCapabilities.contains(id) || 
           com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry
               .getCapabilitiesForRole(baseRoleCode).contains(id)
}
