package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilitySet
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration

@JvmInline
value class ContextId(val raw: String)

@JvmInline
value class ViewId(val raw: String)

data class ViewSet(
    val available: Set<ViewId>,
    val start: ViewId,
)

/**
 * Інтерфейс для будь-якого об'єкта, що має конфігурацію з БД.
 */
interface ConfigurableState {
    val config: ContextConfiguration
}

/**
 * Основний інтерфейс стану, який тепер ОБОВ'ЯЗКОВО
 * наслідує ConfigurableState.
 */
interface ContextState : ConfigurableState {
    val id: ContextId
    val features: CapabilitySet
    val views: ViewSet
    // override val config: ContextConfiguration // успадковується
}
