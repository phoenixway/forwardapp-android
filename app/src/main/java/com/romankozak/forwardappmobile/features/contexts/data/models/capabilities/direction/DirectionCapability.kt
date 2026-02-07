package com.romankozak.forwardappmobile.features.contexts.data.models.capabilities.direction

import com.romankozak.forwardappmobile.core.capability.Capability
import com.romankozak.forwardappmobile.core.capability.CapabilityDescriptor
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilityRuntime
import com.romankozak.forwardappmobile.core.context.ViewId

object DirectionCapability : Capability {
    override val descriptor =
        object : CapabilityDescriptor {
            override val id = CapabilityId("direction")
            override val label: String = "Directions"
            override val iconRes: Int? = null
            override val navRoute: String = "direction_root"
            override val supportedViews: Set<ViewId> = setOf(ViewId("direction_main"))
        }

    override fun register(runtime: CapabilityRuntime) {
        // No-op for now.
    }
}
