package com.romankozak.forwardappmobile.shared.features.inbox.data

import com.romankozak.forwardappmobile.shared.database.InboxRecords
import com.romankozak.forwardappmobile.shared.data.database.models.InboxRecord as DomainInboxRecord

fun InboxRecords.toDomain(): DomainInboxRecord {
    return DomainInboxRecord(
        id = this.id,
        projectId = this.projectId,
        text = this.text,
        createdAt = this.createdAt,
        order = this.item_order
    )
}

fun DomainInboxRecord.toSqlDelight(): InboxRecords {
    return InboxRecords(
        id = this.id,
        projectId = this.projectId,
        text = this.text,
        createdAt = this.createdAt,
        item_order = this.order
    )
}
