// File: NotesCapability.kt

package com.romankozak.forwardappmobile.features.contexts.data.models.capabilities.notes

import com.romankozak.forwardappmobile.core.capability.*
import com.romankozak.forwardappmobile.core.context.ViewId

object NotesCapability : Capability {

    override val descriptor = object : CapabilityDescriptor {
        override val id = CapabilityId("notes")
        override val label: String = "Нотатки"
        override val iconRes: Int? = null
        override val navRoute: String = "notes_root"

        // Вказуємо, які ViewId підтримує ця можливість
        override val supportedViews: Set<ViewId> = setOf(ViewId("notes_main"))
    }

    override fun register(runtime: CapabilityRuntime) {
        // Логіка реєстрації в рантаймі (якщо потрібна)
    }
}