package com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.data.mappers

import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.LegacyNotes
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.domain.model.LegacyNote

fun LegacyNotes.toDomain(): LegacyNote =
    LegacyNote(
        id = id,
        projectId = projectId,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
