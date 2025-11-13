package com.romankozak.forwardappmobile.shared.features.projects.views.inbox.data.mappers

import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.InboxRecords
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.model.InboxRecord

fun InboxRecords.toDomain(): InboxRecord =
    InboxRecord(
        id = id,
        projectId = projectId,
        text = text,
        createdAt = createdAt,
        itemOrder = itemOrder,
    )
