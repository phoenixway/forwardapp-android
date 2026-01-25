package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilityId

data class ContextRole(
    val code: String,
    val label: String, // Напр. "Пацієнт", "Модуль коду"
    val defaultCapabilities: Set<CapabilityId>,
    val availableViews: Set<ViewId>,
    val startView: ViewId,
)
