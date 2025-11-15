package com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.data.mappers

import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.NoteDocumentItems
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.NoteDocuments
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.model.NoteDocument
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.model.NoteDocumentItem

fun NoteDocuments.toDomain(): NoteDocument =
    NoteDocument(
        id = id,
        projectId = projectId,
        name = name,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastCursorPosition = lastCursorPosition,
    )

fun NoteDocumentItems.toDomain(): NoteDocumentItem =
    NoteDocumentItem(
        id = id,
        documentId = listId,
        parentId = parentId,
        content = content,
        isCompleted = isCompleted,
        order = itemOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
