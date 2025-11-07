package com.romankozak.forwardappmobile.data.legacy

import com.romankozak.forwardappmobile.core.database.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.database.models.NoteDocumentEntity

/**
 * Адаптер для перетворення застарілих нотаток у нову модель `NoteDocument`.
 * Використовуємо, коли потрібно відобразити legacy-нотатки в оновленому UI.
 */
fun LegacyNoteEntity.toNoteDocument(): NoteDocumentEntity =
    NoteDocumentEntity(
        id = id,
        projectId = projectId,
        name = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        content = content,
        lastCursorPosition = 0,
    )
