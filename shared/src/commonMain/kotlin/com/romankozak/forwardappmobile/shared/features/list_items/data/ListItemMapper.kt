package com.romankozak.forwardappmobile.shared.features.list_items.data

import com.romankozak.forwardappmobile.shared.data.database.models.ListItem
import com.romankozak.forwardappmobile.shared.database.ListItems

fun ListItems.toDomain(): ListItem {
    return ListItem(
        id = this.id,
        projectId = this.projectId
    )
}

fun ListItem.toSqlDelight(order: Long, entityId: String?, itemType: String?): ListItems {
    return ListItems(
        id = this.id,
        projectId = this.projectId,
        item_order = order,
        entityId = entityId,
        itemType = itemType
    )
}