package com.romankozak.forwardappmobile.shared.features.projects.listitems.data.mappers

import com.romankozak.forwardappmobile.shared.database.ListItems
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItem

fun ListItem.toDb(): ListItems = ListItems(
    id = id,
    projectId = projectId,
    itemOrder = itemOrder,
    entityId = entityId,
    itemType = itemType,
)

fun ListItems.toDomain(): ListItem = ListItem(
    id = id,
    projectId = projectId,
    itemOrder = itemOrder,
    entityId = entityId ?: "",
    itemType = itemType ?: "",
)