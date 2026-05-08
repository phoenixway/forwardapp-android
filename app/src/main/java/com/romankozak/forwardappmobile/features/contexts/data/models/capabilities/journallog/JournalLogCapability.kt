package com.romankozak.forwardappmobile.features.contexts.data.models.capabilities.journallog

import com.romankozak.forwardappmobile.core.capability.Capability
import com.romankozak.forwardappmobile.core.capability.CapabilityDescriptor
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilityRuntime
import com.romankozak.forwardappmobile.core.context.ViewId

object JournalLogCapability : Capability {
    override val descriptor =
        object : CapabilityDescriptor {
            override val id = CapabilityId("journal_log")
            override val label: String = "Journal Log"
            override val iconRes: Int? = null
            override val navRoute: String = "journal_log"
            override val supportedViews: Set<ViewId> = setOf(ViewId("journal_log"))
        }

    override fun register(runtime: CapabilityRuntime) {
        // Context screen renders this capability directly.
    }
}
