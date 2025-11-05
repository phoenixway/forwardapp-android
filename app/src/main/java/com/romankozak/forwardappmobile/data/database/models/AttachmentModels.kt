package com.romankozak.forwardappmobile.data.database.models

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class AttachmentWithProject(
    @Embedded val attachment: AttachmentEntity,
    @ColumnInfo(name = "project_id") val projectId: String?,
    @ColumnInfo(name = "attachment_order") val attachmentOrder: Long?,
)
