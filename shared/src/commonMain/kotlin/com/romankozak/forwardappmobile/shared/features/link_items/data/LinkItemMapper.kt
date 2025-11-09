package com.romankozak.forwardappmobile.shared.features.link_items.data

import com.romankozak.forwardappmobile.shared.database.LinkItems
import com.romankozak.forwardappmobile.shared.data.database.models.LinkItemEntity as DomainLinkItemEntity

fun LinkItems.toDomain(): DomainLinkItemEntity {
    return DomainLinkItemEntity(
        id = this.id,
        linkData = this.linkData,
        createdAt = this.createdAt
    )
}

fun DomainLinkItemEntity.toSqlDelight(): LinkItems {
    return LinkItems(
        id = this.id,
        linkData = this.linkData,
        createdAt = this.createdAt
    )
}
