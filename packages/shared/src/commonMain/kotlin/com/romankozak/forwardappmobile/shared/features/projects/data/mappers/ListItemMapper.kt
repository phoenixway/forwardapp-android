package com.romankozak.forwardappmobile.shared.features.projects.data.mappers

import com.romankozak.forwardappmobile.shared.database.ListItems
import com.romankozak.forwardappmobile.shared.features.projects.data.models.ListItem
import com.romankozak.forwardappmobile.shared.data.models.ListItemTypeValues

fun ListItems.toDomain(): ListItem = ListItem(
    id = id,
    projectId = projectId,
    entityId = entityId ?: "",
    itemType = itemType ?: ListItemTypeValues.GOAL,
    itemOrder = itemOrder
)