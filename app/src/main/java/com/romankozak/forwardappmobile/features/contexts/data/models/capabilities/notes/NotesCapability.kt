package com.romankozak.forwardappmobile.features.contexts.data.models.capabilities.notes

import com.romankozak.forwardappmobile.core.capability.*

object NotesCapability : Capability {

    override val descriptor = CapabilityDescriptor(
        id = CapabilityId("notes"),
        title = "Notes",
    )

    override fun register(runtime: CapabilityRuntime) {
        // ПОКИ ПУСТО
        // runtime.registerScreen(...)
        // runtime.registerRule(...)
    }
}
