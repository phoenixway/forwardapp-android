package com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.data.mappers

import com.romankozak.forwardappmobile.shared.database.Notes
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.domain.model.LegacyNote

fun Notes.toDomain(): LegacyNote = LegacyNote(
    id = id,
    projectId = projectId,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt
)
