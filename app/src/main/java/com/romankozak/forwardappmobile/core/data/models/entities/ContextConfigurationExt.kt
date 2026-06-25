package com.romankozak.forwardappmobile.core.data.models.entities

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextCapabilitiesResolver

fun ContextConfiguration.has(id: CapabilityId): Boolean {
    return ContextCapabilitiesResolver().resolve(this).contains(id)
}
