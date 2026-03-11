package com.romankozak.forwardappmobile.core.navigation.capability

import com.romankozak.forwardappmobile.core.context.ViewId
import com.romankozak.forwardappmobile.core.gate.CapabilityGate

class ContextAwareViewResolver(
    private val viewRegistry: ViewRegistry,
    private val capabilityGate: CapabilityGate, // Шлюз, який ми створили раніше
) : ViewResolver {
    override fun resolve(viewId: ViewId): ScreenId {
        val descriptor =
            viewRegistry.get(viewId)
                ?: error("View $viewId not registered")

        // ПЕРЕВІРКА: чи дозволена можливість, якій належить ця в'юха
        check(capabilityGate.isEnabled(descriptor.ownerCapability)) {
            "Access denied to view: ${viewId.raw}"
        }

        return descriptor.screenId
    }
}
