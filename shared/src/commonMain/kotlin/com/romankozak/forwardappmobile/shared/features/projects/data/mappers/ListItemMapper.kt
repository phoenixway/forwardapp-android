package com.romankozak.forwardappmobile.shared.features.projects.data.mappers

import com.romankozak.forwardappmobile.shared.database.ListItems
import com.romankozak.forwardappmobile.shared.features.projects.data.models.ListItem

fun ListItems.toDomain(): ListItem {
    return ListItem(
        id = id,
        projectId = projectId,
        itemType = itemType,
        entityId = entityId,
        order = order
    )
}
