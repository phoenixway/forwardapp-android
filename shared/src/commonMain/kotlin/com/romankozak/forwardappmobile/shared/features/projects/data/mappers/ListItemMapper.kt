package com.romankozak.forwardappmobile.shared.features.projects.data.mappers

import com.romankozak.forwardappmobile.shared.database.ListItems
import com.romankozak.forwardappmobile.shared.features.projects.data.models.ListItem

fun ListItems.toDomain(): ListItem = ListItem(
    id = id,
    projectId = projectId,
    entityId = entityId,
    itemType = itemType,
    orderIndex = itemOrder
)