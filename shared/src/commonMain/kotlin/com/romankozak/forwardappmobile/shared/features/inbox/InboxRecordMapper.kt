package com.romankozak.forwardappmobile.shared.features.inbox

import com.romankozak.forwardappmobile.shared.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.shared.database.InboxRecords

fun InboxRecords.toDomain(): InboxRecord {
    return InboxRecord(
        id = this.id,
        projectId = this.projectId,
        text = this.text,
        createdAt = this.createdAt,
        order = this.item_order
    )
}

fun InboxRecord.toSqlDelight(): InboxRecords {
    return InboxRecords(
        id = this.id,
        projectId = this.projectId,
        text = this.text,
        createdAt = this.createdAt,
        item_order = this.order
    )
}
