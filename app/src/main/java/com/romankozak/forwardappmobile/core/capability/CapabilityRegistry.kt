package com.romankozak.forwardappmobile.core.capability

/**
 * Реєстр усіх доступних можливостей (Capabilities) у системі.
 * Використовує Multibindings для автоматичного збору дескрипторів з різних модулів.
 */
interface CapabilityRegistry {
    /** Повертає набір усіх зареєстрованих дескрипторів можливостей.  */
    fun all(): Set<CapabilityDescriptor>

    /** Знаходить дескриптор за його унікальним ідентифікатором.  */
    fun get(id: CapabilityId): CapabilityDescriptor?
}
