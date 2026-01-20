package com.romankozak.forwardappmobile.core.navigation.capability

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ViewId

data class ViewDescriptor(
    val id: ViewId,               // Унікальний ID в'юхи (напр. "history") [cite: 5]
    val ownerCapability: CapabilityId, // Яка можливість надає цю в'юху
    val screenId: ScreenId,        // Фізичний маршрут у навігації
    val label: String              // Назва для відображення в меню
)