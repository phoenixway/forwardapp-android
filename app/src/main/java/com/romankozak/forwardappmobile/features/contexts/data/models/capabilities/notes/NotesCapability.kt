package com.romankozak.forwardappmobile.features.contexts.data.models.capabilities.notes

import com.romankozak.forwardappmobile.core.capability.*

object NotesCapability : Capability {

    override val descriptor = object : CapabilityDescriptor {
        override val id = CapabilityId("notes")
        override val title = "Notes"
    }

    override fun register(runtime: CapabilityRuntime) {
        TODO("Not yet implemented")
    }
}

