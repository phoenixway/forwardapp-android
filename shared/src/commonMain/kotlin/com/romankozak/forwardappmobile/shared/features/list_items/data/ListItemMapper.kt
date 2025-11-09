package com.romankozak.forwardappmobile.shared.features.list_items.data

import com.romankozak.forwardappmobile.shared.database.ListItems
import com.romankozak.forwardappmobile.shared.data.database.models.ListItem as DomainListItem

fun ListItems.toDomain(): DomainListItem {
    return DomainListItem(
        id = this.id,
        projectId = this.projectId
    )
}

fun DomainListItem.toSqlDelight(order: Long, entityId: String?, itemType: String?): ListItems {
    return ListItems(
        id = this.id,
        projectId = this.projectId,
        item_order = order,
        entityId = entityId,
        itemType = itemType
    )
}
