package com.romankozak.forwardappmobile.shared.features.inbox

import com.romankozak.forwardappmobile.shared.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.shared.database.Inbox_records

fun Inbox_records.toDomain(): InboxRecord {
    return InboxRecord(
        id = this.id,
        projectId = this.project_id,
        text = this.text,
        createdAt = this.created_at,
        order = this.item_order
    )
}
