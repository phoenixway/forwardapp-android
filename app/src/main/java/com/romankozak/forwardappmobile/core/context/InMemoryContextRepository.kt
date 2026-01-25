package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilityDescriptor
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilityRegistry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реалізація реєстру, що зберігає можливості в пам'яті.
 * Отримує набір дескрипторів через ін'єкцію конструктора.
 */
@Singleton
class InMemoryCapabilityRegistry
    @Inject
    constructor(
        // Hilt автоматично збирає всі об'єкти CapabilityDescriptor,
        // які були позначені як @IntoSet у своїх модулях.
        private val availableCapabilities: Set<@JvmSuppressWildcards CapabilityDescriptor>,
    ) : CapabilityRegistry {
        // Створюємо карту для швидкого доступу за ID.
        private val map = availableCapabilities.associateBy { it.id }

        /** @inheritDoc */
        override fun all(): Set<CapabilityDescriptor> = map.values.toSet()

        /** @inheritDoc */
        override fun get(id: CapabilityId): CapabilityDescriptor? = map[id]
    }
