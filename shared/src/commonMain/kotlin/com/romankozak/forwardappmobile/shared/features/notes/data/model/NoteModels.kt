package com.romankozak.forwardappmobile.shared.features.notes.data.model

import com.benasher44.uuid.uuid4
import kotlinx.datetime.Clock

private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * Legacy single-note entry that predates the Note Document editor.
 */
data class LegacyNote(
    val id: String = uuid4().toString(),
    val projectId: String,
    val title: String,
    val content: String,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
)

/**
 * Rich note document entity that supports hierarchical items and attachments.
 */
data class NoteDocument(
    val id: String = uuid4().toString(),
    val projectId: String,
    val name: String,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
    val content: String? = null,
    val lastCursorPosition: Int = 0,
)

/**
 * Individual node inside a note document.
 */
data class NoteDocumentItem(
    val id: String = uuid4().toString(),
    val listId: String,
    val parentId: String? = null,
    val content: String,
    val isCompleted: Boolean = false,
    val itemOrder: Long = 0,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
)
