// core/capability/CapabilityDescriptor.kt
package com.romankozak.forwardappmobile.core.capability

// core/capability/CoreEntities.kt
interface CapabilityDescriptor {
    val id: CapabilityId
    val label: String       // Назва для UI (напр. "Нотатки")
    val iconRes: Int?       // Іконка для меню
    val navRoute: String    // Маршрут для Jetpack Compose Navigation
}