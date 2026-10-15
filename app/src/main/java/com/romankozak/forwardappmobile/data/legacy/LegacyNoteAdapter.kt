package com.romankozak.forwardappmobile.data.legacy

import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity

/**
 * Адаптер для перетворення застарілих нотаток у нову модель `NoteDocument`.
 * Використовуємо, коли потрібно відобразити legacy-нотатки в оновленому UI.
 */
// Legacy projection only. A standalone Note has no Context-backed
// NoteDocument representation and must not receive a fabricated owner.
fun LegacyNoteEntity.toNoteDocument(): NoteDocumentEntity? =
    contextId?.let { historicalContextId ->
        NoteDocumentEntity(
            id = id,
            contextId = historicalContextId,
            name = title,
            createdAt = createdAt,
            updatedAt = updatedAt,
            content = content,
            lastCursorPosition = 0,
        )
    }
