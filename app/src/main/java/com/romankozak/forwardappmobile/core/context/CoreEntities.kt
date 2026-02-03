package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilitySet
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.gate.ConfigurableState


@JvmInline
value class ContextId(val raw: String)

@JvmInline
value class ViewId(val raw: String)

data class ViewSet(
    val available: Set<ViewId>,
    val start: ViewId,
)
/**
 * Оновлений інтерфейс стану, який тепер знає про свою конфігурацію.
 */
interface ContextState : ConfigurableState {
    val id: ContextId
    val features: CapabilitySet
    val views: ViewSet
    
    // Властивість config успадковується з ConfigurableState:
    // override val config: ContextConfiguration
}

/**
 * Приклад реалізації для стабільного контексту.
 */
data class PersistentContextState(
    override val id: ContextId,
    override val config: ContextConfiguration, // Конфігурація, отримана з Room
    override val features: CapabilitySet,
    override val views: ViewSet
) : ContextState
