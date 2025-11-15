package com.romankozak.forwardappmobile.shared.features.notes.data.mappers

import com.romankozak.forwardappmobile.shared.database.Notes
import com.romankozak.forwardappmobile.shared.features.notes.domain.model.Note

fun Notes.toDomain(): Note {
    return Note(
        id = id,
        projectId = projectId,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
