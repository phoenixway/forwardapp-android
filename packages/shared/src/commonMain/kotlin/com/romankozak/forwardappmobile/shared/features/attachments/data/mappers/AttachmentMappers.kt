package com.romankozak.forwardappmobile.shared.features.attachments.data.mappers

import com.romankozak.forwardappmobile.shared.features.attachments.Attachments
import com.romankozak.forwardappmobile.shared.features.attachments.GetAttachmentsForProject
import com.romankozak.forwardappmobile.shared.features.attachments.domain.model.Attachment
import com.romankozak.forwardappmobile.shared.features.attachments.domain.model.ProjectAttachment

fun Attachments.toDomain(): Attachment =
    Attachment(
        id = id,
        attachmentType = attachmentType,
        entityId = entityId,
        ownerProjectId = ownerProjectId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun GetAttachmentsForProject.toDomain(): ProjectAttachment =
    ProjectAttachment(
        attachment = Attachment(
            id = id,
            attachmentType = attachmentType,
            entityId = entityId,
            ownerProjectId = ownerProjectId,
            createdAt = createdAt,
            updatedAt = updatedAt,
        ),
        projectId = projectId,
        attachmentOrder = attachmentOrder,
    )
