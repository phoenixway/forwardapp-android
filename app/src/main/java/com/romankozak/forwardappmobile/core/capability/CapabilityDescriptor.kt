// core/capability/CapabilityDescriptor.kt
package com.romankozak.forwardappmobile.core.capability

import com.romankozak.forwardappmobile.core.context.ViewId

// core/capability/CoreEntities.kt
interface CapabilityDescriptor {
    val id: CapabilityId
    val label: String
    val iconRes: Int?
    val navRoute: String
    val supportedViews: Set<ViewId>
}